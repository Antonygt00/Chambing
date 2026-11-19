package Chambing.demo.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import Chambing.demo.Model.Empresa;
import Chambing.demo.Model.PagoDestacado;
import Chambing.demo.Model.PlanDestacado;
import Chambing.demo.Model.Propuesta;
import Chambing.demo.repository.EmpresaRepository;
import Chambing.demo.repository.PagoDestacadoRepository;
import Chambing.demo.repository.PropuestaRepository;
import Chambing.demo.service.PasarelaPago.Checkout;
import Chambing.demo.service.PasarelaPago.Resultado;

@Service
public class DestacadaService {

    private final PropuestaRepository propuestas;
    private final PagoDestacadoRepository pagos;
    private final EmpresaRepository empresas;
    private final PasarelaPago pasarela;
    private final CorreoService correo;

    public DestacadaService(PropuestaRepository propuestas, PagoDestacadoRepository pagos,
                            EmpresaRepository empresas, PasarelaPago pasarela, CorreoService correo) {
        this.propuestas = propuestas;
        this.pagos = pagos;
        this.empresas = empresas;
        this.pasarela = pasarela;
        this.correo = correo;
    }

    public List<Map<String, Object>> planes() {
        return java.util.Arrays.stream(PlanDestacado.values()).map(p -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("codigo", p.name());
            item.put("nombre", p.getNombre());
            item.put("dias", p.getDias());
            item.put("precioCop", p.getPrecioCop());
            return item;
        }).toList();
    }

    public boolean vigente(Propuesta p, LocalDateTime ahora) {
        return p != null && p.isDestacada() && p.getFechaFinDestacada() != null
                && p.getFechaFinDestacada().isAfter(ahora);
    }

    public Map<String, Object> iniciar(String empresaId, String propuestaId, String planCodigo) {
        PlanDestacado plan = PlanDestacado.desde(planCodigo);
        Propuesta propuesta = propuestas.findById(propuestaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Propuesta no encontrada"));
        if (!empresaId.equals(propuesta.getEmpresaId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes destacar una propuesta de otra empresa");
        }
        if (vigente(propuesta, LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta oferta ya está destacada hasta " + propuesta.getFechaFinDestacada());
        }
        PagoDestacado pago = new PagoDestacado();
        pago.setReferencia("CHB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        pago.setPropuestaId(propuestaId);
        pago.setEmpresaId(empresaId);
        pago.setPlan(plan.name());
        pago.setDias(plan.getDias());
        pago.setMontoCop(plan.getPrecioCop());
        pago.setEstado("PENDIENTE");
        pago.setProveedor(pasarela.nombre());
        pago.setCreadoEn(LocalDateTime.now());
        pagos.save(pago);

        Checkout checkout = pasarela.iniciar(pago);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("pagoId", pago.getId());
        out.put("referencia", pago.getReferencia());
        out.put("plan", plan.name());
        out.put("dias", plan.getDias());
        out.put("montoCop", plan.getPrecioCop());
        out.put("moneda", "COP");
        out.put("estado", "PENDIENTE");
        out.put("proveedor", checkout.proveedor());
        out.put("checkoutUrl", checkout.checkoutUrl());
        out.put("propuestaId", propuestaId);
        return out;
    }

    public PagoDestacado confirmar(Resultado resultado) {
        if (resultado == null || resultado.referencia() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El webhook no trae referencia");
        }
        PagoDestacado pago = pagos.findByReferencia(resultado.referencia())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no encontrado"));
        if ("APROBADO".equals(pago.getEstado())) {
            return pago;
        }
        pago.setEstado(resultado.aprobado() ? "APROBADO" : "RECHAZADO");
        pago.setConfirmadoEn(LocalDateTime.now());
        pagos.save(pago);
        if (resultado.aprobado()) {
            activar(pago);
        }
        return pago;
    }

    private void activar(PagoDestacado pago) {
        propuestas.findById(pago.getPropuestaId()).ifPresent(p -> {
            LocalDateTime inicio = LocalDateTime.now();
            if (vigente(p, inicio) && p.getFechaFinDestacada() != null) {
                inicio = p.getFechaFinDestacada();
            }
            p.setDestacada(true);
            p.setPlanDestacado(pago.getPlan());
            p.setFechaInicioDestacada(LocalDateTime.now());
            p.setFechaFinDestacada(inicio.plusDays(pago.getDias()));
            propuestas.save(p);

            empresas.findById(pago.getEmpresaId()).ifPresent(emp -> {
                Map<String, String> vars = new java.util.HashMap<>();
                vars.put("nombreEmpresa", emp.getNombreEmpresa() != null ? emp.getNombreEmpresa() : "tu empresa");
                vars.put("referencia", pago.getReferencia() != null ? pago.getReferencia() : "");
                vars.put("plan", pago.getPlan() != null ? pago.getPlan() : "");
                vars.put("fechaFinDestacada", p.getFechaFinDestacada() != null
                        ? p.getFechaFinDestacada().toString() : "");
                vars.put("urlBase", "https://chambing.com.co");

                correo.enviarHtml(
                        emp.getCorreo(),
                        "Tu oferta quedó destacada",
                        "05_oferta_destacada.html",
                        vars
                );
            });
        });
    }

    @Scheduled(fixedDelay = 900_000)
    public int expirar() {
        LocalDateTime ahora = LocalDateTime.now();
        int n = 0;
        for (Propuesta p : propuestas.findAll()) {
            if (p.isDestacada() && (p.getFechaFinDestacada() == null || !p.getFechaFinDestacada().isAfter(ahora))) {
                p.setDestacada(false);
                propuestas.save(p);
                n++;
            }
        }
        return n;
    }

    public Map<String, Object> resumenAdmin() {
        expirar();
        LocalDateTime ahora = LocalDateTime.now();
        List<Propuesta> activas = propuestas.findAll().stream().filter(p -> vigente(p, ahora)).toList();
        List<PagoDestacado> aprobados = pagos.findByEstado("APROBADO");
        long ingresos = aprobados.stream().mapToLong(PagoDestacado::getMontoCop).sum();
        Map<String, Long> porPlan = new LinkedHashMap<>();
        for (PlanDestacado plan : PlanDestacado.values()) {
            porPlan.put(plan.name(), aprobados.stream().filter(p -> plan.name().equals(p.getPlan())).count());
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ingresosCop", ingresos);
        out.put("pagosAprobados", aprobados.size());
        out.put("destacadasActivas", activas.size());
        out.put("porPlan", porPlan);
        out.put("activas", activas.stream().map(p -> Map.of(
                "idPropuesta", p.getIdPropuesta(),
                "titulo", p.getTitulo() == null ? "" : p.getTitulo(),
                "empresa", p.getEmpresaNombre() == null ? "" : p.getEmpresaNombre(),
                "plan", p.getPlanDestacado() == null ? "" : p.getPlanDestacado(),
                "hasta", String.valueOf(p.getFechaFinDestacada()))).toList());
        return out;
    }

    public Empresa exigirEmpresa(String id) {
        return empresas.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));
    }
}
