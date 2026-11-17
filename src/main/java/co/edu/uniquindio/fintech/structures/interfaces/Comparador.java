package co.edu.uniquindio.fintech.structures.interfaces;

/**
 * Criterio de orden propio del proyecto.
 *
 * <p>Existe para que el código fuera de {@code structures} pueda definir órdenes
 * (por ejemplo, por fecha de ejecución) mediante lambdas sin importar
 * {@code java.util.Comparator}. Los adaptadores lo traducen internamente.
 *
 * @param <T> tipo de los elementos comparados
 */
@FunctionalInterface
public interface Comparador<T> {

    /**
     * @return negativo si {@code a} va antes que {@code b}, cero si son equivalentes
     *         y positivo si {@code a} va después que {@code b}
     */
    int comparar(T a, T b);

    /** @return un comparador con el orden inverso a este */
    default Comparador<T> invertido() {
        return (a, b) -> comparar(b, a);
    }
}
