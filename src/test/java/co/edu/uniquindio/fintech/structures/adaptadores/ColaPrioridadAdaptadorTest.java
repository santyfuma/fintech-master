package co.edu.uniquindio.fintech.structures.adaptadores;

import co.edu.uniquindio.fintech.structures.interfaces.ColaPrioridad;
import co.edu.uniquindio.fintech.structures.interfaces.Comparador;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColaPrioridadAdaptadorTest {

    private static final Comparador<Integer> ASCENDENTE = Integer::compare;

    /** Elemento con prioridad y desempate, como una operación programada (fecha, consecutivo). */
    private record Tarea(int prioridad, int llegada) {
    }

    private static final Comparador<Tarea> POR_PRIORIDAD_Y_LLEGADA = (a, b) -> {
        int c = Integer.compare(a.prioridad(), b.prioridad());
        return c != 0 ? c : Integer.compare(a.llegada(), b.llegada());
    };

    @Test
    void colaNuevaEstaVacia() {
        ColaPrioridad<Integer> cola = new ColaPrioridadAdaptador<>(ASCENDENTE);
        assertTrue(cola.estaVacia());
        assertEquals(0, cola.tamanio());
        assertFalse(cola.iterator().hasNext());
    }

    @Test
    void extraerYConsultarSobreColaVaciaLanzanIllegalState() {
        ColaPrioridad<Integer> cola = new ColaPrioridadAdaptador<>(ASCENDENTE);
        assertThrows(IllegalStateException.class, cola::extraer);
        assertThrows(IllegalStateException.class, cola::consultar);
    }

    @Test
    void extraeElMenorSegunElComparador() {
        ColaPrioridad<Integer> cola = new ColaPrioridadAdaptador<>(ASCENDENTE);
        cola.insertar(5);
        cola.insertar(1);
        cola.insertar(4);
        cola.insertar(2);
        cola.insertar(3);
        assertEquals(5, cola.tamanio());
        for (int esperado = 1; esperado <= 5; esperado++) {
            assertEquals(esperado, (int) cola.extraer());
        }
        assertTrue(cola.estaVacia());
    }

    @Test
    void conComparadorInvertidoExtraeElMayor() {
        ColaPrioridad<Integer> cola = new ColaPrioridadAdaptador<>(ASCENDENTE.invertido());
        cola.insertar(2);
        cola.insertar(9);
        cola.insertar(5);
        assertEquals(9, (int) cola.extraer());
        assertEquals(5, (int) cola.extraer());
        assertEquals(2, (int) cola.extraer());
    }

    @Test
    void consultarNoRetira() {
        ColaPrioridad<Integer> cola = new ColaPrioridadAdaptador<>(ASCENDENTE);
        cola.insertar(7);
        cola.insertar(3);
        assertEquals(3, (int) cola.consultar());
        assertEquals(3, (int) cola.consultar());
        assertEquals(2, cola.tamanio());
    }

    @Test
    void prioridadesRepetidasSeExtraenTodas() {
        ColaPrioridad<Integer> cola = new ColaPrioridadAdaptador<>(ASCENDENTE);
        cola.insertar(2);
        cola.insertar(1);
        cola.insertar(2);
        cola.insertar(1);
        assertEquals(1, (int) cola.extraer());
        assertEquals(1, (int) cola.extraer());
        assertEquals(2, (int) cola.extraer());
        assertEquals(2, (int) cola.extraer());
        assertTrue(cola.estaVacia());
    }

    @Test
    void elDesempateDelComparadorDefineElOrdenEntreIgualPrioridad() {
        ColaPrioridad<Tarea> cola = new ColaPrioridadAdaptador<>(POR_PRIORIDAD_Y_LLEGADA);
        cola.insertar(new Tarea(2, 1));
        cola.insertar(new Tarea(1, 3));
        cola.insertar(new Tarea(1, 2));
        cola.insertar(new Tarea(2, 0));
        assertEquals(new Tarea(1, 2), cola.extraer());
        assertEquals(new Tarea(1, 3), cola.extraer());
        assertEquals(new Tarea(2, 0), cola.extraer());
        assertEquals(new Tarea(2, 1), cola.extraer());
    }

    @Test
    void eliminarQuitaUnElementoYConservaElOrden() {
        ColaPrioridad<Integer> cola = new ColaPrioridadAdaptador<>(ASCENDENTE);
        for (int v : new int[]{4, 1, 3, 2, 5}) {
            cola.insertar(v);
        }
        assertTrue(cola.eliminar(1));
        assertTrue(cola.eliminar(4));
        assertFalse(cola.eliminar(99));
        assertEquals(3, cola.tamanio());
        assertEquals(2, (int) cola.extraer());
        assertEquals(3, (int) cola.extraer());
        assertEquals(5, (int) cola.extraer());
    }

    @Test
    void eliminarConDuplicadosQuitaSoloUno() {
        ColaPrioridad<Integer> cola = new ColaPrioridadAdaptador<>(ASCENDENTE);
        cola.insertar(1);
        cola.insertar(1);
        assertTrue(cola.eliminar(1));
        assertEquals(1, cola.tamanio());
        assertEquals(1, (int) cola.consultar());
    }

    @Test
    void eliminarSobreColaVaciaDevuelveFalse() {
        ColaPrioridad<Integer> cola = new ColaPrioridadAdaptador<>(ASCENDENTE);
        assertFalse(cola.eliminar(1));
    }

    @Test
    void rechazaNulos() {
        ColaPrioridad<Integer> cola = new ColaPrioridadAdaptador<>(ASCENDENTE);
        assertThrows(NullPointerException.class, () -> cola.insertar(null));
        assertThrows(NullPointerException.class, () -> cola.eliminar(null));
        assertThrows(NullPointerException.class, () -> new ColaPrioridadAdaptador<Integer>(null));
        assertTrue(cola.estaVacia());
    }

    @Test
    void iteradorRecorreTodosYEsDeSoloLectura() {
        ColaPrioridad<Integer> cola = new ColaPrioridadAdaptador<>(ASCENDENTE);
        cola.insertar(3);
        cola.insertar(1);
        cola.insertar(2);
        int suma = 0;
        for (int v : cola) {
            suma += v;
        }
        assertEquals(6, suma);
        Iterator<Integer> it = cola.iterator();
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove);
        assertEquals(3, cola.tamanio());
    }

    @Test
    void limpiarVaciaLaCola() {
        ColaPrioridad<Integer> cola = new ColaPrioridadAdaptador<>(ASCENDENTE);
        cola.insertar(1);
        cola.insertar(2);
        cola.limpiar();
        assertTrue(cola.estaVacia());
        assertThrows(IllegalStateException.class, cola::consultar);
        cola.insertar(8);
        assertEquals(8, (int) cola.consultar());
    }

    @Test
    void valoresAleatoriosSalenOrdenados() {
        Random azar = new Random(20261115L);
        int[] valores = new int[2000];
        ColaPrioridad<Integer> cola = new ColaPrioridadAdaptador<>(ASCENDENTE);
        for (int i = 0; i < valores.length; i++) {
            valores[i] = azar.nextInt(500) - 250;
            cola.insertar(valores[i]);
        }
        Arrays.sort(valores);
        for (int esperado : valores) {
            assertEquals(esperado, (int) cola.extraer());
        }
        assertTrue(cola.estaVacia());
    }
}
