package Chambing.demo.Model;

import java.time.LocalDateTime;

public record DocumentoPostulacion(String id, String nombre, long tamano, LocalDateTime fecha) {}
