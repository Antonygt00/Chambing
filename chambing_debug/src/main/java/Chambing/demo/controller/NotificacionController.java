package Chambing.demo.controller;

import Chambing.demo.Model.Notificacion;
import Chambing.demo.service.AccesoSeleccion;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {
    private final MongoTemplate mongo;
    private final AccesoSeleccion acceso;
    public NotificacionController(MongoTemplate mongo, AccesoSeleccion acceso) {
        this.mongo = mongo; this.acceso = acceso;
    }
    @GetMapping public Object listar(@AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new ResponseStatusException(BAD_REQUEST, "Paginación no válida");
        String destinatario = acceso.destinatario(principal);
        Query q = Query.query(Criteria.where("destinatario").is(destinatario));
        long total = mongo.count(q, Notificacion.class);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id")));
        return new PageImpl<>(mongo.find(q.with(pageable), Notificacion.class), pageable, total);
    }
    @GetMapping("/pendientes") public Object pendientes(@AuthenticationPrincipal UserDetails principal) {
        long total = mongo.count(Query.query(Criteria.where("destinatario")
            .is(acceso.destinatario(principal)).and("leida").is(false)), Notificacion.class);
        return Map.of("total", total);
    }
    @PutMapping("/{id}/leida") public void leer(@PathVariable String id,
            @AuthenticationPrincipal UserDetails principal) {
        var r = mongo.updateFirst(Query.query(Criteria.where("_id").is(id)
            .and("destinatario").is(acceso.destinatario(principal))),
            new Update().set("leida", true), Notificacion.class);
        if (r.getMatchedCount() == 0) throw new ResponseStatusException(NOT_FOUND, "Notificación no encontrada");
    }
    @PutMapping("/leidas") public void leerTodas(@AuthenticationPrincipal UserDetails principal) {
        mongo.updateMulti(Query.query(Criteria.where("destinatario").is(acceso.destinatario(principal))
            .and("leida").is(false)), new Update().set("leida", true), Notificacion.class);
    }
}
