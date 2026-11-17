package co.edu.uniquindio.fintech.repository;

import co.edu.uniquindio.fintech.model.Transaccion;

import java.math.BigDecimal;

/**
 * Decide el peso que aporta una transferencia a la arista
 * {@code usuarioOrigen -> usuarioDestino} del grafo de transferencias. <b>Pendiente</b>
 * (API.md, sección 10): si el peso es monto acumulado, número de transferencias o ambos,
 * qué estados cuentan y si se incluyen las transferencias entre billeteras del mismo
 * usuario lo define la fase de analítica.
 */
@FunctionalInterface
public interface CriterioPesoTransferencia {

    /**
     * @return peso no negativo que se suma a la arista, o {@code null} para no incluir
     *         esta transferencia en el grafo
     */
    BigDecimal peso(Transaccion transferencia, String usuarioOrigen, String usuarioDestino);
}
