package co.edu.uniquindio.fintech.dto;

import co.edu.uniquindio.fintech.model.NivelUsuario;

/** Puntos y nivel de un usuario; siguiente nivel y faltantes son null en PLATINO. */
public record PuntosRespuesta(String identificacion, long puntosAcumulados, NivelUsuario nivel,
                              NivelUsuario siguienteNivel, Long puntosParaSiguienteNivel) {
}
