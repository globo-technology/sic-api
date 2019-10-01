package sic.service;

import java.util.List;

import org.springframework.data.domain.Page;
import sic.modelo.criteria.BusquedaProveedorCriteria;
import sic.modelo.Proveedor;

import javax.validation.Valid;

public interface IProveedorService {

  Proveedor getProveedorNoEliminadoPorId(long idProveedor);

  void actualizar(@Valid Proveedor proveedor);

  Page<Proveedor> buscarProveedores(BusquedaProveedorCriteria criteria);

  void eliminar(long idProveedor);

  Proveedor getProveedorPorRazonSocial(String razonSocial);

  List<Proveedor> getProveedores();

  Proveedor guardar(@Valid Proveedor proveedor);

  String generarNroDeProveedor();
}
