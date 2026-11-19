package Chambing.demo.controller;

import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Chambing.demo.Model.Empresa;
import Chambing.demo.Model.Mensaje;
import Chambing.demo.Model.Usuario;
import Chambing.demo.repository.EmpresaRepository;
import Chambing.demo.repository.MensajeRepository;
import Chambing.demo.repository.UsuarioRepository;

@RestController
@RequestMapping("/api/mensajes")
public class MensajeController {

    private final MensajeRepository mensajes;
    private final UsuarioRepository usuarios;
    private final EmpresaRepository empresas;

    public MensajeController(MensajeRepository mensajes, UsuarioRepository usuarios, EmpresaRepository empresas) {
        this.mensajes = mensajes;
        this.usuarios = usuarios;
        this.empresas = empresas;
    }

    @GetMapping
    public ResponseEntity<?> bandeja(@AuthenticationPrincipal UserDetails principal) {
        String yo = actor(principal);
        if (yo == null) return ResponseEntity.status(401).body("No autenticado");
        return ResponseEntity.ok(mensajes.findByDeOrParaOrderByFechaAsc(yo, yo));
    }

    @PostMapping
    public ResponseEntity<?> enviar(@RequestBody Mensaje entrada, @AuthenticationPrincipal UserDetails principal) {
        String yo = actor(principal);
        if (yo == null) return ResponseEntity.status(401).body("No autenticado");
        if (entrada.getPara() == null || entrada.getTexto() == null || entrada.getTexto().isBlank()) {
            return ResponseEntity.badRequest().body("Indica destinatario (para) y texto");
        }
        if (entrada.getTexto().length() > 2000) {
            return ResponseEntity.badRequest().body("El mensaje admite hasta 2000 caracteres");
        }
        Mensaje m = new Mensaje();
        m.setDe(yo);
        m.setPara(entrada.getPara());
        m.setPropuestaId(entrada.getPropuestaId());
        m.setTexto(entrada.getTexto().trim());
        m.setFecha(LocalDateTime.now());
        m.setLeido(false);
        return ResponseEntity.ok(mensajes.save(m));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> borrar(@PathVariable String id, @AuthenticationPrincipal UserDetails principal) {
        String yo = actor(principal);
        return mensajes.findById(id).map(m -> {
            if (!yo.equals(m.getDe()) && !yo.equals(m.getPara())) {
                return ResponseEntity.status(403).body((Object) "No puedes borrar este mensaje");
            }
            mensajes.delete(m);
            return ResponseEntity.ok((Object) "Mensaje eliminado");
        }).orElse(ResponseEntity.notFound().build());
    }

    private String actor(UserDetails principal) {
        if (principal == null) return null;
        Usuario u = usuarios.findByEmail(principal.getUsername()).orElse(null);
        if (u != null) return "USER:" + u.getIdUsuario();
        Empresa e = empresas.findByCorreo(principal.getUsername()).orElse(null);
        if (e != null) return "EMPRESA:" + e.getIdEmpresa();
        return null;
    }
}
