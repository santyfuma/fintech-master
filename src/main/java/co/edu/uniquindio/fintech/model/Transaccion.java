package co.edu.uniquindio.fintech.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transacción registrada (enunciado 4.2), exitosa, rechazada o revertida.
 *
 * <p>Billeteras según el tipo (se verifica al construir):
 * <ul>
 *   <li>RECARGA: solo {@code billeteraDestino}.</li>
 *   <li>RETIRO: solo {@code billeteraOrigen}.</li>
 *   <li>TRANSFERENCIA: ambas, distintas entre sí.</li>
 *   <li>REVERSION: las billeteras del movimiento que deshace (al menos una).</li>
 * </ul>
 *
 * <p>Originales y reversiones se distinguen sin ambigüedad: {@code transaccionRevertida}
 * es obligatoria si el tipo es REVERSION y debe ser {@code null} en cualquier otro
 * tipo. La transacción original revertida queda con estado REVERTIDA. Calcular los
 * puntos, aplicar saldos y decidir qué se puede revertir corresponde a {@code service}.
 *
 * <p>Inmutable; el único cambio previsto después de registrarla es el estado
 * ({@link #conEstado}).
 *
 * @param id                   identificador {@code TRX-NNNNNN}
 * @param fecha                fecha y hora de la operación
 * @param tipo                 tipo de transacción
 * @param valor                valor de la operación
 * @param billeteraOrigen      código de la billetera origen, o null si no aplica
 * @param billeteraDestino     código de la billetera destino, o null si no aplica
 * @param estado               estado de la transacción
 * @param puntosGenerados      puntos generados (su cálculo y signo los decide service)
 * @param nivelRiesgo          nivel de riesgo asignado
 * @param transaccionRevertida en una REVERSION, el identificador de la transacción original
 */
public record Transaccion(String id, LocalDateTime fecha, TipoTransaccion tipo, BigDecimal valor,
                          String billeteraOrigen, String billeteraDestino, EstadoTransaccion estado,
                          long puntosGenerados, NivelRiesgo nivelRiesgo, String transaccionRevertida) {

    public Transaccion {
        Validaciones.identificador(id, Identificadores.PATRON_TRANSACCION, "id");
        Validaciones.requerido(fecha, "fecha");
        Validaciones.requerido(tipo, "tipo");
        Validaciones.requerido(valor, "valor");
        Validaciones.requerido(estado, "estado");
        Validaciones.requerido(nivelRiesgo, "nivelRiesgo");
        Validaciones.identificadorOpcional(billeteraOrigen, Identificadores.PATRON_BILLETERA, "billeteraOrigen");
        Validaciones.identificadorOpcional(billeteraDestino, Identificadores.PATRON_BILLETERA, "billeteraDestino");
        Validaciones.billeterasSegunTipo(tipo, billeteraOrigen, billeteraDestino);
        if (tipo == TipoTransaccion.REVERSION) {
            Validaciones.identificador(transaccionRevertida, Identificadores.PATRON_TRANSACCION, "transaccionRevertida");
            if (transaccionRevertida.equals(id)) {
                throw new IllegalArgumentException("Una reversión no puede referenciarse a sí misma: " + id);
            }
        } else if (transaccionRevertida != null) {
            throw new IllegalArgumentException(
                    "Solo una transacción de tipo REVERSION puede tener transaccionRevertida");
        }
    }

    /** @return {@code true} si esta transacción es la reversión de otra */
    public boolean esReversion() {
        return tipo == TipoTransaccion.REVERSION;
    }

    /** @return copia con el estado indicado */
    public Transaccion conEstado(EstadoTransaccion nuevoEstado) {
        return new Transaccion(id, fecha, tipo, valor, billeteraOrigen, billeteraDestino, nuevoEstado,
                puntosGenerados, nivelRiesgo, transaccionRevertida);
    }
}
