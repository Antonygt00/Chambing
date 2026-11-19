package Chambing.demo.service;

import Chambing.demo.Model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.bson.Document;
import org.springframework.boot.*;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Component;

/** Una sola carga inicial: no repone ubicaciones eliminadas por el administrador. */
@Component
@Order(20)
public class MigracionUbicaciones implements ApplicationRunner {
    private final MongoTemplate mongo;
    private final ObjectMapper mapper;
    public MigracionUbicaciones(MongoTemplate mongo, ObjectMapper mapper) { this.mongo = mongo; this.mapper = mapper; }
    @Override public void run(ApplicationArguments args) throws Exception {
        if (mongo.exists(Query.query(Criteria.where("_id").is("ubicaciones_v1")), "migraciones_catalogos")) return;
        if (!mongo.collectionExists(Departamento.class)) mongo.createCollection(Departamento.class);
        if (!mongo.collectionExists(Ciudad.class)) mongo.createCollection(Ciudad.class);
        mongo.indexOps(Departamento.class).ensureIndex(new Index().on("clave", Sort.Direction.ASC).unique().named("departamento_clave"));
        mongo.indexOps(Ciudad.class).ensureIndex(new Index().on("departamentoId", Sort.Direction.ASC)
            .on("clave", Sort.Direction.ASC).unique().named("ciudad_por_departamento"));
        Map<String, List<String>> base;
        try (var stream = new ClassPathResource("data/ubicaciones-iniciales.json").getInputStream()) {
            base = mapper.readValue(stream, new TypeReference<Map<String, List<String>>>() {});
        }
        for (var item : base.entrySet()) {
            Departamento d = departamento(item.getKey());
            for (String nombre : item.getValue()) ciudad(d.id(), nombre);
        }
        // Agrupar evita cargar todos los perfiles, que pueden ser muchos.
        for (String coleccion : List.of("usuarios", "empresas")) {
            var grupos = mongo.getCollection(coleccion).aggregate(List.of(new Document("$group",
                new Document("_id", new Document("departamento", "$direccion.departamento").append("ciudad", "$direccion.ciudad")))));
            for (Document grupo : grupos) {
                Document direccion = grupo.get("_id", Document.class);
                if (!(direccion.get("departamento") instanceof String dep) || !valido(dep)) continue;
                Departamento d = departamento(dep.trim());
                Update update = new Update().set("direccion.departamentoId", d.id()).set("direccion.departamento", d.nombre());
                Criteria criteria = Criteria.where("direccion.departamento").is(dep)
                    .and("direccion.ciudad").is(direccion.get("ciudad"));
                if (direccion.get("ciudad") instanceof String nombre && valido(nombre)) {
                    Ciudad c = ciudad(d.id(), nombre.trim());
                    update.set("direccion.ciudadId", c.id()).set("direccion.ciudad", c.nombre());
                }
                mongo.updateMulti(Query.query(criteria), update, coleccion);
            }
        }
        mongo.upsert(Query.query(Criteria.where("_id").is("ubicaciones_v1")),
            new Update().set("completada", true).set("fecha", new Date()), "migraciones_catalogos");
    }
    private boolean valido(String texto) { return !texto.isBlank() && texto.trim().length() <= 100 && texto.chars().noneMatch(Character::isISOControl); }
    private Departamento departamento(String nombre) {
        Query q = Query.query(Criteria.where("clave").is(UbicacionService.clave(nombre)));
        mongo.upsert(q, new Update().setOnInsert("_id", UUID.randomUUID().toString()).setOnInsert("nombre", nombre)
            .setOnInsert("activo", true), Departamento.class);
        return mongo.findOne(q, Departamento.class);
    }
    private Ciudad ciudad(String departamentoId, String nombre) {
        Query q = Query.query(Criteria.where("departamentoId").is(departamentoId).and("clave").is(UbicacionService.clave(nombre)));
        mongo.upsert(q, new Update().setOnInsert("_id", UUID.randomUUID().toString()).setOnInsert("nombre", nombre)
            .setOnInsert("activo", true), Ciudad.class);
        return mongo.findOne(q, Ciudad.class);
    }
}
