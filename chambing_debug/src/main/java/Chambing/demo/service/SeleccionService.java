package Chambing.demo.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import Chambing.demo.Model.CambioFase;
import Chambing.demo.Model.FaseSeleccion;
import Chambing.demo.Model.Postulacion;
import Chambing.demo.Model.Propuesta;
import Chambing.demo.repository.PostulacionRepository;

@Service
public class SeleccionService {
    private final PostulacionRepository repo;
    private final NotificacionService notificaciones;
    public SeleccionService(PostulacionRepository repo, NotificacionService notificaciones) {
        this.repo = repo; this.notificaciones = notificaciones;
    }
    public static String estado(String valor) {
        return switch (Objects.toString(valor, "Pendiente").toLowerCase(Locale.ROOT)) {
            case "aprobado", "aprobada", "seleccionado" -> "Aprobado";
            case "rechazado", "rechazada" -> "Rechazado";
            default -> "Pendiente";
        };
    }
    public static FaseSeleccion fase(Postulacion p) {
        return p.getFaseActual() != null ? p.getFaseActual()
            : estado(p.getEstadoPostulacion()).equals("Aprobado")
                ? FaseSeleccion.SELECCIONADO : FaseSeleccion.INSCRIPCION;
    }
    public Postulacion inscribir(Postulacion p, Propuesta oferta) {
        p.setIdPostulacion(UUID.randomUUID().toString());
        p.setInscripcionClave(p.getUsuarioId() + ":" + p.getPropuestaId());
        p.setFechaPostulacion(LocalDateTime.now());
        p.setFechaActualizacion(p.getFechaPostulacion());
        p.setFaseActual(FaseSeleccion.INSCRIPCION);
        p.setEstadoPostulacion("Pendiente");
        p.getHistorial().add(new CambioFase(null, p.getFaseActual(), "Pendiente",
            p.getFechaPostulacion(), p.getUsuarioId(), "Inscripción recibida", ""));

        // Usuario
        notificaciones.agregar(p,
                "USER:" + p.getUsuarioId(),
                "Recibimos tu postulación a " + p.getPropuestaTitulo(),
                enlaceUsuario(p),
                "postulacion_recibida");

        // Empresa
        notificaciones.agregar(p,
                "EMPRESA:" + oferta.getEmpresaId(),
                p.getUsuarioNombre() + " se postuló a " + p.getPropuestaTitulo(),
                enlaceEmpresa(p),
                "nueva_postulacion_empresa");

        return repo.save(p);
    }

    public Postulacion cambiar(Postulacion p, FaseSeleccion nueva, String resultado,
            String observacion, String nota, String actor, Long version) {
        if (version != null && !version.equals(p.getVersion()))
            throw new ResponseStatusException(CONFLICT, "La postulación cambió. Recarga antes de continuar");
        observacion = texto(observacion); nota = texto(nota);
        FaseSeleccion anterior = fase(p);
        String previo = estado(p.getEstadoPostulacion());
        if (nueva == null) nueva = anterior;
        if (resultado == null) resultado = nueva == FaseSeleccion.SELECCIONADO ? "Aprobado" : "Pendiente";
        if (!Set.of("Pendiente", "Aprobado", "Rechazado").contains(resultado))
            throw new ResponseStatusException(BAD_REQUEST, "Resultado no válido");
        if (resultado.equals("Aprobado")) nueva = FaseSeleccion.SELECCIONADO;
        if (nueva == FaseSeleccion.SELECCIONADO && resultado.equals("Pendiente"))
            nueva = FaseSeleccion.POTENCIAL_CANDIDATO;
        if (anterior == nueva && previo.equals(resultado)) {
            if (!observacion.isBlank() || !nota.isBlank())
                throw new ResponseStatusException(BAD_REQUEST, "Elige una fase o resultado diferente para registrar la observación y la nota");
            return p;
        }
        if ((nueva.ordinal() < anterior.ordinal() || resultado.equals("Rechazado")
                || !previo.equals("Pendiente")) && observacion.isBlank())
            throw new ResponseStatusException(BAD_REQUEST, "Explica el rechazo, retroceso o reapertura en la observación");
        p.setFaseActual(nueva); p.setEstadoPostulacion(resultado);
        p.setFechaActualizacion(LocalDateTime.now());
        p.getHistorial().add(new CambioFase(anterior, nueva, resultado,
            p.getFechaActualizacion(), actor, observacion, nota));

        boolean rechazado = resultado.equals("Rechazado");
        String mensaje = rechazado
                ? "Tu postulación a " + p.getPropuestaTitulo() + " fue rechazada"
                : "Tu postulación a " + p.getPropuestaTitulo() + " pasó a " + nueva.getNombre();
        String tipo = rechazado ? "postulacion_rechazada" : "cambio_fase";

        notificaciones.agregar(p, "USER:" + p.getUsuarioId(), mensaje, enlaceUsuario(p), tipo);
        return repo.save(p);
    }
    private String texto(String v) {
        if (v != null && v.length() > 2000)
            throw new ResponseStatusException(BAD_REQUEST, "Las observaciones admiten hasta 2000 caracteres");
        return v == null ? "" : v.trim();
    }
    public static String enlaceUsuario(Postulacion p) { return "/proposals-states?postulacion=" + p.getIdPostulacion(); }
    public static String enlaceEmpresa(Postulacion p) {
        return "/proposals-answer?idPropuesta=" + p.getPropuestaId() + "&postulacion=" + p.getIdPostulacion();
    }
    /** Proyección explícita: el usuario nunca recibe notas internas ni la outbox. */
    public Map<String, Object> vista(Postulacion p, boolean empresa) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("idPostulacion", p.getIdPostulacion()); v.put("usuarioId", p.getUsuarioId());
        v.put("usuarioNombre", p.getUsuarioNombre()); v.put("usuarioEmail", p.getUsuarioEmail());
        v.put("propuestaId", p.getPropuestaId()); v.put("propuestaTitulo", p.getPropuestaTitulo());
        v.put("estadoPostulacion", estado(p.getEstadoPostulacion())); v.put("faseActual", fase(p));
        v.put("fechaPostulacion", p.getFechaPostulacion()); v.put("fechaActualizacion", p.getFechaActualizacion());
        v.put("version", p.getVersion()); v.put("documentos", p.getDocumentos());
        v.put("historial", p.getHistorial().stream().map(c -> {
            Map<String, Object> h = new LinkedHashMap<>();
            h.put("faseAnterior", c.faseAnterior()); h.put("fase", c.fase());
            h.put("resultado", c.resultado()); h.put("fecha", c.fecha()); h.put("observacion", c.observacion());
            if (empresa) { h.put("actor", c.actor()); h.put("notaInterna", c.notaInterna()); }
            return h;
        }).toList());
        return v;
    }
}
