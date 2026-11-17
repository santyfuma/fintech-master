package co.edu.uniquindio.fintech.model;

/**
 * Nivel de riesgo de una transacción o de un evento de auditoría (enunciado 4.9).
 *
 * <p><b>Provisional, pendiente de confirmación:</b> la escala BAJO/MEDIO/ALTO no la fija
 * el enunciado. NINGUNO marca las transacciones sin patrón inusual. Los umbrales que
 * asignan cada nivel siguen pendientes (CONTEXTO.md, sección 11).
 */
public enum NivelRiesgo {
    NINGUNO,
    BAJO,
    MEDIO,
    ALTO
}
