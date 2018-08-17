package sic.controller;

import java.math.BigDecimal;
import java.util.ResourceBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sic.aspect.AccesoRolesPermitidos;
import sic.modelo.*;
import sic.service.BusinessServiceException;
import sic.service.IClienteService;
import sic.service.ICuentaCorrienteService;
import sic.service.IProveedorService;

@RestController
@RequestMapping("/api/v1")
public class CuentaCorrienteController {

  private final ICuentaCorrienteService cuentaCorrienteService;
  private final IProveedorService proveedorService;
  private final IClienteService clienteService;
  private static final int TAMANIO_PAGINA_DEFAULT = 50;

  @Autowired
  public CuentaCorrienteController(
      ICuentaCorrienteService cuentaCorrienteService,
      IProveedorService proveedorService,
      IClienteService clienteService) {
    this.cuentaCorrienteService = cuentaCorrienteService;
    this.clienteService = clienteService;
    this.proveedorService = proveedorService;
  }

  @GetMapping("/cuentas-corriente/clientes/{idCliente}")
  @ResponseStatus(HttpStatus.OK)
  @AccesoRolesPermitidos({
    Rol.ADMINISTRADOR,
    Rol.ENCARGADO,
    Rol.VENDEDOR,
    Rol.VIAJANTE,
    Rol.COMPRADOR
  })
  public CuentaCorrienteCliente getCuentaCorrientePorCliente(@PathVariable Long idCliente) {
    return cuentaCorrienteService.getCuentaCorrientePorCliente(
        clienteService.getClientePorId(idCliente));
  }

  @GetMapping("/cuentas-corriente/proveedores/{idProveedor}")
  @ResponseStatus(HttpStatus.OK)
  @AccesoRolesPermitidos({
    Rol.ADMINISTRADOR,
    Rol.ENCARGADO,
    Rol.VENDEDOR,
    Rol.VIAJANTE,
    Rol.COMPRADOR
  })
  public CuentaCorrienteProveedor getCuentaCorrientePorProveedor(@PathVariable Long idProveedor) {
    return cuentaCorrienteService.getCuentaCorrientePorProveedor(
        proveedorService.getProveedorPorId(idProveedor));
  }

  @GetMapping("/cuentas-corriente/clientes/{idCliente}/saldo")
  @ResponseStatus(HttpStatus.OK)
  @AccesoRolesPermitidos({
    Rol.ADMINISTRADOR,
    Rol.ENCARGADO,
    Rol.VENDEDOR,
    Rol.VIAJANTE,
    Rol.COMPRADOR
  })
  public BigDecimal getSaldoCuentaCorrienteCliente(@PathVariable long idCliente) {
    return cuentaCorrienteService
        .getCuentaCorrientePorCliente(clienteService.getClientePorId(idCliente))
        .getSaldo();
  }

  @GetMapping("/cuentas-corriente/proveedores/{idProveedor}/saldo")
  @ResponseStatus(HttpStatus.OK)
  @AccesoRolesPermitidos({
    Rol.ADMINISTRADOR,
    Rol.ENCARGADO,
    Rol.VENDEDOR,
    Rol.VIAJANTE,
    Rol.COMPRADOR
  })
  public BigDecimal getSaldoCuentaCorrienteProveedor(@PathVariable long idProveedor) {
    return cuentaCorrienteService
        .getCuentaCorrientePorProveedor(proveedorService.getProveedorPorId(idProveedor))
        .getSaldo();
  }

  @GetMapping("/cuentas-corriente/{idCuentaCorriente}/renglones")
  @ResponseStatus(HttpStatus.OK)
  @AccesoRolesPermitidos({
    Rol.ADMINISTRADOR,
    Rol.ENCARGADO,
    Rol.VENDEDOR,
    Rol.VIAJANTE,
    Rol.COMPRADOR
  })
  public Page<RenglonCuentaCorriente> getRenglonesCuentaCorriente(
      @PathVariable long idCuentaCorriente,
      @RequestParam(required = false) Integer pagina,
      @RequestParam(required = false) Integer tamanio) {
    if (tamanio == null || tamanio <= 0) tamanio = TAMANIO_PAGINA_DEFAULT;
    if (pagina == null || pagina < 0) pagina = 0;
    Pageable pageable = new PageRequest(pagina, tamanio);
    return cuentaCorrienteService.getRenglonesCuentaCorriente(idCuentaCorriente, pageable);
  }

  @GetMapping("/cuentas-corriente/clientes/{idCliente}/reporte")
  @AccesoRolesPermitidos({
    Rol.ADMINISTRADOR,
    Rol.ENCARGADO,
    Rol.VENDEDOR,
    Rol.VIAJANTE,
    Rol.COMPRADOR
  })
  public ResponseEntity<byte[]> getReporteCuentaCorrienteXls(
      @PathVariable long idCliente,
      @RequestParam(required = false) Integer pagina,
      @RequestParam(required = false) Integer tamanio,
      @RequestParam(required = false) String formato) {
    if (tamanio == null || tamanio <= 0) tamanio = TAMANIO_PAGINA_DEFAULT;
    if (pagina == null || pagina < 0) pagina = 0;
    Pageable pageable = new PageRequest(pagina, tamanio);
    HttpHeaders headers = new HttpHeaders();
    headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
    switch (formato) {
      case "xlsx":
        headers.setContentType(new MediaType("application", "vnd.ms-excel"));
        headers.set("Content-Disposition", "attachment; filename=EstadoCuentaCorriente.xlsx");
        byte[] reporteXls =
            cuentaCorrienteService.getReporteCuentaCorrienteCliente(
                cuentaCorrienteService.getCuentaCorrientePorCliente(
                    clienteService.getClientePorId(idCliente)),
                pageable,
                formato);
        headers.setContentLength(reporteXls.length);
        return new ResponseEntity<>(reporteXls, headers, HttpStatus.OK);
      case "pdf":
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.add("Content-Disposition", "attachment; filename=EstadoCuentaCorriente.pdf");
        byte[] reportePDF =
            cuentaCorrienteService.getReporteCuentaCorrienteCliente(
                cuentaCorrienteService.getCuentaCorrientePorCliente(
                    clienteService.getClientePorId(idCliente)),
                pageable,
                formato);
        return new ResponseEntity<>(reportePDF, headers, HttpStatus.OK);
      default:
        throw new BusinessServiceException(
            ResourceBundle.getBundle("Mensajes").getString("mensaje_formato_no_valido"));
    }
  }
}
