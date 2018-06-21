package sic.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import javax.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import sic.modelo.Credencial;
import sic.modelo.Rol;
import sic.modelo.Usuario;
import sic.service.IUsuarioService;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

  private final IUsuarioService usuarioService;

  @Value("${SIC_JWT_KEY}")
  private String secretkey;

  @Autowired
  public AuthController(IUsuarioService usuarioService) {
    this.usuarioService = usuarioService;
  }

  private String generarToken(long idUsuario, List<Rol> rolesDeUsuario) {
    // 24hs desde la fecha actual para expiration
    Date today = new Date();
    Calendar c = Calendar.getInstance();
    c.setTime(today);
    c.add(Calendar.DATE, 1);
    Date tomorrow = c.getTime();
    return Jwts.builder()
        .setIssuedAt(today)
        .setExpiration(tomorrow)
        .signWith(SignatureAlgorithm.HS512, secretkey)
        .claim("idUsuario", idUsuario)
        .claim("roles", rolesDeUsuario)
        .compact();
  }

  @PostMapping("/login")
  public String login(@RequestBody Credencial credencial) {
    Usuario usuario;
    try {
      usuario = usuarioService.autenticarUsuario(credencial);
    } catch (EntityNotFoundException ex) {
      throw new UnauthorizedException(
          ResourceBundle.getBundle("Mensajes").getString("mensaje_usuario_logInInvalido"), ex);
    }
    String token = this.generarToken(usuario.getId_Usuario(), usuario.getRoles());
    usuarioService.actualizarToken(token, usuario.getId_Usuario());
    return token;
  }

  @PutMapping("/logout")
  public void logout(@RequestHeader("Authorization") String token) {
    Claims claims;
    try {
      claims = Jwts.parser().setSigningKey(secretkey).parseClaimsJws(token.substring(7)).getBody();
    } catch (JwtException ex) {
      throw new UnauthorizedException(
          ResourceBundle.getBundle("Mensajes").getString("mensaje_error_token_vacio_invalido"), ex);
    }
    long idUsuario = (int) claims.get("idUsuario");
    usuarioService.actualizarToken("", idUsuario);
  }
}
