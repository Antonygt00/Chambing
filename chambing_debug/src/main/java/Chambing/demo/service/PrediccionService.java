package Chambing.demo.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import Chambing.demo.Model.Educacion;
import Chambing.demo.Model.Empresa;
import Chambing.demo.Model.Habilidad;
import Chambing.demo.Model.Postulacion;
import Chambing.demo.Model.Propuesta;
import Chambing.demo.Model.Usuario;
import Chambing.demo.repository.EmpresaRepository;
import Chambing.demo.repository.PostulacionRepository;
import Chambing.demo.repository.PropuestaRepository;
import Chambing.demo.repository.UsuarioRepository;

@Service
public class PrediccionService {

    private static final Logger log = LoggerFactory.getLogger(PrediccionService.class);
    private final RestClient http;
    private final UsuarioRepository usuarios;
    private final EmpresaRepository empresas;
    private final PropuestaRepository propuestas;
    private final PostulacionRepository postulaciones;

    private final OfertaListadoService listado;

    public PrediccionService(
            @Value("${prediccion.service.url:http://localhost:8001}") String url,
            UsuarioRepository usuarios, EmpresaRepository empresas,
            PropuestaRepository propuestas, PostulacionRepository postulaciones,
            OfertaListadoService listado) {
        SimpleClientHttpRequestFactory http = new SimpleClientHttpRequestFactory();
        http.setConnectTimeout(Duration.ofMillis(800));
        http.setReadTimeout(Duration.ofMillis(1500));
        this.http = RestClient.builder().baseUrl(url).requestFactory(http).build();
        this.usuarios = usuarios;
        this.empresas = empresas;
        this.propuestas = propuestas;
        this.postulaciones = postulaciones;
        this.listado = listado;
    }

    public Map<String, Object> paraUsuario(Usuario usuario, String idPropuesta) {
        Propuesta propuesta = propuestas.findById(idPropuesta)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Propuesta no encontrada"));
        return predecir(usuario, propuesta, postulaciones.findByUsuarioId(usuario.getIdUsuario()));
    }

    public List<Map<String, Object>> recomendadas(Usuario usuario, int limite) {
        List<Postulacion> historial = postulaciones.findByUsuarioId(usuario.getIdUsuario());
        @SuppressWarnings("unchecked")
        List<Propuesta> activas = (List<Propuesta>) listado.pagina(
                null, null, null, null, null, null, null, null, 0, Math.max(limite, 8)).get("content");
        List<Map<String, Object>> features = new ArrayList<>();
        for (Propuesta p : activas) features.add(features(usuario, p, historial));
        List<Map<String, Object>> preds = lote(features);
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < activas.size(); i++) {
            Propuesta p = activas.get(i);
            Map<String, Object> pred = i < preds.size() ? preds.get(i) : heuristica(features.get(i));
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("idPropuesta", p.getIdPropuesta());
            fila.put("titulo", p.getTitulo());
            fila.put("empresaNombre", p.getEmpresaNombre());
            fila.put("ciudad", p.getCiudad());
            fila.put("salario", p.getSalario());
            fila.put("destacada", p.isDestacada());
            fila.put("probabilidad", pred.get("probabilidad"));
            fila.put("etiqueta", pred.get("etiqueta"));
            items.add(fila);
        }
        items.sort(Comparator.comparingDouble((Map<String, Object> m) -> numero(m.get("probabilidad"))).reversed());
        return items.stream().limit(limite).toList();
    }

    public void enriquecer(List<Map<String, Object>> vistas, Propuesta propuesta) {
        List<Map<String, Object>> features = new ArrayList<>();
        for (Map<String, Object> vista : vistas) {
            String usuarioId = String.valueOf(vista.get("usuarioId"));
            Usuario usuario = usuarios.findById(usuarioId).orElse(null);
            features.add(usuario == null
                    ? featuresMinimas(propuesta)
                    : features(usuario, propuesta, postulaciones.findByUsuarioId(usuarioId)));
        }
        List<Map<String, Object>> preds = lote(features);
        for (int i = 0; i < vistas.size(); i++) {
            Map<String, Object> pred = i < preds.size() ? preds.get(i) : heuristica(features.get(i));
            vistas.get(i).put("probabilidad", pred.get("probabilidad"));
            vistas.get(i).put("etiqueta", pred.get("etiqueta"));
        }
    }

    private Map<String, Object> predecir(Usuario usuario, Propuesta propuesta, List<Postulacion> historial) {
        Map<String, Object> feature = features(usuario, propuesta, historial);
        List<Map<String, Object>> lote = lote(List.of(feature));
        return lote.isEmpty() ? heuristica(feature) : lote.get(0);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> lote(List<Map<String, Object>> features) {
        List<Map<String, Object>> utiles = features.stream().filter(f -> f.get("edad") != null).toList();
        if (utiles.isEmpty()) {
            return features.stream().map(this::heuristica).toList();
        }
        try {
            Map<String, Object> body = Map.of("items", features);
            Map<String, Object> respuesta = http.post().uri("/predict/batch")
                    .body(body).retrieve().body(Map.class);
            if (respuesta != null && respuesta.get("resultados") instanceof List<?> lista) {
                return (List<Map<String, Object>>) lista;
            }
        } catch (Exception e) {
            log.info("Microservicio de predicción no disponible, se usa la heurística local: {}", e.getMessage());
        }
        return features.stream().map(this::heuristica).toList();
    }

    Map<String, Object> features(Usuario usuario, Propuesta propuesta, List<Postulacion> historial) {
        long previas = historial.stream().filter(p -> !propuesta.getIdPropuesta().equals(p.getPropuestaId())).count();
        long aprobadas = historial.stream()
                .filter(p -> !propuesta.getIdPropuesta().equals(p.getPropuestaId()))
                .filter(p -> "Aprobado".equalsIgnoreCase(p.getEstadoPostulacion()))
                .count();
        double pct = previas == 0 ? 0.35 : (double) aprobadas / previas;
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("edad", usuario.getEdad() == null ? 28 : usuario.getEdad());
        f.put("misma_ciudad", mismaCiudad(usuario, propuesta) ? 1 : 0);
        f.put("salario", propuesta.getSalario() == null ? 0 : propuesta.getSalario());
        f.put("cantidad_requisitos", contarRequisitos(propuesta));
        f.put("num_postulantes", postulaciones.countByPropuestaId(propuesta.getIdPropuesta()));
        f.put("num_postulaciones_previas", previas);
        f.put("pct_aprobadas", Math.round(pct * 1000d) / 1000d);
        f.put("anios_experiencia", anios(usuario));
        f.put("nivel_educativo", nivel(usuario));
        f.put("skills_coincidentes", skills(usuario, propuesta));
        f.put("categoria", propuesta.getCategoria() == null ? "General" : propuesta.getCategoria());
        return f;
    }

    private Map<String, Object> featuresMinimas(Propuesta propuesta) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("edad", 28);
        f.put("misma_ciudad", 0);
        f.put("salario", propuesta.getSalario() == null ? 0 : propuesta.getSalario());
        f.put("cantidad_requisitos", contarRequisitos(propuesta));
        f.put("num_postulantes", postulaciones.countByPropuestaId(propuesta.getIdPropuesta()));
        f.put("num_postulaciones_previas", 0);
        f.put("pct_aprobadas", 0.35);
        f.put("anios_experiencia", 0);
        f.put("nivel_educativo", 1);
        f.put("skills_coincidentes", 0);
        f.put("categoria", propuesta.getCategoria() == null ? "General" : propuesta.getCategoria());
        return f;
    }

    private Map<String, Object> heuristica(Map<String, Object> f) {
        if (f.get("edad") == null) {
            return Map.of("probabilidad", 0.4, "etiqueta", "Media", "modelo", "heuristica");
        }
        double score = 0.35
                + 0.12 * numero(f.get("misma_ciudad"))
                + 0.04 * Math.min(numero(f.get("anios_experiencia")), 8)
                + 0.03 * numero(f.get("nivel_educativo"))
                + 0.06 * Math.min(numero(f.get("skills_coincidentes")), 5)
                + 0.25 * numero(f.get("pct_aprobadas"))
                - 0.004 * numero(f.get("cantidad_requisitos"))
                - 0.004 * Math.min(numero(f.get("num_postulantes")), 40);
        double p = Math.max(0.05, Math.min(0.95, score));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("probabilidad", Math.round(p * 1000d) / 1000d);
        out.put("etiqueta", etiqueta(p));
        out.put("modelo", "heuristica");
        return out;
    }

    static String etiqueta(double p) {
        if (p >= 0.66) return "Alta";
        if (p >= 0.40) return "Media";
        return "Baja";
    }

    private boolean mismaCiudad(Usuario usuario, Propuesta propuesta) {
        String ciudadOferta = propuesta.getCiudad();
        if ((ciudadOferta == null || ciudadOferta.isBlank()) && propuesta.getEmpresaId() != null) {
            Empresa emp = empresas.findById(propuesta.getEmpresaId()).orElse(null);
            if (emp != null && emp.getDireccion() != null) ciudadOferta = emp.getDireccion().getCiudad();
        }
        String ciudadUsuario = usuario.getDireccion() == null ? null : usuario.getDireccion().getCiudad();
        if (ciudadOferta == null || ciudadUsuario == null) return false;
        return norm(ciudadOferta).equals(norm(ciudadUsuario));
    }

    private int contarRequisitos(Propuesta p) {
        int n = 0;
        if (p.getRequisitos() != null && !p.getRequisitos().isBlank()) {
            n += p.getRequisitos().split("[\\n,;]").length;
        }
        if (p.getSkillsRequeridas() != null) n += p.getSkillsRequeridas().size();
        return Math.max(n, 1);
    }

    private int anios(Usuario usuario) {
        if (usuario.getAniosExperiencia() != null) return usuario.getAniosExperiencia();
        if (usuario.getExperiencia() == null) return 0;
        int anio = java.time.Year.now().getValue();
        int total = 0;
        for (var exp : usuario.getExperiencia()) {
            if (exp.getAnioInicio() == null) continue;
            int fin = exp.getAnioFin() == null ? anio : exp.getAnioFin();
            total += Math.max(0, fin - exp.getAnioInicio());
        }
        return total;
    }

    private int nivel(Usuario usuario) {
        if (usuario.getEducacion() == null || usuario.getEducacion().isEmpty()) return 1;
        return usuario.getEducacion().stream().mapToInt(this::rango).max().orElse(1);
    }

    private int rango(Educacion e) {
        String n = e.getNivel() == null ? "" : norm(e.getNivel());
        if (n.contains("posgrado") || n.contains("maestr") || n.contains("doctor")) return 5;
        if (n.contains("profesional") || n.contains("universit") || n.contains("pregrado")) return 4;
        if (n.contains("tecnolog")) return 3;
        if (n.contains("tecnic")) return 2;
        return 1;
    }

    private int skills(Usuario usuario, Propuesta propuesta) {
        if (usuario.getHabilidades() == null || propuesta.getSkillsRequeridas() == null) return 0;
        int n = 0;
        for (String req : propuesta.getSkillsRequeridas()) {
            String r = norm(req);
            for (Habilidad h : usuario.getHabilidades()) {
                if (h.getNombre() != null && (norm(h.getNombre()).contains(r) || r.contains(norm(h.getNombre())))) {
                    n++;
                    break;
                }
            }
        }
        return n;
    }

    private String norm(String s) {
        return s.toLowerCase(Locale.ROOT).replace("á", "a").replace("é", "e")
                .replace("í", "i").replace("ó", "o").replace("ú", "u").trim();
    }

    private double numero(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0; }
    }
}
