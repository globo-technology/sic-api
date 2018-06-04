package sic.controller;

import java.util.List;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import sic.modelo.BusquedaUsuarioCriteria;
import sic.modelo.Rol;
import sic.modelo.Usuario;
import sic.service.IEmpresaService;
import sic.service.IUsuarioService;

@RestController
@RequestMapping("/api/v1")
public class UsuarioController {
    
    private final IUsuarioService usuarioService;
    private final IEmpresaService empresaService;
    private final int TAMANIO_PAGINA_DEFAULT = 50;

    @Value("${SIC_JWT_KEY}")
    private String secretkey;
    
    @Autowired
    public UsuarioController(IUsuarioService usuarioService, IEmpresaService empresaService) {
        this.usuarioService = usuarioService;
        this.empresaService =  empresaService;
    }
    
    @GetMapping("/usuarios/{idUsuario}")
    @ResponseStatus(HttpStatus.OK)
    public Usuario getUsuarioPorId(@PathVariable long idUsuario) {
        return usuarioService.getUsuarioPorId(idUsuario);
    }

    @GetMapping("/usuarios/busqueda/criteria")
    @ResponseStatus(HttpStatus.OK)
    public Page<Usuario> buscarUsuarios(@RequestParam Long idEmpresa,
                                        @RequestParam(required = false) String username,
                                        @RequestParam(required = false) String nombre,
                                        @RequestParam(required = false) String apellido,
                                        @RequestParam(required = false) String email,
                                        @RequestParam(required = false) Integer pagina,
                                        @RequestParam(required = false) Integer tamanio,
                                        @RequestParam(required = false) List<Rol> roles,
                                        @RequestHeader("Authorization") String token) {
        if (tamanio == null || tamanio <= 0) tamanio = TAMANIO_PAGINA_DEFAULT;
        if (pagina == null || pagina < 0) pagina = 0;
        Pageable pageable = new PageRequest(pagina, tamanio, new Sort(Sort.Direction.ASC, "username"));
        BusquedaUsuarioCriteria criteria = BusquedaUsuarioCriteria.builder()
                .buscarPorNombreDeUsuario(username != null)
                .username(username)
                .buscaPorNombre(nombre != null)
                .nombre(nombre)
                .buscaPorApellido(apellido!= null)
                .apellido(apellido)
                .buscaPorEmail(email != null)
                .email(email)
                .buscarPorRol(roles != null && !roles.isEmpty())
                .roles(roles)
                .empresa(empresaService.getEmpresaPorId(idEmpresa))
                .pageable(pageable)
                .build();
        Claims claims = Jwts.parser().setSigningKey(secretkey).parseClaimsJws(token.substring(7)).getBody();
        return usuarioService.buscarUsuarios(criteria, (int) claims.get("idUsuario"));
    }
    
    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public Usuario guardar(@RequestBody Usuario usuario,
                           @RequestParam(required = false) Long idCliente) {
        return usuarioService.guardar(usuario, idCliente);
    }
    
    @PutMapping("/usuarios")
    @ResponseStatus(HttpStatus.OK)
    public void actualizar(@RequestBody Usuario usuario,
                           @RequestParam(required = false) Long idCliente) {
       usuarioService.actualizar(usuario, idCliente);
    }
    
    @PutMapping("/usuarios/{idUsuario}/empresas/{idEmpresaPredeterminada}")
    @ResponseStatus(HttpStatus.OK)
    public void actualizarIdEmpresaDeUsuario(@PathVariable long idUsuario, @PathVariable long idEmpresaPredeterminada) {
       usuarioService.actualizarIdEmpresaDeUsuario(idUsuario, idEmpresaPredeterminada);
    }
    
    @DeleteMapping("/usuarios/{idUsuario}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable long idUsuario) {
        usuarioService.eliminar(idUsuario);
    }    
}