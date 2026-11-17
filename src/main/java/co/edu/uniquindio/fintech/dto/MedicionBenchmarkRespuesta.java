package co.edu.uniquindio.fintech.dto;

public record MedicionBenchmarkRespuesta(String operacion, String estructura, int elementos, int repeticiones,
                                         long promedioNanos, long minimoNanos, long maximoNanos, String resultado) {
}
