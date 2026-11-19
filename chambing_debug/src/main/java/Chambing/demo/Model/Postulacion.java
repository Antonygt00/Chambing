package Chambing.demo.Model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Version;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "postulaciones")
public class Postulacion {

    @Id
    private String idPostulacion;

    private String usuarioId;
    private String usuarioNombre;
    private String usuarioEmail;

    private String propuestaId;
    private String propuestaTitulo;
    @JsonIgnore
    @org.springframework.data.mongodb.core.index.Indexed(unique = true, sparse = true)
    private String inscripcionClave;
    @JsonIgnore public String getInscripcionClave() { return inscripcionClave; }
    public void setInscripcionClave(String clave) { this.inscripcionClave = clave; }

    private String estadoPostulacion;

    private LocalDateTime fechaPostulacion;

    @Version private Long version;
    private FaseSeleccion faseActual;
    private LocalDateTime fechaActualizacion;
    private List<CambioFase> historial = new ArrayList<>();
    private List<DocumentoPostulacion> documentos = new ArrayList<>();
    // Outbox: se guarda atómicamente junto al cambio de la postulación.
    @JsonIgnore private List<Notificacion> notificacionesPendientes = new ArrayList<>();

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public FaseSeleccion getFaseActual() { return faseActual; }
    public void setFaseActual(FaseSeleccion faseActual) { this.faseActual = faseActual; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fecha) { this.fechaActualizacion = fecha; }
    public List<CambioFase> getHistorial() { return historial; }
    public void setHistorial(List<CambioFase> historial) { this.historial = historial; }
    public List<DocumentoPostulacion> getDocumentos() { return documentos; }
    public void setDocumentos(List<DocumentoPostulacion> documentos) { this.documentos = documentos; }
    @JsonIgnore public List<Notificacion> getNotificacionesPendientes() { return notificacionesPendientes; }
    public void setNotificacionesPendientes(List<Notificacion> pendientes) { this.notificacionesPendientes = pendientes; }

    public String getIdPostulacion() { return idPostulacion; }
    public void setIdPostulacion(String idPostulacion) { this.idPostulacion = idPostulacion; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }

    public String getUsuarioEmail() { return usuarioEmail; }
    public void setUsuarioEmail(String usuarioEmail) { this.usuarioEmail = usuarioEmail; }

    public String getPropuestaId() { return propuestaId; }
    public void setPropuestaId(String propuestaId) { this.propuestaId = propuestaId; }

    public String getPropuestaTitulo() { return propuestaTitulo; }
    public void setPropuestaTitulo(String propuestaTitulo) { this.propuestaTitulo = propuestaTitulo; }

    public String getEstadoPostulacion() { return estadoPostulacion; }
    public void setEstadoPostulacion(String estadoPostulacion) { this.estadoPostulacion = estadoPostulacion; }

    public LocalDateTime getFechaPostulacion() { return fechaPostulacion; }
    public void setFechaPostulacion(LocalDateTime fechaPostulacion) { this.fechaPostulacion = fechaPostulacion; }
}
