package Chambing.demo;

import java.util.List;

import org.bson.Document;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import Chambing.demo.Model.Notificacion;
import Chambing.demo.Model.Postulacion;
import Chambing.demo.service.NotificacionService;

class NotificacionServiceTest {
    @Test void outboxUsaIdEmbebidoCorrectoYReintentoNoRestableceLectura() {
        var mongo = mock(MongoTemplate.class);
        @SuppressWarnings("unchecked")
        com.mongodb.client.MongoCollection<Document> collection = mock(com.mongodb.client.MongoCollection.class);
        when(mongo.getCollection("postulaciones")).thenReturn(collection);
        var service = new NotificacionService(mongo);
        var p = new Postulacion(); p.setIdPostulacion("p");
        service.agregar(p, "USER:u", "Cambio", "/proposals-states?postulacion=p", "cambio_fase");
        when(mongo.find(any(Query.class), eq(Postulacion.class))).thenReturn(List.of(p));
        service.entregarPendientes();
        verify(mongo).upsert(any(Query.class), argThat(update -> update.getUpdateObject().get("$setOnInsert", Document.class).getBoolean("leida").equals(false)
            && !update.getUpdateObject().containsKey("$set")), eq(Notificacion.class));
        verify(collection).updateOne(any(org.bson.conversions.Bson.class), argThat((org.bson.conversions.Bson update) -> ((Document) update).get("$pull", Document.class)
            .get("notificacionesPendientes", Document.class).getString("_id").equals(p.getNotificacionesPendientes().getFirst().id())));
    }

    @Test void falloDeEntregaConservaPendientesParaReintento() {
        var mongo = mock(MongoTemplate.class);
        var service = new NotificacionService(mongo);
        var p = new Postulacion(); p.setIdPostulacion("p");
        service.agregar(p, "USER:u", "Cambio", "/proposals-states?postulacion=p", "cambio_fase");
        when(mongo.find(any(Query.class), eq(Postulacion.class))).thenReturn(List.of(p));
        when(mongo.upsert(any(Query.class), any(Update.class), eq(Notificacion.class))).thenThrow(new IllegalStateException("Sin conexión"));
        assertDoesNotThrow(service::entregarPendientes);
        verify(mongo, never()).updateFirst(any(Query.class), any(Update.class), eq(Postulacion.class));
    }

    @Test void mongoSerializaIdDeNotificacionEmbebidaComoId() throws Exception {
        var context = new MongoMappingContext();
        context.setSimpleTypeHolder(org.springframework.data.mongodb.core.convert.MongoCustomConversions
            .create(adapter -> {}).getSimpleTypeHolder());
        context.afterPropertiesSet();
        var converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context);
        converter.afterPropertiesSet();
        var n = new Notificacion("n", "USER:u", "Mensaje", "/proposals-states", "p", null, false, "cambio_fase");
        var doc = new Document();
        converter.write(n, doc);
        assertEquals("n", doc.getString("_id"));
    }
}