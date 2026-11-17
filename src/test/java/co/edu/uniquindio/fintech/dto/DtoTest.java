package co.edu.uniquindio.fintech.dto;

import co.edu.uniquindio.fintech.model.Billetera;
import co.edu.uniquindio.fintech.model.EstadoBilletera;
import co.edu.uniquindio.fintech.model.EstadoNotificacion;
import co.edu.uniquindio.fintech.model.EstadoOperacionProgramada;
import co.edu.uniquindio.fintech.model.EstadoTransaccion;
import co.edu.uniquindio.fintech.model.EventoAuditoria;
import co.edu.uniquindio.fintech.model.NivelRiesgo;
import co.edu.uniquindio.fintech.model.NivelUsuario;
import co.edu.uniquindio.fintech.model.Notificacion;
import co.edu.uniquindio.fintech.model.OperacionProgramada;
import co.edu.uniquindio.fintech.model.PatronInusual;
import co.edu.uniquindio.fintech.model.Periodicidad;
import co.edu.uniquindio.fintech.model.TipoBilletera;
import co.edu.uniquindio.fintech.model.TipoNotificacion;
import co.edu.uniquindio.fintech.model.TipoOperacionProgramada;
import co.edu.uniquindio.fintech.model.TipoTransaccion;
import co.edu.uniquindio.fintech.model.Transaccion;
import co.edu.uniquindio.fintech.model.Usuario;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Forma de los DTO y conversión desde las entidades (sin dependencias de Spring). */
class DtoTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 15, 18, 0);

    private static String componentes(Class<?> tipo) {
        StringBuilder sb = new StringBuilder("[");
        RecordComponent[] componentes = tipo.getRecordComponents();
        for (int i = 0; i < componentes.length; i++) {
            sb.append(i == 0 ? "" : ", ").append(componentes[i].getName());
        }
        return sb.append(']').toString();
    }

    @Test
    void todosLosDtoSonRecords() {
        Class<?>[] dtos = {SolicitudRegistroUsuario.class, SolicitudActualizacionUsuario.class,
                SolicitudCreacionBilletera.class, SolicitudActualizacionBilletera.class, SolicitudRecarga.class,
                SolicitudRetiro.class, SolicitudTransferencia.class, SolicitudOperacionProgramada.class,
                UsuarioRespuesta.class, BilleteraRespuesta.class, SaldoRespuesta.class, TransaccionRespuesta.class,
                OperacionProgramadaRespuesta.class, NotificacionRespuesta.class, EventoAuditoriaRespuesta.class,
                RespuestaError.class};
        for (Class<?> dto : dtos) {
            assertTrue(dto.isRecord(), dto.getSimpleName());
        }
    }

    @Test
    void lasSolicitudesNoExponenCamposInternos() {
        // Ni saldos, puntos, niveles, estados calculados ni identificadores generados.
        assertEquals("[identificacion, nombre, correo]", componentes(SolicitudRegistroUsuario.class));
        assertEquals("[nombre, correo]", componentes(SolicitudActualizacionUsuario.class));
        assertEquals("[nombre, tipo]", componentes(SolicitudCreacionBilletera.class));
        assertEquals("[nombre, tipo, estado]", componentes(SolicitudActualizacionBilletera.class));
        assertEquals("[billeteraDestino, valor]", componentes(SolicitudRecarga.class));
        assertEquals("[billeteraOrigen, valor]", componentes(SolicitudRetiro.class));
        assertEquals("[billeteraOrigen, billeteraDestino, valor]", componentes(SolicitudTransferencia.class));
        assertEquals("[tipo, valor, billeteraOrigen, billeteraDestino, fechaEjecucion, periodicidad]",
                componentes(SolicitudOperacionProgramada.class));
    }

    @Test
    void elFormatoDeErrorEsElDeLaApi() {
        assertEquals("[estado, error, mensaje, fecha]", componentes(RespuestaError.class));
        RespuestaError error = new RespuestaError(409, "SALDO_INSUFICIENTE",
                "Saldo insuficiente para realizar la operación", FECHA);
        assertEquals(409, error.estado());
        assertEquals("SALDO_INSUFICIENTE", error.error());
        assertEquals(FECHA, error.fecha());
    }

    @Test
    void validacionesDeFormaDeLasSolicitudes() {
        assertTrue(new SolicitudTransferencia("BIL-000001", "BIL-000002", BigDecimal.ONE).isBilleterasDistintas());
        assertTrue(!new SolicitudTransferencia("BIL-000001", "BIL-000001", BigDecimal.ONE).isBilleterasDistintas());
        assertTrue(new SolicitudTransferencia(null, "BIL-000001", BigDecimal.ONE).isBilleterasDistintas());
        assertTrue(new SolicitudOperacionProgramada(TipoOperacionProgramada.RECARGA, BigDecimal.ONE, null,
                "BIL-000001", FECHA, Periodicidad.UNICA).isBilleterasCoherentesConElTipo());
        assertTrue(!new SolicitudOperacionProgramada(TipoOperacionProgramada.RETIRO, BigDecimal.ONE, null,
                "BIL-000001", FECHA, Periodicidad.UNICA).isBilleterasCoherentesConElTipo());
        assertTrue(new SolicitudOperacionProgramada(null, BigDecimal.ONE, null, null, FECHA, Periodicidad.UNICA)
                .isBilleterasCoherentesConElTipo());
    }

    @Test
    void respuestasDesdeEntidades() {
        Usuario usuario = new Usuario("1094000111", "Ana", "ana@correo.co", 750, NivelUsuario.PLATA);
        assertEquals(new UsuarioRespuesta("1094000111", "Ana", "ana@correo.co", 750, NivelUsuario.PLATA),
                UsuarioRespuesta.desde(usuario));

        Billetera billetera = new Billetera("BIL-000001", "1094000111", "Ahorro", TipoBilletera.AHORRO,
                new BigDecimal("2500.50"), EstadoBilletera.ACTIVA);
        assertEquals(new BilleteraRespuesta("BIL-000001", "1094000111", "Ahorro", TipoBilletera.AHORRO,
                new BigDecimal("2500.50"), EstadoBilletera.ACTIVA), BilleteraRespuesta.desde(billetera));
        assertEquals(new SaldoRespuesta("BIL-000001", new BigDecimal("2500.50")), SaldoRespuesta.desde(billetera));

        Transaccion transaccion = new Transaccion("TRX-000011", FECHA, TipoTransaccion.REVERSION,
                new BigDecimal("10.00"), "BIL-000002", "BIL-000001", EstadoTransaccion.EXITOSA, -3,
                NivelRiesgo.NINGUNO, "TRX-000010");
        TransaccionRespuesta tr = TransaccionRespuesta.desde(transaccion);
        assertEquals("TRX-000011", tr.id());
        assertEquals("TRX-000010", tr.transaccionRevertida());
        assertEquals(-3, tr.puntosGenerados());
        assertEquals(new BigDecimal("10.00"), tr.valor());
        assertEquals(transaccion.billeteraOrigen(), tr.billeteraOrigen());

        OperacionProgramada op = new OperacionProgramada("OPR-000001", TipoOperacionProgramada.RECARGA,
                BigDecimal.TEN, null, "BIL-000001", FECHA, Periodicidad.MENSUAL, EstadoOperacionProgramada.PENDIENTE);
        OperacionProgramadaRespuesta opr = OperacionProgramadaRespuesta.desde(op);
        assertEquals("OPR-000001", opr.id());
        assertEquals(Periodicidad.MENSUAL, opr.periodicidad());
        assertEquals(null, opr.billeteraOrigen());

        Notificacion notificacion = new Notificacion("NOT-000001", "1094000111", TipoNotificacion.SALDO_BAJO,
                "Saldo bajo", FECHA, EstadoNotificacion.PENDIENTE);
        assertEquals(EstadoNotificacion.PENDIENTE, NotificacionRespuesta.desde(notificacion).estado());

        EventoAuditoria evento = new EventoAuditoria("AUD-000001", "1094000111", "TRX-000011",
                PatronInusual.DESTINO_REPETITIVO, "Cinco envíos al mismo destino", NivelRiesgo.MEDIO, FECHA);
        EventoAuditoriaRespuesta er = EventoAuditoriaRespuesta.desde(evento);
        assertEquals("TRX-000011", er.transaccion());
        assertEquals(PatronInusual.DESTINO_REPETITIVO, er.patron());
    }
}
