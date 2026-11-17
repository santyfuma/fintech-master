package co.edu.uniquindio.fintech.model;

/**
 * Tipo de una operación programada (enunciado 4.3).
 *
 * <p>Es distinto de {@link TipoTransaccion} porque una reversión no se puede programar.
 * Los ejemplos del enunciado encajan en estos tipos: recargas automáticas (RECARGA),
 * transferencias periódicas y ahorro automático (TRANSFERENCIA) y pagos recurrentes
 * (TRANSFERENCIA o RETIRO, según el destino del pago).
 *
 * <p><b>Provisional, pendiente de confirmación:</b> que RETIRO sea programable.
 */
public enum TipoOperacionProgramada {
    RECARGA(TipoTransaccion.RECARGA),
    RETIRO(TipoTransaccion.RETIRO),
    TRANSFERENCIA(TipoTransaccion.TRANSFERENCIA);

    private final TipoTransaccion tipoTransaccion;

    TipoOperacionProgramada(TipoTransaccion tipoTransaccion) {
        this.tipoTransaccion = tipoTransaccion;
    }

    /** @return el tipo de la transacción que se registra al ejecutar la operación */
    public TipoTransaccion tipoTransaccion() {
        return tipoTransaccion;
    }
}
