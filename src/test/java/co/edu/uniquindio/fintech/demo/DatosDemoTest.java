package co.edu.uniquindio.fintech.demo;

import co.edu.uniquindio.fintech.model.EstadoOperacionProgramada;
import co.edu.uniquindio.fintech.model.EstadoTransaccion;
import co.edu.uniquindio.fintech.model.EventoAuditoria;
import co.edu.uniquindio.fintech.model.NivelUsuario;
import co.edu.uniquindio.fintech.model.OperacionProgramada;
import co.edu.uniquindio.fintech.model.PatronInusual;
import co.edu.uniquindio.fintech.model.TipoTransaccion;
import co.edu.uniquindio.fintech.model.Transaccion;
import co.edu.uniquindio.fintech.model.Usuario;
import co.edu.uniquindio.fintech.repository.RepositorioEnMemoria;
import co.edu.uniquindio.fintech.soporte.RelojAjustable;
import co.edu.uniquindio.fintech.soporte.SistemaPrueba;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Protege el conjunto de demostración {@code datos-demo/}: carga sin errores de
 * integridad, contiene los casos que documenta y es exactamente lo que produce
 * {@link GeneradorDatosDemo}.
 */
class DatosDemoTest {

    private static final Path DATOS_DEMO = Path.of("datos-demo");

    @TempDir
    Path copia;

    @TempDir
    Path regenerados;

    private SistemaPrueba cargarCopia() throws IOException {
        try (var archivos = Files.list(DATOS_DEMO)) {
            for (Path p : (Iterable<Path>) archivos::iterator) {
                Files.copy(p, copia.resolve(p.getFileName()));
            }
        }
        // Reloj al final de la historia: nada vencido queda por ejecutar al cargar.
        return new SistemaPrueba(copia, new RelojAjustable(LocalDateTime.of(2026, 11, 15, 18, 0)),
                GeneradorDatosDemo.CATALOGO);
    }

    @Test
    void cargaSinErroresYConLosCasosDocumentados() throws IOException {
        SistemaPrueba s = cargarCopia(); // el arranque recupera el diario y verifica la integridad
        assertTrue(s.entorno.diario.pendientes().estaVacia());
        assertEquals(8, s.datos.usuarios().cantidad());
        assertEquals(12, s.datos.billeteras().cantidad());

        boolean[] niveles = new boolean[NivelUsuario.values().length];
        for (Usuario u : s.datos.usuarios().todos()) {
            niveles[u.nivel().ordinal()] = true;
            assertEquals(s.puntos.nivelPara(u.puntosAcumulados()), u.nivel());
        }
        for (boolean presente : niveles) {
            assertTrue(presente, "debe haber usuarios en los cuatro niveles");
        }

        boolean[] patrones = new boolean[PatronInusual.values().length];
        for (EventoAuditoria e : s.datos.auditoria().todos()) {
            patrones[e.patron().ordinal()] = true;
        }
        for (boolean presente : patrones) {
            assertTrue(presente, "deben aparecer los seis patrones de detección");
        }

        int rechazadas = 0;
        int revertidas = 0;
        int reversiones = 0;
        for (Transaccion t : s.datos.transacciones().todos()) {
            rechazadas += t.estado() == EstadoTransaccion.RECHAZADA ? 1 : 0;
            revertidas += t.estado() == EstadoTransaccion.REVERTIDA ? 1 : 0;
            reversiones += t.tipo() == TipoTransaccion.REVERSION ? 1 : 0;
        }
        assertEquals(3, rechazadas);
        assertEquals(2, revertidas);
        assertEquals(2, reversiones);

        boolean[] estados = new boolean[EstadoOperacionProgramada.values().length];
        for (OperacionProgramada o : s.datos.operacionesProgramadas().todos()) {
            estados[o.estado().ordinal()] = true;
        }
        for (boolean presente : estados) {
            assertTrue(presente, "debe haber programadas en todos los estados");
        }
        assertEquals(2, s.datos.beneficios().cantidad());
        assertTrue(s.grafo.tieneCiclo(), "Laura → Andrés → Camila → Laura forman un ciclo");
        s.usuarios.eliminar("1008"); // usuario sin registros asociados: se puede eliminar
    }

    @Test
    void regenerarProduceLosMismosRegistros() throws Exception {
        GeneradorDatosDemo.main(new String[]{regenerados.toString()});
        SistemaPrueba nuevo = new SistemaPrueba(regenerados, new RelojAjustable(LocalDateTime.of(2026, 11, 15, 18, 0)),
                GeneradorDatosDemo.CATALOGO);
        SistemaPrueba publicado = cargarCopia();
        mismos(publicado.datos.usuarios(), nuevo.datos.usuarios());
        mismos(publicado.datos.billeteras(), nuevo.datos.billeteras());
        mismos(publicado.datos.transacciones(), nuevo.datos.transacciones());
        mismos(publicado.datos.operacionesProgramadas(), nuevo.datos.operacionesProgramadas());
        mismos(publicado.datos.notificaciones(), nuevo.datos.notificaciones());
        mismos(publicado.datos.auditoria(), nuevo.datos.auditoria());
        mismos(publicado.datos.beneficios(), nuevo.datos.beneficios());
    }

    @Test
    void elCatalogoConfiguradoEsElDeLosDatosDeDemostracion() throws IOException {
        String propiedades = Files.readString(Path.of("src", "main", "resources", "application.properties"));
        assertTrue(propiedades.contains("fintech.beneficios.catalogo=" + GeneradorDatosDemo.CATALOGO + "\n")
                        || propiedades.contains("fintech.beneficios.catalogo=" + GeneradorDatosDemo.CATALOGO + "\r\n"),
                "application.properties debe usar el mismo catálogo que GeneradorDatosDemo.CATALOGO");
    }

    private static <T> void mismos(RepositorioEnMemoria<T> esperado, RepositorioEnMemoria<T> real) {
        Lista<T> a = esperado.todos();
        Lista<T> b = real.todos();
        assertEquals(a.tamanio(), b.tamanio());
        for (int i = 0; i < a.tamanio(); i++) {
            assertEquals(a.obtener(i), b.obtener(i));
        }
    }
}
