package co.edu.uniquindio.fintech.model;

/**
 * Tipo de notificación: alertas del enunciado 4.7 más la alerta de seguridad de 4.9.
 */
public enum TipoNotificacion {
    SALDO_BAJO,
    OPERACION_PROGRAMADA_PROXIMA,
    OPERACION_RECHAZADA,
    ASCENSO_NIVEL,
    CANJE_REALIZADO,
    ALERTA_SEGURIDAD
}
