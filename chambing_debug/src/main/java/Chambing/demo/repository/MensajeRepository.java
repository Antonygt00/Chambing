package Chambing.demo.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import Chambing.demo.Model.Mensaje;

public interface MensajeRepository extends MongoRepository<Mensaje, String> {
    List<Mensaje> findByDeOrParaOrderByFechaAsc(String de, String para);
}
