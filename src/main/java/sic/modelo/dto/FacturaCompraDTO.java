package sic.modelo.dto;

import java.io.Serializable;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true,  exclude = {"razonSocialProveedor", "idProveedor"})
@Builder
public class FacturaCompraDTO extends FacturaDTO implements Serializable {

    private long idProveedor;
    private String razonSocialProveedor;
    
}
