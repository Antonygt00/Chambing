package Chambing.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import Chambing.demo.Model.PagoDestacado;

public interface PagoDestacadoRepository extends MongoRepository<PagoDestacado, String> {
    Optional<PagoDestacado> findByReferencia(String referencia);
    List<PagoDestacado> findByEstado(String estado);
    List<PagoDestacado> findByEmpresaId(String empresaId);
}
