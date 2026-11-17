package co.edu.uniquindio.fintech.dto;

import co.edu.uniquindio.fintech.model.Identificadores;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/** Recarga de saldo ({@code POST /api/transacciones/recargas}). */
public record SolicitudRecarga(
        @NotBlank(message = "La billetera destino es obligatoria")
        @Pattern(regexp = Identificadores.PATRON_BILLETERA,
                message = "La billetera destino debe tener el formato BIL-NNNNNN") String billeteraDestino,
        @NotNull(message = "El valor es obligatorio")
        @DecimalMin(value = "0", inclusive = false, message = "El valor debe ser mayor que cero") BigDecimal valor) {
}
