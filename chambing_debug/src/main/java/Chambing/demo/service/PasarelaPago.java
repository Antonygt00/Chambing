package Chambing.demo.service;

import java.util.Map;

import Chambing.demo.Model.PagoDestacado;

public interface PasarelaPago {

    String nombre();

    /**
     * Inicia el cobro. No activa la oferta: eso ocurre solo cuando el webhook confirma el pago.
     */
    Checkout iniciar(PagoDestacado pago);

    Resultado interpretar(Map<String, Object> payload, String firma);

    record Checkout(String proveedor, String referencia, String checkoutUrl, String estado) {}

    record Resultado(String referencia, boolean aprobado, String estado) {}
}
