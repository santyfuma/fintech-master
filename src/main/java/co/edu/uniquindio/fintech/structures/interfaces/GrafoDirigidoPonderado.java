package co.edu.uniquindio.fintech.structures.interfaces;

import java.math.BigDecimal;

/**
 * Contrato del grafo dirigido ponderado (implementación propia en
 * {@code structures.propias}, sin bibliotecas externas).
 *
 * <p>Entre un par ordenado de vértices existe como máximo una arista. Los pesos son
 * {@link BigDecimal} no negativos y no nulos, para operar montos sin errores de
 * redondeo; se comparan con {@code compareTo} (no con {@code equals}, que distingue
 * la escala). El significado del peso (monto acumulado, número de transferencias…)
 * lo decide la capa de servicio, no el grafo.
 *
 * <p>Uso previsto: transferencias entre usuarios, relaciones entre billeteras, rutas
 * frecuentes de movimiento de dinero, ciclos y patrones de interacción.
 *
 * @param <V> tipo de los vértices (identificadores; deben implementar
 *            {@code equals} y {@code hashCode})
 */
public interface GrafoDirigidoPonderado<V> {

    // ---------------------------------------------------------------- vértices

    /** @return {@code true} si el vértice se agregó; {@code false} si ya existía */
    boolean agregarVertice(V vertice);

    /**
     * Elimina el vértice y todas sus aristas de entrada y salida.
     *
     * @return {@code true} si existía
     */
    boolean eliminarVertice(V vertice);

    boolean contieneVertice(V vertice);

    /** @return copia de los vértices, sin orden garantizado */
    Lista<V> vertices();

    int cantidadVertices();

    // ---------------------------------------------------------------- aristas y pesos

    /**
     * Crea la arista {@code origen -> destino} o reemplaza su peso.
     * Agrega los vértices que no existan.
     *
     * @throws IllegalArgumentException si el peso es negativo
     */
    void agregarArista(V origen, V destino, BigDecimal peso);

    /**
     * Suma {@code incremento} al peso de la arista; si no existe la crea con ese peso.
     * Agrega los vértices que no existan. Sirve para acumular transferencias repetidas.
     *
     * @throws IllegalArgumentException si el incremento es negativo
     */
    void incrementarPeso(V origen, V destino, BigDecimal incremento);

    /** @return {@code true} si la arista existía */
    boolean eliminarArista(V origen, V destino);

    boolean contieneArista(V origen, V destino);

    /**
     * @return peso de la arista
     * @throws IllegalArgumentException si la arista no existe
     */
    BigDecimal peso(V origen, V destino);

    /** @return copia de todas las aristas, sin orden garantizado */
    Lista<Arista<V>> aristas();

    int cantidadAristas();

    /**
     * @return aristas que salen del vértice
     * @throws IllegalArgumentException si el vértice no existe
     */
    Lista<Arista<V>> aristasSalientes(V vertice);

    /**
     * @return aristas que llegan al vértice
     * @throws IllegalArgumentException si el vértice no existe
     */
    Lista<Arista<V>> aristasEntrantes(V vertice);

    /** @throws IllegalArgumentException si el vértice no existe */
    int gradoSalida(V vertice);

    /** @throws IllegalArgumentException si el vértice no existe */
    int gradoEntrada(V vertice);

    // ---------------------------------------------------------------- recorridos

    /**
     * Recorrido en anchura (BFS) desde el vértice indicado.
     *
     * @return vértices alcanzables en orden de visita, iniciando por {@code inicio}
     * @throws IllegalArgumentException si el vértice no existe
     */
    Lista<V> recorridoAnchura(V inicio);

    /**
     * Recorrido en profundidad (DFS) desde el vértice indicado.
     *
     * @return vértices alcanzables en orden de visita, iniciando por {@code inicio}
     * @throws IllegalArgumentException si el vértice no existe
     */
    Lista<V> recorridoProfundidad(V inicio);

    // ---------------------------------------------------------------- rutas

    /** @return {@code true} si existe un camino dirigido de {@code origen} a {@code destino} */
    boolean existeRuta(V origen, V destino);

    /**
     * Ruta con menor número de aristas (BFS).
     *
     * @return vértices de la ruta incluyendo extremos; lista vacía si no existe
     */
    Lista<V> rutaMenosSaltos(V origen, V destino);

    /**
     * Ruta de menor peso total (Dijkstra; válido porque los pesos son no negativos).
     *
     * @return vértices de la ruta incluyendo extremos; lista vacía si no existe
     */
    Lista<V> rutaMenorPeso(V origen, V destino);

    /**
     * @return peso total de {@link #rutaMenorPeso}, {@code BigDecimal.ZERO} si
     *         {@code origen} y {@code destino} son el mismo vértice, o {@code null} si no
     *         existe ruta
     */
    BigDecimal pesoRutaMenor(V origen, V destino);

    // ---------------------------------------------------------------- ciclos

    /** @return {@code true} si el grafo contiene al menos un ciclo dirigido */
    boolean tieneCiclo();

    /**
     * Ciclos dirigidos simples que pasan por el vértice indicado, con a lo sumo
     * {@code longitudMaxima} aristas (la cota evita la explosión combinatoria).
     * Cada ciclo se representa empezando y terminando en {@code vertice},
     * por ejemplo {@code [A, B, C, A]}.
     *
     * @throws IllegalArgumentException si el vértice no existe o la longitud es menor que 1
     */
    Lista<Lista<V>> ciclosQueIncluyen(V vertice, int longitudMaxima);

    // ---------------------------------------------------------------- general

    boolean estaVacio();

    /** Elimina todos los vértices y aristas. */
    void limpiar();
}
