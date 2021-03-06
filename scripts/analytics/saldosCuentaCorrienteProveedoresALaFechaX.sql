
-- SOLO TIENE EN CUENTA LOS COMPROBANTES BLANCOS
-- SETEAR LA FECHA DESEADA EN EL WHERE

select proveedor.idFiscal, proveedor.razonSocial, sum(rengloncuentacorriente.monto) as 'montoAcumulado'
from cuentacorriente inner join cuentacorrienteproveedor on cuentacorriente.id_cuenta_corriente = cuentacorrienteproveedor.id_cuenta_corriente
	inner join rengloncuentacorriente on cuentacorriente.id_cuenta_corriente = rengloncuentacorriente.id_cuenta_corriente
	inner join proveedor on cuentacorrienteproveedor.id_Proveedor = proveedor.id_Proveedor
where rengloncuentacorriente.eliminado = false
	and rengloncuentacorriente.fecha <= '2019-12-31 23:59:59'
    and (rengloncuentacorriente.tipo_comprobante = 'FACTURA_A'
		or rengloncuentacorriente.tipo_comprobante = 'FACTURA_B'
		or rengloncuentacorriente.tipo_comprobante = 'FACTURA_C'
        or rengloncuentacorriente.tipo_comprobante = 'NOTA_CREDITO_A'
        or rengloncuentacorriente.tipo_comprobante = 'NOTA_CREDITO_B'
        or rengloncuentacorriente.tipo_comprobante = 'NOTA_DEBITO_A'
        or rengloncuentacorriente.tipo_comprobante = 'NOTA_DEBITO_B'
        or rengloncuentacorriente.tipo_comprobante = 'RECIBO')
group by cuentacorriente.id_cuenta_corriente
order by montoAcumulado asc
