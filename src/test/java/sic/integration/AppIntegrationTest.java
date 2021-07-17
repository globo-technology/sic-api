package sic.integration;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.MessageSource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClientResponseException;
import sic.model.Caja;
import sic.model.CantidadEnSucursal;
import sic.model.Cliente;
import sic.model.ConfiguracionSucursal;
import sic.model.FacturaCompra;
import sic.model.FacturaVenta;
import sic.model.Gasto;
import sic.model.Medida;
import sic.model.NotaCredito;
import sic.model.NotaDebito;
import sic.model.Pedido;
import sic.model.Producto;
import sic.model.Proveedor;
import sic.model.Recibo;
import sic.model.Remito;
import sic.model.Rubro;
import sic.model.Sucursal;
import sic.model.Ubicacion;
import sic.model.Usuario;
import sic.modelo.*;
import sic.modelo.RenglonFactura;
import sic.modelo.criteria.*;
import sic.modelo.dto.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(locations = "/application.properties")
class AppIntegrationTest {

  String token;
  final String apiPrefix = "/api/v1";
  static final BigDecimal CIEN = new BigDecimal("100");

  @Autowired TestRestTemplate restTemplate;
  @Autowired MessageSource messageSource;

  void iniciarSesionComoAdministrador() {
    this.token =
        restTemplate
            .postForEntity(
                apiPrefix + "/login",
                new Credencial("dueño", "dueño123"),
                String.class)
            .getBody();
    assertNotNull(this.token);
  }

  @BeforeEach
  void setup() {
    // Interceptor de RestTemplate para JWT
    List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
    interceptors.add(
        (ClientHttpRequestInterceptor)
            (HttpRequest request, byte[] body, ClientHttpRequestExecution execution) -> {
              request.getHeaders().set("Authorization", "Bearer " + token);
              return execution.execute(request, body);
            });
    restTemplate.getRestTemplate().setInterceptors(interceptors);
    // ErrorHandler para RestTemplate
    restTemplate
        .getRestTemplate()
        .setErrorHandler(
            new ResponseErrorHandler() {
              @Override
              public boolean hasError(ClientHttpResponse response) throws IOException {
                HttpStatus.Series series = response.getStatusCode().series();
                return (HttpStatus.Series.CLIENT_ERROR.equals(series)
                    || HttpStatus.Series.SERVER_ERROR.equals(series));
              }

              @Override
              public void handleError(ClientHttpResponse response) throws IOException {
                String mensaje = IOUtils.toString(response.getBody(), Charset.defaultCharset());
                throw new RestClientResponseException(
                    mensaje,
                    response.getRawStatusCode(),
                    response.getStatusText(),
                    response.getHeaders(),
                    null,
                    Charset.defaultCharset());
              }
            });
  }

  @Test
  @DisplayName("Iniciar actividad con una nueva sucursal y un usuario Administrador")
  @Order(1)
  void iniciarActividadComercial() throws IOException {
    this.token =
        restTemplate
            .postForEntity(
                apiPrefix + "/login",
                new Credencial("test", "test"),
                String.class)
            .getBody();
    Usuario credencial =
        Usuario.builder()
            .username("dueño")
            .password("dueño123")
            .nombre("Max")
            .apellido("Power")
            .email("liderDeLaEmpresa@yahoo.com.br")
            .roles(new ArrayList<>(Collections.singletonList(Rol.ADMINISTRADOR)))
            .build();
    credencial = restTemplate.postForObject(apiPrefix + "/usuarios", credencial, Usuario.class);
    credencial.setHabilitado(true);
    restTemplate.put(apiPrefix + "/usuarios", credencial);
    this.iniciarSesionComoAdministrador();
    Sucursal sucursal =
        Sucursal.builder()
            .nombre("FirstOfAll")
            .categoriaIVA(CategoriaIVA.RESPONSABLE_INSCRIPTO)
            .email("support@globocorporation.com")
            .idFiscal(20311023188L)
            .ubicacion(Ubicacion.builder().idLocalidad(1L).idProvincia(1L).build())
            .build();
    Sucursal sucursalRecuperada =
        restTemplate.postForObject(apiPrefix + "/sucursales", sucursal, Sucursal.class);
    assertEquals(sucursal, sucursalRecuperada);
    ConfiguracionSucursal configuracionSucursal =
        restTemplate.getForObject(
            apiPrefix + "/configuraciones-sucursal/" + sucursalRecuperada.getIdSucursal(),
            ConfiguracionSucursal.class);
    configuracionSucursal.setPuntoDeRetiro(true);
    configuracionSucursal.setPredeterminada(true);
    restTemplate.put(apiPrefix + "/configuraciones-sucursal", configuracionSucursal);
    configuracionSucursal =
        restTemplate.getForObject(
            apiPrefix + "/configuraciones-sucursal/" + sucursalRecuperada.getIdSucursal(),
            ConfiguracionSucursal.class);
    assertTrue(configuracionSucursal.isPuntoDeRetiro());
    assertTrue(configuracionSucursal.isPredeterminada());
    File resource = new ClassPathResource("/certificadoAfipTest.p12").getFile();
    byte[] certificadoAfip = new byte[(int) resource.length()];
    FileInputStream fileInputStream = new FileInputStream(resource);
    fileInputStream.read(certificadoAfip);
    fileInputStream.close();
    this.iniciarSesionComoAdministrador();
    configuracionSucursal.setCertificadoAfip(certificadoAfip);
    configuracionSucursal.setFacturaElectronicaHabilitada(true);
    configuracionSucursal.setFirmanteCertificadoAfip("globo");
    configuracionSucursal.setPasswordCertificadoAfip("globo123");
    configuracionSucursal.setNroPuntoDeVentaAfip(2);
    restTemplate.put(apiPrefix + "/configuraciones-sucursal", configuracionSucursal);
    ConfiguracionSucursal configuracionSucursalActualizada =
        restTemplate.getForObject(
            apiPrefix + "/configuraciones-sucursal/" + sucursalRecuperada.getIdSucursal(),
            ConfiguracionSucursal.class);
    assertEquals(configuracionSucursal, configuracionSucursalActualizada);
  }

  @Test
  @DisplayName("Abrir caja con $1000 en efectivo y registrar un gasto por $500 con transferencia")
  @Order(2)
  void testEscenarioAbrirCaja() {
    this.iniciarSesionComoAdministrador();
    Caja cajaAbierta =
        restTemplate.postForObject(
            apiPrefix + "/cajas/apertura/sucursales/1?saldoApertura=1000", null, Caja.class);
    assertEquals(EstadoCaja.ABIERTA, cajaAbierta.getEstado());
    assertEquals(new BigDecimal("1000"), cajaAbierta.getSaldoApertura());
    NuevoGastoDTO nuevoGasto =
        NuevoGastoDTO.builder()
            .monto(new BigDecimal("500"))
            .concepto("Pago de Agua")
            .idFormaDePago(1L)
            .idSucursal(1L)
            .build();
    List<Sucursal> sucursales =
        Arrays.asList(restTemplate.getForObject(apiPrefix + "/sucursales", Sucursal[].class));
    assertFalse(sucursales.isEmpty());
    assertEquals(1, sucursales.size());
    restTemplate.postForObject(apiPrefix + "/gastos", nuevoGasto, Gasto.class);
    BusquedaGastoCriteria criteria = BusquedaGastoCriteria.builder().idSucursal(1L).build();
    HttpEntity<BusquedaGastoCriteria> requestEntity = new HttpEntity<>(criteria);
    PaginaRespuestaRest<Gasto> resultadoBusqueda =
        restTemplate
            .exchange(
                apiPrefix + "/gastos/busqueda/criteria",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<PaginaRespuestaRest<Gasto>>() {})
            .getBody();
    assertNotNull(resultadoBusqueda);
    List<Gasto> gastosRecuperados = resultadoBusqueda.getContent();
    assertEquals(1, gastosRecuperados.size());
    assertEquals(new BigDecimal("500.000000000000000"), gastosRecuperados.get(0).getMonto());
    assertEquals("Pago de Agua", gastosRecuperados.get(0).getConcepto());
  }

  @Test
  @DisplayName(
      "Comprar productos al proveedor RI con factura A y verificar saldo CC, luego saldar la CC con un cheque de 3ro")
  @Order(3)
  void testEscenarioCompraEscenario1() {
    this.iniciarSesionComoAdministrador();
    Proveedor proveedor =
        Proveedor.builder()
            .categoriaIVA(CategoriaIVA.RESPONSABLE_INSCRIPTO)
            .razonSocial("Chamaco S.R.L.")
            .build();
    Proveedor proveedorRecuperado =
        restTemplate.postForObject(apiPrefix + "/proveedores", proveedor, Proveedor.class);
    assertEquals(proveedor, proveedorRecuperado);
    Rubro rubro = Rubro.builder().nombre("Ferreteria").build();
    Rubro rubroDadoDeAlta = restTemplate.postForObject(apiPrefix + "/rubros", rubro, Rubro.class);
    assertEquals(rubro, rubroDadoDeAlta);
    Medida medida = Medida.builder().nombre("Metro").build();
    Medida medidaDadaDeAlta =
        restTemplate.postForObject(apiPrefix + "/medidas", medida, Medida.class);
    assertEquals(medida, medidaDadaDeAlta);
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
            .precioCosto(CIEN)
            .gananciaPorcentaje(new BigDecimal("900"))
            .gananciaNeto(new BigDecimal("900"))
            .precioVentaPublico(new BigDecimal("1000"))
            .ivaPorcentaje(new BigDecimal("21.0"))
            .ivaNeto(new BigDecimal("210"))
            .precioLista(new BigDecimal("1210"))
            .porcentajeBonificacionPrecio(new BigDecimal("20"))
            .build();
    NuevoProductoDTO nuevoProductoDos =
        NuevoProductoDTO.builder()
            .descripcion("Reflector led 100w")
            .cantidadEnSucursal(
                new HashMap<Long, BigDecimal>() {
                  {
                    put(1L, new BigDecimal("9"));
                  }
                })
            .bulto(BigDecimal.ONE)
            .precioCosto(CIEN)
            .gananciaPorcentaje(new BigDecimal("900"))
            .gananciaNeto(new BigDecimal("900"))
            .precioVentaPublico(new BigDecimal("1000"))
            .ivaPorcentaje(new BigDecimal("10.5"))
            .ivaNeto(new BigDecimal("105"))
            .precioLista(new BigDecimal("1105"))
            .porcentajeBonificacionPrecio(new BigDecimal("20"))
            .build();
    NuevoProductoDTO nuevoProductoTres =
        NuevoProductoDTO.builder()
            .descripcion("Canilla Monocomando")
            .cantidadEnSucursal(
                new HashMap<Long, BigDecimal>() {
                  {
                    put(1L, new BigDecimal("10"));
                  }
                })
            .bulto(BigDecimal.ONE)
            .precioCosto(new BigDecimal("10859.73"))
            .gananciaPorcentaje(new BigDecimal("11.37"))
            .gananciaNeto(new BigDecimal("1234.751"))
            .precioVentaPublico(new BigDecimal("12094.481"))
            .ivaPorcentaje(new BigDecimal("10.5"))
            .ivaNeto(new BigDecimal("1269.921"))
            .precioLista(new BigDecimal("13364.402"))
            .porcentajeBonificacionPrecio(BigDecimal.TEN)
            .build();
    Sucursal sucursal = restTemplate.getForObject(apiPrefix + "/sucursales/1", Sucursal.class);
    restTemplate.postForObject(
        apiPrefix
            + "/productos?idMedida="
            + medidaDadaDeAlta.getIdMedida()
            + "&idRubro="
            + rubroDadoDeAlta.getIdRubro()
            + "&idProveedor="
            + proveedorRecuperado.getIdProveedor()
            + "&idSucursal="
            + sucursal.getIdSucursal(),
        nuevoProductoUno,
        Producto.class);
    BusquedaProductoCriteria criteria =
        BusquedaProductoCriteria.builder().descripcion("Ventilador").build();
    HttpEntity<BusquedaProductoCriteria> requestEntity = new HttpEntity<>(criteria);
    PaginaRespuestaRest<Producto> resultadoBusqueda =
        restTemplate
            .exchange(
                apiPrefix + "/productos/busqueda/criteria/sucursales/1",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<PaginaRespuestaRest<Producto>>() {})
            .getBody();
    assertNotNull(resultadoBusqueda);
    List<Producto> productosRecuperados = resultadoBusqueda.getContent();
    assertEquals("Ventilador de pie", productosRecuperados.get(0).getDescripcion());
    assertEquals(
        new BigDecimal("10.000000000000000"),
        productosRecuperados.get(0).getCantidadTotalEnSucursales());
    assertEquals(new BigDecimal("0E-15"), productosRecuperados.get(0).getCantidadReservada());
    assertEquals("Metro", productosRecuperados.get(0).getNombreMedida());
    assertEquals(
        new BigDecimal("100.000000000000000"), productosRecuperados.get(0).getPrecioCosto());
    assertEquals(
        new BigDecimal("900.000000000000000"), productosRecuperados.get(0).getGananciaPorcentaje());
    assertEquals(
        new BigDecimal("900.000000000000000"), productosRecuperados.get(0).getGananciaNeto());
    assertEquals(
        new BigDecimal("1000.000000000000000"),
        productosRecuperados.get(0).getPrecioVentaPublico());
    assertEquals(
        new BigDecimal("21.000000000000000"), productosRecuperados.get(0).getIvaPorcentaje());
    assertEquals(new BigDecimal("210.000000000000000"), productosRecuperados.get(0).getIvaNeto());
    assertEquals(
        new BigDecimal("1210.000000000000000"), productosRecuperados.get(0).getPrecioLista());
    assertEquals("Ferreteria", productosRecuperados.get(0).getNombreRubro());
    assertEquals(
        new BigDecimal("0E-15"), productosRecuperados.get(0).getPorcentajeBonificacionOferta());
    assertEquals(
        new BigDecimal("20.000000000000000"),
        productosRecuperados.get(0).getPorcentajeBonificacionPrecio());
    assertEquals(
        new BigDecimal("968.000000000000000"),
        productosRecuperados.get(0).getPrecioBonificado());
    restTemplate.postForObject(
        apiPrefix
            + "/productos?idMedida="
            + medidaDadaDeAlta.getIdMedida()
            + "&idRubro="
            + rubroDadoDeAlta.getIdRubro()
            + "&idProveedor="
            + proveedorRecuperado.getIdProveedor()
            + "&idSucursal="
            + sucursal.getIdSucursal(),
        nuevoProductoDos,
        Producto.class);
    criteria = BusquedaProductoCriteria.builder().descripcion("Reflector").build();
    requestEntity = new HttpEntity<>(criteria);
    resultadoBusqueda =
        restTemplate
            .exchange(
                apiPrefix + "/productos/busqueda/criteria/sucursales/1",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<PaginaRespuestaRest<Producto>>() {})
            .getBody();
    assertNotNull(resultadoBusqueda);
    productosRecuperados = resultadoBusqueda.getContent();
    assertEquals("Reflector led 100w", productosRecuperados.get(0).getDescripcion());
    assertEquals(
        new BigDecimal("9.000000000000000"),
        productosRecuperados.get(0).getCantidadTotalEnSucursales());
    assertEquals("Metro", productosRecuperados.get(0).getNombreMedida());
    assertEquals(
        new BigDecimal("100.000000000000000"), productosRecuperados.get(0).getPrecioCosto());
    assertEquals(
        new BigDecimal("900.000000000000000"), productosRecuperados.get(0).getGananciaPorcentaje());
    assertEquals(
        new BigDecimal("900.000000000000000"), productosRecuperados.get(0).getGananciaNeto());
    assertEquals(
        new BigDecimal("1000.000000000000000"),
        productosRecuperados.get(0).getPrecioVentaPublico());
    assertEquals(
        new BigDecimal("10.500000000000000"), productosRecuperados.get(0).getIvaPorcentaje());
    assertEquals(new BigDecimal("105.000000000000000"), productosRecuperados.get(0).getIvaNeto());
    assertEquals(
        new BigDecimal("1105.000000000000000"), productosRecuperados.get(0).getPrecioLista());
    assertEquals("Ferreteria", productosRecuperados.get(0).getNombreRubro());
    assertEquals(
        new BigDecimal("0E-15"), productosRecuperados.get(0).getPorcentajeBonificacionOferta());
    assertEquals(
        new BigDecimal("20.000000000000000"),
        productosRecuperados.get(0).getPorcentajeBonificacionPrecio());
    assertEquals(
        new BigDecimal("884.000000000000000"),
        productosRecuperados.get(0).getPrecioBonificado());
    restTemplate.postForObject(
        apiPrefix
            + "/productos?idMedida="
            + medidaDadaDeAlta.getIdMedida()
            + "&idRubro="
            + rubroDadoDeAlta.getIdRubro()
            + "&idProveedor="
            + proveedorRecuperado.getIdProveedor()
            + "&idSucursal="
            + sucursal.getIdSucursal(),
        nuevoProductoTres,
        Producto.class);
    criteria = BusquedaProductoCriteria.builder().descripcion("Canilla").build();
    requestEntity = new HttpEntity<>(criteria);
    resultadoBusqueda =
        restTemplate
            .exchange(
                apiPrefix + "/productos/busqueda/criteria/sucursales/1",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<PaginaRespuestaRest<Producto>>() {})
            .getBody();
    assertNotNull(resultadoBusqueda);
    productosRecuperados = resultadoBusqueda.getContent();
    assertEquals("Canilla Monocomando", productosRecuperados.get(0).getDescripcion());
    assertEquals(
        new BigDecimal("10.000000000000000"),
        productosRecuperados.get(0).getCantidadTotalEnSucursales());
    assertEquals("Metro", productosRecuperados.get(0).getNombreMedida());
    assertEquals(
        new BigDecimal("10859.730000000000000"), productosRecuperados.get(0).getPrecioCosto());
    assertEquals(
        new BigDecimal("11.370000000000000"), productosRecuperados.get(0).getGananciaPorcentaje());
    assertEquals(
        new BigDecimal("1234.751000000000000"), productosRecuperados.get(0).getGananciaNeto());
    assertEquals(
        new BigDecimal("12094.481000000000000"),
        productosRecuperados.get(0).getPrecioVentaPublico());
    assertEquals(
        new BigDecimal("10.500000000000000"), productosRecuperados.get(0).getIvaPorcentaje());
    assertEquals(new BigDecimal("1269.921000000000000"), productosRecuperados.get(0).getIvaNeto());
    assertEquals(
        new BigDecimal("13364.402000000000000"), productosRecuperados.get(0).getPrecioLista());
    assertEquals("Ferreteria", productosRecuperados.get(0).getNombreRubro());
    assertEquals(
        new BigDecimal("0E-15"), productosRecuperados.get(0).getPorcentajeBonificacionOferta());
    assertEquals(
        new BigDecimal("10.000000000000000"),
        productosRecuperados.get(0).getPorcentajeBonificacionPrecio());
    assertEquals(
        new BigDecimal("12027.961800000000000"),
        productosRecuperados.get(0).getPrecioBonificado());
    List<NuevoRenglonFacturaDTO> nuevosRenglones = new ArrayList<>();
    criteria = BusquedaProductoCriteria.builder().build();
    requestEntity = new HttpEntity<>(criteria);
    resultadoBusqueda =
        restTemplate
            .exchange(
                apiPrefix + "/productos/busqueda/criteria/sucursales/1",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<PaginaRespuestaRest<Producto>>() {})
            .getBody();
    assertNotNull(resultadoBusqueda);
    productosRecuperados = resultadoBusqueda.getContent();
    NuevoRenglonFacturaDTO nuevoRenglon =
        NuevoRenglonFacturaDTO.builder()
            .cantidad(new BigDecimal("4"))
            .idProducto(productosRecuperados.get(2).getIdProducto())
            .bonificacion(new BigDecimal("20"))
            .build();
    nuevosRenglones.add(nuevoRenglon);
    nuevoRenglon =
        NuevoRenglonFacturaDTO.builder()
            .cantidad(new BigDecimal("3"))
            .idProducto(productosRecuperados.get(1).getIdProducto())
            .bonificacion(new BigDecimal("20"))
            .build();
    nuevosRenglones.add(nuevoRenglon);
    NuevaFacturaCompraDTO nuevaFacturaCompraDTO =
        NuevaFacturaCompraDTO.builder()
            .idProveedor(1L)
            .idSucursal(1L)
            .tipoDeComprobante(TipoDeComprobante.FACTURA_A)
            .renglones(nuevosRenglones)
            .recargoPorcentaje(BigDecimal.TEN)
            .descuentoPorcentaje(new BigDecimal("25"))
            .fecha(LocalDateTime.now())
            .build();
    restTemplate.postForObject(
        apiPrefix + "/facturas/compras", nuevaFacturaCompraDTO, FacturaCompra[].class);
    BusquedaFacturaCompraCriteria criteriaCompra =
        BusquedaFacturaCompraCriteria.builder()
            .idSucursal(1L)
            .tipoComprobante(TipoDeComprobante.FACTURA_A)
            .build();
    HttpEntity<BusquedaFacturaCompraCriteria> requestEntityCompra =
        new HttpEntity<>(criteriaCompra);
    PaginaRespuestaRest<FacturaCompra> resultadoBusquedaCompra =
        restTemplate
            .exchange(
                apiPrefix + "/facturas/compras/busqueda/criteria",
                HttpMethod.POST,
                requestEntityCompra,
                new ParameterizedTypeReference<PaginaRespuestaRest<FacturaCompra>>() {})
            .getBody();
    assertNotNull(resultadoBusquedaCompra);
    List<FacturaCompra> facturasRecuperadas = resultadoBusquedaCompra.getContent();
    assertEquals(1, facturasRecuperadas.size());
    assertEquals(new BigDecimal("560.0"), facturasRecuperadas.get(0).getSubTotal());
    assertEquals(new BigDecimal("56.0"), facturasRecuperadas.get(0).getRecargoNeto());
    assertEquals(new BigDecimal("140.0"), facturasRecuperadas.get(0).getDescuentoNeto());
    assertEquals(new BigDecimal("476.0"), facturasRecuperadas.get(0).getSubTotalBruto());
    assertEquals(new BigDecimal("21.42"), facturasRecuperadas.get(0).getIva105Neto());
    assertEquals(new BigDecimal("57.12"), facturasRecuperadas.get(0).getIva21Neto());
    assertEquals(new BigDecimal("554.54"), facturasRecuperadas.get(0).getTotal());
    assertNotNull(facturasRecuperadas.get(0).getFechaAlta());
    assertEquals(
        proveedorRecuperado.getRazonSocial(), facturasRecuperadas.get(0).getRazonSocialProveedor());
    assertEquals(sucursal.getNombre(), facturasRecuperadas.get(0).getNombreSucursal());
    assertEquals(
        new BigDecimal("-554.540000000000000"),
        restTemplate.getForObject(
            apiPrefix + "/cuentas-corriente/proveedores/1/saldo", BigDecimal.class));
    Recibo recibo =
        Recibo.builder()
            .monto(554.54)
            .concepto("Recibo para proveedor")
            .idSucursal(sucursal.getIdSucursal())
            .idProveedor(proveedorRecuperado.getIdProveedor())
            .idFormaDePago(2L)
            .build();
    Recibo reciboRecuperado =
        restTemplate.postForObject(apiPrefix + "/recibos/proveedores", recibo, Recibo.class);
    assertEquals(recibo, reciboRecuperado);
    assertEquals(
        0.0,
        restTemplate
            .getForObject(apiPrefix + "/cuentas-corriente/proveedores/1/saldo", BigDecimal.class)
            .doubleValue());
    criteria = BusquedaProductoCriteria.builder().build();
    requestEntity = new HttpEntity<>(criteria);
    resultadoBusqueda =
        restTemplate
            .exchange(
                apiPrefix + "/productos/busqueda/criteria/sucursales/1",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<PaginaRespuestaRest<Producto>>() {})
            .getBody();
    assertNotNull(resultadoBusqueda);
    productosRecuperados = resultadoBusqueda.getContent();
    assertEquals(
        new BigDecimal("14.000000000000000"),
        productosRecuperados.get(2).getCantidadTotalEnSucursales());
    assertEquals(
        new BigDecimal("12.000000000000000"),
        productosRecuperados.get(1).getCantidadTotalEnSucursales());
    assertEquals(
        new BigDecimal("10.000000000000000"),
        productosRecuperados.get(0).getCantidadTotalEnSucursales());
  }

  @Test
  @DisplayName(
          "Dar de alta una nota de credito por una unidad fallada, chequear salgo CC y stock")
  @Order(4)
  void testEscenarioCompraEscenario2() {
    this.iniciarSesionComoAdministrador();
    assertEquals(
            0.0,
            restTemplate
                    .getForObject(apiPrefix + "/cuentas-corriente/proveedores/1/saldo", BigDecimal.class)
                    .doubleValue());
    BusquedaProductoCriteria criteria =
            BusquedaProductoCriteria.builder().descripcion("Ventilador").build();
    HttpEntity<BusquedaProductoCriteria> requestEntity = new HttpEntity<>(criteria);
    PaginaRespuestaRest<Producto> resultadoBusqueda =
            restTemplate
                    .exchange(
                            apiPrefix + "/productos/busqueda/criteria/sucursales/1",
                            HttpMethod.POST,
                            requestEntity,
                            new ParameterizedTypeReference<PaginaRespuestaRest<Producto>>() {})
                    .getBody();
    assertNotNull(resultadoBusqueda);
    List<Producto> productosRecuperados = resultadoBusqueda.getContent();
    assertEquals("Ventilador de pie", productosRecuperados.get(0).getDescripcion());
    assertEquals(new BigDecimal("14.000000000000000"), productosRecuperados.get(0).getCantidadTotalEnSucursales());
    BusquedaFacturaCompraCriteria criteriaCompra =
            BusquedaFacturaCompraCriteria.builder()
                    .idSucursal(1L)
                    .tipoComprobante(TipoDeComprobante.FACTURA_A)
                    .build();
    HttpEntity<BusquedaFacturaCompraCriteria> requestEntityCompra =
            new HttpEntity<>(criteriaCompra);
    PaginaRespuestaRest<FacturaCompra> resultadoBusquedaCompra =
            restTemplate
                    .exchange(
                            apiPrefix + "/facturas/compras/busqueda/criteria",
                            HttpMethod.POST,
                            requestEntityCompra,
                            new ParameterizedTypeReference<PaginaRespuestaRest<FacturaCompra>>() {})
                    .getBody();
    assertNotNull(resultadoBusquedaCompra);
    List<FacturaCompra> facturasRecuperadas = resultadoBusquedaCompra.getContent();
    assertEquals(1, facturasRecuperadas.size());
    List<sic.model.RenglonFactura> renglonesFacturaCompra = Arrays.asList(restTemplate.getForObject(apiPrefix + "/facturas/" +
            facturasRecuperadas.get(0).getIdFactura() + "/renglones", sic.model.RenglonFactura[].class));
    NuevaNotaCreditoDeFacturaDTO nuevaNotaCreditoDeFacturaDTO = NuevaNotaCreditoDeFacturaDTO
            .builder()
            .idFactura(facturasRecuperadas.get(0).getIdFactura())
            .cantidades(new BigDecimal[]{BigDecimal.ONE})
            .idsRenglonesFactura(new Long[]{renglonesFacturaCompra.get(0).getIdRenglonFactura()})
            .modificaStock(true)
            .motivo("Unidad Fallada")
            .build();
    NotaCredito notaCredito = restTemplate.postForObject(apiPrefix + "/notas/credito/factura", nuevaNotaCreditoDeFacturaDTO, NotaCredito.class);
    assertEquals(notaCredito.getIdFacturaCompra(), facturasRecuperadas.get(0).getIdFactura());
    assertEquals(notaCredito.getIdNota(), 1L);
    assertEquals(
            82.28,
            restTemplate
                    .getForObject(apiPrefix + "/cuentas-corriente/proveedores/1/saldo", BigDecimal.class)
                    .doubleValue());
    resultadoBusqueda =
            restTemplate
                    .exchange(
                            apiPrefix + "/productos/busqueda/criteria/sucursales/1",
                            HttpMethod.POST,
                            requestEntity,
                            new ParameterizedTypeReference<PaginaRespuestaRest<Producto>>() {})
                    .getBody();
    assertNotNull(resultadoBusqueda);
    productosRecuperados = resultadoBusqueda.getContent();
    assertEquals("Ventilador de pie", productosRecuperados.get(0).getDescripcion());
    assertEquals(new BigDecimal("13.000000000000000"), productosRecuperados.get(0).getCantidadTotalEnSucursales());
  }

  @Test
  @DisplayName("Actualizar CC segun ND por mora, luego verificar saldo CC")
  @Order(5)
  void testEscenarioNotaDebito() {
    this.iniciarSesionComoAdministrador();
    BusquedaProveedorCriteria criteriaParaProveedores = BusquedaProveedorCriteria.builder().build();
    HttpEntity<BusquedaProveedorCriteria> requestEntityParaProveedores =
        new HttpEntity<>(criteriaParaProveedores);
    PaginaRespuestaRest<Proveedor> resultadoBusquedaProveedor =
        restTemplate
            .exchange(
                apiPrefix + "/proveedores/busqueda/criteria",
                HttpMethod.POST,
                requestEntityParaProveedores,
                new ParameterizedTypeReference<PaginaRespuestaRest<Proveedor>>() {})
            .getBody();
    assertNotNull(resultadoBusquedaProveedor);
    List<Proveedor> proveedoresRecuperados = resultadoBusquedaProveedor.getContent();
    assertEquals(1, proveedoresRecuperados.size());
    BusquedaReciboCriteria criteriaParaRecibos =
        BusquedaReciboCriteria.builder()
            .idProveedor(proveedoresRecuperados.get(0).getIdProveedor())
            .build();
    HttpEntity<BusquedaReciboCriteria> requestEntity = new HttpEntity<>(criteriaParaRecibos);
    PaginaRespuestaRest<Recibo> resultadoBusquedaRecibo =
        restTemplate
            .exchange(
                apiPrefix + "/recibos/busqueda/criteria",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<PaginaRespuestaRest<Recibo>>() {})
            .getBody();
    assertNotNull(resultadoBusquedaRecibo);
    List<Recibo> recibosRecuperados = resultadoBusquedaRecibo.getContent();
    assertEquals(1, recibosRecuperados.size());
    NuevaNotaDebitoDeReciboDTO nuevaNotaDebitoDeReciboDTO =
        NuevaNotaDebitoDeReciboDTO.builder()
            .idRecibo(recibosRecuperados.get(0).getIdRecibo())
            .gastoAdministrativo(new BigDecimal("1500.00"))
            .motivo("No pagamos, la vida es así.")
            .tipoDeComprobante(TipoDeComprobante.NOTA_DEBITO_A)
            .build();
//    NotaDebito notaDebitoCalculada =
//        restTemplate.postForObject(
//            apiPrefix + "/notas/debito/calculos", nuevaNotaDebitoDeReciboDTO, NotaDebito.class);
//    NotaDebito notaDebitoGuardada =
        restTemplate.postForObject(
            apiPrefix + "/notas/debito", nuevaNotaDebitoDeReciboDTO, NotaDebito.class);

    BusquedaNotaCriteria criteriaNota =
            BusquedaNotaCriteria.builder()
                    .idSucursal(1L)
                    .tipoComprobante(TipoDeComprobante.NOTA_DEBITO_A)
                    .build();
    HttpEntity<BusquedaNotaCriteria> requestEntityNota = new HttpEntity<>(criteriaNota);
    PaginaRespuestaRest<NotaDebito> resultadoBusquedaNota =
            restTemplate
                    .exchange(
                            apiPrefix + "/notas/debito/busqueda/criteria",
                            HttpMethod.POST,
                            requestEntityNota,
                            new ParameterizedTypeReference<PaginaRespuestaRest<NotaDebito>>() {})
                    .getBody();
    assertNotNull(resultadoBusquedaNota);
    List<NotaDebito> notasRecuperadas = resultadoBusquedaNota.getContent();
    List<sic.model.RenglonNotaDebito> renglones =
            Arrays.asList(
                    restTemplate.getForObject(
                            apiPrefix + "/notas/renglones/debito/" + notasRecuperadas.get(0).getIdNota(),
                            sic.model.RenglonNotaDebito[].class));
    assertNotNull(renglones);
    assertEquals(2, renglones.size());
    // renglones
    assertEquals("Nº Recibo 2-1: Recibo para proveedor", renglones.get(0).getDescripcion());
    assertEquals(new BigDecimal("554.540000000000000"), renglones.get(0).getMonto());
    assertEquals(new BigDecimal("554.540000000000000"), renglones.get(0).getImporteBruto());
    assertEquals(new BigDecimal("0E-15"), renglones.get(0).getIvaPorcentaje());
    assertEquals(new BigDecimal("0E-15"), renglones.get(0).getIvaNeto());
    assertEquals(new BigDecimal("554.540000000000000"), renglones.get(0).getImporteNeto());
    assertEquals("Gasto Administrativo", renglones.get(1).getDescripcion());
    assertEquals(new BigDecimal("1239.669421487603306"), renglones.get(1).getMonto());
    assertEquals(new BigDecimal("1239.669421487603306"), renglones.get(1).getImporteBruto());
    assertEquals(new BigDecimal("21.000000000000000"), renglones.get(1).getIvaPorcentaje());
    assertEquals(new BigDecimal("260.330578512396694"), renglones.get(1).getIvaNeto());
    assertEquals(new BigDecimal("1500.000000000000000"), renglones.get(1).getImporteNeto());
  }

  @Test
  @DisplayName("Dar de alta un producto con imagen")
  @Order(6)
  void testEscenarioAltaDeProductoConImagen() throws IOException {
    this.iniciarSesionComoAdministrador();
    List<Medida> medidas =
        Arrays.asList(restTemplate.getForObject(apiPrefix + "/medidas", Medida[].class));
    assertFalse(medidas.isEmpty());
    assertEquals(1, medidas.size());
    List<Rubro> rubros =
        Arrays.asList(restTemplate.getForObject(apiPrefix + "/rubros", Rubro[].class));
    assertFalse(rubros.isEmpty());
    assertEquals(1, rubros.size());
    List<Sucursal> sucursales =
        Arrays.asList(restTemplate.getForObject(apiPrefix + "/sucursales", Sucursal[].class));
    assertFalse(sucursales.isEmpty());
    assertEquals(1, sucursales.size());
    BusquedaProveedorCriteria criteriaParaProveedores = BusquedaProveedorCriteria.builder().build();
    HttpEntity<BusquedaProveedorCriteria> requestEntityParaProveedores =
        new HttpEntity<>(criteriaParaProveedores);
    PaginaRespuestaRest<Proveedor> resultadoBusquedaProveedor =
        restTemplate
            .exchange(
                apiPrefix + "/proveedores/busqueda/criteria",
                HttpMethod.POST,
                requestEntityParaProveedores,
                new ParameterizedTypeReference<PaginaRespuestaRest<Proveedor>>() {})
            .getBody();
    assertNotNull(resultadoBusquedaProveedor);
    List<Proveedor> proveedoresRecuperados = resultadoBusquedaProveedor.getContent();
    assertEquals(1, proveedoresRecuperados.size());
    BufferedImage bImage = ImageIO.read(getClass().getResource("/imagenProductoTest.jpeg"));
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ImageIO.write(bImage, "jpeg", bos);
    NuevoProductoDTO nuevoProductoCuatro =
        NuevoProductoDTO.builder()
            .descripcion("Corta Papas - Vegetales")
            .cantidadEnSucursal(
                new HashMap<Long, BigDecimal>() {
                  {
                    put(1L, BigDecimal.TEN);
                  }
                })
            .bulto(BigDecimal.ONE)
            .precioCosto(CIEN)
            .gananciaPorcentaje(new BigDecimal("900"))
            .gananciaNeto(new BigDecimal("900"))
            .precioVentaPublico(new BigDecimal("1000"))
            .ivaPorcentaje(new BigDecimal("10.5"))
            .ivaNeto(new BigDecimal("105"))
            .precioLista(new BigDecimal("1105"))
            .porcentajeBonificacionPrecio(new BigDecimal("20"))
            .publico(true)
            .imagen(bos.toByteArray())
            .build();
    Producto productoConImagen =
        restTemplate.postForObject(
            apiPrefix
                + "/productos?idMedida="
                + medidas.get(0).getIdMedida()
                + "&idRubro="
                + rubros.get(0).getIdRubro()
                + "&idProveedor="
                + proveedoresRecuperados.get(0).getIdProveedor(),
            nuevoProductoCuatro,
            Producto.class);
    assertNotNull(productoConImagen.getUrlImagen());
  }

  @Test
  @DisplayName("Actualizar el producto modificando la imagen, y luego eliminar el enlace")
  @Order(7)
  void testEscenarioModificarImagenDeProducto() throws IOException {
    this.iniciarSesionComoAdministrador();
    BusquedaProductoCriteria productosCriteria = BusquedaProductoCriteria.builder()
            .descripcion("Corta Papas - Vegetales")
            .build();
    HttpEntity<BusquedaProductoCriteria> requestEntityProductos =
            new HttpEntity<>(productosCriteria);
    PaginaRespuestaRest<Producto> resultadoBusqueda =
            restTemplate
                    .exchange(
                            apiPrefix + "/productos/busqueda/criteria/sucursales/1",
                            HttpMethod.POST,
                            requestEntityProductos,
                            new ParameterizedTypeReference<PaginaRespuestaRest<Producto>>() {})
                    .getBody();
    assertNotNull(resultadoBusqueda);
    List<Producto> productosRecuperados = resultadoBusqueda.getContent();
    assertEquals(1, productosRecuperados.size());
    Producto productoParaActualizar = productosRecuperados.get(0);
    BufferedImage bImage = ImageIO.read(getClass().getResource("/imagenProductoTest.jpeg"));
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ImageIO.write(bImage, "jpeg", bos);
    productoParaActualizar.setImagen(bos.toByteArray());
    restTemplate.put(apiPrefix + "/productos", productoParaActualizar);
    productoParaActualizar = restTemplate.getForObject(apiPrefix + "/productos/" + productoParaActualizar.getIdProducto()
            + "/sucursales/1", Producto.class);
    assertNotNull(productoParaActualizar.getUrlImagen());
    productoParaActualizar.setUrlImagen(null);
    restTemplate.put(apiPrefix + "/productos", productoParaActualizar);
    productoParaActualizar = restTemplate.getForObject(apiPrefix + "/productos/" + productoParaActualizar.getIdProducto()
            + "/sucursales/1", Producto.class);
    assertNull(productoParaActualizar.getUrlImagen());
  }

  @Test
  @DisplayName("Dar de alta un cliente y levantar un pedido con reserva, verificar stock")
  @Order(8)
  void testEscenarioAltaClienteYPedido() {
    this.iniciarSesionComoAdministrador();
    Usuario credencial =
        Usuario.builder()
            .username("elenanocanete")
            .password("siempredebarrio")
            .nombre("Juan")
            .apellido("Canete")
            .email("caniete@yahoo.com.br")
            .roles(new ArrayList<>(Collections.singletonList(Rol.COMPRADOR)))
            .habilitado(true)
            .build();
    Usuario credencialDadaDeAlta =
        restTemplate.postForObject(apiPrefix + "/usuarios", credencial, Usuario.class);
    credencialDadaDeAlta.setHabilitado(true);
    assertEquals(credencial, credencialDadaDeAlta);
    Cliente cliente =
        Cliente.builder()
            .montoCompraMinima(new BigDecimal("500"))
            .nombreFiscal("Juan Fernando Canete")
            .categoriaIVA(CategoriaIVA.RESPONSABLE_INSCRIPTO)
            .idFiscal(30703176840L)
            .telefono("3785663322")
            .idCredencial(credencialDadaDeAlta.getIdUsuario())
            .ubicacionFacturacion(Ubicacion.builder().idProvincia(1L).idLocalidad(1L).build())
            .email("correoparapagos@gmail.com")
            .puedeComprarAPlazo(true)
            .build();
    Cliente clienteRecuperado =
        restTemplate.postForObject(apiPrefix + "/clientes", cliente, Cliente.class);
    assertEquals(cliente, clienteRecuperado);
    Producto productoUno = restTemplate.getForObject(apiPrefix + "/productos/1/sucursales/1", Producto.class);
    Producto productoDos = restTemplate.getForObject(apiPrefix + "/productos/2/sucursales/1", Producto.class);
    assertEquals(new BigDecimal("13.000000000000000"), productoUno.getCantidadTotalEnSucursales());
    assertEquals(new BigDecimal("12.000000000000000"), productoDos.getCantidadTotalEnSucursales());
    List<NuevoRenglonPedidoDTO> renglonesPedidoDTO = new ArrayList<>();
    renglonesPedidoDTO.add(
        NuevoRenglonPedidoDTO.builder()
            .idProductoItem(1L)
            .cantidad(new BigDecimal("5.000000000000000"))
            .build());
    renglonesPedidoDTO.add(
        NuevoRenglonPedidoDTO.builder()
            .idProductoItem(2L)
            .cantidad(new BigDecimal("2.000000000000000"))
            .build());
    PedidoDTO pedidoDTO =
        PedidoDTO.builder()
            .descuentoPorcentaje(new BigDecimal("15.000000000000000"))
            .recargoPorcentaje(new BigDecimal("5"))
            .renglones(renglonesPedidoDTO)
            .idSucursal(1L)
            .idCliente(clienteRecuperado.getIdCliente())
            .tipoDeEnvio(TipoDeEnvio.RETIRO_EN_SUCURSAL)
            .build();
    Pedido pedidoRecuperado =
        restTemplate.postForObject(apiPrefix + "/pedidos", pedidoDTO, Pedido.class);
    productoUno = restTemplate.getForObject(apiPrefix + "/productos/1/sucursales/1", Producto.class);
    productoDos = restTemplate.getForObject(apiPrefix + "/productos/2/sucursales/1", Producto.class);
    assertEquals(new BigDecimal("8.000000000000000"), productoUno.getCantidadTotalEnSucursales());
    assertEquals(new BigDecimal("10.000000000000000"), productoDos.getCantidadTotalEnSucursales());
    assertEquals(new BigDecimal("5.000000000000000"), productoUno.getCantidadReservada());
    assertEquals(new BigDecimal("2.000000000000000"), productoDos.getCantidadReservada());
    assertEquals(new BigDecimal("5947.200000000000000"), pedidoRecuperado.getTotal());
    assertEquals(EstadoPedido.ABIERTO, pedidoRecuperado.getEstado());
    List<sic.model.RenglonPedido> renglonesDelPedido =
        Arrays.asList(
            restTemplate.getForObject(
                apiPrefix + "/pedidos/" + pedidoRecuperado.getIdPedido() + "/renglones",
                sic.model.RenglonPedido[].class));
    assertEquals(2, renglonesDelPedido.size());
    assertEquals("Ventilador de pie", renglonesDelPedido.get(0).getDescripcionItem());
    assertEquals("Metro", renglonesDelPedido.get(0).getMedidaItem());
    assertEquals(
        new BigDecimal("1210.000000000000000"), renglonesDelPedido.get(0).getPrecioUnitario());
    assertEquals(new BigDecimal("5.000000000000000"), renglonesDelPedido.get(0).getCantidad());
    assertEquals(
        new BigDecimal("20.000000000000000"),
        renglonesDelPedido.get(0).getBonificacionPorcentaje());
    assertEquals(
        new BigDecimal("242.000000000000000"), renglonesDelPedido.get(0).getBonificacionNeta());
    assertEquals(
        new BigDecimal("6050.000000000000000000000000000000"),
        renglonesDelPedido.get(0).getImporteAnterior());
    assertEquals(
        new BigDecimal("4840.000000000000000000000000000000"),
        renglonesDelPedido.get(0).getImporte());
    assertEquals("Reflector led 100w", renglonesDelPedido.get(1).getDescripcionItem());
    assertEquals("Metro", renglonesDelPedido.get(1).getMedidaItem());
    assertEquals(
        new BigDecimal("1105.000000000000000"), renglonesDelPedido.get(1).getPrecioUnitario());
    assertEquals(new BigDecimal("2.000000000000000"), renglonesDelPedido.get(1).getCantidad());
    assertEquals(
        new BigDecimal("20.000000000000000"),
        renglonesDelPedido.get(1).getBonificacionPorcentaje());
    assertEquals(
        new BigDecimal("221.000000000000000"), renglonesDelPedido.get(1).getBonificacionNeta());
    assertEquals(
        new BigDecimal("2210.000000000000000000000000000000"),
        renglonesDelPedido.get(1).getImporteAnterior());
    assertEquals(
        new BigDecimal("1768.000000000000000000000000000000"),
        renglonesDelPedido.get(1).getImporte());
  }

  @Test
  @DisplayName(
      "Modificar el pedido agregando un nuevo producto y cambiando la cantidad de uno ya existente, reservando y verificando stock")
  @Order(9)
  void testEscenarioModificacionPedido() {
    this.iniciarSesionComoAdministrador();
    BusquedaPedidoCriteria criteria = BusquedaPedidoCriteria.builder().idSucursal(1L).build();
    HttpEntity<BusquedaPedidoCriteria> requestEntity = new HttpEntity<>(criteria);
    PaginaRespuestaRest<Pedido> resultadoBusquedaPedido =
        restTemplate
            .exchange(
                apiPrefix + "/pedidos/busqueda/criteria",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<PaginaRespuestaRest<Pedido>>() {})
            .getBody();
    assertNotNull(resultadoBusquedaPedido);
    List<Pedido> pedidosRecuperados = resultadoBusquedaPedido.getContent();
    assertEquals(1, pedidosRecuperados.size());
    List<sic.model.RenglonPedido> renglonesPedidos =
        Arrays.asList(
            restTemplate.getForObject(
                apiPrefix + "/pedidos/" + pedidosRecuperados.get(0).getIdPedido() + "/renglones",
                sic.model.RenglonPedido[].class));
    assertNotNull(renglonesPedidos);
    assertEquals(2, renglonesPedidos.size());
    List<NuevoRenglonPedidoDTO> renglonesPedidoDTO = new ArrayList<>();
    renglonesPedidos.forEach(
        renglonPedido ->
            renglonesPedidoDTO.add(
                NuevoRenglonPedidoDTO.builder()
                    .idProductoItem(renglonPedido.getIdProductoItem())
                    .cantidad(renglonPedido.getCantidad())
                    .build()));
    renglonesPedidoDTO.get(1).setCantidad(new BigDecimal("3"));
    renglonesPedidoDTO.add(
        NuevoRenglonPedidoDTO.builder().idProductoItem(3L).cantidad(BigDecimal.TEN).build());
    PedidoDTO pedidoDTO =
        PedidoDTO.builder()
            .idPedido(pedidosRecuperados.get(0).getIdPedido())
            .descuentoPorcentaje(new BigDecimal("15.000000000000000"))
            .recargoPorcentaje(new BigDecimal("5"))
            .renglones(renglonesPedidoDTO)
            .idSucursal(1L)
            .idCliente(pedidosRecuperados.get(0).getCliente().getIdCliente())
            .tipoDeEnvio(TipoDeEnvio.RETIRO_EN_SUCURSAL)
            .build();
    restTemplate.put(apiPrefix + "/pedidos", pedidoDTO);
    Producto productoDos = restTemplate.getForObject(apiPrefix + "/productos/2/sucursales/1", Producto.class);
    assertEquals(new BigDecimal("9.000000000000000"), productoDos.getCantidadTotalEnSucursales());
    assertEquals(new BigDecimal("3.000000000000000"), productoDos.getCantidadReservada());
    criteria = BusquedaPedidoCriteria.builder().idSucursal(1L).build();
    requestEntity = new HttpEntity<>(criteria);
    resultadoBusquedaPedido =
        restTemplate
            .exchange(
                apiPrefix + "/pedidos/busqueda/criteria",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<PaginaRespuestaRest<Pedido>>() {})
            .getBody();
    assertNotNull(resultadoBusquedaPedido);
    pedidosRecuperados = resultadoBusquedaPedido.getContent();
    assertEquals(1, pedidosRecuperados.size());
    assertEquals(EstadoPedido.ABIERTO, pedidosRecuperados.get(0).getEstado());
    List<sic.model.RenglonPedido> renglonesDelPedido =
        Arrays.asList(
            restTemplate.getForObject(
                apiPrefix + "/pedidos/" + pedidosRecuperados.get(0).getIdPedido() + "/renglones",
                sic.model.RenglonPedido[].class));
    assertEquals(3, renglonesDelPedido.size());
    assertEquals("Ventilador de pie", renglonesDelPedido.get(0).getDescripcionItem());
    assertEquals("Metro", renglonesDelPedido.get(0).getMedidaItem());
    assertEquals(
        new BigDecimal("1210.000000000000000"), renglonesDelPedido.get(0).getPrecioUnitario());
    assertEquals(new BigDecimal("5.000000000000000"), renglonesDelPedido.get(0).getCantidad());
    assertEquals(
        new BigDecimal("20.000000000000000"),
        renglonesDelPedido.get(0).getBonificacionPorcentaje());
    assertEquals(
        new BigDecimal("242.000000000000000"), renglonesDelPedido.get(0).getBonificacionNeta());
    assertEquals(
        new BigDecimal("6050.000000000000000000000000000000"),
        renglonesDelPedido.get(0).getImporteAnterior());
    assertEquals(
        new BigDecimal("4840.000000000000000000000000000000"),
        renglonesDelPedido.get(0).getImporte());
    assertEquals("Reflector led 100w", renglonesDelPedido.get(1).getDescripcionItem());
    assertEquals("Metro", renglonesDelPedido.get(1).getMedidaItem());
    assertEquals(
        new BigDecimal("1105.000000000000000"), renglonesDelPedido.get(1).getPrecioUnitario());
    assertEquals(new BigDecimal("3.000000000000000"), renglonesDelPedido.get(1).getCantidad());
    assertEquals(
        new BigDecimal("20.000000000000000"),
        renglonesDelPedido.get(1).getBonificacionPorcentaje());
    assertEquals(
        new BigDecimal("221.000000000000000"), renglonesDelPedido.get(1).getBonificacionNeta());
    assertEquals(
        new BigDecimal("3315.000000000000000000000000000000"),
        renglonesDelPedido.get(1).getImporteAnterior());
    assertEquals(
        new BigDecimal("2652.000000000000000000000000000000"),
        renglonesDelPedido.get(1).getImporte());
    assertEquals("Canilla Monocomando", renglonesDelPedido.get(2).getDescripcionItem());
    assertEquals("Metro", renglonesDelPedido.get(2).getMedidaItem());
    assertEquals(
        new BigDecimal("13364.402000000000000"), renglonesDelPedido.get(2).getPrecioUnitario());
    assertEquals(new BigDecimal("10.000000000000000"), renglonesDelPedido.get(2).getCantidad());
    assertEquals(
        new BigDecimal("10.000000000000000"),
        renglonesDelPedido.get(2).getBonificacionPorcentaje());
    assertEquals(
        new BigDecimal("1336.440200000000000"), renglonesDelPedido.get(2).getBonificacionNeta());
    assertEquals(
        new BigDecimal("133644.020000000000000000000000000000"),
        renglonesDelPedido.get(2).getImporteAnterior());
    assertEquals(
        new BigDecimal("120279.618000000000000000000000000000"),
        renglonesDelPedido.get(2).getImporte());
  }

  @Test
  @DisplayName(
      "Facturar pedido al cliente RI con factura dividida, luego saldar la CC con efectivo y verificar stock")
  @Order(10)
  void testEscenarioVenta1() {
    this.iniciarSesionComoAdministrador();
    BusquedaPedidoCriteria criteria = BusquedaPedidoCriteria.builder().idSucursal(1L).build();
    HttpEntity<BusquedaPedidoCriteria> requestEntity = new HttpEntity<>(criteria);
    PaginaRespuestaRest<Pedido> resultadoBusquedaPedido =
        restTemplate
            .exchange(
                apiPrefix + "/pedidos/busqueda/criteria",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<PaginaRespuestaRest<Pedido>>() {})
            .getBody();
    assertNotNull(resultadoBusquedaPedido);
    List<Pedido> pedidosRecuperados = resultadoBusquedaPedido.getContent();
    assertEquals(1, pedidosRecuperados.size());
    List<RenglonFactura> renglones =
        Arrays.asList(
            restTemplate.getForObject(
                apiPrefix
                    + "/facturas/ventas/renglones/pedidos/"
                    + pedidosRecuperados.get(0).getIdPedido()
                    + "?tipoDeComprobante=FACTURA_A",
                RenglonFactura[].class));
    assertEquals("Ventilador de pie", renglones.get(0).getDescripcionItem());
    assertEquals(new BigDecimal("5.000000000000000"), renglones.get(0).getCantidad());
    assertEquals(new BigDecimal("1000.000000000000000"), renglones.get(0).getPrecioUnitario());
    assertEquals(
        new BigDecimal("20.000000000000000"), renglones.get(0).getBonificacionPorcentaje());
    assertEquals(new BigDecimal("200.000000000000000"), renglones.get(0).getBonificacionNeta());
    assertEquals(new BigDecimal("21.000000000000000"), renglones.get(0).getIvaPorcentaje());
    assertEquals(
        new BigDecimal("168.000000000000000000000000000000000000000000000"),
        renglones.get(0).getIvaNeto());
    assertEquals(new BigDecimal("900.000000000000000"), renglones.get(0).getGananciaPorcentaje());
    assertEquals(new BigDecimal("900.000000000000000"), renglones.get(0).getGananciaNeto());
    assertEquals(
        new BigDecimal("6050.000000000000000000000000000000"),
        renglones.get(0).getImporteAnterior());
    assertEquals(
        new BigDecimal("4000.000000000000000000000000000000"), renglones.get(0).getImporte());
    assertEquals("Reflector led 100w", renglones.get(1).getDescripcionItem());
    assertEquals(new BigDecimal("3.000000000000000"), renglones.get(1).getCantidad());
    assertEquals(new BigDecimal("1000.000000000000000"), renglones.get(1).getPrecioUnitario());
    assertEquals(
        new BigDecimal("20.000000000000000"), renglones.get(1).getBonificacionPorcentaje());
    assertEquals(new BigDecimal("200.000000000000000"), renglones.get(1).getBonificacionNeta());
    assertEquals(new BigDecimal("10.500000000000000"), renglones.get(1).getIvaPorcentaje());
    assertEquals(
        new BigDecimal("84.000000000000000000000000000000000000000000000"),
        renglones.get(1).getIvaNeto());
    assertEquals(new BigDecimal("900.000000000000000"), renglones.get(1).getGananciaPorcentaje());
    assertEquals(new BigDecimal("900.000000000000000"), renglones.get(1).getGananciaNeto());
    assertEquals(
        new BigDecimal("3315.000000000000000000000000000000"),
        renglones.get(1).getImporteAnterior());
    assertEquals(
        new BigDecimal("2400.000000000000000000000000000000"), renglones.get(1).getImporte());
    assertEquals("Canilla Monocomando", renglones.get(2).getDescripcionItem());
    assertEquals(new BigDecimal("10.000000000000000"), renglones.get(2).getCantidad());
    assertEquals(new BigDecimal("12094.481000000000000"), renglones.get(2).getPrecioUnitario());
    assertEquals(
        new BigDecimal("10.000000000000000"), renglones.get(2).getBonificacionPorcentaje());
    assertEquals(new BigDecimal("1209.448100000000000"), renglones.get(2).getBonificacionNeta());
    assertEquals(new BigDecimal("10.500000000000000"), renglones.get(2).getIvaPorcentaje());
    assertEquals(
        new BigDecimal("1142.928454500000000000000000000000000000000000000"),
        renglones.get(2).getIvaNeto());
    assertEquals(new BigDecimal("11.370000000000000"), renglones.get(2).getGananciaPorcentaje());
    assertEquals(new BigDecimal("1234.751000000000000"), renglones.get(2).getGananciaNeto());
    assertEquals(
        new BigDecimal("133644.020000000000000000000000000000"),
        renglones.get(2).getImporteAnterior());
    assertEquals(
        new BigDecimal("108850.329000000000000000000000000000"), renglones.get(2).getImporte());
    Cliente cliente = restTemplate.getForObject(apiPrefix + "/clientes/1", Cliente.class);
    assertNotNull(cliente);
    int[] indices = new int[] {0};
    NuevaFacturaVentaDTO nuevaFacturaVentaDTO =
        NuevaFacturaVentaDTO.builder()
            .idCliente(1L)
            .idSucursal(1L)
            .tipoDeComprobante(TipoDeComprobante.FACTURA_A)
            .recargoPorcentaje(new BigDecimal("10"))
            .descuentoPorcentaje(new BigDecimal("25"))
            .indices(indices)
            .build();
    FacturaVenta[] facturas =
        restTemplate.postForObject(
            apiPrefix + "/facturas/ventas/pedidos/" + pedidosRecuperados.get(0).getIdPedido(), nuevaFacturaVentaDTO, FacturaVenta[].class);
    Producto productoUno = restTemplate.getForObject(apiPrefix + "/productos/1/sucursales/1", Producto.class);
    assertEquals(new BigDecimal("8.000000000000000"), productoUno.getCantidadTotalEnSucursales());
    Producto productoDos = restTemplate.getForObject(apiPrefix + "/productos/2/sucursales/1", Producto.class);
    assertEquals(new BigDecimal("9.000000000000000"), productoDos.getCantidadTotalEnSucursales());
    Producto productoTres = restTemplate.getForObject(apiPrefix + "/productos/3/sucursales/1", Producto.class);
    assertEquals(new BigDecimal("0E-15"), productoTres.getCantidadTotalEnSucursales());
    assertEquals(2, facturas.length);
    assertEquals(TipoDeComprobante.FACTURA_A, facturas[1].getTipoComprobante());
    assertEquals(TipoDeComprobante.FACTURA_X, facturas[0].getTipoComprobante());
    assertNotEquals(0L, facturas[1].getCae());
    assertNotNull(
        restTemplate.getForObject(
            apiPrefix + "/facturas/ventas/" + facturas[0].getIdFactura() + "/reporte",
            byte[].class));
    assertNotNull(
        restTemplate.getForObject(
            apiPrefix + "/facturas/ventas/" + facturas[1].getIdFactura() + "/reporte",
            byte[].class));
    assertEquals(cliente.getNombreFiscal(), facturas[0].getNombreFiscalCliente());
    Sucursal sucursal = restTemplate.getForObject(apiPrefix + "/sucursales/1", Sucursal.class);
    assertNotNull(sucursal);
    assertEquals(sucursal.getNombre(), facturas[0].getNombreSucursal());
    assertEquals(cliente.getNombreFiscal(), facturas[1].getNombreFiscalCliente());
    assertEquals(sucursal.getNombre(), facturas[1].getNombreSucursal());
    Usuario credencial = restTemplate.getForObject(apiPrefix + "/usuarios/2", Usuario.class);
    assertNotNull(credencial);
    assertEquals(
        credencial.getNombre()
            + " "
            + credencial.getApellido()
            + " ("
            + credencial.getUsername()
            + ")",
        facturas[0].getNombreUsuario());
    assertEquals(
        credencial.getNombre()
            + " "
            + credencial.getApellido()
            + " ("
            + credencial.getUsername()
            + ")",
        facturas[1].getNombreUsuario());
    assertEquals(new BigDecimal("1600.000000000000000000000000000000"), facturas[0].getSubTotal());
    assertEquals(
        new BigDecimal("113650.329000000000000000000000000000"), facturas[1].getSubTotal());
    assertEquals(new BigDecimal("160.000000000000000"), facturas[0].getRecargoNeto());
    assertEquals(new BigDecimal("11365.032900000000000"), facturas[1].getRecargoNeto());
    assertEquals(new BigDecimal("400.000000000000000"), facturas[0].getDescuentoNeto());
    assertEquals(new BigDecimal("28412.582250000000000"), facturas[1].getDescuentoNeto());
    assertEquals(BigDecimal.ZERO, facturas[0].getIva105Neto());
    assertEquals(
        new BigDecimal(
            "9929.091863250000000000000000000000000000000000000000000000000000000000000000000"),
        facturas[1].getIva105Neto());
    assertEquals(BigDecimal.ZERO, facturas[0].getIva21Neto());
    assertEquals(
        new BigDecimal("428.400000000000000000000000000000000000000000000000000000000000"),
        facturas[1].getIva21Neto());
    assertEquals(
        new BigDecimal("1360.000000000000000000000000000000"), facturas[0].getSubTotalBruto());
    assertEquals(
        new BigDecimal("96602.779650000000000000000000000000"), facturas[1].getSubTotalBruto());
    assertEquals(new BigDecimal("1360.000000000000000000000000000000"), facturas[0].getTotal());
    assertEquals(
        new BigDecimal(
            "106960.271513250000000000000000000000000000000000000000000000000000000000000000000"),
        facturas[1].getTotal());
    List<RenglonFactura> renglonesFacturaUno =
        Arrays.asList(
            restTemplate.getForObject(
                apiPrefix + "/facturas/" + facturas[0].getIdFactura() + "/renglones",
                RenglonFactura[].class));
    List<RenglonFactura> renglonesFacturaDos =
        Arrays.asList(
            restTemplate.getForObject(
                apiPrefix + "/facturas/" + facturas[1].getIdFactura() + "/renglones",
                RenglonFactura[].class));
    assertEquals(2.0, renglonesFacturaUno.get(0).getCantidad().doubleValue());
    assertEquals(3.0, renglonesFacturaDos.get(0).getCantidad().doubleValue());
    assertEquals(3.0, renglonesFacturaDos.get(1).getCantidad().doubleValue());
    assertEquals(10.000000000000000, renglonesFacturaDos.get(2).getCantidad().doubleValue());
    assertEquals(
        -108320.27151325,
        restTemplate
            .getForObject(apiPrefix + "/cuentas-corriente/clientes/1/saldo", BigDecimal.class)
            .doubleValue());
    Recibo recibo =
        Recibo.builder()
            .concepto("Recibo Test")
            .monto(108320.27151325)
            .idSucursal(sucursal.getIdSucursal())
            .idCliente(cliente.getIdCliente())
            .idFormaDePago(1L)
            .build();
    Recibo reciboDeFactura =
        restTemplate.postForObject(apiPrefix + "/recibos/clientes", recibo, Recibo.class);
    assertNotNull(
        restTemplate.getForObject(
            apiPrefix + "/recibos/" + reciboDeFactura.getIdRecibo() + "/reporte", byte[].class));
    assertEquals(recibo, reciboDeFactura);
    assertEquals(
        0.0,
        restTemplate
            .getForObject(apiPrefix + "/cuentas-corriente/clientes/1/saldo", BigDecimal.class)
            .doubleValue());
    criteria = BusquedaPedidoCriteria.builder().idSucursal(1L).build();
    requestEntity = new HttpEntity<>(criteria);
    resultadoBusquedaPedido =
        restTemplate
            .exchange(
                apiPrefix + "/pedidos/busqueda/criteria",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<PaginaRespuestaRest<Pedido>>() {})
            .getBody();
    assertNotNull(resultadoBusquedaPedido);
    pedidosRecuperados = resultadoBusquedaPedido.getContent();
    assertEquals(1, pedidosRecuperados.size());
    assertEquals(EstadoPedido.CERRADO, pedidosRecuperados.get(0).getEstado());
  }

  @Test
  @DisplayName(
          "Dar de alta un transportista, luego crear dos remitos por las facturas anteriores usando ese transportista")
  @Order(11)
  void testEscenarioRemito() {
    this.iniciarSesionComoAdministrador();
    TransportistaDTO transportistaNuevo = new TransportistaDTO();
    UbicacionDTO ubicacionParaTransportista = new UbicacionDTO();
    ubicacionParaTransportista.setIdLocalidad(1L);
    ubicacionParaTransportista.setIdProvincia(1L);
    transportistaNuevo.setUbicacion(ubicacionParaTransportista);
    transportistaNuevo.setNombre("Transportista nuevo");
    transportistaNuevo.setTelefono("3795448866");
    transportistaNuevo.setWeb("transportista.com.ar");
    transportistaNuevo = restTemplate.postForObject(apiPrefix + "/transportistas", transportistaNuevo, TransportistaDTO.class);
    assertEquals("Transportista nuevo", transportistaNuevo.getNombre());
    assertEquals("3795448866", transportistaNuevo.getTelefono());
    assertEquals("transportista.com.ar", transportistaNuevo.getWeb());
    BusquedaFacturaVentaCriteria criteria = BusquedaFacturaVentaCriteria.builder().idSucursal(1L).build();
    HttpEntity<BusquedaFacturaVentaCriteria> requestEntity = new HttpEntity<>(criteria);
    PaginaRespuestaRest<FacturaVenta> resultadoBusquedaFactura =
            restTemplate
                    .exchange(
                            apiPrefix + "/facturas/ventas/busqueda/criteria",
                            HttpMethod.POST,
                            requestEntity,
                            new ParameterizedTypeReference<PaginaRespuestaRest<FacturaVenta>>() {})
                    .getBody();
    assertNotNull(resultadoBusquedaFactura);
    List<FacturaVenta> facturasVenta = resultadoBusquedaFactura.getContent();
    assertEquals(2, facturasVenta.size());
    BigDecimal[] cantidadesDeBultos = new BigDecimal[]{new BigDecimal("6"), BigDecimal.TEN};
    TipoBulto[] tipoBulto = new TipoBulto[]{TipoBulto.CAJA, TipoBulto.ATADO};
    Remito remitoResultante =
        restTemplate.postForObject(
            apiPrefix + "/remitos",
            NuevoRemitoDTO.builder()
                .idFacturaVenta(new long[] {facturasVenta.get(0).getIdFactura(), facturasVenta.get(1).getIdFactura()})
                .cantidadPorBulto(cantidadesDeBultos)
                .tiposDeBulto(tipoBulto)
                .costoDeEnvio(new BigDecimal("25"))
                .idTransportista(1L)
                .pesoTotalEnKg(new BigDecimal("72"))
                .volumenTotalEnM3(new BigDecimal("118"))
                .build(),
            Remito.class);
    assertNotNull(remitoResultante);
    assertEquals(1L, remitoResultante.getIdRemito());
    assertNotNull(remitoResultante.getFecha());
    assertEquals(2L, remitoResultante.getSerie());
    assertEquals(1L, remitoResultante.getNroRemito());
    assertEquals(1L, remitoResultante.getIdCliente());
    assertEquals("Juan Fernando Canete", remitoResultante.getNombreFiscalCliente());
    assertNotNull(remitoResultante.getNroDeCliente());
    assertEquals(CategoriaIVA.RESPONSABLE_INSCRIPTO, remitoResultante.getCategoriaIVACliente());
    assertEquals(1L, remitoResultante.getIdSucursal());
    assertEquals("FirstOfAll", remitoResultante.getNombreSucursal());
    assertEquals(2L, remitoResultante.getIdUsuario());
    assertEquals("Max Power (dueño)", remitoResultante.getNombreUsuario());
    assertEquals("Corrientes Corrientes", remitoResultante.getDetalleEnvio());
    assertEquals(new BigDecimal("25"), remitoResultante.getCostoDeEnvio());
    assertEquals(new BigDecimal("108320.271513250000000"), remitoResultante.getTotalFacturas());
    assertEquals(new BigDecimal("108345.271513250000000"), remitoResultante.getTotal());
    assertEquals(new BigDecimal("72"), remitoResultante.getPesoTotalEnKg());
    assertEquals(new BigDecimal("118"), remitoResultante.getVolumenTotalEnM3());
    assertEquals(new BigDecimal("16"), remitoResultante.getCantidadDeBultos());
    assertEquals(remitoResultante.getNombreTransportista(), transportistaNuevo.getNombre());
    assertNotNull(restTemplate.getForObject(apiPrefix + "/remitos/" + remitoResultante.getIdRemito() + "/reporte", byte[].class));
    resultadoBusquedaFactura =
            restTemplate
                    .exchange(
                            apiPrefix + "/facturas/ventas/busqueda/criteria",
                            HttpMethod.POST,
                            requestEntity,
                            new ParameterizedTypeReference<PaginaRespuestaRest<FacturaVenta>>() {})
                    .getBody();
    assertNotNull(resultadoBusquedaFactura);
    facturasVenta = resultadoBusquedaFactura.getContent();
    assertEquals(2, facturasVenta.size());
    assertNotNull(facturasVenta.get(1).getRemito());
    assertEquals(1L,facturasVenta.get(1).getRemito().getIdRemito());
    assertNotNull(facturasVenta.get(0).getRemito());
    assertEquals( 1L,facturasVenta.get(0).getRemito().getIdRemito());
  }

  @Test
  @DisplayName("Realizar devolucion parcial de productos y verificar saldo CC")
  @Order(12)
  void testEscenarioVenta2() {
    this.iniciarSesionComoAdministrador();
    BusquedaFacturaVentaCriteria criteria =
        BusquedaFacturaVentaCriteria.builder()
            .idSucursal(1L)
            .tipoComprobante(TipoDeComprobante.FACTURA_X)
            .numSerie(2L)
            .numFactura(1L)
            .build();
    HttpEntity<BusquedaFacturaVentaCriteria> requestEntity = new HttpEntity<>(criteria);
    PaginaRespuestaRest<FacturaVenta> resultadoBusqueda =
        restTemplate
            .exchange(
                apiPrefix + "/facturas/ventas/busqueda/criteria",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<PaginaRespuestaRest<FacturaVenta>>() {})
            .getBody();
    assertNotNull(resultadoBusqueda);
    List<FacturaVenta> facturasRecuperadas = resultadoBusqueda.getContent();
    Long[] idsRenglonesFactura = new Long[1];
    idsRenglonesFactura[0] = 3L;
    BigDecimal[] cantidades = new BigDecimal[1];
    cantidades[0] = new BigDecimal("1");
    NuevaNotaCreditoDeFacturaDTO nuevaNotaCreditoDTO =
        NuevaNotaCreditoDeFacturaDTO.builder()
            .idFactura(facturasRecuperadas.get(0).getIdFactura())
            .idsRenglonesFactura(idsRenglonesFactura)
            .cantidades(cantidades)
            .modificaStock(true)
            .motivo("No funcionan.")
            .build();
    NotaCredito notaCreditoCalculada =
        restTemplate.postForObject(
            apiPrefix + "/notas/credito/calculos", nuevaNotaCreditoDTO, NotaCredito.class);
    NotaCredito notaCreditoGuardada =
        restTemplate.postForObject(
            apiPrefix + "/notas/credito/factura", nuevaNotaCreditoDTO, NotaCredito.class);
    assertEquals(TipoDeComprobante.NOTA_CREDITO_X, notaCreditoCalculada.getTipoComprobante());
    BusquedaNotaCriteria criteriaNota =
        BusquedaNotaCriteria.builder()
            .idSucursal(1L)
            .tipoComprobante(TipoDeComprobante.NOTA_CREDITO_X)
            .build();
    HttpEntity<BusquedaNotaCriteria> requestEntityNota = new HttpEntity<>(criteriaNota);
    PaginaRespuestaRest<NotaCredito> resultadoBusquedaNota =
        restTemplate
            .exchange(
                apiPrefix + "/notas/credito/busqueda/criteria",
                HttpMethod.POST,
                requestEntityNota,
                new ParameterizedTypeReference<PaginaRespuestaRest<NotaCredito>>() {})
            .getBody();
    assertNotNull(resultadoBusquedaNota);
    List<NotaCredito> notasRecuperadas = resultadoBusquedaNota.getContent();
    List<sic.model.RenglonNotaCredito> renglones =
        Arrays.asList(
            restTemplate.getForObject(
                apiPrefix + "/notas/renglones/credito/" + notasRecuperadas.get(0).getIdNota(),
                sic.model.RenglonNotaCredito[].class));
    assertNotNull(renglones);
    assertEquals(1, renglones.size());
    // renglones
    assertEquals(new BigDecimal("1.000000000000000"), renglones.get(0).getCantidad());
    assertEquals(new BigDecimal("1000.000000000000000"), renglones.get(0).getPrecioUnitario());
    assertEquals(new BigDecimal("20.000000000000000"), renglones.get(0).getDescuentoPorcentaje());
    assertEquals(new BigDecimal("200.000000000000000"), renglones.get(0).getDescuentoNeto());
    assertEquals(new BigDecimal("21.000000000000000"), renglones.get(0).getIvaPorcentaje());
    assertEquals(0.0, renglones.get(0).getIvaNeto().doubleValue());
    assertEquals(new BigDecimal("1000.000000000000000"), renglones.get(0).getImporte());
    assertEquals(new BigDecimal("800.000000000000000"), renglones.get(0).getImporteBruto());
    assertEquals(new BigDecimal("800.000000000000000"), renglones.get(0).getImporteNeto());
    // pie de nota
    assertEquals(
        new BigDecimal("800.000000000000000000000000000000"), notaCreditoGuardada.getSubTotal());
    assertEquals(new BigDecimal("80.000000000000000"), notaCreditoGuardada.getRecargoNeto());
    assertEquals(new BigDecimal("200.000000000000000"), notaCreditoGuardada.getDescuentoNeto());
    assertEquals(
        new BigDecimal("680.000000000000000000000000000000"),
        notaCreditoGuardada.getSubTotalBruto());
    assertEquals(0.0, notaCreditoGuardada.getIva21Neto().doubleValue());
    assertEquals(BigDecimal.ZERO, notaCreditoGuardada.getIva105Neto());
    assertEquals(
        new BigDecimal("680.000000000000000000000000000000"), notaCreditoGuardada.getTotal());
    assertNotNull(restTemplate.getForObject(apiPrefix + "/notas/1/reporte", byte[].class));
    assertEquals(
        655.0,
        restTemplate
            .getForObject(apiPrefix + "/cuentas-corriente/clientes/1/saldo", BigDecimal.class)
            .doubleValue());
  }

  @Test
  @DisplayName("Registrar un cliente nuevo y enviar un pedido mediante carrito de compra")
  @Order(13)
  void testEscenarioRegistracionYPedidoDelNuevoCliente() {
    RegistracionClienteAndUsuarioDTO registro =
        RegistracionClienteAndUsuarioDTO.builder()
            .apellido("Stark")
            .nombre("Sansa María")
            .categoriaIVA(CategoriaIVA.RESPONSABLE_INSCRIPTO)
            .email("sansa@got.com")
            .telefono("4157899667")
            .password("caraDeMala")
            .recaptcha("111111")
            .nombreFiscal("theRedWolf")
            .build();
    restTemplate.postForObject(apiPrefix + "/registracion", registro, Void.class);
    this.iniciarSesionComoAdministrador();
    Usuario usuario = restTemplate.getForObject(apiPrefix + "/usuarios/4", Usuario.class);
    assertEquals("Sansa María", usuario.getNombre());
    assertEquals("Stark", usuario.getApellido());
    assertTrue(usuario.isHabilitado());
    Cliente cliente = restTemplate.getForObject(apiPrefix + "/clientes/2", Cliente.class);
    assertEquals("theRedWolf", cliente.getNombreFiscal());
    assertEquals(0.0, cliente.getMontoCompraMinima().doubleValue());
    assertFalse(cliente.isPuedeComprarAPlazo());
    sic.model.CuentaCorrienteCliente cuentaCorrienteCliente =
        restTemplate.getForObject(
            apiPrefix + "/cuentas-corriente/clientes/" + cliente.getIdCliente(),
            sic.model.CuentaCorrienteCliente.class);
    assertNotNull(cuentaCorrienteCliente);
    assertEquals(0.0, cuentaCorrienteCliente.getSaldo().doubleValue());
    cliente.setUbicacionFacturacion(Ubicacion.builder().idLocalidad(2L).idProvincia(2L).build());
    restTemplate.put(apiPrefix + "/clientes", cliente);
    this.token =
        restTemplate
            .postForEntity(
                apiPrefix + "/login",
                new Credencial(usuario.getUsername(), "caraDeMala"),
                String.class)
            .getBody();
    assertNotNull(this.token);
    restTemplate.postForObject(
        apiPrefix + "/carrito-compra/productos/1?cantidad=5", null, ItemCarritoCompra.class);
    restTemplate.postForObject(
        apiPrefix + "/carrito-compra/productos/2?cantidad=9", null, ItemCarritoCompra.class);
    ItemCarritoCompra item1 =
        restTemplate.getForObject(
            apiPrefix + "/carrito-compra/productos/1/sucursales/1", ItemCarritoCompra.class);
    assertNotNull(item1);
    assertEquals(1L, item1.getProducto().getIdProducto().longValue());
    assertEquals(5, item1.getCantidad().doubleValue());
    ItemCarritoCompra item2 =
        restTemplate.getForObject(
            apiPrefix + "/carrito-compra/productos/2/sucursales/1", ItemCarritoCompra.class);
    assertNotNull(item2);
    assertEquals(2L, item2.getProducto().getIdProducto().longValue());
    assertEquals(9, item2.getCantidad().doubleValue());
    NuevaOrdenDePagoDTO nuevaOrdenDePagoDTO =
        NuevaOrdenDePagoDTO.builder()
                .idSucursal(1L)
                .tipoDeEnvio(TipoDeEnvio.USAR_UBICACION_FACTURACION)
                .build();
    Pedido pedido =
        restTemplate.postForObject(
            apiPrefix + "/carrito-compra", nuevaOrdenDePagoDTO, Pedido.class);
    assertNotNull(pedido);
    assertEquals(14, pedido.getCantidadArticulos().doubleValue());
    assertEquals(new BigDecimal("12796.000000000000000"), pedido.getTotal());
    assertEquals(EstadoPedido.ABIERTO, pedido.getEstado());
    List<sic.model.RenglonPedido> renglonesDelPedido =
        Arrays.asList(
            restTemplate.getForObject(
                apiPrefix + "/pedidos/" + pedido.getIdPedido() + "/renglones",
                sic.model.RenglonPedido[].class));
    assertEquals(2, renglonesDelPedido.size());
    assertEquals("Reflector led 100w", renglonesDelPedido.get(0).getDescripcionItem());
    assertEquals("Metro", renglonesDelPedido.get(0).getMedidaItem());
    assertEquals(
        new BigDecimal("1105.000000000000000"), renglonesDelPedido.get(0).getPrecioUnitario());
    assertEquals(new BigDecimal("9.000000000000000"), renglonesDelPedido.get(0).getCantidad());
    assertEquals(
        new BigDecimal("20.000000000000000"),
        renglonesDelPedido.get(0).getBonificacionPorcentaje());
    assertEquals(
        new BigDecimal("221.000000000000000"), renglonesDelPedido.get(0).getBonificacionNeta());
    assertEquals(
        new BigDecimal("9945.000000000000000000000000000000"),
        renglonesDelPedido.get(0).getImporteAnterior());
    assertEquals(
        new BigDecimal("7956.000000000000000000000000000000"),
        renglonesDelPedido.get(0).getImporte());
    assertEquals("Ventilador de pie", renglonesDelPedido.get(1).getDescripcionItem());
    assertEquals("Metro", renglonesDelPedido.get(1).getMedidaItem());
    assertEquals(
        new BigDecimal("1210.000000000000000"), renglonesDelPedido.get(1).getPrecioUnitario());
    assertEquals(new BigDecimal("5.000000000000000"), renglonesDelPedido.get(1).getCantidad());
    assertEquals(
        new BigDecimal("20.000000000000000"),
        renglonesDelPedido.get(1).getBonificacionPorcentaje());
    assertEquals(
        new BigDecimal("242.000000000000000"), renglonesDelPedido.get(1).getBonificacionNeta());
    assertEquals(
        new BigDecimal("6050.000000000000000000000000000000"),
        renglonesDelPedido.get(1).getImporteAnterior());
    assertEquals(
        new BigDecimal("4840.000000000000000000000000000000"),
        renglonesDelPedido.get(1).getImporte());
  }

  @Test
  @DisplayName("Cerrar caja y verificar movimientos")
  @Order(14)
  void testEscenarioCerrarCaja1() {
    this.iniciarSesionComoAdministrador();
    List<Sucursal> sucursales =
        Arrays.asList(restTemplate.getForObject(apiPrefix + "/sucursales", Sucursal[].class));
    assertNotNull(sucursales);
    assertEquals(1, sucursales.size());
    BusquedaCajaCriteria criteriaParaBusquedaCaja =
        BusquedaCajaCriteria.builder().idSucursal(sucursales.get(0).getIdSucursal()).build();
    HttpEntity<BusquedaCajaCriteria> requestEntityParaProveedores =
        new HttpEntity<>(criteriaParaBusquedaCaja);
    PaginaRespuestaRest<Caja> resultadosBusquedaCaja =
        restTemplate
            .exchange(
                apiPrefix + "/cajas/busqueda/criteria",
                HttpMethod.POST,
                requestEntityParaProveedores,
                new ParameterizedTypeReference<PaginaRespuestaRest<Caja>>() {})
            .getBody();
    assertNotNull(resultadosBusquedaCaja);
    List<Caja> cajasRecuperadas = resultadosBusquedaCaja.getContent();
    assertEquals(1, cajasRecuperadas.size());
    assertEquals(EstadoCaja.ABIERTA, cajasRecuperadas.get(0).getEstado());
    restTemplate.put(
        apiPrefix + "/cajas/" + cajasRecuperadas.get(0).getIdCaja() + "/cierre?monto=5276.66",
        null);
    resultadosBusquedaCaja =
        restTemplate
            .exchange(
                apiPrefix + "/cajas/busqueda/criteria",
                HttpMethod.POST,
                requestEntityParaProveedores,
                new ParameterizedTypeReference<PaginaRespuestaRest<Caja>>() {})
            .getBody();
    assertNotNull(resultadosBusquedaCaja);
    cajasRecuperadas = resultadosBusquedaCaja.getContent();
    assertEquals(1, cajasRecuperadas.size());
    assertEquals(EstadoCaja.CERRADA, cajasRecuperadas.get(0).getEstado());
    assertEquals(
        new BigDecimal("1000.000000000000000"), cajasRecuperadas.get(0).getSaldoApertura());
    List<MovimientoCaja> movimientoCajas =
        Arrays.asList(
            restTemplate.getForObject(
                apiPrefix
                    + "/cajas/"
                    + cajasRecuperadas.get(0).getIdCaja()
                    + "/movimientos?idFormaDePago=1",
                MovimientoCaja[].class));
    assertEquals(2, movimientoCajas.size());
    assertEquals(new BigDecimal("108320.271513250000000"), movimientoCajas.get(0).getMonto());
    assertEquals(new BigDecimal("-500.000000000000000"), movimientoCajas.get(1).getMonto());
    movimientoCajas =
        Arrays.asList(
            restTemplate.getForObject(
                apiPrefix
                    + "/cajas/"
                    + cajasRecuperadas.get(0).getIdCaja()
                    + "/movimientos?idFormaDePago=2",
                MovimientoCaja[].class));
    assertEquals(1, movimientoCajas.size());
    assertEquals(new BigDecimal("-554.540000000000000"), movimientoCajas.get(0).getMonto());
    assertEquals(
        new BigDecimal("108820.271513250000000"),
        restTemplate.getForObject(
            apiPrefix + "/cajas/" + cajasRecuperadas.get(0).getIdCaja() + "/saldo-afecta-caja",
            BigDecimal.class));
    assertEquals(
        new BigDecimal("108265.731513250000000"),
        restTemplate.getForObject(
            apiPrefix + "/cajas/" + cajasRecuperadas.get(0).getIdCaja() + "/saldo-sistema",
            BigDecimal.class));
  }

  @Test
  @DisplayName("Reabrir caja, corregir saldo con un gasto por $750 en efectivo")
  @Order(15)
  void testEscenarioCerrarCaja2() {
    this.iniciarSesionComoAdministrador();
    List<Sucursal> sucursales =
        Arrays.asList(restTemplate.getForObject(apiPrefix + "/sucursales", Sucursal[].class));
    assertEquals(1, sucursales.size());
    BusquedaCajaCriteria criteriaParaBusquedaCaja =
        BusquedaCajaCriteria.builder().idSucursal(sucursales.get(0).getIdSucursal()).build();
    HttpEntity<BusquedaCajaCriteria> requestEntityParaProveedores =
        new HttpEntity<>(criteriaParaBusquedaCaja);
    PaginaRespuestaRest<Caja> resultadosBusquedaCaja =
        restTemplate
            .exchange(
                apiPrefix + "/cajas/busqueda/criteria",
                HttpMethod.POST,
                requestEntityParaProveedores,
                new ParameterizedTypeReference<PaginaRespuestaRest<Caja>>() {})
            .getBody();
    assertNotNull(resultadosBusquedaCaja);
    List<Caja> cajasRecuperadas = resultadosBusquedaCaja.getContent();
    assertEquals(1, cajasRecuperadas.size());
    assertEquals(EstadoCaja.CERRADA, cajasRecuperadas.get(0).getEstado());
    restTemplate.put(
        apiPrefix + "/cajas/" + cajasRecuperadas.get(0).getIdCaja() + "/reapertura?monto=1100",
        null);
    resultadosBusquedaCaja =
        restTemplate
            .exchange(
                apiPrefix + "/cajas/busqueda/criteria",
                HttpMethod.POST,
                requestEntityParaProveedores,
                new ParameterizedTypeReference<PaginaRespuestaRest<Caja>>() {})
            .getBody();
    assertNotNull(resultadosBusquedaCaja);
    cajasRecuperadas = resultadosBusquedaCaja.getContent();
    assertEquals(1, cajasRecuperadas.size());
    assertEquals(EstadoCaja.ABIERTA, cajasRecuperadas.get(0).getEstado());
    assertEquals(
        new BigDecimal("1100.000000000000000"), cajasRecuperadas.get(0).getSaldoApertura());
    NuevoGastoDTO nuevoGasto =
        NuevoGastoDTO.builder()
            .concepto("Gasto olvidado")
            .monto(new BigDecimal("750"))
            .idSucursal(1L)
            .idFormaDePago(1L)
            .build();
    Gasto gasto =
        restTemplate.postForObject(
            apiPrefix + "/gastos", nuevoGasto, Gasto.class);
   assertNotNull(gasto);
    List<MovimientoCaja> movimientoCajas =
        Arrays.asList(
            restTemplate.getForObject(
                apiPrefix
                    + "/cajas/"
                    + cajasRecuperadas.get(0).getIdCaja()
                    + "/movimientos?idFormaDePago=1",
                MovimientoCaja[].class));
    assertEquals(3, movimientoCajas.size());
    assertEquals(new BigDecimal("-750.000000000000000"), movimientoCajas.get(0).getMonto());
    assertEquals(new BigDecimal("108320.271513250000000"), movimientoCajas.get(1).getMonto());
    assertEquals(new BigDecimal("-500.000000000000000"), movimientoCajas.get(2).getMonto());
    movimientoCajas =
        Arrays.asList(
            restTemplate.getForObject(
                apiPrefix
                    + "/cajas/"
                    + cajasRecuperadas.get(0).getIdCaja()
                    + "/movimientos?idFormaDePago=2",
                MovimientoCaja[].class));
    assertEquals(1, movimientoCajas.size());
    assertEquals(new BigDecimal("-554.540000000000000"), movimientoCajas.get(0).getMonto());
    assertEquals(
        new BigDecimal("108170.271513250000000"),
        restTemplate.getForObject(
            apiPrefix + "/cajas/" + cajasRecuperadas.get(0).getIdCaja() + "/saldo-afecta-caja",
            BigDecimal.class));
    assertEquals(
        new BigDecimal("107615.731513250000000"),
        restTemplate.getForObject(
            apiPrefix + "/cajas/" + cajasRecuperadas.get(0).getIdCaja() + "/saldo-sistema",
            BigDecimal.class));
  }

  @Test
  @DisplayName("Facturar un pedido, luego intentar cancelarlo sin éxito")
  @Order(16)
  void testEscenarioFacturarPedidoAndIntentarEliminarlo() {
    this.iniciarSesionComoAdministrador();
    BusquedaProductoCriteria productosCriteria = BusquedaProductoCriteria.builder().build();
    HttpEntity<BusquedaProductoCriteria> requestEntityProductos =
        new HttpEntity<>(productosCriteria);
    PaginaRespuestaRest<Producto> resultadoBusqueda =
        restTemplate
            .exchange(
                apiPrefix + "/productos/busqueda/criteria/sucursales/1",
                HttpMethod.POST,
                requestEntityProductos,
                new ParameterizedTypeReference<PaginaRespuestaRest<Producto>>() {})
            .getBody();
    assertNotNull(resultadoBusqueda);
    List<Producto> productosRecuperados = resultadoBusqueda.getContent();
    assertEquals(
        new BigDecimal("0E-15"), productosRecuperados.get(2).getCantidadTotalEnSucursales());
    assertEquals(
        new BigDecimal("4.000000000000000"),
        productosRecuperados.get(3).getCantidadTotalEnSucursales());
    BusquedaPedidoCriteria pedidoCriteria =
        BusquedaPedidoCriteria.builder().idSucursal(1L).estadoPedido(EstadoPedido.ABIERTO).build();
    HttpEntity<BusquedaPedidoCriteria> requestEntity = new HttpEntity<>(pedidoCriteria);
    PaginaRespuestaRest<Pedido> resultadoBusquedaPedido =
        restTemplate
            .exchange(
                apiPrefix + "/pedidos/busqueda/criteria",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<PaginaRespuestaRest<Pedido>>() {})
            .getBody();
    assertNotNull(resultadoBusquedaPedido);
    List<Pedido> pedidosRecuperados = resultadoBusquedaPedido.getContent();
    assertEquals(1, pedidosRecuperados.size());
    List<RenglonFactura> renglones =
        Arrays.asList(
            restTemplate.getForObject(
                apiPrefix
                    + "/facturas/ventas/renglones/pedidos/"
                    + pedidosRecuperados.get(0).getIdPedido()
                    + "?tipoDeComprobante=FACTURA_X",
                RenglonFactura[].class));
    assertEquals("Reflector led 100w", renglones.get(0).getDescripcionItem());
    assertEquals(new BigDecimal("9.000000000000000"), renglones.get(0).getCantidad());
    assertEquals(new BigDecimal("1000.000000000000000"), renglones.get(0).getPrecioUnitario());
    assertEquals(
        new BigDecimal("20.000000000000000"), renglones.get(0).getBonificacionPorcentaje());
    assertEquals(new BigDecimal("200.000000000000000"), renglones.get(0).getBonificacionNeta());
    assertEquals(new BigDecimal("10.500000000000000"), renglones.get(0).getIvaPorcentaje());
    assertEquals(new BigDecimal("0"), renglones.get(0).getIvaNeto());
    assertEquals(new BigDecimal("900.000000000000000"), renglones.get(0).getGananciaPorcentaje());
    assertEquals(new BigDecimal("900.000000000000000"), renglones.get(0).getGananciaNeto());
    assertEquals(
        new BigDecimal("9945.000000000000000000000000000000"),
        renglones.get(0).getImporteAnterior());
    assertEquals(
        new BigDecimal("7200.000000000000000000000000000000"), renglones.get(0).getImporte());
    assertEquals("Ventilador de pie", renglones.get(1).getDescripcionItem());
    assertEquals(new BigDecimal("5.000000000000000"), renglones.get(1).getCantidad());
    assertEquals(new BigDecimal("1000.000000000000000"), renglones.get(1).getPrecioUnitario());
    assertEquals(
        new BigDecimal("20.000000000000000"), renglones.get(1).getBonificacionPorcentaje());
    assertEquals(new BigDecimal("200.000000000000000"), renglones.get(1).getBonificacionNeta());
    assertEquals(new BigDecimal("21.000000000000000"), renglones.get(1).getIvaPorcentaje());
    assertEquals(new BigDecimal("0"), renglones.get(1).getIvaNeto());
    assertEquals(new BigDecimal("900.000000000000000"), renglones.get(1).getGananciaPorcentaje());
    assertEquals(new BigDecimal("900.000000000000000"), renglones.get(1).getGananciaNeto());
    assertEquals(
        new BigDecimal("6050.000000000000000000000000000000"),
        renglones.get(1).getImporteAnterior());
    assertEquals(
        new BigDecimal("4000.000000000000000000000000000000"), renglones.get(1).getImporte());
    Cliente cliente = restTemplate.getForObject(apiPrefix + "/clientes/2", Cliente.class);
    assertNotNull(cliente);
    int[] indices = new int[] {0};
    NuevaFacturaVentaDTO nuevaFacturaVentaDTO =
        NuevaFacturaVentaDTO.builder()
            .idCliente(pedidosRecuperados.get(0).getCliente().getIdCliente())
            .idSucursal(pedidosRecuperados.get(0).getIdSucursal())
            .tipoDeComprobante(TipoDeComprobante.FACTURA_X)
            .recargoPorcentaje(new BigDecimal("10"))
            .descuentoPorcentaje(new BigDecimal("25"))
            .indices(indices)
            .build();
    restTemplate.postForObject(
        apiPrefix + "/facturas/ventas/pedidos/" + pedidosRecuperados.get(0).getIdPedido(), nuevaFacturaVentaDTO, FacturaVenta[].class);
    pedidoCriteria =
        BusquedaPedidoCriteria.builder().idSucursal(1L).estadoPedido(EstadoPedido.CERRADO).build();
    requestEntity = new HttpEntity<>(pedidoCriteria);
    resultadoBusquedaPedido =
        restTemplate
            .exchange(
                apiPrefix + "/pedidos/busqueda/criteria",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<PaginaRespuestaRest<Pedido>>() {})
            .getBody();
    assertNotNull(resultadoBusquedaPedido);
    pedidosRecuperados = resultadoBusquedaPedido.getContent();
    assertEquals(2, pedidosRecuperados.size());
    assertEquals(EstadoPedido.CERRADO, pedidosRecuperados.get(0).getEstado());
    resultadoBusqueda =
        restTemplate
            .exchange(
                apiPrefix + "/productos/busqueda/criteria/sucursales/1",
                HttpMethod.POST,
                requestEntityProductos,
                new ParameterizedTypeReference<PaginaRespuestaRest<Producto>>() {})
            .getBody();
    assertNotNull(resultadoBusqueda);
    productosRecuperados = resultadoBusqueda.getContent();
    assertEquals(
        new BigDecimal("0E-15"), productosRecuperados.get(2).getCantidadTotalEnSucursales());
    assertEquals(
        new BigDecimal("4.000000000000000"),
        productosRecuperados.get(3).getCantidadTotalEnSucursales());
    RestClientResponseException thrown =
        assertThrows(
            RestClientResponseException.class,
            () -> restTemplate.put(apiPrefix + "/pedidos/2", null));
    assertNotNull(thrown.getMessage());
    assertTrue(
        thrown
            .getMessage()
            .contains(
                messageSource.getMessage(
                    "mensaje_no_se_puede_cancelar_pedido",
                    new Object[] {EstadoPedido.CERRADO},
                    Locale.getDefault())));
  }

  @Test
  @DisplayName("Actualizar stock de un producto para tener cantidades en dos sucursales")
  @Order(17)
  void testEscenarioActualizarStockParaDosSucursales() {
    this.iniciarSesionComoAdministrador();
    Cliente clienteParaEditar = restTemplate.getForObject(apiPrefix + "/clientes/2", Cliente.class);
    clienteParaEditar.setPuedeComprarAPlazo(true);
    restTemplate.put(apiPrefix + "/clientes", clienteParaEditar);
    Sucursal sucursalNueva =
            Sucursal.builder()
                    .nombre("Sucursal Centrica")
                    .categoriaIVA(CategoriaIVA.RESPONSABLE_INSCRIPTO)
                    .email("sucursalNueva@gmail.com")
                    .idFiscal(30711184569L)
                    .ubicacion(Ubicacion.builder().idLocalidad(1L).idProvincia(1L).build())
                    .build();
    Sucursal sucursalRecuperada =
            restTemplate.postForObject(apiPrefix + "/sucursales", sucursalNueva, Sucursal.class);
    assertEquals(sucursalNueva, sucursalRecuperada);
    ConfiguracionSucursal configuracionSucursal =
            restTemplate.getForObject(
                    apiPrefix + "/configuraciones-sucursal/" + sucursalRecuperada.getIdSucursal(),
                    ConfiguracionSucursal.class);
    configuracionSucursal.setPuntoDeRetiro(true);
    restTemplate.put(apiPrefix + "/configuraciones-sucursal", configuracionSucursal);
    configuracionSucursal =
            restTemplate.getForObject(
                    apiPrefix + "/configuraciones-sucursal/" + sucursalRecuperada.getIdSucursal(),
                    ConfiguracionSucursal.class);
    assertTrue(configuracionSucursal.isPuntoDeRetiro());
    assertFalse(configuracionSucursal.isComparteStock());
    List<Sucursal> sucursales =
            Arrays.asList(restTemplate.getForObject(apiPrefix + "/sucursales", Sucursal[].class));
    assertFalse(sucursales.isEmpty());
    assertEquals(2, sucursales.size());
    BusquedaProductoCriteria productosCriteria = BusquedaProductoCriteria.builder()
            .descripcion("Corta Papas - Vegetales").build();
    HttpEntity<BusquedaProductoCriteria> requestEntityProductos =
            new HttpEntity<>(productosCriteria);
    PaginaRespuestaRest<Producto> resultadoBusqueda =
            restTemplate
                    .exchange(
                            apiPrefix + "/productos/busqueda/criteria/sucursales/1",
                            HttpMethod.POST,
                            requestEntityProductos,
                            new ParameterizedTypeReference<PaginaRespuestaRest<Producto>>() {})
                    .getBody();
    assertNotNull(resultadoBusqueda);
    Set<CantidadEnSucursalDTO> cantidadEnSucursales = new HashSet<>();
    CantidadEnSucursalDTO cantidadEnSucursalUno = CantidadEnSucursalDTO.builder()
            .cantidad(new BigDecimal("40"))
            .idSucursal(1L)
            .build();
    CantidadEnSucursalDTO cantidadEnSucursalDos = CantidadEnSucursalDTO.builder()
            .cantidad(new BigDecimal("100"))
            .idSucursal(2L)
            .build();
    cantidadEnSucursales.add(cantidadEnSucursalUno);
    cantidadEnSucursales.add(cantidadEnSucursalDos);
    ProductoDTO productoParaActualizar =
        ProductoDTO.builder()
            .idProducto(resultadoBusqueda.getContent().get(0).getIdProducto())
            .cantidadEnSucursales(cantidadEnSucursales)
            .precioCosto(CIEN)
            .gananciaPorcentaje(new BigDecimal("900"))
            .gananciaNeto(new BigDecimal("900"))
            .precioVentaPublico(new BigDecimal("1000"))
            .ivaPorcentaje(new BigDecimal("10.5"))
            .ivaNeto(new BigDecimal("105"))
            .precioLista(new BigDecimal("1105"))
            .porcentajeBonificacionPrecio(new BigDecimal("20"))
            .build();
    restTemplate.put(apiPrefix + "/productos", productoParaActualizar);
    Producto productoParaControlarStock =
        restTemplate.getForObject(
            apiPrefix + "/productos/" + resultadoBusqueda.getContent().get(0).getIdProducto() + "/sucursales/1",
            Producto.class);
    assertEquals(140, productoParaControlarStock.getCantidadTotalEnSucursales().doubleValue());
  }

  @Test
  @DisplayName("Realizar un pedido que requiera del stock de ambas")
  @Order(18)
  void testEscenarioPedidoConStockDeDosSucursales() {
    this.iniciarSesionComoAdministrador();
    Usuario usuario = restTemplate.getForObject(apiPrefix + "/usuarios/4", Usuario.class);
    this.token =
        restTemplate
            .postForEntity(
                apiPrefix + "/login",
                new Credencial(usuario.getUsername(), "caraDeMala"),
                String.class)
            .getBody();
    assertNotNull(this.token);
    restTemplate.postForObject(
            apiPrefix + "/carrito-compra/productos/4?cantidad=50", null, ItemCarritoCompra.class);
    ItemCarritoCompra item1 =
            restTemplate.getForObject(
                    apiPrefix + "/carrito-compra/productos/4/sucursales/1", ItemCarritoCompra.class);
    assertNotNull(item1);
    assertEquals(4L, item1.getProducto().getIdProducto().longValue());
    assertEquals(50, item1.getCantidad().doubleValue());
    NuevaOrdenDePagoDTO nuevaOrdenDePagoDTO = NuevaOrdenDePagoDTO.builder()
            .idSucursal(1L)
            .tipoDeEnvio(TipoDeEnvio.USAR_UBICACION_FACTURACION)
            .build();
    RestClientResponseException thrown =
            assertThrows(
                    RestClientResponseException.class,
                    () ->
                            restTemplate.postForObject(
                                    apiPrefix + "/carrito-compra", nuevaOrdenDePagoDTO, Pedido.class));
    assertNotNull(thrown.getMessage());
    assertTrue(
            thrown
                    .getMessage()
                    .contains(
                            messageSource.getMessage("mensaje_pedido_sin_stock", null, Locale.getDefault())));
    this.iniciarSesionComoAdministrador();
    ConfiguracionSucursal configuracionSucursal =
            restTemplate.getForObject(
                    apiPrefix + "/configuraciones-sucursal/2",
                    ConfiguracionSucursal.class);
    configuracionSucursal.setComparteStock(true);
    restTemplate.put(apiPrefix + "/configuraciones-sucursal", configuracionSucursal);
    configuracionSucursal =
            restTemplate.getForObject(
                    apiPrefix + "/configuraciones-sucursal/2",
                    ConfiguracionSucursal.class);
    assertTrue(configuracionSucursal.isPuntoDeRetiro());
    assertTrue(configuracionSucursal.isComparteStock());
    usuario = restTemplate.getForObject(apiPrefix + "/usuarios/4", Usuario.class);
    this.token =
        restTemplate
            .postForEntity(
                apiPrefix + "/login",
                new Credencial(usuario.getUsername(), "caraDeMala"),
                String.class)
            .getBody();
    assertNotNull(this.token);
    Pedido pedido =
        restTemplate.postForObject(
            apiPrefix + "/carrito-compra", nuevaOrdenDePagoDTO, Pedido.class);
    assertEquals(new BigDecimal("50.000000000000000") , pedido.getCantidadArticulos());
    this.iniciarSesionComoAdministrador();
    BusquedaProductoCriteria productosCriteria = BusquedaProductoCriteria.builder()
            .descripcion("Corta Papas - Vegetales").build();
    HttpEntity<BusquedaProductoCriteria> requestEntityProductos =
            new HttpEntity<>(productosCriteria);
    PaginaRespuestaRest<Producto> resultadoBusqueda =
            restTemplate
                    .exchange(
                            apiPrefix + "/productos/busqueda/criteria/sucursales/1",
                            HttpMethod.POST,
                            requestEntityProductos,
                            new ParameterizedTypeReference<PaginaRespuestaRest<Producto>>() {})
                    .getBody();
    assertNotNull(resultadoBusqueda);
    Producto productoParaControlarStock =
            restTemplate.getForObject(
                    apiPrefix + "/productos/" + resultadoBusqueda.getContent().get(0).getIdProducto() + "/sucursales/1",
                    Producto.class);
    assertEquals(90 , productoParaControlarStock.getCantidadTotalEnSucursales().doubleValue());
    List<CantidadEnSucursal> cantidadEnSucursalesParaAssert = new ArrayList<>(productoParaControlarStock.getCantidadEnSucursales());
    assertEquals(new BigDecimal("0E-15") , cantidadEnSucursalesParaAssert.get(0).getCantidad());
    assertEquals(new BigDecimal("90.000000000000000") , cantidadEnSucursalesParaAssert.get(1).getCantidad());
    assertEquals(1L , cantidadEnSucursalesParaAssert.get(0).getIdSucursal());
    assertEquals(2L , cantidadEnSucursalesParaAssert.get(1).getIdSucursal());
  }

  @Test
  @DisplayName("Facturar el pedido anterior")
  @Order(19)
  void testEscenarioFacturarPedido() {
    this.iniciarSesionComoAdministrador();
    BusquedaPedidoCriteria pedidoCriteria =
            BusquedaPedidoCriteria.builder().idSucursal(1L).estadoPedido(EstadoPedido.ABIERTO).build();
    HttpEntity<BusquedaPedidoCriteria> requestEntity = new HttpEntity<>(pedidoCriteria);
    PaginaRespuestaRest<Pedido> resultadoBusquedaPedido =
            restTemplate
                    .exchange(
                            apiPrefix + "/pedidos/busqueda/criteria",
                            HttpMethod.POST,
                            requestEntity,
                            new ParameterizedTypeReference<PaginaRespuestaRest<Pedido>>() {})
                    .getBody();
    assertNotNull(resultadoBusquedaPedido);
    List<Pedido> pedidosRecuperados = resultadoBusquedaPedido.getContent();
    assertEquals(1, pedidosRecuperados.size());
    List<RenglonFactura> renglones =
            Arrays.asList(
                    restTemplate.getForObject(
                            apiPrefix
                                    + "/facturas/ventas/renglones/pedidos/"
                                    + pedidosRecuperados.get(0).getIdPedido()
                                    + "?tipoDeComprobante=FACTURA_X",
                            RenglonFactura[].class));
    assertEquals("Corta Papas - Vegetales", renglones.get(0).getDescripcionItem());
    assertEquals(new BigDecimal("50.000000000000000"), renglones.get(0).getCantidad());
    assertEquals(new BigDecimal("1000.000000000000000"), renglones.get(0).getPrecioUnitario());
    assertEquals(
            new BigDecimal("20.000000000000000"), renglones.get(0).getBonificacionPorcentaje());
    assertEquals(new BigDecimal("200.000000000000000"), renglones.get(0).getBonificacionNeta());
    assertEquals(new BigDecimal("10.500000000000000"), renglones.get(0).getIvaPorcentaje());
    assertEquals(new BigDecimal("0"), renglones.get(0).getIvaNeto());
    assertEquals(new BigDecimal("900.000000000000000"), renglones.get(0).getGananciaPorcentaje());
    assertEquals(new BigDecimal("900.000000000000000"), renglones.get(0).getGananciaNeto());
    assertEquals(
            new BigDecimal("55250.000000000000000000000000000000"),
            renglones.get(0).getImporteAnterior());
    assertEquals(
            new BigDecimal("40000.000000000000000000000000000000"), renglones.get(0).getImporte());
    int[] indices = new int[] {0};
    NuevaFacturaVentaDTO nuevaFacturaVentaDTO =
            NuevaFacturaVentaDTO.builder()
                    .idCliente(pedidosRecuperados.get(0).getCliente().getIdCliente())
                    .idSucursal(pedidosRecuperados.get(0).getIdSucursal())
                    .tipoDeComprobante(TipoDeComprobante.FACTURA_X)
                    .recargoPorcentaje(new BigDecimal("10"))
                    .descuentoPorcentaje(new BigDecimal("25"))
                    .indices(indices)
                    .build();
    restTemplate.postForObject(
            apiPrefix + "/facturas/ventas/pedidos/" + pedidosRecuperados.get(0).getIdPedido(), nuevaFacturaVentaDTO, FacturaVenta[].class);
  }

  @Test
  @DisplayName("Verificar stock y cerrar caja")
  @Order(20)
  void testEscenarioVerificarStockAndCerrarCaja() {
    this.iniciarSesionComoAdministrador();
    BusquedaProductoCriteria productosCriteria = BusquedaProductoCriteria.builder().build();
    HttpEntity<BusquedaProductoCriteria> requestEntityProductos =
        new HttpEntity<>(productosCriteria);
    PaginaRespuestaRest<Producto> resultadoBusqueda =
        restTemplate
            .exchange(
                apiPrefix + "/productos/busqueda/criteria/sucursales/1",
                HttpMethod.POST,
                requestEntityProductos,
                new ParameterizedTypeReference<PaginaRespuestaRest<Producto>>() {})
            .getBody();
    assertNotNull(resultadoBusqueda);
    List<Producto> productosRecuperados = resultadoBusqueda.getContent();
    assertEquals(
        new BigDecimal("0E-15"),
        productosRecuperados.get(0).getCantidadTotalEnSucursales());
    assertEquals(
        new BigDecimal("90.000000000000000"),
        productosRecuperados.get(1).getCantidadTotalEnSucursales());
    assertEquals(
            new BigDecimal("0E-15"), productosRecuperados.get(2).getCantidadTotalEnSucursales());
    assertEquals(
            new BigDecimal("4.000000000000000"),
            productosRecuperados.get(3).getCantidadTotalEnSucursales());
    List<Sucursal> sucursales =
        Arrays.asList(restTemplate.getForObject(apiPrefix + "/sucursales", Sucursal[].class));
    assertEquals(2, sucursales.size());
    BusquedaCajaCriteria criteriaParaBusquedaCaja =
        BusquedaCajaCriteria.builder().idSucursal(sucursales.get(0).getIdSucursal()).build();
    HttpEntity<BusquedaCajaCriteria> requestEntityParaProveedores =
        new HttpEntity<>(criteriaParaBusquedaCaja);
    PaginaRespuestaRest<Caja> resultadosBusquedaCaja =
        restTemplate
            .exchange(
                apiPrefix + "/cajas/busqueda/criteria",
                HttpMethod.POST,
                requestEntityParaProveedores,
                new ParameterizedTypeReference<PaginaRespuestaRest<Caja>>() {})
            .getBody();
    assertNotNull(resultadosBusquedaCaja);
    List<Caja> cajasRecuperadas = resultadosBusquedaCaja.getContent();
    assertEquals(1, cajasRecuperadas.size());
    assertEquals(EstadoCaja.ABIERTA, cajasRecuperadas.get(0).getEstado());
    restTemplate.put(
        apiPrefix + "/cajas/" + cajasRecuperadas.get(0).getIdCaja() + "/cierre?monto=5276.66",
        null);
    requestEntityParaProveedores = new HttpEntity<>(criteriaParaBusquedaCaja);
    resultadosBusquedaCaja =
        restTemplate
            .exchange(
                apiPrefix + "/cajas/busqueda/criteria",
                HttpMethod.POST,
                requestEntityParaProveedores,
                new ParameterizedTypeReference<PaginaRespuestaRest<Caja>>() {})
            .getBody();
    assertNotNull(resultadosBusquedaCaja);
    cajasRecuperadas = resultadosBusquedaCaja.getContent();
    assertEquals(1, cajasRecuperadas.size());
    assertEquals(EstadoCaja.CERRADA, cajasRecuperadas.get(0).getEstado());
  }
}
