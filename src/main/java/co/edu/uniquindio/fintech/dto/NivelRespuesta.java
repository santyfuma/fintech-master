package co.edu.uniquindio.fintech.dto;

import co.edu.uniquindio.fintech.model.NivelUsuario;

/**
 * Nivel con su rango de puntos (enunciado) y sus ventajas configuradas.
 *
 * @param puntosMaximos null para PLATINO (sin límite superior)
 */
public record NivelRespuesta(NivelUsuario nivel, long puntosMinimos, Long puntosMaximos, String[] ventajas) {

    public static NivelRespuesta desde(NivelUsuario nivel, String[] ventajas) {
        Long maximo = nivel.puntosMaximos() == Long.MAX_VALUE ? null : nivel.puntosMaximos();
        return new NivelRespuesta(nivel, nivel.puntosMinimos(), maximo, ventajas);
    }
}
