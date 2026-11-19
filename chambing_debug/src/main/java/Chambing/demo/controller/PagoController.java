package Chambing.demo.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Chambing.demo.Model.PagoDestacado;
import Chambing.demo.service.DestacadaService;
import Chambing.demo.service.PasarelaPago;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final DestacadaService destacadas;
    private final PasarelaPago pasarela;
    private final boolean simulacion;

    public PagoController(DestacadaService destacadas, PasarelaPago pasarela,
                          @Value("${app.pagos.modo:simulacion}") String modo) {
        this.destacadas = destacadas;
        this.pasarela = pasarela;
        this.simulacion = "simulacion".equalsIgnoreCase(modo);
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestBody Map<String, Object> payload,
                                     @RequestHeader(value = "X-Chambing-Firma", required = false) String firma) {
        try {
            PagoDestacado pago = destacadas.confirmar(pasarela.interpretar(payload, firma));
            return ResponseEntity.ok(Map.of(
                    "referencia", pago.getReferencia(),
                    "estado", pago.getEstado(),
                    "propuestaId", pago.getPropuestaId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping(value = "/simular/{referencia}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> pagina(@PathVariable String referencia) {
        if (!simulacion) return ResponseEntity.notFound().build();
        String html = """
                <!doctype html><html lang="es"><head><meta charset="utf-8"><title>Pago simulado</title>
                <style>body{font-family:sans-serif;max-width:520px;margin:40px auto;padding:0 16px}
                button{background:#0b6b4f;color:#fff;border:0;padding:10px 16px;border-radius:6px;cursor:pointer}
                .sec{margin-top:12px}</style></head><body>
                <h1>Pago simulado Chambing</h1>
                <p>Referencia <strong>%s</strong>. Wompi está desactivado. Elige el resultado del pago:</p>
                <form method="post" action="/api/pagos/simular/%s/aprobar"><button type="submit">Simular pago exitoso</button></form>
                <form class="sec" method="post" action="/api/pagos/simular/%s/rechazar"><button type="submit" style="background:#9b2335">Simular error de pago</button></form>
                </body></html>
                """.formatted(referencia, referencia, referencia);
        return ResponseEntity.ok(html);
    }

    @PostMapping(value = "/simular/{referencia}/aprobar", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> aprobar(@PathVariable String referencia) {
        return simular(referencia, true);
    }

    @PostMapping(value = "/simular/{referencia}/rechazar", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> rechazar(@PathVariable String referencia) {
        return simular(referencia, false);
    }

    private ResponseEntity<String> simular(String referencia, boolean aprobado) {
        if (!simulacion) return ResponseEntity.notFound().build();
        PagoDestacado pago = destacadas.confirmar(new PasarelaPago.Resultado(
                referencia, aprobado, aprobado ? "APROBADO" : "RECHAZADO"));
        String mensaje = aprobado
                ? "Pago aprobado. La oferta quedó destacada."
                : "Pago rechazado. La oferta no se destacó.";
        return ResponseEntity.ok("""
                <!doctype html><html lang="es"><head><meta charset="utf-8"><title>Resultado</title></head>
                <body style="font-family:sans-serif;max-width:520px;margin:40px auto">
                <h1>%s</h1><p>Referencia %s · estado %s</p>
                <p><a href="/company-dashboard">Volver al panel</a></p></body></html>
                """.formatted(mensaje, pago.getReferencia(), pago.getEstado()));
    }
}
