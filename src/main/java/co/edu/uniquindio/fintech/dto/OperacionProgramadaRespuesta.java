package co.edu.uniquindio.fintech.dto;

import co.edu.uniquindio.fintech.model.EstadoOperacionProgramada;
import co.edu.uniquindio.fintech.model.OperacionProgramada;
import co.edu.uniquindio.fintech.model.Periodicidad;
import co.edu.uniquindio.fintech.model.TipoOperacionProgramada;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Datos de una operación programada. */
public record OperacionProgramadaRespuesta(String id, TipoOperacionProgramada tipo, BigDecimal valor,
                                           String billeteraOrigen, String billeteraDestino,
                                           LocalDateTime fechaEjecucion, Periodicidad periodicidad,
                                           EstadoOperacionProgramada estado) {

    public static OperacionProgramadaRespuesta desde(OperacionProgramada o) {
        return new OperacionProgramadaRespuesta(o.id(), o.tipo(), o.valor(), o.billeteraOrigen(),
                o.billeteraDestino(), o.fechaEjecucion(), o.periodicidad(), o.estado());
    }
}
