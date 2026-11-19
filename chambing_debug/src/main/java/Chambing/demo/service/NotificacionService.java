package Chambing.demo.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.bson.Document;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import Chambing.demo.Model.Notificacion;
import Chambing.demo.Model.Postulacion;

@Service
public class NotificacionService {
    private final MongoTemplate mongo;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CorreoService correo;

    public NotificacionService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public void agregar(Postulacion p, String destinatario, String mensaje, String enlace, String tipo) {
        p.getNotificacionesPendientes().add(new Notificacion(
                UUID.randomUUID().toString(),
                destinatario,
                mensaje,
                enlace,
                p.getIdPostulacion(),
                LocalDateTime.now(),
                false,
                tipo
        ));
    }

    /** Reintento durable e idempotente; no sobreescribe la marca de lectura. */
    @Scheduled(fixedDelay = 15000)
    public void entregarPendientes() {
        Query q = Query.query(Criteria.where("notificacionesPendientes.0").exists(true)).limit(50);
        try {
            for (Postulacion p : mongo.find(q, Postulacion.class)) {
                for (Notificacion n : p.getNotificacionesPendientes()) {
                    mongo.upsert(Query.query(Criteria.where("_id").is(n.id())), new Update()
                            .setOnInsert("destinatario", n.destinatario())
                            .setOnInsert("mensaje", n.mensaje())
                            .setOnInsert("enlace", n.enlace())
                            .setOnInsert("postulacionId", n.postulacionId())
                            .setOnInsert("fecha", n.fecha())
                            .setOnInsert("leida", false)
                            .setOnInsert("tipo", n.tipo()),
                            Notificacion.class);

                    if (correo != null) {
                        correo.avisarHtml(n, p);   // ← envía HTML según el tipo
                    }

                    Object id = org.bson.types.ObjectId.isValid(p.getIdPostulacion())
                            ? new Document("$in", java.util.List.of(p.getIdPostulacion(), new org.bson.types.ObjectId(p.getIdPostulacion())))
                            : p.getIdPostulacion();
                    mongo.getCollection("postulaciones").updateOne(
                            new Document("_id", id),
                            new Document("$pull", new Document("notificacionesPendientes", new Document("_id", n.id())))
                    );
                }
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).warn("Se reintentará la entrega de notificaciones", e);
        }
    }
}