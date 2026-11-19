package Chambing.demo.Model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "notificaciones")
@CompoundIndex(name = "bandeja_destinatario", def = "{'destinatario':1,'fecha':-1}")
public record Notificacion(
        @Id String id,
        String destinatario,
        String mensaje,
        String enlace,
        String postulacionId,
        LocalDateTime fecha,
        boolean leida,
        String tipo
) {}