package sic.service.impl;

import java.math.BigDecimal;

import org.springframework.validation.annotation.Validated;
import sic.modelo.*;
import sic.service.IGastoService;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sic.service.BusinessServiceException;
import sic.repository.GastoRepository;
import sic.service.ICajaService;
import sic.service.IEmpresaService;

@Service
@Validated
public class GastoServiceImpl implements IGastoService {

    private final GastoRepository gastoRepository;
    private final IEmpresaService empresaService;
    private final ICajaService cajaService;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    @Lazy
    public GastoServiceImpl(GastoRepository gastoRepository, IEmpresaService empresaService, ICajaService cajaService) {
        this.gastoRepository = gastoRepository;
        this.empresaService = empresaService;
        this.cajaService = cajaService;
    }
    
    @Override
    public Gasto getGastoPorId(Long idGasto) {
        Gasto gasto = gastoRepository.findById(idGasto);
        if (gasto == null) {
            throw new EntityNotFoundException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_gasto_no_existente"));
        }
        return gasto;
    }

    @Override
    public void validarGasto(Gasto gasto) {
        this.cajaService.validarMovimiento(gasto.getFecha(), gasto.getEmpresa().getId_Empresa());
        if (gastoRepository.findOne(gasto.getId_Gasto()) != null) {
            throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_gasto_duplicada"));
        }
    }

    @Override
    @Transactional
    public Gasto guardar(@Valid Gasto gasto) {
        this.validarGasto(gasto);
        gasto.setNroGasto(this.getUltimoNumeroDeGasto(gasto.getEmpresa().getId_Empresa()) + 1);
        gasto = gastoRepository.save(gasto);
        logger.warn("El Gasto {} se guardó correctamente.", gasto);
        return gasto;
    }

    @Override
    public List<Gasto> getGastosEntreFechasYFormaDePago(Empresa empresa, FormaDePago formaDePago, Date desde, Date hasta) {
        return gastoRepository.getGastosEntreFechasPorFormaDePago(empresa.getId_Empresa(), formaDePago.getId_FormaDePago(), desde, hasta);
    }

    @Override
    @Transactional
    public void actualizar(Gasto gasto) {
        gastoRepository.save(gasto);
    }
    
    @Override
    @Transactional
    public void eliminar(long idGasto) {
        Gasto gastoParaEliminar = this.getGastoPorId(idGasto);
        if (this.cajaService.getUltimaCaja(gastoParaEliminar.getEmpresa().getId_Empresa()).getEstado().equals(EstadoCaja.CERRADA)) {
            throw new BusinessServiceException(ResourceBundle.getBundle("Mensajes")
                    .getString("mensaje_caja_cerrada"));
        }
        gastoParaEliminar.setEliminado(true);
        gastoRepository.save(gastoParaEliminar);
    }

    @Override
    public long getUltimoNumeroDeGasto(long idEmpresa) {
        Gasto gasto =
          gastoRepository.findTopByEmpresaAndEliminadoOrderByNroGastoDesc(empresaService.getEmpresaPorId(idEmpresa), false);
        if (gasto == null) {
            return 1; // No existe ningun Gasto anterior
        } else {
            return 1 + gasto.getNroGasto();
        }
    }

    @Override
    public BigDecimal getTotalGastosEntreFechasYFormaDePago(long idEmpresa, long idFormaDePago, Date desde, Date hasta) {
        BigDecimal total = gastoRepository.getTotalGastosEntreFechasPorFormaDePago(idEmpresa, idFormaDePago, desde, hasta);
        return (total == null) ? BigDecimal.ZERO : total;
    }

    @Override
    public BigDecimal getTotalGastosQueAfectanCajaEntreFechas(long idEmpresa, Date desde, Date hasta) {
        BigDecimal total = gastoRepository.getTotalGastosQueAfectanCajaEntreFechas(idEmpresa, desde, hasta);
        return (total == null) ? BigDecimal.ZERO : total;
    }

    @Override
    public BigDecimal getTotalGastosEntreFechas(long idEmpresa, Date desde, Date hasta) {
        BigDecimal total = gastoRepository.getTotalGastosEntreFechas(idEmpresa, desde, hasta);
        return (total == null) ? BigDecimal.ZERO : total;
    }

}
