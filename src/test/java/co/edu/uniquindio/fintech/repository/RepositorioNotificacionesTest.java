package co.edu.uniquindio.fintech.repository;

import co.edu.uniquindio.fintech.model.EstadoNotificacion;
import co.edu.uniquindio.fintech.soporte.EntornoDatos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static co.edu.uniquindio.fintech.soporte.DatosPrueba.ids;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.lista;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.not;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.notificacion;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** NOT-1 Ana, NOT-2 Beto, NOT-3 Ana, NOT-4 Ana, todas pendientes. */
class RepositorioNotificacionesTest {

    @TempDir
    Path directorio;

    private RepositorioNotificaciones notificaciones;

    @BeforeEach
    void preparar() {
        notificaciones = new EntornoDatos(directorio).iniciar().notificaciones();
        notificaciones.registrar(notificacion(1, "ana"));
        notificaciones.registrar(notificacion(2, "beto"));
        notificaciones.registrar(notificacion(3, "ana"));
        notificaciones.registrar(notificacion(4, "ana"));
    }

    private static String n(int... numeros) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < numeros.length; i++) {
            sb.append(i == 0 ? "" : ", ").append(not(numeros[i]));
        }
        return sb.append(']').toString();
    }

    private void despachar(int numero) {
        notificaciones.actualizar(notificaciones.buscar(not(numero)).conEstado(EstadoNotificacion.DESPACHADA));
    }

    @Test
    void colaFifoDePendientes() {
        assertEquals(n(1, 2, 3, 4), ids(notificaciones.pendientes()));
        assertEquals(not(1), notificaciones.siguientePendiente().id());
    }

    @Test
    void lasDespachadasDejanDeEstarPendientes() {
        despachar(1);
        assertEquals(not(2), notificaciones.siguientePendiente().id());
        despachar(3); // en medio de la cola
        assertEquals(n(2, 4), ids(notificaciones.pendientes()));
        assertEquals(not(2), notificaciones.siguientePendiente().id());
        despachar(2);
        despachar(4);
        assertNull(notificaciones.siguientePendiente());
        assertEquals("[]", ids(notificaciones.pendientes()));
        assertEquals(4, notificaciones.cantidad());
    }

    @Test
    void volverAPendienteNoDuplicaLaNotificacionEnLaCola() {
        despachar(3);
        notificaciones.actualizar(notificaciones.buscar(not(3)).conEstado(EstadoNotificacion.PENDIENTE));
        notificaciones.actualizar(notificaciones.buscar(not(3)).conEstado(EstadoNotificacion.PENDIENTE));
        assertEquals(n(1, 2, 3, 4), ids(notificaciones.pendientes()));
    }

    @Test
    void unaNotificacionRegistradaComoDespachadaNoEntraALaCola() {
        notificaciones.registrar(notificacion(5, "beto").conEstado(EstadoNotificacion.DESPACHADA));
        assertEquals(n(1, 2, 3, 4), ids(notificaciones.pendientes()));
    }

    @Test
    void porUsuarioYRecientes() {
        assertEquals(n(1, 3, 4), ids(notificaciones.porUsuario("ana")));
        assertEquals(n(4, 3, 1), ids(notificaciones.recientes("ana")));
        assertEquals(0, notificaciones.limiteRecientes());
        RepositorioNotificaciones conLimite = new EntornoDatos(directorio).iniciar(2).notificaciones();
        assertEquals(n(4, 3), ids(conLimite.recientes("ana")));
        assertEquals(n(2), ids(conLimite.recientes("beto")));
        assertEquals(n(1, 3, 4), ids(conLimite.porUsuario("ana")));
    }

    @Test
    void reconstruccionDesdeJsonConservaElOrdenDeLaCola() {
        despachar(1);
        despachar(3);
        RepositorioNotificaciones reconstruido = new EntornoDatos(directorio).iniciar().notificaciones();
        assertEquals(n(2, 4), ids(reconstruido.pendientes()));
        assertEquals(not(2), reconstruido.siguientePendiente().id());
        assertEquals(ids(notificaciones.porUsuario("ana")), ids(reconstruido.porUsuario("ana")));
    }

    @Test
    void elLimiteNoPuedeSerNegativo() {
        EntornoDatos entorno = new EntornoDatos(directorio);
        assertThrows(IllegalArgumentException.class, () -> new RepositorioNotificaciones(
                entorno.almacenes.notificaciones(), entorno.fabrica, entorno.cerrojo, lista(), -1));
    }
}
