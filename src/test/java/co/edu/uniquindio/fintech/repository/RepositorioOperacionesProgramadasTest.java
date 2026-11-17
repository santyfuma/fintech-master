package co.edu.uniquindio.fintech.repository;

import co.edu.uniquindio.fintech.config.ContextoDatos;
import co.edu.uniquindio.fintech.model.EstadoOperacionProgramada;
import co.edu.uniquindio.fintech.soporte.EntornoDatos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static co.edu.uniquindio.fintech.soporte.DatosPrueba.FECHA;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.billetera;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.ids;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.opr;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.recargaProgramada;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.transferenciaProgramada;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * OPR-1 Ana  +3 días · OPR-2 Ana +1 día · OPR-3 Beto +1 día (empata con OPR-2) ·
 * OPR-4 recarga a Beto +2 días.
 */
class RepositorioOperacionesProgramadasTest {

    @TempDir
    Path directorio;

    private RepositorioOperacionesProgramadas operaciones;

    @BeforeEach
    void preparar() {
        ContextoDatos contexto = new EntornoDatos(directorio).iniciar();
        contexto.billeteras().registrar(billetera(1, "ana"));
        contexto.billeteras().registrar(billetera(2, "ana"));
        contexto.billeteras().registrar(billetera(3, "beto"));
        operaciones = contexto.operacionesProgramadas();
        operaciones.registrar(transferenciaProgramada(1, 1, 3, FECHA.plusDays(3)));
        operaciones.registrar(transferenciaProgramada(2, 1, 2, FECHA.plusDays(1)));
        operaciones.registrar(transferenciaProgramada(3, 3, 1, FECHA.plusDays(1)));
        operaciones.registrar(recargaProgramada(4, 3, FECHA.plusDays(2)));
    }

    private void cambiarEstado(int n, EstadoOperacionProgramada estado) {
        operaciones.actualizar(operaciones.buscar(opr(n)).conEstado(estado));
    }

    @Test
    void colaDePrioridadPorFechaConDesempatePorIdentificador() {
        assertEquals(4, operaciones.cantidadPendientes());
        assertEquals(opr(2), operaciones.siguientePendiente().id());
    }

    @Test
    void lasEjecutadasCanceladasYFallidasSalenDeLasPendientes() {
        cambiarEstado(2, EstadoOperacionProgramada.EJECUTADA);
        assertEquals(opr(3), operaciones.siguientePendiente().id());
        cambiarEstado(3, EstadoOperacionProgramada.CANCELADA);
        assertEquals(opr(4), operaciones.siguientePendiente().id());
        cambiarEstado(4, EstadoOperacionProgramada.FALLIDA);
        assertEquals(opr(1), operaciones.siguientePendiente().id());
        cambiarEstado(1, EstadoOperacionProgramada.EJECUTADA);
        assertNull(operaciones.siguientePendiente());
        assertEquals(0, operaciones.cantidadPendientes());
        assertEquals(4, operaciones.cantidad());
    }

    @Test
    void reprogramarReubicaLaOperacionSinDuplicarla() {
        operaciones.actualizar(operaciones.buscar(opr(1)).conFechaEjecucion(FECHA.plusHours(1)));
        assertEquals(opr(1), operaciones.siguientePendiente().id());
        assertEquals(4, operaciones.cantidadPendientes());
        cambiarEstado(1, EstadoOperacionProgramada.EJECUTADA);
        assertEquals(opr(2), operaciones.siguientePendiente().id());
        assertEquals(3, operaciones.cantidadPendientes());
    }

    @Test
    void consultaPorUsuario() {
        assertEquals("[" + opr(1) + ", " + opr(2) + "]", ids(operaciones.porUsuario("ana")));
        assertEquals("[" + opr(3) + ", " + opr(4) + "]", ids(operaciones.porUsuario("beto")));
        cambiarEstado(1, EstadoOperacionProgramada.CANCELADA);
        assertEquals("[" + opr(1) + ", " + opr(2) + "]", ids(operaciones.porUsuario("ana")));
    }

    @Test
    void billeteraInexistenteSeRechaza() {
        assertThrows(IllegalStateException.class,
                () -> operaciones.registrar(transferenciaProgramada(5, 1, 9, FECHA)));
    }

    @Test
    void reconstruccionDesdeJsonDaElMismoResultado() {
        cambiarEstado(2, EstadoOperacionProgramada.EJECUTADA);
        RepositorioOperacionesProgramadas reconstruido =
                new EntornoDatos(directorio).iniciar().operacionesProgramadas();
        assertEquals(3, reconstruido.cantidadPendientes());
        assertEquals(opr(3), reconstruido.siguientePendiente().id());
        assertEquals(ids(operaciones.todos()), ids(reconstruido.todos()));
        assertEquals(ids(operaciones.porUsuario("beto")), ids(reconstruido.porUsuario("beto")));
    }
}
