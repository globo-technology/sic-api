package sic.controller;

import java.math.BigDecimal;
import java.util.*;

import io.jsonwebtoken.Claims;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sic.aspect.AccesoRolesPermitidos;
import sic.modelo.*;
import sic.modelo.criteria.BusquedaNotaCriteria;
import sic.modelo.dto.*;
import sic.service.*;
import sic.exception.BusinessServiceException;

@RestController
@RequestMapping("/api/v1")
public class NotaController {

  private final INotaService notaService;
  private final IReciboService reciboService;
  private final ISucursalService sucursalService;
  private final IClienteService clienteService;
  private final IProveedorService proveedorService;
  private final IUsuarioService usuarioService;
  private final IFacturaService facturaService;
  private final IAuthService authService;
  private final ModelMapper modelMapper;
  private final MessageSource messageSource;

  @Autowired
  public NotaController(
      INotaService notaService,
      IReciboService reciboService,
      ISucursalService sucursalService,
      IClienteService clienteService,
      IProveedorService proveedorService,
      IUsuarioService usuarioService,
      IFacturaService facturaService,
      IAuthService authService,
      ModelMapper modelMapper,
      MessageSource messageSource) {
    this.notaService = notaService;
    this.reciboService = reciboService;
    this.sucursalService = sucursalService;
    this.clienteService = clienteService;
    this.proveedorService = proveedorService;
    this.usuarioService = usuarioService;
    this.facturaService = facturaService;
    this.authService = authService;
    this.modelMapper = modelMapper;
    this.messageSource = messageSource;
  }

  @GetMapping("/notas/{idNota}")
  public Nota getNota(@PathVariable long idNota) {
    return notaService.getNotaNoEliminadaPorId(idNota);
  }

  @DeleteMapping("/notas/{idNota}")
  @AccesoRolesPermitidos(Rol.ADMINISTRADOR)
  public void eliminarNota(@PathVariable long idNota) {
    notaService.eliminarNota(idNota);
  }

  @GetMapping("/notas/credito/tipos/sucursales/{idSucursal}")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR})
  public TipoDeComprobante[] getTipoNotaCreditoSucursal(@PathVariable long idSucursal) {
    return notaService.getTiposNotaCredito(sucursalService.getSucursalPorId(idSucursal));
  }


  @GetMapping("/notas/debito/tipos/sucursales/{idSucursal}")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR})
  public TipoDeComprobante[] getTipoNotaDebitoSucursal(@PathVariable long idSucursal) {
    return notaService.getTiposNotaDebito(sucursalService.getSucursalPorId(idSucursal));
  }

  @GetMapping("/notas/{idNota}/facturas")
  public Factura getFacturaNotaCredito(@PathVariable long idNota) {
    return notaService.getFacturaDeLaNotaCredito(idNota);
  }

  @GetMapping("/notas/debito/recibo/{idRecibo}/existe")
  public boolean existeNotaDebitoRecibo(@PathVariable long idRecibo) {
    return notaService.existsNotaDebitoPorRecibo(reciboService.getReciboNoEliminadoPorId(idRecibo));
  }

  @GetMapping("/notas/clientes/tipos/credito")
  public List<TipoDeComprobante> getTipoNotaCreditoCliente(
      @RequestParam long idCliente, @RequestParam long idSucursal) {
    return notaService.getTipoNotaCreditoCliente(idCliente, idSucursal);
  }

  @GetMapping("/notas/clientes/tipos/debito")
  public List<TipoDeComprobante> getTipoNotaDebitoCliente(
      @RequestParam long idCliente, @RequestParam long idSucursal) {
    return notaService.getTipoNotaDebitoCliente(idCliente, idSucursal);
  }

  @GetMapping("/notas/proveedores/tipos/credito")
  public List<TipoDeComprobante> getTipoNotaCreditoProveedor(
    @RequestParam long idProveedor, @RequestParam long idSucursal) {
    return notaService.getTipoNotaCreditoProveedor(idProveedor, idSucursal);
  }

  @GetMapping("/notas/proveedores/tipos/debito")
  public List<TipoDeComprobante> getTipoNotaDebitoProveedor(
    @RequestParam long idProveedor, @RequestParam long idSucursal) {
    return notaService.getTipoNotaDebitoProveedor(idProveedor, idSucursal);
  }

  @GetMapping("/notas/renglones/credito/{idNotaCredito}")
  public List<RenglonNotaCredito> getRenglonesDeNotaCreditoCliente(
      @PathVariable long idNotaCredito) {
    return notaService.getRenglonesDeNotaCredito(idNotaCredito);
  }

  @GetMapping("/notas/renglones/debito/{idNotaDebito}")
  public List<RenglonNotaDebito> getRenglonesDeNotaDebitoCliente(@PathVariable long idNotaDebito) {
    return notaService.getRenglonesDeNotaDebito(idNotaDebito);
  }

  @PostMapping("/notas/credito/calculos")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO})
  public NotaCredito calcularNotaCreditoConFactura(
      @RequestBody NuevaNotaCreditoDeFacturaDTO nuevaNotaCreditoDeFacturaDTO,
      @RequestHeader("Authorization") String authorizationHeader) {
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    return notaService.calcularNotaCreditoConFactura(
        nuevaNotaCreditoDeFacturaDTO,
        usuarioService.getUsuarioNoEliminadoPorId(((Integer) claims.get("idUsuario")).longValue()));
  }

  @PostMapping("/notas/credito/calculos-sin-factura")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO})
  public NotaCredito calcularNotaCreditoSinFactura(
      @RequestBody NuevaNotaCreditoSinFacturaDTO nuevaNotaCreditoSinFacturaDTO,
      @RequestHeader("Authorization") String authorizationHeader) {
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    return notaService.calcularNotaCreditoSinFactura(
        nuevaNotaCreditoSinFacturaDTO,
        usuarioService.getUsuarioNoEliminadoPorId(((Integer) claims.get("idUsuario")).longValue()));
  }

  @PostMapping("/notas/debito/calculos")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO})
  public NotaDebito calcularNotaDebitoDeRecibo(
      @RequestBody NuevaNotaDebitoDeReciboDTO nuevaNotaDebitoDeReciboDTO,
      @RequestHeader("Authorization") String authorizationHeader) {
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    return notaService.calcularNotaDebitoConRecibo(
        nuevaNotaDebitoDeReciboDTO,
        usuarioService.getUsuarioNoEliminadoPorId(((Integer) claims.get("idUsuario")).longValue()));
  }

  @PostMapping("/notas/debito/calculos-sin-recibo")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO})
  public NotaDebito calcularNotaDebitoSinRecibo(
      @RequestBody NuevaNotaDebitoSinReciboDTO nuevaNotaDebitoSinReciboDTO,
      @RequestHeader("Authorization") String authorizationHeader) {
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    return notaService.calcularNotaDebitoSinRecibo(
        nuevaNotaDebitoSinReciboDTO,
        usuarioService.getUsuarioNoEliminadoPorId(((Integer) claims.get("idUsuario")).longValue()));
  }

  @PostMapping("/notas/credito/factura")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO})
  public NotaCredito guardarNotaCreditoDeFactura(
      @RequestBody NuevaNotaCreditoDeFacturaDTO nuevaNotaCreditoDeFacturaDTO,
      @RequestHeader("Authorization") String authorizationHeader) {
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    return notaService.guardarNotaCredito(
        notaService.calcularNotaCreditoConFactura(
            nuevaNotaCreditoDeFacturaDTO,
            usuarioService.getUsuarioNoEliminadoPorId(
                ((Integer) claims.get("idUsuario")).longValue())));
  }

  @PostMapping("/notas/credito/sin-factura")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO})
  public NotaCredito guardarNotaCreditoSinFactura(
      @RequestBody NuevaNotaCreditoSinFacturaDTO nuevaNotaCreditoSinFacturaDTO,
      @RequestHeader("Authorization") String authorizationHeader) {
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    return notaService.guardarNotaCredito(
        notaService.calcularNotaCreditoSinFactura(
            nuevaNotaCreditoSinFacturaDTO,
            usuarioService.getUsuarioNoEliminadoPorId(
                ((Integer) claims.get("idUsuario")).longValue())));
  }

  @PostMapping("/notas/debito")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO})
  public NotaDebito guardarNotaDebitoDeRecibo(
      @RequestBody NuevaNotaDebitoDeReciboDTO nuevaNotaDebitoDeReciboDTO,
      @RequestHeader("Authorization") String authorizationHeader) {
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    return notaService.guardarNotaDebito(
        notaService.calcularNotaDebitoConRecibo(
            nuevaNotaDebitoDeReciboDTO,
            usuarioService.getUsuarioNoEliminadoPorId(
                ((Integer) claims.get("idUsuario")).longValue())));
  }

  @PostMapping("/notas/debito/sin-recibo")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO})
  public NotaDebito guardarNotaDebitoSinRecibo(
          @RequestBody NuevaNotaDebitoSinReciboDTO nuevaNotaDebitoSinReciboDTO,
          @RequestHeader("Authorization") String authorizationHeader) {
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    return notaService.guardarNotaDebito(notaService.calcularNotaDebitoSinRecibo(
            nuevaNotaDebitoSinReciboDTO,
            usuarioService.getUsuarioNoEliminadoPorId(((Integer) claims.get("idUsuario")).longValue())));
  }

  @GetMapping("/notas/{idNota}/reporte")
  public ResponseEntity<byte[]> getReporteNota(@PathVariable long idNota) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    Nota nota = notaService.getNotaNoEliminadaPorId(idNota);
    String fileName = (nota instanceof NotaCredito) ? "NotaCredito.pdf" : "NotaDebito.pdf";
    headers.add("content-disposition", "inline; filename=" + fileName);
    headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
    byte[] reportePDF = notaService.getReporteNota(nota);
    return new ResponseEntity<>(reportePDF, headers, HttpStatus.OK);
  }

  @PostMapping("/notas/{idNota}/autorizacion")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR})
  public Nota autorizarNota(@PathVariable long idNota) {
    return notaService.autorizarNota(notaService.getNotaNoEliminadaPorId(idNota));
  }

  @GetMapping("/notas/debito/total")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR})
  public BigDecimal calcularTotalDebito(
      BigDecimal subTotalBruto, BigDecimal iva21Neto, BigDecimal montoNoGravado) {
    return notaService.calcularTotalDebito(subTotalBruto, iva21Neto, montoNoGravado);
  }

  @PostMapping("/notas/credito/busqueda/criteria")
  public Page<NotaCredito> buscarNotasCredito(
    @RequestBody BusquedaNotaCriteria busquedaNotaCriteria,
    @RequestHeader("Authorization") String authorizationHeader) {
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    return notaService.buscarNotasCredito(busquedaNotaCriteria, (int) claims.get("idUsuario"));
  }

  @PostMapping("/notas/debito/busqueda/criteria")
  public Page<NotaDebito> buscarNotasDebito(
    @RequestBody BusquedaNotaCriteria busquedaNotaCriteria,
    @RequestHeader("Authorization") String authorizationHeader) {
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    return notaService.buscarNotasDebito(busquedaNotaCriteria, (int) claims.get("idUsuario"));
  }

  @PostMapping("/notas/total-credito/criteria")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR})
  public BigDecimal getTotalNotasCredito(
      @RequestBody BusquedaNotaCriteria busquedaNotaCriteria,
      @RequestHeader("Authorization") String authorizationHeader) {
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    return notaService.calcularTotalCredito(busquedaNotaCriteria, (int) claims.get("idUsuario"));
  }

  @PostMapping("/notas/total-debito/criteria")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR})
  public BigDecimal getTotalNotasDebito(
      @RequestBody BusquedaNotaCriteria busquedaNotaCriteria,
      @RequestHeader("Authorization") String authorizationHeader) {
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    return notaService.calcularTotalDebito(busquedaNotaCriteria, (int) claims.get("idUsuario"));
  }

  @PostMapping("/notas/total-iva-credito/criteria")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR})
  public BigDecimal getTotalIvaCredito(
      @RequestBody BusquedaNotaCriteria busquedaNotaCriteria,
      @RequestHeader("Authorization") String authorizationHeader) {
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    return notaService.calcularTotalIVACredito(busquedaNotaCriteria, (int) claims.get("idUsuario"));
  }

  @PostMapping("/notas/total-iva-debito/criteria")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR})
  public BigDecimal getTotalIvaDebito(
      @RequestBody BusquedaNotaCriteria busquedaNotaCriteria,
      @RequestHeader("Authorization") String authorizationHeader) {
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    return notaService.calcularTotalIVADebito(busquedaNotaCriteria, (int) claims.get("idUsuario"));
  }
}
