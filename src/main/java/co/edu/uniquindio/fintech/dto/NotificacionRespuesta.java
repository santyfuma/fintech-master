package co.edu.uniquindio.fintech.dto;

import co.edu.uniquindio.fintech.model.EstadoNotificacion;
import co.edu.uniquindio.fintech.model.Notificacion;
import co.edu.uniquindio.fintech.model.TipoNotificacion;

import java.time.LocalDateTime;

/** Datos de una notificación. */
public record NotificacionRespuesta(String id, String identificacionUsuario, TipoNotificacion tipo, String mensaje,
                                    LocalDateTime fecha, EstadoNotificacion estado) {

    public static NotificacionRespuesta desde(Notificacion n) {
        return new NotificacionRespuesta(n.id(), n.identificacionUsuario(), n.tipo(), n.mensaje(), n.fecha(),
                n.estado());
    }
}
