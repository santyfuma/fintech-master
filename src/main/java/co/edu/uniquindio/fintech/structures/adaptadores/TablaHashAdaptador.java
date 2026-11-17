package co.edu.uniquindio.fintech.structures.adaptadores;

import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import co.edu.uniquindio.fintech.structures.interfaces.TablaHash;
import co.edu.uniquindio.fintech.structures.propias.ListaEnlazada;

import java.util.HashMap;
import java.util.Objects;

/**
 * {@link TablaHash} implementada como adaptador sobre {@link HashMap}
 * (tabla con encadenamiento, factor de carga 0,75 y redimensionamiento automático).
 *
 * <p>Complejidad temporal (n = pares, c = capacidad interna):
 * <ul>
 *   <li>{@code poner}: promedio O(1) amortizado (el redimensionamiento cuesta O(n)).</li>
 *   <li>{@code obtener}, {@code contieneClave}, {@code eliminar}: promedio O(1).</li>
 *   <li>Peor caso de las cuatro anteriores (muchas claves en la misma cubeta): O(n).
 *       {@code HashMap} convierte las cubetas grandes en árboles, lo que lo reduce a
 *       O(log n) solo si las claves son {@link Comparable}.</li>
 *   <li>{@code tamanio}, {@code estaVacia}: O(1).</li>
 *   <li>{@code claves}, {@code valores}: O(n + c) (recorren la tabla y copian a una lista).</li>
 *   <li>{@code limpiar}: O(c).</li>
 * </ul>
 * El promedio O(1) supone un {@code hashCode} que distribuya bien las claves.
 *
 * <p>No acepta claves ni valores {@code null} ({@link NullPointerException}). Poner una
 * clave existente reemplaza su valor y devuelve el anterior.
 *
 * @param <K> tipo de la clave
 * @param <V> tipo del valor
 */
public class TablaHashAdaptador<K, V> implements TablaHash<K, V> {

    private final HashMap<K, V> pares = new HashMap<>();

    @Override
    public V poner(K clave, V valor) {
        Objects.requireNonNull(clave, "clave");
        Objects.requireNonNull(valor, "valor");
        return pares.put(clave, valor);
    }

    @Override
    public V obtener(K clave) {
        return pares.get(Objects.requireNonNull(clave, "clave"));
    }

    @Override
    public boolean contieneClave(K clave) {
        return pares.containsKey(Objects.requireNonNull(clave, "clave"));
    }

    @Override
    public V eliminar(K clave) {
        return pares.remove(Objects.requireNonNull(clave, "clave"));
    }

    @Override
    public int tamanio() {
        return pares.size();
    }

    @Override
    public boolean estaVacia() {
        return pares.isEmpty();
    }

    @Override
    public void limpiar() {
        pares.clear();
    }

    @Override
    public Lista<K> claves() {
        Lista<K> copia = new ListaEnlazada<>();
        for (K clave : pares.keySet()) {
            copia.agregar(clave);
        }
        return copia;
    }

    @Override
    public Lista<V> valores() {
        Lista<V> copia = new ListaEnlazada<>();
        for (V valor : pares.values()) {
            copia.agregar(valor);
        }
        return copia;
    }
}
