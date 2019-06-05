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
import sic.modelo.dto.*;
import sic.service.*;

@RestController
@RequestMapping("/api/v1")
public class NotaController {

  private final INotaService notaService;
  private final IReciboService reciboService;
  private final IEmpresaService empresaService;
  private final IClienteService clienteService;
  private final IProveedorService proveedorService;
  private final IUsuarioService usuarioService;
  private final IFacturaService facturaService;
  private final IAuthService authService;
  private final ModelMapper modelMapper;
  private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("Mensajes");
  private static final BigDecimal IVA_21 = new BigDecimal("21");
  private static final BigDecimal IVA_105 = new BigDecimal("10.5");
  private static final int TAMANIO_PAGINA_DEFAULT = 25;

  @Autowired
  public NotaController(
      INotaService notaService,
      IReciboService reciboService,
      IEmpresaService empresaService,
      IClienteService clienteService,
      IProveedorService proveedorService,
      IUsuarioService usuarioService,
      IFacturaService facturaService,
      IAuthService authService,
      ModelMapper modelMapper) {
    this.notaService = notaService;
    this.reciboService = reciboService;
    this.empresaService = empresaService;
    this.clienteService = clienteService;
    this.proveedorService = proveedorService;
    this.usuarioService = usuarioService;
    this.facturaService = facturaService;
    this.authService = authService;
    this.modelMapper = modelMapper;
  }

  @GetMapping("/notas/{idNota}")
  @AccesoRolesPermitidos({
    Rol.ADMINISTRADOR,
    Rol.ENCARGADO,
    Rol.VENDEDOR,
    Rol.VIAJANTE,
    Rol.COMPRADOR
  })
  public Nota getNota(@PathVariable long idNota) {
    return notaService.getNotaNoEliminadaPorId(idNota);
  }

  @GetMapping("/notas/busqueda/criteria")
  @AccesoRolesPermitidos({
    Rol.ADMINISTRADOR,
    Rol.ENCARGADO,
    Rol.VENDEDOR,
    Rol.VIAJANTE,
    Rol.COMPRADOR
  })
  public Page<Nota> buscarNotas(
    @RequestParam Long idEmpresa,
    @RequestParam(required = false) Long desde,
    @RequestParam(required = false) Long hasta,
    @RequestParam(required = false) Long idCliente,
    @RequestParam(required = false) Long idViajante,
    @RequestParam(required = false) Integer nroSerie,
    @RequestParam(required = false) Integer nroNota,
    @RequestParam(required = false) TipoDeComprobante tipoDeComprobante,
    @RequestParam(required = false) Long idUsuario,
    @RequestParam(required = false) Movimiento movimiento,
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
    BusquedaNotaCriteria criteria =
      BusquedaNotaCriteria.builder()
        .idEmpresa(idEmpresa)
        .buscaPorFecha((desde != null) && (hasta != null))
        .fechaDesde(fechaDesde.getTime())
        .fechaHasta(fechaHasta.getTime())
        .movimiento(movimiento)
        .buscaCliente(movimiento.equals(Movimiento.VENTA) && idCliente != null)
        .idCliente(idCliente)
        .buscaViajante(movimiento.equals(Movimiento.VENTA) && idViajante != null)
        .idViajante(idViajante)
        .buscaUsuario(idUsuario != null)
        .idUsuario(idUsuario)
        .buscaPorNumeroNota((nroSerie != null) && (nroNota != null))
        .numSerie((nroSerie != null) ? nroSerie : 0)
        .numNota((nroNota != null) ? nroNota : 0)
        .buscaPorTipoComprobante(tipoDeComprobante != null)
        .tipoComprobante(tipoDeComprobante)
        .pageable(this.getPageable(pagina, ordenarPor, sentido))
        .build();
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    return notaService.buscarNotas(criteria, (int) claims.get("idUsuario"));
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

  @GetMapping("/notas/tipos/empresas/{idEmpresa}")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR})
  public TipoDeComprobante[] getTipoNotaCreditoEmpresa(@PathVariable long idEmpresa) {
    return notaService.getTiposNota(empresaService.getEmpresaPorId(idEmpresa));
  }

  @GetMapping("/notas/{idNota}/facturas")
  @AccesoRolesPermitidos({
    Rol.ADMINISTRADOR,
    Rol.ENCARGADO,
    Rol.VENDEDOR,
    Rol.VIAJANTE,
    Rol.COMPRADOR
  })
  public Factura getFacturaNotaCredito(@PathVariable long idNota) {
    return notaService.getFacturaDeLaNotaCredito(idNota);
  }

  @GetMapping("/notas/debito/recibo/{idRecibo}/existe")
  @AccesoRolesPermitidos({
    Rol.ADMINISTRADOR,
    Rol.ENCARGADO,
    Rol.VENDEDOR,
    Rol.VIAJANTE,
    Rol.COMPRADOR
  })
  public boolean existeNotaDebitoRecibo(@PathVariable long idRecibo) {
    return notaService.existsNotaDebitoPorRecibo(reciboService.getReciboNoEliminadoPorId(idRecibo));
  }

  @GetMapping("/notas/clientes/tipos/credito")
  @AccesoRolesPermitidos({
    Rol.ADMINISTRADOR,
    Rol.ENCARGADO,
    Rol.VENDEDOR,
    Rol.VIAJANTE,
    Rol.COMPRADOR
  })
  public List<TipoDeComprobante> getTipoNotaCreditoCliente(
      @RequestParam long idCliente, @RequestParam long idEmpresa) {
    return notaService.getTipoNotaCreditoCliente(idCliente, idEmpresa);
  }

  @GetMapping("/notas/clientes/tipos/debito")
  @AccesoRolesPermitidos({
    Rol.ADMINISTRADOR,
    Rol.ENCARGADO,
    Rol.VENDEDOR,
    Rol.VIAJANTE,
    Rol.COMPRADOR
  })
  public List<TipoDeComprobante> getTipoNotaDebitoCliente(
      @RequestParam long idCliente, @RequestParam long idEmpresa) {
    return notaService.getTipoNotaDebitoCliente(idCliente, idEmpresa);
  }

  @GetMapping("/notas/proveedores/tipos/credito")
  @AccesoRolesPermitidos({
    Rol.ADMINISTRADOR,
    Rol.ENCARGADO,
    Rol.VENDEDOR,
    Rol.VIAJANTE,
    Rol.COMPRADOR
  })
  public List<TipoDeComprobante> getTipoNotaCreditoProveedor(
    @RequestParam long idProveedor, @RequestParam long idEmpresa) {
    return notaService.getTipoNotaCreditoProveedor(idProveedor, idEmpresa);
  }

  @GetMapping("/notas/proveedores/tipos/debito")
  @AccesoRolesPermitidos({
    Rol.ADMINISTRADOR,
    Rol.ENCARGADO,
    Rol.VENDEDOR,
    Rol.VIAJANTE,
    Rol.COMPRADOR
  })
  public List<TipoDeComprobante> getTipoNotaDebitoProveedor(
    @RequestParam long idProveedor, @RequestParam long idEmpresa) {
    return notaService.getTipoNotaDebitoProveedor(idProveedor, idEmpresa);
  }

  @GetMapping("/notas/renglones/credito/{idNotaCredito}")
  @AccesoRolesPermitidos({
    Rol.ADMINISTRADOR,
    Rol.ENCARGADO,
    Rol.VENDEDOR,
    Rol.VIAJANTE,
    Rol.COMPRADOR
  })
  public List<RenglonNotaCredito> getRenglonesDeNotaCreditoCliente(
      @PathVariable long idNotaCredito) {
    return notaService.getRenglonesDeNotaCredito(idNotaCredito);
  }

  @GetMapping("/notas/renglones/debito/{idNotaDebito}")
  @AccesoRolesPermitidos({
    Rol.ADMINISTRADOR,
    Rol.ENCARGADO,
    Rol.VENDEDOR,
    Rol.VIAJANTE,
    Rol.COMPRADOR
  })
  public List<RenglonNotaDebito> getRenglonesDeNotaDebitoCliente(@PathVariable long idNotaDebito) {
    return notaService.getRenglonesDeNotaDebito(idNotaDebito);
  }

  @PostMapping("/notas/credito/calculos")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO})
  public NotaCredito calcularNotaCreditoConFactura(
      @RequestBody NuevaNotaCreditoDeFacturaDTO nuevaNotaCreditoDeFacturaDTO,
      @RequestHeader("Authorization") String authorizationHeader) {
    NotaCredito notaCreditoNueva = new NotaCredito();
    Factura factura = facturaService.getFacturaNoEliminadaPorId(nuevaNotaCreditoDeFacturaDTO.getIdFactura());
    if (Arrays.asList(nuevaNotaCreditoDeFacturaDTO.getCantidades()).contains(null)
        || Arrays.asList(nuevaNotaCreditoDeFacturaDTO.getIdsRenglonesFactura()).contains(null)) {
      throw new BusinessServiceException(
          RESOURCE_BUNDLE.getString("mensaje_nota_de_renglones_vacio"));
    } else {
      notaCreditoNueva.setRenglonesNotaCredito(
          notaService.calcularRenglonCreditoProducto(
              notaService.getTipoDeNotaCreditoSegunFactura(factura.getTipoComprobante()),
              nuevaNotaCreditoDeFacturaDTO.getCantidades(),
              nuevaNotaCreditoDeFacturaDTO.getIdsRenglonesFactura()));
    }
    List<BigDecimal> importes = new ArrayList<>();
    List<BigDecimal> cantidades = new ArrayList<>();
    List<BigDecimal> ivaPorcentajeRenglones = new ArrayList<>();
    List<BigDecimal> ivaNetoRenglones = new ArrayList<>();
    notaCreditoNueva
        .getRenglonesNotaCredito()
        .forEach(
            r -> {
              importes.add(r.getImporteBruto());
              cantidades.add(r.getCantidad());
              ivaPorcentajeRenglones.add(r.getIvaPorcentaje());
              ivaNetoRenglones.add(r.getIvaNeto());
            });
    notaCreditoNueva.setSubTotal(
        notaService.calcularSubTotalCredito(
            importes.toArray(new BigDecimal[notaCreditoNueva.getRenglonesNotaCredito().size()])));
    notaCreditoNueva.setDescuentoPorcentaje(factura.getDescuentoPorcentaje());
    notaCreditoNueva.setDescuentoNeto(notaService.calcularDecuentoNetoCredito(notaCreditoNueva.getSubTotal(), notaCreditoNueva.getDescuentoPorcentaje()));
    notaCreditoNueva.setRecargoPorcentaje(factura.getRecargoPorcentaje());
    notaCreditoNueva.setRecargoNeto(notaService.calcularRecargoNetoCredito(notaCreditoNueva.getSubTotal(), notaCreditoNueva.getRecargoPorcentaje()));
    notaCreditoNueva.setTipoComprobante(notaService.getTipoDeNotaCreditoSegunFactura(factura.getTipoComprobante()));
    notaCreditoNueva.setIva105Neto(notaService.calcularIVANetoCredito(notaService.getTipoDeNotaCreditoSegunFactura(factura.getTipoComprobante()),
      cantidades.toArray(new BigDecimal[0]),
      ivaPorcentajeRenglones.toArray(new BigDecimal[0]),
      ivaNetoRenglones.toArray(new BigDecimal[0]), IVA_105, factura.getDescuentoPorcentaje(),
      factura.getRecargoPorcentaje()));
    notaCreditoNueva.setIva21Neto(notaService.calcularIVANetoCredito(notaService.getTipoDeNotaCreditoSegunFactura(factura.getTipoComprobante()),
      cantidades.toArray(new BigDecimal[0]),
      ivaPorcentajeRenglones.toArray(new BigDecimal[0]),
      ivaNetoRenglones.toArray(new BigDecimal[0]), IVA_21, factura.getDescuentoPorcentaje(),
      factura.getRecargoPorcentaje()));
    notaCreditoNueva.setSubTotalBruto(notaService.calcularSubTotalBrutoCredito(notaService.getTipoDeNotaCreditoSegunFactura(factura.getTipoComprobante()),
      notaCreditoNueva.getSubTotal(), notaCreditoNueva.getRecargoNeto(), notaCreditoNueva.getDescuentoNeto(),
      notaCreditoNueva.getIva105Neto(), notaCreditoNueva.getIva21Neto()));
    notaCreditoNueva.setTotal(notaService.calcularTotalCredito(notaCreditoNueva.getSubTotalBruto(), notaCreditoNueva.getIva105Neto(), notaCreditoNueva.getIva21Neto()));
    notaCreditoNueva.setFecha(new Date());
    if (factura instanceof FacturaVenta) {
      notaCreditoNueva.setCliente(clienteService.getClienteNoEliminadoPorId(((FacturaVenta)factura).getIdCliente()));
      notaCreditoNueva.setFacturaVenta((FacturaVenta)factura);
    } else if (factura instanceof FacturaCompra) {
      notaCreditoNueva.setProveedor(proveedorService.getProveedorNoEliminadoPorId(((FacturaCompra)factura).getIdProveedor()));
      notaCreditoNueva.setFacturaCompra((FacturaCompra) factura);
    }
    notaCreditoNueva.setEmpresa(factura.getEmpresa());
    notaCreditoNueva.setModificaStock(nuevaNotaCreditoDeFacturaDTO.isModificaStock());
    notaCreditoNueva.setMotivo(nuevaNotaCreditoDeFacturaDTO.getMotivo());
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    notaCreditoNueva.setUsuario(usuarioService.getUsuarioNoEliminadoPorId(((Integer) claims.get("idUsuario")).longValue()));
    return notaCreditoNueva;
  }

  @PostMapping("/notas/credito/calculos-sin-factura")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO})
  public NotaCredito calcularNotaCreditoSinFactura(
      @RequestBody NuevaNotaCreditoSinFacturaDTO nuevaNotaCreditoSinFacturaDTO,
      @RequestHeader("Authorization") String authorizationHeader) {
    NotaCredito notaCreditoNueva = new NotaCredito();
    if (nuevaNotaCreditoSinFacturaDTO.getDetalle() == null
        || nuevaNotaCreditoSinFacturaDTO.getDetalle().isEmpty()) {
      throw new BusinessServiceException(
          RESOURCE_BUNDLE.getString("mensaje_nota_renglon_sin_descripcion"));
    }
    List<RenglonNotaCredito> renglones = new ArrayList<>();
    notaCreditoNueva.setTipoComprobante(nuevaNotaCreditoSinFacturaDTO.getTipo());
    renglones.add(
        notaService.calcularRenglonCredito(
            nuevaNotaCreditoSinFacturaDTO.getTipo(),
            nuevaNotaCreditoSinFacturaDTO.getDetalle(),
            nuevaNotaCreditoSinFacturaDTO.getMonto()));
    notaCreditoNueva.setRenglonesNotaCredito(renglones);
    if (notaCreditoNueva.getTipoComprobante() == TipoDeComprobante.NOTA_CREDITO_A
        || notaCreditoNueva.getTipoComprobante() == TipoDeComprobante.NOTA_CREDITO_B
      || notaCreditoNueva.getTipoComprobante() == TipoDeComprobante.NOTA_CREDITO_C
        || notaCreditoNueva.getTipoComprobante() == TipoDeComprobante.NOTA_CREDITO_Y
        || notaCreditoNueva.getTipoComprobante() == TipoDeComprobante.NOTA_CREDITO_PRESUPUESTO) {
      notaCreditoNueva.setSubTotal(
          notaCreditoNueva.getRenglonesNotaCredito().get(0).getImporteBruto());
    } else if (notaCreditoNueva.getTipoComprobante() == TipoDeComprobante.NOTA_CREDITO_X) {
      notaCreditoNueva.setSubTotal(
          notaCreditoNueva.getRenglonesNotaCredito().get(0).getImporteNeto());
    }
    notaCreditoNueva.setDescuentoPorcentaje(BigDecimal.ZERO);
    notaCreditoNueva.setDescuentoNeto(BigDecimal.ZERO);
    notaCreditoNueva.setRecargoPorcentaje(BigDecimal.ZERO);
    notaCreditoNueva.setRecargoNeto(BigDecimal.ZERO);
    notaCreditoNueva.setIva105Neto(BigDecimal.ZERO);
    notaCreditoNueva.setIva21Neto(notaCreditoNueva.getRenglonesNotaCredito().get(0).getIvaNeto());
    BigDecimal subTotalBruto = notaCreditoNueva.getSubTotal();
    if (notaCreditoNueva.getTipoComprobante() == TipoDeComprobante.NOTA_CREDITO_B
        || notaCreditoNueva.getTipoComprobante() == TipoDeComprobante.NOTA_CREDITO_C
        || notaCreditoNueva.getTipoComprobante() == TipoDeComprobante.NOTA_CREDITO_PRESUPUESTO) {
      subTotalBruto = subTotalBruto.subtract(notaCreditoNueva.getIva21Neto());
    }
    notaCreditoNueva.setSubTotalBruto(subTotalBruto);
    notaCreditoNueva.setTotal(
        notaService.calcularTotalNota(notaCreditoNueva.getRenglonesNotaCredito()));
    notaCreditoNueva.setFecha(new Date());
    if ((nuevaNotaCreditoSinFacturaDTO.getIdCliente() != null
            && nuevaNotaCreditoSinFacturaDTO.getIdProveedor() != null)
        || (nuevaNotaCreditoSinFacturaDTO.getIdCliente() == null
            && nuevaNotaCreditoSinFacturaDTO.getIdProveedor() == null)) {
      throw new BusinessServiceException(RESOURCE_BUNDLE.getString("mensaje_nota_cliente_proveedor_juntos"));
    }
    if (nuevaNotaCreditoSinFacturaDTO.getIdCliente() != null) {
      notaCreditoNueva.setCliente(
          clienteService.getClienteNoEliminadoPorId(nuevaNotaCreditoSinFacturaDTO.getIdCliente()));
    }
    if (nuevaNotaCreditoSinFacturaDTO.getIdProveedor() != null) {
      notaCreditoNueva.setProveedor(
          proveedorService.getProveedorNoEliminadoPorId(nuevaNotaCreditoSinFacturaDTO.getIdProveedor()));
    }
    notaCreditoNueva.setEmpresa(
        empresaService.getEmpresaPorId(nuevaNotaCreditoSinFacturaDTO.getIdEmpresa()));
    notaCreditoNueva.setModificaStock(false);
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    notaCreditoNueva.setUsuario(
        usuarioService.getUsuarioNoEliminadoPorId(((Integer) claims.get("idUsuario")).longValue()));
    notaCreditoNueva.setMotivo(nuevaNotaCreditoSinFacturaDTO.getMotivo());
    return notaCreditoNueva;
  }

  @PostMapping("/notas/debito/calculos")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO})
  public NotaDebito calcularNotaDebitoDeRecibo(
      @RequestBody NuevaNotaDebitoDeReciboDTO nuevaNotaDebitoDeReciboDTO,
      @RequestHeader("Authorization") String authorizationHeader) {
    NotaDebito notaDebitoCalculada = new NotaDebito();
    notaDebitoCalculada.setFecha(new Date());
    Recibo reciboRelacionado =
        reciboService.getReciboNoEliminadoPorId(nuevaNotaDebitoDeReciboDTO.getIdRecibo());
    if (reciboRelacionado.getCliente() != null) {
      notaDebitoCalculada.setCliente(reciboRelacionado.getCliente());
      notaDebitoCalculada.setMovimiento(Movimiento.VENTA);
      if (!this.getTipoNotaDebitoCliente(
              reciboRelacionado.getIdCliente(), reciboRelacionado.getIdEmpresa())
          .contains(nuevaNotaDebitoDeReciboDTO.getTipoDeComprobante())) {
        throw new BusinessServiceException(
            RESOURCE_BUNDLE.getString("mensaje_nota_tipo_no_valido"));
      }
    } else if (reciboRelacionado.getProveedor() != null
        && nuevaNotaDebitoDeReciboDTO.getTipoDeComprobante() != null) {
      notaDebitoCalculada.setProveedor(reciboRelacionado.getProveedor());
      notaDebitoCalculada.setMovimiento(Movimiento.COMPRA);
      if (!this.getTipoNotaDebitoProveedor(
              reciboRelacionado.getIdProveedor(), reciboRelacionado.getIdEmpresa())
          .contains(nuevaNotaDebitoDeReciboDTO.getTipoDeComprobante())) {
        throw new BusinessServiceException(
            RESOURCE_BUNDLE.getString("mensaje_nota_tipo_no_valido"));
      }
    } else {
      throw new BusinessServiceException(
          RESOURCE_BUNDLE.getString("mensaje_nota_parametros_faltantes"));
    }
    notaDebitoCalculada.setTipoComprobante(nuevaNotaDebitoDeReciboDTO.getTipoDeComprobante());
    List<RenglonNotaDebito> renglones = new ArrayList<>();
    renglones.add(notaService.calcularRenglonDebitoConRecibo(reciboRelacionado));
    renglones.add(
        notaService.calcularRenglonDebito(
            nuevaNotaDebitoDeReciboDTO.getGastoAdministrativo(),
            nuevaNotaDebitoDeReciboDTO.getTipoDeComprobante()));
    notaDebitoCalculada.setRenglonesNotaDebito(renglones);
    notaDebitoCalculada.setMontoNoGravado(
      (notaDebitoCalculada.getTipoComprobante() == TipoDeComprobante.NOTA_DEBITO_C
        || notaDebitoCalculada.getTipoComprobante() == TipoDeComprobante.NOTA_DEBITO_X)
        ? BigDecimal.ZERO
        : reciboRelacionado.getMonto());
    notaDebitoCalculada.setIva21Neto(notaDebitoCalculada.getRenglonesNotaDebito().get(1).getIvaNeto());
    notaDebitoCalculada.setIva105Neto(BigDecimal.ZERO);
    notaDebitoCalculada.setMotivo(nuevaNotaDebitoDeReciboDTO.getMotivo());
    if (notaDebitoCalculada.getTipoComprobante() == TipoDeComprobante.NOTA_DEBITO_C
      || notaDebitoCalculada.getTipoComprobante() == TipoDeComprobante.NOTA_DEBITO_X) {
      notaDebitoCalculada.setSubTotalBruto(notaDebitoCalculada.getRenglonesNotaDebito().get(0).getImporteBruto()
        .add(notaDebitoCalculada.getRenglonesNotaDebito().get(1).getImporteBruto()));
    } else {
      notaDebitoCalculada.setSubTotalBruto(notaDebitoCalculada.getRenglonesNotaDebito().get(1).getImporteBruto());
    }
    notaDebitoCalculada.setTotal(
        notaService.calcularTotalDebito(
            notaDebitoCalculada.getSubTotalBruto(),
            notaDebitoCalculada.getIva21Neto(),
            notaDebitoCalculada.getMontoNoGravado()));
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    notaDebitoCalculada.setUsuario(
        usuarioService.getUsuarioNoEliminadoPorId(((Integer) claims.get("idUsuario")).longValue()));
    notaDebitoCalculada.setRecibo(reciboRelacionado);
    notaDebitoCalculada.setEmpresa(reciboRelacionado.getEmpresa());
    return notaDebitoCalculada;
  }

  @PostMapping("/notas/debito/calculos-sin-recibo")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO})
  public NotaDebito calcularNotaDebitoSinRecibo(
    @RequestBody NuevaNotaDebitoSinReciboDTO nuevaNotaDebitoSinReciboDTO,
    @RequestHeader("Authorization") String authorizationHeader) {
    NotaDebito notaDebitoCalculada = new NotaDebito();
    notaDebitoCalculada.setFecha(new Date());
    if (nuevaNotaDebitoSinReciboDTO.getTipoDeComprobante() != null) {
      if (nuevaNotaDebitoSinReciboDTO.getIdCliente() != null) {
        Cliente cliente =
            clienteService.getClienteNoEliminadoPorId(nuevaNotaDebitoSinReciboDTO.getIdCliente());
        notaDebitoCalculada.setCliente(cliente);
        notaDebitoCalculada.setMovimiento(Movimiento.VENTA);
        notaDebitoCalculada.setEmpresa(cliente.getEmpresa());
        if (!this.getTipoNotaDebitoCliente(
                nuevaNotaDebitoSinReciboDTO.getIdCliente(), cliente.getIdEmpresa())
            .contains(nuevaNotaDebitoSinReciboDTO.getTipoDeComprobante())) {
          throw new BusinessServiceException(
              RESOURCE_BUNDLE.getString("mensaje_nota_tipo_no_valido"));
        }
      } else if (nuevaNotaDebitoSinReciboDTO.getIdProveedor() != null) {
        Proveedor proveedor =
            proveedorService.getProveedorNoEliminadoPorId(
                nuevaNotaDebitoSinReciboDTO.getIdProveedor());
        notaDebitoCalculada.setProveedor(proveedor);
        notaDebitoCalculada.setMovimiento(Movimiento.COMPRA);
        notaDebitoCalculada.setEmpresa(proveedor.getEmpresa());
        if (!this.getTipoNotaDebitoProveedor(
                nuevaNotaDebitoSinReciboDTO.getIdProveedor(), proveedor.getIdEmpresa())
            .contains(nuevaNotaDebitoSinReciboDTO.getTipoDeComprobante())) {
          throw new BusinessServiceException(
              RESOURCE_BUNDLE.getString("mensaje_nota_tipo_no_valido"));
        }
      }
    } else {
      throw new BusinessServiceException(
          RESOURCE_BUNDLE.getString("mensaje_nota_parametros_faltantes"));
    }
    notaDebitoCalculada.setTipoComprobante(nuevaNotaDebitoSinReciboDTO.getTipoDeComprobante());
    notaDebitoCalculada.setIva105Neto(BigDecimal.ZERO);
    notaDebitoCalculada.setMontoNoGravado(BigDecimal.ZERO);
    notaDebitoCalculada.setMotivo(nuevaNotaDebitoSinReciboDTO.getMotivo());
    List<RenglonNotaDebito> renglones = new ArrayList<>();
    renglones.add(
        notaService.calcularRenglonDebito(
            nuevaNotaDebitoSinReciboDTO.getGastoAdministrativo(),
            nuevaNotaDebitoSinReciboDTO.getTipoDeComprobante()));
    notaDebitoCalculada.setRenglonesNotaDebito(renglones);
    notaDebitoCalculada.setIva21Neto(notaDebitoCalculada.getRenglonesNotaDebito().get(0).getIvaNeto());
    notaDebitoCalculada.setSubTotalBruto(notaDebitoCalculada.getRenglonesNotaDebito().get(0).getImporteBruto());
    notaDebitoCalculada.setTotal(
      notaService.calcularTotalDebito(
        notaDebitoCalculada.getSubTotalBruto(),
        notaDebitoCalculada.getIva21Neto(),
        notaDebitoCalculada.getMontoNoGravado()));
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    notaDebitoCalculada.setUsuario(
      usuarioService.getUsuarioNoEliminadoPorId(((Integer) claims.get("idUsuario")).longValue()));
    return notaDebitoCalculada;
  }

  @PostMapping("/notas/credito")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO})
  public NotaCredito guardarNotaCredito(
      @RequestBody NotaCreditoDTO notaCreditoDTO,
      @RequestHeader("Authorization") String authorizationHeader) {
    NotaCredito notaCredito = modelMapper.map(notaCreditoDTO, NotaCredito.class);
    notaCredito.setEmpresa(empresaService.getEmpresaPorId(notaCreditoDTO.getIdEmpresa()));
    if ((notaCreditoDTO.getIdCliente() != null && notaCreditoDTO.getIdProveedor() != null)
        || (notaCreditoDTO.getIdCliente() == null && notaCreditoDTO.getIdProveedor() == null)) {
      throw new BusinessServiceException(RESOURCE_BUNDLE.getString("mensaje_nota_cliente_proveedor_juntos"));
    }
    if (notaCreditoDTO.getIdCliente() != null) {
      notaCredito.setCliente(clienteService.getClienteNoEliminadoPorId(notaCreditoDTO.getIdCliente()));
      notaCredito.setMovimiento(Movimiento.VENTA);
      if (notaCreditoDTO.getIdFacturaVenta() != null) {
        notaCredito.setFacturaVenta(
            (FacturaVenta) facturaService.getFacturaNoEliminadaPorId(notaCreditoDTO.getIdFacturaVenta()));
      }
    }
    if (notaCreditoDTO.getIdProveedor() != null) {
      notaCredito.setProveedor(proveedorService.getProveedorNoEliminadoPorId(notaCreditoDTO.getIdProveedor()));
      notaCredito.setMovimiento(Movimiento.COMPRA);
      if (notaCreditoDTO.getIdFacturaCompra() != null) {
        notaCredito.setFacturaCompra(
            (FacturaCompra) facturaService.getFacturaNoEliminadaPorId(notaCreditoDTO.getIdFacturaCompra()));
      }
    }
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    notaCredito.setUsuario(
        usuarioService.getUsuarioNoEliminadoPorId(((Integer) claims.get("idUsuario")).longValue()));
    return notaService.guardarNotaCredito(notaCredito);
  }

  @PostMapping("/notas/debito")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO})
  public NotaDebito guardarNotaDebito(
      @RequestBody NotaDebitoDTO notaDebitoDTO,
      @RequestHeader("Authorization") String authorizationHeader) {
    NotaDebito notaDebito = modelMapper.map(notaDebitoDTO, NotaDebito.class);
    notaDebito.setEmpresa(empresaService.getEmpresaPorId(notaDebitoDTO.getIdEmpresa()));
    if (notaDebitoDTO.getIdCliente() != null) {
      notaDebito.setCliente(clienteService.getClienteNoEliminadoPorId(notaDebitoDTO.getIdCliente()));
      notaDebito.setMovimiento(Movimiento.VENTA);
    }
    if (notaDebitoDTO.getIdProveedor() != null) {
      notaDebito.setProveedor(
          proveedorService.getProveedorNoEliminadoPorId(notaDebitoDTO.getIdProveedor()));
      notaDebito.setMovimiento(Movimiento.COMPRA);
    }
    // recibo
    if (notaDebitoDTO.getIdRecibo() != null) {
      notaDebito.setRecibo(reciboService.getReciboNoEliminadoPorId(notaDebitoDTO.getIdRecibo()));
    }
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    notaDebito.setUsuario(
        usuarioService.getUsuarioNoEliminadoPorId(((Integer) claims.get("idUsuario")).longValue()));
    return notaService.guardarNotaDebito(notaDebito);
  }

  @GetMapping("/notas/{idNota}/reporte")
  @AccesoRolesPermitidos({
    Rol.ADMINISTRADOR,
    Rol.ENCARGADO,
    Rol.VENDEDOR,
    Rol.VIAJANTE,
    Rol.COMPRADOR
  })
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

  @GetMapping("/notas/total-credito/criteria")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR})
  public BigDecimal getTotalNotasCredito(
    @RequestParam Long idEmpresa,
    @RequestParam(required = false) Long desde,
    @RequestParam(required = false) Long hasta,
    @RequestParam(required = false) Long idCliente,
    @RequestParam(required = false) Integer nroSerie,
    @RequestParam(required = false) Integer nroNota,
    @RequestParam(required = false) TipoDeComprobante tipoDeComprobante,
    @RequestParam(required = false) Movimiento movimiento,
    @RequestParam(required = false) Long idUsuario,
    @RequestParam(required = false) Long idViajante,
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
    BusquedaNotaCriteria criteria =
      BusquedaNotaCriteria.builder()
        .idEmpresa(idEmpresa)
        .buscaPorFecha((desde != null) && (hasta != null))
        .fechaDesde(fechaDesde.getTime())
        .fechaHasta(fechaHasta.getTime())
        .movimiento(movimiento)
        .buscaCliente(idCliente != null)
        .idCliente(idCliente)
        .buscaUsuario(idUsuario != null)
        .idUsuario(idUsuario)
        .buscaViajante(idViajante != null)
        .idViajante(idViajante)
        .buscaPorNumeroNota((nroSerie != null) && (nroNota != null))
        .numSerie((nroSerie != null) ? nroSerie : 0)
        .numNota((nroNota != null) ? nroNota : 0)
        .buscaPorTipoComprobante(tipoDeComprobante != null)
        .tipoComprobante(tipoDeComprobante)
        .pageable(this.getPageable(pagina, ordenarPor, sentido))
        .build();
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    return notaService.calcularTotalCredito(criteria, (int) claims.get("idUsuario"));
  }

  @GetMapping("/notas/total-debito/criteria")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR})
  public BigDecimal getTotalNotasDebito(
    @RequestParam Long idEmpresa,
    @RequestParam(required = false) Long desde,
    @RequestParam(required = false) Long hasta,
    @RequestParam(required = false) Long idCliente,
    @RequestParam(required = false) Integer nroSerie,
    @RequestParam(required = false) Integer nroNota,
    @RequestParam(required = false) TipoDeComprobante tipoDeComprobante,
    @RequestParam(required = false) Movimiento movimiento,
    @RequestParam(required = false) Long idUsuario,
    @RequestParam(required = false) Long idViajante,
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
    BusquedaNotaCriteria criteria =
      BusquedaNotaCriteria.builder()
        .idEmpresa(idEmpresa)
        .buscaPorFecha((desde != null) && (hasta != null))
        .fechaDesde(fechaDesde.getTime())
        .fechaHasta(fechaHasta.getTime())
        .movimiento(movimiento)
        .buscaCliente(idCliente != null)
        .idCliente(idCliente)
        .buscaUsuario(idUsuario != null)
        .idUsuario(idUsuario)
        .buscaViajante(idViajante != null)
        .idViajante(idViajante)
        .buscaPorNumeroNota((nroSerie != null) && (nroNota != null))
        .numSerie((nroSerie != null) ? nroSerie : 0)
        .numNota((nroNota != null) ? nroNota : 0)
        .buscaPorTipoComprobante(tipoDeComprobante != null)
        .tipoComprobante(tipoDeComprobante)
        .pageable(this.getPageable(pagina, ordenarPor, sentido))
        .build();
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    return notaService.calcularTotalDebito(criteria, (int) claims.get("idUsuario"));
  }

  @GetMapping("/notas/total-iva-credito/criteria")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR})
  public BigDecimal getTotalIvaCredito(
    @RequestParam Long idEmpresa,
    @RequestParam(required = false) Long desde,
    @RequestParam(required = false) Long hasta,
    @RequestParam(required = false) Long idCliente,
    @RequestParam(required = false) Integer nroSerie,
    @RequestParam(required = false) Integer nroNota,
    @RequestParam(required = false) TipoDeComprobante tipoDeComprobante,
    @RequestParam(required = false) Movimiento movimiento,
    @RequestParam(required = false) Long idUsuario,
    @RequestParam(required = false) Long idViajante,
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
    BusquedaNotaCriteria criteria =
      BusquedaNotaCriteria.builder()
        .idEmpresa(idEmpresa)
        .buscaPorFecha((desde != null) && (hasta != null))
        .fechaDesde(fechaDesde.getTime())
        .fechaHasta(fechaHasta.getTime())
        .movimiento(movimiento)
        .buscaCliente(idCliente != null)
        .idCliente(idCliente)
        .buscaUsuario(idUsuario != null)
        .idUsuario(idUsuario)
        .buscaViajante(idViajante != null)
        .idViajante(idViajante)
        .buscaPorNumeroNota((nroSerie != null) && (nroNota != null))
        .numSerie((nroSerie != null) ? nroSerie : 0)
        .numNota((nroNota != null) ? nroNota : 0)
        .buscaPorTipoComprobante(tipoDeComprobante != null)
        .tipoComprobante(tipoDeComprobante)
        .pageable(this.getPageable(pagina, ordenarPor, sentido))
        .build();
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    return notaService.calcularTotalIVACredito(criteria, (int) claims.get("idUsuario"));
  }

  @GetMapping("/notas/total-iva-debito/criteria")
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR})
  public BigDecimal getTotalIvaDebito(
    @RequestParam Long idEmpresa,
    @RequestParam(required = false) Long desde,
    @RequestParam(required = false) Long hasta,
    @RequestParam(required = false) Long idCliente,
    @RequestParam(required = false) Integer nroSerie,
    @RequestParam(required = false) Integer nroNota,
    @RequestParam(required = false) TipoDeComprobante tipoDeComprobante,
    @RequestParam(required = false) Movimiento movimiento,
    @RequestParam(required = false) Long idUsuario,
    @RequestParam(required = false) Long idViajante,
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
    BusquedaNotaCriteria criteria =
      BusquedaNotaCriteria.builder()
        .idEmpresa(idEmpresa)
        .buscaPorFecha((desde != null) && (hasta != null))
        .fechaDesde(fechaDesde.getTime())
        .fechaHasta(fechaHasta.getTime())
        .movimiento(movimiento)
        .buscaCliente(idCliente != null)
        .idCliente(idCliente)
        .buscaUsuario(idUsuario != null)
        .idUsuario(idUsuario)
        .buscaViajante(idViajante != null)
        .idViajante(idViajante)
        .buscaPorNumeroNota((nroSerie != null) && (nroNota != null))
        .numSerie((nroSerie != null) ? nroSerie : 0)
        .numNota((nroNota != null) ? nroNota : 0)
        .buscaPorTipoComprobante(tipoDeComprobante != null)
        .tipoComprobante(tipoDeComprobante)
        .pageable(this.getPageable(pagina, ordenarPor, sentido))
        .build();
    Claims claims = authService.getClaimsDelToken(authorizationHeader);
    return notaService.calcularTotalIVADebito(criteria, (int) claims.get("idUsuario"));
  }
}
