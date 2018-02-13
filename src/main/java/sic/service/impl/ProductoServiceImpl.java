package sic.service.impl;

import com.querydsl.core.BooleanBuilder;
import java.io.IOException;
import sic.modelo.BusquedaProductoCriteria;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
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
import sic.modelo.Empresa;
import sic.modelo.Medida;
import sic.modelo.Movimiento;
import sic.modelo.Producto;
import sic.modelo.Proveedor;
import sic.modelo.QProducto;
import sic.modelo.Rubro;
import sic.service.IProductoService;
import sic.service.BusinessServiceException;
import sic.service.ServiceException;
import sic.modelo.TipoDeOperacion;
import sic.util.Validator;
import sic.repository.ProductoRepository;

@Service
public class ProductoServiceImpl implements IProductoService {

    private final ProductoRepository productoRepository;
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public ProductoServiceImpl(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    private void validarOperacion(TipoDeOperacion operacion, Producto producto) {
        //Entrada de Datos
        if (producto.getCantidad().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_producto_cantidad_negativa"));
        }
        if (producto.getCantMinima().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_producto_cantidadMinima_negativa"));
        }
        if (producto.getVentaMinima().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_producto_cantidadVentaMinima_invalida"));
        }
        if (producto.getPrecioCosto().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_producto_precioCosto_negativo"));
        }
        if (producto.getPrecioVentaPublico().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_producto_precioVentaPublico_negativo"));
        }
        if (producto.getIva_porcentaje().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_producto_IVAPorcentaje_negativo"));
        }
        if (producto.getImpuestoInterno_porcentaje().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_producto_ImpInternoPorcentaje_negativo"));
        }
        if (producto.getGanancia_porcentaje().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_producto_gananciaPorcentaje_negativo"));
        }
        if (producto.getPrecioLista().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_producto_precioLista_negativo"));
        }
        //Requeridos
        if (Validator.esVacio(producto.getDescripcion())) {
            throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_producto_vacio_descripcion"));
        }
        if (producto.getMedida() == null) {
            throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_producto_vacio_medida"));
        }
        if (producto.getRubro() == null) {
            throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_producto_vacio_rubro"));
        }
        if (producto.getProveedor() == null) {
            throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_producto_vacio_proveedor"));
        }
        if (producto.getEmpresa() == null) {
            throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_producto_vacio_empresa"));
        }
        //Duplicados
        //Codigo
        if (!producto.getCodigo().equals("")) {
            Producto productoDuplicado = this.getProductoPorCodigo(producto.getCodigo(), producto.getEmpresa());
            if (operacion.equals(TipoDeOperacion.ACTUALIZACION)
                    && productoDuplicado != null
                    && productoDuplicado.getId_Producto() != producto.getId_Producto()) {
                throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                        .getString("mensaje_producto_duplicado_codigo"));
            }
            if (operacion.equals(TipoDeOperacion.ALTA)
                    && productoDuplicado != null
                    && !producto.getCodigo().equals("")) {
                throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                        .getString("mensaje_producto_duplicado_codigo"));
            }
        }
        //Descripcion
        Producto productoDuplicado = this.getProductoPorDescripcion(producto.getDescripcion(), producto.getEmpresa());
        if (operacion.equals(TipoDeOperacion.ALTA) && productoDuplicado != null) {
            throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_producto_duplicado_descripcion"));
        }
        if (operacion.equals(TipoDeOperacion.ACTUALIZACION)) {
            if (productoDuplicado != null && productoDuplicado.getId_Producto() != producto.getId_Producto()) {
                throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                        .getString("mensaje_producto_duplicado_descripcion"));
            }
        }
    }

    @Override
    public Page<Producto> buscarProductos(BusquedaProductoCriteria criteria) {
        //Empresa
        if (criteria.getEmpresa() == null) {
            throw new EntityNotFoundException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_empresa_no_existente"));
        }
        //Rubro
        if (criteria.isBuscarPorRubro() == true && criteria.getRubro() == null) {
            throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_producto_vacio_rubro"));
        }
        //Proveedor
        if (criteria.isBuscarPorProveedor() == true && criteria.getProveedor() == null) {
            throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_producto_vacio_proveedor"));
        }
        QProducto qproducto = QProducto.producto;
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(qproducto.empresa.eq(criteria.getEmpresa()).and(qproducto.eliminado.eq(false)));
        if (criteria.isBuscarPorCodigo() == true && criteria.isBuscarPorDescripcion() == true) {
            builder.and(qproducto.codigo.containsIgnoreCase(criteria.getCodigo())
                    .or(this.buildPredicadoDescripcion(criteria.getDescripcion(), qproducto)));            
        } else {
            if (criteria.isBuscarPorCodigo() == true) {
                builder.and(qproducto.codigo.containsIgnoreCase(criteria.getCodigo()));
            }
            if (criteria.isBuscarPorDescripcion() == true) {
                builder.and(this.buildPredicadoDescripcion(criteria.getDescripcion(), qproducto));
            }
        }
        if (criteria.isBuscarPorRubro() == true) {
            builder.and(qproducto.rubro.eq(criteria.getRubro()));
        }
        if (criteria.isBuscarPorProveedor()) {
            builder.and(qproducto.proveedor.eq(criteria.getProveedor()));
        }
        if (criteria.isListarSoloFaltantes() == true) {
            builder.and(qproducto.cantidad.loe(qproducto.cantMinima)).and(qproducto.ilimitado.eq(false));
        }
        int pageNumber = 0;
        int pageSize = Integer.MAX_VALUE;        
        if (criteria.getPageable() != null) {
            pageNumber = criteria.getPageable().getPageNumber();
            pageSize = criteria.getPageable().getPageSize();
        }
        Pageable pageable = new PageRequest(pageNumber, pageSize, new Sort(Sort.Direction.ASC, "descripcion"));
        return productoRepository.findAll(builder, pageable);
    }
    
    private BooleanBuilder buildPredicadoDescripcion(String descripcion, QProducto qproducto) {
        String[] terminos = descripcion.split(" ");
        BooleanBuilder descripcionProducto = new BooleanBuilder();
        for (String termino : terminos) {
            descripcionProducto.and(qproducto.descripcion.containsIgnoreCase(termino));
        }
        return descripcionProducto;
    }

    @Override
    @Transactional
    public Producto guardar(Producto producto) {
        if (producto.getCodigo() == null) {
            producto.setCodigo("");
        }
        this.validarOperacion(TipoDeOperacion.ALTA, producto);
        producto = productoRepository.save(producto);
        LOGGER.warn("El Producto " + producto + " se guardó correctamente.");
        return producto;
    }

    @Override
    @Transactional
    public void actualizar(Producto producto) {
        this.validarOperacion(TipoDeOperacion.ACTUALIZACION, producto);
        productoRepository.save(producto);
        LOGGER.warn("El Producto " + producto + " se modificó correctamente.");
    }

    @Override
    public void actualizarStock(HashMap<Long, BigDecimal> idsYCantidades, TipoDeOperacion operacion, Movimiento movimiento) {
        idsYCantidades.entrySet().forEach(entry -> {
            Producto producto = productoRepository.findById(entry.getKey());
            if (producto == null) {
                LOGGER.warn("Se intenta actualizar el stock de un producto eliminado.");
            }
            if (producto != null && producto.isIlimitado() == false) {
                if (movimiento.equals(Movimiento.VENTA)) {
                    if (operacion == TipoDeOperacion.ALTA) {
                        producto.setCantidad(producto.getCantidad().subtract(entry.getValue()));
                    }

                    if (operacion == TipoDeOperacion.ELIMINACION || operacion == TipoDeOperacion.ACTUALIZACION) {
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
            throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_error_ids_duplicados"));
        }
        List<Producto> productos = new ArrayList<>();
        for (Long i : idProducto) {
            Producto producto = this.getProductoPorId(i);
            if (producto == null) {
                throw new EntityNotFoundException(ResourceBundle.getBundle("Mensajes")
                        .getString("mensaje_producto_no_existente"));
            }
            producto.setEliminado(true);
            productos.add(producto);
        }
        productoRepository.save(productos);
    }

    @Override
    @Transactional
    public List<Producto> modificarMultiplesProductos(long[] idProducto,
            boolean checkPrecios,
            BigDecimal gananciaNeto,
            BigDecimal gananciaPorcentaje,
            BigDecimal impuestoInternoNeto,
            BigDecimal impuestoInternoPorcentaje,
            BigDecimal IVANeto,
            BigDecimal IVAPorcentaje,
            BigDecimal precioCosto,
            BigDecimal precioLista,
            BigDecimal precioVentaPublico,
            boolean checkMedida, Medida medida,
            boolean checkRubro, Rubro rubro,
            boolean checkProveedor, Proveedor proveedor) {
        if (Validator.tieneDuplicados(idProducto)) {
            throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_error_ids_duplicados"));
        }
        List<Producto> productos = new ArrayList<>();
        for (long i : idProducto) {
            productos.add(this.getProductoPorId(i));
        }
        //Requeridos
        if (checkMedida == true && medida == null) {
            throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_producto_vacio_medida"));
        }
        if (checkRubro == true && rubro == null) {
            throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_producto_vacio_rubro"));
        }
        if (checkProveedor == true && proveedor == null) {
            throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_producto_vacio_proveedor"));
        }        
        productos.stream().forEach(p -> {
            if (checkMedida == true) p.setMedida(medida);
            if (checkRubro == true) p.setRubro(rubro);
            if (checkProveedor == true) p.setProveedor(proveedor);           
            if (checkPrecios == true) {            
                p.setPrecioCosto(precioCosto);
                p.setGanancia_porcentaje(gananciaPorcentaje);
                p.setGanancia_neto(gananciaNeto);
                p.setPrecioVentaPublico(precioVentaPublico);
                p.setIva_porcentaje(IVAPorcentaje);
                p.setIva_neto(IVANeto);
                p.setImpuestoInterno_porcentaje(impuestoInternoPorcentaje);
                p.setImpuestoInterno_neto(impuestoInternoNeto);
                p.setPrecioLista(precioLista);            
            }
            if (checkPrecios == true || checkMedida == true || checkRubro == true || checkProveedor == true) {            
                p.setFechaUltimaModificacion(new Date());                
            }
        });        
        productoRepository.save(productos);
        LOGGER.warn("Los Productos " + productos + " se modificaron correctamente.");
        return productos;
    }

    @Override
    public Producto getProductoPorId(long idProducto) {
        Producto producto = productoRepository.findOne(idProducto);
        if (producto == null) {
            throw new EntityNotFoundException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_producto_no_existente"));
        }
        return producto;
    }

    @Override
    public Producto getProductoPorCodigo(String codigo, Empresa empresa) {
        if (codigo.isEmpty() == true || empresa == null) {
            return null;
        } else {
            return productoRepository.findByCodigoAndEmpresaAndEliminado(codigo, empresa, false);
        }
    }

    @Override
    public Producto getProductoPorDescripcion(String descripcion, Empresa empresa) {
        return productoRepository.findByDescripcionAndEmpresaAndEliminado(descripcion, empresa, false);
    }

    @Override
    public BigDecimal calcularValorStock(BusquedaProductoCriteria criteria) {
        return productoRepository.calcularValorStock(criteria);
    }

    @Override
    public Map<BigDecimal, Producto> getProductosSinStockDisponible(long[] idProducto, BigDecimal[] cantidad) {
        Map productos = new HashMap();
        int longitudIds = idProducto.length;
        int longitudCantidades = cantidad.length;
        if (longitudIds == longitudCantidades) {
            for (int i = 0; i < longitudIds; i++) {
                Producto p = this.getProductoPorId(idProducto[i]);
                if (p.isIlimitado() == false && p.getCantidad().compareTo(cantidad[i]) < 0) {
                    productos.put(p.getId_Producto(), cantidad[i]);
                }
            }
        } else {
            throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_error_logitudes_arrays"));
        }
        return productos;
    }

    @Override
    public Map<BigDecimal, Producto> getProductosNoCumplenCantidadVentaMinima(long[] idProducto, BigDecimal[] cantidad) {
        Map productos = new HashMap();
        int longitudIds = idProducto.length;
        int longitudCantidades = cantidad.length;
        if (longitudIds == longitudCantidades) {
            for (int i = 0; i < longitudIds; i++) {
                Producto p = this.getProductoPorId(idProducto[i]);
                if (p.getVentaMinima().compareTo(cantidad[i]) > 0) {
                    productos.put(cantidad[i], p);
                }
            }
        } else {
            throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_error_logitudes_arrays"));
        }
        return productos;
    }

    @Override
    public BigDecimal calcularGanancia_Porcentaje(BigDecimal precioDeListaNuevo,
            BigDecimal precioDeListaAnterior, BigDecimal pvp, BigDecimal ivaPorcentaje,
            BigDecimal impInternoPorcentaje, BigDecimal precioCosto, boolean ascendente) {
        //evita la division por cero
        if (precioCosto.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal resultado;
        if (ascendente == false) {
            resultado = pvp.subtract(precioCosto).divide(precioCosto, 16, RoundingMode.HALF_UP).multiply(new BigDecimal(100)); 
        } else if (precioDeListaAnterior.compareTo(BigDecimal.ZERO) == 0 || precioCosto.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        } else {
            resultado = precioDeListaNuevo;
            BigDecimal porcentajeIncremento = precioDeListaNuevo.divide(precioDeListaAnterior);
            resultado = resultado.subtract(porcentajeIncremento.multiply(impInternoPorcentaje.divide(new BigDecimal(100)).multiply(pvp)));
            resultado = resultado.subtract(porcentajeIncremento.multiply(ivaPorcentaje.divide(new BigDecimal(100)).multiply(pvp)));
            resultado = resultado.subtract(precioCosto).multiply(new BigDecimal(100)).divide(precioCosto);
        }
        return resultado;
    }

    @Override
    public BigDecimal calcularGanancia_Neto(BigDecimal precioCosto, BigDecimal ganancia_porcentaje) {
        return precioCosto.multiply(ganancia_porcentaje).divide(new BigDecimal(100));
    }

    @Override
    public BigDecimal calcularPVP(BigDecimal precioCosto, BigDecimal ganancia_porcentaje) {
        return precioCosto.add(precioCosto.multiply(ganancia_porcentaje.divide(new BigDecimal(100))));
    }

    @Override
    public BigDecimal calcularIVA_Neto(BigDecimal pvp, BigDecimal iva_porcentaje) {
        return pvp.multiply(iva_porcentaje).divide(new BigDecimal(100));
    }

    @Override
    public BigDecimal calcularImpInterno_Neto(BigDecimal pvp, BigDecimal impInterno_porcentaje) {
        return pvp.multiply(impInterno_porcentaje).divide(new BigDecimal(100));
    }

    @Override
    public BigDecimal calcularPrecioLista(BigDecimal PVP, BigDecimal iva_porcentaje, BigDecimal impInterno_porcentaje) {
        BigDecimal resulIVA = PVP.multiply(iva_porcentaje.divide(new BigDecimal(100)));
        BigDecimal resultImpInterno = PVP.multiply(impInterno_porcentaje.divide(new BigDecimal(100)));
        return PVP.add(resulIVA).add(resultImpInterno);
    }

    @Override
    public byte[] getReporteListaDePreciosPorEmpresa(List<Producto> productos, Empresa empresa) {
        ClassLoader classLoader = FacturaServiceImpl.class.getClassLoader();
        InputStream isFileReport = classLoader.getResourceAsStream("sic/vista/reportes/ListaPreciosProductos.jasper");        
        Map params = new HashMap();
        params.put("empresa", empresa);
        if (!empresa.getLogo().isEmpty()) {
            try {
                params.put("logo", new ImageIcon(ImageIO.read(new URL(empresa.getLogo()))).getImage());
            } catch (IOException ex) {
                LOGGER.error(ex.getMessage());
                throw new ServiceException(ResourceBundle.getBundle("Mensajes")
                        .getString("mensaje_empresa_404_logo"), ex);
            }
        }
        JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(productos);
        try {
            return JasperExportManager.exportReportToPdf(JasperFillManager.fillReport(isFileReport, params, ds));
        } catch (JRException ex) {
            LOGGER.error(ex.getMessage());
            throw new ServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_error_reporte"), ex);
        }
    }
    
}
