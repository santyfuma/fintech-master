package co.edu.uniquindio.fintech.dto;

import jakarta.validation.constraints.NotBlank;

/** Canje de puntos ({@code POST /api/usuarios/{id}/canjes}). */
public record SolicitudCanje(@NotBlank(message = "El código del beneficio es obligatorio") String beneficio) {
}
