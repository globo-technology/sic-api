package sic.modelo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sic.modelo.CategoriaIVA;
import sic.modelo.TipoDeCliente;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClienteDTO implements Serializable {

  private long id_Cliente;
  private TipoDeCliente tipoDeCliente;
  private BigDecimal bonificacion;
  private String razonSocial;
  private String nombreFantasia;
  private String direccion;
  private CategoriaIVA categoriaIVA;
  private String idFiscal;
  private String email;
  private String telefono;
  private Long idLocalidad;
  private String nombreLocalidad;
  private Long idProvincia;
  private String nombreProvincia;
  private Long idPais;
  private String nombrePais;
  private String contacto;
  private Date fechaAlta;
  private Long idEmpresa;
  private String nombreEmpresa;
  private Long idViajante;
  private String nombreViajante;
  private Long idCredencial;
  private String nombreCredencial;
  private boolean predeterminado;
  private BigDecimal saldoCuentaCorriente;
  private Date fechaUltimoMovimiento;

}
