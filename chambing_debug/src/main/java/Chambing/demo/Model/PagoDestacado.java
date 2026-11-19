package Chambing.demo.Model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "pagos_destacados")
public class PagoDestacado {

    @Id
    private String id;

    @Indexed(unique = true)
    private String referencia;

    private String propuestaId;
    private String empresaId;
    private String plan;
    private int dias;
    private long montoCop;
    /** PENDIENTE, APROBADO, RECHAZADO */
    private String estado;
    private String proveedor;
    private LocalDateTime creadoEn;
    private LocalDateTime confirmadoEn;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public String getPropuestaId() { return propuestaId; }
    public void setPropuestaId(String propuestaId) { this.propuestaId = propuestaId; }
    public String getEmpresaId() { return empresaId; }
    public void setEmpresaId(String empresaId) { this.empresaId = empresaId; }
    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }
    public int getDias() { return dias; }
    public void setDias(int dias) { this.dias = dias; }
    public long getMontoCop() { return montoCop; }
    public void setMontoCop(long montoCop) { this.montoCop = montoCop; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
    public LocalDateTime getConfirmadoEn() { return confirmadoEn; }
    public void setConfirmadoEn(LocalDateTime confirmadoEn) { this.confirmadoEn = confirmadoEn; }
}
