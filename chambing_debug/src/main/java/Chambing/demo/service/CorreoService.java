package Chambing.demo.service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import Chambing.demo.Model.Notificacion;
import Chambing.demo.Model.Postulacion;
import Chambing.demo.repository.EmpresaRepository;
import Chambing.demo.repository.UsuarioRepository;
import jakarta.mail.internet.MimeMessage;

/**
 * Envío opcional. Si MAIL_HOST no está definido, Spring no crea JavaMailSender
 * y este servicio solo registra el aviso en el log.
 */
@Service
public class CorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);
    private static final DateTimeFormatter FECHA_ES =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", new Locale("es", "CO"));

    private final ObjectProvider<JavaMailSender> mail;
    private final UsuarioRepository usuarios;
    private final EmpresaRepository empresas;
    private final String from;
    private final String urlBase;

    public CorreoService(ObjectProvider<JavaMailSender> mail,
                         UsuarioRepository usuarios,
                         EmpresaRepository empresas,
                         @Value("${app.mail.from:no-reply@chambing.local}") String from,
                         @Value("${app.frontend.base-url:https://chambing.com.co}") String urlBase) {
        this.mail = mail;
        this.usuarios = usuarios;
        this.empresas = empresas;
        this.from = from;
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
    }

    /** Compatibilidad: aviso simple en texto plano (campana interna / fallback). */
    public void avisar(String destinatario, String mensaje) {
        if (destinatario == null || !destinatario.contains(":")) return;
        String[] partes = destinatario.split(":", 2);
        String email = resolverEmail(partes[0], partes[1]);
        enviar(email, "Chambing", mensaje);
    }

    /**
     * Envía el correo HTML según el tipo de la notificación,
     * usando la postulación completa para rellenar placeholders.
     */
    public void avisarHtml(Notificacion n, Postulacion p) {
        if (n == null || n.destinatario() == null || !n.destinatario().contains(":")) return;

        String[] partes = n.destinatario().split(":", 2);
        String email = resolverEmail(partes[0], partes[1]);
        if (email == null || email.isBlank()) return;

        String tipo = n.tipo() != null ? n.tipo() : "";
        String asunto;
        String plantilla;
        Map<String, String> vars = new HashMap<>();

        vars.put("urlBase", urlBase);
        vars.put("idPostulacion", safe(p.getIdPostulacion()));
        vars.put("propuestaTitulo", safe(p.getPropuestaTitulo()));
        vars.put("nombreUsuario", safe(p.getUsuarioNombre()));
        vars.put("idPropuesta", safe(p.getPropuestaId()));

        if (p.getFechaPostulacion() != null) {
            vars.put("fechaPostulacion", p.getFechaPostulacion().format(FECHA_ES));
        } else {
            vars.put("fechaPostulacion", "");
        }

        switch (tipo) {
            case "postulacion_recibida" -> {
                asunto = "Recibimos tu postulación a " + safe(p.getPropuestaTitulo());
                plantilla = "01_postulacion_recibida.html";
            }
            case "nueva_postulacion_empresa" -> {
                asunto = safe(p.getUsuarioNombre()) + " se postuló a " + safe(p.getPropuestaTitulo());
                plantilla = "02_nueva_postulacion_empresa.html";
                // nombre de la empresa (si se puede resolver)
                if ("EMPRESA".equals(partes[0])) {
                    empresas.findById(partes[1]).ifPresent(e ->
                            vars.put("nombreEmpresa", safe(e.getNombreEmpresa())));
                }
                vars.putIfAbsent("nombreEmpresa", "tu empresa");
            }
            case "cambio_fase" -> {
                String fase = p.getFaseActual() != null ? p.getFaseActual().getNombre() : "nueva fase";
                asunto = "Tu postulación a " + safe(p.getPropuestaTitulo()) + " pasó a " + fase;
                plantilla = "03_cambio_fase.html";
                vars.put("faseActual", fase);
                vars.put("mensajeAdicional", safe(n.mensaje()));
            }
            case "postulacion_rechazada" -> {
                asunto = "Tu postulación a " + safe(p.getPropuestaTitulo()) + " fue rechazada";
                plantilla = "04_postulacion_rechazada.html";
            }
            default -> {
                // Fallback a texto plano si no hay tipo conocido
                enviar(email, "Chambing", n.mensaje());
                return;
            }
        }

        enviarHtml(email, asunto, plantilla, vars);
    }

    /** Texto plano (se mantiene por compatibilidad). */
    public void enviar(String destino, String asunto, String cuerpo) {
        if (destino == null || destino.isBlank()) return;
        JavaMailSender sender = mail.getIfAvailable();
        if (sender == null) {
            log.info("Correo no enviado (SMTP sin configurar) para {}: {}", destino, asunto);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(destino);
            msg.setSubject(asunto);
            msg.setText(cuerpo);
            sender.send(msg);
        } catch (Exception e) {
            log.warn("No se pudo enviar el correo a {}", destino, e);
        }
    }

    /** HTML con plantilla desde classpath:emails/ */
    public void enviarHtml(String destino, String asunto, String plantilla, Map<String, String> variables) {
        if (destino == null || destino.isBlank()) return;
        JavaMailSender sender = mail.getIfAvailable();
        if (sender == null) {
            log.info("Correo HTML no enviado (SMTP sin configurar) para {}: {}", destino, asunto);
            return;
        }
        try {
            String html = cargarPlantilla(plantilla, variables);
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(destino);
            helper.setSubject(asunto);
            helper.setText(html, true); // true = HTML
            sender.send(message);
            log.info("Correo HTML enviado a {} [{}]", destino, plantilla);
        } catch (Exception e) {
            log.warn("No se pudo enviar el correo HTML a {}", destino, e);
        }
    }

    private String cargarPlantilla(String nombre, Map<String, String> vars) throws Exception {
        ClassPathResource resource = new ClassPathResource("emails/" + nombre);
        String html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (vars != null) {
            for (Map.Entry<String, String> e : vars.entrySet()) {
                html = html.replace("{{" + e.getKey() + "}}", e.getValue() != null ? e.getValue() : "");
            }
        }
        return html;
    }

    private String resolverEmail(String tipo, String id) {
        return switch (tipo) {
            case "USER" -> usuarios.findById(id).map(u -> u.getEmail()).orElse(null);
            case "EMPRESA" -> empresas.findById(id).map(e -> e.getCorreo()).orElse(null);
            default -> null;
        };
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }
}