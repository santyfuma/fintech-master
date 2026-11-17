package co.edu.uniquindio.fintech.structures.interfaces;

/**
 * Pila LIFO (último en entrar, primero en salir).
 *
 * <p>Uso previsto: operaciones reversibles e historial de deshacer.
 * La iteración recorre desde la cima hacia el fondo.
 *
 * @param <T> tipo de los elementos
 */
public interface Pila<T> extends Coleccion<T> {

    /** Coloca el elemento en la cima. */
    void apilar(T elemento);

    /**
     * Retira y devuelve el elemento de la cima.
     *
     * @throws IllegalStateException si la pila está vacía
     */
    T desapilar();

    /**
     * Devuelve el elemento de la cima sin retirarlo.
     *
     * @throws IllegalStateException si la pila está vacía
     */
    T cima();
}
