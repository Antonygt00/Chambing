package Chambing.demo.Model;

public class Educacion {
    private String titulo;
    private String institucion;
    /** Bachillerato, Tecnico, Tecnologo, Profesional, Posgrado */
    private String nivel;
    private Integer anioFin;

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getInstitucion() { return institucion; }
    public void setInstitucion(String institucion) { this.institucion = institucion; }
    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }
    public Integer getAnioFin() { return anioFin; }
    public void setAnioFin(Integer anioFin) { this.anioFin = anioFin; }
}
