package Chambing.demo.Model;

import jakarta.validation.constraints.NotBlank;

public class Direccion {

    @NotBlank
    private String calle;

    private String ciudad;

    private String departamento;
    private String departamentoId;
    private String ciudadId;

    public String getDepartamentoId() { return departamentoId; }
    public void setDepartamentoId(String id) { this.departamentoId = id; }
    public String getCiudadId() { return ciudadId; }
    public void setCiudadId(String id) { this.ciudadId = id; }

    public String getCalle() { return calle; }
    public void setCalle(String calle) { this.calle = calle; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }
}
