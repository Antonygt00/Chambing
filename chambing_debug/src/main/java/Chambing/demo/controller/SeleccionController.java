package Chambing.demo.controller;

import Chambing.demo.Model.*;
import Chambing.demo.service.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

@RestController
public class SeleccionController {
    public record Cambio(FaseSeleccion fase, String resultado, String observacion, String notaInterna, Long version) {}
    private final AccesoSeleccion acceso;
    private final SeleccionService seleccion;
    public SeleccionController(AccesoSeleccion acceso, SeleccionService seleccion) {
        this.acceso = acceso; this.seleccion = seleccion;
    }
    @GetMapping("/api/postulaciones/{id}")
    public Object detalle(@PathVariable String id, @AuthenticationPrincipal UserDetails principal) {
        Postulacion p = acceso.postulacion(id);
        return seleccion.vista(p, acceso.puedeLeer(principal, p));
    }
    @PutMapping("/empresa/{idEmpresa}/postulaciones/{id}/fase")
    public Object cambiar(@PathVariable String idEmpresa, @PathVariable String id,
            @RequestBody Cambio cambio, @AuthenticationPrincipal UserDetails principal) {
        Postulacion p = acceso.gestionar(principal, idEmpresa, id);
        if (cambio.fase() == null)
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Elige una fase");
        return seleccion.vista(seleccion.cambiar(p, cambio.fase(), cambio.resultado(),
            cambio.observacion(), cambio.notaInterna(), idEmpresa, cambio.version()), true);
    }
}
