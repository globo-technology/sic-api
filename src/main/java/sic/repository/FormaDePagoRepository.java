package sic.repository;

import java.util.List;
import org.springframework.data.repository.PagingAndSortingRepository;
import sic.modelo.FormaDePago;

public interface FormaDePagoRepository extends PagingAndSortingRepository<FormaDePago, Long> {

  FormaDePago findByNombreAndEliminada(String nombre, boolean eliminada);

  FormaDePago findByAndPredeterminadoAndEliminada(boolean predeterminado, boolean eliminada);

  List<FormaDePago> findAllByAndEliminadaOrderByNombreAsc(boolean eliminada);

  List<FormaDePago> findAllByOrderByNombreAsc();
}
