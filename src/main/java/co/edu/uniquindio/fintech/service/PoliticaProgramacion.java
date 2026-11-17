package co.edu.uniquindio.fintech.service;

/**
 * Reglas de las operaciones programadas que el enunciado no fija (4.3).
 *
 * <p><b>Provisional:</b> se permiten retiros programados (pagos recurrentes externos).
 * Una operación periódica ejecutada (o fallida) queda con ese estado y se <b>genera una
 * nueva operación</b> PENDIENTE para la siguiente fecha; así cada ejecución deja su
 * propio registro con su resultado.
 */
public record PoliticaProgramacion(boolean permitirRetiros) {

    public static PoliticaProgramacion provisional() {
        return new PoliticaProgramacion(true);
    }
}
