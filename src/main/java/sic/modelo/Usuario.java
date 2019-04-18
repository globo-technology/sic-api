package sic.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;
import org.modelmapper.internal.bytebuddy.implementation.bind.annotation.Empty;

@Entity
@Table(name = "usuario")
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"username", "email"})
@ToString(exclude = {"roles", "password"})
@JsonIgnoreProperties({"token", "passwordRecoveryKey", "passwordRecoveryKeyExpirationDate", "eliminado"})
public class Usuario implements Serializable {

    @Id
    @GeneratedValue
    private long id_Usuario;
    
    @Column(nullable = false)
    @NotEmpty(message = "{mensaje_usuario_vacio_username}")
    private String username;
    
    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    
    @Column(nullable = false)
    @NotEmpty(message = "{mensaje_usuario_vacio_nombre}")
    private String nombre;
    
    @Column(nullable = false)
    @NotEmpty(message = "{mensaje_usuario_vacio_apellido}")
    private String apellido;
    
    @Column(nullable = false)
    @Email(message = "{mensaje_usuario_invalido_email}")
    private String email;
    
    @Column(length = 300)
    private String token;
    
    private long idEmpresaPredeterminada;

    private String passwordRecoveryKey;

    @Temporal(TemporalType.TIMESTAMP)
    private Date passwordRecoveryKeyExpirationDate;
    
    @ElementCollection(targetClass = Rol.class)
    @CollectionTable(name="rol", joinColumns = @JoinColumn(name = "id_Usuario"))
    @Enumerated(EnumType.STRING)
    @Column(name="nombre")
    private List<Rol> roles;

    private boolean habilitado;
    
    private boolean eliminado;
        
}