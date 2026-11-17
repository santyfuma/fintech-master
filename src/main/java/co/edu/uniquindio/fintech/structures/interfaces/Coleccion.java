package co.edu.uniquindio.fintech.structures.interfaces;

/**
 * Operaciones comunes a las estructuras lineales (lista, pila, cola y cola de prioridad).
 *
 * <p>Extiende {@link Iterable} (de {@code java.lang}) para que el código fuera de
 * {@code structures} pueda recorrerlas con {@code for-each} sin importar {@code java.util}.
 *
 * @param <T> tipo de los elementos
 */
public interface Coleccion<T> extends Iterable<T> {

    /** @return número de elementos almacenados */
    int tamanio();

    /** @return {@code true} si no contiene elementos */
    boolean estaVacia();

    /** Elimina todos los elementos. */
    void limpiar();
}
