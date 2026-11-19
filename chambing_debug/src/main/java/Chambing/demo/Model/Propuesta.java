package Chambing.demo.Model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

@Document(collection = "propuestas")
public class Propuesta {

    @Id
    private String idPropuesta;

    @NotBlank
    private String titulo;

    private String descripcion;

    private String requisitos;

    @DecimalMin("0.0")
    private Double salario;

    private String empresaId;
    private String empresaNombre;

    private String estado;

    private String categoria;

    /** PRESENCIAL, REMOTO o HIBRIDO */
    private String modalidad;
    /** Indefinido, Fijo, Prestacion de servicios, Aprendizaje */
    private String tipoContrato;
    private String nivelExperiencia;
    private List<String> beneficios = new ArrayList<>();
    private List<String> skillsRequeridas = new ArrayList<>();
    private LocalDate fechaCierre;
    private Integer numeroVacantes;
    private String ciudad;
    private LocalDateTime fechaPublicacion;

    @Indexed
    private boolean destacada;
    private LocalDateTime fechaInicioDestacada;
    private LocalDateTime fechaFinDestacada;
    /** BASICA, ESTANDAR o PLUS */
    private String planDestacado;

    public String getIdPropuesta() { return idPropuesta; }
    public void setIdPropuesta(String idPropuesta) { this.idPropuesta = idPropuesta; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getRequisitos() { return requisitos; }
    public void setRequisitos(String requisitos) { this.requisitos = requisitos; }

    public Double getSalario() { return salario; }
    public void setSalario(Double salario) { this.salario = salario; }

    public String getEmpresaId() { return empresaId; }
    public void setEmpresaId(String empresaId) { this.empresaId = empresaId; }

    public String getEmpresaNombre() { return empresaNombre; }
    public void setEmpresaNombre(String empresaNombre) { this.empresaNombre = empresaNombre; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getModalidad() { return modalidad; }
    public void setModalidad(String modalidad) { this.modalidad = modalidad; }
    public String getTipoContrato() { return tipoContrato; }
    public void setTipoContrato(String tipoContrato) { this.tipoContrato = tipoContrato; }
    public String getNivelExperiencia() { return nivelExperiencia; }
    public void setNivelExperiencia(String nivelExperiencia) { this.nivelExperiencia = nivelExperiencia; }
    public List<String> getBeneficios() { return beneficios; }
    public void setBeneficios(List<String> beneficios) { this.beneficios = beneficios; }
    public List<String> getSkillsRequeridas() { return skillsRequeridas; }
    public void setSkillsRequeridas(List<String> skillsRequeridas) { this.skillsRequeridas = skillsRequeridas; }
    public LocalDate getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDate fechaCierre) { this.fechaCierre = fechaCierre; }
    public Integer getNumeroVacantes() { return numeroVacantes; }
    public void setNumeroVacantes(Integer numeroVacantes) { this.numeroVacantes = numeroVacantes; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public LocalDateTime getFechaPublicacion() { return fechaPublicacion; }
    public void setFechaPublicacion(LocalDateTime fechaPublicacion) { this.fechaPublicacion = fechaPublicacion; }
    public boolean isDestacada() { return destacada; }
    public void setDestacada(boolean destacada) { this.destacada = destacada; }
    public LocalDateTime getFechaInicioDestacada() { return fechaInicioDestacada; }
    public void setFechaInicioDestacada(LocalDateTime fechaInicioDestacada) { this.fechaInicioDestacada = fechaInicioDestacada; }
    public LocalDateTime getFechaFinDestacada() { return fechaFinDestacada; }
    public void setFechaFinDestacada(LocalDateTime fechaFinDestacada) { this.fechaFinDestacada = fechaFinDestacada; }
    public String getPlanDestacado() { return planDestacado; }
    public void setPlanDestacado(String planDestacado) { this.planDestacado = planDestacado; }
}