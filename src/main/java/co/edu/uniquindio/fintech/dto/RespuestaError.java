package co.edu.uniquindio.fintech.dto;

import java.time.LocalDateTime;

/**
 * Cuerpo uniforme de error de la API (docs/API.md).
 *
 * @param estado  código HTTP
 * @param error   código del error, en mayúsculas (p. ej. {@code SALDO_INSUFICIENTE})
 * @param mensaje explicación para el cliente, sin detalles técnicos internos
 * @param fecha   fecha y hora del error según el {@code Clock} de la aplicación
 */
public record RespuestaError(int estado, String error, String mensaje, LocalDateTime fecha) {
}
