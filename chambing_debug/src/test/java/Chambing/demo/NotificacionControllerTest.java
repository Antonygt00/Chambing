package Chambing.demo;

import Chambing.demo.Model.*;
import Chambing.demo.controller.NotificacionController;
import Chambing.demo.service.AccesoSeleccion;
import com.mongodb.client.result.UpdateResult;
import java.util.List;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificacionControllerTest {
    MongoTemplate mongo = mock(MongoTemplate.class);
    AccesoSeleccion acceso = mock(AccesoSeleccion.class);
    NotificacionController controller = new NotificacionController(mongo,acceso);
    Usuario u = new Usuario();
    @BeforeEach void setup() { when(acceso.destinatario(u)).thenReturn("USER:u"); }
    @Test void listadoYContadorSiempreFiltranPorDestinatario() {
        when(mongo.find(any(Query.class),eq(Notificacion.class))).thenReturn(List.of());
        controller.listar(u,0,20); controller.pendientes(u);
        var queries = ArgumentCaptor.forClass(Query.class);
        verify(mongo,times(2)).count(queries.capture(),eq(Notificacion.class));
        for(Query q : queries.getAllValues()) assertEquals("USER:u",q.getQueryObject().getString("destinatario"));
        assertEquals(false,queries.getAllValues().get(1).getQueryObject().getBoolean("leida"));
    }
    @Test void noPermiteMarcarNotificacionDeOtraCuenta() {
        when(mongo.updateFirst(any(Query.class),any(Update.class),eq(Notificacion.class)))
            .thenReturn(UpdateResult.acknowledged(0,0L,null));
        var error = assertThrows(ResponseStatusException.class,() -> controller.leer("ajena",u));
        assertEquals(404,error.getStatusCode().value());
        verify(mongo).updateFirst(argThat(q -> "USER:u".equals(q.getQueryObject().getString("destinatario"))
            && "ajena".equals(q.getQueryObject().getString("_id"))),any(Update.class),eq(Notificacion.class));
    }
    @Test void marcarTodasSoloActualizaLasPropiasSinLeer() {
        controller.leerTodas(u);
        verify(mongo).updateMulti(argThat(q -> "USER:u".equals(q.getQueryObject().getString("destinatario"))
            && Boolean.FALSE.equals(q.getQueryObject().getBoolean("leida"))),any(Update.class),eq(Notificacion.class));
    }
    @Test void paginacionInvalidaNoConsultaBase() {
        assertThrows(ResponseStatusException.class,() -> controller.listar(u,-1,20));
        assertThrows(ResponseStatusException.class,() -> controller.listar(u,0,101));
        assertThrows(ResponseStatusException.class,() -> controller.listar(u,0,0));
        verifyNoInteractions(mongo);
    }
}
