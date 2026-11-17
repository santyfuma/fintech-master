package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.exception.ConflictoNegocioException;
import co.edu.uniquindio.fintech.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.fintech.model.EstadoNotificacion;
import co.edu.uniquindio.fintech.model.Notificacion;
import co.edu.uniquindio.fintech.model.TipoNotificacion;
import co.edu.uniquindio.fintech.soporte.SistemaPrueba;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;

import static co.edu.uniquindio.fintech.soporte.Afirmaciones.lanza;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificacionServiceTest {

    @TempDir
    Path directorio;

    private SistemaPrueba s;

    @BeforeEach
    void iniciar() {
        s = new SistemaPrueba(directorio);
        s.usuario("100");
        s.usuario("200");
    }

    @Test
    void despacharPendientesLasAtiendeEnOrdenFifo() {
        Notificacion n1 = s.notificaciones.crear("100", TipoNotificacion.SALDO_BAJO, "uno");
        s.reloj.avanzar(Duration.ofMinutes(1));
        Notificacion n2 = s.notificaciones.crear("200", TipoNotificacion.SALDO_BAJO, "dos");
        Notificacion n3 = s.notificaciones.crear("100", TipoNotificacion.ASCENSO_NIVEL, "tres");
        assertEquals(3, s.notificaciones.pendientes().tamanio());
        Lista<Notificacion> despachadas = s.notificaciones.despacharPendientes();
        assertEquals(n1.id(), despachadas.obtener(0).id());
        assertEquals(n2.id(), despachadas.obtener(1).id());
        assertEquals(n3.id(), despachadas.obtener(2).id());
        assertTrue(s.notificaciones.pendientes().estaVacia());
        assertTrue(s.notificaciones.despacharPendientes().estaVacia());
        assertEquals(EstadoNotificacion.DESPACHADA, s.reiniciar().notificaciones.buscar(n2.id()).estado());
    }

    @Test
    void despacharUnaYNoDosVeces() {
        Notificacion n = s.notificaciones.crear("100", TipoNotificacion.SALDO_BAJO, "x");
        assertEquals(EstadoNotificacion.DESPACHADA, s.notificaciones.despachar(n.id()).estado());
        lanza(ConflictoNegocioException.class, "NOTIFICACION_YA_DESPACHADA", () -> s.notificaciones.despachar(n.id()));
        lanza(RecursoNoEncontradoException.class, "NOTIFICACION_NO_ENCONTRADA",
                () -> s.notificaciones.despachar("NOT-000999"));
    }

    @Test
    void lasOperacionesGeneranNotificacionesPendientesDelUsuarioCorrecto() {
        String a = s.billetera("100", "20000");
        String b = s.billetera("200", "0");
        s.transacciones.transferir(a, b, new BigDecimal("15000")); // saldo bajo en la billetera de 100
        assertEquals(1, s.notificaciones("100", TipoNotificacion.SALDO_BAJO));
        assertEquals(0, s.notificaciones("200", TipoNotificacion.SALDO_BAJO));
        for (Notificacion n : s.notificaciones.porUsuario("100")) {
            assertEquals(EstadoNotificacion.PENDIENTE, n.estado());
        }
    }

    @Test
    void recientesDevuelveLaMasNuevaPrimero() {
        s.notificaciones.crear("100", TipoNotificacion.SALDO_BAJO, "vieja");
        s.reloj.avanzar(Duration.ofMinutes(1));
        Notificacion nueva = s.notificaciones.crear("100", TipoNotificacion.SALDO_BAJO, "nueva");
        assertEquals(nueva.id(), s.notificaciones.recientes("100").obtener(0).id());
    }
}
