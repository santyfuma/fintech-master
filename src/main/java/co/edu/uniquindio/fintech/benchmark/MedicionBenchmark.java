package co.edu.uniquindio.fintech.benchmark;

/**
 * Resultado agregado de una estructura en una comparación.
 *
 * @param operacion     operación medida
 * @param estructura    estructura usada
 * @param elementos     cantidad de elementos
 * @param repeticiones  repeticiones medidas (sin contar el calentamiento)
 * @param promedioNanos tiempo promedio por repetición (System.nanoTime)
 * @param minimoNanos   tiempo mínimo
 * @param maximoNanos   tiempo máximo
 * @param resultado     verificación del resultado (debe coincidir entre estructuras)
 */
public record MedicionBenchmark(String operacion, String estructura, int elementos, int repeticiones,
                                long promedioNanos, long minimoNanos, long maximoNanos, String resultado) {
}
