package co.edu.uniquindio.fintech.dto;

import co.edu.uniquindio.fintech.model.EstadoBilletera;
import co.edu.uniquindio.fintech.model.TipoBilletera;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Modificación de una billetera ({@code PUT /api/billeteras/{codigo}}): nombre, tipo o
 * estado, según docs/API.md. El saldo nunca se modifica por esta vía.
 */
public record SolicitudActualizacionBilletera(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotNull(message = "El tipo de billetera es obligatorio") TipoBilletera tipo,
        @NotNull(message = "El estado es obligatorio") EstadoBilletera estado) {
}
