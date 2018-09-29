package sic.builder;

import java.math.BigDecimal;
import java.util.Date;

import sic.modelo.CategoriaIVA;
import sic.modelo.Empresa;
import sic.modelo.Localidad;
import sic.modelo.Proveedor;

public class ProveedorBuilder {

  private long id_Proveedor = 0L;
  private String codigo = "ABC123";
  private String razonSocial = "Chamaco S.R.L.";
  private String direccion = "La Rioja 2047";
  private CategoriaIVA categoriaIVA = CategoriaIVA.RESPONSABLE_INSCRIPTO;
  private Long idFiscal = 23127895679L;
  private String telPrimario = "379 4356778";
  private String telSecundario = "379 4894514";
  private String contacto = "Raul Gamez";
  private String email = "chamacosrl@gmail.com";
  private String web = "www.chamacosrl.com.ar";
  private Localidad localidad = new LocalidadBuilder().build();
  private Empresa empresa = new EmpresaBuilder().build();
  private boolean eliminado = false;
  private BigDecimal saldoCuentaCorriente = BigDecimal.ZERO;
  private Date fechaUltimoMovimiento = null;

  public Proveedor build() {
    return new Proveedor(
        id_Proveedor,
        codigo,
        razonSocial,
        direccion,
        categoriaIVA,
        idFiscal,
        telPrimario,
        telSecundario,
        contacto,
        email,
        web,
        localidad,
        empresa,
        eliminado,
        saldoCuentaCorriente,
        fechaUltimoMovimiento);
  }

  public ProveedorBuilder withId_Proveedor(long id_Proveedor) {
    this.id_Proveedor = id_Proveedor;
    return this;
  }

  public ProveedorBuilder withCodigo(String codigo) {
    this.codigo = codigo;
    return this;
  }

  public ProveedorBuilder withRazonSocial(String razonSocial) {
    this.razonSocial = razonSocial;
    return this;
  }

  public ProveedorBuilder withDireccion(String direccion) {
    this.direccion = direccion;
    return this;
  }

  public ProveedorBuilder withCategoriaIVA(CategoriaIVA categoriaIVA) {
    this.categoriaIVA = categoriaIVA;
    return this;
  }

  public ProveedorBuilder withIdFiscal(Long idFiscal) {
    this.idFiscal = idFiscal;
    return this;
  }

  public ProveedorBuilder withTelPrimario(String telPrimario) {
    this.telPrimario = telPrimario;
    return this;
  }

  public ProveedorBuilder withTelSecundario(String telSecundario) {
    this.telSecundario = telSecundario;
    return this;
  }

  public ProveedorBuilder withContacto(String contacto) {
    this.contacto = contacto;
    return this;
  }

  public ProveedorBuilder withEmail(String email) {
    this.email = email;
    return this;
  }

  public ProveedorBuilder withWeb(String web) {
    this.web = web;
    return this;
  }

  public ProveedorBuilder withLocalidad(Localidad localidad) {
    this.localidad = localidad;
    return this;
  }

  public ProveedorBuilder withEmpresa(Empresa empresa) {
    this.empresa = empresa;
    return this;
  }

  public ProveedorBuilder withEliminado(boolean eliminado) {
    this.eliminado = eliminado;
    return this;
  }

  public ProveedorBuilder withSaldoCuentaCorriente(BigDecimal saldoCuentaCorriente) {
    this.saldoCuentaCorriente = saldoCuentaCorriente;
    return this;
  }

  public ProveedorBuilder withFechaUltimoMovimiento(Date fechaUltimoMovimiento) {
    this.fechaUltimoMovimiento = fechaUltimoMovimiento;
    return this;
  }
}
