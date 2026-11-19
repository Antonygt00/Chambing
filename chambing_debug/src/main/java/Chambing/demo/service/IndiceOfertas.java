package Chambing.demo.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class IndiceOfertas implements ApplicationRunner {
    private final OfertaListadoService listado;

    public IndiceOfertas(OfertaListadoService listado) { this.listado = listado; }

    @Override
    public void run(ApplicationArguments args) {
        try {
            listado.asegurarIndiceTexto();
        } catch (Exception ignored) {
            // Mongo puede no estar disponible en tests; el listado igual filtra en memoria.
        }
    }
}
