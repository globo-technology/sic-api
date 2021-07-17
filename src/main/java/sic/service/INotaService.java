package sic.service;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import sic.modelo.*;
import sic.modelo.criteria.BusquedaNotaCriteria;
import sic.modelo.dto.NuevaNotaCreditoDeFacturaDTO;
import sic.modelo.dto.NuevaNotaCreditoSinFacturaDTO;
import sic.modelo.dto.NuevaNotaDebitoDeReciboDTO;
import sic.modelo.dto.NuevaNotaDebitoSinReciboDTO;

public interface INotaService {

  NotaCredito guardarNotaCredito(NotaCredito nota);

  NotaDebito guardarNotaDebito(NotaDebito nota);

  NotaCredito calcularNotaCreditoConFactura(
      NuevaNotaCreditoDeFacturaDTO nuevaNotaCreditoDeFacturaDTO, Usuario usuario);

  NotaCredito calcularNotaCreditoSinFactura(
      NuevaNotaCreditoSinFacturaDTO nuevaNotaCreditoSinFacturaDTO, Usuario usuario);

  NotaDebito calcularNotaDebitoConRecibo(
      NuevaNotaDebitoDeReciboDTO nuevaNotaDebitoDeReciboDTO, Usuario usuario);

  NotaDebito calcularNotaDebitoSinRecibo(
      NuevaNotaDebitoSinReciboDTO nuevaNotaDebitoSinReciboDTO, Usuario usuario);

  boolean existsByFacturaVentaAndEliminada(FacturaVenta facturaVenta);

  Nota autorizarNota(Nota nota);

  byte[] getReporteNota(Nota nota);

  Nota getNotaNoEliminadaPorId(long idNota);

  void eliminarNota(long idNota);

  Page<NotaCredito> buscarNotasCredito(
      BusquedaNotaCriteria busquedaNotaCriteria, long idUsuarioLoggedIn);

  Page<NotaDebito> buscarNotasDebito(
      BusquedaNotaCriteria busquedaNotaCriteria, long idUsuarioLoggedIn);

  Factura getFacturaDeLaNotaCredito(Long idNota);

  boolean existsNotaDebitoPorRecibo(Recibo recibo);

  List<NotaCredito> getNotasCreditoPorFactura(Long idFactura);

  long getSiguienteNumeroNotaDebitoCliente(Long idSucursal, TipoDeComprobante tipoComprobante);

  long getSiguienteNumeroNotaCreditoCliente(Long idSucursal, TipoDeComprobante tipoComprobante);

  List<TipoDeComprobante> getTipoNotaCreditoCliente(Long idCliente, Long idSucursal);

  List<TipoDeComprobante> getTipoNotaDebitoCliente(Long idCliente, Long idSucursal);

  List<TipoDeComprobante> getTipoNotaCreditoProveedor(Long idProveedor, Long idSucursal);

  List<TipoDeComprobante> getTipoNotaDebitoProveedor(Long idProveedor, Long idSucursal);

  TipoDeComprobante[] getTiposNotaCredito(Sucursal sucursal);

  TipoDeComprobante[] getTiposNotaDebito(Sucursal sucursal);

  TipoDeComprobante getTipoDeNotaCreditoSegunFactura(TipoDeComprobante tipo);

  List<RenglonNotaCredito> getRenglonesDeNotaCredito(Long idNota);

  List<RenglonNotaDebito> getRenglonesDeNotaDebito(long idNota);

  List<RenglonFactura> getRenglonesFacturaModificadosParaNotaCredito(long idFactura);

  BigDecimal calcularTotalNota(List<RenglonNotaCredito> renglonesNota);

  List<RenglonNotaCredito> calcularRenglonesCreditoProducto(
      TipoDeComprobante tipo, BigDecimal[] cantidad, Long[] idRenglonFactura);

  RenglonNotaCredito calcularRenglonCredito(
      TipoDeComprobante tipo, String detalle, BigDecimal monto);

  RenglonNotaDebito calcularRenglonDebitoConRecibo(Recibo recibo);

  RenglonNotaDebito calcularRenglonDebito(BigDecimal monto, TipoDeComprobante tipoDeComprobante);

  BigDecimal calcularSubTotalCredito(BigDecimal[] importesBrutos);

  BigDecimal calcularDecuentoNetoCredito(BigDecimal subTotal, BigDecimal descuentoPorcentaje);

  BigDecimal calcularRecargoNetoCredito(BigDecimal subTotal, BigDecimal recargoPorcentaje);

  BigDecimal calcularSubTotalBrutoCredito(
      TipoDeComprobante tipoDeComprobante,
      BigDecimal subTotal,
      BigDecimal recargoNeto,
      BigDecimal descuentoNeto,
      BigDecimal iva105Neto,
      BigDecimal iva21Neto);

  BigDecimal calcularIVANetoCredito(
      TipoDeComprobante tipoDeComprobante,
      BigDecimal[] cantidades,
      BigDecimal[] ivaPorcentajeRenglones,
      BigDecimal[] ivaNetoRenglones,
      BigDecimal ivaPorcentaje,
      BigDecimal descuentoPorcentaje,
      BigDecimal recargoPorcentaje);

  void validarReglasDeNegocio(Nota nota);

  void validarCalculosDebito(NotaDebito notaDebito);

  BigDecimal calcularTotalCredito(
      BigDecimal subTotalBruto, BigDecimal iva105Neto, BigDecimal iva21Neto);

  BigDecimal calcularTotalDebito(
      BigDecimal subTotalBruto, BigDecimal iva21Neto, BigDecimal montoNoGravado);

  BigDecimal calcularTotalCredito(BusquedaNotaCriteria criteria, long idUsuarioLoggedIn);

  BigDecimal calcularTotalDebito(BusquedaNotaCriteria criteria, long idUsuarioLoggedIn);

  BigDecimal calcularTotalIVACredito(BusquedaNotaCriteria criteria, long idUsuarioLoggedIn);

  BigDecimal calcularTotalIVADebito(BusquedaNotaCriteria criteria, long idUsuarioLoggedIn);

  boolean existeNotaCreditoAnteriorSinAutorizar(ComprobanteAFIP comprobante);

  boolean existeNotaDebitoAnteriorSinAutorizar(ComprobanteAFIP comprobante);
}
