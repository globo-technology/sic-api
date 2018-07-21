package sic.service;

import org.springframework.data.domain.Page;
import sic.modelo.BusquedaUsuarioCriteria;
import sic.modelo.Credencial;
import sic.modelo.Rol;
import sic.modelo.Usuario;

public interface IUsuarioService {

  Usuario getUsuarioPorId(Long idUsuario);

  void actualizar(Usuario usuario, Usuario usuarioLoggedIn);

  void actualizarToken(String token, long idUsuario);

  void eliminar(long idUsuario);

  Usuario autenticarUsuario(Credencial credencial);

  Page<Usuario> buscarUsuarios(BusquedaUsuarioCriteria criteria, long idUsuarioLoggedIn);

  Usuario guardar(Usuario usuario);

  int actualizarIdEmpresaDeUsuario(long idUsuario, long idEmpresaPredeterminada);

  Page<Usuario> getUsuariosPorRol(Rol rol);
}
