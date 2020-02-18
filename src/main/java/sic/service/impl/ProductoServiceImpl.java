package sic.service.impl;

import com.querydsl.core.BooleanBuilder;

import java.io.*;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.*;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import sic.modelo.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sic.modelo.criteria.BusquedaProductoCriteria;
import sic.modelo.dto.ProductoFaltanteDTO;
import sic.modelo.dto.ProductosParaActualizarDTO;
import sic.modelo.dto.ProductosParaVerificarStockDTO;
import sic.service.*;
import sic.exception.BusinessServiceException;
import sic.exception.ServiceException;
import sic.repository.ProductoRepository;
import sic.util.CalculosComprobante;

@Service
@Validated
public class ProductoServiceImpl implements IProductoService {

  private final ProductoRepository productoRepository;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private static final BigDecimal CIEN = new BigDecimal("100");
  private static final long TAMANIO_MAXIMO_IMAGEN = 1024000L;
  private final IRubroService rubroService;
  private final IProveedorService proveedorService;
  private final IMedidaService medidaService;
  private final ICarritoCompraService carritoCompraService;
  private final IPhotoVideoUploader photoVideoUploader;
  private static final int TAMANIO_PAGINA_DEFAULT = 25;
  private final MessageSource messageSource;

  @Autowired
  @Lazy
  public ProductoServiceImpl(
    ProductoRepository productoRepository,
    IRubroService rubroService,
    IProveedorService proveedorService,
    IMedidaService medidaService,
    ICarritoCompraService carritoCompraService,
    IPhotoVideoUploader photoVideoUploader,
    MessageSource messageSource) {
    this.productoRepository = productoRepository;
    this.rubroService = rubroService;
    this.proveedorService = proveedorService;
    this.medidaService = medidaService;
    this.carritoCompraService = carritoCompraService;
    this.photoVideoUploader = photoVideoUploader;
    this.messageSource = messageSource;
  }

  private void validarOperacion(TipoDeOperacion operacion, Producto producto) {
    if (producto.isOferta()
        && producto.getPorcentajeBonificacionOferta().compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessServiceException(
          messageSource.getMessage(
              "mensaje_producto_oferta_inferior_0", null, Locale.getDefault()));
    }
    // Codigo
    if (!producto.getCodigo().equals("")) {
      Producto productoDuplicado = this.getProductoPorCodigo(producto.getCodigo());
      if (operacion.equals(TipoDeOperacion.ACTUALIZACION)
          && productoDuplicado != null
          && !producto.getIdProducto().equals(productoDuplicado.getIdProducto())) {
        throw new BusinessServiceException(
            messageSource.getMessage(
                "mensaje_producto_duplicado_codigo", null, Locale.getDefault()));
      }
      if (operacion.equals(TipoDeOperacion.ALTA)
          && productoDuplicado != null
          && !producto.getCodigo().equals("")) {
        throw new BusinessServiceException(
          messageSource.getMessage(
            "mensaje_producto_duplicado_codigo", null, Locale.getDefault()));
      }
    }
    // Descripcion
    Producto productoDuplicado = this.getProductoPorDescripcion(producto.getDescripcion());
    if (operacion.equals(TipoDeOperacion.ALTA) && productoDuplicado != null) {
      throw new BusinessServiceException(
          messageSource.getMessage(
              "mensaje_producto_duplicado_descripcion", null, Locale.getDefault()));
    }
    if (operacion.equals(TipoDeOperacion.ACTUALIZACION)
        && productoDuplicado != null
        && !producto.getIdProducto().equals(productoDuplicado.getIdProducto()))
      throw new BusinessServiceException(
          messageSource.getMessage(
              "mensaje_producto_duplicado_descripcion", null, Locale.getDefault()));
    this.validarCalculos(producto);
  }

  private void validarCalculos(Producto producto) {
    Double[] iva = {10.5, 21.0, 0.0};
    if (!Arrays.asList(iva).contains(producto.getIvaPorcentaje().doubleValue())) {
      throw new BusinessServiceException(
          messageSource.getMessage(
              "mensaje_error_iva_no_valido",
              new Object[] {producto.getDescripcion()},
              Locale.getDefault()));
    }
    if (producto
            .getGananciaNeto()
            .setScale(3, RoundingMode.HALF_UP)
            .compareTo(
                this.calcularGananciaNeto(
                        producto.getPrecioCosto(), producto.getGananciaPorcentaje())
                    .setScale(3, RoundingMode.HALF_UP))
        != 0) {
      throw new BusinessServiceException(
          messageSource.getMessage(
              "mensaje_producto_ganancia_neta_incorrecta",
              new Object[] {producto.getDescripcion()},
              Locale.getDefault()));
    }
    if (producto
            .getPrecioVentaPublico()
            .setScale(3, RoundingMode.HALF_UP)
            .compareTo(
                this.calcularPVP(producto.getPrecioCosto(), producto.getGananciaPorcentaje())
                    .setScale(3, RoundingMode.HALF_UP))
        != 0) {
      throw new BusinessServiceException(
          messageSource.getMessage(
              "mensaje_precio_venta_publico_incorrecto",
              new Object[] {producto.getDescripcion()},
              Locale.getDefault()));
    }
    if (producto
            .getIvaNeto()
            .setScale(3, RoundingMode.HALF_UP)
            .compareTo(
                this.calcularIVANeto(producto.getPrecioVentaPublico(), producto.getIvaPorcentaje())
                    .setScale(3, RoundingMode.HALF_UP))
        != 0) {
      throw new BusinessServiceException(
          messageSource.getMessage(
              "mensaje_producto_iva_neto_incorrecto",
              new Object[] {producto.getDescripcion()},
              Locale.getDefault()));
    }
    if (producto
            .getPrecioLista()
            .setScale(3, RoundingMode.HALF_UP)
            .compareTo(
                this.calcularPrecioLista(
                        producto.getPrecioVentaPublico(), producto.getIvaPorcentaje())
                    .setScale(3, RoundingMode.HALF_UP))
        != 0) {
      throw new BusinessServiceException(
          messageSource.getMessage(
              "mensaje_producto_precio_lista_incorrecto",
              new Object[] {producto.getDescripcion()},
              Locale.getDefault()));
    }
  }

  @Override
  public Page<Producto> buscarProductos(BusquedaProductoCriteria criteria) {
    Page<Producto> productos =
        productoRepository.findAll(
            this.getBuilder(criteria),
            this.getPageable(
                criteria.getPagina(),
                criteria.getOrdenarPor(),
                criteria.getSentido(),
                TAMANIO_PAGINA_DEFAULT));
    productos.stream()
        .filter(Producto::isOferta)
        .forEach(
            producto ->
                producto.setPrecioBonificado(
                    producto
                        .getPrecioLista()
                        .multiply(
                            (new BigDecimal("100"))
                                .subtract(producto.getPorcentajeBonificacionOferta())
                                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP))));
    return productos;
  }

  @Override
  public List<Producto> buscarProductosParaReporte(BusquedaProductoCriteria criteria) {
    criteria.setPagina(0);
    return productoRepository
        .findAll(
            this.getBuilder(criteria),
            this.getPageable(
                criteria.getPagina(),
                criteria.getOrdenarPor(),
                criteria.getSentido(),
                Integer.MAX_VALUE))
        .getContent();
  }

  private Pageable getPageable(Integer pagina, String ordenarPor, String sentido, int tamanioPagina) {
    if (pagina == null) pagina = 0;
    String ordenDefault = "descripcion";
    if (ordenarPor == null || sentido == null) {
      return PageRequest.of(pagina, tamanioPagina, new Sort(Sort.Direction.ASC, ordenDefault));
    } else {
      switch (sentido) {
        case "ASC":
          return PageRequest.of(pagina, tamanioPagina, new Sort(Sort.Direction.ASC, ordenarPor));
        case "DESC":
          return PageRequest.of(pagina, tamanioPagina, new Sort(Sort.Direction.DESC, ordenarPor));
        default:
          return PageRequest.of(pagina, tamanioPagina, new Sort(Sort.Direction.DESC, ordenDefault));
      }
    }
  }

  private BooleanBuilder getBuilder(BusquedaProductoCriteria criteria) {
    QProducto qProducto = QProducto.producto;
    BooleanBuilder builder = new BooleanBuilder();
    builder.and(qProducto.eliminado.eq(false));
    if (criteria.getCodigo() != null && criteria.getDescripcion() != null)
      builder.and(
          qProducto
              .codigo
              .containsIgnoreCase(criteria.getCodigo())
              .or(this.buildPredicadoDescripcion(criteria.getDescripcion(), qProducto)));
    else {
      if (criteria.getCodigo() != null)
        builder.and(qProducto.codigo.containsIgnoreCase(criteria.getCodigo()));
      if (criteria.getDescripcion() != null)
        builder.and(this.buildPredicadoDescripcion(criteria.getDescripcion(), qProducto));
    }
    if (criteria.getIdRubro() != null)
      builder.and(qProducto.rubro.idRubro.eq(criteria.getIdRubro()));
    if (criteria.getIdProveedor() != null)
      builder.and(qProducto.proveedor.idProveedor.eq(criteria.getIdProveedor()));
    if (criteria.isListarSoloFaltantes())
      builder
          .and(qProducto.cantidadEnSucursales.any().cantidad.loe(qProducto.cantMinima))
          .and(qProducto.ilimitado.eq(false));
    if (criteria.isListarSoloEnStock())
      builder
          .and(qProducto.cantidadEnSucursales.any().cantidad.gt(BigDecimal.ZERO))
          .and(qProducto.ilimitado.eq(false));
    if (criteria.getPublico() != null) {
      if (criteria.getPublico()) builder.and(qProducto.publico.isTrue());
      else builder.and(qProducto.publico.isFalse());
    }
    if (criteria.getOferta() != null && criteria.getOferta())
      builder.and(qProducto.oferta.isTrue());
    return builder;
  }

  private BooleanBuilder buildPredicadoDescripcion(String descripcion, QProducto qProducto) {
    String[] terminos = descripcion.split(" ");
    BooleanBuilder descripcionProducto = new BooleanBuilder();
    for (String termino : terminos) {
      descripcionProducto.and(qProducto.descripcion.containsIgnoreCase(termino));
    }
    return descripcionProducto;
  }

  @Override
  @Transactional
  public Producto guardar(@Valid Producto producto, byte[] imagen) {
    if (producto.isOferta() && imagen == null)
      throw new BusinessServiceException(
          messageSource.getMessage(
              "mensaje_producto_oferta_sin_imagen",
              new Object[] {producto.getDescripcion()},
              Locale.getDefault()));
    if (producto.getCodigo() == null) producto.setCodigo("");
    producto.setEliminado(false);
    this.calcularPrecioBonificado(producto);
    this.validarOperacion(TipoDeOperacion.ALTA, producto);
    // se setea siempre en false momentaniamente
    producto.setIlimitado(false);
    producto = productoRepository.save(producto);
    logger.warn("El Producto {} se guardó correctamente.", producto);
    if (imagen != null) this.subirImagenProducto(producto.getIdProducto(), imagen);
    return producto;
  }

  @Override
  @Transactional
  public void actualizar(@Valid Producto productoPorActualizar, Producto productoPersistido, byte[] imagen) {
    if (productoPorActualizar.isOferta()
        && (productoPorActualizar.getUrlImagen() == null
            || productoPorActualizar.getUrlImagen().isEmpty())
        && imagen == null)
      throw new BusinessServiceException(
          messageSource.getMessage(
              "mensaje_producto_oferta_sin_imagen",
              new Object[] {productoPorActualizar.getDescripcion()},
              Locale.getDefault()));
    productoPorActualizar.setEliminado(productoPersistido.isEliminado());
    productoPorActualizar.setFechaAlta(productoPersistido.getFechaAlta());
    productoPorActualizar.setFechaUltimaModificacion(LocalDateTime.now());
    if ((productoPersistido.getUrlImagen() != null && !productoPersistido.getUrlImagen().isEmpty())
        && (productoPorActualizar.getUrlImagen() == null
            || productoPorActualizar.getUrlImagen().isEmpty())) {
      photoVideoUploader.borrarImagen(
          Producto.class.getSimpleName() + productoPersistido.getIdProducto());
    }
    this.validarOperacion(TipoDeOperacion.ACTUALIZACION, productoPorActualizar);
    this.calcularPrecioBonificado(productoPorActualizar);
    if (productoPersistido.isPublico() && !productoPorActualizar.isPublico()) {
      carritoCompraService.eliminarItem(productoPersistido.getIdProducto());
    }
    //se setea siempre en false momentaniamente
    productoPorActualizar.setIlimitado(false);
    productoPorActualizar = productoRepository.save(productoPorActualizar);
    logger.warn("El Producto {} se modificó correctamente.", productoPorActualizar);
    if (imagen != null) this.subirImagenProducto(productoPorActualizar.getIdProducto(), imagen);
  }

  private void calcularPrecioBonificado(Producto producto) {
    producto.setPrecioBonificado(producto.getPrecioLista());
    if (producto.isOferta()
        && producto.getPorcentajeBonificacionOferta() != null
        && producto.getPorcentajeBonificacionOferta().compareTo(BigDecimal.ZERO) > 0) {
      producto.setPrecioBonificado(
          producto
              .getPrecioLista()
              .subtract(
                  CalculosComprobante.calcularProporcion(
                      producto.getPrecioLista(), producto.getPorcentajeBonificacionOferta())));
    } else if (producto.getPorcentajeBonificacionPrecio() != null
        && producto.getPorcentajeBonificacionPrecio().compareTo(BigDecimal.ZERO) > 0) {
      producto.setOferta(false);
      producto.setPrecioBonificado(
          producto
              .getPrecioLista()
              .subtract(
                  CalculosComprobante.calcularProporcion(
                      producto.getPrecioLista(), producto.getPorcentajeBonificacionPrecio())));
    }
  }

  @Override
  public void actualizarStock(
      Map<Long, BigDecimal> idsYCantidades,
      Long idSucursal,
      TipoDeOperacion operacion,
      Movimiento movimiento,
      TipoDeComprobante tipoDeComprobante) {
    idsYCantidades.forEach(
        (idProducto, cantidad) -> {
          Optional<Producto> producto = productoRepository.findById(idProducto);
          if (producto.isPresent() && !producto.get().isIlimitado()) {
            List<TipoDeComprobante> tiposDeFactura =
                Arrays.asList(
                    TipoDeComprobante.FACTURA_A,
                    TipoDeComprobante.FACTURA_B,
                    TipoDeComprobante.FACTURA_C,
                    TipoDeComprobante.FACTURA_X,
                    TipoDeComprobante.FACTURA_Y,
                    TipoDeComprobante.PRESUPUESTO);
            List<TipoDeComprobante> tiposDeNotaCreditoQueAfectanStock =
                Arrays.asList(
                    TipoDeComprobante.NOTA_CREDITO_A,
                    TipoDeComprobante.NOTA_CREDITO_B,
                    TipoDeComprobante.NOTA_CREDITO_C,
                    TipoDeComprobante.NOTA_CREDITO_X,
                    TipoDeComprobante.NOTA_CREDITO_X,
                    TipoDeComprobante.NOTA_CREDITO_Y,
                    TipoDeComprobante.NOTA_CREDITO_PRESUPUESTO);
            switch (movimiento) {
              case VENTA:
                if (tiposDeFactura.contains(tipoDeComprobante)) {
                  this.cambiaStockPorFacturaVentaOrNotaCreditoCompra(
                      idSucursal, operacion, producto.get(), cantidad);
                }
                if (tiposDeNotaCreditoQueAfectanStock.contains(tipoDeComprobante)) {
                  this.cambiaStockPorFacturaCompraOrNotaCreditoVenta(
                      idSucursal, operacion, producto.get(), cantidad);
                }
                break;
              case COMPRA:
                if (tiposDeFactura.contains(tipoDeComprobante)) {
                  this.cambiaStockPorFacturaCompraOrNotaCreditoVenta(
                      idSucursal, operacion, producto.get(), cantidad);
                }
                if (tiposDeNotaCreditoQueAfectanStock.contains(tipoDeComprobante)) {
                  this.cambiaStockPorFacturaVentaOrNotaCreditoCompra(
                      idSucursal, operacion, producto.get(), cantidad);
                }
                break;
              default:
                throw new BusinessServiceException(
                    messageSource.getMessage(
                        "mensaje_movimiento_no_valido", null, Locale.getDefault()));
            }
            productoRepository.save(producto.get());
          } else {
            logger.warn("Se intenta actualizar el stock de un producto eliminado.");
          }
        });
  }

  private void cambiaStockPorFacturaVentaOrNotaCreditoCompra(
      Long idSucursal, TipoDeOperacion operacion, Producto producto, BigDecimal cantidad) {
    if (operacion == TipoDeOperacion.ALTA) {
      producto
          .getCantidadEnSucursales()
          .forEach(
              cantidadEnSucursal -> {
                if (cantidadEnSucursal.getSucursal().getIdSucursal() == idSucursal) {
                  cantidadEnSucursal.setCantidad(
                      cantidadEnSucursal.getCantidad().subtract(cantidad));
                }
              });
    }
    if (operacion == TipoDeOperacion.ELIMINACION) {
      producto
          .getCantidadEnSucursales()
          .forEach(
              cantidadEnSucursal -> {
                if (cantidadEnSucursal.getSucursal().getIdSucursal() == idSucursal) {
                  cantidadEnSucursal.setCantidad(cantidadEnSucursal.getCantidad().add(cantidad));
                }
              });
    }
    producto.setCantidadTotalEnSucursales(
        producto
            .getCantidadEnSucursales()
            .stream()
            .map(CantidadEnSucursal::getCantidad)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
    producto.setHayStock(producto.getCantidadTotalEnSucursales().compareTo(BigDecimal.ZERO) > 0);
  }

  private void cambiaStockPorFacturaCompraOrNotaCreditoVenta(
      Long idSucursal, TipoDeOperacion operacion, Producto producto, BigDecimal cantidad) {
    if (operacion == TipoDeOperacion.ALTA) {
      producto
          .getCantidadEnSucursales()
          .forEach(
              cantidadEnSucursal -> {
                if (cantidadEnSucursal.getSucursal().getIdSucursal() == idSucursal) {
                  cantidadEnSucursal.setCantidad(cantidadEnSucursal.getCantidad().add(cantidad));
                }
              });
    }
    if (operacion == TipoDeOperacion.ELIMINACION) {
      producto
          .getCantidadEnSucursales()
          .forEach(
              cantidadEnSucursal -> {
                if (cantidadEnSucursal.getSucursal().getIdSucursal() == idSucursal) {
                  cantidadEnSucursal.setCantidad(
                      cantidadEnSucursal.getCantidad().subtract(cantidad));
                }
              });
    }
    producto.setCantidadTotalEnSucursales(
        producto
            .getCantidadEnSucursales()
            .stream()
            .map(CantidadEnSucursal::getCantidad)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
    producto.setHayStock(producto.getCantidadTotalEnSucursales().compareTo(BigDecimal.ZERO) > 0);
  }

  @Override
  @Transactional
  public void eliminarMultiplesProductos(long[] idProducto) {
    if (this.contieneDuplicados(idProducto)) {
      throw new BusinessServiceException(messageSource.getMessage(
        "mensaje_error_ids_duplicados", null, Locale.getDefault()));
    }
    List<Producto> productos = new ArrayList<>();
    for (Long i : idProducto) {
      Producto producto = this.getProductoNoEliminadoPorId(i);
      if (producto == null) {
        throw new EntityNotFoundException(messageSource.getMessage(
          "mensaje_producto_no_existente", null, Locale.getDefault()));
      }
      carritoCompraService.eliminarItem(i);
      producto.setEliminado(true);
      if (producto.getUrlImagen() != null && !producto.getUrlImagen().isEmpty()) {
        photoVideoUploader.borrarImagen(Producto.class.getSimpleName() + producto.getIdProducto());
      }
      productos.add(producto);
    }
    productoRepository.saveAll(productos);
  }

  @Override
  @Transactional
  public void actualizarMultiples(ProductosParaActualizarDTO productosParaActualizarDTO) {
    boolean actualizaPrecios = productosParaActualizarDTO.getGananciaNeto() != null
      && productosParaActualizarDTO.getGananciaPorcentaje() != null
      && productosParaActualizarDTO.getIvaNeto() != null
      && productosParaActualizarDTO.getIvaPorcentaje() != null
      && productosParaActualizarDTO.getPrecioCosto() != null
      && productosParaActualizarDTO.getPrecioLista() != null
      && productosParaActualizarDTO.getPrecioVentaPublico() != null;
    boolean aplicaDescuentoRecargoPorcentaje = productosParaActualizarDTO.getDescuentoRecargoPorcentaje() != null;
    if (aplicaDescuentoRecargoPorcentaje && actualizaPrecios) {
      throw new BusinessServiceException(messageSource.getMessage(
        "mensaje_modificar_producto_no_permitido", null, Locale.getDefault()));
    }
    // Requeridos
    if (this.contieneDuplicados(productosParaActualizarDTO.getIdProducto())) {
      throw new BusinessServiceException(messageSource.getMessage(
        "mensaje_error_ids_duplicados", null, Locale.getDefault()));
    }
    List<Producto> productos = new ArrayList<>();
    for (long i : productosParaActualizarDTO.getIdProducto()) {
      productos.add(this.getProductoNoEliminadoPorId(i));
    }
    BigDecimal multiplicador = BigDecimal.ZERO;
    if (aplicaDescuentoRecargoPorcentaje) {
      if (productosParaActualizarDTO.getDescuentoRecargoPorcentaje().compareTo(BigDecimal.ZERO)
          > 0) {
        multiplicador =
            productosParaActualizarDTO
                .getDescuentoRecargoPorcentaje()
                .divide(CIEN, 15, RoundingMode.HALF_UP)
                .add(BigDecimal.ONE);
      } else {
        multiplicador =
            BigDecimal.ONE.subtract(
                productosParaActualizarDTO
                    .getDescuentoRecargoPorcentaje()
                    .abs()
                    .divide(CIEN, 15, RoundingMode.HALF_UP));
      }
    }
    for (Producto p : productos) {
      if (productosParaActualizarDTO.getIdMedida() != null) {
        p.setMedida(medidaService.getMedidaNoEliminadaPorId(productosParaActualizarDTO.getIdMedida()));
      }
      if (productosParaActualizarDTO.getIdRubro() != null) {
        Rubro rubro = rubroService.getRubroNoEliminadoPorId(productosParaActualizarDTO.getIdRubro());
        p.setRubro(rubro);
      }
      if (productosParaActualizarDTO.getIdProveedor() != null) {
        Proveedor proveedor =
            proveedorService.getProveedorNoEliminadoPorId(productosParaActualizarDTO.getIdProveedor());
        p.setProveedor(proveedor);
      }
      if (actualizaPrecios) {
        p.setPrecioCosto(productosParaActualizarDTO.getPrecioCosto());
        p.setGananciaPorcentaje(productosParaActualizarDTO.getGananciaPorcentaje());
        p.setGananciaNeto(productosParaActualizarDTO.getGananciaNeto());
        p.setPrecioVentaPublico(productosParaActualizarDTO.getPrecioVentaPublico());
        p.setIvaPorcentaje(productosParaActualizarDTO.getIvaPorcentaje());
        p.setIvaNeto(productosParaActualizarDTO.getIvaNeto());
        p.setPrecioLista(productosParaActualizarDTO.getPrecioLista());
        p.setFechaUltimaModificacion(LocalDateTime.now());
      }
      if (aplicaDescuentoRecargoPorcentaje) {
        p.setPrecioCosto(p.getPrecioCosto().multiply(multiplicador));
        p.setGananciaNeto(this.calcularGananciaNeto(p.getPrecioCosto(), p.getGananciaPorcentaje()));
        p.setPrecioVentaPublico(this.calcularPVP(p.getPrecioCosto(), p.getGananciaPorcentaje()));
        p.setIvaNeto(this.calcularIVANeto(p.getPrecioVentaPublico(), p.getIvaPorcentaje()));
        p.setPrecioLista(
            this.calcularPrecioLista(p.getPrecioVentaPublico(), p.getIvaPorcentaje()));
        p.setFechaUltimaModificacion(LocalDateTime.now());
      }
      if (productosParaActualizarDTO.getIdMedida() != null
          || productosParaActualizarDTO.getIdRubro() != null
          || productosParaActualizarDTO.getIdProveedor() != null
          || actualizaPrecios
          || aplicaDescuentoRecargoPorcentaje) {
        p.setFechaUltimaModificacion(LocalDateTime.now());
      }
      if (productosParaActualizarDTO.getPublico() != null) {
        p.setPublico(productosParaActualizarDTO.getPublico());
        if (!productosParaActualizarDTO.getPublico())
          carritoCompraService.eliminarItem(p.getIdProducto());
      }
      if (productosParaActualizarDTO.getPorcentajeBonificacionPrecio() != null
          && productosParaActualizarDTO.getPorcentajeBonificacionPrecio().compareTo(BigDecimal.ZERO)
              >= 0) {
        p.setPorcentajeBonificacionPrecio(
            productosParaActualizarDTO.getPorcentajeBonificacionPrecio());
      }
      this.calcularPrecioBonificado(p);
      this.validarOperacion(TipoDeOperacion.ACTUALIZACION, p);
    }
    productoRepository.saveAll(productos);
    logger.warn("Los Productos {} se modificaron correctamente.", productos);
  }

  @Override
  @Transactional
  public void guardarCantidadesDeSucursalNueva(Sucursal sucursal) {
    CantidadEnSucursal cantidadNueva = new CantidadEnSucursal();
    cantidadNueva.setSucursal(sucursal);
    cantidadNueva.setCantidad(BigDecimal.ZERO);
    List<Producto> productos =
      productoRepository.findAllByEliminado(false);
    productos.forEach(producto -> producto.getCantidadEnSucursales().add(cantidadNueva));
    this.productoRepository.saveAll(productos);
  }

  @Override
  @Transactional
  public void subirImagenProducto(long idProducto, byte[] imagen) {
    if (imagen.length > TAMANIO_MAXIMO_IMAGEN)
      throw new BusinessServiceException(
          messageSource.getMessage("mensaje_error_tamanio_no_valido", null, Locale.getDefault()));
    String urlImagen =
        photoVideoUploader.subirImagen(Producto.class.getSimpleName() + idProducto, imagen);
    productoRepository.actualizarUrlImagen(idProducto, urlImagen);
  }

  @Override
  public Producto getProductoNoEliminadoPorId(long idProducto) {
    Optional<Producto> producto = productoRepository.findById(idProducto);
    if (producto.isPresent() && !producto.get().isEliminado()) {
      if (producto.get().isOferta()) {
        producto
            .get()
            .setPrecioBonificado(
                producto
                    .get()
                    .getPrecioLista()
                    .multiply(
                        (new BigDecimal("100"))
                            .subtract(producto.get().getPorcentajeBonificacionOferta())
                            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)));
      }
      return producto.get();
    } else {
      throw new EntityNotFoundException(
          messageSource.getMessage("mensaje_producto_no_existente", null, Locale.getDefault()));
    }
  }

  @Override
  public Page<Producto> getProductosConPrecioBonificado(Page<Producto> productos) {
    productos.stream()
        .filter(
            producto ->
                !producto.isOferta()
                    && producto.getPorcentajeBonificacionPrecio() != null
                    && producto.getPorcentajeBonificacionPrecio().compareTo(BigDecimal.ZERO) > 0)
        .forEach(
            p ->
                p.setPrecioBonificado(
                    p.getPrecioLista()
                        .subtract(
                            p.getPrecioLista()
                                .multiply(
                                    p.getPorcentajeBonificacionPrecio()
                                        .divide(new BigDecimal("100"), RoundingMode.HALF_UP)))));
    return productos;
  }

  @Override
  public Producto getProductoPorCodigo(String codigo) {
    if (codigo.isEmpty()) {
      return null;
    } else {
      return productoRepository.findByCodigoAndEliminado(codigo, false);
    }
  }

  @Override
  public Producto getProductoPorDescripcion(String descripcion) {
    return productoRepository.findByDescripcionAndEliminado(descripcion, false);
  }

  @Override
  public BigDecimal calcularValorStock(BusquedaProductoCriteria criteria) {
    return productoRepository.calcularValorStock(this.getBuilder(criteria));
  }

  @Override
  public List<ProductoFaltanteDTO> getProductosSinStockDisponible(
      ProductosParaVerificarStockDTO productosParaVerificarStockDTO) {
    List<ProductoFaltanteDTO> productosFaltantes = new ArrayList<>();
    int longitudIds = productosParaVerificarStockDTO.getIdProducto().length;
    int longitudCantidades = productosParaVerificarStockDTO.getCantidad().length;
    if (longitudIds == longitudCantidades) {
      for (int i = 0; i < longitudIds; i++) {
        BigDecimal cantidadLambda = productosParaVerificarStockDTO.getCantidad()[i];
        Producto producto =
            this.getProductoNoEliminadoPorId(productosParaVerificarStockDTO.getIdProducto()[i]);
        producto.getCantidadEnSucursales().stream()
            .filter(
                cantidadEnSucursal ->
                    cantidadEnSucursal
                        .getIdSucursal()
                        .equals(productosParaVerificarStockDTO.getIdSucursal()))
            .forEach(
                cantidadEnSucursal -> {
                  if (!producto.isIlimitado()
                      && cantidadEnSucursal.getCantidad().compareTo(cantidadLambda) < 0) {
                    productosFaltantes.add(
                        ProductoFaltanteDTO.builder()
                            .idProducto(producto.getIdProducto())
                            .codigo(producto.getCodigo())
                            .descripcion(producto.getDescripcion())
                            .cantidadSolicitada(cantidadLambda)
                            .cantidadDisponible(cantidadEnSucursal.getCantidad())
                            .build());
                  }
                });
      }
    } else {
      throw new BusinessServiceException(
          messageSource.getMessage("mensaje_error_logitudes_arrays", null, Locale.getDefault()));
    }
    return productosFaltantes;
  }

  @Override
  public BigDecimal calcularGananciaPorcentaje(
      BigDecimal precioDeListaNuevo,
      BigDecimal precioDeListaAnterior,
      BigDecimal pvp,
      BigDecimal ivaPorcentaje,
      BigDecimal impInternoPorcentaje,
      BigDecimal precioCosto,
      boolean ascendente) {
    // evita la division por cero
    if (precioCosto.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
    BigDecimal resultado;
    if (!ascendente) {
      resultado =
          pvp.subtract(precioCosto).divide(precioCosto, 15, RoundingMode.HALF_UP).multiply(CIEN);
    } else if (precioDeListaAnterior.compareTo(BigDecimal.ZERO) == 0
        || precioCosto.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    } else {
      resultado = precioDeListaNuevo;
      BigDecimal porcentajeIncremento =
          precioDeListaNuevo.divide(precioDeListaAnterior, 15, RoundingMode.HALF_UP);
      resultado =
          resultado.subtract(
              porcentajeIncremento.multiply(
                  impInternoPorcentaje.divide(CIEN, 15, RoundingMode.HALF_UP).multiply(pvp)));
      resultado =
          resultado.subtract(
              porcentajeIncremento.multiply(
                  ivaPorcentaje.divide(CIEN, 15, RoundingMode.HALF_UP).multiply(pvp)));
      resultado =
          resultado
              .subtract(precioCosto)
              .multiply(CIEN)
              .divide(precioCosto, 15, RoundingMode.HALF_UP);
    }
    return resultado;
  }

  @Override
  public BigDecimal calcularGananciaNeto(BigDecimal precioCosto, BigDecimal gananciaPorcentaje) {
    return precioCosto.multiply(gananciaPorcentaje).divide(CIEN, 15, RoundingMode.HALF_UP);
  }

  @Override
  public BigDecimal calcularPVP(BigDecimal precioCosto, BigDecimal gananciaPorcentaje) {
    return precioCosto.add(
        precioCosto.multiply(gananciaPorcentaje.divide(CIEN, 15, RoundingMode.HALF_UP)));
  }

  @Override
  public BigDecimal calcularIVANeto(BigDecimal pvp, BigDecimal ivaPorcentaje) {
    return pvp.multiply(ivaPorcentaje).divide(CIEN, 15, RoundingMode.HALF_UP);
  }

  @Override
  public BigDecimal calcularPrecioLista(BigDecimal pvp, BigDecimal ivaPorcentaje) {
    BigDecimal resulIVA = pvp.multiply(ivaPorcentaje.divide(CIEN, 15, RoundingMode.HALF_UP));
    return pvp.add(resulIVA);
  }

  @Override
  public byte[] getListaDePrecios(List<Producto> productos, String formato) {
    ClassLoader classLoader = FacturaServiceImpl.class.getClassLoader();
    InputStream isFileReport =
        classLoader.getResourceAsStream("sic/vista/reportes/ListaPreciosProductos.jasper");
    Map<String, Object> params = new HashMap<>();
    JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(productos);
    switch (formato) {
      case "xlsx":
        try {
          return xlsReportToArray(JasperFillManager.fillReport(isFileReport, params, ds));
        } catch (JRException ex) {
          logger.error(ex.getMessage());
          throw new ServiceException(messageSource.getMessage(
            "mensaje_error_reporte", null, Locale.getDefault()), ex);
        }
      case "pdf":
        try {
          return JasperExportManager.exportReportToPdf(
              JasperFillManager.fillReport(isFileReport, params, ds));
        } catch (JRException ex) {
          logger.error(ex.getMessage());
          throw new ServiceException(messageSource.getMessage(
            "mensaje_error_reporte", null, Locale.getDefault()), ex);
        }
      default:
        throw new BusinessServiceException(messageSource.getMessage(
          "mensaje_formato_no_valido", null, Locale.getDefault()));
    }
  }

  private byte[] xlsReportToArray(JasperPrint jasperPrint) {
    byte[] bytes = null;
    try {
      JRXlsxExporter jasperXlsxExportMgr = new JRXlsxExporter();
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      SimpleOutputStreamExporterOutput simpleOutputStreamExporterOutput =
          new SimpleOutputStreamExporterOutput(out);
      jasperXlsxExportMgr.setExporterInput(new SimpleExporterInput(jasperPrint));
      jasperXlsxExportMgr.setExporterOutput(simpleOutputStreamExporterOutput);
      jasperXlsxExportMgr.exportReport();
      bytes = out.toByteArray();
      out.close();
    } catch (JRException ex) {
      logger.error(ex.getMessage());
      throw new ServiceException(messageSource.getMessage(
        "mensaje_error_reporte", null, Locale.getDefault()), ex);
    } catch (IOException ex) {
      logger.error(ex.getMessage());
    }
    return bytes;
  }

  @Override
  public List<Producto> getMultiplesProductosPorId(List<Long> idsProductos) {
    return productoRepository.findByIdProductoInOrderByIdProductoAsc(idsProductos);
  }

  private boolean contieneDuplicados(long[] array) {
      Set<Long> set = new HashSet<>();
      for (long i : array) {
        if (set.contains(i)) return true;
        set.add(i);
      }
      return false;
    }

}
