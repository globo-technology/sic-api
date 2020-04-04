package sic.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import sic.modelo.*;
import sic.modelo.dto.NuevoRenglonPedidoDTO;
import sic.modelo.dto.NuevosResultadosComprobanteDTO;
import sic.service.impl.PedidoServiceImpl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class PedidoControllerTest {

  @Mock PedidoServiceImpl pedidoService;

  @InjectMocks PedidoController pedidoController;

  @Test
  void shouldGetPedidoPorId() {
    Pedido pedido = new Pedido();
    pedido.setNroPedido(1L);
    when(pedidoService.getPedidoNoEliminadoPorId(2L)).thenReturn(pedido);
    when(pedidoService.calcularTotalActualDePedido(pedido)).thenReturn(pedido);
    assertEquals(pedido, pedidoController.getPedidoPorId(2L));
  }

  @Test
  void shouldCalcularRenglonesPedido() {
    List<NuevoRenglonPedidoDTO> nuevoRenglonPedido = new ArrayList<>();
    nuevoRenglonPedido.add(
        NuevoRenglonPedidoDTO.builder().idProductoItem(1L).cantidad(BigDecimal.TEN).build());
    nuevoRenglonPedido.add(
        NuevoRenglonPedidoDTO.builder().idProductoItem(2L).cantidad(BigDecimal.ONE).build());
    List<RenglonPedido> renglones = new ArrayList<>();
    long[] idsProducto = {1L, 2L};
    BigDecimal[] cantidades = {BigDecimal.TEN, BigDecimal.ONE};
    when(pedidoService.calcularRenglonesPedido(idsProducto, cantidades)).thenReturn(renglones);
    assertEquals(renglones, pedidoController.calcularRenglonesPedido(nuevoRenglonPedido));
  }

  @Test
  void shouldCalcularResultadosPedido() {
    NuevosResultadosComprobanteDTO nuevosResultadosComprobanteDTO =
        NuevosResultadosComprobanteDTO.builder().build();
    Resultados resultados = new Resultados();
    when(pedidoService.calcularResultadosPedido(nuevosResultadosComprobanteDTO))
        .thenReturn(resultados);
    assertEquals(
        resultados, pedidoController.calcularResultadosPedido(nuevosResultadosComprobanteDTO));
  }
}
