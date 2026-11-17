package co.edu.uniquindio.fintech.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Modificación de los datos básicos de un usuario ({@code PUT /api/usuarios/{id}}).
 * La identificación viene en la ruta y no se puede cambiar.
 */
public record SolicitudActualizacionUsuario(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido") String correo) {
}
