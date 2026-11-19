package Chambing.demo.Model;

public enum FaseSeleccion {
    INSCRIPCION("Inscripción"), REVISION_DOCUMENTAL("Revisión documental"),
    POTENCIAL_CANDIDATO("Potencial candidato"), SELECCIONADO("Seleccionado");

    private final String nombre;
    FaseSeleccion(String nombre) { this.nombre = nombre; }
    public String getNombre() { return nombre; }
}
