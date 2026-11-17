package co.edu.uniquindio.fintech.dto;

import co.edu.uniquindio.fintech.model.Identificadores;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/** Retiro de saldo ({@code POST /api/transacciones/retiros}). */
public record SolicitudRetiro(
        @NotBlank(message = "La billetera origen es obligatoria")
        @Pattern(regexp = Identificadores.PATRON_BILLETERA,
                message = "La billetera origen debe tener el formato BIL-NNNNNN") String billeteraOrigen,
        @NotNull(message = "El valor es obligatorio")
        @DecimalMin(value = "0", inclusive = false, message = "El valor debe ser mayor que cero") BigDecimal valor) {
}
