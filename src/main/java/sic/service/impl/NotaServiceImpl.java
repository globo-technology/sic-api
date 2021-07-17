package sic.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import javax.imageio.ImageIO;
import javax.persistence.EntityNotFoundException;
import javax.swing.ImageIcon;

import com.querydsl.core.BooleanBuilder;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sic.modelo.*;
import sic.modelo.criteria.BusquedaNotaCriteria;
import sic.modelo.dto.NuevaNotaCreditoDeFacturaDTO;
import sic.modelo.dto.NuevaNotaCreditoSinFacturaDTO;
import sic.modelo.dto.NuevaNotaDebitoDeReciboDTO;
import sic.modelo.dto.NuevaNotaDebitoSinReciboDTO;
import sic.repository.NotaCreditoRepository;
import sic.repository.NotaDebitoRepository;
import sic.service.*;
import sic.repository.NotaRepository;
import sic.exception.BusinessServiceException;
import sic.exception.ServiceException;
import sic.util.CustomValidator;

@Service
public class NotaServiceImpl implements INotaService {

  private final NotaRepository<Nota> notaRepository;
  private final NotaCreditoRepository notaCreditoRepository;
  private final NotaDebitoRepository notaDebitoRepository;
  private final IFacturaService facturaService;
  private final INotaService notaService;
  private final IReciboService reciboService;
  private final IClienteService clienteService;
  private final IProveedorService proveedorService;
  private final ISucursalService sucursalService;
  private final IUsuarioService usuarioService;
  private final IProductoService productoService;
  private final ICuentaCorrienteService cuentaCorrienteService;
  private final IMercadoPagoService mercadoPagoService;
  private final IAfipService afipService;
  private static final BigDecimal IVA_21 = new BigDecimal("21");
  private static final BigDecimal IVA_105 = new BigDecimal("10.5");
  private static final BigDecimal CIEN = new BigDecimal("100");
  private static final int TAMANIO_PAGINA_DEFAULT = 25;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final MessageSource messageSource;
  private final CustomValidator customValidator;

  @Autowired
  @Lazy
  public NotaServiceImpl(
    NotaRepository<Nota> notaRepository,
    NotaCreditoRepository notaCreditoRepository,
    NotaDebitoRepository notaDebitoRepository,
    IFacturaService facturaService,
    INotaService notaService,
    IReciboService reciboService,
    IClienteService clienteService,
    IProveedorService proveedorService,
    IUsuarioService usuarioService,
    IProductoService productoService,
    ISucursalService sucursalService,
    ICuentaCorrienteService cuentaCorrienteService,
    IMercadoPagoService mercadoPagoService,
    IAfipService afipService,
    MessageSource messageSource,
    CustomValidator customValidator) {
    this.notaRepository = notaRepository;
    this.notaCreditoRepository = notaCreditoRepository;
    this.notaDebitoRepository = notaDebitoRepository;
    this.facturaService = facturaService;
    this.notaService = notaService;
    this.reciboService = reciboService;
    this.clienteService = clienteService;
    this.proveedorService = proveedorService;
    this.usuarioService = usuarioService;
    this.sucursalService = sucursalService;
    this.productoService = productoService;
    this.cuentaCorrienteService = cuentaCorrienteService;
    this.mercadoPagoService = mercadoPagoService;
    this.afipService = afipService;
    this.messageSource = messageSource;
    this.customValidator = customValidator;
  }

  @Override
  public Nota getNotaNoEliminadaPorId(long idNota) {
    Optional<Nota> nota = notaRepository.findById(idNota);
    if (nota.isPresent() && !nota.get().isEliminada()) {
      return nota.get();
    } else {
      throw new EntityNotFoundException(
          messageSource.getMessage("mensaje_nota_eliminada", null, Locale.getDefault()));
    }
  }

  @Override
  @Transactional
  public void eliminarNota(long idNota) {
    Nota nota = this.getNotaNoEliminadaPorId(idNota);
    if (nota.getMovimiento() == Movimiento.COMPRA && nota.getFacturaCompra() != null) {
      throw new BusinessServiceException(
          messageSource.getMessage(
              "mensaje_nota_compra_con_factura_asociada", null, Locale.getDefault()));
    }
    if (nota.getMovimiento() == Movimiento.VENTA) {
      if (nota.getCae() != 0L) {
        throw new BusinessServiceException(
            messageSource.getMessage("mensaje_eliminar_nota_aprobada", null, Locale.getDefault()));
      }
      if (nota instanceof NotaCredito) {
        NotaCredito nc = (NotaCredito) nota;
        if (nc.isModificaStock()) {
          this.actualizarStock(
              nc.getRenglonesNotaCredito(),
              nota.getIdSucursal(),
              TipoDeOperacion.ELIMINACION,
              nc.getMovimiento());
        }
      }
    }
    this.cuentaCorrienteService.asentarEnCuentaCorriente(nota, TipoDeOperacion.ELIMINACION);
    nota.setEliminada(true);
    notaRepository.save(nota);
    logger.warn("La Nota {} se eliminó correctamente.", nota);
  }

  @Override
  public boolean existsByFacturaVentaAndEliminada(FacturaVenta facturaVenta) {
    return notaCreditoRepository.existsByFacturaVentaAndEliminada(facturaVenta, false);
  }

  @Override
  public Page<NotaCredito> buscarNotasCredito(
    BusquedaNotaCriteria busquedaNotaCriteria, long idUsuarioLoggedIn) {
    return notaCreditoRepository.findAll(
        this.getBuilderNotaCredito(busquedaNotaCriteria, idUsuarioLoggedIn),
        this.getPageable(
            (busquedaNotaCriteria.getPagina() == null || busquedaNotaCriteria.getPagina() < 0)
                ? 0
                : busquedaNotaCriteria.getPagina(),
            busquedaNotaCriteria.getOrdenarPor(),
            busquedaNotaCriteria.getSentido()));
  }

  @Override
  public Page<NotaDebito> buscarNotasDebito(
      BusquedaNotaCriteria busquedaNotaCriteria, long idUsuarioLoggedIn) {
    return notaDebitoRepository.findAll(
        this.getBuilderNotaDebito(busquedaNotaCriteria, idUsuarioLoggedIn),
        this.getPageable(
            (busquedaNotaCriteria.getPagina() == null || busquedaNotaCriteria.getPagina() < 0)
                ? 0
                : busquedaNotaCriteria.getPagina(),
            busquedaNotaCriteria.getOrdenarPor(),
            busquedaNotaCriteria.getSentido()));
  }

  private Pageable getPageable(Integer pagina, String ordenarPor, String sentido) {
    if (pagina == null) pagina = 0;
    String ordenDefault = "fecha";
    if (ordenarPor == null || sentido == null) {
      return PageRequest.of(pagina, TAMANIO_PAGINA_DEFAULT, Sort.by(Sort.Direction.DESC, ordenDefault));
    } else {
      switch (sentido) {
        case "ASC":
          return PageRequest.of(pagina, TAMANIO_PAGINA_DEFAULT, Sort.by(Sort.Direction.ASC, ordenarPor));
        case "DESC":
          return PageRequest.of(pagina, TAMANIO_PAGINA_DEFAULT, Sort.by(Sort.Direction.DESC, ordenarPor));
        default:
          return PageRequest.of(pagina, TAMANIO_PAGINA_DEFAULT, Sort.by(Sort.Direction.DESC, ordenDefault));
      }
    }
  }

  private BooleanBuilder getBuilderNotaCredito(
      BusquedaNotaCriteria criteria, long idUsuarioLoggedIn) {
    QNotaCredito qNotaCredito = QNotaCredito.notaCredito;
    BooleanBuilder builder = new BooleanBuilder();
    builder.and(
        qNotaCredito
            .sucursal
            .idSucursal
            .eq(criteria.getIdSucursal())
            .and(qNotaCredito.eliminada.eq(false)));
    if (criteria.getMovimiento() == Movimiento.VENTA)
      builder.and(qNotaCredito.movimiento.eq(Movimiento.VENTA));
    if (criteria.getMovimiento() == Movimiento.COMPRA)
      builder.and(qNotaCredito.movimiento.eq(Movimiento.COMPRA));
    if (criteria.getFechaDesde() != null || criteria.getFechaHasta() != null) {
      this.establecerLimitesDeFechasDeCriteria(criteria);
      if (criteria.getFechaDesde() != null && criteria.getFechaHasta() != null) {
        builder.and(qNotaCredito.fecha.between(criteria.getFechaDesde(), criteria.getFechaHasta()));
      } else if (criteria.getFechaDesde() != null) {
        builder.and(qNotaCredito.fecha.after(criteria.getFechaDesde()));
      } else if (criteria.getFechaHasta() != null) {
        builder.and(qNotaCredito.fecha.before(criteria.getFechaHasta()));
      }
    }
    if (criteria.getIdUsuario() != null)
      builder.and(qNotaCredito.usuario.idUsuario.eq(criteria.getIdUsuario()));
    if (criteria.getIdCliente() != null)
      builder.and(qNotaCredito.cliente.idCliente.eq(criteria.getIdCliente()));
    if (criteria.getIdViajante() != null)
      builder.and(qNotaCredito.cliente.viajante.idUsuario.eq(criteria.getIdViajante()));
    if (criteria.getIdProveedor() != null)
      builder.and(qNotaCredito.proveedor.idProveedor.eq(criteria.getIdCliente()));
    if (criteria.getTipoComprobante() != null)
      builder.and(qNotaCredito.tipoComprobante.eq(criteria.getTipoComprobante()));
    if (criteria.getNumSerie() != null && criteria.getNumNota() != null)
      builder
          .and(qNotaCredito.serie.eq(criteria.getNumSerie()))
          .and(qNotaCredito.nroNota.eq(criteria.getNumNota()));
    Usuario usuarioLogueado = usuarioService.getUsuarioNoEliminadoPorId(idUsuarioLoggedIn);
    BooleanBuilder rsPredicate = new BooleanBuilder();
    if (!usuarioLogueado.getRoles().contains(Rol.ADMINISTRADOR)
        && !usuarioLogueado.getRoles().contains(Rol.VENDEDOR)
        && !usuarioLogueado.getRoles().contains(Rol.ENCARGADO)) {
      for (Rol rol : usuarioLogueado.getRoles()) {
        switch (rol) {
          case VIAJANTE:
            rsPredicate.or(qNotaCredito.cliente.viajante.eq(usuarioLogueado));
            break;
          case COMPRADOR:
            Cliente clienteRelacionado =
                clienteService.getClientePorIdUsuario(idUsuarioLoggedIn);
            if (clienteRelacionado != null) {
              rsPredicate.or(qNotaCredito.cliente.eq(clienteRelacionado));
            } else {
              rsPredicate.or(qNotaCredito.cliente.isNull());
            }
            break;
        }
      }
      builder.and(rsPredicate);
    }
    return builder;
  }

  private BooleanBuilder getBuilderNotaDebito(
      BusquedaNotaCriteria criteria, long idUsuarioLoggedIn) {
    QNotaDebito qNotaDebito = QNotaDebito.notaDebito;
    BooleanBuilder builder = new BooleanBuilder();
    builder.and(
        qNotaDebito
            .sucursal
            .idSucursal
            .eq(criteria.getIdSucursal())
            .and(qNotaDebito.eliminada.eq(false)));
    if (criteria.getMovimiento() == Movimiento.VENTA)
      builder.and(qNotaDebito.movimiento.eq(Movimiento.VENTA));
    if (criteria.getMovimiento() == Movimiento.COMPRA)
      builder.and(qNotaDebito.movimiento.eq(Movimiento.COMPRA));
    if (criteria.getFechaDesde() != null || criteria.getFechaHasta() != null) {
      this.establecerLimitesDeFechasDeCriteria(criteria);
      if (criteria.getFechaDesde() != null && criteria.getFechaHasta() != null) {
        builder.and(qNotaDebito.fecha.between(criteria.getFechaDesde(), criteria.getFechaHasta()));
      } else if (criteria.getFechaDesde() != null) {
        builder.and(qNotaDebito.fecha.after(criteria.getFechaDesde()));
      } else if (criteria.getFechaHasta() != null) {
        builder.and(qNotaDebito.fecha.before(criteria.getFechaHasta()));
      }
    }
    if (criteria.getIdUsuario() != null)
      builder.and(qNotaDebito.usuario.idUsuario.eq(criteria.getIdUsuario()));
    if (criteria.getIdCliente() != null)
      builder.and(qNotaDebito.cliente.idCliente.eq(criteria.getIdCliente()));
    if (criteria.getIdViajante() != null)
      builder.and(qNotaDebito.cliente.viajante.idUsuario.eq(criteria.getIdViajante()));
    if (criteria.getIdProveedor() != null)
      builder.and(qNotaDebito.proveedor.idProveedor.eq(criteria.getIdProveedor()));
    if (criteria.getTipoComprobante() != null)
      builder.and(qNotaDebito.tipoComprobante.eq(criteria.getTipoComprobante()));
    if (criteria.getNumNota() != null && criteria.getNumSerie() != null)
      builder
          .and(qNotaDebito.serie.eq(criteria.getNumSerie()))
          .and(qNotaDebito.nroNota.eq(criteria.getNumNota()));
    Usuario usuarioLogueado = usuarioService.getUsuarioNoEliminadoPorId(idUsuarioLoggedIn);
    BooleanBuilder rsPredicate = new BooleanBuilder();
    if (!usuarioLogueado.getRoles().contains(Rol.ADMINISTRADOR)
        && !usuarioLogueado.getRoles().contains(Rol.VENDEDOR)
        && !usuarioLogueado.getRoles().contains(Rol.ENCARGADO)) {
      for (Rol rol : usuarioLogueado.getRoles()) {
        switch (rol) {
          case VIAJANTE:
            rsPredicate.or(qNotaDebito.cliente.viajante.eq(usuarioLogueado));
            break;
          case COMPRADOR:
            Cliente clienteRelacionado =
                clienteService.getClientePorIdUsuario(idUsuarioLoggedIn);
            if (clienteRelacionado != null) {
              rsPredicate.or(qNotaDebito.cliente.eq(clienteRelacionado));
            } else {
              rsPredicate.or(qNotaDebito.cliente.isNull());
            }
            break;
        }
      }
      builder.and(rsPredicate);
    }
    return builder;
  }

  @Override
  public Factura getFacturaDeLaNotaCredito(Long idNota) {
    Optional<NotaCredito> nc = this.notaCreditoRepository.findById(idNota);
    return nc.map(notaCredito -> (notaCredito.getFacturaVenta() != null
      ? notaCredito.getFacturaVenta()
      : notaCredito.getFacturaCompra())).orElse(null);
  }

  @Override
  public boolean existsNotaDebitoPorRecibo(Recibo recibo) {
    return notaDebitoRepository.existsByReciboAndEliminada(recibo, false);
  }

  @Override
  public List<TipoDeComprobante> getTipoNotaCreditoCliente(Long idCliente, Long idSucursal) {
    List<TipoDeComprobante> tiposPermitidos = new ArrayList<>();
    Sucursal sucursal = sucursalService.getSucursalPorId(idSucursal);
    Cliente cliente = clienteService.getClienteNoEliminadoPorId(idCliente);
    if (CategoriaIVA.discriminaIVA(sucursal.getCategoriaIVA())
        && CategoriaIVA.discriminaIVA(cliente.getCategoriaIVA())) {
      tiposPermitidos.add(TipoDeComprobante.NOTA_CREDITO_A);
    } else if (CategoriaIVA.discriminaIVA(sucursal.getCategoriaIVA())
        && !CategoriaIVA.discriminaIVA(cliente.getCategoriaIVA())) {
      tiposPermitidos.add(TipoDeComprobante.NOTA_CREDITO_B);
    } else {
      tiposPermitidos.add(TipoDeComprobante.NOTA_CREDITO_C);
    }
    tiposPermitidos.add(TipoDeComprobante.NOTA_CREDITO_X);
    tiposPermitidos.add(TipoDeComprobante.NOTA_CREDITO_PRESUPUESTO);
    return tiposPermitidos;
  }

  @Override
  public List<TipoDeComprobante> getTipoNotaDebitoCliente(Long idCliente, Long idSucursal) {
    List<TipoDeComprobante> tiposPermitidos = new ArrayList<>();
    Sucursal sucursal = sucursalService.getSucursalPorId(idSucursal);
    Cliente cliente = clienteService.getClienteNoEliminadoPorId(idCliente);
    if (CategoriaIVA.discriminaIVA(sucursal.getCategoriaIVA())
        && CategoriaIVA.discriminaIVA(cliente.getCategoriaIVA())) {
      tiposPermitidos.add(TipoDeComprobante.NOTA_DEBITO_A);
    } else if (CategoriaIVA.discriminaIVA(sucursal.getCategoriaIVA())
        && !CategoriaIVA.discriminaIVA(cliente.getCategoriaIVA())) {
      tiposPermitidos.add(TipoDeComprobante.NOTA_DEBITO_B);
    } else {
      tiposPermitidos.add(TipoDeComprobante.NOTA_DEBITO_C);
    }
    tiposPermitidos.add(TipoDeComprobante.NOTA_DEBITO_X);
    tiposPermitidos.add(TipoDeComprobante.NOTA_DEBITO_PRESUPUESTO);
    return tiposPermitidos;
  }

  @Override
  public List<TipoDeComprobante> getTipoNotaCreditoProveedor(Long idProveedor, Long idSucursal) {
    List<TipoDeComprobante> tiposPermitidos = new ArrayList<>();
    Sucursal sucursal = sucursalService.getSucursalPorId(idSucursal);
    Proveedor proveedor = proveedorService.getProveedorNoEliminadoPorId(idProveedor);
    if (CategoriaIVA.discriminaIVA(sucursal.getCategoriaIVA())) {
      if (CategoriaIVA.discriminaIVA(proveedor.getCategoriaIVA())) {
        tiposPermitidos.add(TipoDeComprobante.NOTA_CREDITO_A);
      } else {
        tiposPermitidos.add(TipoDeComprobante.NOTA_CREDITO_C);
      }
    } else {
      if (CategoriaIVA.discriminaIVA(proveedor.getCategoriaIVA())) {
        tiposPermitidos.add(TipoDeComprobante.NOTA_CREDITO_B);
      } else {
        tiposPermitidos.add(TipoDeComprobante.NOTA_CREDITO_C);
      }
    }
    tiposPermitidos.add(TipoDeComprobante.NOTA_CREDITO_X);
    tiposPermitidos.add(TipoDeComprobante.NOTA_CREDITO_PRESUPUESTO);
    return tiposPermitidos;
  }

  @Override
  public List<TipoDeComprobante> getTipoNotaDebitoProveedor(Long idProveedor, Long idSucursal) {
    List<TipoDeComprobante> tiposPermitidos = new ArrayList<>();
    Sucursal sucursal = sucursalService.getSucursalPorId(idSucursal);
    Proveedor proveedor = proveedorService.getProveedorNoEliminadoPorId(idProveedor);
    if (CategoriaIVA.discriminaIVA(sucursal.getCategoriaIVA())) {
      if (CategoriaIVA.discriminaIVA(proveedor.getCategoriaIVA())) {
        tiposPermitidos.add(TipoDeComprobante.NOTA_DEBITO_A);
      } else {
        tiposPermitidos.add(TipoDeComprobante.NOTA_DEBITO_C);
      }
    } else {
      if (CategoriaIVA.discriminaIVA(proveedor.getCategoriaIVA())) {
        tiposPermitidos.add(TipoDeComprobante.NOTA_DEBITO_B);
      } else {
        tiposPermitidos.add(TipoDeComprobante.NOTA_DEBITO_C);
      }
    }
    tiposPermitidos.add(TipoDeComprobante.NOTA_DEBITO_X);
    tiposPermitidos.add(TipoDeComprobante.NOTA_DEBITO_PRESUPUESTO);
    return tiposPermitidos;
  }

  @Override
  public List<NotaCredito> getNotasCreditoPorFactura(Long idFactura) {
    List<NotaCredito> notasCredito = new ArrayList<>();
    Factura factura = facturaService.getFacturaNoEliminadaPorId(idFactura);
    if (factura instanceof FacturaVenta) {
      notasCredito =
          notaCreditoRepository.findAllByFacturaVentaAndEliminada((FacturaVenta) factura, false);
    } else if (factura instanceof FacturaCompra) {
      notasCredito =
          notaCreditoRepository.findAllByFacturaCompraAndEliminada((FacturaCompra) factura, false);
    }
    return notasCredito;
  }

  @Override
  public List<RenglonFactura> getRenglonesFacturaModificadosParaNotaCredito(long idFactura) {
    HashMap<Long, BigDecimal> listaCantidadesProductosUnificados = new HashMap<>();
    this.getNotasCreditoPorFactura(idFactura)
        .forEach(
            n ->
                n.getRenglonesNotaCredito()
                    .forEach(
                        rnc -> {
                          if (listaCantidadesProductosUnificados.containsKey(
                              rnc.getIdProductoItem())) {
                            listaCantidadesProductosUnificados.put(
                                rnc.getIdProductoItem(),
                                listaCantidadesProductosUnificados
                                    .get(rnc.getIdProductoItem())
                                    .add(rnc.getCantidad()));
                          } else {
                            listaCantidadesProductosUnificados.put(
                                rnc.getIdProductoItem(), rnc.getCantidad());
                          }
                        }));
    List<RenglonFactura> renglonesFactura = facturaService.getRenglonesDeLaFactura(idFactura);
    if (!listaCantidadesProductosUnificados.isEmpty()) {
      renglonesFactura.forEach(
          rf -> {
            if (listaCantidadesProductosUnificados.containsKey(rf.getIdProductoItem())) {
              rf.setCantidad(
                  rf.getCantidad()
                      .subtract(listaCantidadesProductosUnificados.get(rf.getIdProductoItem())));
            }
          });
    }
    return renglonesFactura;
  }

  @Override
  public long getSiguienteNumeroNotaDebitoCliente(
      Long idSucursal, TipoDeComprobante tipoDeComprobante) {
    Sucursal sucursal = sucursalService.getSucursalPorId(idSucursal);
    Long numeroNota =
        notaDebitoRepository.buscarMayorNumNotaDebitoClienteSegunTipo(
            tipoDeComprobante,
            sucursal.getConfiguracionSucursal().getNroPuntoDeVentaAfip(),
            idSucursal);
    return (numeroNota == null) ? 1 : numeroNota + 1;
  }

  @Override
  public long getSiguienteNumeroNotaCreditoCliente(
      Long idSucursal, TipoDeComprobante tipoDeComprobante) {
    Sucursal sucursal = sucursalService.getSucursalPorId(idSucursal);
    Long numeroNota =
        notaCreditoRepository.buscarMayorNumNotaCreditoClienteSegunTipo(
            tipoDeComprobante,
            sucursal.getConfiguracionSucursal().getNroPuntoDeVentaAfip(),
            idSucursal);
    return (numeroNota == null) ? 1 : numeroNota + 1;
  }

  @Override
  public TipoDeComprobante[] getTiposNotaCredito(Sucursal sucursal) {
    // cuando la Empresa discrimina IVA
    if (CategoriaIVA.discriminaIVA(sucursal.getCategoriaIVA())) {
      TipoDeComprobante[] tiposPermitidos = new TipoDeComprobante[5];
      tiposPermitidos[0] = TipoDeComprobante.NOTA_CREDITO_A;
      tiposPermitidos[1] = TipoDeComprobante.NOTA_CREDITO_B;
      tiposPermitidos[2] = TipoDeComprobante.NOTA_CREDITO_X;
      tiposPermitidos[3] = TipoDeComprobante.NOTA_CREDITO_Y;
      tiposPermitidos[4] = TipoDeComprobante.NOTA_CREDITO_PRESUPUESTO;
      return tiposPermitidos;
    } else {
      // cuando la Empresa NO discrimina IVA
      TipoDeComprobante[] tiposPermitidos = new TipoDeComprobante[4];
      tiposPermitidos[0] = TipoDeComprobante.NOTA_CREDITO_B;
      tiposPermitidos[1] = TipoDeComprobante.NOTA_CREDITO_X;
      tiposPermitidos[2] = TipoDeComprobante.NOTA_CREDITO_Y;
      tiposPermitidos[3] = TipoDeComprobante.NOTA_CREDITO_PRESUPUESTO;
      return tiposPermitidos;
    }
  }

  @Override
  public TipoDeComprobante[] getTiposNotaDebito(Sucursal sucursal) {
    // cuando la Empresa discrimina IVA
    if (CategoriaIVA.discriminaIVA(sucursal.getCategoriaIVA())) {
      TipoDeComprobante[] tiposPermitidos = new TipoDeComprobante[5];
      tiposPermitidos[0] = TipoDeComprobante.NOTA_DEBITO_A;
      tiposPermitidos[1] = TipoDeComprobante.NOTA_DEBITO_B;
      tiposPermitidos[2] = TipoDeComprobante.NOTA_DEBITO_X;
      tiposPermitidos[3] = TipoDeComprobante.NOTA_DEBITO_Y;
      tiposPermitidos[4] = TipoDeComprobante.NOTA_DEBITO_PRESUPUESTO;
      return tiposPermitidos;
    } else {
      // cuando la Empresa NO discrimina IVA
      TipoDeComprobante[] tiposPermitidos = new TipoDeComprobante[4];
      tiposPermitidos[0] = TipoDeComprobante.NOTA_DEBITO_B;
      tiposPermitidos[1] = TipoDeComprobante.NOTA_DEBITO_X;
      tiposPermitidos[2] = TipoDeComprobante.NOTA_DEBITO_Y;
      tiposPermitidos[3] = TipoDeComprobante.NOTA_DEBITO_PRESUPUESTO;
      return tiposPermitidos;
    }
  }

  @Override
  public List<RenglonNotaCredito> getRenglonesDeNotaCredito(Long idNota) {
    Optional<NotaCredito> nc = this.notaCreditoRepository.findById(idNota);
    if (nc.isPresent()) {
      return nc.get().getRenglonesNotaCredito();
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public List<RenglonNotaDebito> getRenglonesDeNotaDebito(long idNota) {
    Optional<NotaDebito> nd = this.notaDebitoRepository.findById(idNota);
    if (nd.isPresent()) {
      return nd.get().getRenglonesNotaDebito();
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public void validarReglasDeNegocio(Nota nota) {
    if (nota instanceof NotaCredito && nota.getMovimiento().equals(Movimiento.VENTA)) {
      if (nota.getFacturaVenta() != null
          && nota.getFecha().isBefore(nota.getFacturaVenta().getFecha())) {
        throw new BusinessServiceException(
            messageSource.getMessage("mensaje_nota_fecha_comprobante_relacionado_incorrecta", null, Locale.getDefault()));
      }
      if (nota.getCae() != 0L) {
        throw new BusinessServiceException(
            messageSource.getMessage("mensaje_nota_cliente_cae", null, Locale.getDefault()));
      }
    } else if (nota instanceof NotaCredito && nota.getMovimiento().equals(Movimiento.COMPRA)) {
      if (nota.getFacturaCompra() != null
          && nota.getFecha().isBefore(nota.getFacturaCompra().getFecha())) {
        throw new BusinessServiceException(
            messageSource.getMessage("mensaje_nota_fecha_comprobante_relacionado_incorrecta", null, Locale.getDefault()));
      }
      if (nota.getFecha().isAfter(LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(59))) {
        throw new BusinessServiceException(
            messageSource.getMessage("mensaje_nota_fecha_incorrecta", null, Locale.getDefault()));
      }
    }
    if (nota instanceof NotaCredito) {
      if (((NotaCredito) nota).getRenglonesNotaCredito() == null
          || ((NotaCredito) nota).getRenglonesNotaCredito().isEmpty()) {
        throw new BusinessServiceException(
            messageSource.getMessage("mensaje_nota_de_renglones_vacio", null, Locale.getDefault()));
      }
    } else {
      if (((NotaDebito) nota).getRenglonesNotaDebito() == null
          || ((NotaDebito) nota).getRenglonesNotaDebito().isEmpty()) {
        throw new BusinessServiceException(
            messageSource.getMessage("mensaje_nota_de_renglones_vacio", null, Locale.getDefault()));
      }
    }
  }

  @Override
  public void validarCalculosDebito(NotaDebito notaDebito) {
    // monto no gravado
    if ((notaDebito.getTipoComprobante() == TipoDeComprobante.NOTA_DEBITO_C
            || notaDebito.getTipoComprobante() == TipoDeComprobante.NOTA_DEBITO_X)
        && notaDebito.getMontoNoGravado().compareTo(BigDecimal.ZERO) != 0) {
      throw new BusinessServiceException(
          messageSource.getMessage(
              "mensaje_nota_c_x_monto_no_gravado_no_valido", null, Locale.getDefault()));
    } else if ((notaDebito.getTipoComprobante() != TipoDeComprobante.NOTA_DEBITO_C
        && notaDebito.getTipoComprobante() != TipoDeComprobante.NOTA_DEBITO_X)) {
      BigDecimal montoComprobante = BigDecimal.ZERO;
      if (notaDebito.getRecibo() != null) {
        montoComprobante = notaDebito.getRecibo().getMonto();
      }
      if (notaDebito.getMontoNoGravado().compareTo(montoComprobante) != 0) {
        throw new BusinessServiceException(
            messageSource.getMessage(
                "mensaje_nota_monto_no_gravado_no_valido", null, Locale.getDefault()));
      }
    }
    // iva 21
    BigDecimal iva21 = BigDecimal.ZERO;
    if (notaDebito.getTipoComprobante() == TipoDeComprobante.NOTA_DEBITO_A
        || notaDebito.getTipoComprobante() == TipoDeComprobante.NOTA_DEBITO_B
        || notaDebito.getTipoComprobante() == TipoDeComprobante.NOTA_DEBITO_Y
        || notaDebito.getTipoComprobante() == TipoDeComprobante.NOTA_DEBITO_PRESUPUESTO) {
      iva21 = notaDebito.getSubTotalBruto().multiply(new BigDecimal("0.21"));
      if (notaDebito.getIva21Neto().compareTo(iva21) != 0) {
        throw new BusinessServiceException(
            messageSource.getMessage("mensaje_nota_iva21_no_valido", null, Locale.getDefault()));
      }
    }
    if ((notaDebito.getTipoComprobante() == TipoDeComprobante.NOTA_DEBITO_C
            || notaDebito.getTipoComprobante() == TipoDeComprobante.NOTA_DEBITO_X)
        && notaDebito.getIva21Neto().compareTo(BigDecimal.ZERO) != 0) {
      throw new BusinessServiceException(
          messageSource.getMessage("mensaje_nota_iva21_no_valido", null, Locale.getDefault()));
    }
    // total
    if ((notaDebito.getTipoComprobante() == TipoDeComprobante.NOTA_DEBITO_C
        && notaDebito.getSubTotalBruto().compareTo(notaDebito.getTotal()) != 0)) {
      throw new BusinessServiceException(
          messageSource.getMessage("mensaje_nota_total_no_valido", null, Locale.getDefault()));
    }
    if (notaDebito.getTipoComprobante() != TipoDeComprobante.NOTA_DEBITO_C
        && notaDebito.getTipoComprobante() != TipoDeComprobante.NOTA_DEBITO_X
        && notaDebito.getRecibo() != null
        && (notaDebito
                .getTotal()
                .compareTo(
                    this.calcularTotalDebito(
                        notaDebito.getSubTotalBruto(), iva21, notaDebito.getRecibo().getMonto()))
            != 0)) {
      throw new BusinessServiceException(
          messageSource.getMessage("mensaje_nota_total_no_valido", null, Locale.getDefault()));
    }
  }

  @Override
  @Transactional
  public NotaCredito guardarNotaCredito(NotaCredito notaCredito) {
    customValidator.validar(notaCredito);
    if (notaCredito.getFecha() == null) {
      notaCredito.setFecha(LocalDateTime.now());
    }
    this.validarReglasDeNegocio(notaCredito);
    if (notaCredito.getMovimiento().equals(Movimiento.VENTA)) {
      if (notaCredito.getFacturaVenta() != null) {
        notaCredito.setTipoComprobante(
            this.getTipoDeNotaCreditoSegunFactura(
                notaCredito.getFacturaVenta().getTipoComprobante()));
      } else {
        if (!this.getTipoNotaCreditoCliente(
                notaCredito.getCliente().getIdCliente(), notaCredito.getSucursal().getIdSucursal())
            .contains(notaCredito.getTipoComprobante())) {
          throw new BusinessServiceException(
              messageSource.getMessage("mensaje_nota_tipo_no_valido", null, Locale.getDefault()));
        }
      }
      notaCredito.setSerie(
          notaCredito.getSucursal().getConfiguracionSucursal().getNroPuntoDeVentaAfip());
      notaCredito.setNroNota(
          this.getSiguienteNumeroNotaCreditoCliente(
              notaCredito.getIdSucursal(), notaCredito.getTipoComprobante()));
    } else if (notaCredito.getMovimiento().equals(Movimiento.COMPRA)
        && notaCredito.getFacturaCompra() != null) {
      notaCredito.setTipoComprobante(
          this.getTipoDeNotaCreditoSegunFactura(
              notaCredito.getFacturaCompra().getTipoComprobante()));
    } else {
      if (!this.getTipoNotaCreditoProveedor(
              notaCredito.getProveedor().getIdProveedor(),
              notaCredito.getSucursal().getIdSucursal())
          .contains(notaCredito.getTipoComprobante())) {
        throw new BusinessServiceException(
            messageSource.getMessage("mensaje_nota_tipo_no_valido", null, Locale.getDefault()));
      }
    }
    if (notaCredito.isModificaStock()) {
      this.actualizarStock(
          notaCredito.getRenglonesNotaCredito(),
          notaCredito.getIdSucursal(),
          TipoDeOperacion.ALTA,
          notaCredito.getMovimiento());
    }
    notaCredito = notaCreditoRepository.save(notaCredito);
    this.cuentaCorrienteService.asentarEnCuentaCorriente(notaCredito, TipoDeOperacion.ALTA);
    logger.warn("La Nota {} se guardó correctamente.", notaCredito);
    return notaCredito;
  }

  @Override
  public NotaCredito calcularNotaCreditoConFactura(
      NuevaNotaCreditoDeFacturaDTO nuevaNotaCreditoDeFacturaDTO, Usuario usuario) {
    NotaCredito notaCreditoNueva = new NotaCredito();
    Factura factura =
        facturaService.getFacturaNoEliminadaPorId(nuevaNotaCreditoDeFacturaDTO.getIdFactura());
    if (Arrays.asList(nuevaNotaCreditoDeFacturaDTO.getCantidades()).contains(null)
        || Arrays.asList(nuevaNotaCreditoDeFacturaDTO.getIdsRenglonesFactura()).contains(null)) {
      throw new BusinessServiceException(
          messageSource.getMessage("mensaje_nota_de_renglones_vacio", null, Locale.getDefault()));
    } else {
      notaCreditoNueva.setRenglonesNotaCredito(
          notaService.calcularRenglonesCreditoProducto(
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
    notaCreditoNueva.setDescuentoNeto(
        notaService.calcularDecuentoNetoCredito(
            notaCreditoNueva.getSubTotal(), notaCreditoNueva.getDescuentoPorcentaje()));
    notaCreditoNueva.setRecargoPorcentaje(factura.getRecargoPorcentaje());
    notaCreditoNueva.setRecargoNeto(
        notaService.calcularRecargoNetoCredito(
            notaCreditoNueva.getSubTotal(), notaCreditoNueva.getRecargoPorcentaje()));
    notaCreditoNueva.setTipoComprobante(
        notaService.getTipoDeNotaCreditoSegunFactura(factura.getTipoComprobante()));
    notaCreditoNueva.setIva105Neto(
        notaService.calcularIVANetoCredito(
            notaService.getTipoDeNotaCreditoSegunFactura(factura.getTipoComprobante()),
            cantidades.toArray(new BigDecimal[0]),
            ivaPorcentajeRenglones.toArray(new BigDecimal[0]),
            ivaNetoRenglones.toArray(new BigDecimal[0]),
            IVA_105,
            factura.getDescuentoPorcentaje(),
            factura.getRecargoPorcentaje()));
    notaCreditoNueva.setIva21Neto(
        notaService.calcularIVANetoCredito(
            notaService.getTipoDeNotaCreditoSegunFactura(factura.getTipoComprobante()),
            cantidades.toArray(new BigDecimal[0]),
            ivaPorcentajeRenglones.toArray(new BigDecimal[0]),
            ivaNetoRenglones.toArray(new BigDecimal[0]),
            IVA_21,
            factura.getDescuentoPorcentaje(),
            factura.getRecargoPorcentaje()));
    notaCreditoNueva.setSubTotalBruto(
        notaService.calcularSubTotalBrutoCredito(
            notaService.getTipoDeNotaCreditoSegunFactura(factura.getTipoComprobante()),
            notaCreditoNueva.getSubTotal(),
            notaCreditoNueva.getRecargoNeto(),
            notaCreditoNueva.getDescuentoNeto(),
            notaCreditoNueva.getIva105Neto(),
            notaCreditoNueva.getIva21Neto()));
    notaCreditoNueva.setTotal(
        notaService.calcularTotalCredito(
            notaCreditoNueva.getSubTotalBruto(),
            notaCreditoNueva.getIva105Neto(),
            notaCreditoNueva.getIva21Neto()));
    notaCreditoNueva.setFecha(LocalDateTime.now());
    if (factura instanceof FacturaVenta) {
      notaCreditoNueva.setCliente(
          clienteService.getClienteNoEliminadoPorId(((FacturaVenta) factura).getIdCliente()));
      notaCreditoNueva.setFacturaVenta((FacturaVenta) factura);
      notaCreditoNueva.setMovimiento(Movimiento.VENTA);
    } else if (factura instanceof FacturaCompra) {
      notaCreditoNueva.setProveedor(
          proveedorService.getProveedorNoEliminadoPorId(
              ((FacturaCompra) factura).getIdProveedor()));
      notaCreditoNueva.setFacturaCompra((FacturaCompra) factura);
      notaCreditoNueva.setMovimiento(Movimiento.COMPRA);
    }
    notaCreditoNueva.setSucursal(factura.getSucursal());
    notaCreditoNueva.setModificaStock(nuevaNotaCreditoDeFacturaDTO.isModificaStock());
    notaCreditoNueva.setMotivo(nuevaNotaCreditoDeFacturaDTO.getMotivo());
    notaCreditoNueva.setUsuario(usuario);
    return notaCreditoNueva;
  }

  @Override
  public NotaCredito calcularNotaCreditoSinFactura(
      NuevaNotaCreditoSinFacturaDTO nuevaNotaCreditoSinFacturaDTO, Usuario usuario) {
    NotaCredito notaCreditoNueva = new NotaCredito();
    if (nuevaNotaCreditoSinFacturaDTO.getDetalle() == null
        || nuevaNotaCreditoSinFacturaDTO.getDetalle().isEmpty()) {
      throw new BusinessServiceException(
          messageSource.getMessage(
              "mensaje_nota_renglon_sin_descripcion", null, Locale.getDefault()));
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
    notaCreditoNueva.setFecha(LocalDateTime.now());
    if ((nuevaNotaCreditoSinFacturaDTO.getIdCliente() != null
            && nuevaNotaCreditoSinFacturaDTO.getIdProveedor() != null)
        || (nuevaNotaCreditoSinFacturaDTO.getIdCliente() == null
            && nuevaNotaCreditoSinFacturaDTO.getIdProveedor() == null)) {
      throw new BusinessServiceException(
          messageSource.getMessage(
              "mensaje_nota_cliente_proveedor_juntos", null, Locale.getDefault()));
    }
    if (nuevaNotaCreditoSinFacturaDTO.getIdCliente() != null) {
      notaCreditoNueva.setCliente(
          clienteService.getClienteNoEliminadoPorId(nuevaNotaCreditoSinFacturaDTO.getIdCliente()));
      notaCreditoNueva.setMovimiento(Movimiento.VENTA);
    }
    if (nuevaNotaCreditoSinFacturaDTO.getIdProveedor() != null) {
      notaCreditoNueva.setProveedor(
          proveedorService.getProveedorNoEliminadoPorId(
              nuevaNotaCreditoSinFacturaDTO.getIdProveedor()));
      notaCreditoNueva.setMovimiento(Movimiento.COMPRA);
    }
    notaCreditoNueva.setSucursal(
        sucursalService.getSucursalPorId(nuevaNotaCreditoSinFacturaDTO.getIdSucursal()));
    notaCreditoNueva.setModificaStock(false);
    notaCreditoNueva.setUsuario(usuario);
    notaCreditoNueva.setMotivo(nuevaNotaCreditoSinFacturaDTO.getMotivo());
    return notaCreditoNueva;
  }

  @Override
  public NotaDebito calcularNotaDebitoConRecibo(
      NuevaNotaDebitoDeReciboDTO nuevaNotaDebitoDeReciboDTO, Usuario usuario) {
    NotaDebito notaDebitoCalculada = new NotaDebito();
    notaDebitoCalculada.setFecha(LocalDateTime.now());
    Recibo reciboRelacionado =
        reciboService.getReciboNoEliminadoPorId(nuevaNotaDebitoDeReciboDTO.getIdRecibo());
    if (reciboRelacionado.getCliente() != null) {
      notaDebitoCalculada.setCliente(reciboRelacionado.getCliente());
      notaDebitoCalculada.setMovimiento(Movimiento.VENTA);
      if (!notaService
          .getTipoNotaDebitoCliente(
              reciboRelacionado.getIdCliente(), reciboRelacionado.getIdSucursal())
          .contains(nuevaNotaDebitoDeReciboDTO.getTipoDeComprobante())) {
        throw new BusinessServiceException(
            messageSource.getMessage("mensaje_nota_tipo_no_valido", null, Locale.getDefault()));
      }
    } else if (reciboRelacionado.getProveedor() != null
        && nuevaNotaDebitoDeReciboDTO.getTipoDeComprobante() != null) {
      notaDebitoCalculada.setProveedor(reciboRelacionado.getProveedor());
      notaDebitoCalculada.setMovimiento(Movimiento.COMPRA);
      if (!notaService
          .getTipoNotaDebitoProveedor(
              reciboRelacionado.getIdProveedor(), reciboRelacionado.getIdSucursal())
          .contains(nuevaNotaDebitoDeReciboDTO.getTipoDeComprobante())) {
        throw new BusinessServiceException(
            messageSource.getMessage("mensaje_nota_tipo_no_valido", null, Locale.getDefault()));
      }
    } else {
      throw new BusinessServiceException(
          messageSource.getMessage("mensaje_nota_parametros_faltantes", null, Locale.getDefault()));
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
    notaDebitoCalculada.setIva21Neto(
        notaDebitoCalculada.getRenglonesNotaDebito().get(1).getIvaNeto());
    notaDebitoCalculada.setIva105Neto(BigDecimal.ZERO);
    notaDebitoCalculada.setMotivo(nuevaNotaDebitoDeReciboDTO.getMotivo());
    if (notaDebitoCalculada.getTipoComprobante() == TipoDeComprobante.NOTA_DEBITO_C
        || notaDebitoCalculada.getTipoComprobante() == TipoDeComprobante.NOTA_DEBITO_X) {
      notaDebitoCalculada.setSubTotalBruto(
          notaDebitoCalculada
              .getRenglonesNotaDebito()
              .get(0)
              .getImporteBruto()
              .add(notaDebitoCalculada.getRenglonesNotaDebito().get(1).getImporteBruto()));
    } else {
      notaDebitoCalculada.setSubTotalBruto(
          notaDebitoCalculada.getRenglonesNotaDebito().get(1).getImporteBruto());
    }
    notaDebitoCalculada.setTotal(
        notaService.calcularTotalDebito(
            notaDebitoCalculada.getSubTotalBruto(),
            notaDebitoCalculada.getIva21Neto(),
            notaDebitoCalculada.getMontoNoGravado()));
    notaDebitoCalculada.setUsuario(usuario);
    notaDebitoCalculada.setRecibo(reciboRelacionado);
    notaDebitoCalculada.setSucursal(reciboRelacionado.getSucursal());
    return notaDebitoCalculada;
  }

  @Override
  public NotaDebito calcularNotaDebitoSinRecibo(
      NuevaNotaDebitoSinReciboDTO nuevaNotaDebitoSinReciboDTO, Usuario usuario) {
    NotaDebito notaDebitoCalculada = new NotaDebito();
    notaDebitoCalculada.setFecha(LocalDateTime.now());
    notaDebitoCalculada.setSucursal(sucursalService.getSucursalPorId(nuevaNotaDebitoSinReciboDTO.getIdSucursal()));
    if (nuevaNotaDebitoSinReciboDTO.getTipoDeComprobante() != null) {
      if (nuevaNotaDebitoSinReciboDTO.getIdCliente() != null) {
        Cliente cliente =
            clienteService.getClienteNoEliminadoPorId(nuevaNotaDebitoSinReciboDTO.getIdCliente());
        notaDebitoCalculada.setCliente(cliente);
        notaDebitoCalculada.setMovimiento(Movimiento.VENTA);
        if (!this.getTipoNotaDebitoCliente(
                nuevaNotaDebitoSinReciboDTO.getIdCliente(), nuevaNotaDebitoSinReciboDTO.getIdSucursal())
            .contains(nuevaNotaDebitoSinReciboDTO.getTipoDeComprobante())) {
          throw new BusinessServiceException(
              messageSource.getMessage("mensaje_nota_tipo_no_valido", null, Locale.getDefault()));
        }
      } else if (nuevaNotaDebitoSinReciboDTO.getIdProveedor() != null) {
        Proveedor proveedor =
            proveedorService.getProveedorNoEliminadoPorId(
                nuevaNotaDebitoSinReciboDTO.getIdProveedor());
        notaDebitoCalculada.setProveedor(proveedor);
        notaDebitoCalculada.setMovimiento(Movimiento.COMPRA);
        if (!this.getTipoNotaDebitoProveedor(
                nuevaNotaDebitoSinReciboDTO.getIdProveedor(), nuevaNotaDebitoSinReciboDTO.getIdSucursal())
            .contains(nuevaNotaDebitoSinReciboDTO.getTipoDeComprobante())) {
          throw new BusinessServiceException(
              messageSource.getMessage("mensaje_nota_tipo_no_valido", null, Locale.getDefault()));
        }
      }
    } else {
      throw new BusinessServiceException(
          messageSource.getMessage("mensaje_nota_parametros_faltantes", null, Locale.getDefault()));
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
    notaDebitoCalculada.setIva21Neto(
        notaDebitoCalculada.getRenglonesNotaDebito().get(0).getIvaNeto());
    notaDebitoCalculada.setSubTotalBruto(
        notaDebitoCalculada.getRenglonesNotaDebito().get(0).getImporteBruto());
    notaDebitoCalculada.setTotal(
        notaService.calcularTotalDebito(
            notaDebitoCalculada.getSubTotalBruto(),
            notaDebitoCalculada.getIva21Neto(),
            notaDebitoCalculada.getMontoNoGravado()));
    notaDebitoCalculada.setUsuario(usuario);
    return notaDebitoCalculada;
  }

  @Override
  @Transactional
  public NotaDebito guardarNotaDebito(NotaDebito notaDebito) {
    //customValidator.validar(notaDebito);
    if (notaDebito.getFecha() == null) {
      notaDebito.setFecha(LocalDateTime.now());
    }
    //this.validarReglasDeNegocio(notaDebito);
    if (notaDebito.getMovimiento().equals(Movimiento.VENTA)) {
      if (!this.getTipoNotaDebitoCliente(
              notaDebito.getCliente().getIdCliente(), notaDebito.getSucursal().getIdSucursal())
          .contains(notaDebito.getTipoComprobante())) {
        throw new BusinessServiceException(
            messageSource.getMessage("mensaje_nota_tipo_no_valido", null, Locale.getDefault()));
      }
      notaDebito.setSerie(
          notaDebito.getSucursal().getConfiguracionSucursal().getNroPuntoDeVentaAfip());
      notaDebito.setNroNota(
          this.getSiguienteNumeroNotaDebitoCliente(
              notaDebito.getIdSucursal(), notaDebito.getTipoComprobante()));
    } else if (notaDebito.getMovimiento().equals(Movimiento.COMPRA)
        && !this.getTipoNotaDebitoProveedor(
                notaDebito.getProveedor().getIdProveedor(),
                notaDebito.getSucursal().getIdSucursal())
            .contains(notaDebito.getTipoComprobante())) {
      throw new BusinessServiceException(
          messageSource.getMessage("mensaje_nota_tipo_no_valido", null, Locale.getDefault()));
    }
    //this.validarCalculosDebito(notaDebito);
    notaDebito = notaDebitoRepository.save(notaDebito);
    if (notaDebito.getRecibo() != null
        && notaDebito.getRecibo().getIdPagoMercadoPago() != null
        && !notaDebito.getRecibo().getIdPagoMercadoPago().isEmpty()) {
      mercadoPagoService.devolverPago(notaDebito.getRecibo().getIdPagoMercadoPago());
    }
    cuentaCorrienteService.asentarEnCuentaCorriente(notaDebito, TipoDeOperacion.ALTA);
    logger.warn("La Nota {} se guardó correctamente.", notaDebito);
    return notaDebito;
  }

  @Override
  @Transactional
  public Nota autorizarNota(Nota nota) {
    if (nota.getMovimiento().equals(Movimiento.VENTA)) {
      BigDecimal montoNoGravado =
          (nota instanceof NotaDebito) ? ((NotaDebito) nota).getMontoNoGravado() : BigDecimal.ZERO;
      Cliente cliente;
      if (nota instanceof NotaCredito) {
        if (nota.getFacturaVenta() != null && nota.getFacturaVenta().getCae() == 0L) {
          throw new BusinessServiceException(
              messageSource.getMessage(
                  "mensaje_nota_factura_relacionada_sin_CAE", null, Locale.getDefault()));
        }
        cliente = nota.getCliente();
      } else {
        cliente = nota.getCliente();
      }
      ComprobanteAFIP comprobante =
          ComprobanteAFIP.builder()
              .idComprobante(nota.getIdNota())
              .fecha(nota.getFecha())
              .tipoComprobante(nota.getTipoComprobante())
              .cae(nota.getCae())
              .vencimientoCAE(nota.getVencimientoCae())
              .numSerieAfip(nota.getNumSerieAfip())
              .numFacturaAfip(nota.getNumNotaAfip())
              .sucursal(nota.getSucursal())
              .cliente(clienteService.crearClienteEmbedded(cliente))
              .subtotalBruto(nota.getSubTotalBruto())
              .iva105neto(nota.getIva105Neto())
              .iva21neto(nota.getIva21Neto())
              .montoNoGravado(montoNoGravado)
              .total(nota.getTotal())
              .build();
      afipService.autorizar(comprobante);
      nota.setCae(comprobante.getCae());
      nota.setVencimientoCae(comprobante.getVencimientoCAE());
      nota.setNumSerieAfip(comprobante.getNumSerieAfip());
      nota.setNumNotaAfip(comprobante.getNumFacturaAfip());
    } else {
      throw new BusinessServiceException(
          messageSource.getMessage("mensaje_comprobanteAFIP_invalido", null, Locale.getDefault()));
    }
    return nota;
  }

  @Override
  public TipoDeComprobante getTipoDeNotaCreditoSegunFactura(TipoDeComprobante tipo) {
    switch (tipo) {
      case FACTURA_A:
        return TipoDeComprobante.NOTA_CREDITO_A;
      case FACTURA_B:
        return TipoDeComprobante.NOTA_CREDITO_B;
      case FACTURA_C:
        return TipoDeComprobante.NOTA_CREDITO_C;
      case FACTURA_X:
        return TipoDeComprobante.NOTA_CREDITO_X;
      case FACTURA_Y:
        return TipoDeComprobante.NOTA_CREDITO_Y;
      case PRESUPUESTO:
        return TipoDeComprobante.NOTA_CREDITO_PRESUPUESTO;
      default:
        throw new ServiceException(
            messageSource.getMessage("mensaje_nota_tipo_no_valido", null, Locale.getDefault()));
    }
  }

  private void actualizarStock(
      List<RenglonNotaCredito> renglonesNotaCredito,
      Long idSucursal,
      TipoDeOperacion tipoOperacion,
      Movimiento movimiento) {
    HashMap<Long, BigDecimal> idsYCantidades = new HashMap<>();
    renglonesNotaCredito.forEach(r -> idsYCantidades.put(r.getIdProductoItem(), r.getCantidad()));
    productoService.actualizarStockNotaCredito(idsYCantidades, idSucursal, tipoOperacion, movimiento);
  }

  @Override
  public byte[] getReporteNota(Nota nota) {
    ClassLoader classLoader = NotaServiceImpl.class.getClassLoader();
    InputStream isFileReport;
    JRBeanCollectionDataSource ds;
    Map<String, Object> params = new HashMap<>();
    if (nota instanceof NotaCredito) {
      isFileReport = classLoader.getResourceAsStream("sic/vista/reportes/NotaCredito.jasper");
      List<RenglonNotaCredito> renglones = this.getRenglonesDeNotaCredito(nota.getIdNota());
      ds = new JRBeanCollectionDataSource(renglones);
      params.put("notaCredito", nota);
    } else {
      isFileReport = classLoader.getResourceAsStream("sic/vista/reportes/NotaDebito.jasper");
      List<RenglonNotaDebito> renglones = this.getRenglonesDeNotaDebito(nota.getIdNota());
      ds = new JRBeanCollectionDataSource(renglones);
      params.put("notaDebito", nota);
    }
    ConfiguracionSucursal configuracionSucursal = nota.getSucursal().getConfiguracionSucursal();
    params.put("preImpresa", configuracionSucursal.isUsarFacturaVentaPreImpresa());
    if (nota.getTipoComprobante().equals(TipoDeComprobante.NOTA_CREDITO_B)
        || nota.getTipoComprobante().equals(TipoDeComprobante.NOTA_CREDITO_C)
        || nota.getTipoComprobante().equals(TipoDeComprobante.NOTA_CREDITO_X)
        || nota.getTipoComprobante().equals(TipoDeComprobante.NOTA_DEBITO_B)
        || nota.getTipoComprobante().equals(TipoDeComprobante.NOTA_DEBITO_C)
        || nota.getTipoComprobante().equals(TipoDeComprobante.NOTA_DEBITO_X)
        || nota.getTipoComprobante().equals(TipoDeComprobante.NOTA_CREDITO_PRESUPUESTO)
        || nota.getTipoComprobante().equals(TipoDeComprobante.NOTA_DEBITO_PRESUPUESTO)) {
      nota.setSubTotalBruto(nota.getTotal());
      nota.setIva105Neto(BigDecimal.ZERO);
      nota.setIva21Neto(BigDecimal.ZERO);
    }
    if (nota.getTipoComprobante().equals(TipoDeComprobante.NOTA_CREDITO_A)
        || nota.getTipoComprobante().equals(TipoDeComprobante.NOTA_CREDITO_B)
        || nota.getTipoComprobante().equals(TipoDeComprobante.NOTA_CREDITO_C)
        || nota.getTipoComprobante().equals(TipoDeComprobante.NOTA_DEBITO_A)
        || nota.getTipoComprobante().equals(TipoDeComprobante.NOTA_DEBITO_B)
        || nota.getTipoComprobante().equals(TipoDeComprobante.NOTA_DEBITO_C)) {
      if (nota.getNumSerieAfip() != 0 && nota.getNumNotaAfip() != 0) {
        params.put("serie", nota.getNumSerieAfip());
        params.put("nroNota", nota.getNumNotaAfip());
      } else {
        params.put("serie", null);
        params.put("nroNota", null);
      }
    } else {
      params.put("serie", nota.getSerie());
      params.put("nroNota", nota.getNroNota());
    }
    if (nota.getSucursal().getLogo() != null && !nota.getSucursal().getLogo().isEmpty()) {
      try {
        params.put(
            "logo", new ImageIcon(ImageIO.read(new URL(nota.getSucursal().getLogo()))).getImage());
      } catch (IOException ex) {
        logger.error(ex.getMessage());
        throw new ServiceException(
            messageSource.getMessage("mensaje_sucursal_404_logo", null, Locale.getDefault()), ex);
      }
    }
    try {
      return JasperExportManager.exportReportToPdf(
          JasperFillManager.fillReport(isFileReport, params, ds));
    } catch (JRException ex) {
      logger.error(ex.getMessage());
      throw new ServiceException(
          messageSource.getMessage("mensaje_error_reporte", null, Locale.getDefault()), ex);
    }
  }

  @Override
  public BigDecimal calcularTotalNota(List<RenglonNotaCredito> renglonesNota) {
    BigDecimal total = BigDecimal.ZERO;
    for (RenglonNotaCredito renglon : renglonesNota) {
      total = total.add(renglon.getImporteNeto());
    }
    return total;
  }

  @Override
  public List<RenglonNotaCredito> calcularRenglonesCreditoProducto(
      TipoDeComprobante tipo, BigDecimal[] cantidad, Long[] idRenglonFactura) {
    List<RenglonNotaCredito> renglonesNota = new ArrayList<>();
    RenglonNotaCredito renglonNota;
    if (cantidad.length == idRenglonFactura.length) {
      for (int i = 0; i < idRenglonFactura.length; i++) {
        RenglonFactura renglonFactura = facturaService.getRenglonFactura(idRenglonFactura[i]);
        if (renglonFactura.getCantidad().compareTo(cantidad[i]) < 0
            || cantidad[i].compareTo(BigDecimal.ZERO) < 0) {
          throw new BusinessServiceException(
              messageSource.getMessage(
                  "mensaje_nota_de_credito_cantidad_no_valida",
                  new Object[] {renglonFactura.getDescripcionItem()},
                  Locale.getDefault()));
        }
        renglonNota = new RenglonNotaCredito();
        renglonNota.setIdProductoItem(renglonFactura.getIdProductoItem());
        renglonNota.setCodigoItem(renglonFactura.getCodigoItem());
        renglonNota.setDescripcionItem(renglonFactura.getDescripcionItem());
        renglonNota.setMedidaItem(renglonFactura.getMedidaItem());
        renglonNota.setCantidad(cantidad[i]);
        renglonNota.setPrecioUnitario(renglonFactura.getPrecioUnitario());
        renglonNota.setDescuentoPorcentaje(renglonFactura.getBonificacionPorcentaje());
        renglonNota.setDescuentoNeto(
            renglonFactura
                .getBonificacionPorcentaje()
                .divide(CIEN, 15, RoundingMode.HALF_UP)
                .multiply(renglonNota.getPrecioUnitario()));
        renglonNota.setGananciaPorcentaje(renglonFactura.getGananciaPorcentaje());
        renglonNota.setGananciaNeto(
            renglonNota
                .getGananciaPorcentaje()
                .divide(CIEN, 15, RoundingMode.HALF_UP)
                .multiply(renglonNota.getPrecioUnitario()));
        renglonNota.setIvaPorcentaje(renglonFactura.getIvaPorcentaje());
        if (tipo.equals(TipoDeComprobante.FACTURA_Y)) {
          renglonNota.setIvaPorcentaje(
              renglonFactura
                  .getIvaPorcentaje()
                  .divide(new BigDecimal("2"), 15, RoundingMode.HALF_UP));
        }
        renglonNota.setIvaNeto(
            (tipo == TipoDeComprobante.NOTA_CREDITO_A
                    || tipo == TipoDeComprobante.NOTA_CREDITO_B
                    || tipo == TipoDeComprobante.NOTA_CREDITO_C
                    || tipo == TipoDeComprobante.NOTA_CREDITO_PRESUPUESTO)
                ? renglonFactura.getIvaNeto()
                : BigDecimal.ZERO);
        renglonNota.setImporte(renglonNota.getPrecioUnitario().multiply(cantidad[i]));
        renglonNota.setImporteBruto(
            renglonNota
                .getImporte()
                .subtract(renglonNota.getDescuentoNeto().multiply(cantidad[i])));
        if (tipo == TipoDeComprobante.NOTA_CREDITO_B
            || tipo == TipoDeComprobante.NOTA_CREDITO_C
            || tipo == TipoDeComprobante.NOTA_CREDITO_PRESUPUESTO) {
          renglonNota.setImporteNeto(renglonNota.getImporteBruto());
        } else {
          renglonNota.setImporteNeto(
              renglonNota.getImporteBruto().add(renglonNota.getIvaNeto().multiply(cantidad[i])));
        }
        renglonesNota.add(renglonNota);
      }
    }
    return renglonesNota;
  }

  @Override
  public RenglonNotaCredito calcularRenglonCredito(
      TipoDeComprobante tipo, String detalle, BigDecimal monto) {
    this.validarTipoNotaCredito(tipo);
    RenglonNotaCredito renglonNota = new RenglonNotaCredito();
    renglonNota.setIdProductoItem(null);
    renglonNota.setCodigoItem(null);
    renglonNota.setDescripcionItem(detalle);
    renglonNota.setMedidaItem(null);
    renglonNota.setCantidad(BigDecimal.ONE);
    BigDecimal subTotal =
        monto
            .multiply(new BigDecimal("100"))
            .divide(new BigDecimal("121"), 15, RoundingMode.HALF_UP);
    renglonNota.setPrecioUnitario(
        (tipo == TipoDeComprobante.NOTA_CREDITO_B
                || tipo == TipoDeComprobante.NOTA_CREDITO_C
                || tipo == TipoDeComprobante.NOTA_CREDITO_X
                || tipo == TipoDeComprobante.NOTA_CREDITO_PRESUPUESTO)
            ? monto
            : subTotal);
    renglonNota.setDescuentoPorcentaje(BigDecimal.ZERO);
    renglonNota.setDescuentoNeto(BigDecimal.ZERO);
    renglonNota.setGananciaPorcentaje(BigDecimal.ZERO);
    renglonNota.setGananciaNeto(BigDecimal.ZERO);
    renglonNota.setIvaPorcentaje(
        (tipo == TipoDeComprobante.NOTA_CREDITO_A
                || tipo == TipoDeComprobante.NOTA_CREDITO_B
                || tipo == TipoDeComprobante.NOTA_CREDITO_PRESUPUESTO)
            ? new BigDecimal("21")
            : BigDecimal.ZERO);
    renglonNota.setIvaNeto(
        (tipo == TipoDeComprobante.NOTA_CREDITO_X || tipo == TipoDeComprobante.NOTA_CREDITO_C)
            ? BigDecimal.ZERO
            : monto.subtract(subTotal));
    renglonNota.setImporte(
        (tipo == TipoDeComprobante.NOTA_CREDITO_B
                || tipo == TipoDeComprobante.NOTA_CREDITO_C
                || tipo == TipoDeComprobante.NOTA_CREDITO_X
                || tipo == TipoDeComprobante.NOTA_CREDITO_PRESUPUESTO)
            ? monto
            : subTotal);
    renglonNota.setImporteBruto(
        (tipo == TipoDeComprobante.NOTA_CREDITO_B
                || tipo == TipoDeComprobante.NOTA_CREDITO_C
                || tipo == TipoDeComprobante.NOTA_CREDITO_X
                || tipo == TipoDeComprobante.NOTA_CREDITO_PRESUPUESTO)
            ? monto
            : subTotal);
    if (tipo == TipoDeComprobante.NOTA_CREDITO_B
        || tipo == TipoDeComprobante.NOTA_CREDITO_C
        || tipo == TipoDeComprobante.NOTA_CREDITO_PRESUPUESTO) {
      renglonNota.setImporteNeto(renglonNota.getImporteBruto());
    } else {
      renglonNota.setImporteNeto(renglonNota.getImporteBruto().add(renglonNota.getIvaNeto()));
    }
    return renglonNota;
  }

  private void validarTipoNotaCredito(TipoDeComprobante tipo) {
    TipoDeComprobante[] tiposPermitidos = {
      TipoDeComprobante.NOTA_CREDITO_A,
      TipoDeComprobante.NOTA_CREDITO_B,
      TipoDeComprobante.NOTA_CREDITO_C,
      TipoDeComprobante.NOTA_CREDITO_Y,
      TipoDeComprobante.NOTA_CREDITO_X,
      TipoDeComprobante.NOTA_CREDITO_PRESUPUESTO,
    };
    if (!Arrays.asList(tiposPermitidos).contains(tipo)) {
      throw new BusinessServiceException(
          messageSource.getMessage(
              "mensaje_tipo_de_comprobante_no_valido", null, Locale.getDefault()));
    }
  }

  @Override
  public RenglonNotaDebito calcularRenglonDebitoConRecibo(Recibo recibo) {
    RenglonNotaDebito renglonNota = new RenglonNotaDebito();
    String descripcion =
        "Nº Recibo " + recibo.getNumSerie() + "-" + recibo.getNumRecibo() + ": " + recibo.getConcepto();
    renglonNota.setDescripcion(descripcion);
    renglonNota.setMonto(recibo.getMonto());
    renglonNota.setImporteBruto(renglonNota.getMonto());
    renglonNota.setIvaPorcentaje(BigDecimal.ZERO);
    renglonNota.setIvaNeto(BigDecimal.ZERO);
    renglonNota.setImporteNeto(recibo.getMonto());
    return renglonNota;
  }

  @Override
  public RenglonNotaDebito calcularRenglonDebito(
      BigDecimal monto, TipoDeComprobante tipoDeComprobante) {
    RenglonNotaDebito renglonNota = new RenglonNotaDebito();
    renglonNota.setDescripcion("Gasto Administrativo");
    switch (tipoDeComprobante) {
      case NOTA_DEBITO_A:
      case NOTA_DEBITO_B:
      case NOTA_DEBITO_PRESUPUESTO:
        renglonNota.setMonto(
            monto.multiply(CIEN).divide(new BigDecimal("121"), 15, RoundingMode.HALF_UP));
        renglonNota.setIvaPorcentaje(IVA_21);
        renglonNota.setIvaNeto(
            renglonNota.getMonto().multiply(IVA_21.divide(CIEN, 15, RoundingMode.HALF_UP)));
        break;
      case NOTA_DEBITO_C:
      case NOTA_DEBITO_X:
        renglonNota.setMonto(monto);
        renglonNota.setIvaPorcentaje(BigDecimal.ZERO);
        renglonNota.setIvaNeto(BigDecimal.ZERO);
        break;
      case NOTA_DEBITO_Y:
        renglonNota.setMonto(
            monto.multiply(CIEN).divide(new BigDecimal("110.5"), 15, RoundingMode.HALF_UP));
        renglonNota.setIvaPorcentaje(IVA_105);
        renglonNota.setIvaNeto(
            renglonNota.getMonto().multiply(IVA_105.divide(CIEN, 15, RoundingMode.HALF_UP)));
        break;
      default:
        throw new BusinessServiceException(
            messageSource.getMessage(
                "mensaje_tipo_de_comprobante_no_valido", null, Locale.getDefault()));
    }
    renglonNota.setImporteBruto(renglonNota.getMonto());
    renglonNota.setImporteNeto(renglonNota.getIvaNeto().add(renglonNota.getImporteBruto()));
    return renglonNota;
  }

  @Override
  public BigDecimal calcularSubTotalCredito(BigDecimal[] importes) {
    BigDecimal resultado = BigDecimal.ZERO;
    for (BigDecimal importe : importes) {
      resultado = resultado.add(importe);
    }
    return resultado;
  }

  @Override
  public BigDecimal calcularDecuentoNetoCredito(
      BigDecimal subTotal, BigDecimal descuentoPorcentaje) {
    BigDecimal resultado = BigDecimal.ZERO;
    if (descuentoPorcentaje.compareTo(BigDecimal.ZERO) != 0) {
      resultado = subTotal.multiply(descuentoPorcentaje).divide(CIEN, 15, RoundingMode.HALF_UP);
    }
    return resultado;
  }

  @Override
  public BigDecimal calcularRecargoNetoCredito(BigDecimal subTotal, BigDecimal recargoPorcentaje) {
    BigDecimal resultado = BigDecimal.ZERO;
    if (recargoPorcentaje.compareTo(BigDecimal.ZERO) != 0) {
      resultado = subTotal.multiply(recargoPorcentaje).divide(CIEN, 15, RoundingMode.HALF_UP);
    }
    return resultado;
  }

  @Override
  public BigDecimal calcularIVANetoCredito(
      TipoDeComprobante tipoDeComprobante,
      BigDecimal[] cantidades,
      BigDecimal[] ivaPorcentajeRenglones,
      BigDecimal[] ivaNetoRenglones,
      BigDecimal ivaPorcentaje,
      BigDecimal descuentoPorcentaje,
      BigDecimal recargoPorcentaje) {
    BigDecimal resultado = BigDecimal.ZERO;
    int indice = cantidades.length;
    for (int i = 0; i < indice; i++) {
      if (ivaPorcentajeRenglones[i].compareTo(ivaPorcentaje) == 0) {
        if (tipoDeComprobante == TipoDeComprobante.NOTA_CREDITO_A
            || tipoDeComprobante == TipoDeComprobante.NOTA_CREDITO_B
            || tipoDeComprobante == TipoDeComprobante.NOTA_CREDITO_C
            || tipoDeComprobante == TipoDeComprobante.NOTA_CREDITO_PRESUPUESTO) {
          resultado =
              resultado.add(
                  cantidades[i].multiply(
                      ivaNetoRenglones[i]
                          .subtract(
                              ivaNetoRenglones[i].multiply(
                                  descuentoPorcentaje.divide(CIEN, 15, RoundingMode.HALF_UP)))
                          .add(
                              ivaNetoRenglones[i].multiply(
                                  recargoPorcentaje.divide(CIEN, 15, RoundingMode.HALF_UP)))));
        } else {
          resultado = resultado.add(cantidades[i].multiply(ivaNetoRenglones[i]));
        }
      }
    }
    return resultado;
  }

  @Override
  public BigDecimal calcularSubTotalBrutoCredito(
      TipoDeComprobante tipoDeComprobante,
      BigDecimal subTotal,
      BigDecimal recargoNeto,
      BigDecimal descuentoNeto,
      BigDecimal iva105Neto,
      BigDecimal iva21Neto) {
    BigDecimal resultado = subTotal.add(recargoNeto).subtract(descuentoNeto);
    if (tipoDeComprobante == TipoDeComprobante.NOTA_CREDITO_B
        || tipoDeComprobante == TipoDeComprobante.NOTA_CREDITO_C
        || tipoDeComprobante == TipoDeComprobante.NOTA_CREDITO_PRESUPUESTO) {
      resultado = resultado.subtract(iva105Neto.add(iva21Neto));
    }
    return resultado;
  }

  @Override
  public BigDecimal calcularTotalCredito(
      BigDecimal subTotalBruto, BigDecimal iva105Neto, BigDecimal iva21Neto) {
    return subTotalBruto.add(iva105Neto).add(iva21Neto);
  }

  @Override
  public BigDecimal calcularTotalDebito(
      BigDecimal subTotalBruto, BigDecimal iva21Neto, BigDecimal montoNoGravado) {
    return subTotalBruto.add(iva21Neto).add(montoNoGravado);
  }

  @Override
  public BigDecimal calcularTotalCredito(BusquedaNotaCriteria criteria, long idUsuarioLoggedIn) {
    this.establecerLimitesDeFechasDeCriteria(criteria);
    BigDecimal totalNotaCredito =
        notaCreditoRepository.calcularTotalCredito(
            this.getBuilderNotaCredito(criteria, idUsuarioLoggedIn));
    return (totalNotaCredito != null ? totalNotaCredito : BigDecimal.ZERO);
  }

  @Override
  public BigDecimal calcularTotalDebito(BusquedaNotaCriteria criteria, long idUsuarioLoggedIn) {
    this.establecerLimitesDeFechasDeCriteria(criteria);
    BigDecimal totalNotaDebito =
        notaDebitoRepository.calcularTotalDebito(
            this.getBuilderNotaDebito(criteria, idUsuarioLoggedIn));
    return (totalNotaDebito != null ? totalNotaDebito : BigDecimal.ZERO);
  }

  @Override
  public BigDecimal calcularTotalIVACredito(BusquedaNotaCriteria criteria, long idUsuarioLoggedIn) {
    this.establecerLimitesDeFechasDeCriteria(criteria);
    BigDecimal ivaNotaCredito =
        notaCreditoRepository.calcularIVACredito(
            this.getBuilderNotaCredito(criteria, idUsuarioLoggedIn),
            new TipoDeComprobante[] {
              TipoDeComprobante.NOTA_CREDITO_A, TipoDeComprobante.NOTA_CREDITO_B
            });
    return (ivaNotaCredito != null ? ivaNotaCredito : BigDecimal.ZERO);
  }

  @Override
  public BigDecimal calcularTotalIVADebito(BusquedaNotaCriteria criteria, long idUsuarioLoggedIn) {
    this.establecerLimitesDeFechasDeCriteria(criteria);
    BigDecimal ivaNotaDebito =
        notaDebitoRepository.calcularIVADebito(
            this.getBuilderNotaDebito(criteria, idUsuarioLoggedIn),
            new TipoDeComprobante[] {
              TipoDeComprobante.NOTA_DEBITO_A, TipoDeComprobante.NOTA_DEBITO_B
            });
    return (ivaNotaDebito != null ? ivaNotaDebito : BigDecimal.ZERO);
  }

  @Override
  public boolean existeNotaCreditoAnteriorSinAutorizar(ComprobanteAFIP comprobante) {
    QNotaCredito qNotaCredito = QNotaCredito.notaCredito;
    BooleanBuilder builder = new BooleanBuilder();
    builder.and(
        qNotaCredito
            .idNota
            .lt(comprobante.getIdComprobante())
            .and(qNotaCredito.eliminada.eq(false))
            .and(qNotaCredito.sucursal.idSucursal.eq(comprobante.getSucursal().getIdSucursal()))
            .and(qNotaCredito.tipoComprobante.eq(comprobante.getTipoComprobante()))
            .and(qNotaCredito.cliente.isNotNull()));
    Page<NotaCredito> notaAnterior =
        notaCreditoRepository.findAll(
            builder, PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "fecha")));
    return notaAnterior.getContent().get(0).getCae() == 0L;
  }

  @Override
  public boolean existeNotaDebitoAnteriorSinAutorizar(ComprobanteAFIP comprobante) {
    QNotaDebito qNotaDebito = QNotaDebito.notaDebito;
    BooleanBuilder builder = new BooleanBuilder();
    builder.and(
        qNotaDebito
            .idNota
            .lt(comprobante.getIdComprobante())
            .and(qNotaDebito.eliminada.eq(false))
            .and(qNotaDebito.sucursal.idSucursal.eq(comprobante.getSucursal().getIdSucursal()))
            .and(qNotaDebito.tipoComprobante.eq(comprobante.getTipoComprobante()))
            .and(qNotaDebito.cliente.isNotNull()));
    Page<NotaDebito> notaAnterior =
        notaDebitoRepository.findAll(
            builder, PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "fecha")));
    return (!notaAnterior.getContent().isEmpty() && notaAnterior.getContent().get(0).getCae() == 0L);
  }

  private void establecerLimitesDeFechasDeCriteria(BusquedaNotaCriteria criteria) {
    if (criteria.getFechaDesde() != null && criteria.getFechaHasta() != null) {
      criteria.setFechaDesde(criteria.getFechaDesde().withHour(0).withMinute(0).withSecond(0).withNano(0));
      criteria.setFechaHasta(criteria.getFechaHasta().withHour(23).withMinute(59).withSecond(59).withNano(999999999));
    }
  }
}
