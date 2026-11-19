package Chambing.demo;

import Chambing.demo.Model.*;
import Chambing.demo.repository.*;
import Chambing.demo.service.AccesoSeleccion;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccesoSeleccionTest {
    UsuarioRepository usuarios = mock(UsuarioRepository.class);
    EmpresaRepository empresas = mock(EmpresaRepository.class);
    PropuestaRepository ofertas = mock(PropuestaRepository.class);
    PostulacionRepository postulaciones = mock(PostulacionRepository.class);
    AccesoSeleccion acceso = new AccesoSeleccion(usuarios,empresas,ofertas,postulaciones);
    Usuario u; Empresa e; Postulacion p;
    @BeforeEach void setup() {
        u = new Usuario(); u.setIdUsuario("u"); u.setEmail("u@mail.com");
        e = new Empresa(); e.setIdEmpresa("e"); e.setCorreo("e@mail.com");
        p = new Postulacion(); p.setIdPostulacion("p"); p.setUsuarioId("u"); p.setPropuestaId("o");
        Propuesta o = new Propuesta(); o.setEmpresaId("e");
        when(usuarios.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        when(empresas.findByCorreo(e.getCorreo())).thenReturn(Optional.of(e));
        when(postulaciones.findById("p")).thenReturn(Optional.of(p));
        when(ofertas.findById("o")).thenReturn(Optional.of(o));
    }
    @Test void propietarioUsuarioPuedeLeerPeroNoGestionar() {
        assertFalse(acceso.puedeLeer(u,p));
        assertThrows(ResponseStatusException.class, () -> acceso.gestionar(u,"e","p"));
        assertThrows(ResponseStatusException.class, () -> acceso.usuario(u,"otra"));
    }
    @Test void empresaSoloPuedeVerPostulantesDeSuOferta() {
        assertTrue(acceso.puedeLeer(e,p));
        Propuesta otra = new Propuesta(); otra.setEmpresaId("otra");
        when(ofertas.findById("o")).thenReturn(Optional.of(otra));
        assertThrows(ResponseStatusException.class, () -> acceso.gestionar(e,"e","p"));
    }
    @Test void rechazaCuentaDeshabilitadaYAislaDestinatarios() {
        assertEquals("USER:u",acceso.destinatario(u)); assertEquals("EMPRESA:e",acceso.destinatario(e));
        e.setEnabled(false);
        assertThrows(ResponseStatusException.class, () -> acceso.destinatario(e));
        assertThrows(ResponseStatusException.class, () -> acceso.empresa(e,"e"));
        assertThrows(ResponseStatusException.class, () -> acceso.puedeLeer(null,p));
    }
    @Test void compartirCorreoNoPermiteCambiarTipoDeCuenta() {
        u.setEmail(e.getCorreo());
        assertThrows(ResponseStatusException.class, () -> acceso.empresa(u,"e"));
    }
}
