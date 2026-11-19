package Chambing.demo.Model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "mensajes")
public class Mensaje {
    @Id
    private String id;
    /** USER:{id} o EMPRESA:{id} */
    private String de;
    private String para;
    private String propuestaId;
    private String texto;
    private LocalDateTime fecha;
    private boolean leido;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDe() { return de; }
    public void setDe(String de) { this.de = de; }
    public String getPara() { return para; }
    public void setPara(String para) { this.para = para; }
    public String getPropuestaId() { return propuestaId; }
    public void setPropuestaId(String propuestaId) { this.propuestaId = propuestaId; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public boolean isLeido() { return leido; }
    public void setLeido(boolean leido) { this.leido = leido; }
}
