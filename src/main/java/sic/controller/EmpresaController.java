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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sic.aspect.AccesoRolesPermitidos;
import sic.modelo.Empresa;
import sic.modelo.Rol;
import sic.service.IEmpresaService;

@RestController
@RequestMapping("/api/v1")
public class EmpresaController {
        
    public final IEmpresaService empresaService;

    @Autowired
    public EmpresaController(IEmpresaService empresaService) {
        this.empresaService = empresaService;
    }    
    
    @GetMapping("/empresas")
    @ResponseStatus(HttpStatus.OK)
    @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR, Rol.VIAJANTE, Rol.COMPRADOR})
    public List<Empresa> getEmpresas() {
        return empresaService.getEmpresas();
    }
    
    @GetMapping("/empresas/{idEmpresa}")
    @ResponseStatus(HttpStatus.OK)
    @AccesoRolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.VENDEDOR, Rol.VIAJANTE, Rol.COMPRADOR})
    public Empresa getEmpresaPorId(@PathVariable long idEmpresa) {
        return empresaService.getEmpresaPorId(idEmpresa);
    }
    
    @PostMapping("/empresas")
    @ResponseStatus(HttpStatus.CREATED)
    @AccesoRolesPermitidos(Rol.ADMINISTRADOR)
    public Empresa guardar(@RequestBody Empresa empresa) {
        return empresaService.guardar(empresa);
    }

  @PutMapping("/empresas")
  @ResponseStatus(HttpStatus.OK)
  @AccesoRolesPermitidos(Rol.ADMINISTRADOR)
  public void actualizar(@RequestBody Empresa empresa) {
    if (empresaService.getEmpresaPorId(empresa.getId_Empresa()) != null)
      empresaService.actualizar(empresa);
  }

    @DeleteMapping("/empresas/{idEmpresa}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @AccesoRolesPermitidos(Rol.ADMINISTRADOR)
    public void eliminar(@PathVariable long idEmpresa) {
        empresaService.eliminar(idEmpresa);
    }
    
    @PostMapping("/empresas/logo")
    @ResponseStatus(HttpStatus.CREATED)
    @AccesoRolesPermitidos(Rol.ADMINISTRADOR)
    public String uploadLogo(@RequestBody byte[] imagen) {
        return empresaService.guardarLogo(imagen);
    }
}
