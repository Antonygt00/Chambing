package Chambing.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import Chambing.demo.Model.Propuesta;
import Chambing.demo.service.OrdenDestacadas;

class OrdenDestacadasTest {

    @Test
    void cadaPaginaEmpiezaConHastaTresDestacadasYLaRotacionCambiaElOrden() {
        List<Propuesta> destacadas = ofertas("d", 5);
        List<Propuesta> organicas = ofertas("o", 20);

        List<Propuesta> pagina = OrdenDestacadas.intercalar(destacadas, organicas, 10, 0);
        assertEquals("d0", pagina.get(0).getIdPropuesta());
        assertEquals("d2", pagina.get(2).getIdPropuesta());
        assertEquals("o0", pagina.get(3).getIdPropuesta());
        assertEquals("d3", pagina.get(10).getIdPropuesta());
        assertEquals("d4", pagina.get(11).getIdPropuesta());
        assertEquals("o7", pagina.get(12).getIdPropuesta());

        List<Propuesta> rotada = OrdenDestacadas.intercalar(destacadas, organicas, 10, 1);
        assertEquals("d1", rotada.get(0).getIdPropuesta());
    }

    private List<Propuesta> ofertas(String prefijo, int n) {
        List<Propuesta> lista = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Propuesta p = new Propuesta();
            p.setIdPropuesta(prefijo + i);
            lista.add(p);
        }
        return lista;
    }
}
