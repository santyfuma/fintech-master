package co.edu.uniquindio.fintech.dto;

import co.edu.uniquindio.fintech.model.EstadoTransaccion;
import co.edu.uniquindio.fintech.model.NivelRiesgo;
import co.edu.uniquindio.fintech.model.TipoTransaccion;
import co.edu.uniquindio.fintech.model.Transaccion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Datos de una transacción (consultas e historial). */
public record TransaccionRespuesta(String id, LocalDateTime fecha, TipoTransaccion tipo, BigDecimal valor,
                                   String billeteraOrigen, String billeteraDestino, EstadoTransaccion estado,
                                   long puntosGenerados, NivelRiesgo nivelRiesgo, String transaccionRevertida) {

    public static TransaccionRespuesta desde(Transaccion t) {
        return new TransaccionRespuesta(t.id(), t.fecha(), t.tipo(), t.valor(), t.billeteraOrigen(),
                t.billeteraDestino(), t.estado(), t.puntosGenerados(), t.nivelRiesgo(), t.transaccionRevertida());
    }
}
