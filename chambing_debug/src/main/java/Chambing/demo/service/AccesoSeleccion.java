package Chambing.demo.service;

import Chambing.demo.Model.*;
import Chambing.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
public class AccesoSeleccion {
    private final UsuarioRepository usuarios;
    private final EmpresaRepository empresas;
    private final PropuestaRepository propuestas;
    private final PostulacionRepository postulaciones;
    public AccesoSeleccion(UsuarioRepository usuarios, EmpresaRepository empresas,
            PropuestaRepository propuestas, PostulacionRepository postulaciones) {
        this.usuarios = usuarios; this.empresas = empresas;
        this.propuestas = propuestas; this.postulaciones = postulaciones;
    }
    public Usuario usuario(UserDetails principal, String id) {
        if (principal == null) throw new ResponseStatusException(UNAUTHORIZED, "Inicia sesión");
        if (!(principal instanceof Usuario)) throw new ResponseStatusException(FORBIDDEN, "Se requiere una cuenta de usuario");
        Usuario u = usuarios.findByEmail(principal.getUsername())
            .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Se requiere una cuenta de usuario"));
        if (!u.isEnabled() || !u.getIdUsuario().equals(id))
            throw new ResponseStatusException(FORBIDDEN, "Solo puedes consultar tus postulaciones");
        return u;
    }
    public Empresa empresa(UserDetails principal, String id) {
        if (principal == null) throw new ResponseStatusException(UNAUTHORIZED, "Inicia sesión");
        if (!(principal instanceof Empresa)) throw new ResponseStatusException(FORBIDDEN, "Se requiere una cuenta de empresa");
        Empresa e = empresas.findByCorreo(principal.getUsername())
            .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Se requiere una cuenta de empresa"));
        if (!e.isEnabled() || !e.getIdEmpresa().equals(id))
            throw new ResponseStatusException(FORBIDDEN, "Solo puedes gestionar tus postulantes");
        return e;
    }
    public Postulacion postulacion(String id) {
        return postulaciones.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Postulación no encontrada"));
    }
    public Propuesta propuesta(String id) {
        return propuestas.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Oferta no encontrada"));
    }
    public Postulacion gestionar(UserDetails principal, String empresaId, String id) {
        empresa(principal, empresaId);
        Postulacion p = postulacion(id);
        if (!empresaId.equals(propuesta(p.getPropuestaId()).getEmpresaId()))
            throw new ResponseStatusException(FORBIDDEN, "Esta postulación pertenece a otra empresa");
        return p;
    }
    public boolean puedeLeer(UserDetails principal, Postulacion p) {
        if (principal == null) throw new ResponseStatusException(UNAUTHORIZED, "Inicia sesión");
        // La clase del principal distingue cuentas que compartan el mismo correo.
        if (principal instanceof Usuario) { usuario(principal, p.getUsuarioId()); return false; }
        if (principal instanceof Empresa e) {
            gestionar(principal, e.getIdEmpresa(), p.getIdPostulacion()); return true;
        }
        throw new ResponseStatusException(FORBIDDEN, "No puedes consultar esta postulación");
    }
    public String destinatario(UserDetails principal) {
        if (principal instanceof Usuario u && u.isEnabled()) return "USER:" + u.getIdUsuario();
        if (principal instanceof Empresa e && e.isEnabled()) return "EMPRESA:" + e.getIdEmpresa();
        throw new ResponseStatusException(FORBIDDEN, "Inicia sesión con tu cuenta");
    }
}
