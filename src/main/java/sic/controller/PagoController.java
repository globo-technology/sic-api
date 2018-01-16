package sic.controller;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sic.modelo.Pago;
import sic.service.IFacturaService;
import sic.service.INotaService;
import sic.service.IPagoService;

@RestController
@RequestMapping("/api/v1")
public class PagoController {
    
    private final IPagoService pagoService;
    private final IFacturaService facturaService;
    private final INotaService notaService;
    private final int TAMANIO_PAGINA_DEFAULT = 50;
    
    @Autowired
    public PagoController(IPagoService pagoService, IFacturaService facturaService, INotaService notaService) {
        this.pagoService = pagoService;
        this.facturaService = facturaService;     
        this.notaService = notaService;
    }
    
    @GetMapping("/pagos/{idPago}")
    @ResponseStatus(HttpStatus.OK)
    public Pago getPagoPorId(@PathVariable long idPago) {
        return pagoService.getPagoPorId(idPago);
    }
    
    @GetMapping("/pagos/total")
    @ResponseStatus(HttpStatus.OK)
    public double calcularTotalPagos(@RequestParam long[] idPago) {
        List<Pago> pagos = new ArrayList<>();
        for (long id : idPago) {
            pagos.add(pagoService.getPagoPorId(id));
        }
        return pagoService.calcularTotalPagos(pagos);
    }
    
    @GetMapping("/pagos/facturas/{idFactura}")
    @ResponseStatus(HttpStatus.OK)
    public List<Pago> getPagosDeLaFactura(@PathVariable long idFactura) {
        return pagoService.getPagosDeLaFactura(idFactura);
    }
    
    @GetMapping("/pagos/notas/{idNota}")
    @ResponseStatus(HttpStatus.OK)
    public List<Pago> getPagosDeLaNotaDebito(@PathVariable long idNota) {
        return notaService.getPagosNota(idNota);
    }
    
    @GetMapping("/pagos/recibos/{idRecibo}")
    @ResponseStatus(HttpStatus.OK)
    public List<Pago> getPagosDeRecibo(@PathVariable long idRecibo) {
        return pagoService.getPagosRelacionadosAlRecibo(idRecibo);
    }
    
    @GetMapping("/pagos/clientes/{idCliente}")
    @ResponseStatus(HttpStatus.OK)
    public Page<Pago> getPagosPorClienteEntreFechas(@PathVariable long idCliente,
                                                    @RequestParam(required = false) Long desde,
                                                    @RequestParam(required = false) Long hasta, 
                                                    @RequestParam(required = false) Integer pagina,
                                                    @RequestParam(required = false) Integer tamanio) {
        Calendar fechaDesde = Calendar.getInstance();
        Calendar fechaHasta = Calendar.getInstance();
        if ((desde != null) && (hasta != null)) {
            fechaDesde.setTimeInMillis(desde);    
            fechaHasta.setTimeInMillis(hasta);
        }
        fechaDesde.set(Calendar.HOUR_OF_DAY, 0);
        fechaDesde.set(Calendar.MINUTE, 0);
        fechaDesde.set(Calendar.SECOND, 0);
        fechaHasta.set(Calendar.HOUR_OF_DAY, 23);
        fechaHasta.set(Calendar.MINUTE, 59);
        fechaHasta.set(Calendar.SECOND, 59);
        if (tamanio == null || tamanio <= 0) {
            tamanio = TAMANIO_PAGINA_DEFAULT;
        }
        if (pagina == null || pagina < 0) {
            pagina = 0;
        }
        Pageable pageable = new PageRequest(pagina, tamanio, new Sort(Sort.Direction.DESC, "fecha"));
        return pagoService.getPagosPorClienteEntreFechas(idCliente, fechaDesde.getTime(), fechaHasta.getTime(), pageable);
    }
    
    @GetMapping("/pagos/facturas/{idFactura}/saldo")
    @ResponseStatus(HttpStatus.OK)
    public double getSaldoAPagarFacturas(@PathVariable long idFactura) {
        return pagoService.getSaldoAPagarFactura(idFactura);
    }
    
    @GetMapping("/pagos/facturas/{idFactura}/total-pagado")
    @ResponseStatus(HttpStatus.OK)
    public double getTotalPagadoFacturas(@PathVariable long idFactura) {
        return facturaService.getTotalPagado(idFactura);
    }
    
    @GetMapping("/pagos/notas/{idNota}/saldo")
    @ResponseStatus(HttpStatus.OK)
    public double getSaldoAPagarNotas(@PathVariable long idNota) {
        return pagoService.getSaldoAPagarNotaDebito(idNota);
    }
    
    @GetMapping("/pagos/notas/{idNota}/total-pagado")
    @ResponseStatus(HttpStatus.OK)
    public double getTotalPagadoNotas(@PathVariable long idNota) {
        return notaService.getTotalPagado(idNota);
    }
    
    @GetMapping("/pagos/busqueda")
    @ResponseStatus(HttpStatus.OK)
    public List<Pago> getPagosPorCajaYFormaDePago(@RequestParam long idEmpresa,
                                                  @RequestParam long idFormaDePago,
                                                  @RequestParam long desde,
                                                  @RequestParam long hasta) {
        Date fechaDesde = new Date(desde);
        Date fechaHasta = new Date(hasta);
        return pagoService.getPagosCompraEntreFechasYFormaDePago(idEmpresa, idFormaDePago, fechaDesde, fechaHasta);
    }
 
}
