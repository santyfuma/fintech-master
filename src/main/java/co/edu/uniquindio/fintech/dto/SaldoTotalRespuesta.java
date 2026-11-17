package co.edu.uniquindio.fintech.dto;

import java.math.BigDecimal;

/** Saldo total de un usuario, calculado en el servidor. */
public record SaldoTotalRespuesta(String identificacion, BigDecimal saldoTotal, int billeteras) {
}
