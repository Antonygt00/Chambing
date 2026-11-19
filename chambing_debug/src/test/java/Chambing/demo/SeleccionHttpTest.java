package Chambing.demo;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import Chambing.demo.Model.CambioFase;
import Chambing.demo.Model.DocumentoPostulacion;
import Chambing.demo.Model.Empresa;
import Chambing.demo.Model.FaseSeleccion;
import Chambing.demo.Model.Postulacion;
import Chambing.demo.Model.Propuesta;
import Chambing.demo.Model.Usuario;
import Chambing.demo.controller.DocumentoController;
import Chambing.demo.controller.ErroresSeleccion;
import Chambing.demo.controller.SeleccionController;
import Chambing.demo.repository.EmpresaRepository;
import Chambing.demo.repository.PostulacionRepository;
import Chambing.demo.repository.PropuestaRepository;
import Chambing.demo.repository.UsuarioRepository;
import Chambing.demo.service.AccesoSeleccion;
import Chambing.demo.service.NotificacionService;
import Chambing.demo.service.SeleccionService;

class SeleccionHttpTest {
    UsuarioRepository usuarios = mock(UsuarioRepository.class);
    EmpresaRepository empresas = mock(EmpresaRepository.class);
    PropuestaRepository ofertas = mock(PropuestaRepository.class);
    PostulacionRepository repo = mock(PostulacionRepository.class);
    GridFsTemplate grid = mock(GridFsTemplate.class);
    NotificacionService avisos = mock(NotificacionService.class);
    AccesoSeleccion acceso = new AccesoSeleccion(usuarios,empresas,ofertas,repo);
    SeleccionService seleccion = new SeleccionService(repo,avisos);
    MockMvc mvc;
    Usuario u; Empresa e; Postulacion p; Propuesta oferta;
    @BeforeEach void setup() {
        u = new Usuario(); u.setIdUsuario("u"); u.setEmail("u@mail.com");
        e = new Empresa(); e.setIdEmpresa("e"); e.setCorreo("e@mail.com");
        p = new Postulacion(); p.setIdPostulacion("p"); p.setUsuarioId("u"); p.setPropuestaId("o");
        p.setFaseActual(FaseSeleccion.INSCRIPCION); p.setEstadoPostulacion("Pendiente"); p.setVersion(0L);
        oferta = new Propuesta(); oferta.setEmpresaId("e"); oferta.setEstado("Activa");
        when(usuarios.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        when(empresas.findByCorreo(e.getCorreo())).thenReturn(Optional.of(e));
        when(repo.findById("p")).thenReturn(Optional.of(p));
        when(ofertas.findById("o")).thenReturn(Optional.of(oferta));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        mvc = MockMvcBuilders.standaloneSetup(new SeleccionController(acceso,seleccion),
            new DocumentoController(acceso,seleccion,repo,grid,avisos))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .setControllerAdvice(new ErroresSeleccion()).build();
        login(u);
    }
    void login(org.springframework.security.core.userdetails.UserDetails principal) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,java.util.List.of()));
    }
    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }
    @Test void detalleUsuarioOcultaNotasPeroEmpresaLasVe() throws Exception {
        p.getHistorial().add(new CambioFase(null,FaseSeleccion.INSCRIPCION,"Pendiente",null,"e","Visible","Privado"));
        mvc.perform(get("/api/postulaciones/p")).andExpect(status().isOk())
            .andExpect(jsonPath("$.historial[0].observacion").value("Visible"))
            .andExpect(jsonPath("$.historial[0].notaInterna").doesNotExist())
            .andExpect(jsonPath("$.notificacionesPendientes").doesNotExist());
        login(e);
        mvc.perform(get("/api/postulaciones/p")).andExpect(status().isOk())
            .andExpect(jsonPath("$.historial[0].notaInterna").value("Privado"));
    }
    @Test void otroUsuarioNoPuedeConsultarNiDescargar() throws Exception {
        p.setUsuarioId("otro");
        mvc.perform(get("/api/postulaciones/p")).andExpect(status().isForbidden());
        mvc.perform(get("/api/postulaciones/p/documentos/abc")).andExpect(status().isForbidden());
        verifyNoInteractions(grid);
    }
    @Test void empresaAjenaNoPuedeCambiarFase() throws Exception {
        login(e); oferta.setEmpresaId("otra");
        mvc.perform(put("/empresa/e/postulaciones/p/fase").contentType("application/json")
            .content("{\"fase\":\"SELECCIONADO\",\"version\":0}"))
            .andExpect(status().isForbidden());
        verify(repo,never()).save(any());
    }
    @Test void cambioValidoYVersionDesactualizada() throws Exception {
        login(e);
        mvc.perform(put("/empresa/e/postulaciones/p/fase").contentType("application/json")
            .content("{\"fase\":\"REVISION_DOCUMENTAL\",\"version\":0,\"notaInterna\":\"Privado\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.faseActual").value("REVISION_DOCUMENTAL"));
        mvc.perform(put("/empresa/e/postulaciones/p/fase").contentType("application/json")
            .content("{\"fase\":\"SELECCIONADO\",\"version\":5}"))
            .andExpect(status().isConflict());
    }
    @Test void rechazaFaseInexistenteYFaltaDeFase() throws Exception {
        login(e);
        mvc.perform(put("/empresa/e/postulaciones/p/fase").contentType("application/json")
            .content("{\"fase\":\"INVENTADA\"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/empresa/e/postulaciones/p/fase").contentType("application/json")
            .content("{}")).andExpect(status().isBadRequest());
        verify(repo,never()).save(any());
    }
    @Test void rechazaDocumentoQueNoEsPdf() throws Exception {
        var archivo = new MockMultipartFile("archivo","archivo.pdf","application/pdf","No soy PDF".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/postulaciones/p/documentos").file(archivo)).andExpect(status().isBadRequest());
        verifyNoInteractions(grid);
    }
    @Test void pdfSeGuardaYGeneraAvisoParaEmpresa() throws Exception {
        var archivo = new MockMultipartFile("archivo","cv.pdf","application/pdf","%PDF-1.7\ncontenido".getBytes(StandardCharsets.UTF_8));
        when(grid.store(any(java.io.InputStream.class),eq("cv.pdf"),eq("application/pdf"))).thenReturn(new ObjectId());
        mvc.perform(multipart("/api/postulaciones/p/documentos").file(archivo))
            .andExpect(status().isOk()).andExpect(jsonPath("$.documentos[0].nombre").value("cv.pdf"));
        verify(avisos).agregar(eq(p), eq("EMPRESA:e"), contains("documentación"), contains("proposals-answer"), eq("documento_adjunto"));
    }
    @Test void rechazaPdfGrandeExcesoDeArchivosYProcesoFinalizado() throws Exception {
        var grande = new MockMultipartFile("archivo","cv.pdf","application/pdf",new byte[5*1024*1024+1]);
        mvc.perform(multipart("/api/postulaciones/p/documentos").file(grande)).andExpect(status().isBadRequest());
        var archivo = new MockMultipartFile("archivo","cv.pdf","application/pdf","%PDF-1.7".getBytes(StandardCharsets.UTF_8));
        for(int i=0;i<5;i++) p.getDocumentos().add(new DocumentoPostulacion("d"+i,"cv.pdf",10,null));
        mvc.perform(multipart("/api/postulaciones/p/documentos").file(archivo)).andExpect(status().isBadRequest());
        p.getDocumentos().clear(); p.setEstadoPostulacion("Rechazado");
        mvc.perform(multipart("/api/postulaciones/p/documentos").file(archivo)).andExpect(status().isConflict());
        verifyNoInteractions(grid);
    }
    @Test void conflictoAlGuardarLimpiaArchivoHuerfano() throws Exception {
        var archivo = new MockMultipartFile("archivo","cv.pdf","application/pdf","%PDF-1.7".getBytes(StandardCharsets.UTF_8));
        when(grid.store(any(java.io.InputStream.class),anyString(),eq("application/pdf"))).thenReturn(new ObjectId());
        when(repo.save(any())).thenThrow(new org.springframework.dao.OptimisticLockingFailureException("Concurrente"));
        mvc.perform(multipart("/api/postulaciones/p/documentos").file(archivo)).andExpect(status().isConflict());
        verify(grid).delete(any(org.springframework.data.mongodb.core.query.Query.class));
    }
}
