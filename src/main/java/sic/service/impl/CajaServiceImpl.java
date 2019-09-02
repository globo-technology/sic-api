package sic.service.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.DateExpression;
import com.querydsl.core.types.dsl.Expressions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import org.springframework.context.MessageSource;
import org.springframework.validation.annotation.Validated;
import sic.modelo.*;
import sic.modelo.criteria.BusquedaCajaCriteria;
import sic.service.*;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sic.exception.BusinessServiceException;
import sic.util.FormatterFechaHora;
import sic.util.Validator;
import sic.repository.CajaRepository;

@Service
@Validated
public class CajaServiceImpl implements ICajaService {

  private final CajaRepository cajaRepository;
  private final IFormaDePagoService formaDePagoService;
  private final IGastoService gastoService;
  private final ISucursalService sucursalService;
  private final IUsuarioService usuarioService;
  private final IReciboService reciboService;
  private final IClockService clockService;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final MessageSource messageSource;

  @Autowired
  public CajaServiceImpl(
      CajaRepository cajaRepository,
      IFormaDePagoService formaDePagoService,
      IGastoService gastoService,
      ISucursalService sucursalService,
      IUsuarioService usuarioService,
      IReciboService reciboService,
      IClockService clockService,
      MessageSource messageSource) {
    this.cajaRepository = cajaRepository;
    this.formaDePagoService = formaDePagoService;
    this.gastoService = gastoService;
    this.sucursalService = sucursalService;
    this.usuarioService = usuarioService;
    this.reciboService = reciboService;
    this.clockService = clockService;
    this.messageSource = messageSource;
  }

  @Override
  public void validarOperacion(@Valid Caja caja) {
    // Una Caja por dia
    Caja ultimaCaja = this.getUltimaCaja(caja.getSucursal().getIdSucursal());
    if (ultimaCaja != null) {
      if (ultimaCaja.getEstado() == EstadoCaja.ABIERTA) {
        throw new BusinessServiceException(messageSource.getMessage(
          "mensaje_caja_anterior_abierta", null, Locale.getDefault()));
      }
      if (Validator.compararDias(ultimaCaja.getFechaApertura(), caja.getFechaApertura()) >= 0) {
        throw new BusinessServiceException(messageSource.getMessage(
          "mensaje_fecha_apertura_no_valida", null, Locale.getDefault()));
      }
    }
    // Duplicados
    if (cajaRepository.findById(caja.getId_Caja()) != null) {
      throw new BusinessServiceException(messageSource.getMessage(
        "mensaje_caja_duplicada", null, Locale.getDefault()));
    }
  }

  @Override
  public void validarMovimiento(Date fechaMovimiento, long idSucursal) {
    Caja caja = this.getUltimaCaja(idSucursal);
    if (caja == null) {
      throw new BusinessServiceException(messageSource.getMessage(
        "mensaje_caja_no_existente", null, Locale.getDefault()));
    }
    if (caja.getEstado().equals(EstadoCaja.CERRADA)) {
      throw new BusinessServiceException(messageSource.getMessage(
        "mensaje_caja_cerrada", null, Locale.getDefault()));
    }
    if (fechaMovimiento.before(caja.getFechaApertura())) {
      throw new BusinessServiceException(messageSource.getMessage(
        "mensaje_caja_movimiento_fecha_no_valida", null, Locale.getDefault()));
    }
  }

  @Override
  @Transactional
  public Caja abrirCaja(Sucursal sucursal, Usuario usuarioApertura, BigDecimal saldoApertura) {
    Caja caja = new Caja();
    caja.setEstado(EstadoCaja.ABIERTA);
    caja.setSucursal(sucursal);
    caja.setSaldoApertura(saldoApertura);
    caja.setUsuarioAbreCaja(usuarioApertura);
    caja.setFechaApertura(this.clockService.getFechaActual());
    this.validarOperacion(caja);
    return cajaRepository.save(caja);
  }

  @Override
  public void actualizar(@Valid Caja caja) {
    cajaRepository.save(caja);
  }

  @Override
  @Transactional
  public void eliminar(Long idCaja) {
    Caja caja = this.getCajaPorId(idCaja);
    if (caja == null) {
      throw new EntityNotFoundException(messageSource.getMessage(
        "mensaje_caja_no_existente", null, Locale.getDefault()));
    }
    caja.setEliminada(true);
    this.actualizar(caja);
  }

  @Override
  public Caja getUltimaCaja(long idSucursal) {
    Pageable pageable = PageRequest.of(0, 1);
    List<Caja> topCaja =
        cajaRepository
            .findTopBySucursalAndEliminadaOrderByIdCajaDesc(idSucursal, pageable)
            .getContent();
    return (topCaja.isEmpty()) ? null : topCaja.get(0);
  }

  @Override
  public Caja getCajaPorId(Long idCaja) {
    Optional<Caja> caja = cajaRepository.findById(idCaja);
    if (caja.isPresent() && !caja.get().isEliminada()) {
      return caja.get();
    } else {
      throw new EntityNotFoundException(messageSource.getMessage(
        "mensaje_caja_no_existente", null, Locale.getDefault()));
    }
  }

  @Override
  public Page<Caja> getCajasCriteria(BusquedaCajaCriteria criteria) {
    int pageNumber = 0;
    int pageSize = Integer.MAX_VALUE;
    Sort sorting = new Sort(Sort.Direction.DESC, "fechaApertura");
    if (criteria.getPageable() != null) {
      pageNumber = criteria.getPageable().getPageNumber();
      pageSize = criteria.getPageable().getPageSize();
      sorting = criteria.getPageable().getSort();
    }
    Pageable pageable = PageRequest.of(pageNumber, pageSize, sorting);
    return cajaRepository.findAll(getBuilder(criteria), pageable);
  }

  private BooleanBuilder getBuilder(BusquedaCajaCriteria criteria) {
    // Fecha
    if (criteria.isBuscaPorFecha()
        && (criteria.getFechaDesde() == null || criteria.getFechaHasta() == null)) {
      throw new BusinessServiceException(messageSource.getMessage(
        "mensaje_caja_fechas_invalidas", null, Locale.getDefault()));
    }
    if (criteria.isBuscaPorFecha()) {
      Calendar cal = new GregorianCalendar();
      cal.setTime(criteria.getFechaDesde());
      cal.set(Calendar.HOUR_OF_DAY, 0);
      cal.set(Calendar.MINUTE, 0);
      cal.set(Calendar.SECOND, 0);
      criteria.setFechaDesde(cal.getTime());
      cal.setTime(criteria.getFechaHasta());
      cal.set(Calendar.HOUR_OF_DAY, 23);
      cal.set(Calendar.MINUTE, 59);
      cal.set(Calendar.SECOND, 59);
      criteria.setFechaHasta(cal.getTime());
    }
    QCaja qcaja = QCaja.caja;
    BooleanBuilder builder = new BooleanBuilder();
    builder.and(
        qcaja.sucursal.idSucursal.eq(criteria.getIdSucursal()).and(qcaja.eliminada.eq(false)));
    if (criteria.isBuscaPorUsuarioApertura() && !criteria.isBuscaPorUsuarioCierre()) {
      builder.and(qcaja.usuarioAbreCaja.id_Usuario.eq(criteria.getIdUsuarioApertura()));
    }
    if (criteria.isBuscaPorUsuarioCierre() && !criteria.isBuscaPorUsuarioApertura()) {
      builder.and(qcaja.usuarioCierraCaja.id_Usuario.eq(criteria.getIdUsuarioCierre()));
    }
    if (criteria.isBuscaPorUsuarioCierre() && criteria.isBuscaPorUsuarioApertura()) {
      builder.and(
          qcaja
              .usuarioAbreCaja
              .id_Usuario
              .eq(criteria.getIdUsuarioApertura())
              .and(qcaja.usuarioCierraCaja.id_Usuario.eq(criteria.getIdUsuarioCierre())));
    }
    if (criteria.isBuscaPorFecha()) {
      FormatterFechaHora formateadorFecha =
          new FormatterFechaHora(FormatterFechaHora.FORMATO_FECHAHORA_INTERNACIONAL);
      DateExpression<Date> fDesde =
          Expressions.dateTemplate(
              Date.class,
              "convert({0}, datetime)",
              formateadorFecha.format(criteria.getFechaDesde()));
      DateExpression<Date> fHasta =
          Expressions.dateTemplate(
              Date.class,
              "convert({0}, datetime)",
              formateadorFecha.format(criteria.getFechaHasta()));
      builder.and(qcaja.fechaApertura.between(fDesde, fHasta));
    }
    return builder;
  }

  @Override
  public Caja cerrarCaja(long idCaja, BigDecimal monto, Long idUsuario, boolean scheduling) {
    Caja cajaACerrar = this.getCajaPorId(idCaja);
    cajaACerrar.setSaldoReal(monto);
    if (scheduling) {
      LocalDateTime fechaCierre =
        LocalDateTime.ofInstant(cajaACerrar.getFechaApertura().toInstant(), ZoneId.systemDefault());
      fechaCierre = fechaCierre.withHour(23);
      fechaCierre = fechaCierre.withMinute(59);
      fechaCierre = fechaCierre.withSecond(59);
      cajaACerrar.setFechaCierre(Date.from(fechaCierre.atZone(ZoneId.systemDefault()).toInstant()));
    } else {
      cajaACerrar.setFechaCierre(this.clockService.getFechaActual());
    }
    if (idUsuario != null) {
      cajaACerrar.setUsuarioCierraCaja(usuarioService.getUsuarioNoEliminadoPorId(idUsuario));
    }
    cajaACerrar.setSaldoSistema(this.getSaldoSistema(cajaACerrar));
    cajaACerrar.setEstado(EstadoCaja.CERRADA);
    this.actualizar(cajaACerrar);
    logger.warn("La Caja {} se cerró correctamente.", cajaACerrar);
    return cajaACerrar;
  }

  @Scheduled(cron = "30 0 0 * * *") // Todos los dias a las 00:00:30
  public void cerrarCajas() {
    logger.warn("Cierre automático de Cajas a las {}", LocalDateTime.now());
    List<Sucursal> sucursals = this.sucursalService.getSucusales();
    sucursals
        .stream()
        .map(sucursal -> this.getUltimaCaja(sucursal.getIdSucursal()))
        .filter(
            ultimaCajaDeSucursal ->
                ((ultimaCajaDeSucursal != null)
                    && (ultimaCajaDeSucursal.getEstado() == EstadoCaja.ABIERTA)))
        .forEachOrdered(
            ultimaCajaDeSucursal -> {
              LocalDate fechaActual =
                  LocalDate.of(
                      LocalDate.now().getYear(),
                      LocalDate.now().getMonth(),
                      LocalDate.now().getDayOfMonth());
              Calendar fechaHoraCaja = new GregorianCalendar();
              fechaHoraCaja.setTime(ultimaCajaDeSucursal.getFechaApertura());
              LocalDate fechaCaja =
                  LocalDate.of(
                      fechaHoraCaja.get(Calendar.YEAR),
                      fechaHoraCaja.get(Calendar.MONTH) + 1,
                      fechaHoraCaja.get(Calendar.DAY_OF_MONTH));
              if (fechaCaja.compareTo(fechaActual) < 0) {
                this.cerrarCaja(
                    ultimaCajaDeSucursal.getId_Caja(),
                    this.getSaldoQueAfectaCaja(ultimaCajaDeSucursal),
                    ultimaCajaDeSucursal.getUsuarioAbreCaja().getId_Usuario(),
                    true);
              }
            });
  }

  @Override
  public BigDecimal getSaldoQueAfectaCaja(Caja caja) {
    Date fechaHasta = new Date();
    if (caja.getFechaCierre() != null) {
      fechaHasta = caja.getFechaCierre();
    }
    BigDecimal totalRecibosCliente =
        reciboService.getTotalRecibosClientesQueAfectanCajaEntreFechas(
            caja.getSucursal().getIdSucursal(), caja.getFechaApertura(), fechaHasta);
    BigDecimal totalRecibosProveedor =
        reciboService.getTotalRecibosProveedoresQueAfectanCajaEntreFechas(
            caja.getSucursal().getIdSucursal(), caja.getFechaApertura(), fechaHasta);
    BigDecimal totalGastos =
        gastoService.getTotalGastosQueAfectanCajaEntreFechas(
            caja.getSucursal().getIdSucursal(), caja.getFechaApertura(), fechaHasta);
    return caja.getSaldoApertura()
        .add(totalRecibosCliente)
        .subtract(totalRecibosProveedor)
        .subtract(totalGastos);
  }

  @Override
  public BigDecimal getSaldoSistema(Caja caja) {
    if (caja.getEstado().equals(EstadoCaja.ABIERTA)) {
      Date fechaHasta = new Date();
      if (caja.getFechaCierre() != null) {
        fechaHasta = caja.getFechaCierre();
      }
      BigDecimal totalRecibosCliente =
          reciboService.getTotalRecibosClientesEntreFechas(
              caja.getSucursal().getIdSucursal(), caja.getFechaApertura(), fechaHasta);
      BigDecimal totalRecibosProveedor =
          reciboService.getTotalRecibosProveedoresEntreFechas(
              caja.getSucursal().getIdSucursal(), caja.getFechaApertura(), fechaHasta);
      BigDecimal totalGastos =
          gastoService.getTotalGastosEntreFechas(
              caja.getSucursal().getIdSucursal(), caja.getFechaApertura(), fechaHasta);
      return caja.getSaldoApertura()
          .add(totalRecibosCliente)
          .subtract(totalRecibosProveedor)
          .subtract(totalGastos);
    } else {
      return caja.getSaldoSistema();
    }
  }

  @Override
  public boolean isUltimaCajaAbierta(long idSucursal) {
    Caja caja = cajaRepository.isUltimaCajaAbierta(idSucursal);
    return (caja != null)
        && cajaRepository.isUltimaCajaAbierta(idSucursal).getEstado().equals(EstadoCaja.ABIERTA);
  }

  private BigDecimal getTotalMovimientosPorFormaDePago(Caja caja, FormaDePago fdp) {
    Date fechaHasta = new Date();
    if (caja.getFechaCierre() != null) {
      fechaHasta = caja.getFechaCierre();
    }
    BigDecimal recibosTotal =
        reciboService
            .getTotalRecibosClientesEntreFechasPorFormaDePago(
                caja.getSucursal().getIdSucursal(),
                fdp.getId_FormaDePago(),
                caja.getFechaApertura(),
                fechaHasta)
            .subtract(
                reciboService.getTotalRecibosProveedoresEntreFechasPorFormaDePago(
                    caja.getSucursal().getIdSucursal(),
                    fdp.getId_FormaDePago(),
                    caja.getFechaApertura(),
                    fechaHasta));
    BigDecimal gastosTotal =
        gastoService.getTotalGastosEntreFechasYFormaDePago(
            caja.getSucursal().getIdSucursal(),
            fdp.getId_FormaDePago(),
            caja.getFechaApertura(),
            fechaHasta);
    return recibosTotal.subtract(gastosTotal);
  }

  @Override
  public Map<Long, BigDecimal> getTotalesDeFormaDePago(long idCaja) {
    Caja caja = cajaRepository.findById(idCaja);
    Map<Long, BigDecimal> totalesPorFomaDePago = new HashMap<>();
    formaDePagoService
        .getFormasDePago()
        .forEach(
            fdp -> {
              BigDecimal total = this.getTotalMovimientosPorFormaDePago(caja, fdp);
              if (total.compareTo(BigDecimal.ZERO) != 0) {
                totalesPorFomaDePago.put(fdp.getId_FormaDePago(), total);
              }
            });
    return totalesPorFomaDePago;
  }

  @Override
  public BigDecimal getSaldoSistemaCajas(BusquedaCajaCriteria criteria) {
    return cajaRepository.getSaldoSistemaCajas(this.getBuilder(criteria));
  }

  @Override
  public BigDecimal getSaldoRealCajas(BusquedaCajaCriteria criteria) {
    return cajaRepository.getSaldoRealCajas(this.getBuilder(criteria));
  }

  @Override
  public List<MovimientoCaja> getMovimientosPorFormaDePagoEntreFechas(
    Sucursal sucursal, FormaDePago formaDePago, Date desde, Date hasta) {
    List<MovimientoCaja> movimientos = new ArrayList<>();
    gastoService
        .getGastosEntreFechasYFormaDePago(sucursal, formaDePago, desde, hasta)
        .forEach(gasto -> movimientos.add(new MovimientoCaja(gasto)));
    reciboService
        .getRecibosEntreFechasPorFormaDePago(desde, hasta, formaDePago, sucursal)
        .forEach(recibo -> movimientos.add(new MovimientoCaja(recibo)));
    Collections.sort(movimientos);
    return movimientos;
  }

  @Override
  @Transactional
  public void reabrirCaja(long idCaja, BigDecimal saldoAperturaNuevo) {
    Caja caja = getCajaPorId(idCaja);
    Caja ultimaCaja = this.getUltimaCaja(caja.getSucursal().getIdSucursal());
    if (ultimaCaja == null) {
      throw new BusinessServiceException(messageSource.getMessage(
        "mensaje_caja_no_existente", null, Locale.getDefault()));
    }
    if (caja.getId_Caja() == ultimaCaja.getId_Caja()) {
      caja.setSaldoSistema(null);
      caja.setSaldoApertura(saldoAperturaNuevo);
      caja.setSaldoReal(null);
      caja.setEstado(EstadoCaja.ABIERTA);
      caja.setUsuarioCierraCaja(null);
      caja.setFechaCierre(null);
      this.actualizar(caja);
    } else {
      throw new EntityNotFoundException(messageSource.getMessage(
        "mensaje_caja_re_apertura_no_valida", null, Locale.getDefault()));
    }
  }

  @Override
  public Caja encontrarCajaCerradaQueContengaFechaEntreFechaAperturaYFechaCierre(
    long idSucursal, Date fecha) {
    return cajaRepository.encontrarCajaCerradaQueContengaFechaEntreFechaAperturaYFechaCierre(
      idSucursal, fecha);
  }

  @Override
  @Transactional
  public int actualizarSaldoSistema(Caja caja, BigDecimal monto) {
    return cajaRepository.actualizarSaldoSistema(caja.getId_Caja(), monto);
  }
}
