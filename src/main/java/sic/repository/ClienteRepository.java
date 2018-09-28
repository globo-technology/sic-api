package sic.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import sic.modelo.Cliente;
import sic.modelo.Empresa;

public interface ClienteRepository
    extends PagingAndSortingRepository<Cliente, Long>, QueryDslPredicateExecutor<Cliente> {

  @Query("SELECT c FROM Cliente c WHERE c.id_Cliente = :idCliente AND c.eliminado = false")
  Cliente findById(@Param("idCliente") long idCliente);

  Cliente findByIdFiscalAndEmpresaAndEliminado(Long idFiscal, Empresa empresa, boolean eliminado);

  Cliente findByRazonSocialAndEmpresaAndEliminado(
      String razonSocial, Empresa empresa, boolean eliminado);

  Cliente findByEmpresaAndPredeterminadoAndEliminado(
      Empresa empresa, boolean predeterminado, boolean eliminado);

  boolean existsByEmpresaAndPredeterminadoAndEliminado(
      Empresa empresa, boolean predeterminado, boolean eliminado);

  @Query(
      "SELECT c FROM Pedido p INNER JOIN p.cliente c WHERE p.id_Pedido = :idPedido AND c.eliminado = false")
  Cliente findClienteByIdPedido(@Param("idPedido") long idPedido);

  @Query(
      "SELECT c FROM Cliente c WHERE c.credencial.id_Usuario = :idUsuario " +
        "AND c.empresa.id_Empresa = :idEmpresa AND c.eliminado = false")
  Cliente findClienteByIdUsuarioYidEmpresa(
      @Param("idUsuario") long idUsuario, @Param("idEmpresa") long idEmpresa);

  @Modifying
  @Query("UPDATE Cliente c SET c.viajante = null WHERE c.viajante.id_Usuario = :idUsuarioViajante")
  int desvincularClienteDeViajante(@Param("idUsuarioViajante") long idUsuarioViajante);

  @Modifying
  @Query(
      "UPDATE Cliente c SET c.credencial = null WHERE c.credencial.id_Usuario = :idUsuarioCredencial")
  int desvincularClienteDeCredencial(@Param("idUsuarioCredencial") long idUsuarioCredencial);

  Cliente findByNroClienteAndEmpresaAndEliminado(
      String nroCliente, Empresa empresa, boolean eliminado);
}
