package Chambing.demo.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import Chambing.demo.Model.PagoDestacado;

/**
 * Integración Wompi (sandbox por defecto).
 * Variables: WOMPI_PUBLIC_KEY, WOMPI_PRIVATE_KEY, WOMPI_INTEGRITY_SECRET, WOMPI_EVENTS_SECRET.
 * La firma de integridad del checkout es SHA-256 de referencia + montoEnCentavos + COP + secreto.
 * Documentación: https://docs.wompi.co/docs/colombia/widget-checkout-web/
 *
 * Desactivado a propósito: no hay credenciales de prueba.
 * Para volver a usarlo, descomenta estas dos anotaciones y las propiedades app.pagos.wompi.* :
 *   @Service
 *   @ConditionalOnProperty(name = "app.pagos.proveedor", havingValue = "wompi")
 */
public class PasarelaWompi implements PasarelaPago {

    private final String publicKey;
    private final String privateKey;
    private final String integrity;
    private final String events;
    private final String api;
    private final String redirect;
    private final RestClient http;

    public PasarelaWompi(
            @Value("${app.pagos.wompi.public-key:}") String publicKey,
            @Value("${app.pagos.wompi.private-key:}") String privateKey,
            @Value("${app.pagos.wompi.integrity:}") String integrity,
            @Value("${app.pagos.wompi.events:}") String events,
            @Value("${app.pagos.wompi.api:https://sandbox.wompi.co/v1}") String api,
            @Value("${app.pagos.public-base-url:https://chambing.com.co}") String baseUrl) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.integrity = integrity;
        this.events = events;
        this.api = api;
        this.redirect = baseUrl + "/company-dashboard?pago=retorno";
        this.http = RestClient.builder().baseUrl(api).build();
    }

    @Override
    public String nombre() { return "wompi"; }

    @Override
    public Checkout iniciar(PagoDestacado pago) {
        if (privateKey == null || privateKey.isBlank() || integrity == null || integrity.isBlank()) {
            throw new IllegalStateException(
                    "Wompi no está configurado (llave pública " + (publicKey == null || publicKey.isBlank() ? "ausente" : "presente")
                            + "). Define WOMPI_PRIVATE_KEY y WOMPI_INTEGRITY_SECRET, o usa app.pagos.proveedor=simulada");
        }
        long centavos = pago.getMontoCop() * 100L;
        String firma = sha256(pago.getReferencia() + centavos + "COP" + integrity);
        Map<String, Object> body = Map.of(
                "name", "Oferta destacada " + pago.getPlan(),
                "description", "Destacar propuesta " + pago.getPropuestaId(),
                "single_use", true,
                "collect_shipping", false,
                "currency", "COP",
                "amount_in_cents", centavos,
                "redirect_url", redirect,
                "sku", pago.getReferencia(),
                "signature", Map.of("integrity", firma));
        @SuppressWarnings("unchecked")
        Map<String, Object> respuesta = http.post()
                .uri("/payment_links")
                .header("Authorization", "Bearer " + privateKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);
        String url = extraerLink(respuesta);
        return new Checkout(nombre(), pago.getReferencia(), url, "PENDIENTE");
    }

    @Override
    public Resultado interpretar(Map<String, Object> payload, String firma) {
        String referencia = null;
        String estado = null;
        Object data = payload.get("data");
        if (data instanceof Map<?, ?> mapa) {
            Object tx = mapa.get("transaction");
            if (tx instanceof Map<?, ?> transaccion) {
                referencia = texto(transaccion.get("reference"));
                estado = texto(transaccion.get("status"));
            }
        }
        if (referencia == null) referencia = texto(payload.get("referencia"));
        if (estado == null) estado = texto(payload.get("estado"));
        if (events != null && !events.isBlank() && firma != null && !firma.isBlank() && !firma.equals(events)) {
            throw new IllegalArgumentException("Firma de evento Wompi inválida");
        }
        boolean aprobado = "APPROVED".equalsIgnoreCase(estado) || "APROBADO".equalsIgnoreCase(estado);
        return new Resultado(referencia, aprobado, aprobado ? "APROBADO" : "RECHAZADO");
    }

    @SuppressWarnings("unchecked")
    private String extraerLink(Map<String, Object> respuesta) {
        if (respuesta == null) {
            throw new IllegalStateException("Wompi no devolvió un link de pago");
        }
        Object data = respuesta.get("data");
        if (data instanceof Map<?, ?> mapa && mapa.get("id") != null) {
            return "https://checkout.wompi.co/l/" + mapa.get("id");
        }
        throw new IllegalStateException("Respuesta de Wompi sin id de link: " + respuesta);
    }

    private String texto(Object valor) {
        return valor == null ? null : String.valueOf(valor);
    }

    static String sha256(String valor) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el pago", e);
        }
    }
}
