package co.edu.uniquindio.fintech.model;

import java.time.LocalDateTime;

/**
 * Notificación dirigida a un usuario (enunciado 4.7).
 *
 * <p>Se crea PENDIENTE y pasa a DESPACHADA al atenderse desde la cola de despacho
 * ({@link #conEstado}).
 *
 * @param id                    identificador {@code NOT-NNNNNN}
 * @param identificacionUsuario identificación del usuario destinatario
 * @param tipo                  tipo de notificación
 * @param mensaje               texto para el usuario
 * @param fecha                 fecha y hora de creación
 * @param estado                estado de despacho
 */
public record Notificacion(String id, String identificacionUsuario, TipoNotificacion tipo, String mensaje,
                           LocalDateTime fecha, EstadoNotificacion estado) {

    public Notificacion {
        Validaciones.identificador(id, Identificadores.PATRON_NOTIFICACION, "id");
        Validaciones.texto(identificacionUsuario, "identificacionUsuario");
        Validaciones.requerido(tipo, "tipo");
        Validaciones.texto(mensaje, "mensaje");
        Validaciones.requerido(fecha, "fecha");
        Validaciones.requerido(estado, "estado");
    }

    /** @return copia con el estado indicado */
    public Notificacion conEstado(EstadoNotificacion nuevoEstado) {
        return new Notificacion(id, identificacionUsuario, tipo, mensaje, fecha, nuevoEstado);
    }
}
