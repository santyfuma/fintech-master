package co.edu.uniquindio.fintech.structures.adaptadores;

import co.edu.uniquindio.fintech.structures.interfaces.Cola;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Objects;

/**
 * {@link Cola} FIFO implementada como adaptador sobre {@link ArrayDeque}
 * (arreglo circular redimensionable). Se encola por el final y se atiende por la cabeza.
 *
 * <p>Complejidad temporal:
 * <ul>
 *   <li>{@code encolar}: O(1) amortizado; O(n) cuando el arreglo interno se redimensiona.</li>
 *   <li>{@code desencolar}, {@code frente}, {@code tamanio}, {@code estaVacia}: O(1).</li>
 *   <li>{@code limpiar}: O(n).</li>
 * </ul>
 *
 * <p>No acepta {@code null} ({@link NullPointerException}). La iteración va del frente
 * al final y es de solo lectura.
 *
 * @param <T> tipo de los elementos
 */
public class ColaAdaptador<T> implements Cola<T> {

    private final ArrayDeque<T> elementos = new ArrayDeque<>();

    @Override
    public void encolar(T elemento) {
        elementos.addLast(Objects.requireNonNull(elemento, "elemento"));
    }

    @Override
    public T desencolar() {
        validarNoVacia();
        return elementos.pollFirst();
    }

    @Override
    public T frente() {
        validarNoVacia();
        return elementos.peekFirst();
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
            throw new IllegalStateException("La cola está vacía");
        }
    }
}
