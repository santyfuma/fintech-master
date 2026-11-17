package co.edu.uniquindio.fintech.dto;

import co.edu.uniquindio.fintech.model.TipoBilletera;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Creación de una billetera ({@code POST /api/usuarios/{id}/billeteras}). El propietario
 * viene en la ruta; código, saldo inicial y estado los asigna el sistema.
 */
public record SolicitudCreacionBilletera(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotNull(message = "El tipo de billetera es obligatorio") TipoBilletera tipo) {
}
