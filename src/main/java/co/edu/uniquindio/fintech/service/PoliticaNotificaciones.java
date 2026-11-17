package co.edu.uniquindio.fintech.service;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Cuándo se generan alertas automáticas (enunciado 4.7). <b>Provisional:</b> el
 * enunciado no fija el umbral de "saldo bajo" ni la anticipación de "operación
 * programada próxima a ejecutarse".
 *
 * @param umbralSaldoBajo          saldo por debajo del cual se avisa tras un débito
 * @param anticipacionProgramadas  cuánto antes de su fecha se avisa una operación programada
 */
public record PoliticaNotificaciones(BigDecimal umbralSaldoBajo, Duration anticipacionProgramadas) {

    public PoliticaNotificaciones {
        if (umbralSaldoBajo == null || umbralSaldoBajo.signum() < 0 || anticipacionProgramadas == null
                || anticipacionProgramadas.isNegative()) {
            throw new IllegalArgumentException("Umbral y anticipación deben ser no negativos");
        }
    }

    /** Saldo bajo: menos de 10.000; aviso 24 horas antes. */
    public static PoliticaNotificaciones provisional() {
        return new PoliticaNotificaciones(new BigDecimal("10000"), Duration.ofHours(24));
    }
}
