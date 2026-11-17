package co.edu.uniquindio.fintech.model;

/**
 * Estado de una transacción.
 * <ul>
 *   <li>EXITOSA: se aplicó.</li>
 *   <li>RECHAZADA: no se aplicó (se registra igual, enunciado 4.2 y 4.7).</li>
 *   <li>REVERTIDA: se aplicó y luego otra transacción de tipo REVERSION la anuló.</li>
 * </ul>
 */
public enum EstadoTransaccion {
    EXITOSA,
    RECHAZADA,
    REVERTIDA
}
