package co.edu.uniquindio.fintech.structures.adaptadores;

import co.edu.uniquindio.fintech.structures.interfaces.Cola;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColaAdaptadorTest {

    private static String contenido(Iterable<String> coleccion) {
        StringBuilder sb = new StringBuilder("[");
        for (String e : coleccion) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(e);
        }
        return sb.append(']').toString();
    }

    @Test
    void colaNuevaEstaVacia() {
        Cola<String> cola = new ColaAdaptador<>();
        assertTrue(cola.estaVacia());
        assertEquals(0, cola.tamanio());
        assertEquals("[]", contenido(cola));
    }

    @Test
    void desencolarYFrenteSobreColaVaciaLanzanIllegalState() {
        Cola<String> cola = new ColaAdaptador<>();
        assertThrows(IllegalStateException.class, cola::desencolar);
        assertThrows(IllegalStateException.class, cola::frente);
    }

    @Test
    void conservaElOrdenFifo() {
        Cola<String> cola = new ColaAdaptador<>();
        cola.encolar("a");
        cola.encolar("b");
        cola.encolar("c");
        assertEquals(3, cola.tamanio());
        assertEquals("a", cola.desencolar());
        assertEquals("b", cola.desencolar());
        assertEquals("c", cola.desencolar());
        assertTrue(cola.estaVacia());
        assertThrows(IllegalStateException.class, cola::desencolar);
    }

    @Test
    void frenteConsultaSinRetirar() {
        Cola<String> cola = new ColaAdaptador<>();
        cola.encolar("a");
        cola.encolar("b");
        assertEquals("a", cola.frente());
        assertEquals("a", cola.frente());
        assertEquals(2, cola.tamanio());
    }

    @Test
    void operacionesIntercaladasConservanFifo() {
        Cola<String> cola = new ColaAdaptador<>();
        cola.encolar("a");
        cola.encolar("b");
        assertEquals("a", cola.desencolar());
        cola.encolar("c");
        assertEquals("b", cola.frente());
        assertEquals("b", cola.desencolar());
        assertEquals("c", cola.desencolar());
        assertTrue(cola.estaVacia());
    }

    @Test
    void permiteDuplicados() {
        Cola<String> cola = new ColaAdaptador<>();
        cola.encolar("x");
        cola.encolar("x");
        assertEquals(2, cola.tamanio());
    }

    @Test
    void iteraDesdeElFrenteSinModificar() {
        Cola<String> cola = new ColaAdaptador<>();
        cola.encolar("a");
        cola.encolar("b");
        cola.encolar("c");
        assertEquals("[a, b, c]", contenido(cola));
        assertEquals(3, cola.tamanio());
    }

    @Test
    void iteradorEsDeSoloLectura() {
        Cola<String> cola = new ColaAdaptador<>();
        cola.encolar("a");
        Iterator<String> it = cola.iterator();
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove);
        assertEquals(1, cola.tamanio());
    }

    @Test
    void rechazaNulos() {
        Cola<String> cola = new ColaAdaptador<>();
        assertThrows(NullPointerException.class, () -> cola.encolar(null));
        assertTrue(cola.estaVacia());
    }

    @Test
    void limpiarVaciaLaCola() {
        Cola<String> cola = new ColaAdaptador<>();
        cola.encolar("a");
        cola.limpiar();
        assertTrue(cola.estaVacia());
        assertThrows(IllegalStateException.class, cola::frente);
        cola.encolar("b");
        assertEquals("b", cola.frente());
    }

    @Test
    void muchosElementosConservanFifoTrasDarLaVueltaAlArreglo() {
        Cola<String> cola = new ColaAdaptador<>();
        int siguienteEsperado = 0;
        int siguienteNuevo = 0;
        // Se alternan inserciones y extracciones para que el arreglo circular dé la vuelta.
        for (int ronda = 0; ronda < 100; ronda++) {
            for (int i = 0; i < 150; i++) {
                cola.encolar("e" + siguienteNuevo++);
            }
            for (int i = 0; i < 100; i++) {
                assertEquals("e" + siguienteEsperado++, cola.desencolar());
            }
        }
        assertEquals(siguienteNuevo - siguienteEsperado, cola.tamanio());
        while (!cola.estaVacia()) {
            assertEquals("e" + siguienteEsperado++, cola.desencolar());
        }
        assertEquals(siguienteNuevo, siguienteEsperado);
    }
}
