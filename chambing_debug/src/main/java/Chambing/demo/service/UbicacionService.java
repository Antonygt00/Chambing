package Chambing.demo.service;

import Chambing.demo.Model.*;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
public class UbicacionService {
    private final MongoTemplate mongo;
    public UbicacionService(MongoTemplate mongo) { this.mongo = mongo; }
    public static String clave(String nombre) {
        String valor = Normalizer.normalize(Objects.toString(nombre, "").trim(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        return valor.matches("bogota d\\.?c\\.?") ? "bogota" : valor;
    }
    private String nombre(String valor) {
        String n = Objects.toString(valor, "").trim().replaceAll("\\s+", " ");
        if (n.isEmpty() || n.length() > 100 || n.chars().anyMatch(Character::isISOControl))
            throw new ResponseStatusException(BAD_REQUEST, "El nombre debe tener entre 1 y 100 caracteres");
        return n;
    }
    public Departamento departamento(String id) {
        Departamento d = id == null ? null : mongo.findById(id, Departamento.class);
        if (d == null) throw new ResponseStatusException(NOT_FOUND, "Departamento no encontrado");
        return d;
    }
    public Ciudad ciudad(String id) {
        Ciudad c = id == null ? null : mongo.findById(id, Ciudad.class);
        if (c == null) throw new ResponseStatusException(NOT_FOUND, "Ciudad no encontrada");
        return c;
    }
    public List<Departamento> departamentos(boolean todos) {
        Query q = todos ? new Query() : Query.query(Criteria.where("activo").is(true));
        return mongo.find(q.with(Sort.by("nombre")), Departamento.class);
    }
    public List<Ciudad> ciudades(String depId, boolean todos) {
        Departamento d = departamento(depId);
        if (!todos && !d.activo()) return List.of();
        Query q = Query.query(Criteria.where("departamentoId").is(depId));
        if (!todos) q.addCriteria(Criteria.where("activo").is(true));
        return mongo.find(q.with(Sort.by("nombre")), Ciudad.class);
    }
    public Object pagina(boolean ciudades, int page, int size, String texto, String depId) {
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(BAD_REQUEST, "Paginación no válida");
        Query q = new Query();
        if (texto != null && !texto.isBlank())
            q.addCriteria(Criteria.where("clave").regex(Pattern.quote(clave(texto)), "i"));
        if (ciudades && depId != null && !depId.isBlank()) q.addCriteria(Criteria.where("departamentoId").is(depId));
        var pageable = PageRequest.of(page, size, Sort.by("nombre").and(Sort.by("id")));
        if (!ciudades) {
            long total = mongo.count(q, Departamento.class);
            return new PageImpl<>(mongo.find(q.with(pageable), Departamento.class), pageable, total);
        }
        long total = mongo.count(q, Ciudad.class);
        Map<String, String> nombres = new HashMap<>();
        departamentos(true).forEach(d -> nombres.put(d.id(), d.nombre()));
        var items = mongo.find(q.with(pageable), Ciudad.class).stream().map(c -> Map.of(
            "id", c.id(), "nombre", c.nombre(), "departamentoId", c.departamentoId(),
            "departamentoNombre", nombres.getOrDefault(c.departamentoId(), "Departamento eliminado"), "activo", c.activo())).toList();
        return new PageImpl<>(items, pageable, total);
    }
    public Departamento guardarDepartamento(String id, String valor, Boolean activo) {
        Departamento previo = id == null ? null : departamento(id);
        String n = nombre(valor);
        Query duplicado = Query.query(Criteria.where("clave").is(clave(n)));
        if (id != null) duplicado.addCriteria(Criteria.where("_id").ne(id));
        if (mongo.exists(duplicado, Departamento.class)) throw new ResponseStatusException(CONFLICT, "Ya existe ese departamento");
        Departamento d = mongo.save(new Departamento(id == null ? UUID.randomUUID().toString() : id,
            n, clave(n), activo == null ? previo == null || previo.activo() : activo));
        if (previo != null && !previo.nombre().equals(n)) actualizarDirecciones("departamentoId", id, new Update().set("direccion.departamento", n));
        return d;
    }
    public Ciudad guardarCiudad(String id, String valor, String departamentoId, Boolean activo) {
        Ciudad previo = id == null ? null : ciudad(id);
        Departamento d = departamento(departamentoId);
        String n = nombre(valor);
        Query duplicado = Query.query(Criteria.where("departamentoId").is(d.id()).and("clave").is(clave(n)));
        if (id != null) duplicado.addCriteria(Criteria.where("_id").ne(id));
        if (mongo.exists(duplicado, Ciudad.class)) throw new ResponseStatusException(CONFLICT, "Ya existe esa ciudad en el departamento");
        Ciudad c = mongo.save(new Ciudad(id == null ? UUID.randomUUID().toString() : id, n, clave(n), d.id(),
            activo == null ? previo == null || previo.activo() : activo));
        if (previo != null) actualizarDirecciones("ciudadId", id, new Update().set("direccion.ciudad", n)
            .set("direccion.departamentoId", d.id()).set("direccion.departamento", d.nombre()));
        return c;
    }
    private void actualizarDirecciones(String campo, String id, Update cambio) {
        for (String coleccion : List.of("usuarios", "empresas"))
            mongo.updateMulti(Query.query(Criteria.where("direccion." + campo).is(id)), cambio, coleccion);
    }
    public void eliminarDepartamento(String id) {
        departamento(id);
        if (mongo.exists(Query.query(Criteria.where("departamentoId").is(id)), Ciudad.class) || usado("departamentoId", id))
            throw new ResponseStatusException(CONFLICT, "El departamento tiene ciudades o direcciones asociadas. Puedes desactivarlo");
        mongo.remove(Query.query(Criteria.where("_id").is(id)), Departamento.class);
    }
    public void eliminarCiudad(String id) {
        ciudad(id);
        if (usado("ciudadId", id)) throw new ResponseStatusException(CONFLICT, "La ciudad tiene direcciones asociadas. Puedes desactivarla");
        mongo.remove(Query.query(Criteria.where("_id").is(id)), Ciudad.class);
    }
    private boolean usado(String campo, String id) {
        Query q = Query.query(Criteria.where("direccion." + campo).is(id));
        return mongo.exists(q, "usuarios") || mongo.exists(q, "empresas");
    }
    /** Acepta los nombres del cliente antiguo; las nuevas vistas envían también IDs. */
    public Direccion validar(Direccion dir, Direccion anterior) {
        if (dir == null) return null;
        if (vacio(dir.getDepartamentoId()) && vacio(dir.getCiudadId()) && vacio(dir.getDepartamento()) && vacio(dir.getCiudad())) return dir;
        Departamento d = vacio(dir.getDepartamentoId())
            ? mongo.findOne(Query.query(Criteria.where("clave").is(clave(dir.getDepartamento()))), Departamento.class)
            : departamento(dir.getDepartamentoId());
        if (d == null) throw new ResponseStatusException(BAD_REQUEST, "Selecciona un departamento del catálogo");
        Ciudad c = vacio(dir.getCiudadId())
            ? mongo.findOne(Query.query(Criteria.where("departamentoId").is(d.id()).and("clave").is(clave(dir.getCiudad()))), Ciudad.class)
            : ciudad(dir.getCiudadId());
        if (c == null || !c.departamentoId().equals(d.id()))
            throw new ResponseStatusException(BAD_REQUEST, "La ciudad debe pertenecer al departamento seleccionado");
        boolean misma = anterior != null && d.id().equals(anterior.getDepartamentoId()) && c.id().equals(anterior.getCiudadId());
        if ((!d.activo() || !c.activo()) && !misma)
            throw new ResponseStatusException(BAD_REQUEST, "La ubicación está inactiva. Selecciona otra");
        dir.setDepartamentoId(d.id()); dir.setCiudadId(c.id()); dir.setDepartamento(d.nombre()); dir.setCiudad(c.nombre());
        return dir;
    }
    private boolean vacio(String valor) { return valor == null || valor.isBlank(); }
}
