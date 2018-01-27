package sic.modelo.dto;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import sic.modelo.Pago;
import sic.modelo.RenglonFactura;
import sic.modelo.TipoDeComprobante;

@Data
@EqualsAndHashCode(of = {"fecha", "tipoComprobante", "numSerie", "numFactura", "nombreEmpresa"})
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id_Factura", scope = FacturaDTO.class)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
   @JsonSubTypes.Type(value = FacturaCompraDTO.class, name = "FacturaCompra"),
   @JsonSubTypes.Type(value = FacturaVentaDTO.class, name = "FacturaVenta"),    
})
public abstract class FacturaDTO implements Serializable {
    
    private long id_Factura = 0L;
    private Date fecha;
    private TipoDeComprobante tipoComprobante = TipoDeComprobante.FACTURA_A;
    private long numSerie = 0;
    private long numFactura = 1;
    private Date fechaVencimiento;    
    private String nombreTransportista = "Correo OCA";
    private List<RenglonFactura> renglones;
    private List<Pago> pagos;
    private double subTotal = 6500;
    private double recargo_porcentaje = 0.0;
    private double recargo_neto = 0.0;
    private double descuento_porcentaje = 0.0;
    private double descuento_neto = 0.0;
    private double subTotal_bruto = 6500;
    private double iva_105_neto = 0.0;
    private double iva_21_neto = 1365;
    private double impuestoInterno_neto = 0.0;
    private double total = 7865;
    private String observaciones = "Factura por Default";
    private boolean pagada = false;
    private String nombreEmpresa = "Globo Corporation";
    private boolean eliminada = false;
}
