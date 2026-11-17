package co.edu.uniquindio.fintech.dto;

import co.edu.uniquindio.fintech.model.Billetera;

import java.math.BigDecimal;

/** Saldo actual de una billetera ({@code GET /api/billeteras/{codigo}/saldo}). */
public record SaldoRespuesta(String codigoBilletera, BigDecimal saldo) {

    public static SaldoRespuesta desde(Billetera billetera) {
        return new SaldoRespuesta(billetera.codigo(), billetera.saldo());
    }
}
