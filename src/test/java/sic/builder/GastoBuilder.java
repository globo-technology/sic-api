package sic.builder;

import java.math.BigDecimal;
import sic.modelo.Sucursal;
import java.time.LocalDateTime;
import sic.modelo.FormaDePago;
import sic.modelo.Gasto;
import sic.modelo.Usuario;

public class GastoBuilder {

    private long idGasto = 0L;
    private long nroGasto = 34;
    private LocalDateTime fecha = LocalDateTime.now();
    private String concepto = "Gasto Factura de Luz - Builder";
    private Sucursal sucursal = new SucursalBuilder().build();
    private Usuario usuario = new UsuarioBuilder().build();
    private FormaDePago formaDePago = new FormaDePagoBuilder().build();
    private BigDecimal monto = new BigDecimal("245.33");
    private boolean eliminado = false;

    public Gasto build() {
        return new Gasto(idGasto, nroGasto, fecha, concepto, sucursal, usuario, formaDePago, monto, eliminado);
    }

    public GastoBuilder withIdGasto(long idGasto) {
        this.idGasto = idGasto;
        return this;
    }

    public GastoBuilder withNroGasto(long nroGasto) {
        this.nroGasto = nroGasto;
        return this;
    }

    public GastoBuilder withFecha(LocalDateTime fecha) {
        this.fecha = fecha;
        return this;
    }

    public GastoBuilder withConcepto(String concepto) {
        this.concepto = concepto;
        return this;
    }

    public GastoBuilder withEmpresa(Sucursal sucursal) {
        this.sucursal = sucursal;
        return this;
    }

    public GastoBuilder withUsuario(Usuario usuario) {
        this.usuario = usuario;
        return this;
    }

    public GastoBuilder withFormaDePago(FormaDePago formaDePago) {
        this.formaDePago = formaDePago;
        return this;
    }

    public GastoBuilder withMonto(BigDecimal monto) {
        this.monto = monto;
        return this;
    }

    public GastoBuilder withEliminado(boolean eliminado) {
        this.eliminado = eliminado;
        return this;
    }

}
