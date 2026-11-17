package co.edu.uniquindio.fintech.structures.adaptadores;

import co.edu.uniquindio.fintech.structures.interfaces.Pila;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PilaAdaptadorTest {

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
    void pilaNuevaEstaVacia() {
        Pila<String> pila = new PilaAdaptador<>();
        assertTrue(pila.estaVacia());
        assertEquals(0, pila.tamanio());
        assertEquals("[]", contenido(pila));
    }

    @Test
    void desapilarYCimaSobrePilaVaciaLanzanIllegalState() {
        Pila<String> pila = new PilaAdaptador<>();
        assertThrows(IllegalStateException.class, pila::desapilar);
        assertThrows(IllegalStateException.class, pila::cima);
    }

    @Test
    void conservaElOrdenLifo() {
        Pila<String> pila = new PilaAdaptador<>();
        pila.apilar("a");
        pila.apilar("b");
        pila.apilar("c");
        assertEquals(3, pila.tamanio());
        assertEquals("c", pila.desapilar());
        assertEquals("b", pila.desapilar());
        assertEquals("a", pila.desapilar());
        assertTrue(pila.estaVacia());
        assertThrows(IllegalStateException.class, pila::desapilar);
    }

    @Test
    void cimaConsultaSinRetirar() {
        Pila<String> pila = new PilaAdaptador<>();
        pila.apilar("a");
        pila.apilar("b");
        assertEquals("b", pila.cima());
        assertEquals("b", pila.cima());
        assertEquals(2, pila.tamanio());
    }

    @Test
    void operacionesIntercaladas() {
        Pila<String> pila = new PilaAdaptador<>();
        pila.apilar("a");
        pila.apilar("b");
        assertEquals("b", pila.desapilar());
        pila.apilar("c");
        assertEquals("c", pila.cima());
        assertEquals("c", pila.desapilar());
        assertEquals("a", pila.desapilar());
        assertTrue(pila.estaVacia());
    }

    @Test
    void permiteDuplicados() {
        Pila<String> pila = new PilaAdaptador<>();
        pila.apilar("x");
        pila.apilar("x");
        assertEquals(2, pila.tamanio());
        assertEquals("x", pila.desapilar());
        assertEquals("x", pila.desapilar());
    }

    @Test
    void iteraDesdeLaCimaHaciaElFondoSinModificar() {
        Pila<String> pila = new PilaAdaptador<>();
        pila.apilar("a");
        pila.apilar("b");
        pila.apilar("c");
        assertEquals("[c, b, a]", contenido(pila));
        assertEquals(3, pila.tamanio());
    }

    @Test
    void iteradorEsDeSoloLectura() {
        Pila<String> pila = new PilaAdaptador<>();
        pila.apilar("a");
        Iterator<String> it = pila.iterator();
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove);
        assertEquals(1, pila.tamanio());
    }

    @Test
    void rechazaNulos() {
        Pila<String> pila = new PilaAdaptador<>();
        assertThrows(NullPointerException.class, () -> pila.apilar(null));
        assertTrue(pila.estaVacia());
    }

    @Test
    void limpiarVaciaLaPila() {
        Pila<String> pila = new PilaAdaptador<>();
        pila.apilar("a");
        pila.apilar("b");
        pila.limpiar();
        assertTrue(pila.estaVacia());
        assertThrows(IllegalStateException.class, pila::cima);
        pila.apilar("c");
        assertEquals("c", pila.cima());
    }

    @Test
    void muchosElementosConservanLifo() {
        Pila<String> pila = new PilaAdaptador<>();
        for (int i = 0; i < 10_000; i++) {
            pila.apilar("e" + i);
        }
        assertEquals(10_000, pila.tamanio());
        for (int i = 9_999; i >= 0; i--) {
            assertEquals("e" + i, pila.desapilar());
        }
        assertTrue(pila.estaVacia());
    }
}
