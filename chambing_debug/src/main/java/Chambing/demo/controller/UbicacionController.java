package Chambing.demo.controller;

import Chambing.demo.service.UbicacionService;
import org.springframework.web.bind.annotation.*;

/** Las rutas /admin/** están protegidas por AdminSessionConfig. */
@RestController
public class UbicacionController {
    public record Datos(String nombre, String departamentoId, Boolean activo) {}
    private final UbicacionService servicio;
    public UbicacionController(UbicacionService servicio) { this.servicio = servicio; }
    @GetMapping("/api/catalogos/departamentos") public Object departamentosPublicos() { return servicio.departamentos(false); }
    @GetMapping("/api/catalogos/ciudades") public Object ciudadesPublicas(@RequestParam String departamentoId) { return servicio.ciudades(departamentoId, false); }
    @GetMapping("/admin/departamentos/opciones") public Object departamentos() { return servicio.departamentos(true); }
    @GetMapping("/admin/ciudades/opciones") public Object ciudades(@RequestParam String departamentoId) { return servicio.ciudades(departamentoId, true); }
    @GetMapping("/admin/departamentos") public Object paginaDepartamentos(@RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="10") int size, @RequestParam(defaultValue="") String q) { return servicio.pagina(false, page, size, q, null); }
    @GetMapping("/admin/ciudades") public Object paginaCiudades(@RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="10") int size, @RequestParam(defaultValue="") String q,
        @RequestParam(required=false) String departamentoId) { return servicio.pagina(true, page, size, q, departamentoId); }
    @PostMapping("/admin/departamentos") public Object crearDepartamento(@RequestBody Datos datos) { return servicio.guardarDepartamento(null, datos.nombre(), datos.activo()); }
    @PutMapping("/admin/departamentos/{id}") public Object editarDepartamento(@PathVariable String id, @RequestBody Datos datos) { return servicio.guardarDepartamento(id, datos.nombre(), datos.activo()); }
    @DeleteMapping("/admin/departamentos/{id}") public void eliminarDepartamento(@PathVariable String id) { servicio.eliminarDepartamento(id); }
    @PostMapping("/admin/ciudades") public Object crearCiudad(@RequestBody Datos datos) { return servicio.guardarCiudad(null, datos.nombre(), datos.departamentoId(), datos.activo()); }
    @PutMapping("/admin/ciudades/{id}") public Object editarCiudad(@PathVariable String id, @RequestBody Datos datos) { return servicio.guardarCiudad(id, datos.nombre(), datos.departamentoId(), datos.activo()); }
    @DeleteMapping("/admin/ciudades/{id}") public void eliminarCiudad(@PathVariable String id) { servicio.eliminarCiudad(id); }
}
