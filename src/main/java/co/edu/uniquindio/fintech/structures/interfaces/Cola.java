package co.edu.uniquindio.fintech.structures.interfaces;

/**
 * Cola FIFO (primero en entrar, primero en salir).
 *
 * <p>Uso previsto: notificaciones pendientes de despacho.
 * La iteración recorre desde el frente hacia el final.
 *
 * @param <T> tipo de los elementos
 */
public interface Cola<T> extends Coleccion<T> {

    /** Agrega el elemento al final de la cola. */
    void encolar(T elemento);

    /**
     * Retira y devuelve el elemento del frente.
     *
     * @throws IllegalStateException si la cola está vacía
     */
    T desencolar();

    /**
     * Devuelve el elemento del frente sin retirarlo.
     *
     * @throws IllegalStateException si la cola está vacía
     */
    T frente();
}
