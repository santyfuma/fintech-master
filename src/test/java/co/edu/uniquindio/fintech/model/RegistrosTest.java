package co.edu.uniquindio.fintech.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Notificación, evento de auditoría y beneficio canjeado. */
class RegistrosTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 15, 18, 0);

    @Test
    void notificacionPendienteYDespachada() {
        Notificacion pendiente = new Notificacion("NOT-000120", "1094000111", TipoNotificacion.SALDO_BAJO,
                "Tu billetera BIL-000001 tiene saldo bajo", FECHA, EstadoNotificacion.PENDIENTE);
        assertEquals("NOT-000120", pendiente.id());
        assertEquals("1094000111", pendiente.identificacionUsuario());
        assertEquals(TipoNotificacion.SALDO_BAJO, pendiente.tipo());
        assertEquals(FECHA, pendiente.fecha());
        Notificacion despachada = pendiente.conEstado(EstadoNotificacion.DESPACHADA);
        assertEquals(EstadoNotificacion.DESPACHADA, despachada.estado());
        assertEquals(EstadoNotificacion.PENDIENTE, pendiente.estado());
        assertEquals(pendiente.mensaje(), despachada.mensaje());
    }

    @Test
    void notificacionValidaSusCampos() {
        assertThrows(IllegalArgumentException.class, () -> new Notificacion("AUD-000001", "1",
                TipoNotificacion.SALDO_BAJO, "m", FECHA, EstadoNotificacion.PENDIENTE));
        assertThrows(IllegalArgumentException.class, () -> new Notificacion("NOT-000001", "1",
                TipoNotificacion.SALDO_BAJO, " ", FECHA, EstadoNotificacion.PENDIENTE));
        assertThrows(NullPointerException.class, () -> new Notificacion("NOT-000001", "1",
                TipoNotificacion.SALDO_BAJO, "m", FECHA, null));
    }

    @Test
    void eventoDeAuditoriaConYSinTransaccion() {
        EventoAuditoria conTransaccion = new EventoAuditoria("AUD-000007", "1094000111", "TRX-000042",
                PatronInusual.MONTO_INUSUALMENTE_ALTO, "Retiro 10 veces mayor que el promedio",
                NivelRiesgo.ALTO, FECHA);
        assertEquals("TRX-000042", conTransaccion.transaccion());
        assertEquals(PatronInusual.MONTO_INUSUALMENTE_ALTO, conTransaccion.patron());
        assertEquals(NivelRiesgo.ALTO, conTransaccion.nivelRiesgo());

        EventoAuditoria sinTransaccion = new EventoAuditoria("AUD-000008", "1094000111", null,
                PatronInusual.CAMBIO_BRUSCO_DE_FRECUENCIA, "Frecuencia triplicada", NivelRiesgo.MEDIO, FECHA);
        assertNull(sinTransaccion.transaccion());
    }

    @Test
    void eventoDeAuditoriaValidaSusCampos() {
        assertThrows(IllegalArgumentException.class, () -> new EventoAuditoria("AUD-000001", "1",
                "BIL-000001", PatronInusual.DESTINO_REPETITIVO, "d", NivelRiesgo.BAJO, FECHA));
        assertThrows(NullPointerException.class, () -> new EventoAuditoria("AUD-000001", "1",
                null, null, "d", NivelRiesgo.BAJO, FECHA));
        assertThrows(IllegalArgumentException.class, () -> new EventoAuditoria("AUD-000001", "1",
                null, PatronInusual.DESTINO_REPETITIVO, "", NivelRiesgo.BAJO, FECHA));
    }

    @Test
    void beneficioCanjeado() {
        BeneficioCanjeado canje = new BeneficioCanjeado("CAN-000015", "1094000111", "Descuento en comisión",
                300, FECHA, EstadoCanje.REALIZADO);
        assertEquals("CAN-000015", canje.id());
        assertEquals("Descuento en comisión", canje.beneficio());
        assertEquals(300, canje.puntosDescontados());
        assertEquals(EstadoCanje.ANULADO, canje.conEstado(EstadoCanje.ANULADO).estado());
        assertEquals(EstadoCanje.REALIZADO, canje.estado());
        assertThrows(IllegalArgumentException.class, () -> new BeneficioCanjeado("CAN-1", "1", "b", 1,
                FECHA, EstadoCanje.REALIZADO));
        assertThrows(IllegalArgumentException.class, () -> new BeneficioCanjeado("CAN-000001", "1", " ", 1,
                FECHA, EstadoCanje.REALIZADO));
    }
}
