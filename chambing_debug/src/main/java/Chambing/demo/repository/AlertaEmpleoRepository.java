package Chambing.demo.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import Chambing.demo.Model.AlertaEmpleo;

public interface AlertaEmpleoRepository extends MongoRepository<AlertaEmpleo, String> {
    List<AlertaEmpleo> findByUsuarioId(String usuarioId);
}
