package Chambing.demo.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;

@Document(collection = "ciudades")
@CompoundIndex(name = "ciudad_por_departamento", def = "{'departamentoId':1,'clave':1}", unique = true)
public record Ciudad(@Id String id, String nombre, String clave, String departamentoId, boolean activo) {}
