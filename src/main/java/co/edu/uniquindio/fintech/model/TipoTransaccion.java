package co.edu.uniquindio.fintech.model;

/**
 * Tipo de una transacción registrada.
 *
 * <p>REVERSION identifica de forma explícita a las transacciones que revierten a otra;
 * solo ellas llevan la referencia {@code transaccionRevertida}.
 */
public enum TipoTransaccion {
    RECARGA,
    RETIRO,
    TRANSFERENCIA,
    REVERSION;

    /**
     * Forma de las billeteras de cada tipo de movimiento (dato estructural, no regla
     * financiera): una recarga solo tiene destino, un retiro solo origen, una
     * transferencia ambas y distintas, y una reversión al menos una.
     *
     * @return {@code true} si la combinación de billeteras corresponde a este tipo
     */
    public boolean billeterasCoherentes(String billeteraOrigen, String billeteraDestino) {
        return switch (this) {
            case RECARGA -> billeteraOrigen == null && billeteraDestino != null;
            case RETIRO -> billeteraOrigen != null && billeteraDestino == null;
            case TRANSFERENCIA -> billeteraOrigen != null && billeteraDestino != null
                    && !billeteraOrigen.equals(billeteraDestino);
            case REVERSION -> billeteraOrigen != null || billeteraDestino != null;
        };
    }
}
