package co.edu.uniquindio.fintech.dto;

import java.time.LocalDateTime;

/** Resultado de procesar las operaciones vencidas hasta el instante del reloj. */
public record ProcesamientoRespuesta(LocalDateTime procesadoHasta, int ejecutadas, int fallidas,
                                     ResultadoEjecucionRespuesta[] resultados) {
}
