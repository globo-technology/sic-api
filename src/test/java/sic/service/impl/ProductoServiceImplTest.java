package sic.service.impl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import sic.builder.ProductoBuilder;
import sic.exception.BusinessServiceException;
import sic.modelo.*;
import sic.modelo.criteria.BusquedaProductoCriteria;
import sic.modelo.dto.NuevoProductoDTO;
import sic.repository.ProductoRepository;
import sic.service.IMedidaService;
import sic.service.IProveedorService;
import sic.service.IRubroService;
import sic.service.ISucursalService;

import javax.persistence.EntityNotFoundException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AppTest.class)
class ProductoServiceImplTest {

  @Autowired private MessageSource messageSourceTest;

  @Mock private IMedidaService medidaService;
  @Mock private IRubroService rubroService;
  @Mock private IProveedorService proveedorService;
  @Mock private ISucursalService sucursalService;
  @Mock private ProductoRepository productoRepository;
  @Mock private MessageSource messageSourceTestMock;

  @InjectMocks private ProductoServiceImpl productoService;

  @Test
  void shouldCalcularGananciaPorcentajeDescendente() {
    BigDecimal precioCosto = new BigDecimal("12.34");
    BigDecimal pvp = new BigDecimal("23.45");
    BigDecimal resultadoEsperado = new BigDecimal("90.032414910859000");
    BigDecimal resultadoObtenido =
        productoService.calcularGananciaPorcentaje(null, null, pvp, null, null, precioCosto, false);
    assertEquals(resultadoEsperado, resultadoObtenido);
  }

  @Test
  void shouldCalcularGananciaPorcentajeAscendente() {
    BigDecimal precioCosto = new BigDecimal("12.34");
    BigDecimal pvp = new BigDecimal("23.45");
    BigDecimal resultadoEsperado = new BigDecimal("95.223662884927066");
    BigDecimal resultadoObtenido =
        productoService.calcularGananciaPorcentaje(
            new BigDecimal("30"),
            new BigDecimal("25"),
            pvp,
            new BigDecimal("21"),
            BigDecimal.ZERO,
            precioCosto,
            true);
    assertEquals(resultadoEsperado, resultadoObtenido);
  }

  @Test
  void shouldCalcularGananciaNeto() {
    BigDecimal precioCosto = new BigDecimal("12.34");
    BigDecimal gananciaPorcentaje = new BigDecimal("100");
    BigDecimal resultadoEsperado = new BigDecimal("12.340000000000000");
    BigDecimal resultadoObtenido =
        productoService.calcularGananciaNeto(precioCosto, gananciaPorcentaje);
    assertEquals(resultadoEsperado, resultadoObtenido);
  }

  @Test
  void shouldcalcularCalcularPVP() {
    BigDecimal precioCosto = new BigDecimal("12.34");
    BigDecimal gananciaPorcentaje = new BigDecimal("100");
    BigDecimal resultadoEsperado = new BigDecimal("24.68000000000000000");
    BigDecimal resultadoObtenido = productoService.calcularPVP(precioCosto, gananciaPorcentaje);
    assertEquals(resultadoEsperado, resultadoObtenido);
  }

  @Test
  void shouldCalcularIVANeto() {
    BigDecimal pvp = new BigDecimal("24.68");
    BigDecimal ivaPorcentaje = new BigDecimal("21");
    BigDecimal resultadoEsperado = new BigDecimal("5.182800000000000");
    BigDecimal resultadoObtenido = productoService.calcularIVANeto(pvp, ivaPorcentaje);
    assertEquals(resultadoEsperado, resultadoObtenido);
  }

  @Test
  void shouldCalcularPrecioLista() {
    BigDecimal pvp = new BigDecimal("24.68");
    BigDecimal ivaPorcentaje = new BigDecimal("21");
    BigDecimal resultadoEsperado = new BigDecimal("29.86280000000000000");
    BigDecimal resultadoObtenido = productoService.calcularPrecioLista(pvp, ivaPorcentaje);
    assertEquals(resultadoEsperado, resultadoObtenido);
  }

  @Test
  public void shouldThrownBusinessExceptionAltaProductoCodigoDuplicado() {
    NuevoProductoDTO nuevoProductoUno =
        NuevoProductoDTO.builder()
            .descripcion("Ventilador de pie")
            .cantidadEnSucursal(
                new HashMap<Long, BigDecimal>() {
                  {
                    put(1L, BigDecimal.TEN);
                  }
                })
            .bulto(BigDecimal.ONE)
            .precioCosto(new BigDecimal("100"))
            .gananciaPorcentaje(new BigDecimal("900"))
            .gananciaNeto(new BigDecimal("900"))
            .precioVentaPublico(new BigDecimal("1000"))
            .ivaPorcentaje(new BigDecimal("21.0"))
            .ivaNeto(new BigDecimal("210"))
            .precioLista(new BigDecimal("1210"))
            .porcentajeBonificacionPrecio(new BigDecimal("20"))
            .porcentajeBonificacionOferta(new BigDecimal("-1"))
            .oferta(true)
            .imagen((new String("imagen")).getBytes())
            .codigo("12345")
            .build();
    when(proveedorService.getProveedorNoEliminadoPorId(1L)).thenReturn(new Proveedor());
    when(rubroService.getRubroNoEliminadoPorId(1L)).thenReturn(new Rubro());
    when(medidaService.getMedidaNoEliminadaPorId(1L)).thenReturn(new Medida());
    Sucursal[] sucursales = new Sucursal[] {new Sucursal()};
    when(sucursalService.getSucusales(false)).thenReturn(Arrays.asList(sucursales));
    when(productoRepository.findByDescripcionAndEliminado("Ventilador de pie", false))
        .thenReturn(null);
    Producto productoDuplicado = new Producto();
    productoDuplicado.setDescripcion("12345");
    when(productoRepository.findByCodigoAndEliminado("12345", false)).thenReturn(productoDuplicado);
    when(messageSourceTestMock.getMessage(
            "mensaje_producto_duplicado_codigo", null, Locale.getDefault()))
        .thenReturn(
            messageSourceTest.getMessage(
                "mensaje_producto_duplicado_codigo", null, Locale.getDefault()));
    BusinessServiceException thrown =
        assertThrows(
            BusinessServiceException.class,
            () -> productoService.guardar(nuevoProductoUno, 1L, 1L, 1L));
    assertNotNull(thrown.getMessage());
    assertTrue(
        thrown
            .getMessage()
            .contains(
                messageSourceTest.getMessage(
                    "mensaje_producto_duplicado_codigo", null, Locale.getDefault())));
  }

  @Test
  public void shouldThrownBusinessExceptionAltaProductoDescripcionDuplicado() {
    NuevoProductoDTO nuevoProductoUno =
        NuevoProductoDTO.builder()
            .descripcion("Ventilador de pie")
            .cantidadEnSucursal(
                new HashMap<Long, BigDecimal>() {
                  {
                    put(1L, BigDecimal.TEN);
                  }
                })
            .bulto(BigDecimal.ONE)
            .precioCosto(new BigDecimal("100"))
            .gananciaPorcentaje(new BigDecimal("900"))
            .gananciaNeto(new BigDecimal("900"))
            .precioVentaPublico(new BigDecimal("1000"))
            .ivaPorcentaje(new BigDecimal("21.0"))
            .ivaNeto(new BigDecimal("210"))
            .precioLista(new BigDecimal("1210"))
            .porcentajeBonificacionPrecio(new BigDecimal("20"))
            .porcentajeBonificacionOferta(new BigDecimal("-1"))
            .oferta(true)
            .imagen((new String("imagen")).getBytes())
            .codigo("12345")
            .build();
    when(proveedorService.getProveedorNoEliminadoPorId(1L)).thenReturn(new Proveedor());
    when(rubroService.getRubroNoEliminadoPorId(1L)).thenReturn(new Rubro());
    when(medidaService.getMedidaNoEliminadaPorId(1L)).thenReturn(new Medida());
    Sucursal[] sucursales = new Sucursal[] {new Sucursal()};
    when(sucursalService.getSucusales(false)).thenReturn(Arrays.asList(sucursales));
    Producto productoDuplicado = new Producto();
    productoDuplicado.setDescripcion("Ventilador de pie");
    when(productoRepository.findByDescripcionAndEliminado("Ventilador de pie", false))
        .thenReturn(productoDuplicado);
    when(messageSourceTestMock.getMessage(
            "mensaje_producto_duplicado_descripcion", null, Locale.getDefault()))
        .thenReturn(
            messageSourceTest.getMessage(
                "mensaje_producto_duplicado_descripcion", null, Locale.getDefault()));
    BusinessServiceException thrown =
        assertThrows(
            BusinessServiceException.class,
            () -> productoService.guardar(nuevoProductoUno, 1L, 1L, 1L));
    assertNotNull(thrown.getMessage());
    assertTrue(
        thrown
            .getMessage()
            .contains(
                messageSourceTest.getMessage(
                    "mensaje_producto_duplicado_descripcion", null, Locale.getDefault())));
  }

  @Test
  public void shouldThrownBusinessExceptionActualizarProductoSinImagen() {
    Producto productoParaActualizar = new ProductoBuilder().withOferta(true).build();
    when(messageSourceTestMock.getMessage(
            "mensaje_producto_oferta_sin_imagen",
            new Object[] {productoParaActualizar.getDescripcion()},
            Locale.getDefault()))
        .thenReturn(
            messageSourceTest.getMessage(
                "mensaje_producto_oferta_sin_imagen",
                new Object[] {productoParaActualizar.getDescripcion()},
                Locale.getDefault()));
    BusinessServiceException thrown =
        assertThrows(
            BusinessServiceException.class,
            () -> productoService.actualizar(productoParaActualizar, productoParaActualizar, null));
    assertNotNull(thrown.getMessage());
    assertTrue(
        thrown
            .getMessage()
            .contains(
                messageSourceTest.getMessage(
                    "mensaje_producto_oferta_sin_imagen",
                    new Object[] {productoParaActualizar.getDescripcion()},
                    Locale.getDefault())));
  }

  @Test
  public void shouldThrownBusinessExceptionActualizacionProductoDuplicadoCodigo() {
    Producto productoParaActualizar = new ProductoBuilder().withId_Producto(1L).build();
    Producto productoPersistido = new ProductoBuilder().withId_Producto(2L).build();
    when(productoRepository.findByDescripcionAndEliminado("Cinta adhesiva doble faz 3M", false))
        .thenReturn(productoPersistido);
    when(messageSourceTestMock.getMessage(
            "mensaje_producto_duplicado_descripcion", null, Locale.getDefault()))
        .thenReturn(
            messageSourceTest.getMessage(
                "mensaje_producto_duplicado_descripcion", null, Locale.getDefault()));
    BusinessServiceException thrown =
        assertThrows(
            BusinessServiceException.class,
            () -> productoService.actualizar(productoParaActualizar, productoPersistido, null));
    assertNotNull(thrown.getMessage());
    assertTrue(
        thrown
            .getMessage()
            .contains(
                messageSourceTest.getMessage(
                    "mensaje_producto_duplicado_descripcion",
                    new Object[] {productoParaActualizar.getDescripcion()},
                    Locale.getDefault())));
  }

  @Test
  public void shouldTestBusquedaCriteria() {
    BusquedaProductoCriteria criteria =
        BusquedaProductoCriteria.builder()
            .codigo("213")
            .descripcion("testDescripcion")
            .idRubro(1L)
            .idProveedor(2L)
            .listarSoloEnStock(true)
            .listarSoloEnStock(true)
            .publico(true)
            .oferta(true)
            .build();
    String stringBuilder =
        "producto.eliminado = false && (containsIc(producto.codigo,213) || containsIc(producto.descripcion,testDescripcion)) "
            + "&& producto.rubro.idRubro = 1 && producto.proveedor.idProveedor = 2 "
            + "&& any(producto.cantidadEnSucursales).cantidad > 0 && producto.ilimitado = false && producto.publico = true "
            + "&& producto.oferta = true";
    assertEquals(stringBuilder, productoService.getBuilder(criteria).toString());
  }

  @Test
  public void shouldGetProductoConPrecioBonificadoPorOferta() {
    Producto producto = new Producto();
    producto.setPrecioLista(new BigDecimal("100"));
    producto.setPorcentajeBonificacionOferta(new BigDecimal("10"));
    producto.setOferta(true);
    when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
    Producto productoRecuperado = productoService.getProductoNoEliminadoPorId(1L);
    assertEquals(new BigDecimal("100"), productoRecuperado.getPrecioLista());
    assertEquals(new BigDecimal("90.00"), productoRecuperado.getPrecioBonificado());
  }

  @Test
  public void shouldThrownEntityNotFoundException() {
    when(messageSourceTestMock.getMessage(
            "mensaje_producto_no_existente", null, Locale.getDefault()))
        .thenReturn(
            messageSourceTest.getMessage(
                "mensaje_producto_no_existente", null, Locale.getDefault()));
    when(productoRepository.findById(1L)).thenReturn(Optional.empty());
    EntityNotFoundException thrown =
        assertThrows(
            EntityNotFoundException.class, () -> productoService.getProductoNoEliminadoPorId(1L));
    assertNotNull(thrown.getMessage());
    assertTrue(
        thrown
            .getMessage()
            .contains(
                messageSourceTest.getMessage(
                    "mensaje_producto_no_existente", null, Locale.getDefault())));
  }

  @Test
  public void shouldDevolverNullSiCodigoVacio() {
    assertNull(productoService.getProductoPorCodigo("123"));
  }

  //  @Test
  //  void shouldGetProductosSinStockDisponible() {
  //    Producto producto = new Producto();
  //    producto.setIdProducto(1L);
  //    producto.setCantidadTotalEnSucursales(BigDecimal.TEN);
  //    //producto.setCantidadEnSucursales();
  //    producto.setIlimitado(false);
  //    when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
  //    long[] idProducto = {1};
  //    BigDecimal[] cantidad = {BigDecimal.TEN.add(BigDecimal.ONE)};
  //    ProductosParaVerificarStockDTO productosParaVerificarStockDTO =
  //
  // ProductosParaVerificarStockDTO.builder().cantidad(cantidad).idProducto(idProducto).build();
  //    Map<Long, BigDecimal> resultadoObtenido =
  //        productoService.getProductosSinStockDisponible(productosParaVerificarStockDTO);
  //    Assertions.assertFalse(resultadoObtenido.isEmpty());
  //  }
}
