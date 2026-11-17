package co.edu.uniquindio.fintech.structures.interfaces;

/**
 * Tabla hash clave-valor con claves únicas.
 *
 * <p>Uso previsto: acceso rápido a usuarios por identificación, billeteras por
 * código y consultas rápidas de saldos, puntos o configuraciones.
 * Las claves deben implementar {@code equals} y {@code hashCode} de forma consistente.
 *
 * @param <K> tipo de la clave
 * @param <V> tipo del valor
 */
public interface TablaHash<K, V> {

    /**
     * Asocia el valor a la clave, reemplazando el valor previo si existía.
     *
     * @return el valor anterior, o {@code null} si la clave no existía
     */
    V poner(K clave, V valor);

    /** @return el valor asociado, o {@code null} si la clave no existe */
    V obtener(K clave);

    /** @return {@code true} si la clave existe */
    boolean contieneClave(K clave);

    /**
     * Elimina la clave y su valor.
     *
     * @return el valor eliminado, o {@code null} si la clave no existía
     */
    V eliminar(K clave);

    /** @return número de pares almacenados */
    int tamanio();

    /** @return {@code true} si no hay pares almacenados */
    boolean estaVacia();

    /** Elimina todos los pares. */
    void limpiar();

    /** @return copia de las claves, sin orden garantizado */
    Lista<K> claves();

    /** @return copia de los valores, sin orden garantizado */
    Lista<V> valores();
}
