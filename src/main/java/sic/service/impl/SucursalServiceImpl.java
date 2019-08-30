package sic.service.impl;

import java.util.List;
import java.util.Locale;
import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sic.modelo.ConfiguracionDelSistema;
import sic.modelo.Sucursal;
import sic.service.*;
import sic.modelo.TipoDeOperacion;
import sic.repository.SucursalRepository;
import sic.exception.BusinessServiceException;

@Service
public class SucursalServiceImpl implements ISucursalService {

  private final SucursalRepository sucursalRepository;
  private final IConfiguracionDelSistemaService configuracionDelSistemaService;
  private final IPhotoVideoUploader photoVideoUploader;
  private final IUbicacionService ubicacionService;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final MessageSource messageSource;

  @Autowired
  public SucursalServiceImpl(
      SucursalRepository sucursalRepository,
      IConfiguracionDelSistemaService configuracionDelSistemaService,
      IUbicacionService ubicacionService,
      IPhotoVideoUploader photoVideoUploader,
      MessageSource messageSource) {
    this.sucursalRepository = sucursalRepository;
    this.configuracionDelSistemaService = configuracionDelSistemaService;
    this.ubicacionService = ubicacionService;
    this.photoVideoUploader = photoVideoUploader;
    this.messageSource = messageSource;
  }

  @Override
  public Sucursal getSucursalPorId(Long idSucursal) {
    return sucursalRepository
        .findById(idSucursal)
        .orElseThrow(
            () ->
                new EntityNotFoundException(messageSource.getMessage(
                  "mensaje_sucursal_no_existente", null, Locale.getDefault())));
  }

  @Override
  public List<Sucursal> getSucusales() {
    return sucursalRepository.findAllByAndEliminadaOrderByNombreAsc(false);
  }

  @Override
  public Sucursal getSucursalPorNombre(String nombre) {
    return sucursalRepository.findByNombreIsAndEliminadaOrderByNombreAsc(nombre, false);
  }

  @Override
  public Sucursal getSucursalPorIdFiscal(Long idFiscal) {
    return sucursalRepository.findByIdFiscalAndEliminada(idFiscal, false);
  }

  private void validarOperacion(TipoDeOperacion operacion, Sucursal sucursal) {
    // Duplicados
    // Nombre
    Sucursal sucursalDuplicada = this.getSucursalPorNombre(sucursal.getNombre());
    if (operacion == TipoDeOperacion.ALTA && sucursalDuplicada != null) {
      throw new BusinessServiceException(messageSource.getMessage(
        "mensaje_sucursal_duplicado_nombre", null, Locale.getDefault()));
    }
    if (operacion == TipoDeOperacion.ACTUALIZACION) {
      if (sucursalDuplicada != null && sucursalDuplicada.getIdSucursal() != sucursal.getIdSucursal()) {
        throw new BusinessServiceException(messageSource.getMessage(
          "mensaje_sucursal_duplicado_nombre", null, Locale.getDefault()));
      }
    }
    // ID Fiscal
    sucursalDuplicada = this.getSucursalPorIdFiscal(sucursal.getIdFiscal());
    if (operacion == TipoDeOperacion.ALTA
        && sucursalDuplicada != null
        && sucursal.getIdFiscal() != null) {
      throw new BusinessServiceException(messageSource.getMessage(
        "mensaje_sucursal_duplicado_cuip", null, Locale.getDefault()));
    }
    if (operacion == TipoDeOperacion.ACTUALIZACION
        && sucursalDuplicada != null
        && sucursalDuplicada.getIdSucursal() != sucursal.getIdSucursal()
        && sucursal.getIdFiscal() != null) {
      throw new BusinessServiceException(messageSource.getMessage(
        "mensaje_sucursal_duplicado_cuip", null, Locale.getDefault()));
    }
    if (sucursal.getUbicacion() != null
      && sucursal.getUbicacion().getLocalidad() == null) {
      throw new BusinessServiceException(messageSource.getMessage(
        "mensaje_ubicacion_sin_localidad", null, Locale.getDefault()));
    }
  }

  private void crearConfiguracionDelSistema(Sucursal sucursal) {
    ConfiguracionDelSistema cds = new ConfiguracionDelSistema();
    cds.setUsarFacturaVentaPreImpresa(false);
    cds.setCantidadMaximaDeRenglonesEnFactura(28);
    cds.setFacturaElectronicaHabilitada(false);
    cds.setSucursal(sucursal);
    configuracionDelSistemaService.guardar(cds);
  }

  @Override
  @Transactional
  public Sucursal guardar(@Valid Sucursal sucursal) {
    if (sucursal.getUbicacion() != null && sucursal.getUbicacion().getIdLocalidad() != null) {
      sucursal
          .getUbicacion()
          .setLocalidad(
              ubicacionService.getLocalidadPorId(sucursal.getUbicacion().getIdLocalidad()));
    }
    validarOperacion(TipoDeOperacion.ALTA, sucursal);
    sucursal = sucursalRepository.save(sucursal);
    crearConfiguracionDelSistema(sucursal);
    logger.warn("La Sucursal {} se guardó correctamente.", sucursal);
    return sucursal;
  }

  @Override
  @Transactional
  public void actualizar(@Valid Sucursal sucursalParaActualizar, Sucursal sucursalPersistida) {
    if (sucursalPersistida.getLogo() != null
        && !sucursalPersistida.getLogo().isEmpty()
        && (sucursalParaActualizar.getLogo() == null || sucursalParaActualizar.getLogo().isEmpty())) {
      photoVideoUploader.borrarImagen(
          Sucursal.class.getSimpleName() + sucursalPersistida.getIdSucursal());
    }
    this.validarOperacion(TipoDeOperacion.ACTUALIZACION, sucursalParaActualizar);
    sucursalRepository.save(sucursalParaActualizar);
  }

  @Override
  @Transactional
  public void eliminar(Long idSucursal) {
    Sucursal sucursal = this.getSucursalPorId(idSucursal);
    sucursal.setEliminada(true);
    sucursal.setUbicacion(null);
    if (sucursal.getLogo() != null && !sucursal.getLogo().isEmpty()) {
      photoVideoUploader.borrarImagen(Sucursal.class.getSimpleName() + sucursal.getIdSucursal());
    }
    configuracionDelSistemaService.eliminar(
        configuracionDelSistemaService.getConfiguracionDelSistemaPorSucursal(sucursal));
    sucursalRepository.save(sucursal);
  }

  @Override
  public String guardarLogo(long idSucursal, byte[] imagen) {
    if (imagen.length > 1024000L)
      throw new BusinessServiceException(messageSource.getMessage(
        "mensaje_error_tamanio_no_valido", null, Locale.getDefault()));
    return photoVideoUploader.subirImagen(Sucursal.class.getSimpleName() + idSucursal, imagen);
  }
}