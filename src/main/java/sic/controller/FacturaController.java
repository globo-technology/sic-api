package sic.controller;

import java.math.BigDecimal;
import java.util.*;

import io.jsonwebtoken.Claims;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sic.aspect.AccesoRolesPermitidos;
import sic.modelo.*;
import sic.modelo.dto.FacturaCompraDTO;
import sic.modelo.dto.FacturaVentaDTO;
import sic.service.*;

@RestController
@RequestMapping("/api/v1")
public class FacturaController {

  private final IFacturaService facturaService;
  private final IEmpresaService empresaService;
  private final IProveedorService proveedorService;
  private final IClienteService clienteService;
  private final IUsuarioService usuarioService;
  private final ITransportistaService transportistaService;
  private final IReciboService reciboService;
  private final ModelMapper modelMapper;
  private final IAuthService authService;
  private static final int TAMANIO_PAGINA_DEFAULT = 25;

  @Autowired
  public FacturaController(
      IFacturaService facturaService,
      IEmpresaService empresaService,
      IProveedorService proveedorService,
      IClienteService clienteService,
      IUsuarioService usuarioService,
      ITransportistaService transportistaService,
      IReciboService reciboService,
      ModelMapper modelMapper,
      IAuthService authService) {
    this.facturaService = facturaService;
    this.empresaService = empresaService;
    this.proveedorService = proveedorService;
    this.clienteService = clienteService;
    this.usuarioService = usuarioService;
    this.transportistaService = transportistaService;
    this.reciboService = reciboService;
    this.authService = authService;
    this.modelMapper = modelMapper;
  }

    @GetMapping("/facturas/{idFactura}")
    @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR, Rol.VIAJANTE, Rol.COMPRADOR})
    public Factura getFacturaPorId(@PathVariable long idFactura) {
        return facturaService.getFacturaNoEliminadaPorId(idFactura);
    }

  @PostMapping("/facturas/venta")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR})
  public List<FacturaVenta> guardarFacturaVenta(
      @RequestBody FacturaVentaDTO facturaVentaDTO,
      @RequestParam(required = false) long[] idsFormaDePago,
      @RequestParam(required = false) BigDecimal[] montos,
      @RequestParam(required = false) int[] indices,
      @RequestParam(required = false) Long idPedido,
      @RequestHeader("Authorization") String authorizationHeader) {
    FacturaVenta fv = modelMapper.map(facturaVentaDTO, FacturaVenta.class);
    Empresa empresa = empresaService.getEmpresaPorId(facturaVentaDTO.getIdEmpresa());
    fv.setEmpresa(empresa);
    Cliente cliente = clienteService.getClienteNoEliminadoPorId(facturaVentaDTO.getIdCliente());
    if (cliente.getUbicacionFacturacion() == null
        && (fv.getTipoComprobante() == TipoDeComprobante.FACTURA_A
            || fv.getTipoComprobante() == TipoDeComprobante.FACTURA_B
            || fv.getTipoComprobante() == TipoDeComprobante.FACTURA_C)) {
      throw new BusinessServiceException(
          ResourceBundle.getBundle("Mensajes").getString("mensaje_ubicacion_facturacion_vacia"));
    }
    fv.setCliente(cliente);
    fv.setTransportista(transportistaService.getTransportistaNoEliminadoPorId(facturaVentaDTO.getIdTransportista()));
    fv.setFecha(new Date());
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    fv.setUsuario(usuarioService.getUsuarioNoEliminadoPorId(((Integer) claims.get("idUsuario")).longValue()));
    List<FacturaVenta> facturasGuardadas;
    if (indices != null) {
      facturasGuardadas =
          facturaService.guardar(
              facturaService.dividirFactura(fv, indices),
              idPedido,
              reciboService.construirRecibos(
                  idsFormaDePago,
                  empresa,
                  fv.getCliente(),
                  fv.getUsuario(),
                  montos,
                  fv.getTotal(),
                  fv.getFecha()));
    } else {
      List<FacturaVenta> facturas = new ArrayList<>();
      facturas.add(fv);
      facturasGuardadas =
          facturaService.guardar(
              facturas,
              idPedido,
              reciboService.construirRecibos(
                  idsFormaDePago,
                  empresa,
                  fv.getCliente(),
                  fv.getUsuario(),
                  montos,
                  fv.getTotal(),
                  fv.getFecha()));
    }
    return facturasGuardadas;
  }

  @PostMapping("/facturas/compra")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO})
  public List<FacturaCompra> guardarFacturaCompra(
      @RequestBody FacturaCompraDTO facturaCompraDTO,
      @RequestHeader("Authorization") String authorizationHeader) {
    FacturaCompra fc = modelMapper.map(facturaCompraDTO, FacturaCompra.class);
    fc.setEmpresa(empresaService.getEmpresaPorId(facturaCompraDTO.getIdEmpresa()));
    fc.setProveedor(proveedorService.getProveedorNoEliminadoPorId(facturaCompraDTO.getIdProveedor()));
    fc.setTransportista(
        transportistaService.getTransportistaNoEliminadoPorId(facturaCompraDTO.getIdTransportista()));
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    fc.setUsuario(usuarioService.getUsuarioNoEliminadoPorId(((Integer) claims.get("idUsuario")).longValue()));
    List<FacturaCompra> facturas = new ArrayList<>();
    facturas.add(fc);
    return facturaService.guardar(facturas);
  }

    @PostMapping("/facturas/{idFactura}/autorizacion")
    @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR})
    public FacturaVenta autorizarFactura(@PathVariable long idFactura) {
        return facturaService.autorizarFacturaVenta((FacturaVenta) facturaService.getFacturaNoEliminadaPorId(idFactura));
    }

    @GetMapping("/facturas/{idFactura}/renglones")
    @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR, Rol.VIAJANTE, Rol.COMPRADOR})
    public List<RenglonFactura> getRenglonesDeLaFactura(@PathVariable long idFactura) {
            return facturaService.getRenglonesDeLaFactura(idFactura);
    }

  @GetMapping("/facturas/{idFactura}/renglones/notas/credito")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR})
  public List<RenglonFactura> getRenglonesDeLaFacturaModificadosParaCredito(
      @PathVariable long idFactura) {
    return facturaService.getRenglonesDeLaFacturaModificadosParaCredito(idFactura);
  }

  @GetMapping("/facturas/compra/busqueda/criteria")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO})
  public Page<FacturaCompra> buscarFacturaCompra(
    @RequestParam Long idEmpresa,
    @RequestParam(required = false) Long desde,
    @RequestParam(required = false) Long hasta,
    @RequestParam(required = false) Long idProveedor,
    @RequestParam(required = false) Integer nroSerie,
    @RequestParam(required = false) Integer nroFactura,
    @RequestParam(required = false) TipoDeComprobante tipoDeComprobante,
    @RequestParam(required = false) Long idProducto,
    @RequestParam(required = false) Integer pagina,
    @RequestParam(required = false) String ordenarPor,
    @RequestParam(required = false) String sentido) {
    Calendar fechaDesde = Calendar.getInstance();
    Calendar fechaHasta = Calendar.getInstance();
    if ((desde != null) && (hasta != null)) {
      fechaDesde.setTimeInMillis(desde);
      fechaHasta.setTimeInMillis(hasta);
    }
    if (pagina == null || pagina < 0) {
      pagina = 0;
    }
    BusquedaFacturaCompraCriteria criteria =
      BusquedaFacturaCompraCriteria.builder()
        .idEmpresa(idEmpresa)
        .buscaPorFecha((desde != null) && (hasta != null))
        .fechaDesde(fechaDesde.getTime())
        .fechaHasta(fechaHasta.getTime())
        .buscaPorProveedor(idProveedor != null)
        .idProveedor(idProveedor)
        .buscaPorNumeroFactura((nroSerie != null) && (nroFactura != null))
        .numSerie((nroSerie != null) ? nroSerie : 0)
        .numFactura((nroFactura != null) ? nroFactura : 0)
        .buscaPorProducto(idProducto != null)
        .idProducto(idProducto)
        .buscaPorTipoComprobante(tipoDeComprobante != null)
        .tipoComprobante(tipoDeComprobante)
        .cantRegistros(0)
        .pageable(this.getPageable(pagina, ordenarPor, sentido))
        .build();
    return facturaService.buscarFacturaCompra(criteria);
  }

  @GetMapping("/facturas/venta/busqueda/criteria")
  @AccesoRolesPermitidos({
    Rol.ADMINISTRADOR,
    Rol.ENCARGADO,
    Rol.VENDEDOR,
    Rol.VIAJANTE,
    Rol.COMPRADOR
  })
  public Page<FacturaVenta> buscarFacturaVenta(
      @RequestParam Long idEmpresa,
      @RequestParam(required = false) Long desde,
      @RequestParam(required = false) Long hasta,
      @RequestParam(required = false) Long idCliente,
      @RequestParam(required = false) Integer nroSerie,
      @RequestParam(required = false) Integer nroFactura,
      @RequestParam(required = false) Long idViajante,
      @RequestParam(required = false) TipoDeComprobante tipoDeComprobante,
      @RequestParam(required = false) Long idUsuario,
      @RequestParam(required = false) Long nroPedido,
      @RequestParam(required = false) Long idProducto,
      @RequestParam(required = false) Integer pagina,
      @RequestParam(required = false) String ordenarPor,
      @RequestParam(required = false) String sentido,
      @RequestHeader("Authorization") String authorizationHeader) {
    Calendar fechaDesde = Calendar.getInstance();
    Calendar fechaHasta = Calendar.getInstance();
    if ((desde != null) && (hasta != null)) {
      fechaDesde.setTimeInMillis(desde);
      fechaHasta.setTimeInMillis(hasta);
    }
    if (pagina == null || pagina < 0) {
      pagina = 0;
    }
    BusquedaFacturaVentaCriteria criteria =
        BusquedaFacturaVentaCriteria.builder()
            .idEmpresa(idEmpresa)
            .buscaPorFecha((desde != null) && (hasta != null))
            .fechaDesde(fechaDesde.getTime())
            .fechaHasta(fechaHasta.getTime())
            .buscaCliente(idCliente != null)
            .idCliente(idCliente)
            .buscaUsuario(idUsuario != null)
            .idUsuario(idUsuario)
            .buscaViajante(idViajante != null)
            .idViajante(idViajante)
            .buscaPorNumeroFactura((nroSerie != null) && (nroFactura != null))
            .numSerie((nroSerie != null) ? nroSerie : 0)
            .numFactura((nroFactura != null) ? nroFactura : 0)
            .buscarPorPedido(nroPedido != null)
            .nroPedido((nroPedido != null) ? nroPedido : 0)
            .buscaPorProducto(idProducto != null)
            .idProducto(idProducto)
            .buscaPorTipoComprobante(tipoDeComprobante != null)
            .tipoComprobante(tipoDeComprobante)
            .cantRegistros(0)
            .pageable(this.getPageable(pagina, ordenarPor, sentido))
            .build();
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    return facturaService.buscarFacturaVenta(criteria, (int) claims.get("idUsuario"));
  }

  private Pageable getPageable(int pagina, String ordenarPor, String sentido) {
    String ordenDefault = "fecha";
    if (ordenarPor == null || sentido == null) {
      return PageRequest.of(pagina, TAMANIO_PAGINA_DEFAULT, new Sort(Sort.Direction.DESC, ordenDefault));
    } else {
      switch (sentido) {
        case "ASC":
          return PageRequest.of(pagina, TAMANIO_PAGINA_DEFAULT, new Sort(Sort.Direction.ASC, ordenarPor));
        case "DESC":
          return PageRequest.of(pagina, TAMANIO_PAGINA_DEFAULT, new Sort(Sort.Direction.DESC, ordenarPor));
        default:
          return PageRequest.of(pagina, TAMANIO_PAGINA_DEFAULT, new Sort(Sort.Direction.DESC, ordenDefault));
      }
    }
  }

    @GetMapping("/facturas/compra/tipos/empresas/{idEmpresa}/proveedores/{idProveedor}")
    @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR})
    public TipoDeComprobante[] getTipoFacturaCompra(@PathVariable long idEmpresa, @PathVariable long idProveedor) {
        return facturaService.getTipoFacturaCompra(empresaService.getEmpresaPorId(idEmpresa), proveedorService.getProveedorNoEliminadoPorId(idProveedor));
    }

  @GetMapping("/facturas/venta/tipos/empresas/{idEmpresa}/clientes/{idCliente}")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR, Rol.VIAJANTE})
  public TipoDeComprobante[] getTipoFacturaVenta(
      @PathVariable long idEmpresa,
      @PathVariable long idCliente,
      @RequestHeader("Authorization") String authorizationHeader) {
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    long idUsuario = (int) claims.get("idUsuario");
    List<Rol> rolesDeUsuario =
        usuarioService.getUsuarioNoEliminadoPorId(idUsuario).getRoles();
    if (rolesDeUsuario.contains(Rol.ADMINISTRADOR)
        || rolesDeUsuario.contains(Rol.ENCARGADO)
        || rolesDeUsuario.contains(Rol.VENDEDOR)) {
      return facturaService.getTipoFacturaVenta(
          empresaService.getEmpresaPorId(idEmpresa), clienteService.getClienteNoEliminadoPorId(idCliente));
    } else if (rolesDeUsuario.contains(Rol.VIAJANTE)
            || rolesDeUsuario.contains(Rol.COMPRADOR)) {
      return new TipoDeComprobante[] {TipoDeComprobante.PEDIDO};
    }
    return new TipoDeComprobante[0];
  }

    @GetMapping("/facturas/tipos/empresas/{idEmpresa}")
    @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR, Rol.VIAJANTE, Rol.COMPRADOR})
    public TipoDeComprobante[] getTiposFacturaSegunEmpresa(@PathVariable long idEmpresa) {
        return facturaService.getTiposFacturaSegunEmpresa(empresaService.getEmpresaPorId(idEmpresa));
    }    
    
    @GetMapping("/facturas/{idFactura}/reporte")
    @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR, Rol.VIAJANTE, Rol.COMPRADOR})
    public ResponseEntity<byte[]> getReporteFacturaVenta(@PathVariable long idFactura) {        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);        
        headers.add("content-disposition", "inline; filename=Factura.pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        byte[] reportePDF = facturaService.getReporteFacturaVenta(facturaService.getFacturaNoEliminadaPorId(idFactura));
        return new ResponseEntity<>(reportePDF, headers, HttpStatus.OK);
    }
    
    @GetMapping("/facturas/renglones/pedidos/{idPedido}")
    @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR, Rol.VIAJANTE})
    public List<RenglonFactura> getRenglonesPedidoParaFacturar(@PathVariable long idPedido,
                                                               @RequestParam TipoDeComprobante tipoDeComprobante) {
        return facturaService.getRenglonesPedidoParaFacturar(idPedido, tipoDeComprobante);
    }

    @GetMapping("/facturas/renglon")
    @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR, Rol.VIAJANTE})
    public RenglonFactura calcularRenglonVenta(@RequestParam long idProducto,
                                          @RequestParam TipoDeComprobante tipoDeComprobante,
                                          @RequestParam Movimiento movimiento,
                                          @RequestParam BigDecimal cantidad, 
                                          @RequestParam BigDecimal descuentoPorcentaje) {
        return facturaService.calcularRenglon(tipoDeComprobante, movimiento, cantidad, idProducto, descuentoPorcentaje, false);
    }

  @GetMapping("/facturas/total-facturado-venta/criteria")
  @AccesoRolesPermitidos({
    Rol.ADMINISTRADOR,
    Rol.ENCARGADO,
    Rol.VENDEDOR,
    Rol.VIAJANTE,
    Rol.COMPRADOR
  })
  public BigDecimal calcularTotalFacturadoVenta(
      @RequestParam Long idEmpresa,
      @RequestParam(required = false) Long desde,
      @RequestParam(required = false) Long hasta,
      @RequestParam(required = false) Long idCliente,
      @RequestParam(required = false) Integer nroSerie,
      @RequestParam(required = false) Integer nroFactura,
      @RequestParam(required = false) Long idViajante,
      @RequestParam(required = false) TipoDeComprobante tipoDeComprobante,
      @RequestParam(required = false) Long idUsuario,
      @RequestParam(required = false) Long nroPedido,
      @RequestParam(required = false) Long idProducto,
      @RequestHeader("Authorization") String authorizationHeader) {
    Calendar fechaDesde = Calendar.getInstance();
    Calendar fechaHasta = Calendar.getInstance();
    if ((desde != null) && (hasta != null)) {
      fechaDesde.setTimeInMillis(desde);
      fechaHasta.setTimeInMillis(hasta);
    }
    BusquedaFacturaVentaCriteria criteria =
        BusquedaFacturaVentaCriteria.builder()
            .idEmpresa(idEmpresa)
            .buscaPorFecha((desde != null) && (hasta != null))
            .fechaDesde(fechaDesde.getTime())
            .fechaHasta(fechaHasta.getTime())
            .buscaCliente(idCliente != null)
            .idCliente(idCliente)
            .buscaUsuario(idUsuario != null)
            .idUsuario(idUsuario)
            .buscaViajante(idViajante != null)
            .idViajante(idViajante)
            .buscaPorNumeroFactura((nroSerie != null) && (nroFactura != null))
            .numSerie((nroSerie != null) ? nroSerie : 0)
            .numFactura((nroFactura != null) ? nroFactura : 0)
            .buscarPorPedido(nroPedido != null)
            .nroPedido((nroPedido != null) ? nroPedido : 0)
            .buscaPorProducto(idProducto !=null)
            .idProducto((idProducto != null) ? idProducto : 0)
            .buscaPorTipoComprobante(tipoDeComprobante != null)
            .tipoComprobante(tipoDeComprobante)
            .cantRegistros(0)
            .build();
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    return facturaService.calcularTotalFacturadoVenta(criteria, (int) claims.get("idUsuario"));
  }

  @GetMapping("/facturas/total-facturado-compra/criteria")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO})
  public BigDecimal calcularTotalFacturadoCompra(
      @RequestParam Long idEmpresa,
      @RequestParam(required = false) Long desde,
      @RequestParam(required = false) Long hasta,
      @RequestParam(required = false) Long idProveedor,
      @RequestParam(required = false) TipoDeComprobante tipoDeComprobante,
      @RequestParam(required = false) Integer nroSerie,
      @RequestParam(required = false) Integer nroFactura,
      @RequestParam(required = false) Long idProducto) {
    Calendar fechaDesde = Calendar.getInstance();
    Calendar fechaHasta = Calendar.getInstance();
    if ((desde != null) && (hasta != null)) {
      fechaDesde.setTimeInMillis(desde);
      fechaHasta.setTimeInMillis(hasta);
    }
    BusquedaFacturaCompraCriteria criteria =
        BusquedaFacturaCompraCriteria.builder()
            .idEmpresa(idEmpresa)
            .buscaPorFecha((desde != null) && (hasta != null))
            .fechaDesde(fechaDesde.getTime())
            .fechaHasta(fechaHasta.getTime())
            .buscaPorProveedor(idProveedor != null)
            .idProveedor(idProveedor)
            .buscaPorTipoComprobante(tipoDeComprobante != null)
            .tipoComprobante(tipoDeComprobante)
            .buscaPorNumeroFactura((nroSerie != null) && (nroFactura != null))
            .numSerie((nroSerie != null) ? nroSerie : 0)
            .numFactura((nroFactura != null) ? nroFactura : 0)
            .cantRegistros(0)
            .buscaPorProducto(idProducto != null)
            .idProducto((idProducto != null) ? idProducto : 0)
            .build();
    return facturaService.calcularTotalFacturadoCompra(criteria);
  }

  @GetMapping("/facturas/total-iva-venta/criteria")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO})
  public BigDecimal calcularIvaVenta(
      @RequestParam Long idEmpresa,
      @RequestParam(required = false) Long desde,
      @RequestParam(required = false) Long hasta,
      @RequestParam(required = false) Long idCliente,
      @RequestParam(required = false) Integer nroSerie,
      @RequestParam(required = false) Integer nroFactura,
      @RequestParam(required = false) Long idViajante,
      @RequestParam(required = false) TipoDeComprobante tipoDeComprobante,
      @RequestParam(required = false) Long idUsuario,
      @RequestParam(required = false) Long nroPedido,
      @RequestParam(required = false) Long idProducto,
      @RequestHeader("Authorization") String authorizationHeader) {
    Calendar fechaDesde = Calendar.getInstance();
    Calendar fechaHasta = Calendar.getInstance();
    if ((desde != null) && (hasta != null)) {
      fechaDesde.setTimeInMillis(desde);
      fechaHasta.setTimeInMillis(hasta);
    }
    BusquedaFacturaVentaCriteria criteria =
        BusquedaFacturaVentaCriteria.builder()
            .idEmpresa(idEmpresa)
            .buscaPorFecha((desde != null) && (hasta != null))
            .fechaDesde(fechaDesde.getTime())
            .fechaHasta(fechaHasta.getTime())
            .buscaCliente(idCliente != null)
            .idCliente(idCliente)
            .buscaUsuario(idUsuario != null)
            .idUsuario(idUsuario)
            .buscaViajante(idViajante != null)
            .idViajante(idViajante)
            .buscaPorNumeroFactura((nroSerie != null) && (nroFactura != null))
            .numSerie((nroSerie != null) ? nroSerie : 0)
            .numFactura((nroFactura != null) ? nroFactura : 0)
            .buscarPorPedido(nroPedido != null)
            .nroPedido((nroPedido != null) ? nroPedido : 0)
            .buscaPorProducto(idProducto != null)
            .idProducto((idProducto != null) ? idProducto : 0)
            .buscaPorTipoComprobante(tipoDeComprobante != null)
            .tipoComprobante(tipoDeComprobante)
            .cantRegistros(0)
            .build();
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    return facturaService.calcularIvaVenta(criteria, (int) claims.get("idUsuario"));
  }

  @GetMapping("/facturas/total-iva-compra/criteria")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO})
  public BigDecimal calcularTotalIvaCompra(
      @RequestParam Long idEmpresa,
      @RequestParam(required = false) Long desde,
      @RequestParam(required = false) Long hasta,
      @RequestParam(required = false) Long idProveedor,
      @RequestParam(required = false) TipoDeComprobante tipoDeComprobante,
      @RequestParam(required = false) Integer nroSerie,
      @RequestParam(required = false) Integer nroFactura,
      @RequestParam(required = false) Long idProducto) {
    Calendar fechaDesde = Calendar.getInstance();
    Calendar fechaHasta = Calendar.getInstance();
    if ((desde != null) && (hasta != null)) {
      fechaDesde.setTimeInMillis(desde);
      fechaHasta.setTimeInMillis(hasta);
    }
    BusquedaFacturaCompraCriteria criteria =
        BusquedaFacturaCompraCriteria.builder()
            .idEmpresa(idEmpresa)
            .buscaPorFecha((desde != null) && (hasta != null))
            .fechaDesde(fechaDesde.getTime())
            .fechaHasta(fechaHasta.getTime())
            .buscaPorProveedor(idProveedor != null)
            .idProveedor(idProveedor)
            .buscaPorTipoComprobante(tipoDeComprobante != null)
            .tipoComprobante(tipoDeComprobante)
            .buscaPorNumeroFactura((nroSerie != null) && (nroFactura != null))
            .numSerie((nroSerie != null) ? nroSerie : 0)
            .numFactura((nroFactura != null) ? nroFactura : 0)
            .cantRegistros(0)
            .buscaPorProducto(idProducto != null)
            .idProducto((idProducto != null) ? idProducto : 0)
            .build();
    return facturaService.calcularIvaCompra(criteria);
  }

  @GetMapping("/facturas/ganancia-total/criteria")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO})
  public BigDecimal calcularGananciaTotal(
      @RequestParam Long idEmpresa,
      @RequestParam(required = false) Long desde,
      @RequestParam(required = false) Long hasta,
      @RequestParam(required = false) Long idCliente,
      @RequestParam(required = false) Integer nroSerie,
      @RequestParam(required = false) Integer nroFactura,
      @RequestParam(required = false) Long idViajante,
      @RequestParam(required = false) TipoDeComprobante tipoDeComprobante,
      @RequestParam(required = false) Long idUsuario,
      @RequestParam(required = false) Long nroPedido,
      @RequestParam(required = false) Long idProducto,
      @RequestHeader("Authorization") String authorizationHeader) {
    Calendar fechaDesde = Calendar.getInstance();
    Calendar fechaHasta = Calendar.getInstance();
    if ((desde != null) && (hasta != null)) {
      fechaDesde.setTimeInMillis(desde);
      fechaHasta.setTimeInMillis(hasta);
    }
    BusquedaFacturaVentaCriteria criteria =
        BusquedaFacturaVentaCriteria.builder()
            .idEmpresa(idEmpresa)
            .buscaPorFecha((desde != null) && (hasta != null))
            .fechaDesde(fechaDesde.getTime())
            .fechaHasta(fechaHasta.getTime())
            .buscaCliente(idCliente != null)
            .idCliente(idCliente)
            .buscaUsuario(idUsuario != null)
            .idUsuario(idUsuario)
            .buscaViajante(idViajante != null)
            .idViajante(idViajante)
            .buscaPorNumeroFactura((nroSerie != null) && (nroFactura != null))
            .numSerie((nroSerie != null) ? nroSerie : 0)
            .numFactura((nroFactura != null) ? nroFactura : 0)
            .buscarPorPedido(nroPedido != null)
            .nroPedido((nroPedido != null) ? nroPedido : 0)
            .buscaPorProducto(idProducto != null)
            .idProducto((idProducto != null) ? idProducto : 0)
            .buscaPorTipoComprobante(tipoDeComprobante != null)
            .tipoComprobante(tipoDeComprobante)
            .cantRegistros(0)
            .build();
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    return facturaService.calcularGananciaTotal(criteria, (int) claims.get("idUsuario"));
  }
}
