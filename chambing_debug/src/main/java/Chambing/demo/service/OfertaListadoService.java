package Chambing.demo.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import Chambing.demo.Model.Propuesta;

@Service
public class OfertaListadoService {

    private final MongoTemplate mongo;

    public OfertaListadoService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /**
     * Una sola página. Mongo aplica el filtro, el salto y el límite:
     * el navegador no recibe el catálogo completo.
     */
    public Map<String, Object> pagina(String keyword, String ubicacion, Double salarioMin, Double salarioMax,
                                      String categoria, String modalidad, String tipoContrato,
                                      String nivelExperiencia, int numero, int tamano) {
        int size = Math.min(Math.max(tamano, 1), 20);
        int page = Math.max(numero, 0);
        LocalDateTime ahora = LocalDateTime.now();
        mongo.updateMulti(Query.query(new Criteria().andOperator(
                Criteria.where("destacada").is(true),
                new Criteria().orOperator(
                        Criteria.where("fechaFinDestacada").lt(ahora),
                        Criteria.where("fechaFinDestacada").is(null)))),
                new Update().set("destacada", false), Propuesta.class);

        Criteria filtro = filtros(keyword, ubicacion, salarioMin, salarioMax, categoria, modalidad, tipoContrato, nivelExperiencia);
        List<Propuesta> destacadasVigentes = mongo.find(new Query(new Criteria().andOperator(
                filtro, Criteria.where("destacada").is(true), Criteria.where("fechaFinDestacada").gt(ahora))),
                Propuesta.class);
        if (!destacadasVigentes.isEmpty()) {
            int rotacion = (int) (System.currentTimeMillis() / 21_600_000L);
            Collections.rotate(destacadasVigentes, -Math.floorMod(rotacion, destacadasVigentes.size()));
        }
        List<String> idsDestacadas = destacadasVigentes.stream().map(Propuesta::getIdPropuesta).toList();
        Criteria organicas = idsDestacadas.isEmpty()
                ? filtro
                : new Criteria().andOperator(filtro, Criteria.where("_id").nin(idsDestacadas));
        long organicasTotal = mongo.count(Query.query(organicas), Propuesta.class);
        long total = destacadasVigentes.size() + organicasTotal;
        int totalPages = total == 0 ? 0 : (int) Math.ceil(total / (double) size);

        int indiceDestacada = 0;
        int saltoOrganicas = 0;
        int restantes = destacadasVigentes.size();
        for (int i = 0; i < page; i++) {
            int cupoPrevio = Math.min(OrdenDestacadas.MAX_POR_PAGINA, restantes);
            restantes -= cupoPrevio;
            indiceDestacada += cupoPrevio;
            saltoOrganicas += size - cupoPrevio;
        }
        int cupo = page >= totalPages ? 0 : Math.min(OrdenDestacadas.MAX_POR_PAGINA, destacadasVigentes.size() - indiceDestacada);
        List<Propuesta> content = new ArrayList<>();
        if (cupo > 0) {
            content.addAll(destacadasVigentes.subList(indiceDestacada, indiceDestacada + cupo));
        }
        if (page < totalPages && size - cupo > 0) {
            Query hoja = Query.query(organicas)
                    .with(Sort.by(Sort.Direction.DESC, "fechaPublicacion"))
                    .skip(saltoOrganicas)
                    .limit(size - cupo);
            content.addAll(mongo.find(hoja, Propuesta.class));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("content", content);
        out.put("totalElements", total);
        out.put("totalPages", totalPages);
        out.put("number", page);
        out.put("size", size);
        return out;
    }

    private Criteria filtros(String keyword, String ubicacion, Double salarioMin, Double salarioMax,
                             String categoria, String modalidad, String tipoContrato, String nivel) {
        List<Criteria> partes = new ArrayList<>();
        partes.add(new Criteria().orOperator(
                Criteria.where("estado").regex("^\\s*activa\\s*$", "i"),
                Criteria.where("estado").exists(false),
                Criteria.where("estado").is(null),
                Criteria.where("estado").is("")));
        if (keyword != null && !keyword.isBlank()) {
            String q = Pattern.quote(keyword.trim());
            partes.add(new Criteria().orOperator(
                    Criteria.where("titulo").regex(q, "i"),
                    Criteria.where("categoria").regex(q, "i"),
                    Criteria.where("empresaNombre").regex(q, "i"),
                    Criteria.where("ciudad").regex(q, "i")));
        }
        if (ubicacion != null && !ubicacion.isBlank()) {
            partes.add(Criteria.where("ciudad").regex(Pattern.quote(ubicacion.trim()), "i"));
        }
        if (salarioMin != null) partes.add(Criteria.where("salario").gte(salarioMin));
        if (salarioMax != null) partes.add(Criteria.where("salario").lte(salarioMax));
        if (categoria != null && !categoria.isBlank()) {
            partes.add(Criteria.where("categoria").regex(Pattern.quote(categoria.trim()), "i"));
        }
        if (modalidad != null && !modalidad.isBlank()) {
            partes.add(Criteria.where("modalidad").regex("^" + Pattern.quote(modalidad.trim()) + "$", "i"));
        }
        if (tipoContrato != null && !tipoContrato.isBlank()) {
            partes.add(Criteria.where("tipoContrato").regex("^" + Pattern.quote(tipoContrato.trim()) + "$", "i"));
        }
        if (nivel != null && !nivel.isBlank()) {
            partes.add(Criteria.where("nivelExperiencia").regex("^" + Pattern.quote(nivel.trim()) + "$", "i"));
        }
        return new Criteria().andOperator(partes);
    }

    /** Crea el índice de texto si todavía no está. */
    public void asegurarIndiceTexto() {
        boolean existe = mongo.indexOps(Propuesta.class).getIndexInfo().stream()
                .anyMatch(i -> i.getName() != null && i.getName().contains("texto"));
        if (existe) return;
        mongo.indexOps(Propuesta.class).ensureIndex(
                new org.springframework.data.mongodb.core.index.TextIndexDefinition.TextIndexDefinitionBuilder()
                        .onField("titulo", 3F)
                        .onField("descripcion", 2F)
                        .onField("requisitos", 1F)
                        .onField("categoria", 1F)
                        .named("propuesta_texto")
                        .build());
    }
}
