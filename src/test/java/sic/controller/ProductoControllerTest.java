package sic.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.MacProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import sic.modelo.*;
import sic.modelo.dto.CantidadEnSucursalDTO;
import sic.modelo.dto.ProductoDTO;
import sic.service.impl.*;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ProductoController.class})
class ProductoControllerTest {

  @MockBean ProductoServiceImpl productoService;
  @MockBean MedidaServiceImpl medidaService;
  @MockBean RubroServiceImpl rubroService;
  @MockBean ProveedorServiceImpl proveedorService;
  @MockBean SucursalServiceImpl sucursalService;
  @MockBean AuthServiceImpl authService;
  @MockBean ModelMapper modelMapper;

  @Autowired ProductoController productoController;

  @Test
  void shouldActualizarProducto() {
    Sucursal sucursal = new Sucursal();
    sucursal.setIdSucursal(1L);
    Producto productoPersistido = new Producto();
    Set<CantidadEnSucursal> cantidadEnSucursales = new HashSet<>();
    CantidadEnSucursal cantidadEnSucursal = new CantidadEnSucursal();
    cantidadEnSucursal.setCantidad(BigDecimal.TEN);
    cantidadEnSucursal.setSucursal(sucursal);
    cantidadEnSucursales.add(cantidadEnSucursal);
    cantidadEnSucursal.setSucursal(sucursal);
    productoPersistido.setCantidadEnSucursales(cantidadEnSucursales);
    productoPersistido.setCantidadTotalEnSucursales(BigDecimal.TEN);
    ProductoDTO productoDTO = new ProductoDTO();
    productoDTO.setIdProducto(1L);
    Set<CantidadEnSucursalDTO> cantidadesEnSucursales = new HashSet<>();
    CantidadEnSucursalDTO cantidadEnSucursalDTO =
        CantidadEnSucursalDTO.builder().idSucursal(1L).cantidad(BigDecimal.TEN).build();
    cantidadesEnSucursales.add(cantidadEnSucursalDTO);
    productoDTO.setCantidadEnSucursales(cantidadesEnSucursales);
    when(productoService.getProductoNoEliminadoPorId(1L)).thenReturn(productoPersistido);
    when(medidaService.getMedidaNoEliminadaPorId(1L)).thenReturn(new Medida());
    when(rubroService.getRubroNoEliminadoPorId(1L)).thenReturn(new Rubro());
    when(proveedorService.getProveedorNoEliminadoPorId(1L)).thenReturn(new Proveedor());
    Producto productoPorActualizar = new Producto();
    productoPorActualizar.setIdProducto(1L);
    when(modelMapper.map(productoDTO, Producto.class)).thenReturn(productoPorActualizar);
    when(modelMapper.map(cantidadEnSucursalDTO, CantidadEnSucursal.class))
        .thenReturn(cantidadEnSucursal);
    productoController.actualizar(productoDTO, 1L, 1L, 1L);
    verify(productoService).actualizar(any(), any(), any());
  }

  @Test
  void shouldMarcarComoFavorito() {
    LocalDateTime today = LocalDateTime.now();
    ZonedDateTime zdtNow = today.atZone(ZoneId.systemDefault());
    ZonedDateTime zdtInOneMonth = today.plusMonths(1L).atZone(ZoneId.systemDefault());
    SecretKey secretKey = MacProvider.generateKey();
    Claims claims =
        Jwts.parser()
            .setSigningKey(secretKey)
            .parseClaimsJws(
                Jwts.builder()
                    .setIssuedAt(Date.from(zdtNow.toInstant()))
                    .setExpiration(Date.from(zdtInOneMonth.toInstant()))
                    .signWith(SignatureAlgorithm.HS512, secretKey)
                    .claim("idUsuario", 1L)
                    .claim("roles", Collections.singletonList(Rol.ADMINISTRADOR))
                    .claim("app", Aplicacion.SIC_COM)
                    .compact())
            .getBody();
    when(authService.getClaimsDelToken("headers")).thenReturn(claims);
    productoController.marcarComoFavorito(1L, "headers");
    verify(productoService).guardarProductoFavorito(1L, 1L);
  }

  @Test
  void shouldGetProductoFavoritosDelCliente() {
    LocalDateTime today = LocalDateTime.now();
    ZonedDateTime zdtNow = today.atZone(ZoneId.systemDefault());
    ZonedDateTime zdtInOneMonth = today.plusMonths(1L).atZone(ZoneId.systemDefault());
    SecretKey secretKey = MacProvider.generateKey();
    Claims claims =
            Jwts.parser()
                    .setSigningKey(secretKey)
                    .parseClaimsJws(
                            Jwts.builder()
                                    .setIssuedAt(Date.from(zdtNow.toInstant()))
                                    .setExpiration(Date.from(zdtInOneMonth.toInstant()))
                                    .signWith(SignatureAlgorithm.HS512, secretKey)
                                    .claim("idUsuario", 1L)
                                    .claim("roles", Collections.singletonList(Rol.ADMINISTRADOR))
                                    .claim("app", Aplicacion.SIC_COM)
                                    .compact())
                    .getBody();
    when(authService.getClaimsDelToken("headers")).thenReturn(claims);
    productoController.getProductosFavoritosDelCliente(0, "headers");
    verify(productoService).getPaginaProductosFavoritosDelCliente(1L, 0);
  }

  @Test
  void shouldQuitarProductoDeFavoritos() {
    LocalDateTime today = LocalDateTime.now();
    ZonedDateTime zdtNow = today.atZone(ZoneId.systemDefault());
    ZonedDateTime zdtInOneMonth = today.plusMonths(1L).atZone(ZoneId.systemDefault());
    SecretKey secretKey = MacProvider.generateKey();
    Claims claims =
            Jwts.parser()
                    .setSigningKey(secretKey)
                    .parseClaimsJws(
                            Jwts.builder()
                                    .setIssuedAt(Date.from(zdtNow.toInstant()))
                                    .setExpiration(Date.from(zdtInOneMonth.toInstant()))
                                    .signWith(SignatureAlgorithm.HS512, secretKey)
                                    .claim("idUsuario", 1L)
                                    .claim("roles", Collections.singletonList(Rol.ADMINISTRADOR))
                                    .claim("app", Aplicacion.SIC_COM)
                                    .compact())
                    .getBody();
    when(authService.getClaimsDelToken("headers")).thenReturn(claims);
    productoController.quitarProductoDeFavoritos(4L, "headers");
    verify(productoService).quitarProductoDeFavoritos(1L, 4L);
  }

  @Test
  void shouldQuitarProductosDeFavoritos() {
    LocalDateTime today = LocalDateTime.now();
    ZonedDateTime zdtNow = today.atZone(ZoneId.systemDefault());
    ZonedDateTime zdtInOneMonth = today.plusMonths(1L).atZone(ZoneId.systemDefault());
    SecretKey secretKey = MacProvider.generateKey();
    Claims claims =
            Jwts.parser()
                    .setSigningKey(secretKey)
                    .parseClaimsJws(
                            Jwts.builder()
                                    .setIssuedAt(Date.from(zdtNow.toInstant()))
                                    .setExpiration(Date.from(zdtInOneMonth.toInstant()))
                                    .signWith(SignatureAlgorithm.HS512, secretKey)
                                    .claim("idUsuario", 1L)
                                    .claim("roles", Collections.singletonList(Rol.ADMINISTRADOR))
                                    .claim("app", Aplicacion.SIC_COM)
                                    .compact())
                    .getBody();
    when(authService.getClaimsDelToken("headers")).thenReturn(claims);
    productoController.quitarProductosDeFavoritos("headers");
    verify(productoService).quitarProductosDeFavoritos(1L);
  }
}
