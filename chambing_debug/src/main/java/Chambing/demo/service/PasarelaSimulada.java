package Chambing.demo.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import Chambing.demo.Model.PagoDestacado;

/**
 * Pasarela de desarrollo. La oferta queda pendiente hasta que se confirma
 * en /api/pagos/simular/{referencia} o vía POST /api/pagos/webhook.
 */
@Service
@ConditionalOnProperty(name = "app.pagos.proveedor", havingValue = "simulada", matchIfMissing = true)
public class PasarelaSimulada implements PasarelaPago {

    private final String baseUrl;
    private final String secreto;

    public PasarelaSimulada(
            @Value("${app.pagos.public-base-url:https://chambing.com.co}") String baseUrl,
            @Value("${app.pagos.webhook-secret:dev-webhook-chambing}") String secreto) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.secreto = secreto;
    }

    @Override
    public String nombre() { return "simulada"; }

    @Override
    public Checkout iniciar(PagoDestacado pago) {
        String url = baseUrl + "/api/pagos/simular/" + pago.getReferencia();
        return new Checkout(nombre(), pago.getReferencia(), url, "PENDIENTE");
    }

    @Override
    public Resultado interpretar(Map<String, Object> payload, String firma) {
        if (secreto != null && !secreto.isBlank() && (firma == null || !firma.equals(secreto))) {
            throw new IllegalArgumentException("Firma de webhook inválida");
        }
        String referencia = texto(payload.get("referencia"));
        String estado = texto(payload.get("estado"));
        if (referencia == null) {
            referencia = referenciaAnidada(payload);
        }
        if (estado == null) {
            estado = estadoAnidado(payload);
        }
        boolean aprobado = estado != null && (estado.equalsIgnoreCase("APPROVED")
                || estado.equalsIgnoreCase("APROBADO"));
        return new Resultado(referencia, aprobado, aprobado ? "APROBADO" : "RECHAZADO");
    }

    @SuppressWarnings("unchecked")
    private String referenciaAnidada(Map<String, Object> payload) {
        Object data = payload.get("data");
        if (!(data instanceof Map<?, ?> mapa)) return null;
        Object tx = ((Map<String, Object>) mapa).get("transaction");
        if (!(tx instanceof Map<?, ?> transaccion)) return null;
        return texto(((Map<String, Object>) transaccion).get("reference"));
    }

    @SuppressWarnings("unchecked")
    private String estadoAnidado(Map<String, Object> payload) {
        Object data = payload.get("data");
        if (!(data instanceof Map<?, ?> mapa)) return null;
        Object tx = ((Map<String, Object>) mapa).get("transaction");
        if (!(tx instanceof Map<?, ?> transaccion)) return null;
        return texto(((Map<String, Object>) transaccion).get("status"));
    }

    private String texto(Object valor) {
        return valor == null ? null : String.valueOf(valor);
    }
}
