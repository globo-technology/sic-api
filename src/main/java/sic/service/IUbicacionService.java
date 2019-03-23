package sic.service;

import org.springframework.data.domain.Page;
import sic.modelo.*;

import java.util.List;

public interface IUbicacionService {

  Ubicacion getUbicacionPorId(long idUbicacion);

  Page<Localidad> buscar(BusquedaLocalidadCriteria criteria);

  Ubicacion guardarUbicacionDeFacturacionCliente(
    Ubicacion ubicacion,
    Cliente cliente);

  Ubicacion guardarUbicacionDeEnvioCliente(
    Ubicacion ubicacion,
    Cliente cliente);

  Ubicacion guardaUbicacionEmpresa(
    Ubicacion ubicacion,
    Empresa empresa);

  Ubicacion guardaUbicacionProveedor(
    Ubicacion ubicacion,
    Proveedor proveedor);

  Ubicacion guardarUbicacionTransportista(
    Ubicacion ubicacion,
    Transportista transportista);

  Ubicacion guardar(
    Ubicacion ubicacion);

  void actualizar(Ubicacion ubicacion);

  Localidad getLocalidadPorId(Long idLocalidad);

  Localidad getLocalidadPorNombre(String nombre, Provincia provincia);

  List<Localidad> getLocalidadesDeLaProvincia(Provincia provincia);

  Provincia getProvinciaPorId(Long idProvincia);

  Provincia getProvinciaPorNombre(String nombre);

  List<Provincia> getProvincias();

  void actualizarLocalidad(Localidad localidad);

  void validarOperacion(TipoDeOperacion operacion, Localidad localidad);
}
