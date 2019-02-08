package sic.modelo.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UbicacionDTO {

  private long idUbicacion;

  private String descripcion;

  private Double latitud;

  private Double longitud;

  private String calle;

  private Integer numero;

  private Integer piso;

  private String departamento;

  private boolean eliminada;

  private Long idLocalidad;

  private String nombreLocalidad;

  private String codigoPostal;

  private Long idProvincia;

  private String nombreProvincia;
}
