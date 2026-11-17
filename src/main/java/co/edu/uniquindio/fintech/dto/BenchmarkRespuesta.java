package co.edu.uniquindio.fintech.dto;

/** Mediciones de una comparación de estructuras y sus limitaciones (no es un benchmark riguroso). */
public record BenchmarkRespuesta(String comparacion, int elementos, int repeticiones,
                                 MedicionBenchmarkRespuesta[] mediciones, String limitaciones) {
}
