package co.edu.uniquindio.fintech.structures.interfaces;

/**
 * Contrato del árbol binario de búsqueda (implementación propia en
 * {@code structures.propias}, sin bibliotecas externas).
 *
 * <p>Las claves son únicas dentro del árbol. Para almacenar valores con la misma
 * clave lógica (por ejemplo, varios usuarios con los mismos puntos) se usa
 * {@link ClaveCompuesta}: la parte principal es el criterio de orden y la parte de
 * desempate (por ejemplo, la identificación del usuario) hace única la clave.
 *
 * <p>Uso previsto: organizar usuarios por puntos acumulados, búsquedas por rango
 * de puntos (niveles), reportes ordenados y transacciones de mayor valor.
 *
 * @param <K> tipo de la clave, con orden natural
 * @param <V> tipo del valor
 */
public interface ArbolBinarioBusqueda<K extends Comparable<? super K>, V> {

    // ---------------------------------------------------------------- inserción y búsqueda

    /**
     * Inserta el par clave-valor. Si la clave ya existe, reemplaza su valor.
     *
     * @return el valor anterior, o {@code null} si la clave era nueva
     */
    V insertar(K clave, V valor);

    /** @return el valor asociado a la clave, o {@code null} si no existe */
    V buscar(K clave);

    /** @return {@code true} si la clave existe */
    boolean contiene(K clave);

    // ---------------------------------------------------------------- eliminación

    /**
     * Elimina la clave (contemplando los casos de nodo hoja, con un hijo y con dos hijos).
     *
     * @return el valor eliminado, o {@code null} si la clave no existía
     */
    V eliminar(K clave);

    /** Elimina todos los nodos. */
    void limpiar();

    // ---------------------------------------------------------------- extremos

    /**
     * @return la clave mínima
     * @throws IllegalStateException si el árbol está vacío
     */
    K claveMinima();

    /**
     * @return la clave máxima
     * @throws IllegalStateException si el árbol está vacío
     */
    K claveMaxima();

    // ---------------------------------------------------------------- recorridos

    /** @return valores en orden ascendente de clave (izquierda, raíz, derecha) */
    Lista<V> inOrden();

    /** @return valores en orden descendente de clave (derecha, raíz, izquierda) */
    Lista<V> inOrdenInverso();

    /** @return valores en preorden (raíz, izquierda, derecha) */
    Lista<V> preOrden();

    /** @return valores en postorden (izquierda, derecha, raíz) */
    Lista<V> postOrden();

    /** @return valores por niveles, de la raíz hacia las hojas */
    Lista<V> porNiveles();

    /** @return claves en orden ascendente */
    Lista<K> claves();

    // ---------------------------------------------------------------- consultas por rango

    /**
     * Devuelve, en orden ascendente, los valores cuyas claves están en el intervalo
     * cerrado {@code [desde, hasta]}, visitando solo las ramas que pueden contenerlas.
     * Si {@code desde > hasta} devuelve una lista vacía.
     *
     * <p>Con {@link ClaveCompuesta} se usan cotas para consultar por la parte principal:
     * {@code rango(ClaveCompuesta.cotaInferior(501), ClaveCompuesta.cotaSuperior(1000))}.
     */
    Lista<V> rango(K desde, K hasta);

    /**
     * @param cantidad número máximo de valores a devolver (mayor o igual a 0)
     * @return los {@code cantidad} valores de mayor clave, en orden descendente
     */
    Lista<V> mayores(int cantidad);

    // ---------------------------------------------------------------- información

    /** @return número de nodos */
    int tamanio();

    /** @return {@code true} si no tiene nodos */
    boolean estaVacio();

    /** @return altura del árbol: -1 si está vacío, 0 si solo tiene raíz */
    int altura();
}
