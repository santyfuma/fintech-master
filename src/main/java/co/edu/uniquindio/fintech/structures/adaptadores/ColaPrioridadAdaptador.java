package co.edu.uniquindio.fintech.structures.adaptadores;

import co.edu.uniquindio.fintech.structures.interfaces.ColaPrioridad;
import co.edu.uniquindio.fintech.structures.interfaces.Comparador;

import java.util.Iterator;
import java.util.Objects;
import java.util.PriorityQueue;

/**
 * {@link ColaPrioridad} implementada como adaptador sobre {@link PriorityQueue}
 * (montículo binario mínimo en un arreglo). El {@link Comparador} propio recibido en
 * el constructor se traduce internamente; el elemento menor es el más prioritario.
 *
 * <p>Complejidad temporal:
 * <ul>
 *   <li>{@code insertar}: O(log n) amortizado; O(n) cuando el arreglo interno se redimensiona.</li>
 *   <li>{@code extraer}: O(log n).</li>
 *   <li>{@code consultar}, {@code tamanio}, {@code estaVacia}: O(1).</li>
 *   <li>{@code eliminar(elemento)}: O(n) (búsqueda lineal + reacomodo O(log n)).</li>
 *   <li>{@code limpiar}: O(n).</li>
 * </ul>
 *
 * <p>No acepta {@code null} ({@link NullPointerException}). Entre elementos de igual
 * prioridad no hay orden garantizado. La iteración no sigue ningún orden y es de solo lectura.
 *
 * @param <T> tipo de los elementos
 */
public class ColaPrioridadAdaptador<T> implements ColaPrioridad<T> {

    private final PriorityQueue<T> elementos;

    /**
     * @param comparador criterio de prioridad: el menor según él sale primero
     * @throws NullPointerException si el comparador es {@code null}
     */
    public ColaPrioridadAdaptador(Comparador<T> comparador) {
        Objects.requireNonNull(comparador, "comparador");
        this.elementos = new PriorityQueue<>(comparador::comparar);
    }

    @Override
    public void insertar(T elemento) {
        elementos.add(Objects.requireNonNull(elemento, "elemento"));
    }

    @Override
    public T extraer() {
        validarNoVacia();
        return elementos.poll();
    }

    @Override
    public T consultar() {
        validarNoVacia();
        return elementos.peek();
    }

    @Override
    public boolean eliminar(T elemento) {
        return elementos.remove(Objects.requireNonNull(elemento, "elemento"));
    }

    @Override
    public int tamanio() {
        return elementos.size();
    }

    @Override
    public boolean estaVacia() {
        return elementos.isEmpty();
    }

    @Override
    public void limpiar() {
        elementos.clear();
    }

    @Override
    public Iterator<T> iterator() {
        return new IteradorSoloLectura<>(elementos.iterator());
    }

    private void validarNoVacia() {
        if (elementos.isEmpty()) {
            throw new IllegalStateException("La cola de prioridad está vacía");
        }
    }
}
