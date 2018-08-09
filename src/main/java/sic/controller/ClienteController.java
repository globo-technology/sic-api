package sic.controller;

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
import sic.aspect.AccesoRolesPermitidos;
import sic.modelo.*;
import sic.service.*;

import java.util.ResourceBundle;

@RestController
@RequestMapping("/api/v1")
public class ClienteController {

  private final IClienteService clienteService;
  private final IEmpresaService empresaService;
  private final ILocalidadService localidadService;
  private final IUsuarioService usuarioService;
  private final ICondicionIVAService condicionIVAService;

  @Value("${SIC_JWT_KEY}")
  private String secretkey;

  @Autowired
  public ClienteController(
      IClienteService clienteService,
      IEmpresaService empresaService,
      ILocalidadService localidadService,
      IUsuarioService usuarioService,
      ICondicionIVAService condicionIVAService) {
    this.clienteService = clienteService;
    this.empresaService = empresaService;
    this.localidadService = localidadService;
    this.usuarioService = usuarioService;
    this.condicionIVAService = condicionIVAService;
  }

  @GetMapping("/clientes/{idCliente}")
  @ResponseStatus(HttpStatus.OK)
  @AccesoRolesPermitidos({
    Rol.ADMINISTRADOR,
    Rol.ENCARGADO,
    Rol.VENDEDOR,
    Rol.VIAJANTE,
    Rol.COMPRADOR
  })
  public Cliente getCliente(@PathVariable long idCliente) {
    return clienteService.getClientePorId(idCliente);
  }

  @GetMapping("/clientes/busqueda/criteria")
  @ResponseStatus(HttpStatus.OK)
  public Page<Cliente> buscarConCriteria(
      @RequestParam Long idEmpresa,
      @RequestParam(required = false) String nroCliente,
      @RequestParam(required = false) String razonSocial,
      @RequestParam(required = false) String nombreFantasia,
      @RequestParam(required = false) String idFiscal,
      @RequestParam(required = false) Long idViajante,
      @RequestParam(required = false) Long idPais,
      @RequestParam(required = false) Long idProvincia,
      @RequestParam(required = false) Long idLocalidad,
      @RequestParam(required = false) Integer pagina,
      @RequestParam(required = false) Integer tamanio,
      @RequestParam(required = false, defaultValue = "true") boolean conSaldo,
      @RequestHeader("Authorization") String token) {
    final int TAMANIO_PAGINA_DEFAULT = 50;
    if (tamanio == null || tamanio <= 0) tamanio = TAMANIO_PAGINA_DEFAULT;
    if (pagina == null || pagina < 0) pagina = 0;
    Pageable pageable =
        new PageRequest(pagina, tamanio, new Sort(Sort.Direction.ASC, "razonSocial"));
    BusquedaClienteCriteria criteria =
        BusquedaClienteCriteria.builder()
            .buscaPorRazonSocial(razonSocial != null)
            .razonSocial(razonSocial)
            .buscaPorNombreFantasia(nombreFantasia != null)
            .nombreFantasia(nombreFantasia)
            .buscaPorId_Fiscal(idFiscal != null)
            .idFiscal(idFiscal)
            .buscaPorViajante(idViajante != null)
            .idViajante(idViajante)
            .buscaPorPais(idPais != null)
            .idPais(idPais)
            .buscaPorProvincia(idProvincia != null)
            .idProvincia(idProvincia)
            .buscaPorLocalidad(idLocalidad != null)
            .idLocalidad(idLocalidad)
            .buscarPorNroDeCliente(nroCliente != null)
            .nroDeCliente(nroCliente)
            .idEmpresa(idEmpresa)
            .pageable(pageable)
            .conSaldo(conSaldo)
            .build();
    Claims claims =
        Jwts.parser().setSigningKey(secretkey).parseClaimsJws(token.substring(7)).getBody();
    return clienteService.buscarClientes(criteria, (int) claims.get("idUsuario"));
  }

  @GetMapping("/clientes/predeterminado/empresas/{idEmpresa}")
  @ResponseStatus(HttpStatus.OK)
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR})
  public Cliente getClientePredeterminado(@PathVariable long idEmpresa) {
    return clienteService.getClientePredeterminado(empresaService.getEmpresaPorId(idEmpresa));
  }

  @GetMapping("/clientes/existe-predeterminado/empresas/{idEmpresa}")
  @ResponseStatus(HttpStatus.OK)
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR})
  public boolean existeClientePredeterminado(@PathVariable long idEmpresa) {
    return clienteService.existeClientePredeterminado(empresaService.getEmpresaPorId(idEmpresa));
  }

  @DeleteMapping("/clientes/{idCliente}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @AccesoRolesPermitidos(Rol.ADMINISTRADOR)
  public void eliminar(@PathVariable long idCliente) {
    clienteService.eliminar(idCliente);
  }

  @PostMapping("/clientes")
  @ResponseStatus(HttpStatus.CREATED)
  @AccesoRolesPermitidos({
    Rol.ADMINISTRADOR,
    Rol.ENCARGADO,
    Rol.VENDEDOR,
    Rol.VIAJANTE,
    Rol.COMPRADOR
  })
  public Cliente guardar(
      @RequestBody Cliente cliente,
      @RequestParam Long idCondicionIVA,
      @RequestParam Long idLocalidad,
      @RequestParam Long idEmpresa,
      @RequestParam(required = false) Long idViajante,
      @RequestParam(required = false) Long idCredencial,
      @RequestHeader("Authorization") String token) {
    if (idCredencial != null) {
      Claims claims =
          Jwts.parser().setSigningKey(secretkey).parseClaimsJws(token.substring(7)).getBody();
      long idUsuarioLoggedIn = (int) claims.get("idUsuario");
      Usuario usuarioLoggedIn = usuarioService.getUsuarioPorId(idUsuarioLoggedIn);
      if (idCredencial != idUsuarioLoggedIn
          && !usuarioLoggedIn.getRoles().contains(Rol.ADMINISTRADOR)) {
        throw new ForbiddenException(
            ResourceBundle.getBundle("Mensajes").getString("mensaje_usuario_rol_no_valido"));
      } else {
        Usuario usuarioCredencial = usuarioService.getUsuarioPorId(idCredencial);
        cliente.setCredencial(usuarioCredencial);
      }
    }
    cliente.setCondicionIVA(condicionIVAService.getCondicionIVAPorId(idCondicionIVA));
    cliente.setLocalidad(localidadService.getLocalidadPorId(idLocalidad));
    cliente.setEmpresa(empresaService.getEmpresaPorId(idEmpresa));
    if (idViajante != null) {
      cliente.setViajante(usuarioService.getUsuarioPorId(idViajante));
    }
    return clienteService.guardar(cliente);
  }

  @PutMapping("/clientes")
  @ResponseStatus(HttpStatus.OK)
  @AccesoRolesPermitidos({
    Rol.ADMINISTRADOR,
    Rol.ENCARGADO,
    Rol.VENDEDOR,
    Rol.VIAJANTE,
    Rol.COMPRADOR
  })
  public void actualizar(
      @RequestBody Cliente clientePorActualizar,
      @RequestParam(required = false) Long idCondicionIVA,
      @RequestParam(required = false) Long idLocalidad,
      @RequestParam(required = false) Long idEmpresa,
      @RequestParam(required = false) Long idViajante,
      @RequestParam(required = false) Long idCredencial,
      @RequestHeader("Authorization") String token) {
    Cliente clientePersistido =
        clienteService.getClientePorId(clientePorActualizar.getId_Cliente());
    if (idCredencial != null) {
      Claims claims =
              Jwts.parser().setSigningKey(secretkey).parseClaimsJws(token.substring(7)).getBody();
      long idUsuarioLoggedIn = (int) claims.get("idUsuario");
      Usuario usuarioLoggedIn = usuarioService.getUsuarioPorId(idUsuarioLoggedIn);
      if (idCredencial != idUsuarioLoggedIn
              && clientePersistido.getCredencial() != null
              && clientePersistido.getCredencial().getId_Usuario() != idCredencial
              && !usuarioLoggedIn.getRoles().contains(Rol.ADMINISTRADOR)) {
        throw new ForbiddenException(
                ResourceBundle.getBundle("Mensajes").getString("mensaje_usuario_rol_no_valido"));
      } else {
        Usuario usuarioCredencial = usuarioService.getUsuarioPorId(idCredencial);
        clientePorActualizar.setCredencial(usuarioCredencial);
      }
    } else {
      clientePorActualizar.setCredencial(null);
    }
    if (idCondicionIVA != null) {
      clientePorActualizar.setCondicionIVA(
          condicionIVAService.getCondicionIVAPorId(idCondicionIVA));
    } else {
      clientePorActualizar.setCondicionIVA(clientePersistido.getCondicionIVA());
    }
    if (idLocalidad != null) {
      clientePorActualizar.setLocalidad(localidadService.getLocalidadPorId(idLocalidad));
    } else {
      clientePorActualizar.setLocalidad(clientePersistido.getLocalidad());
    }
    if (idEmpresa != null) {
      clientePorActualizar.setEmpresa(empresaService.getEmpresaPorId(idEmpresa));
    } else {
      clientePorActualizar.setEmpresa(clientePersistido.getEmpresa());
    }
    clientePorActualizar.setLocalidad(localidadService.getLocalidadPorId(idLocalidad));
    clientePorActualizar.setEmpresa(empresaService.getEmpresaPorId(idEmpresa));
    if (idViajante != null) {
      clientePorActualizar.setViajante(usuarioService.getUsuarioPorId(idViajante));
    } else {
      clientePorActualizar.setViajante(null);
    }
    clientePorActualizar.setNroCliente(clientePersistido.getNroCliente());
    clientePorActualizar.setFechaAlta(clientePersistido.getFechaAlta());
    clientePorActualizar.setEliminado(clientePersistido.isEliminado());
    clienteService.actualizar(clientePorActualizar, clientePersistido);
  }

  @PutMapping("/clientes/{idCliente}/predeterminado")
  @ResponseStatus(HttpStatus.OK)
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO})
  public void setClientePredeterminado(@PathVariable long idCliente) {
    clienteService.setClientePredeterminado(clienteService.getClientePorId(idCliente));
  }

  @GetMapping("/clientes/pedidos/{idPedido}")
  @ResponseStatus(HttpStatus.OK)
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR, Rol.VIAJANTE})
  public Cliente getClientePorIdPedido(@PathVariable long idPedido) {
    return clienteService.getClientePorIdPedido(idPedido);
  }

  @GetMapping("/clientes/usuarios/{idUsuario}/empresas/{idEmpresa}")
  @ResponseStatus(HttpStatus.OK)
  @AccesoRolesPermitidos({
    Rol.ADMINISTRADOR,
    Rol.ENCARGADO,
    Rol.VENDEDOR,
    Rol.VIAJANTE,
    Rol.COMPRADOR
  })
  public Cliente getClientePorIdUsuario(
      @PathVariable long idUsuario, @PathVariable long idEmpresa) {
    return clienteService.getClientePorIdUsuarioYidEmpresa(idUsuario, idEmpresa);
  }
}
