package co.edu.uniquindio.fintech.controller;

import co.edu.uniquindio.fintech.dto.EventoAuditoriaRespuesta;
import co.edu.uniquindio.fintech.dto.NotificacionRespuesta;
import co.edu.uniquindio.fintech.model.NivelRiesgo;
import co.edu.uniquindio.fintech.service.DeteccionService;
import co.edu.uniquindio.fintech.service.NotificacionService;
import co.edu.uniquindio.fintech.service.UsuarioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Notificaciones (API.md §7) y auditoría de actividad inusual (API.md §8). */
@RestController
public class NotificacionAuditoriaController {

    private final NotificacionService notificaciones;
    private final DeteccionService deteccion;
    private final UsuarioService usuarios;

    public NotificacionAuditoriaController(NotificacionService notificaciones, DeteccionService deteccion,
                                           UsuarioService usuarios) {
        this.notificaciones = notificaciones;
        this.deteccion = deteccion;
        this.usuarios = usuarios;
    }

    /** Historial reciente (la más nueva primero, hasta el límite configurado). */
    @GetMapping("/api/usuarios/{id}/notificaciones")
    public NotificacionRespuesta[] deUsuario(@PathVariable("id") String id) {
        usuarios.buscar(id);
        return Arreglos.de(notificaciones.recientes(id), NotificacionRespuesta::desde, NotificacionRespuesta[]::new);
    }

    @GetMapping("/api/notificaciones/pendientes")
    public NotificacionRespuesta[] pendientes() {
        return Arreglos.de(notificaciones.pendientes(), NotificacionRespuesta::desde, NotificacionRespuesta[]::new);
    }

    /** Despacha (simula el envío de) las pendientes en orden FIFO. */
    @PostMapping("/api/notificaciones/despachar")
    public NotificacionRespuesta[] despachar() {
        return Arreglos.de(notificaciones.despacharPendientes(), NotificacionRespuesta::desde,
                NotificacionRespuesta[]::new);
    }

    @GetMapping("/api/auditoria")
    public EventoAuditoriaRespuesta[] auditoria(@RequestParam(name = "riesgo", required = false) NivelRiesgo riesgo) {
        return Arreglos.de(deteccion.eventos(riesgo), EventoAuditoriaRespuesta::desde, EventoAuditoriaRespuesta[]::new);
    }

    @GetMapping("/api/usuarios/{id}/auditoria")
    public EventoAuditoriaRespuesta[] auditoriaDe(@PathVariable("id") String id,
                                                  @RequestParam(name = "riesgo", required = false) NivelRiesgo riesgo) {
        usuarios.buscar(id);
        return Arreglos.de(deteccion.eventosDe(id, riesgo), EventoAuditoriaRespuesta::desde,
                EventoAuditoriaRespuesta[]::new);
    }
}
