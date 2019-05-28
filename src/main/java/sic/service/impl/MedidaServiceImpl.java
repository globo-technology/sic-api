package sic.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import sic.modelo.Empresa;
import sic.modelo.Medida;
import sic.service.IMedidaService;
import sic.service.BusinessServiceException;
import sic.modelo.TipoDeOperacion;
import sic.repository.MedidaRepository;

@Service
@Validated
public class MedidaServiceImpl implements IMedidaService {

  private final MedidaRepository medidaRepository;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  public MedidaServiceImpl(MedidaRepository medidaRepository) {
    this.medidaRepository = medidaRepository;
  }

  @Override
  public Medida getMedidaNoEliminadaPorId(Long idMedida) {
    Optional<Medida> medida = medidaRepository
      .findById(idMedida);
    if (medida.isPresent() && !medida.get().isEliminada()) {
      return medida.get();
    } else {
      throw new EntityNotFoundException(
        ResourceBundle.getBundle("Mensajes").getString("mensaje_medida_no_existente"));
    }
  }

  @Override
  public List<Medida> getUnidadMedidas(Empresa empresa) {
    return medidaRepository.findAllByAndEmpresaAndEliminadaOrderByNombreAsc(empresa, false);
  }

  @Override
  public Medida getMedidaPorNombre(String nombre, Empresa empresa) {
    return medidaRepository.findByNombreAndEmpresaAndEliminada(nombre, empresa, false);
  }

  @Override
  public void validarOperacion(TipoDeOperacion operacion, Medida medida) {
    // Duplicados
    // Nombre
    Medida medidaDuplicada = this.getMedidaPorNombre(medida.getNombre(), medida.getEmpresa());
    if (operacion.equals(TipoDeOperacion.ALTA) && medidaDuplicada != null) {
      throw new BusinessServiceException(
          ResourceBundle.getBundle("Mensajes").getString("mensaje_medida_duplicada_nombre"));
    }
    if (operacion.equals(TipoDeOperacion.ACTUALIZACION)
        && medidaDuplicada != null
        && medidaDuplicada.getId_Medida() != medida.getId_Medida()) {
      throw new BusinessServiceException(
          ResourceBundle.getBundle("Mensajes").getString("mensaje_medida_duplicada_nombre"));
    }
  }

  @Override
  @Transactional
  public void actualizar(@Valid Medida medida) {
    this.validarOperacion(TipoDeOperacion.ACTUALIZACION, medida);
    medidaRepository.save(medida);
  }

  @Override
  @Transactional
  public Medida guardar(@Valid Medida medida) {
    this.validarOperacion(TipoDeOperacion.ALTA, medida);
    medida = medidaRepository.save(medida);
    logger.warn("La Medida {} se guardó correctamente.", medida);
    return medida;
  }

  @Override
  @Transactional
  public void eliminar(long idMedida) {
    Medida medida = this.getMedidaNoEliminadaPorId(idMedida);
    if (medida == null) {
      throw new EntityNotFoundException(
          ResourceBundle.getBundle("Mensajes").getString("mensaje_medida_no_existente"));
    }
    medida.setEliminada(true);
    medidaRepository.save(medida);
  }
}
