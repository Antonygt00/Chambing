package Chambing.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.web.server.ResponseStatusException;

import Chambing.demo.Model.FaseSeleccion;
import Chambing.demo.Model.Postulacion;
import Chambing.demo.Model.Propuesta;
import Chambing.demo.repository.PostulacionRepository;
import Chambing.demo.service.NotificacionService;
import Chambing.demo.service.SeleccionService;

class SeleccionServiceTest {
    PostulacionRepository repo;
    NotificacionService avisos;
    SeleccionService service;
    Postulacion p;
    @BeforeEach void setup() {
        repo = mock(PostulacionRepository.class); avisos = mock(NotificacionService.class);
        service = new SeleccionService(repo, avisos);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        p = new Postulacion(); p.setIdPostulacion("p"); p.setUsuarioId("u");
        p.setPropuestaId("o"); p.setPropuestaTitulo("Desarrollador"); p.setUsuarioNombre("Ana");
        p.setEstadoPostulacion("Pendiente"); p.setFaseActual(FaseSeleccion.INSCRIPCION); p.setVersion(1L);
    }
    @Test void inscripcionGuardaHistorialYAmbosAvisos() {
        Propuesta oferta = new Propuesta(); oferta.setEmpresaId("e");
        service.inscribir(p,oferta);
        assertNotNull(p.getIdPostulacion()); assertEquals("u:o",p.getInscripcionClave());
        assertEquals(1,p.getHistorial().size()); assertEquals(FaseSeleccion.INSCRIPCION,p.getFaseActual());
                verify(avisos).agregar(eq(p), eq("USER:u"), contains("Recibimos"), contains("proposals-states"), eq("postulacion_recibida"));
        verify(avisos).agregar(eq(p), eq("EMPRESA:e"), contains("Ana"), contains("proposals-answer"), eq("nueva_postulacion_empresa"));
        verify(repo).save(p);
    }
    @Test void cambiosRegistranHistoriaYSeleccionMantieneCompatibilidad() {
        service.cambiar(p,FaseSeleccion.REVISION_DOCUMENTAL,null,"Revisando hoja de vida","Privado","e",1L);
        assertEquals("Pendiente",p.getEstadoPostulacion());
        assertEquals(FaseSeleccion.INSCRIPCION,p.getHistorial().getFirst().faseAnterior());
        service.cambiar(p,FaseSeleccion.SELECCIONADO,null,"Bienvenido","","e",1L);
        assertEquals("Aprobado",p.getEstadoPostulacion()); assertEquals(2,p.getHistorial().size());
        verify(repo,times(2)).save(p);
    }
    @Test void repetirCambioNoGeneraHistorialNiAvisos() {
        service.cambiar(p,FaseSeleccion.INSCRIPCION,null,"","","e",1L);
        assertTrue(p.getHistorial().isEmpty()); verifyNoInteractions(avisos,repo);
    }
    @Test void rechazoConservaFaseYExigeExplicacion() {
        p.setFaseActual(FaseSeleccion.REVISION_DOCUMENTAL);
        assertThrows(ResponseStatusException.class, () -> service.cambiar(p,null,"Rechazado","","","e",1L));
        service.cambiar(p,null,"Rechazado","Falta el requisito de experiencia","Privado","e",1L);
        assertEquals(FaseSeleccion.REVISION_DOCUMENTAL,p.getFaseActual()); assertEquals("Rechazado",p.getEstadoPostulacion());
    }
    @Test void retrocesoYReaperturaRequierenMotivo() {
        p.setFaseActual(FaseSeleccion.POTENCIAL_CANDIDATO);
        assertThrows(ResponseStatusException.class, () -> service.cambiar(p,FaseSeleccion.INSCRIPCION,null,"","","e",1L));
        p.setEstadoPostulacion("Rechazado");
        assertThrows(ResponseStatusException.class, () -> service.cambiar(p,FaseSeleccion.POTENCIAL_CANDIDATO,null,"","","e",1L));
        service.cambiar(p,FaseSeleccion.POTENCIAL_CANDIDATO,null,"Reabrimos la vacante","","e",1L);
        assertEquals("Pendiente",p.getEstadoPostulacion());
    }
    @Test void versionDesactualizadaNoSobreescribeDatos() {
        var error = assertThrows(ResponseStatusException.class, () -> service.cambiar(p,FaseSeleccion.REVISION_DOCUMENTAL,null,"","","e",0L));
        assertEquals(409,error.getStatusCode().value()); verifyNoInteractions(repo,avisos);
    }
    @Test void rechazaResultadosYObservacionesInvalidos() {
        assertThrows(ResponseStatusException.class, () -> service.cambiar(p,null,"Otro","","","e",1L));
        assertThrows(ResponseStatusException.class, () -> service.cambiar(p,FaseSeleccion.REVISION_DOCUMENTAL,null,"a".repeat(2001),"","e",1L));
        verifyNoInteractions(repo,avisos);
    }
    @Test void vistaUsuarioNuncaExponeNotaInternaActorNiOutbox() {
        service.cambiar(p,FaseSeleccion.REVISION_DOCUMENTAL,null,"Visible","Secreto","e",1L);
        var publico = service.vista(p,false);
        assertFalse(publico.toString().contains("Secreto")); assertFalse(publico.toString().contains("notaInterna"));
        assertFalse(publico.containsKey("notificacionesPendientes"));
        assertTrue(service.vista(p,true).toString().contains("Secreto"));
    }
    @Test void legacyAprobadoSeRepresentaSeleccionadoSinInventarHistorial() {
        p.setFaseActual(null); p.setEstadoPostulacion("Aprobada");
        assertEquals(FaseSeleccion.SELECCIONADO,service.vista(p,false).get("faseActual"));
        assertEquals("Aprobado",service.vista(p,false).get("estadoPostulacion")); assertTrue(p.getHistorial().isEmpty());
    }
}
