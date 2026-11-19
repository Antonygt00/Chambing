package Chambing.demo.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

@Document(collection = "departamentos")
public record Departamento(@Id String id, String nombre, @Indexed(name = "departamento_clave", unique = true) String clave, boolean activo) {}
