package co.edu.uniquindio.fintech.model;

/**
 * Estado de una operación programada.
 *
 * <p>Cómo se reprograma una operación periódica tras ejecutarse (nueva fecha o nuevo
 * registro) depende de la modalidad de ejecución, pendiente de decidir.
 */
public enum EstadoOperacionProgramada {
    PENDIENTE,
    EJECUTADA,
    FALLIDA,
    CANCELADA
}
