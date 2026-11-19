package Chambing.demo.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Objects;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import Chambing.demo.Model.DocumentoPostulacion;
import Chambing.demo.Model.Postulacion;
import Chambing.demo.Model.Propuesta;
import Chambing.demo.repository.PostulacionRepository;
import Chambing.demo.service.AccesoSeleccion;
import Chambing.demo.service.NotificacionService;
import Chambing.demo.service.SeleccionService;

@RestController
@RequestMapping("/api/postulaciones/{id}/documentos")
public class DocumentoController {
    private final AccesoSeleccion acceso;
    private final SeleccionService seleccion;
    private final PostulacionRepository repo;
    private final GridFsTemplate archivos;
    private final NotificacionService notificaciones;
    public DocumentoController(AccesoSeleccion acceso, SeleccionService seleccion,
            PostulacionRepository repo, GridFsTemplate archivos, NotificacionService notificaciones) {
        this.acceso = acceso; this.seleccion = seleccion; this.repo = repo;
        this.archivos = archivos; this.notificaciones = notificaciones;
    }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object subir(@PathVariable String id, @RequestParam("archivo") MultipartFile archivo,
            @AuthenticationPrincipal UserDetails principal) throws IOException {
        Postulacion p = acceso.postulacion(id);
        acceso.usuario(principal, p.getUsuarioId());
        if (!SeleccionService.estado(p.getEstadoPostulacion()).equals("Pendiente"))
            throw new ResponseStatusException(CONFLICT, "El proceso ya finalizó");
        if (p.getDocumentos().size() >= 5)
            throw new ResponseStatusException(BAD_REQUEST, "Se admiten hasta 5 documentos por postulación");
        byte[] contenido = archivo.getBytes();
        String nombre = Objects.toString(archivo.getOriginalFilename(), "documento.pdf")
            .replaceAll("[\\\\/\\r\\n\\p{Cntrl}]", "_");
        if (nombre.length() > 150) nombre = nombre.substring(nombre.length() - 150);
        if (contenido.length < 5 || contenido.length > 5 * 1024 * 1024
                || !nombre.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")
                || !new String(contenido, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-"))
            throw new ResponseStatusException(BAD_REQUEST, "Adjunta un PDF válido de hasta 5 MB");
        ObjectId archivoId = archivos.store(new java.io.ByteArrayInputStream(contenido), nombre, "application/pdf");
        try {
            p.getDocumentos().add(new DocumentoPostulacion(archivoId.toHexString(), nombre, contenido.length, LocalDateTime.now()));
            p.setFechaActualizacion(LocalDateTime.now());
            Propuesta oferta = acceso.propuesta(p.getPropuestaId());
            notificaciones.agregar(p, "EMPRESA:" + oferta.getEmpresaId(),
                p.getUsuarioNombre() + " adjuntó documentación a " + p.getPropuestaTitulo(),
                SeleccionService.enlaceEmpresa(p),
                "documento_adjunto");
            return seleccion.vista(repo.save(p), false);
        } catch (RuntimeException e) {
            archivos.delete(Query.query(Criteria.where("_id").is(archivoId)));
            throw e;
        }
    }
    @GetMapping("/{documentoId}")
    public ResponseEntity<byte[]> descargar(@PathVariable String id, @PathVariable String documentoId,
            @AuthenticationPrincipal UserDetails principal) throws IOException {
        Postulacion p = acceso.postulacion(id);
        acceso.puedeLeer(principal, p);
        DocumentoPostulacion documento = p.getDocumentos().stream().filter(d -> d.id().equals(documentoId))
            .findFirst().orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Documento no encontrado"));
        if (!ObjectId.isValid(documentoId)) throw new ResponseStatusException(NOT_FOUND);
        var file = archivos.findOne(Query.query(Criteria.where("_id").is(new ObjectId(documentoId))));
        if (file == null) throw new ResponseStatusException(NOT_FOUND, "Documento no encontrado");
        byte[] contenido;
        try (var stream = archivos.getResource(file).getInputStream()) { contenido = stream.readAllBytes(); }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
            .header("X-Content-Type-Options", "nosniff").header("Cache-Control", "no-store")
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(documento.nombre(), StandardCharsets.UTF_8).build().toString()).body(contenido);
    }
}
