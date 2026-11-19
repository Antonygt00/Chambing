package Chambing.demo.service;

import Chambing.demo.Model.Postulacion;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Component;

/** Idempotente: no cambia resultados ni inventa eventos históricos. */
@Component
public class MigracionSeleccion implements ApplicationRunner {
    private final MongoTemplate mongo;
    public MigracionSeleccion(MongoTemplate mongo) { this.mongo = mongo; }
    @Override public void run(ApplicationArguments args) {
        mongo.updateMulti(Query.query(Criteria.where("version").is(null)),
            new Update().set("version", 0L), Postulacion.class);
        mongo.updateMulti(Query.query(Criteria.where("faseActual").is(null)
                .and("estadoPostulacion").in("Aprobado", "Aprobada", "aprobado", "aprobada")),
            new Update().set("faseActual", "SELECCIONADO"), Postulacion.class);
        mongo.updateMulti(Query.query(Criteria.where("faseActual").is(null)),
            new Update().set("faseActual", "INSCRIPCION"), Postulacion.class);
    }
}
