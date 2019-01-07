package sic.service;

import io.jsonwebtoken.Claims;
import sic.modelo.Rol;

import java.util.List;

public interface IAuthService {

  String generarToken(long idUsuario, List<Rol> rolesDeUsuario);

  void validarToken(String authorizationHeader);

  Claims getClaimsDelToken(String authorizationHeader);
}