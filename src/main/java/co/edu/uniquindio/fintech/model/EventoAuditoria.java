package co.edu.uniquindio.fintech.model;

import java.time.LocalDateTime;

/**
 * Evento de comportamiento financiero inusual registrado en el historial de auditoría
 * (enunciado 4.9). Es un registro histórico, por eso no tiene métodos de cambio.
 *
 * @param id                    identificador {@code AUD-NNNNNN}
 * @param identificacionUsuario identificación del usuario involucrado
 * @param transaccion           identificador de la transacción relacionada, o null si no hay una
 * @param patron                patrón detectado
 * @param descripcion           detalle legible de lo detectado
 * @param nivelRiesgo           nivel de riesgo asignado
 * @param fecha                 fecha y hora de la detección
 */
public record EventoAuditoria(String id, String identificacionUsuario, String transaccion, PatronInusual patron,
                              String descripcion, NivelRiesgo nivelRiesgo, LocalDateTime fecha) {

    public EventoAuditoria {
        Validaciones.identificador(id, Identificadores.PATRON_EVENTO_AUDITORIA, "id");
        Validaciones.texto(identificacionUsuario, "identificacionUsuario");
        Validaciones.identificadorOpcional(transaccion, Identificadores.PATRON_TRANSACCION, "transaccion");
        Validaciones.requerido(patron, "patron");
        Validaciones.texto(descripcion, "descripcion");
        Validaciones.requerido(nivelRiesgo, "nivelRiesgo");
        Validaciones.requerido(fecha, "fecha");
    }
}
