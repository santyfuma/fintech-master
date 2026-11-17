package co.edu.uniquindio.fintech.structures.adaptadores;

import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import co.edu.uniquindio.fintech.structures.interfaces.TablaHash;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TablaHashAdaptadorTest {

    /** Clave con hashCode constante: obliga a que todas colisionen en la misma cubeta. */
    private record ClaveQueColisiona(int valor) {
        @Override
        public int hashCode() {
            return 42;
        }
    }

    @Test
    void tablaNuevaEstaVacia() {
        TablaHash<String, Integer> tabla = new TablaHashAdaptador<>();
        assertTrue(tabla.estaVacia());
        assertEquals(0, tabla.tamanio());
        assertNull(tabla.obtener("x"));
        assertFalse(tabla.contieneClave("x"));
        assertNull(tabla.eliminar("x"));
        assertTrue(tabla.claves().estaVacia());
        assertTrue(tabla.valores().estaVacia());
    }

    @Test
    void ponerYObtener() {
        TablaHash<String, Integer> tabla = new TablaHashAdaptador<>();
        assertNull(tabla.poner("a", 1));
        assertNull(tabla.poner("b", 2));
        assertEquals(2, tabla.tamanio());
        assertFalse(tabla.estaVacia());
        assertEquals(1, (int) tabla.obtener("a"));
        assertEquals(2, (int) tabla.obtener("b"));
        assertTrue(tabla.contieneClave("a"));
        assertFalse(tabla.contieneClave("c"));
        assertNull(tabla.obtener("c"));
    }

    @Test
    void claveRepetidaReemplazaElValorYDevuelveElAnterior() {
        TablaHash<String, Integer> tabla = new TablaHashAdaptador<>();
        tabla.poner("a", 1);
        assertEquals(1, (int) tabla.poner("a", 10));
        assertEquals(10, (int) tabla.poner("a", 100));
        assertEquals(1, tabla.tamanio());
        assertEquals(100, (int) tabla.obtener("a"));
    }

    @Test
    void clavesIgualesPorEqualsSonLaMismaClave() {
        TablaHash<String, Integer> tabla = new TablaHashAdaptador<>();
        tabla.poner(new String("id-1"), 1);
        assertEquals(1, (int) tabla.poner(new String("id-1"), 2));
        assertEquals(1, tabla.tamanio());
        assertEquals(2, (int) tabla.obtener(new String("id-1")));
    }

    @Test
    void eliminarDevuelveElValorYLuegoNull() {
        TablaHash<String, Integer> tabla = new TablaHashAdaptador<>();
        tabla.poner("a", 1);
        tabla.poner("b", 2);
        assertEquals(1, (int) tabla.eliminar("a"));
        assertNull(tabla.eliminar("a"));
        assertFalse(tabla.contieneClave("a"));
        assertNull(tabla.obtener("a"));
        assertEquals(1, tabla.tamanio());
        assertEquals(2, (int) tabla.obtener("b"));
    }

    @Test
    void sePuedeVolverAPonerUnaClaveEliminada() {
        TablaHash<String, Integer> tabla = new TablaHashAdaptador<>();
        tabla.poner("a", 1);
        tabla.eliminar("a");
        assertNull(tabla.poner("a", 5));
        assertEquals(5, (int) tabla.obtener("a"));
    }

    @Test
    void rechazaClavesYValoresNulos() {
        TablaHash<String, Integer> tabla = new TablaHashAdaptador<>();
        assertThrows(NullPointerException.class, () -> tabla.poner(null, 1));
        assertThrows(NullPointerException.class, () -> tabla.poner("a", null));
        assertThrows(NullPointerException.class, () -> tabla.obtener(null));
        assertThrows(NullPointerException.class, () -> tabla.contieneClave(null));
        assertThrows(NullPointerException.class, () -> tabla.eliminar(null));
        assertTrue(tabla.estaVacia());
    }

    @Test
    void ponerConValorNuloNoAlteraElValorExistente() {
        TablaHash<String, Integer> tabla = new TablaHashAdaptador<>();
        tabla.poner("a", 1);
        assertThrows(NullPointerException.class, () -> tabla.poner("a", null));
        assertEquals(1, (int) tabla.obtener("a"));
    }

    @Test
    void clavesYValoresDevuelvenTodosLosElementos() {
        TablaHash<String, Integer> tabla = new TablaHashAdaptador<>();
        tabla.poner("a", 1);
        tabla.poner("b", 2);
        tabla.poner("c", 3);
        Lista<String> claves = tabla.claves();
        Lista<Integer> valores = tabla.valores();
        assertEquals(3, claves.tamanio());
        assertEquals(3, valores.tamanio());
        assertTrue(claves.contiene("a") && claves.contiene("b") && claves.contiene("c"));
        int suma = 0;
        for (int v : valores) {
            suma += v;
        }
        assertEquals(6, suma);
    }

    @Test
    void clavesYValoresSonCopiasIndependientes() {
        TablaHash<String, Integer> tabla = new TablaHashAdaptador<>();
        tabla.poner("a", 1);
        Lista<String> claves = tabla.claves();
        claves.agregar("intrusa");
        claves.eliminar("a");
        assertTrue(tabla.contieneClave("a"));
        assertFalse(tabla.contieneClave("intrusa"));

        tabla.poner("b", 2);
        assertEquals(1, claves.tamanio());
        assertEquals(2, tabla.valores().tamanio());
    }

    @Test
    void limpiarVaciaLaTabla() {
        TablaHash<String, Integer> tabla = new TablaHashAdaptador<>();
        tabla.poner("a", 1);
        tabla.poner("b", 2);
        tabla.limpiar();
        assertTrue(tabla.estaVacia());
        assertEquals(0, tabla.tamanio());
        assertNull(tabla.obtener("a"));
        tabla.poner("a", 3);
        assertEquals(3, (int) tabla.obtener("a"));
    }

    @Test
    void funcionaCorrectamenteConColisiones() {
        TablaHash<ClaveQueColisiona, Integer> tabla = new TablaHashAdaptador<>();
        for (int i = 0; i < 200; i++) {
            assertNull(tabla.poner(new ClaveQueColisiona(i), i * 10));
        }
        assertEquals(200, tabla.tamanio());
        for (int i = 0; i < 200; i++) {
            assertEquals(i * 10, (int) tabla.obtener(new ClaveQueColisiona(i)));
        }
        assertEquals(50, (int) tabla.poner(new ClaveQueColisiona(5), -1));
        // Se eliminan las claves pares excepto la 4.
        for (int i = 0; i < 200; i += 2) {
            if (i != 4) {
                assertEquals(i * 10, (int) tabla.eliminar(new ClaveQueColisiona(i)));
            }
        }
        assertEquals(101, tabla.tamanio());
        assertEquals(40, (int) tabla.obtener(new ClaveQueColisiona(4)));
        assertTrue(tabla.contieneClave(new ClaveQueColisiona(4)));
        assertFalse(tabla.contieneClave(new ClaveQueColisiona(6)));
        assertEquals(-1, (int) tabla.obtener(new ClaveQueColisiona(5)));
    }

    @Test
    void muchasClavesTrasRedimensionamiento() {
        TablaHash<Integer, String> tabla = new TablaHashAdaptador<>();
        for (int i = 0; i < 10_000; i++) {
            tabla.poner(i, "v" + i);
        }
        assertEquals(10_000, tabla.tamanio());
        for (int i = 0; i < 10_000; i++) {
            assertEquals("v" + i, tabla.obtener(i));
        }
        assertEquals(10_000, tabla.claves().tamanio());
    }
}
