package co.edu.uniquindio.fintech.repository;

import co.edu.uniquindio.fintech.config.ContextoDatos;
import co.edu.uniquindio.fintech.exception.PersistenciaException;
import co.edu.uniquindio.fintech.soporte.EntornoDatos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static co.edu.uniquindio.fintech.soporte.DatosPrueba.ids;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositorioUsuariosTest {

    @TempDir
    Path directorio;

    private RepositorioUsuarios usuarios;

    @BeforeEach
    void preparar() {
        usuarios = new EntornoDatos(directorio).iniciar().usuarios();
    }

    private RepositorioUsuarios reiniciado() {
        return new EntornoDatos(directorio).iniciar().usuarios();
    }

    /** a=500, b=500, c=800, d=100 (registrados en ese orden). */
    private void registrarCuatro() {
        usuarios.registrar(usuario("a", 500));
        usuarios.registrar(usuario("b", 500));
        usuarios.registrar(usuario("c", 800));
        usuarios.registrar(usuario("d", 100));
    }

    @Test
    void registrarBuscarYConsultarTodos() {
        registrarCuatro();
        assertEquals(4, usuarios.cantidad());
        assertEquals(usuario("c", 800), usuarios.buscar("c"));
        assertTrue(usuarios.existe("a"));
        assertNull(usuarios.buscar("zz"));
        assertFalse(usuarios.existe("zz"));
        assertEquals("[a, b, c, d]", ids(usuarios.todos()));
    }

    @Test
    void noAdmiteIdentificacionesRepetidas() {
        usuarios.registrar(usuario("a", 1));
        assertThrows(IllegalStateException.class, () -> usuarios.registrar(usuario("a", 99)));
        assertEquals(1, usuarios.cantidad());
        assertEquals(1, usuarios.buscar("a").puntosAcumulados());
        assertEquals(1, usuarios.ordenadosPorPuntos().tamanio());
    }

    @Test
    void usuariosConPuntosRepetidosEnElAbb() {
        registrarCuatro();
        assertEquals("[d, a, b, c]", ids(usuarios.ordenadosPorPuntos()));
        assertEquals("[a, b]", ids(usuarios.porRangoDePuntos(500, 500)));
        assertEquals("[c]", ids(usuarios.porRangoDePuntos(501, 1000)));
        assertEquals("[d, a, b]", ids(usuarios.porRangoDePuntos(0, 500)));
        assertEquals("[c, b]", ids(usuarios.conMasPuntos(2)));
        assertEquals("[]", ids(usuarios.porRangoDePuntos(900, 2000)));
    }

    @Test
    void cambiarLosPuntosReubicaAlUsuarioSinDuplicarlo() {
        registrarCuatro();
        assertEquals(usuario("a", 500), usuarios.actualizar(usuario("a", 900)));
        assertEquals("[d, b, c, a]", ids(usuarios.ordenadosPorPuntos()));
        assertEquals(4, usuarios.ordenadosPorPuntos().tamanio());
        assertEquals("[b]", ids(usuarios.porRangoDePuntos(500, 500)));
        assertEquals("[a]", ids(usuarios.conMasPuntos(1)));
        assertEquals("[a, b, c, d]", ids(usuarios.todos()));
    }

    @Test
    void actualizarSinCambiarPuntosConservaElIndice() {
        registrarCuatro();
        usuarios.actualizar(usuarios.buscar("b").conDatosBasicos("Beatriz", "b@nuevo.co"));
        assertEquals("Beatriz", usuarios.buscar("b").nombre());
        assertEquals("[d, a, b, c]", ids(usuarios.ordenadosPorPuntos()));
        assertEquals("Beatriz", usuarios.porRangoDePuntos(500, 500).obtener(1).nombre());
    }

    @Test
    void actualizarUnUsuarioInexistenteFalla() {
        assertThrows(IllegalStateException.class, () -> usuarios.actualizar(usuario("zz", 1)));
        assertEquals(0, usuarios.cantidad());
    }

    @Test
    void eliminarRetiraDeTodosLosIndicesYDelArchivo() {
        registrarCuatro();
        assertEquals(usuario("a", 500), usuarios.eliminar("a"));
        assertNull(usuarios.eliminar("a"));
        assertNull(usuarios.buscar("a"));
        assertEquals("[b]", ids(usuarios.porRangoDePuntos(500, 500)));
        assertEquals("[b, c, d]", ids(usuarios.todos()));
        assertEquals("[b, c, d]", ids(reiniciado().todos()));
    }

    @Test
    void losCambiosPersistenYLaReconstruccionDaElMismoResultado() {
        registrarCuatro();
        usuarios.actualizar(usuario("d", 650));
        RepositorioUsuarios reconstruido = reiniciado();
        assertEquals(ids(usuarios.todos()), ids(reconstruido.todos()));
        assertEquals(ids(usuarios.ordenadosPorPuntos()), ids(reconstruido.ordenadosPorPuntos()));
        assertEquals("[a, b, d, c]", ids(reconstruido.ordenadosPorPuntos()));
        assertEquals(usuario("d", 650), reconstruido.buscar("d"));
    }

    @Test
    void siFallaLaEscrituraLaMemoriaNoCambia() {
        usuarios.registrar(usuario("a", 1));
        new EntornoDatos(directorio).bloquearEscritura("usuarios.json");
        assertThrows(PersistenciaException.class, () -> usuarios.registrar(usuario("b", 2)));
        assertThrows(PersistenciaException.class, () -> usuarios.actualizar(usuario("a", 50)));
        assertEquals(1, usuarios.cantidad());
        assertNull(usuarios.buscar("b"));
        assertEquals(1, usuarios.buscar("a").puntosAcumulados());
        assertEquals("[a]", ids(usuarios.ordenadosPorPuntos()));
    }

    @Test
    void reflejarSoloActualizaLaMemoria() {
        ContextoDatos contexto = new EntornoDatos(directorio).iniciar();
        contexto.usuarios().reflejar(usuario("x", 10));
        assertEquals("[x]", ids(contexto.usuarios().ordenadosPorPuntos()));
        contexto.usuarios().reflejar(usuario("x", 20));
        assertEquals(1, contexto.usuarios().ordenadosPorPuntos().tamanio());
        assertFalse(new EntornoDatos(directorio).existe("usuarios.json"));
    }
}
