package co.edu.uniquindio.fintech.dto;

import co.edu.uniquindio.fintech.model.Identificadores;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * Transferencia entre billeteras del mismo usuario o de usuarios distintos
 * ({@code POST /api/transacciones/transferencias}). Si el destino es de otro usuario
 * lo determina {@code service} a partir de los códigos.
 */
public record SolicitudTransferencia(
        @NotBlank(message = "La billetera origen es obligatoria")
        @Pattern(regexp = Identificadores.PATRON_BILLETERA,
                message = "La billetera origen debe tener el formato BIL-NNNNNN") String billeteraOrigen,
        @NotBlank(message = "La billetera destino es obligatoria")
        @Pattern(regexp = Identificadores.PATRON_BILLETERA,
                message = "La billetera destino debe tener el formato BIL-NNNNNN") String billeteraDestino,
        @NotNull(message = "El valor es obligatorio")
        @DecimalMin(value = "0", inclusive = false, message = "El valor debe ser mayor que cero") BigDecimal valor) {

    /**
     * Validación de forma: origen y destino deben ser distintos. Si falta alguno, lo
     * informa {@code @NotBlank} y esta comprobación no añade otro error.
     */
    @AssertTrue(message = "La billetera origen y la destino deben ser distintas")
    public boolean isBilleterasDistintas() {
        return billeteraOrigen == null || billeteraDestino == null || !billeteraOrigen.equals(billeteraDestino);
    }
}
