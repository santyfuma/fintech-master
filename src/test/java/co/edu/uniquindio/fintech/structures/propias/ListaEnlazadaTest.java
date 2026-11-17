package co.edu.uniquindio.fintech.structures.propias;

import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListaEnlazadaTest {

    private static Lista<String> listaCon(String... elementos) {
        Lista<String> lista = new ListaEnlazada<>();
        for (String e : elementos) {
            lista.agregar(e);
        }
        return lista;
    }

    /** Contenido recorriendo con el iterador (desde la cabeza). */
    private static String contenido(Lista<String> lista) {
        StringBuilder sb = new StringBuilder("[");
        for (String e : lista) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(e);
        }
        return sb.append(']').toString();
    }

    /** Contenido usando obtener(i): ejercita el recorrido desde la cola en la mitad final. */
    private static String contenidoPorIndices(Lista<String> lista) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < lista.tamanio(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(lista.obtener(i));
        }
        return sb.append(']').toString();
    }

    // ------------------------------------------------------------------ vacía

    @Test
    void listaNuevaEstaVacia() {
        Lista<String> lista = new ListaEnlazada<>();
        assertTrue(lista.estaVacia());
        assertEquals(0, lista.tamanio());
        assertEquals("[]", contenido(lista));
        assertEquals(-1, lista.indiceDe("a"));
        assertFalse(lista.contiene("a"));
        assertFalse(lista.eliminar("a"));
    }

    @Test
    void operacionesPorIndiceSobreListaVaciaLanzanExcepcion() {
        Lista<String> lista = new ListaEnlazada<>();
        assertThrows(IndexOutOfBoundsException.class, () -> lista.obtener(0));
        assertThrows(IndexOutOfBoundsException.class, () -> lista.reemplazar(0, "a"));
        assertThrows(IndexOutOfBoundsException.class, () -> lista.eliminarEn(0));
    }

    @Test
    void iteradorDeListaVaciaNoTieneElementos() {
        Iterator<String> it = new ListaEnlazada<String>().iterator();
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    // ------------------------------------------------------------------ inserción

    @Test
    void agregarConservaElOrdenDeInsercion() {
        Lista<String> lista = listaCon("a", "b", "c");
        assertEquals(3, lista.tamanio());
        assertFalse(lista.estaVacia());
        assertEquals("[a, b, c]", contenido(lista));
        assertEquals("[a, b, c]", contenidoPorIndices(lista));
    }

    @Test
    void agregarEnInicioMedioYFinal() {
        Lista<String> lista = listaCon("b", "d");
        lista.agregarEn(0, "a");
        lista.agregarEn(2, "c");
        lista.agregarEn(4, "e");
        assertEquals("[a, b, c, d, e]", contenido(lista));
        assertEquals("[a, b, c, d, e]", contenidoPorIndices(lista));
        assertEquals(5, lista.tamanio());
    }

    @Test
    void agregarEnCeroSobreListaVacia() {
        Lista<String> lista = new ListaEnlazada<>();
        lista.agregarEn(0, "x");
        assertEquals("[x]", contenido(lista));
        lista.agregar("y");
        assertEquals("[x, y]", contenidoPorIndices(lista));
    }

    @Test
    void agregarEnIndiceInvalidoNoModificaLaLista() {
        Lista<String> lista = listaCon("a", "b");
        assertThrows(IndexOutOfBoundsException.class, () -> lista.agregarEn(-1, "x"));
        assertThrows(IndexOutOfBoundsException.class, () -> lista.agregarEn(3, "x"));
        assertEquals("[a, b]", contenido(lista));
        assertEquals(2, lista.tamanio());
    }

    @Test
    void permiteElementosDuplicados() {
        Lista<String> lista = listaCon("a", "a", "a");
        assertEquals(3, lista.tamanio());
    }

    // ------------------------------------------------------------------ consulta

    @Test
    void obtenerDevuelveCadaPosicionEnListaLarga() {
        Lista<String> lista = new ListaEnlazada<>();
        for (int i = 0; i < 11; i++) {
            lista.agregar("e" + i);
        }
        for (int i = 0; i < 11; i++) {
            assertEquals("e" + i, lista.obtener(i));
        }
    }

    @Test
    void obtenerConIndiceInvalidoLanzaExcepcion() {
        Lista<String> lista = listaCon("a", "b");
        assertThrows(IndexOutOfBoundsException.class, () -> lista.obtener(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> lista.obtener(2));
        assertThrows(IndexOutOfBoundsException.class, () -> lista.obtener(Integer.MAX_VALUE));
    }

    @Test
    void indiceDeYContieneDevuelvenLaPrimeraAparicion() {
        Lista<String> lista = listaCon("a", "b", "a", "c");
        assertEquals(0, lista.indiceDe("a"));
        assertEquals(3, lista.indiceDe("c"));
        assertEquals(-1, lista.indiceDe("z"));
        assertTrue(lista.contiene("b"));
        assertFalse(lista.contiene("z"));
    }

    @Test
    void comparaPorEqualsYNoPorReferencia() {
        Lista<String> lista = listaCon(new String("hola"));
        assertTrue(lista.contiene(new String("hola")));
        assertTrue(lista.eliminar(new String("hola")));
        assertTrue(lista.estaVacia());
    }

    // ------------------------------------------------------------------ reemplazo

    @Test
    void reemplazarDevuelveElAnteriorYNoCambiaElTamanio() {
        Lista<String> lista = listaCon("a", "b", "c");
        assertEquals("b", lista.reemplazar(1, "B"));
        assertEquals("c", lista.reemplazar(2, "C"));
        assertEquals("[a, B, C]", contenido(lista));
        assertEquals(3, lista.tamanio());
        assertThrows(IndexOutOfBoundsException.class, () -> lista.reemplazar(3, "x"));
        assertThrows(IndexOutOfBoundsException.class, () -> lista.reemplazar(-1, "x"));
    }

    // ------------------------------------------------------------------ eliminación

    @Test
    void eliminarEnPrimeroUltimoYMedio() {
        Lista<String> lista = listaCon("a", "b", "c", "d", "e");
        assertEquals("a", lista.eliminarEn(0));
        assertEquals("e", lista.eliminarEn(3));
        assertEquals("c", lista.eliminarEn(1));
        assertEquals("[b, d]", contenido(lista));
        assertEquals("[b, d]", contenidoPorIndices(lista));
        assertEquals(2, lista.tamanio());
    }

    @Test
    void eliminarElUnicoElementoDejaLaListaReutilizable() {
        Lista<String> lista = listaCon("a");
        assertEquals("a", lista.eliminarEn(0));
        assertTrue(lista.estaVacia());
        assertEquals("[]", contenido(lista));
        lista.agregar("b");
        lista.agregarEn(0, "a");
        assertEquals("[a, b]", contenido(lista));
        assertEquals("b", lista.obtener(1));
    }

    @Test
    void eliminarEnIndiceInvalidoLanzaExcepcion() {
        Lista<String> lista = listaCon("a");
        assertThrows(IndexOutOfBoundsException.class, () -> lista.eliminarEn(1));
        assertThrows(IndexOutOfBoundsException.class, () -> lista.eliminarEn(-1));
        assertEquals(1, lista.tamanio());
    }

    @Test
    void eliminarPorElementoQuitaSoloLaPrimeraAparicion() {
        Lista<String> lista = listaCon("a", "b", "a", "b");
        assertTrue(lista.eliminar("b"));
        assertEquals("[a, a, b]", contenido(lista));
        assertTrue(lista.eliminar("b"));
        assertFalse(lista.eliminar("b"));
        assertEquals("[a, a]", contenido(lista));
    }

    @Test
    void eliminarPorElementoEnExtremosMantieneCabezaYCola() {
        Lista<String> lista = listaCon("a", "b", "c");
        assertTrue(lista.eliminar("c"));
        lista.agregar("d");
        assertTrue(lista.eliminar("a"));
        lista.agregarEn(0, "z");
        assertEquals("[z, b, d]", contenido(lista));
        assertEquals("[z, b, d]", contenidoPorIndices(lista));
    }

    @Test
    void limpiarVaciaLaListaYPermiteReutilizarla() {
        Lista<String> lista = listaCon("a", "b", "c");
        lista.limpiar();
        assertTrue(lista.estaVacia());
        assertEquals(0, lista.tamanio());
        assertEquals("[]", contenido(lista));
        lista.agregar("x");
        assertEquals("[x]", contenido(lista));
    }

    // ------------------------------------------------------------------ nulos

    @Test
    void rechazaNulosEnTodasLasOperaciones() {
        Lista<String> lista = listaCon("a");
        assertThrows(NullPointerException.class, () -> lista.agregar(null));
        assertThrows(NullPointerException.class, () -> lista.agregarEn(0, null));
        assertThrows(NullPointerException.class, () -> lista.reemplazar(0, null));
        assertThrows(NullPointerException.class, () -> lista.eliminar(null));
        assertThrows(NullPointerException.class, () -> lista.contiene(null));
        assertThrows(NullPointerException.class, () -> lista.indiceDe(null));
        assertEquals("[a]", contenido(lista));
    }

    // ------------------------------------------------------------------ iterador

    @Test
    void iteradorEsDeSoloLectura() {
        Iterator<String> it = listaCon("a").iterator();
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    @Test
    void iteradorTerminaConNoSuchElement() {
        Iterator<String> it = listaCon("a", "b").iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    // ------------------------------------------------------------------ aleatoria

    @Test
    void secuenciaAleatoriaCoincideConArrayList() {
        Random azar = new Random(20261115L);
        Lista<String> lista = new ListaEnlazada<>();
        ArrayList<String> referencia = new ArrayList<>();
        for (int paso = 0; paso < 5000; paso++) {
            String valor = "v" + azar.nextInt(50);
            int operacion = azar.nextInt(6);
            if (operacion == 0 || referencia.isEmpty()) {
                lista.agregar(valor);
                referencia.add(valor);
            } else if (operacion == 1) {
                int i = azar.nextInt(referencia.size() + 1);
                lista.agregarEn(i, valor);
                referencia.add(i, valor);
            } else if (operacion == 2) {
                int i = azar.nextInt(referencia.size());
                assertEquals(referencia.remove(i), lista.eliminarEn(i));
            } else if (operacion == 3) {
                assertEquals(referencia.remove(valor), lista.eliminar(valor));
            } else if (operacion == 4) {
                int i = azar.nextInt(referencia.size());
                assertEquals(referencia.set(i, valor), lista.reemplazar(i, valor));
            } else {
                assertEquals(referencia.indexOf(valor), lista.indiceDe(valor));
            }
            assertEquals(referencia.size(), lista.tamanio());
        }
        assertEquals(referencia.toString(), contenido(lista));
        assertEquals(referencia.toString(), contenidoPorIndices(lista));
    }
}
