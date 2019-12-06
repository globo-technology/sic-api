package sic.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import sic.modelo.*;
import sic.modelo.criteria.BusquedaPedidoCriteria;
import sic.modelo.calculos.NuevosResultadosPedidoDTO;
import sic.modelo.calculos.Resultados;
import sic.modelo.dto.DetallePedidoDTO;
import sic.modelo.dto.NuevoRenglonPedidoDTO;

import javax.validation.Valid;

public interface IPedidoService {

  Pedido getPedidoNoEliminadoPorId(long idPedido);

  void actualizar(DetallePedidoDTO detallePedidoDTO, long idUsuario);

  void actualizarFacturasDelPedido(@Valid Pedido pedido, List<Factura> facturas);

  Page<Pedido> buscarPedidos(BusquedaPedidoCriteria criteria, long idUsuarioLoggedIn);

  long generarNumeroPedido(Sucursal sucursal);

  Pedido actualizarEstadoPedido(Pedido pedido);

  Pedido calcularTotalActualDePedido(Pedido pedido);

  boolean eliminar(long idPedido);

  List<Factura> getFacturasDelPedido(long id);

  Map<Long, RenglonFactura> getRenglonesFacturadosDelPedido(long nroPedido);

  List<RenglonPedido> getRenglonesDelPedidoOrdenadorPorIdRenglon(Long idPedido);

  List<RenglonPedido> getRenglonesDelPedidoOrdenadoPorIdProducto(Long idPedido);

  byte[] getReportePedido(long idPedido);

  Pedido guardar(DetallePedidoDTO detallePedidoDTO, long idUsuario);

  RenglonPedido calcularRenglonPedido(long idProducto, BigDecimal cantidad, Cliente cliente);

  List<RenglonPedido> calcularRenglonesPedido(@Valid List<NuevoRenglonPedidoDTO> nuevosRenglonesPedidoDTO, Long idCliente);

  Resultados calcularResultadosPedido(NuevosResultadosPedidoDTO calculoPedido);
}
