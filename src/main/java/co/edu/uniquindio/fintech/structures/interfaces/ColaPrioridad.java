package co.edu.uniquindio.fintech.structures.interfaces;

/**
 * Cola de prioridad.
 *
 * <p>El orden lo define un {@link Comparador} recibido al construir la implementación:
 * el elemento <b>menor</b> según el comparador es el de <b>mayor prioridad</b>
 * (por ejemplo, la operación programada con la fecha y hora más próxima).
 * Para elementos con igual prioridad no se garantiza orden de llegada; si se
 * necesita, el comparador debe incluir un criterio de desempate.
 *
 * <p>Uso previsto: operaciones programadas y pagos automáticos según fecha.
 * La iteración no garantiza ningún orden.
 *
 * @param <T> tipo de los elementos
 */
public interface ColaPrioridad<T> extends Coleccion<T> {

    /** Inserta el elemento según su prioridad. */
    void insertar(T elemento);

    /**
     * Retira y devuelve el elemento de mayor prioridad.
     *
     * @throws IllegalStateException si la cola está vacía
     */
    T extraer();

    /**
     * Devuelve el elemento de mayor prioridad sin retirarlo.
     *
     * @throws IllegalStateException si la cola está vacía
     */
    T consultar();

    /**
     * Elimina un elemento cualquiera (según {@code equals}), por ejemplo una
     * operación programada cancelada.
     *
     * @return {@code true} si se eliminó
     */
    boolean eliminar(T elemento);
}
