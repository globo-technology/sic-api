package sic.service;

import java.util.Date;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import sic.modelo.Cliente;
import sic.modelo.Empresa;
import sic.modelo.FacturaVenta;
import sic.modelo.FormaDePago;
import sic.modelo.Pago;
import sic.modelo.Recibo;
import sic.modelo.Usuario;

public interface IReciboService {
    
    Recibo getById(long idRecibo);
    
    Double getMontoById(long idRecibo);
    
    Recibo guardar(Recibo recibo);
    
    Recibo guardarReciboDePago(Pago pago);
    
    long getSiguienteNumeroRecibo(long idEmpresa);
    
    void eliminar(long idRecibo);
    
    List<Recibo> getByClienteAndEmpresaAndEliminado(Cliente cliente, Empresa empresa, boolean eliminado);
    
    List<Recibo> getByUsuarioAndEmpresaAndEliminado(Usuario usuario, Empresa empresa, boolean eliminado);
    
    Page<Recibo> getByFechaBetweenAndClienteAndEmpresaAndEliminado(Date desde, Date hasta, Cliente cliente, Empresa empresa, boolean eliminado, Pageable page);
    
    double pagarMultiplesFacturas(List<FacturaVenta> facturasVenta, Recibo recibo, double monto, FormaDePago formaDePago, String nota);
    
}
