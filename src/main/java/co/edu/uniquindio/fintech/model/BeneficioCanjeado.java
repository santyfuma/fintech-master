package co.edu.uniquindio.fintech.model;

import java.time.LocalDateTime;

/**
 * Beneficio canjeado por un usuario con sus puntos (enunciado 4.4 y 5.1).
 *
 * <p>{@code beneficio} es texto libre que identifica o describe el beneficio: el
 * catálogo de beneficios y sus costos siguen pendientes (CONTEXTO.md, sección 11).
 *
 * @param id                    identificador {@code CAN-NNNNNN}
 * @param identificacionUsuario identificación del usuario
 * @param beneficio             identificación o descripción del beneficio
 * @param puntosDescontados     puntos descontados por el canje
 * @param fecha                 fecha y hora del canje
 * @param estado                estado del canje
 */
public record BeneficioCanjeado(String id, String identificacionUsuario, String beneficio, long puntosDescontados,
                                LocalDateTime fecha, EstadoCanje estado) {

    public BeneficioCanjeado {
        Validaciones.identificador(id, Identificadores.PATRON_BENEFICIO_CANJEADO, "id");
        Validaciones.texto(identificacionUsuario, "identificacionUsuario");
        Validaciones.texto(beneficio, "beneficio");
        Validaciones.requerido(fecha, "fecha");
        Validaciones.requerido(estado, "estado");
    }

    /** @return copia con el estado indicado */
    public BeneficioCanjeado conEstado(EstadoCanje nuevoEstado) {
        return new BeneficioCanjeado(id, identificacionUsuario, beneficio, puntosDescontados, fecha, nuevoEstado);
    }
}
