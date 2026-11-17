package co.edu.uniquindio.fintech.structures.interfaces;

import java.math.BigDecimal;

/**
 * Arista dirigida y ponderada de un {@link GrafoDirigidoPonderado}.
 * Es un valor inmutable devuelto por las consultas del grafo.
 *
 * @param origen  vértice de salida
 * @param destino vértice de llegada
 * @param peso    peso de la arista (no negativo, exacto)
 * @param <V>     tipo de los vértices
 */
public record Arista<V>(V origen, V destino, BigDecimal peso) {
}
