package Chambing.demo;

import Chambing.demo.route.RutasController;
import Chambing.demo.security.AdminSessionConfig;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminSessionConfigTest {
    AnnotationConfigWebApplicationContext context;
    MockMvc mvc;

    @Configuration
    @EnableWebMvc
    @Import({AdminSessionConfig.class, RutasController.class, ApiDePrueba.class})
    static class Config {
        @Bean InternalResourceViewResolver views() {
            return new InternalResourceViewResolver("/templates/", ".html");
        }
    }
    @RestController
    static class ApiDePrueba {
        @GetMapping("/admin/usuarios") public Object usuarios() { return Map.of("total", 0); }
        @PostMapping("/admin/login") public Object login() { return Map.of("status", "login accesible"); }
    }
    @BeforeEach void setup() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(Config.class); context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }
    @AfterEach void cerrar() { context.close(); }

    @Test void formularioEsPublicoConYSinBarraFinal() throws Exception {
        for (String ruta : new String[]{"/admin", "/admin/"})
            mvc.perform(get(ruta)).andExpect(status().isOk()).andExpect(view().name("login-admin"));
    }
    @Test void panelSinSesionRedirigeAlFormulario() throws Exception {
        for (String ruta : new String[]{"/admin/panel", "/admin/panel/"})
            mvc.perform(get(ruta)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin"));
    }
    @Test void panelNoAceptaBanderaFalsaYSiAceptaSesionAdministrativa() throws Exception {
        var session = new MockHttpSession(); session.setAttribute("adminLogged", false);
        mvc.perform(get("/admin/panel").session(session)).andExpect(redirectedUrl("/admin"));
        session.setAttribute("adminLogged", true);
        mvc.perform(get("/admin/panel").session(session)).andExpect(status().isOk()).andExpect(view().name("panel"));
    }
    @Test void apiSigueProtegidaPeroLoginEsAccesible() throws Exception {
        mvc.perform(get("/admin/usuarios")).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Inicia sesión como administrador"));
        mvc.perform(post("/admin/login")).andExpect(status().isOk());
        var session = new MockHttpSession(); session.setAttribute("adminLogged", true);
        mvc.perform(get("/admin/usuarios").session(session)).andExpect(status().isOk());
    }
}
