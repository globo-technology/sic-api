package sic.modelo;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.validation.constraints.*;
import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import sic.controller.Views;

@Entity
@Table(name = "recibo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(of = {"numSerie", "numRecibo", "sucursal", "fecha"})
@JsonIgnoreProperties({"formaDePago", "sucursal", "cliente", "usuario", "proveedor"})
@JsonView(Views.Comprador.class)
public class Recibo implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idRecibo;

  private long numSerie;

  private long numRecibo;

  private String idPagoMercadoPago;

  @NotNull(message = "{mensaje_recibo_fecha_vacia}")
  @PastOrPresent(message = "{mensaje_recibo_fecha_futura_no_permitida}")
  private LocalDateTime fecha;

  private boolean eliminado;

  @Column(nullable = false)
  @NotBlank(message = "{mensaje_recibo_concepto_vacio}")
  private String concepto;

  @ManyToOne
  @JoinColumn(name = "id_FormaDePago", referencedColumnName = "id_FormaDePago")
  @NotNull(message = "{mensaje_recibo_forma_de_pago_vacia}")
  private FormaDePago formaDePago;

  @ManyToOne
  @JoinColumn(name = "idSucursal", referencedColumnName = "idSucursal")
  @NotNull(message = "{mensaje_recibo_sucursal_vacia}")
  private Sucursal sucursal;

  @ManyToOne
  @JoinColumn(name = "id_Cliente", referencedColumnName = "id_Cliente")
  private Cliente cliente;

  @ManyToOne
  @JoinColumn(name = "id_Proveedor", referencedColumnName = "id_Proveedor")
  private Proveedor proveedor;

  @ManyToOne
  @JoinColumn(name = "id_Usuario", referencedColumnName = "id_Usuario")
  @NotNull(message = "{mensaje_recibo_usuario_vacio}")
  private Usuario usuario;

  @Column(precision = 25, scale = 15)
  @Positive(message = "{mensaje_recibo_monto_igual_menor_cero}")
  private BigDecimal monto;

  @JsonGetter("idFormaDePago")
  public long getIdFormaDePago() {
    return formaDePago.getIdFormaDePago();
  }

  @JsonGetter("nombreFormaDePago")
  public String getNombreFormaDePago() {
    return formaDePago.getNombre();
  }

  @JsonGetter("idSucursal")
  public long getIdSucursal() {
    return sucursal.getIdSucursal();
  }

  @JsonGetter("nombreSucursal")
  public String getNombreSucursal() {
    return sucursal.getNombre();
  }

  @JsonGetter("idCliente")
  public Long getIdCliente() {
    return (cliente != null) ? cliente.getIdCliente() : null;
  }

  @JsonGetter("nombreFiscalCliente")
  public String getNombreFiscalCliente() {
    return (cliente != null) ? cliente.getNombreFiscal() : "";
  }

  @JsonGetter("idProveedor")
  public Long getIdProveedor() {
    return (proveedor != null) ? proveedor.getIdProveedor() : null;
  }

  @JsonGetter("razonSocialProveedor")
  public String getRazonSocialProveedor() {
    return (proveedor != null) ? proveedor.getRazonSocial() : "";
  }

  @JsonGetter("nombreUsuario")
  public String getNombreUsuario() {
    return usuario.getNombre() + " " + usuario.getApellido() + " (" + usuario.getUsername() + ")";
  }

  @JsonGetter("idViajante")
  public Long getIdViajante() {
    return (cliente != null && cliente.getViajante() != null) ? cliente.getViajante().getIdUsuario() : null;
  }

  @JsonGetter("nombreViajante")
  public String getNombreViajante() {
    return (cliente != null && cliente.getViajante() != null) ? cliente.getNombreViajante() : null;
  }
}
