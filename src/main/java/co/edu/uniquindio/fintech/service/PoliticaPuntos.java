package co.edu.uniquindio.fintech.service;

import java.math.BigDecimal;

/**
 * Política de puntos (enunciado 4.4): puntos por cada {@code unidad} del valor, según el
 * tipo de movimiento, más un bono si la operación programada se ejecuta con éxito.
 *
 * <p>Los factores 1/2/3 por cada 100 vienen del ejemplo del enunciado. <b>Provisional,
 * pendiente de confirmación:</b> el redondeo (división entera: 199,99 genera 1 punto en
 * una recarga) y el valor del bono (el enunciado dice "bono adicional" sin cifra).
 */
public record PoliticaPuntos(long puntosRecarga, long puntosRetiro, long puntosTransferencia, BigDecimal unidad,
                             long bonoProgramada) {

    public PoliticaPuntos {
        if (puntosRecarga < 0 || puntosRetiro < 0 || puntosTransferencia < 0 || bonoProgramada < 0) {
            throw new IllegalArgumentException("Los puntos de la política no pueden ser negativos");
        }
        if (unidad == null || unidad.signum() <= 0) {
            throw new IllegalArgumentException("La unidad de puntos debe ser positiva");
        }
    }

    /** Valores del ejemplo del enunciado y bono provisional de 10 puntos. */
    public static PoliticaPuntos provisional() {
        return new PoliticaPuntos(1, 2, 3, new BigDecimal("100"), 10);
    }
}
