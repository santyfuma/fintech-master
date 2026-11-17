package co.edu.uniquindio.fintech.structures.adaptadores;

import co.edu.uniquindio.fintech.structures.interfaces.Pila;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Objects;

/**
 * {@link Pila} implementada como adaptador sobre {@link ArrayDeque} (arreglo circular
 * redimensionable). La cima es la cabeza del deque.
 *
 * <p>Complejidad temporal:
 * <ul>
 *   <li>{@code apilar}: O(1) amortizado; O(n) cuando el arreglo interno se redimensiona.</li>
 *   <li>{@code desapilar}, {@code cima}, {@code tamanio}, {@code estaVacia}: O(1).</li>
 *   <li>{@code limpiar}: O(n).</li>
 * </ul>
 *
 * <p>No acepta {@code null} ({@link NullPointerException}). La iteración va de la cima
 * al fondo y es de solo lectura.
 *
 * @param <T> tipo de los elementos
 */
public class PilaAdaptador<T> implements Pila<T> {

    private final ArrayDeque<T> elementos = new ArrayDeque<>();

    @Override
    public void apilar(T elemento) {
        elementos.push(Objects.requireNonNull(elemento, "elemento"));
    }

    @Override
    public T desapilar() {
        validarNoVacia();
        return elementos.pop();
    }

    @Override
    public T cima() {
        validarNoVacia();
        return elementos.peek();
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
            throw new IllegalStateException("La pila está vacía");
        }
    }
}
