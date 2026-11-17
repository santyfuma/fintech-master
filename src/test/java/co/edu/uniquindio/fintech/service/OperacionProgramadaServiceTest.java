package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.dto.SolicitudOperacionProgramada;
import co.edu.uniquindio.fintech.exception.ConflictoNegocioException;
import co.edu.uniquindio.fintech.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.fintech.exception.SolicitudInvalidaException;
import co.edu.uniquindio.fintech.model.EstadoOperacionProgramada;
import co.edu.uniquindio.fintech.model.EstadoTransaccion;
import co.edu.uniquindio.fintech.model.OperacionProgramada;
import co.edu.uniquindio.fintech.model.Periodicidad;
import co.edu.uniquindio.fintech.model.TipoNotificacion;
import co.edu.uniquindio.fintech.model.TipoOperacionProgramada;
import co.edu.uniquindio.fintech.model.Transaccion;
import co.edu.uniquindio.fintech.soporte.SistemaPrueba;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;

import static co.edu.uniquindio.fintech.soporte.Afirmaciones.dinero;
import static co.edu.uniquindio.fintech.soporte.Afirmaciones.lanza;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperacionProgramadaServiceTest {

    @TempDir
    Path directorio;

    private SistemaPrueba s;
    private String a;
    private String b;

    @BeforeEach
    void iniciar() {
        s = new SistemaPrueba(directorio);
        s.usuario("100");
        s.usuario("200");
        a = s.billetera("100", "1000");
        b = s.billetera("200", "0");
    }

    private LocalDateTime en(Duration d) {
        return s.reloj.ahora().plus(d);
    }

    private OperacionProgramada transferencia(String valor, Duration dentroDe, Periodicidad p) {
        return s.programadas.programar(new SolicitudOperacionProgramada(TipoOperacionProgramada.TRANSFERENCIA,
                new BigDecimal(valor), a, b, en(dentroDe), p));
    }

    @Test
    void programarDejaPendienteYPersiste() {
        OperacionProgramada o = transferencia("100", Duration.ofHours(2), Periodicidad.UNICA);
        assertEquals(EstadoOperacionProgramada.PENDIENTE, o.estado());
        assertEquals(o, s.programadas.siguientePendiente());
        assertEquals(o, s.reiniciar().programadas.buscar(o.id()));
        assertEquals(1, s.programadas.porUsuario("100").tamanio());
    }

    @Test
    void validacionesAlProgramar() {
        lanza(SolicitudInvalidaException.class, "FECHA_EJECUCION_PASADA",
                () -> transferencia("10", Duration.ZERO, Periodicidad.UNICA));
        lanza(SolicitudInvalidaException.class, "VALOR_INVALIDO",
                () -> transferencia("0", Duration.ofHours(1), Periodicidad.UNICA));
        lanza(SolicitudInvalidaException.class, "BILLETERAS_INCOHERENTES", () -> s.programadas.programar(
                new SolicitudOperacionProgramada(TipoOperacionProgramada.RECARGA, BigDecimal.TEN, a, null,
                        en(Duration.ofHours(1)), Periodicidad.UNICA)));
        lanza(RecursoNoEncontradoException.class, "BILLETERA_NO_ENCONTRADA", () -> s.programadas.programar(
                new SolicitudOperacionProgramada(TipoOperacionProgramada.RECARGA, BigDecimal.TEN, null, "BIL-000099",
                        en(Duration.ofHours(1)), Periodicidad.UNICA)));
        s.billeteras.desactivar(b);
        lanza(ConflictoNegocioException.class, "BILLETERA_INACTIVA",
                () -> transferencia("10", Duration.ofHours(1), Periodicidad.UNICA));
        lanza(RecursoNoEncontradoException.class, "SIN_OPERACIONES_PENDIENTES", () -> s.programadas.siguientePendiente());
    }

    @Test
    void noSeEjecutaAntesDeSuFechaYSeEjecutaAlLlegarConBono() {
        OperacionProgramada o = transferencia("100", Duration.ofHours(2), Periodicidad.UNICA);
        long puntos = s.puntosDe("100");
        assertTrue(s.programadas.procesarVencidas().estaVacia());
        s.reloj.avanzar(Duration.ofHours(2));
        Lista<OperacionProgramadaService.ResultadoEjecucion> r = s.programadas.procesarVencidas();
        assertEquals(1, r.tamanio());
        assertEquals(EstadoOperacionProgramada.EJECUTADA, r.obtener(0).estado());
        assertNull(r.obtener(0).siguienteOperacion());
        Transaccion t = s.transacciones.buscar(r.obtener(0).transaccion());
        assertEquals(3 + 10, t.puntosGenerados()); // 3 por transferir 100 + bono provisional 10
        assertEquals(puntos + 13, s.puntosDe("100"));
        dinero("900", s.saldo(a));
        dinero("100", s.saldo(b));
        assertEquals(EstadoOperacionProgramada.EJECUTADA, s.programadas.buscar(o.id()).estado());
        assertTrue(s.programadas.procesarVencidas().estaVacia()); // no se ejecuta dos veces
        SistemaPrueba re = s.reiniciar();
        assertEquals(EstadoOperacionProgramada.EJECUTADA, re.programadas.buscar(o.id()).estado());
        dinero("100", re.saldo(b));
    }

    @Test
    void saldoInsuficienteMarcaFallidaRegistraRechazoYNoLanza() {
        OperacionProgramada o = transferencia("5000", Duration.ofMinutes(30), Periodicidad.UNICA);
        s.reloj.avanzar(Duration.ofHours(1));
        OperacionProgramadaService.ResultadoEjecucion r = s.programadas.procesarVencidas().obtener(0);
        assertEquals(EstadoOperacionProgramada.FALLIDA, r.estado());
        assertTrue(r.motivo().contains("Saldo insuficiente"));
        assertEquals(EstadoTransaccion.RECHAZADA, s.transacciones.buscar(r.transaccion()).estado());
        assertEquals(EstadoOperacionProgramada.FALLIDA, s.programadas.buscar(o.id()).estado());
        assertEquals(1, s.notificaciones("100", TipoNotificacion.OPERACION_RECHAZADA));
        dinero("1000", s.saldo(a));
        assertEquals(1, s.programadas.listar(EstadoOperacionProgramada.FALLIDA).tamanio());
    }

    @Test
    void periodicaGeneraLaSiguienteEnLaMismaEntradaDelDiario() {
        OperacionProgramada o = transferencia("10", Duration.ofHours(1), Periodicidad.SEMANAL);
        s.reloj.avanzar(Duration.ofHours(1));
        OperacionProgramadaService.ResultadoEjecucion r = s.programadas.procesarVencidas().obtener(0);
        OperacionProgramada siguiente = s.programadas.buscar(r.siguienteOperacion());
        assertEquals(EstadoOperacionProgramada.PENDIENTE, siguiente.estado());
        assertEquals(o.fechaEjecucion().plusWeeks(1), siguiente.fechaEjecucion());
        assertEquals(Periodicidad.SEMANAL, siguiente.periodicidad());
        SistemaPrueba re = s.reiniciar();
        assertEquals(siguiente, re.programadas.siguientePendiente());
        re.reloj.avanzar(Duration.ofDays(7));
        OperacionProgramadaService.ResultadoEjecucion r2 = re.programadas.procesarVencidas().obtener(0);
        assertEquals(siguiente.id(), r2.operacion());
        dinero("980", re.saldo(a));
    }

    @Test
    void periodicaFallidaTambienSeReprograma() {
        OperacionProgramada o = transferencia("5000", Duration.ofHours(1), Periodicidad.MENSUAL);
        s.reloj.avanzar(Duration.ofHours(1));
        OperacionProgramadaService.ResultadoEjecucion r = s.programadas.procesarVencidas().obtener(0);
        assertEquals(EstadoOperacionProgramada.FALLIDA, r.estado());
        assertEquals(o.fechaEjecucion().plusMonths(1), s.programadas.buscar(r.siguienteOperacion()).fechaEjecucion());
    }

    @Test
    void siguienteFechaMensualAjustaFinDeMes() {
        LocalDateTime enero31 = LocalDateTime.of(2027, 1, 31, 9, 0);
        assertEquals(LocalDateTime.of(2027, 2, 28, 9, 0),
                OperacionProgramadaService.siguienteFecha(enero31, Periodicidad.MENSUAL));
        assertEquals(LocalDateTime.of(2027, 2, 7, 9, 0),
                OperacionProgramadaService.siguienteFecha(enero31, Periodicidad.SEMANAL));
    }

    @Test
    void seEjecutanEnOrdenDeFechaYLuegoDeIdentificador() {
        OperacionProgramada tarde = transferencia("1", Duration.ofHours(3), Periodicidad.UNICA);
        OperacionProgramada temprano = transferencia("2", Duration.ofHours(1), Periodicidad.UNICA);
        OperacionProgramada empate = transferencia("3", Duration.ofHours(1), Periodicidad.UNICA);
        s.reloj.avanzar(Duration.ofHours(4));
        Lista<OperacionProgramadaService.ResultadoEjecucion> r = s.programadas.procesarVencidas();
        assertEquals(temprano.id(), r.obtener(0).operacion());
        assertEquals(empate.id(), r.obtener(1).operacion());
        assertEquals(tarde.id(), r.obtener(2).operacion());
    }

    @Test
    void cancelarSoloPendientesYLaCanceladaNoSeEjecuta() {
        OperacionProgramada o = transferencia("10", Duration.ofHours(1), Periodicidad.UNICA);
        assertEquals(EstadoOperacionProgramada.CANCELADA, s.programadas.cancelar(o.id()).estado());
        lanza(ConflictoNegocioException.class, "OPERACION_NO_CANCELABLE", () -> s.programadas.cancelar(o.id()));
        s.reloj.avanzar(Duration.ofHours(2));
        assertTrue(s.programadas.procesarVencidas().estaVacia());
        dinero("1000", s.saldo(a));
        lanza(RecursoNoEncontradoException.class, "OPERACION_NO_ENCONTRADA", () -> s.programadas.cancelar("OPR-000099"));
    }

    @Test
    void avisaUnaSolaVezLasProximasDentroDeLaAnticipacion() {
        transferencia("10", Duration.ofHours(30), Periodicidad.UNICA);
        s.programadas.procesarVencidas();
        assertEquals(0, s.notificaciones("100", TipoNotificacion.OPERACION_PROGRAMADA_PROXIMA)); // faltan 30 h
        s.reloj.avanzar(Duration.ofHours(7));
        s.programadas.procesarVencidas();
        s.programadas.procesarVencidas();
        assertEquals(1, s.notificaciones("100", TipoNotificacion.OPERACION_PROGRAMADA_PROXIMA));
        s.reiniciar().programadas.procesarVencidas();
        assertEquals(1, s.reiniciar().notificaciones("100", TipoNotificacion.OPERACION_PROGRAMADA_PROXIMA));
    }

    @Test
    void recargaYRetiroProgramados() {
        s.programadas.programar(new SolicitudOperacionProgramada(TipoOperacionProgramada.RECARGA, new BigDecimal("50"),
                null, b, en(Duration.ofMinutes(10)), Periodicidad.UNICA));
        s.programadas.programar(new SolicitudOperacionProgramada(TipoOperacionProgramada.RETIRO, new BigDecimal("20"),
                b, null, en(Duration.ofMinutes(20)), Periodicidad.UNICA));
        s.reloj.avanzar(Duration.ofMinutes(30));
        assertEquals(2, s.programadas.procesarVencidas().tamanio());
        dinero("30", s.saldo(b));
        assertEquals(2, s.programadas.porUsuario("200").tamanio());
    }
}
