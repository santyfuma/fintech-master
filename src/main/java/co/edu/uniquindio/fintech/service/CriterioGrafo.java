package co.edu.uniquindio.fintech.service;

/**
 * Qué representa el peso de una arista del grafo de transferencias (API.md 10).
 * <b>Pendiente</b> en el enunciado: se ofrecen ambos y la API permite elegir; el valor por
 * defecto es configurable ({@code fintech.grafo.peso-por-defecto}).
 */
public enum CriterioGrafo {
    /** Número de transferencias exitosas entre los dos usuarios. */
    CANTIDAD,
    /** Monto acumulado de esas transferencias. */
    MONTO
}
