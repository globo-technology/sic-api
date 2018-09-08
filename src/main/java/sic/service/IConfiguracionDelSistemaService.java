package sic.service;

import sic.modelo.ConfiguracionDelSistema;
import sic.modelo.Empresa;
import sic.modelo.TipoDeOperacion;

public interface IConfiguracionDelSistemaService {

    void actualizar(ConfiguracionDelSistema cds);

    ConfiguracionDelSistema getConfiguracionDelSistemaPorEmpresa(Empresa empresa);

    ConfiguracionDelSistema getConfiguracionDelSistemaPorId(long idConfiguracionDelSistema);

    ConfiguracionDelSistema guardar(ConfiguracionDelSistema cds);
    
    void eliminar(ConfiguracionDelSistema cds);
    
    void validarCds(TipoDeOperacion tipoOperacion, ConfiguracionDelSistema cds);

    int getCantidadMaximaDeRenglonesPorIdEmpresa(long  idEmpresa);

    boolean isFacturaElectronicaHabilitada(long  idEmpresa);
    
}
