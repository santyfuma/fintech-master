package co.edu.uniquindio.fintech.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Operación programada para ejecutarse en una fecha futura (enunciado 4.3).
 *
 * <p>Las billeteras siguen la misma regla de forma que las transacciones: una recarga
 * solo tiene destino, un retiro solo origen y una transferencia ambas, distintas. El
 * propietario se deduce de las billeteras.
 *
 * <p>Inmutable: ejecutar, fallar, cancelar o reprogramar produce copias con
 * {@link #conEstado} y {@link #conFechaEjecucion}. La planificación, la ejecución y la
 * forma de reprogramar las operaciones periódicas corresponden a {@code service}.
 *
 * @param id               identificador {@code OPR-NNNNNN}
 * @param tipo             tipo de operación
 * @param valor            valor de la operación
 * @param billeteraOrigen  código de la billetera origen, o null si no aplica
 * @param billeteraDestino código de la billetera destino, o null si no aplica
 * @param fechaEjecucion   fecha y hora en que debe ejecutarse
 * @param periodicidad     periodicidad (UNICA si no se repite)
 * @param estado           estado de la operación
 */
public record OperacionProgramada(String id, TipoOperacionProgramada tipo, BigDecimal valor,
                                  String billeteraOrigen, String billeteraDestino,
                                  LocalDateTime fechaEjecucion, Periodicidad periodicidad,
                                  EstadoOperacionProgramada estado) {

    public OperacionProgramada {
        Validaciones.identificador(id, Identificadores.PATRON_OPERACION_PROGRAMADA, "id");
        Validaciones.requerido(tipo, "tipo");
        Validaciones.requerido(valor, "valor");
        Validaciones.identificadorOpcional(billeteraOrigen, Identificadores.PATRON_BILLETERA, "billeteraOrigen");
        Validaciones.identificadorOpcional(billeteraDestino, Identificadores.PATRON_BILLETERA, "billeteraDestino");
        Validaciones.billeterasSegunTipo(tipo.tipoTransaccion(), billeteraOrigen, billeteraDestino);
        Validaciones.requerido(fechaEjecucion, "fechaEjecucion");
        Validaciones.requerido(periodicidad, "periodicidad");
        Validaciones.requerido(estado, "estado");
    }

    /** @return copia con el estado indicado */
    public OperacionProgramada conEstado(EstadoOperacionProgramada nuevoEstado) {
        return new OperacionProgramada(id, tipo, valor, billeteraOrigen, billeteraDestino, fechaEjecucion,
                periodicidad, nuevoEstado);
    }

    /** @return copia con una nueva fecha de ejecución */
    public OperacionProgramada conFechaEjecucion(LocalDateTime nuevaFecha) {
        return new OperacionProgramada(id, tipo, valor, billeteraOrigen, billeteraDestino, nuevaFecha,
                periodicidad, estado);
    }
}
