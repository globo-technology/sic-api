package sic.repository;

import java.math.BigDecimal;

import com.querydsl.core.BooleanBuilder;
import sic.modelo.TipoDeComprobante;

public interface FacturaCompraRepositoryCustom {

  BigDecimal calcularTotalFacturadoCompra(BooleanBuilder builder);

  BigDecimal calcularIVACompra(BooleanBuilder builder, TipoDeComprobante[] tipoComprobante);
}
