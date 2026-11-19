package Chambing.demo.Model;

import java.time.LocalDateTime;

/** Un evento real. Los registros antiguos no reciben un historial inventado. */
public record CambioFase(FaseSeleccion faseAnterior, FaseSeleccion fase,
        String resultado, LocalDateTime fecha, String actor,
        String observacion, String notaInterna) {}
