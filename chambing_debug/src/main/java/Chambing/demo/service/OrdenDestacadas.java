package Chambing.demo.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import Chambing.demo.Model.Propuesta;

/**
 * Arma el orden de un listado: como máximo 3 destacadas vigentes al inicio de cada página,
 * rotadas para que no ocupen siempre las mismas posiciones.
 */
public final class OrdenDestacadas {

    public static final int MAX_POR_PAGINA = 3;

    private OrdenDestacadas() {}

    public static List<Propuesta> intercalar(List<Propuesta> destacadas, List<Propuesta> organicas,
                                             int pageSize, int rotacion) {
        int tamano = Math.max(pageSize, MAX_POR_PAGINA);
        List<Propuesta> dest = new ArrayList<>(destacadas);
        if (!dest.isEmpty()) {
            Collections.rotate(dest, -Math.floorMod(rotacion, dest.size()));
        }
        List<Propuesta> salida = new ArrayList<>(dest.size() + organicas.size());
        int i = 0;
        int j = 0;
        while (i < dest.size() || j < organicas.size()) {
            int cupo = Math.min(MAX_POR_PAGINA, dest.size() - i);
            for (int k = 0; k < cupo; k++) {
                salida.add(dest.get(i++));
            }
            int huecos = tamano - cupo;
            if (j >= organicas.size()) {
                if (i >= dest.size()) {
                    break;
                }
                continue;
            }
            for (int k = 0; k < huecos && j < organicas.size(); k++) {
                salida.add(organicas.get(j++));
            }
        }
        return salida;
    }
}
