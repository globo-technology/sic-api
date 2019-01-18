package sic.modelo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReciboDTO implements Serializable {

  private Long idRecibo;
  private long serie;
  private long nroRecibo;
  private Date fecha;
  private boolean eliminado;
  private String concepto;
  private String nombreFormaDePago;
  private String nombreEmpresa;
  private Long idCliente;
  private String nombreFiscalCliente;
  private Long idProveedor;
  private String razonSocialProveedor;
  private String nombreUsuario;
  private double monto;
}
