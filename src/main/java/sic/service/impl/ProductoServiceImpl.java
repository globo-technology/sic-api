package sic.service.impl;

import com.querydsl.core.BooleanBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.*;
import sic.modelo.*;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.*;
import javax.imageio.ImageIO;
import javax.persistence.EntityNotFoundException;
import javax.swing.ImageIcon;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sic.service.*;
import sic.util.Validator;
import sic.repository.ProductoRepository;

@Service
public class ProductoServiceImpl implements IProductoService {

  private final ProductoRepository productoRepository;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private static final BigDecimal CIEN = new BigDecimal("100");
  private final IEmpresaService empresaService;
  private final IRubroService rubroService;
  private final IProveedorService proveedorService;
  private final IMedidaService medidaService;

  @Autowired
  public ProductoServiceImpl(
      ProductoRepository productoRepository,
      IEmpresaService empresaService,
      IRubroService rubroService,
      IProveedorService proveedorService,
      IMedidaService medidaService) {
    this.productoRepository = productoRepository;
    this.empresaService = empresaService;
    this.rubroService = rubroService;
    this.proveedorService = proveedorService;
    this.medidaService = medidaService;
  }

  private void validarOperacion(TipoDeOperacion operacion, Producto producto) {
    // Duplicados
    // Codigo
    if (!producto.getCodigo().equals("")) {
      Producto productoDuplicado =
          this.getProductoPorCodigo(producto.getCodigo(), producto.getEmpresa().getId_Empresa());
      if (operacion.equals(TipoDeOperacion.ACTUALIZACION)
          && productoDuplicado != null
          && productoDuplicado.getId_Producto() != producto.getId_Producto()) {
        throw new BusinessServiceException(
            ResourceBundle.getBundle("Mensajes").getString("mensaje_producto_duplicado_codigo"));
      }
      if (operacion.equals(TipoDeOperacion.ALTA)
          && productoDuplicado != null
          && !producto.getCodigo().equals("")) {
        throw new BusinessServiceException(
            ResourceBundle.getBundle("Mensajes").getString("mensaje_producto_duplicado_codigo"));
      }
    }
    // Descripcion
    Producto productoDuplicado =
        this.getProductoPorDescripcion(producto.getDescripcion(), producto.getEmpresa());
    if (operacion.equals(TipoDeOperacion.ALTA) && productoDuplicado != null) {
      throw new BusinessServiceException(
          ResourceBundle.getBundle("Mensajes").getString("mensaje_producto_duplicado_descripcion"));
    }
    if (operacion.equals(TipoDeOperacion.ACTUALIZACION) && productoDuplicado != null
          && productoDuplicado.getId_Producto() != producto.getId_Producto())
        throw new BusinessServiceException(
            ResourceBundle.getBundle("Mensajes")
                .getString("mensaje_producto_duplicado_descripcion"));
    // Calculos
    Double[] iva = {10.5, 21.0, 0.0};
    if (!Arrays.asList(iva).contains(producto.getIvaPorcentaje().doubleValue())) {
      throw new BusinessServiceException(
          ResourceBundle.getBundle("Mensajes")
              .getString("mensaje_producto_ganancia_neta_incorrecta"));
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
          ResourceBundle.getBundle("Mensajes")
              .getString("mensaje_producto_ganancia_neta_incorrecta"));
    }
    if (producto
            .getPrecioVentaPublico()
            .setScale(3, RoundingMode.HALF_UP)
            .compareTo(
                this.calcularPVP(producto.getPrecioCosto(), producto.getGananciaPorcentaje())
                    .setScale(3, RoundingMode.HALF_UP))
        != 0) {
      throw new BusinessServiceException(
          ResourceBundle.getBundle("Mensajes")
              .getString("mensaje_precio_venta_publico_incorrecto"));
    }
    if (producto
            .getImpuestoInternoNeto()
            .setScale(3, RoundingMode.HALF_UP)
            .compareTo(
                this.calcularImpInternoNeto(
                        producto.getPrecioVentaPublico(), producto.getImpuestoInternoPorcentaje())
                    .setScale(3, RoundingMode.HALF_UP))
        != 0) {
      throw new BusinessServiceException(
          ResourceBundle.getBundle("Mensajes")
              .getString("mensaje_producto_impuesto_interno_neto_incorrecto"));
    }
    if (producto
            .getIvaNeto()
            .setScale(3, RoundingMode.HALF_UP)
            .compareTo(
                this.calcularIVANeto(producto.getPrecioVentaPublico(), producto.getIvaPorcentaje())
                    .setScale(3, RoundingMode.HALF_UP))
        != 0) {
      throw new BusinessServiceException(
          ResourceBundle.getBundle("Mensajes").getString("mensaje_producto_iva_neto_incorrecto"));
    }
    if (producto
            .getPrecioLista()
            .setScale(3, RoundingMode.HALF_UP)
            .compareTo(
                this.calcularPrecioLista(
                        producto.getPrecioVentaPublico(),
                        producto.getIvaPorcentaje(),
                        producto.getImpuestoInternoPorcentaje())
                    .setScale(3,  RoundingMode.HALF_UP))
        != 0) {
      throw new BusinessServiceException(
          ResourceBundle.getBundle("Mensajes")
              .getString("mensaje_producto_precio_lista_incorrecto"));
    }
  }

  @Override
  public Page<Producto> buscarProductos(BusquedaProductoCriteria criteria) {
    return productoRepository.findAll(this.getBuilder(criteria), criteria.getPageable());
  }

  private BooleanBuilder getBuilder(BusquedaProductoCriteria criteria) {
    QProducto qProducto = QProducto.producto;
    BooleanBuilder builder = new BooleanBuilder();
    builder.and(
        qProducto
            .empresa
            .id_Empresa
            .eq(criteria.getIdEmpresa())
            .and(qProducto.eliminado.eq(false)));
    if (criteria.isBuscarPorCodigo() && criteria.isBuscarPorDescripcion())
      builder.and(
          qProducto
              .codigo
              .containsIgnoreCase(criteria.getCodigo())
              .or(this.buildPredicadoDescripcion(criteria.getDescripcion(), qProducto)));
    else {
      if (criteria.isBuscarPorCodigo())
        builder.and(qProducto.codigo.containsIgnoreCase(criteria.getCodigo()));
      if (criteria.isBuscarPorDescripcion())
        builder.and(this.buildPredicadoDescripcion(criteria.getDescripcion(), qProducto));
    }
    if (criteria.isBuscarPorRubro())
      builder.and(qProducto.rubro.id_Rubro.eq(criteria.getIdRubro()));
    if (criteria.isBuscarPorProveedor())
      builder.and(qProducto.proveedor.id_Proveedor.eq(criteria.getIdProveedor()));
    if (criteria.isListarSoloFaltantes())
      builder.and(qProducto.cantidad.loe(qProducto.cantMinima)).and(qProducto.ilimitado.eq(false));
    if (criteria.isBuscaPorVisibilidad())
      if (criteria.getPublico()) builder.and(qProducto.publico.isTrue());
      else builder.and(qProducto.publico.isFalse());
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
  public Producto guardar(
      Producto producto, long idMedida, long idRubro, long idProveedor, long idEmpresa) {
    if (producto.getCodigo() == null) producto.setCodigo("");
    producto.setMedida(medidaService.getMedidaPorId(idMedida));
    producto.setRubro(rubroService.getRubroPorId(idRubro));
    producto.setProveedor(proveedorService.getProveedorPorId(idProveedor));
    producto.setEmpresa(empresaService.getEmpresaPorId(idEmpresa));
    this.validarOperacion(TipoDeOperacion.ALTA, producto);
    producto.setFechaAlta(new Date());
    producto.setFechaUltimaModificacion(new Date());
    producto = productoRepository.save(producto);
    logger.warn("El Producto " + producto + " se guardó correctamente.");
    return producto;
  }

  @Override
  @Transactional
  public void actualizar(
      Producto productoPorActualizar,
      Long idMedida,
      Long idRubro,
      Long idProveedor,
      Long idEmpresa) {
    Producto productoPersistido = this.getProductoPorId(productoPorActualizar.getId_Producto());
    if (idMedida != null) productoPorActualizar.setMedida(medidaService.getMedidaPorId(idMedida));
    else productoPorActualizar.setMedida(productoPersistido.getMedida());
    if (idRubro != null) productoPorActualizar.setRubro(rubroService.getRubroPorId(idRubro));
    else productoPorActualizar.setRubro(productoPersistido.getRubro());
    if (idProveedor != null)
      productoPorActualizar.setProveedor(proveedorService.getProveedorPorId(idProveedor));
    else productoPorActualizar.setProveedor(productoPersistido.getProveedor());
    if (idEmpresa != null)
      productoPorActualizar.setEmpresa(empresaService.getEmpresaPorId(idEmpresa));
    else productoPorActualizar.setEmpresa(productoPersistido.getEmpresa());
    this.validarOperacion(TipoDeOperacion.ACTUALIZACION, productoPorActualizar);
    productoPorActualizar.setFechaUltimaModificacion(new Date());
    productoRepository.save(productoPorActualizar);
    logger.warn("El Producto " + productoPorActualizar + " se modificó correctamente.");
  }

  @Override
  public void actualizarStock(
      Map<Long, BigDecimal> idsYCantidades, TipoDeOperacion operacion, Movimiento movimiento) {
    idsYCantidades
        .entrySet()
        .forEach(
            entry -> {
              Producto producto = productoRepository.findById(entry.getKey());
              if (producto == null) {
                logger.warn("Se intenta actualizar el stock de un producto eliminado.");
              }
              if (producto != null && !producto.isIlimitado()) {
                if (movimiento.equals(Movimiento.VENTA)) {
                  if (operacion == TipoDeOperacion.ALTA) {
                    producto.setCantidad(producto.getCantidad().subtract(entry.getValue()));
                  }
                  if (operacion == TipoDeOperacion.ELIMINACION
                      || operacion == TipoDeOperacion.ACTUALIZACION) {
                    producto.setCantidad(producto.getCantidad().add(entry.getValue()));
                  }
                } else if (movimiento.equals(Movimiento.COMPRA)) {
                  if (operacion == TipoDeOperacion.ALTA) {
                    producto.setCantidad(producto.getCantidad().add(entry.getValue()));
                  }
                  if (operacion == TipoDeOperacion.ELIMINACION) {
                    BigDecimal result = producto.getCantidad().subtract(entry.getValue());
                    if (result.compareTo(BigDecimal.ZERO) < 0) {
                      result = BigDecimal.ZERO;
                    }
                    producto.setCantidad(result);
                  }
                }
                productoRepository.save(producto);
              }
            });
  }

  @Override
  @Transactional
  public void eliminarMultiplesProductos(long[] idProducto) {
    if (Validator.tieneDuplicados(idProducto)) {
      throw new BusinessServiceException(
          ResourceBundle.getBundle("Mensajes").getString("mensaje_error_ids_duplicados"));
    }
    List<Producto> productos = new ArrayList<>();
    for (Long i : idProducto) {
      Producto producto = this.getProductoPorId(i);
      if (producto == null) {
        throw new EntityNotFoundException(
            ResourceBundle.getBundle("Mensajes").getString("mensaje_producto_no_existente"));
      }
      producto.setEliminado(true);
      productos.add(producto);
    }
    productoRepository.save(productos);
  }

  @Override
  @Transactional
  public List<Producto> actualizarMultiples(
      long[] idProducto,
      boolean checkPrecios,
      boolean checkDescuentoRecargoPorcentaje,
      BigDecimal descuentoRecargoPorcentaje,
      BigDecimal gananciaNeto,
      BigDecimal gananciaPorcentaje,
      BigDecimal impuestoInternoNeto,
      BigDecimal impuestoInternoPorcentaje,
      BigDecimal ivaNeto,
      BigDecimal ivaPorcentaje,
      BigDecimal precioCosto,
      BigDecimal precioLista,
      BigDecimal precioVentaPublico,
      boolean checkMedida,
      Long idMedida,
      boolean checkRubro,
      Long idRubro,
      boolean checkProveedor,
      Long idProveedor,
      boolean checkVisibilidad,
      Boolean publico) {
    // Requeridos
    if (Validator.tieneDuplicados(idProducto)) {
      throw new BusinessServiceException(
          ResourceBundle.getBundle("Mensajes").getString("mensaje_error_ids_duplicados"));
    }
    List<Producto> productos = new ArrayList<>();
    for (long i : idProducto) {
      productos.add(this.getProductoPorId(i));
    }
    BigDecimal multiplicador = BigDecimal.ZERO;
    if (checkDescuentoRecargoPorcentaje) {
      if (descuentoRecargoPorcentaje.compareTo(BigDecimal.ZERO) > 0) {
        multiplicador =
            descuentoRecargoPorcentaje.divide(CIEN, 15, RoundingMode.HALF_UP).add(BigDecimal.ONE);
      } else {
        multiplicador =
            BigDecimal.ONE.subtract(
                descuentoRecargoPorcentaje.abs().divide(CIEN, 15, RoundingMode.HALF_UP));
      }
    }
    for (Producto p : productos) {
      if (checkMedida) {
        Medida medida = medidaService.getMedidaPorId(idMedida);
        p.setMedida(medida);
      }
      if (checkRubro) {
        Rubro rubro = rubroService.getRubroPorId(idRubro);
        p.setRubro(rubro);
      }
      if (checkProveedor) {
        Proveedor proveedor = proveedorService.getProveedorPorId(idProveedor);
        p.setProveedor(proveedor);
      }
      if (checkPrecios) {
        p.setPrecioCosto(precioCosto);
        p.setGananciaPorcentaje(gananciaPorcentaje);
        p.setGananciaNeto(gananciaNeto);
        p.setPrecioVentaPublico(precioVentaPublico);
        p.setIvaPorcentaje(ivaPorcentaje);
        p.setIvaNeto(ivaNeto);
        p.setImpuestoInternoPorcentaje(impuestoInternoPorcentaje);
        p.setImpuestoInternoNeto(impuestoInternoNeto);
        p.setPrecioLista(precioLista);
      }
      if (checkDescuentoRecargoPorcentaje) {
        p.setPrecioCosto(p.getPrecioCosto().multiply(multiplicador));
        p.setGananciaNeto(p.getGananciaNeto().multiply(multiplicador));
        p.setPrecioVentaPublico(p.getPrecioVentaPublico().multiply(multiplicador));
        p.setIvaNeto(p.getIvaNeto().multiply(multiplicador));
        p.setImpuestoInternoNeto(p.getImpuestoInternoNeto().multiply(multiplicador));
        p.setPrecioLista(p.getPrecioLista().multiply(multiplicador));
        p.setFechaUltimaModificacion(new Date());
      }
      if (checkMedida || checkRubro || checkProveedor || checkPrecios) {
        p.setFechaUltimaModificacion(new Date());
      }
      if (checkVisibilidad) p.setPublico(publico);
      this.validarOperacion(TipoDeOperacion.ACTUALIZACION, p);
    }
    productoRepository.save(productos);
    logger.warn("Los Productos " + productos + " se modificaron correctamente.");
    return productos;
  }

  @Override
  public Producto getProductoPorId(long idProducto) {
    Producto producto = productoRepository.findOne(idProducto);
    if (producto == null) {
      throw new EntityNotFoundException(
          ResourceBundle.getBundle("Mensajes").getString("mensaje_producto_no_existente"));
    }
    return producto;
  }

  @Override
  public Producto getProductoPorCodigo(String codigo, long idEmpresa) {
    if (codigo.isEmpty()) {
      return null;
    } else {
      Empresa empresa = empresaService.getEmpresaPorId(idEmpresa);
      return productoRepository.findByCodigoAndEmpresaAndEliminado(codigo, empresa, false);
    }
  }

  @Override
  public Producto getProductoPorDescripcion(String descripcion, Empresa empresa) {
    return productoRepository.findByDescripcionAndEmpresaAndEliminado(descripcion, empresa, false);
  }

  @Override
  public BigDecimal calcularValorStock(BusquedaProductoCriteria criteria) {
    return productoRepository.calcularValorStock(this.getBuilder(criteria));
  }

  @Override
  public Map<Long, BigDecimal> getProductosSinStockDisponible(
      long[] idProducto, BigDecimal[] cantidad) {
    Map<Long, BigDecimal> productos = new HashMap<>();
    int longitudIds = idProducto.length;
    int longitudCantidades = cantidad.length;
    if (longitudIds == longitudCantidades) {
      for (int i = 0; i < longitudIds; i++) {
        Producto p = this.getProductoPorId(idProducto[i]);
        if (!p.isIlimitado() && p.getCantidad().compareTo(cantidad[i]) < 0) {
          productos.put(p.getId_Producto(), cantidad[i]);
        }
      }
    } else {
      throw new BusinessServiceException(
          ResourceBundle.getBundle("Mensajes").getString("mensaje_error_logitudes_arrays"));
    }
    return productos;
  }

  @Override
  public Map<Long, BigDecimal> getProductosNoCumplenCantidadVentaMinima(
      long[] idProducto, BigDecimal[] cantidad) {
    Map<Long, BigDecimal> productos = new HashMap<>();
    int longitudIds = idProducto.length;
    int longitudCantidades = cantidad.length;
    if (longitudIds == longitudCantidades) {
      for (int i = 0; i < longitudIds; i++) {
        Producto p = this.getProductoPorId(idProducto[i]);
        if (p.getVentaMinima().compareTo(cantidad[i]) > 0) {
          productos.put(p.getId_Producto(), cantidad[i]);
        }
      }
    } else {
      throw new BusinessServiceException(
          ResourceBundle.getBundle("Mensajes").getString("mensaje_error_logitudes_arrays"));
    }
    return productos;
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
  public BigDecimal calcularImpInternoNeto(BigDecimal pvp, BigDecimal impInternoPorcentaje) {
    return pvp.multiply(impInternoPorcentaje).divide(CIEN, 15, RoundingMode.HALF_UP);
  }

  @Override
  public BigDecimal calcularPrecioLista(
      BigDecimal pvp, BigDecimal ivaPorcentaje, BigDecimal impInternoPorcentaje) {
    BigDecimal resulIVA = pvp.multiply(ivaPorcentaje.divide(CIEN, 15, RoundingMode.HALF_UP));
    BigDecimal resultImpInterno =
        pvp.multiply(impInternoPorcentaje.divide(CIEN, 15, RoundingMode.HALF_UP));
    return pvp.add(resulIVA).add(resultImpInterno);
  }

  @Override
  public byte[] getListaDePreciosPorEmpresa(
      List<Producto> productos, long idEmpresa, String formato) {
    Empresa empresa = empresaService.getEmpresaPorId(idEmpresa);
    ClassLoader classLoader = FacturaServiceImpl.class.getClassLoader();
    InputStream isFileReport =
        classLoader.getResourceAsStream("sic/vista/reportes/ListaPreciosProductos.jasper");
    Map<String, Object> params = new HashMap<>();
    params.put("empresa", empresa);
    if (!empresa.getLogo().isEmpty()) {
      try {
        params.put("logo", new ImageIcon(ImageIO.read(new URL(empresa.getLogo()))).getImage());
      } catch (IOException ex) {
        logger.error(ex.getMessage());
        throw new ServiceException(
            ResourceBundle.getBundle("Mensajes").getString("mensaje_empresa_404_logo"), ex);
      }
    }
    JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(productos);
    switch (formato) {
      case "xlsx":
        try {
          return xlsReportToArray(JasperFillManager.fillReport(isFileReport, params, ds));
        } catch (JRException ex) {
          logger.error(ex.getMessage());
          throw new ServiceException(
              ResourceBundle.getBundle("Mensajes").getString("mensaje_error_reporte"), ex);
        }
      case "pdf":
        try {
          return JasperExportManager.exportReportToPdf(
              JasperFillManager.fillReport(isFileReport, params, ds));
        } catch (JRException ex) {
          logger.error(ex.getMessage());
          throw new ServiceException(
              ResourceBundle.getBundle("Mensajes").getString("mensaje_error_reporte"), ex);
        }
      default:
        throw new BusinessServiceException(
            ResourceBundle.getBundle("Mensajes").getString("mensaje_formato_no_valido"));
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
      throw new ServiceException(
          ResourceBundle.getBundle("Mensajes").getString("mensaje_error_reporte"), ex);
    } catch (IOException ex) {
      logger.error(ex.getMessage());
    }
    return bytes;
  }
}
