package Chambing.demo.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Chambing.demo.service.DestacadaService;

@RestController
public class DestacadaController {

    private final DestacadaService destacadas;

    public DestacadaController(DestacadaService destacadas) {
        this.destacadas = destacadas;
    }

    @GetMapping("/api/destacadas/planes")
    public ResponseEntity<?> planes() {
        return ResponseEntity.ok(destacadas.planes());
    }

    @PostMapping("/empresa/{idEmpresa}/propuestas/{idPropuesta}/destacar")
    public ResponseEntity<?> destacar(@PathVariable String idEmpresa,
                                      @PathVariable String idPropuesta,
                                      @RequestBody Map<String, String> body,
                                      @AuthenticationPrincipal UserDetails principal) {
        if (principal == null) return ResponseEntity.status(401).body("No autenticado");
        try {
            return ResponseEntity.ok(destacadas.iniciar(idEmpresa, idPropuesta, body.get("plan")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
