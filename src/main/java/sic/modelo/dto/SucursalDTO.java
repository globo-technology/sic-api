package sic.modelo.dto;

import lombok.*;
import sic.modelo.CategoriaIVA;

import java.util.Date;

@Data
@EqualsAndHashCode(exclude = {"idSucursal" , "ubicacion"})
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SucursalDTO {

  private long idSucursal;
  private String nombre;
  private String lema;
  private CategoriaIVA categoriaIVA;
  private Long idFiscal;
  private long ingresosBrutos;
  private Date fechaInicioActividad;
  private String email;
  private String telefono;
  private UbicacionDTO ubicacion;
  private String logo;
  private boolean eliminada;

}