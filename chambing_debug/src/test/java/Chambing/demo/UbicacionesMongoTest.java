package Chambing.demo;

import Chambing.demo.Model.*;
import Chambing.demo.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.*;
import java.util.*;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

/** Solo se ejecuta contra un mongod de prueba explícitamente indicado, nunca application.properties. */
@EnabledIfSystemProperty(named="ubicaciones.mongo.port", matches="\\d+")
class UbicacionesMongoTest {
    MongoClient client;
    MongoTemplate mongo;
    UbicacionService service;
    @BeforeEach void setup() {
        client=MongoClients.create("mongodb://127.0.0.1:"+System.getProperty("ubicaciones.mongo.port")+"/?serverSelectionTimeoutMS=3000");
        mongo=new MongoTemplate(client,"chambing_ubicaciones_test_"+UUID.randomUUID().toString().replace("-",""));
        service=new UbicacionService(mongo);
    }
    @AfterEach void limpiar() { if(mongo!=null)mongo.getDb().drop(); if(client!=null)client.close(); }
    Direccion direccion(Departamento d,Ciudad c) {
        Direccion dir=new Direccion();dir.setCalle("Calle de prueba");dir.setDepartamentoId(d.id());dir.setCiudadId(c.id());return dir;
    }
    @Test void crudYFiltroDeCiudadPorDepartamento() {
        Departamento a=service.guardarDepartamento(null,"Bolívar",true), b=service.guardarDepartamento(null,"Atlántico",true);
        Ciudad ciudad=service.guardarCiudad(null,"Cartagena",a.id(),true);
        assertEquals(1,service.ciudades(a.id(),false).size()); assertTrue(service.ciudades(b.id(),false).isEmpty());
        assertEquals("Cartagena",service.validar(direccion(a,ciudad),null).getCiudad());
        service.guardarCiudad(ciudad.id(),"Cartagena de Indias",b.id(),true);
        assertTrue(service.ciudades(a.id(),false).isEmpty());assertEquals(b.id(),service.ciudad(ciudad.id()).departamentoId());
        service.eliminarCiudad(ciudad.id());service.eliminarDepartamento(a.id());
        assertEquals(1,service.departamentos(true).size());
    }
    @Test void duplicadosIgnoranAcentosEspaciosYMayusculasPeroCiudadPuedeRepetirseEnOtroDepartamento() {
        var d=service.guardarDepartamento(null,"Bolívar",true);
        assertThrows(ResponseStatusException.class,()->service.guardarDepartamento(null,"  BOLIVAR  ",true));
        var c=service.guardarCiudad(null,"San José",d.id(),true);
        assertThrows(ResponseStatusException.class,()->service.guardarCiudad(null,"san jose",d.id(),true));
        var otro=service.guardarDepartamento(null,"Otro departamento",true);
        assertNotNull(service.guardarCiudad(null,"San José",otro.id(),true));
        assertThrows(ResponseStatusException.class,()->service.validar(direccion(otro,c),null));
    }
    @Test void nombresAntiguosSeResuelvenYNoSeAceptanNombresInventados() {
        var d=service.guardarDepartamento(null,"Bolívar",true);var c=service.guardarCiudad(null,"Cartagena",d.id(),true);
        var dir=new Direccion();dir.setDepartamento("BOLIVAR");dir.setCiudad("cartagena");service.validar(dir,null);
        assertEquals(d.id(),dir.getDepartamentoId());assertEquals(c.id(),dir.getCiudadId());
        dir.setCiudadId(null);dir.setCiudad("No existe");assertThrows(ResponseStatusException.class,()->service.validar(dir,null));
    }
    @Test void ubicacionesUsadasNoSeEliminanYRenombrarOMoverActualizaSusDirecciones() {
        var d=service.guardarDepartamento(null,"Bolívar",true);var c=service.guardarCiudad(null,"Cartagena",d.id(),true);
        var dir=service.validar(direccion(d,c),null);
        Usuario u=new Usuario();u.setDireccion(dir);mongo.save(u);
        assertThrows(ResponseStatusException.class,()->service.eliminarCiudad(c.id()));
        assertThrows(ResponseStatusException.class,()->service.eliminarDepartamento(d.id()));
        service.guardarDepartamento(d.id(),"Departamento renombrado",true);
        assertEquals("Departamento renombrado",mongo.findById(u.getIdUsuario(),Usuario.class).getDireccion().getDepartamento());
        var otro=service.guardarDepartamento(null,"Atlántico",true);
        service.guardarCiudad(c.id(),"Ciudad renombrada",otro.id(),true);
        var actual=mongo.findById(u.getIdUsuario(),Usuario.class).getDireccion();
        assertEquals(otro.id(),actual.getDepartamentoId());assertEquals("Ciudad renombrada",actual.getCiudad());
    }
    @Test void desactivarConservaDireccionesYExcluyeOpcionesNuevas() {
        var d=service.guardarDepartamento(null,"Bolívar",true);var c=service.guardarCiudad(null,"Cartagena",d.id(),true);
        var anterior=service.validar(direccion(d,c),null);
        service.guardarDepartamento(d.id(),d.nombre(),false);
        assertTrue(service.departamentos(false).isEmpty());assertTrue(service.ciudades(d.id(),false).isEmpty());
        assertNotNull(service.validar(direccion(d,c),anterior));
        assertThrows(ResponseStatusException.class,()->service.validar(direccion(d,c),null));
    }
    @Test void migracionCreaColeccionesConservaDireccionesYNuncaReponeLoBorrado() throws Exception {
        mongo.getCollection("usuarios").insertOne(new Document("direccion",new Document("departamento","Bolivar").append("ciudad","Cartagena")));
        var migracion=new MigracionUbicaciones(mongo,new ObjectMapper());migracion.run(null);
        assertTrue(mongo.collectionExists("departamentos"));assertTrue(mongo.collectionExists("ciudades"));
        Document usuario=mongo.getCollection("usuarios").find().first();
        var dir=usuario.get("direccion",Document.class);assertNotNull(dir.getString("ciudadId"));
        assertEquals("Bolívar",dir.getString("departamento"));
        Departamento dep=service.departamentos(true).stream().filter(x->x.nombre().equals("Amazonas")).findFirst().orElseThrow();
        for(Ciudad ciudad:service.ciudades(dep.id(),true))service.eliminarCiudad(ciudad.id());
        service.eliminarDepartamento(dep.id());migracion.run(null);
        assertThrows(ResponseStatusException.class,()->service.departamento(dep.id()));
        assertThrows(org.springframework.dao.DuplicateKeyException.class,()->mongo.insert(new Departamento("duplicado","BOLIVAR","bolivar",true)));
    }
    @Test void busquedaPaginadaYManejoDeParametrosInvalidos() {
        var d=service.guardarDepartamento(null,"Bolívar",true);service.guardarCiudad(null,"Cartagena",d.id(),true);
        var pagina=(org.springframework.data.domain.Page<?>)service.pagina(true,0,10,"cart",d.id());
        assertEquals(1,pagina.getTotalElements());assertTrue(pagina.getContent().getFirst().toString().contains("Bolívar"));
        assertThrows(ResponseStatusException.class,()->service.pagina(true,-1,10,"",null));
        assertThrows(ResponseStatusException.class,()->service.guardarDepartamento(null," ",true));
        assertThrows(ResponseStatusException.class,()->service.guardarCiudad(null,"Ciudad","no-existe",true));
    }
}
