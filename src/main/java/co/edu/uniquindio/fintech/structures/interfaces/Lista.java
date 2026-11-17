package co.edu.uniquindio.fintech.structures.interfaces;

/**
 * Lista ordenada por posición (índices desde 0).
 *
 * <p>Uso previsto: historiales de transacciones por usuario y por billetera,
 * beneficios canjeados, operaciones de un período y resultados de consultas.
 *
 * @param <T> tipo de los elementos
 */
public interface Lista<T> extends Coleccion<T> {

    /** Agrega el elemento al final. */
    void agregar(T elemento);

    /**
     * Inserta el elemento en la posición indicada, desplazando los siguientes.
     *
     * @throws IndexOutOfBoundsException si {@code indice < 0 || indice > tamanio()}
     */
    void agregarEn(int indice, T elemento);

    /** @throws IndexOutOfBoundsException si el índice no existe */
    T obtener(int indice);

    /**
     * Reemplaza el elemento de la posición indicada.
     *
     * @return el elemento anterior
     * @throws IndexOutOfBoundsException si el índice no existe
     */
    T reemplazar(int indice, T elemento);

    /**
     * Elimina el elemento de la posición indicada.
     *
     * @return el elemento eliminado
     * @throws IndexOutOfBoundsException si el índice no existe
     */
    T eliminarEn(int indice);

    /**
     * Elimina la primera aparición del elemento (según {@code equals}).
     *
     * @return {@code true} si se eliminó
     */
    boolean eliminar(T elemento);

    /** @return {@code true} si la lista contiene el elemento (según {@code equals}) */
    boolean contiene(T elemento);

    /** @return índice de la primera aparición, o {@code -1} si no existe */
    int indiceDe(T elemento);
}
