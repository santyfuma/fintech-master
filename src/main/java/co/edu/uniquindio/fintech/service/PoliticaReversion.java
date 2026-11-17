package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.model.TipoTransaccion;
import co.edu.uniquindio.fintech.model.Transaccion;
import co.edu.uniquindio.fintech.repository.CriterioReversion;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Qué transacciones pueden revertirse (implementa {@link CriterioReversion}, el contrato
 * de la fase 5): tipos permitidos y plazo desde la fecha de la operación.
 *
 * <p>El enunciado (4.6) da como ejemplos una transferencia por error, una recarga mal
 * aplicada y una operación duplicada. <b>Provisional, pendiente de confirmación:</b> los
 * tipos por defecto son RECARGA y TRANSFERENCIA (los dos nombrados) y el plazo, 24 horas.
 */
public final class PoliticaReversion implements CriterioReversion {

    private final boolean recargas;
    private final boolean retiros;
    private final boolean transferencias;
    private final Duration plazo;
    private final Clock reloj;

    public PoliticaReversion(boolean recargas, boolean retiros, boolean transferencias, Duration plazo, Clock reloj) {
        if (plazo == null || plazo.isNegative() || reloj == null) {
            throw new IllegalArgumentException("Plazo y reloj son obligatorios y el plazo no puede ser negativo");
        }
        this.recargas = recargas;
        this.retiros = retiros;
        this.transferencias = transferencias;
        this.plazo = plazo;
        this.reloj = reloj;
    }

    /** RECARGA y TRANSFERENCIA, 24 horas. */
    public static PoliticaReversion provisional(Clock reloj) {
        return new PoliticaReversion(true, false, true, Duration.ofHours(24), reloj);
    }

    /** @return si el tipo de la transacción es reversible según la política */
    public boolean tipoPermitido(TipoTransaccion tipo) {
        return switch (tipo) {
            case RECARGA -> recargas;
            case RETIRO -> retiros;
            case TRANSFERENCIA -> transferencias;
            case REVERSION -> false;
        };
    }

    /** @return si la transacción sigue dentro del plazo de reversión */
    public boolean dentroDelPlazo(Transaccion transaccion) {
        return !transaccion.fecha().isBefore(LocalDateTime.now(reloj).minus(plazo));
    }

    @Override
    public boolean esReversible(Transaccion transaccion) {
        return tipoPermitido(transaccion.tipo()) && dentroDelPlazo(transaccion);
    }

    public Duration plazo() {
        return plazo;
    }
}
