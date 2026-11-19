package Chambing.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import Chambing.demo.Model.Favorito;

public interface FavoritoRepository extends MongoRepository<Favorito, String> {
    List<Favorito> findByUsuarioId(String usuarioId);
    Optional<Favorito> findByUsuarioIdAndPropuestaId(String usuarioId, String propuestaId);
    void deleteByUsuarioIdAndPropuestaId(String usuarioId, String propuestaId);
}
