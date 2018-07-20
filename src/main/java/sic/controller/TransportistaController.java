package sic.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sic.aspect.AccesoRolesPermitidos;
import sic.modelo.*;
import sic.service.IEmpresaService;
import sic.service.ITransportistaService;

@RestController
@RequestMapping("/api/v1")
public class TransportistaController {

  private final ITransportistaService transportistaService;
  private final IEmpresaService empresaService;

  @Autowired
  public TransportistaController(
      ITransportistaService transportistaService, IEmpresaService empresaService) {
    this.transportistaService = transportistaService;
    this.empresaService = empresaService;
  }

  @GetMapping("/transportistas/{idTransportista}")
  @ResponseStatus(HttpStatus.OK)
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO})
  public Transportista getTransportistaPorId(@PathVariable long idTransportista) {
    return transportistaService.getTransportistaPorId(idTransportista);
  }

  @PutMapping("/transportistas")
  @ResponseStatus(HttpStatus.OK)
  @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO})
  public void actualizar(@RequestBody Transportista transportista) {
    if (transportistaService.getTransportistaPorId(transportista.getId_Transportista()) != null) {
      transportistaService.actualizar(transportista);
    }
  }

  @GetMapping("/transportistas/busqueda/criteria")
  @ResponseStatus(HttpStatus.OK)
  @AccesoRolesPermitidos({
    Rol.ADMINISTRADOR,
    Rol.ENCARGADO,
    Rol.VENDEDOR,
    Rol.VIAJANTE,
    Rol.COMPRADOR
  })
  public List<Transportista> buscarTransportista(
      @RequestParam long idEmpresa,
      @RequestParam(required = false) String nombre,
      @RequestParam(required = false) Long idPais,
      @RequestParam(required = false) Long idProvincia,
      @RequestParam(required = false) Long idLocalidad) {
    BusquedaTransportistaCriteria criteria =
        new BusquedaTransportistaCriteria(
            (nombre != null),
            nombre,
            (idPais != null),
            idPais,
            (idProvincia != null),
            idProvincia,
            (idLocalidad != null),
            idLocalidad,
            idEmpresa);
    return transportistaService.buscarTransportistas(criteria);
  }

  @DeleteMapping("/transportistas/{idTransportista}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @AccesoRolesPermitidos(Rol.ADMINISTRADOR)
  public void eliminar(@PathVariable long idTransportista) {
    transportistaService.eliminar(idTransportista);
  }

  @GetMapping("/transportistas/empresas/{idEmpresa}")
  @ResponseStatus(HttpStatus.OK)
  @AccesoRolesPermitidos({
    Rol.ADMINISTRADOR,
    Rol.ENCARGADO,
    Rol.VENDEDOR,
    Rol.VIAJANTE,
    Rol.COMPRADOR
  })
  public List<Transportista> getTransportistas(@PathVariable long idEmpresa) {
    return transportistaService.getTransportistas(empresaService.getEmpresaPorId(idEmpresa));
  }

  @PostMapping("/transportistas")
  @ResponseStatus(HttpStatus.OK)
  public Transportista guardar(@RequestBody Transportista transportista) {
    return transportistaService.guardar(transportista);
  }
}
