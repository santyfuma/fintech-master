package co.edu.uniquindio.fintech.dto;

import co.edu.uniquindio.fintech.model.EstadoOperacionProgramada;

/** Resultado de ejecutar una operación programada vencida. */
public record ResultadoEjecucionRespuesta(String operacion, String transaccion, EstadoOperacionProgramada estado,
                                          String motivo, String siguienteOperacion) {
}
