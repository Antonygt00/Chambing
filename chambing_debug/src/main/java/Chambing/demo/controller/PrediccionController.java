package Chambing.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Chambing.demo.Model.Usuario;
import Chambing.demo.repository.UsuarioRepository;
import Chambing.demo.service.PrediccionService;

@RestController
@RequestMapping("/api/postulaciones")
public class PrediccionController {

    private final PrediccionService prediccion;
    private final UsuarioRepository usuarios;

    public PrediccionController(PrediccionService prediccion, UsuarioRepository usuarios) {
        this.prediccion = prediccion;
        this.usuarios = usuarios;
    }

    @GetMapping("/prediccion/{idPropuesta}")
    public ResponseEntity<?> predecir(@PathVariable String idPropuesta,
                                      @AuthenticationPrincipal UserDetails principal) {
        Usuario usuario = actual(principal);
        if (usuario == null) return ResponseEntity.status(401).body("Inicia sesión como candidato");
        return ResponseEntity.ok(prediccion.paraUsuario(usuario, idPropuesta));
    }

    @GetMapping("/recomendadas")
    public ResponseEntity<?> recomendadas(@AuthenticationPrincipal UserDetails principal) {
        Usuario usuario = actual(principal);
        if (usuario == null) return ResponseEntity.status(401).body("Inicia sesión como candidato");
        if (!usuario.isPerfilCompleto()) return ResponseEntity.status(403).body("Completa tu perfil profesional");
        return ResponseEntity.ok(prediccion.recomendadas(usuario, 5));
    }

    private Usuario actual(UserDetails principal) {
        if (principal == null) return null;
        return usuarios.findByEmail(principal.getUsername()).orElse(null);
    }
}
