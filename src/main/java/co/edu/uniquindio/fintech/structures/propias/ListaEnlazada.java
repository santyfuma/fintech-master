package co.edu.uniquindio.fintech.structures.propias;

import co.edu.uniquindio.fintech.structures.interfaces.Lista;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Implementación propia de {@link Lista} mediante nodos doblemente enlazados,
 * con referencias a la cabeza y a la cola y un contador de tamaño.
 *
 * <p>Complejidad temporal (n = tamaño, i = índice):
 * <ul>
 *   <li>{@code agregar}, {@code tamanio}, {@code estaVacia}, {@code limpiar}: O(1).</li>
 *   <li>{@code agregarEn}, {@code obtener}, {@code reemplazar}, {@code eliminarEn}:
 *       O(min(i, n - i)), porque se recorre desde el extremo más cercano;
 *       O(n) en el peor caso. Insertar o eliminar en los extremos es O(1).</li>
 *   <li>{@code eliminar}, {@code contiene}, {@code indiceDe}: O(n) (búsqueda lineal).</li>
 *   <li>Recorrido completo con el iterador: O(n). Usar {@code obtener(i)} dentro de un
 *       ciclo sobre todos los índices cuesta O(n²); para recorrer se usa {@code for-each}.</li>
 * </ul>
 *
 * <p>No acepta {@code null} ({@link NullPointerException}). No está sincronizada y no
 * debe modificarse mientras se recorre. El iterador es de solo lectura.
 *
 * @param <T> tipo de los elementos
 */
public class ListaEnlazada<T> implements Lista<T> {

    private static final class Nodo<T> {
        private T dato;
        private Nodo<T> anterior;
        private Nodo<T> siguiente;

        private Nodo(T dato) {
            this.dato = dato;
        }
    }

    private Nodo<T> cabeza;
    private Nodo<T> cola;
    private int tamanio;

    @Override
    public void agregar(T elemento) {
        Objects.requireNonNull(elemento, "elemento");
        enlazarAlFinal(new Nodo<>(elemento));
    }

    @Override
    public void agregarEn(int indice, T elemento) {
        Objects.requireNonNull(elemento, "elemento");
        if (indice < 0 || indice > tamanio) {
            throw new IndexOutOfBoundsException(mensajeIndice(indice));
        }
        Nodo<T> nuevo = new Nodo<>(elemento);
        if (indice == tamanio) {
            enlazarAlFinal(nuevo);
            return;
        }
        Nodo<T> actual = nodoEn(indice);
        nuevo.siguiente = actual;
        nuevo.anterior = actual.anterior;
        if (actual.anterior == null) {
            cabeza = nuevo;
        } else {
            actual.anterior.siguiente = nuevo;
        }
        actual.anterior = nuevo;
        tamanio++;
    }

    @Override
    public T obtener(int indice) {
        validarIndiceExistente(indice);
        return nodoEn(indice).dato;
    }

    @Override
    public T reemplazar(int indice, T elemento) {
        Objects.requireNonNull(elemento, "elemento");
        validarIndiceExistente(indice);
        Nodo<T> nodo = nodoEn(indice);
        T anterior = nodo.dato;
        nodo.dato = elemento;
        return anterior;
    }

    @Override
    public T eliminarEn(int indice) {
        validarIndiceExistente(indice);
        Nodo<T> nodo = nodoEn(indice);
        desenlazar(nodo);
        return nodo.dato;
    }

    @Override
    public boolean eliminar(T elemento) {
        Objects.requireNonNull(elemento, "elemento");
        for (Nodo<T> actual = cabeza; actual != null; actual = actual.siguiente) {
            if (actual.dato.equals(elemento)) {
                desenlazar(actual);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contiene(T elemento) {
        return indiceDe(elemento) >= 0;
    }

    @Override
    public int indiceDe(T elemento) {
        Objects.requireNonNull(elemento, "elemento");
        int indice = 0;
        for (Nodo<T> actual = cabeza; actual != null; actual = actual.siguiente) {
            if (actual.dato.equals(elemento)) {
                return indice;
            }
            indice++;
        }
        return -1;
    }

    @Override
    public int tamanio() {
        return tamanio;
    }

    @Override
    public boolean estaVacia() {
        return tamanio == 0;
    }

    @Override
    public void limpiar() {
        cabeza = null;
        cola = null;
        tamanio = 0;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private Nodo<T> actual = cabeza;

            @Override
            public boolean hasNext() {
                return actual != null;
            }

            @Override
            public T next() {
                if (actual == null) {
                    throw new NoSuchElementException();
                }
                T dato = actual.dato;
                actual = actual.siguiente;
                return dato;
            }
        };
    }

    // ------------------------------------------------------------------ auxiliares

    private void enlazarAlFinal(Nodo<T> nuevo) {
        if (cola == null) {
            cabeza = nuevo;
        } else {
            cola.siguiente = nuevo;
            nuevo.anterior = cola;
        }
        cola = nuevo;
        tamanio++;
    }

    private void desenlazar(Nodo<T> nodo) {
        if (nodo.anterior == null) {
            cabeza = nodo.siguiente;
        } else {
            nodo.anterior.siguiente = nodo.siguiente;
        }
        if (nodo.siguiente == null) {
            cola = nodo.anterior;
        } else {
            nodo.siguiente.anterior = nodo.anterior;
        }
        nodo.anterior = null;
        nodo.siguiente = null;
        tamanio--;
    }

    /** Precondición: 0 <= indice < tamanio. Recorre desde el extremo más cercano. */
    private Nodo<T> nodoEn(int indice) {
        Nodo<T> actual;
        if (indice < tamanio / 2) {
            actual = cabeza;
            for (int i = 0; i < indice; i++) {
                actual = actual.siguiente;
            }
        } else {
            actual = cola;
            for (int i = tamanio - 1; i > indice; i--) {
                actual = actual.anterior;
            }
        }
        return actual;
    }

    private void validarIndiceExistente(int indice) {
        if (indice < 0 || indice >= tamanio) {
            throw new IndexOutOfBoundsException(mensajeIndice(indice));
        }
    }

    private String mensajeIndice(int indice) {
        return "Índice " + indice + " fuera de rango para tamaño " + tamanio;
    }
}
