package co.edu.uniquindio.fintech.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Registro de un usuario ({@code POST /api/usuarios}). Puntos y nivel no se reciben:
 * los asigna el sistema.
 */
public record SolicitudRegistroUsuario(
        @NotBlank(message = "La identificación es obligatoria") String identificacion,
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido") String correo) {
}
