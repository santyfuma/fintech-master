package co.edu.uniquindio.fintech.dto;

import co.edu.uniquindio.fintech.model.Identificadores;
import co.edu.uniquindio.fintech.model.Periodicidad;
import co.edu.uniquindio.fintech.model.TipoOperacionProgramada;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Programación de una operación futura ({@code POST /api/operaciones-programadas}).
 *
 * <p>Las billeteras dependen del tipo: una recarga solo lleva destino, un retiro solo
 * origen y una transferencia ambas. Que la fecha sea futura no se valida aquí con
 * {@code @Future}, porque esa anotación usa el reloj del sistema y no el {@code Clock}
 * de la aplicación (necesario para simular el paso del tiempo); lo valida {@code service}.
 */
public record SolicitudOperacionProgramada(
        @NotNull(message = "El tipo de operación es obligatorio") TipoOperacionProgramada tipo,
        @NotNull(message = "El valor es obligatorio")
        @DecimalMin(value = "0", inclusive = false, message = "El valor debe ser mayor que cero") BigDecimal valor,
        @Pattern(regexp = Identificadores.PATRON_BILLETERA,
                message = "La billetera origen debe tener el formato BIL-NNNNNN") String billeteraOrigen,
        @Pattern(regexp = Identificadores.PATRON_BILLETERA,
                message = "La billetera destino debe tener el formato BIL-NNNNNN") String billeteraDestino,
        @NotNull(message = "La fecha de ejecución es obligatoria") LocalDateTime fechaEjecucion,
        @NotNull(message = "La periodicidad es obligatoria") Periodicidad periodicidad) {

    /** Validación de forma de las billeteras según el tipo (si falta el tipo, lo informa {@code @NotNull}). */
    @AssertTrue(message = "Las billeteras no corresponden al tipo de operación: una recarga solo lleva destino, "
            + "un retiro solo origen y una transferencia ambas, distintas")
    public boolean isBilleterasCoherentesConElTipo() {
        return tipo == null || tipo.tipoTransaccion().billeterasCoherentes(billeteraOrigen, billeteraDestino);
    }
}
