package co.edu.uniquindio.fintech.dto;

import co.edu.uniquindio.fintech.model.EventoAuditoria;
import co.edu.uniquindio.fintech.model.NivelRiesgo;
import co.edu.uniquindio.fintech.model.PatronInusual;

import java.time.LocalDateTime;

/** Datos de un evento de auditoría (consulta posterior de eventos detectados). */
public record EventoAuditoriaRespuesta(String id, String identificacionUsuario, String transaccion,
                                       PatronInusual patron, String descripcion, NivelRiesgo nivelRiesgo,
                                       LocalDateTime fecha) {

    public static EventoAuditoriaRespuesta desde(EventoAuditoria e) {
        return new EventoAuditoriaRespuesta(e.id(), e.identificacionUsuario(), e.transaccion(), e.patron(),
                e.descripcion(), e.nivelRiesgo(), e.fecha());
    }
}
