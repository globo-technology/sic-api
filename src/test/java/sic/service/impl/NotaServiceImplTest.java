package sic.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import org.springframework.test.context.junit.jupiter.SpringExtension;
import sic.builder.EmpresaBuilder;
import sic.builder.RenglonNotaCreditoBuilder;
import sic.modelo.*;

@ExtendWith(SpringExtension.class)
class NotaServiceImplTest {

  @Mock private EmpresaServiceImpl empresaServiceImpl;

  @Mock private ClienteServiceImpl clienteService;

  @InjectMocks private NotaServiceImpl notaServiceImpl;

  @Test
  void shouldGetTipoNotaWhenEmpresaYClienteDiscriminanIVA() {
    Empresa empresa = new EmpresaBuilder().build();
    Cliente cliente = new Cliente();
    cliente.setCategoriaIVA(CategoriaIVA.RESPONSABLE_INSCRIPTO);
    when(empresaServiceImpl.getEmpresaPorId(1L)).thenReturn(empresa);
    when(clienteService.getClienteNoEliminadoPorId(1L)).thenReturn(cliente);
    TipoDeComprobante[] expResult = {
      TipoDeComprobante.NOTA_CREDITO_A,
      TipoDeComprobante.NOTA_CREDITO_X,
      TipoDeComprobante.NOTA_CREDITO_PRESUPUESTO,
      TipoDeComprobante.NOTA_DEBITO_A,
      TipoDeComprobante.NOTA_DEBITO_X,
      TipoDeComprobante.NOTA_DEBITO_PRESUPUESTO
    };
    TipoDeComprobante[] result = notaServiceImpl.getTipoNotaCliente(1L, 1L);
    assertArrayEquals(expResult, result);
  }

  @Test
  void shouldGetTipoNotaWhenEmpresaDiscriminaYClienteNoIVA() {
    Empresa empresa = new EmpresaBuilder().build();
    Cliente cliente = new Cliente();
    cliente.setCategoriaIVA(CategoriaIVA.CONSUMIDOR_FINAL);
    when(empresaServiceImpl.getEmpresaPorId(1L)).thenReturn(empresa);
    when(clienteService.getClienteNoEliminadoPorId(1L)).thenReturn(cliente);
    TipoDeComprobante[] expResult = {
      TipoDeComprobante.NOTA_CREDITO_B,
      TipoDeComprobante.NOTA_CREDITO_X,
      TipoDeComprobante.NOTA_CREDITO_PRESUPUESTO,
      TipoDeComprobante.NOTA_DEBITO_B,
      TipoDeComprobante.NOTA_DEBITO_X,
      TipoDeComprobante.NOTA_DEBITO_PRESUPUESTO
    };
    TipoDeComprobante[] result = notaServiceImpl.getTipoNotaCliente(1L, 1L);
    assertArrayEquals(expResult, result);
  }

  @Test
  void shouldGetTipoNotaWhenEmpresaNoDiscriminaYClienteSiIVA() {
    Empresa empresa = new EmpresaBuilder().build();
    Cliente cliente = new Cliente();
    cliente.setCategoriaIVA(CategoriaIVA.RESPONSABLE_INSCRIPTO);
    empresa.setCategoriaIVA(CategoriaIVA.MONOTRIBUTO);
    when(empresaServiceImpl.getEmpresaPorId(1L)).thenReturn(empresa);
    when(clienteService.getClienteNoEliminadoPorId(1L)).thenReturn(cliente);
    TipoDeComprobante[] expResult = {
      TipoDeComprobante.NOTA_CREDITO_X,
      TipoDeComprobante.NOTA_CREDITO_PRESUPUESTO,
      TipoDeComprobante.NOTA_DEBITO_X,
      TipoDeComprobante.NOTA_DEBITO_PRESUPUESTO
    };
    TipoDeComprobante[] result = notaServiceImpl.getTipoNotaCliente(1L, 1L);
    assertArrayEquals(expResult, result);
  }

  @Test
  void shouldGetTipoNotaWhenEmpresaNoDiscriminaYClienteNoIVA() {
    Empresa empresa = new EmpresaBuilder().withId_Empresa(1L).build();
    Cliente cliente = new Cliente();
    cliente.setCategoriaIVA(CategoriaIVA.MONOTRIBUTO);
    empresa.setCategoriaIVA(CategoriaIVA.MONOTRIBUTO);
    when(empresaServiceImpl.getEmpresaPorId(1L)).thenReturn(empresa);
    when(clienteService.getClienteNoEliminadoPorId(1L)).thenReturn(cliente);
    TipoDeComprobante[] expResult = {
      TipoDeComprobante.NOTA_CREDITO_X,
      TipoDeComprobante.NOTA_CREDITO_PRESUPUESTO,
      TipoDeComprobante.NOTA_DEBITO_X,
      TipoDeComprobante.NOTA_DEBITO_PRESUPUESTO
    };
    TipoDeComprobante[] result = notaServiceImpl.getTipoNotaCliente(1L, 1L);
    assertArrayEquals(expResult, result);
  }

  @Test
  void shouldCalcularTotalNotaCredito() {
    RenglonNotaCredito renglonNotaCredito = new RenglonNotaCreditoBuilder().build();
    List<RenglonNotaCredito> renglones = new ArrayList<>();
    renglones.add(renglonNotaCredito);
    assertEquals((new BigDecimal("172.062")).compareTo(notaServiceImpl.calcularTotalNota(renglones)), 0);
  }
}
