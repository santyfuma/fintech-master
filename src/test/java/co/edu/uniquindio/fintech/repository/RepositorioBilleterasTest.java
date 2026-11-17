package co.edu.uniquindio.fintech.repository;

import co.edu.uniquindio.fintech.model.Billetera;
import co.edu.uniquindio.fintech.model.EstadoBilletera;
import co.edu.uniquindio.fintech.soporte.EntornoDatos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.Path;

import static co.edu.uniquindio.fintech.soporte.DatosPrueba.bil;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.billetera;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.ids;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepositorioBilleterasTest {

    @TempDir
    Path directorio;

    private RepositorioBilleteras billeteras;

    @BeforeEach
    void preparar() {
        billeteras = new EntornoDatos(directorio).iniciar().billeteras();
        billeteras.registrar(billetera(1, "ana"));
        billeteras.registrar(billetera(2, "luis"));
        billeteras.registrar(billetera(3, "ana"));
        billeteras.registrar(billetera(4, "ana"));
    }

    @Test
    void registrarYBuscarPorCodigo() {
        assertEquals(billetera(2, "luis"), billeteras.buscar(bil(2)));
        assertNull(billeteras.buscar(bil(9)));
        assertEquals(4, billeteras.cantidad());
        assertThrows(IllegalStateException.class, () -> billeteras.registrar(billetera(1, "otro")));
    }

    @Test
    void variasBilleterasDeUnMismoUsuarioEnOrdenDeCreacion() {
        assertEquals("[" + bil(1) + ", " + bil(3) + ", " + bil(4) + "]", ids(billeteras.porPropietario("ana")));
        assertEquals("[" + bil(2) + "]", ids(billeteras.porPropietario("luis")));
        assertEquals("[]", ids(billeteras.porPropietario("nadie")));
        assertEquals("ana", billeteras.propietarioDe(bil(3)));
        assertNull(billeteras.propietarioDe(bil(9)));
    }

    @Test
    void desactivarConservaElRegistroYLosIndicesSinDuplicados() {
        Billetera inactiva = billeteras.buscar(bil(3)).conEstado(EstadoBilletera.INACTIVA);
        billeteras.actualizar(inactiva);
        assertEquals(EstadoBilletera.INACTIVA, billeteras.buscar(bil(3)).estado());
        assertEquals("[" + bil(1) + ", " + bil(3) + ", " + bil(4) + "]", ids(billeteras.porPropietario("ana")));
        assertEquals(EstadoBilletera.INACTIVA, billeteras.porPropietario("ana").obtener(1).estado());
        assertEquals(4, billeteras.todos().tamanio());
    }

    @Test
    void noExisteEliminacionFisica() {
        for (Method metodo : RepositorioBilleteras.class.getMethods()) {
            assertFalse(metodo.getName().startsWith("eliminar"), metodo.getName());
        }
    }

    @Test
    void reconstruccionDesdeJsonDaElMismoResultado() {
        billeteras.actualizar(billeteras.buscar(bil(1)).conDatos("Viajes", billeteras.buscar(bil(1)).tipo()));
        RepositorioBilleteras reconstruido = new EntornoDatos(directorio).iniciar().billeteras();
        assertEquals(ids(billeteras.todos()), ids(reconstruido.todos()));
        assertEquals(ids(billeteras.porPropietario("ana")), ids(reconstruido.porPropietario("ana")));
        assertEquals("Viajes", reconstruido.buscar(bil(1)).nombre());
    }
}
