package Chambing.demo.controller;

import java.util.Map;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ErroresSeleccion {
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<?> conflicto(OptimisticLockingFailureException e) {
        return ResponseEntity.status(409).body(Map.of("message", "La postulación cambió. Recarga antes de continuar."));
    }
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<?> duplicado(DuplicateKeyException e) {
        return ResponseEntity.status(409).body(Map.of("message", "Ya existe un registro con esos datos."));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> archivoGrande(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(413).body(Map.of("message", "El PDF no puede superar 5 MB."));
    }
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<?> solicitud(org.springframework.web.server.ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason() == null ? "Solicitud no válida" : e.getReason()));
    }
}
