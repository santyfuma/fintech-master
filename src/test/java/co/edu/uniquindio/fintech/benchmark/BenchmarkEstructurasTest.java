package co.edu.uniquindio.fintech.benchmark;

import co.edu.uniquindio.fintech.exception.SolicitudInvalidaException;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import org.junit.jupiter.api.Test;

import static co.edu.uniquindio.fintech.soporte.Afirmaciones.lanza;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Solo verifica la forma y la coherencia de las mediciones; nunca compara tiempos (no son deterministas). */
class BenchmarkEstructurasTest {

    private final BenchmarkEstructuras benchmark = new BenchmarkEstructuras();

    @Test
    void cadaComparacionMideAlMenosDosEstructurasConElMismoResultado() {
        for (ComparacionBenchmark c : ComparacionBenchmark.values()) {
            Lista<MedicionBenchmark> m = benchmark.ejecutar(c, 50, 3);
            assertTrue(m.tamanio() >= 2, c.name());
            for (MedicionBenchmark x : m) {
                assertEquals(50, x.elementos());
                assertEquals(3, x.repeticiones());
                assertTrue(x.minimoNanos() >= 0);
                assertTrue(x.minimoNanos() <= x.promedioNanos() && x.promedioNanos() <= x.maximoNanos());
                assertEquals(m.obtener(0).operacion(), x.operacion());
                assertEquals(m.obtener(0).resultado(), x.resultado(), "las estructuras deben coincidir en " + c);
            }
        }
    }

    @Test
    void esReproducible() {
        String r1 = benchmark.ejecutar(ComparacionBenchmark.RECORRIDO_GRAFO, 30, 1).obtener(0).resultado();
        String r2 = benchmark.ejecutar(ComparacionBenchmark.RECORRIDO_GRAFO, 30, 1).obtener(0).resultado();
        assertEquals(r1, r2);
    }

    @Test
    void limitesDeLosParametros() {
        lanza(SolicitudInvalidaException.class, "PARAMETRO_BENCHMARK_INVALIDO",
                () -> benchmark.ejecutar(ComparacionBenchmark.BUSQUEDA, 9, 1));
        lanza(SolicitudInvalidaException.class, "PARAMETRO_BENCHMARK_INVALIDO",
                () -> benchmark.ejecutar(ComparacionBenchmark.BUSQUEDA, 5001, 1));
        lanza(SolicitudInvalidaException.class, "PARAMETRO_BENCHMARK_INVALIDO",
                () -> benchmark.ejecutar(ComparacionBenchmark.BUSQUEDA, 10, 0));
        lanza(SolicitudInvalidaException.class, "PARAMETRO_BENCHMARK_INVALIDO",
                () -> benchmark.ejecutar(ComparacionBenchmark.BUSQUEDA, 10, 21));
        lanza(SolicitudInvalidaException.class, "PARAMETRO_BENCHMARK_INVALIDO", () -> benchmark.ejecutar(null, 10, 1));
    }
}
