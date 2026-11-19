package Chambing.demo.Model;

/**
 * Planes de monetización "Oferta Destacada".
 * Precios en pesos colombianos, vigencia en días corridos desde la confirmación del pago.
 */
public enum PlanDestacado {
    BASICA("Destacada básica", 7, 25_000),
    ESTANDAR("Destacada estándar", 15, 45_000),
    PLUS("Destacada plus", 30, 80_000);

    private final String nombre;
    private final int dias;
    private final int precioCop;

    PlanDestacado(String nombre, int dias, int precioCop) {
        this.nombre = nombre;
        this.dias = dias;
        this.precioCop = precioCop;
    }

    public String getNombre() { return nombre; }
    public int getDias() { return dias; }
    public int getPrecioCop() { return precioCop; }

    public static PlanDestacado desde(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Selecciona un plan: BASICA, ESTANDAR o PLUS");
        }
        try {
            return PlanDestacado.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Plan no válido. Usa BASICA, ESTANDAR o PLUS");
        }
    }
}
