package Chambing.demo.Model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "favoritos")
@CompoundIndex(name = "usuario_propuesta", def = "{'usuarioId': 1, 'propuestaId': 1}", unique = true)
public class Favorito {
    @Id
    private String id;
    private String usuarioId;
    private String propuestaId;
    private LocalDateTime fecha;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
    public String getPropuestaId() { return propuestaId; }
    public void setPropuestaId(String propuestaId) { this.propuestaId = propuestaId; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
