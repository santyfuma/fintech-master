package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.exception.ConflictoNegocioException;
import co.edu.uniquindio.fintech.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.fintech.exception.SolicitudInvalidaException;
import co.edu.uniquindio.fintech.model.EstadoTransaccion;
import co.edu.uniquindio.fintech.model.NivelRiesgo;
import co.edu.uniquindio.fintech.model.NivelUsuario;
import co.edu.uniquindio.fintech.model.TipoNotificacion;
import co.edu.uniquindio.fintech.model.TipoTransaccion;
import co.edu.uniquindio.fintech.model.Transaccion;
import co.edu.uniquindio.fintech.soporte.SistemaPrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;

import static co.edu.uniquindio.fintech.soporte.Afirmaciones.dinero;
import static co.edu.uniquindio.fintech.soporte.Afirmaciones.lanza;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransaccionServiceTest {

    @TempDir
    Path directorio;

    private SistemaPrueba s;

    @BeforeEach
    void iniciar() {
        s = new SistemaPrueba(directorio);
        s.usuario("100");
        s.usuario("200");
    }

    private static BigDecimal v(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    void recargaAumentaSaldoGeneraPuntosYQuedaExitosa() {
        String a = s.billetera("100", "0");
        Transaccion t = s.transacciones.recargar(a, v("250.99"));
        assertEquals(TipoTransaccion.RECARGA, t.tipo());
        assertEquals(EstadoTransaccion.EXITOSA, t.estado());
        assertNull(t.billeteraOrigen());
        assertEquals(2, t.puntosGenerados()); // 1 punto por cada 100 completos (provisional)
        dinero("250.99", s.saldo(a));
        assertEquals(2, s.puntosDe("100"));
        assertEquals(NivelRiesgo.NINGUNO, t.nivelRiesgo());
    }

    @Test
    void retiroDisminuyeSaldoYGeneraDosPuntosPorCada100() {
        String a = s.billetera("100", "1000");
        Transaccion t = s.transacciones.retirar(a, v("500"));
        dinero("500", s.saldo(a));
        assertEquals(10, t.puntosGenerados());
        assertEquals(10 + 10, s.puntosDe("100"));
    }

    @Test
    void transferenciaMueveDineroYSoloPuntuaAlQueEnvia() {
        String a = s.billetera("100", "2000");
        String b = s.billetera("200", "0");
        long antes200 = s.puntosDe("200");
        Transaccion t = s.transacciones.transferir(a, b, v("1000.50"));
        dinero("999.50", s.saldo(a));
        dinero("1000.50", s.saldo(b));
        assertEquals(30, t.puntosGenerados());
        assertEquals(20 + 30, s.puntosDe("100"));
        assertEquals(antes200, s.puntosDe("200"));
        assertTrue(s.transacciones.historialDeBilletera(b).contiene(t));
        assertTrue(s.transacciones.historialDeUsuario("200").contiene(t));
    }

    @Test
    void decimalesSeSumanSinErroresDeRedondeoYConservanEscala() {
        String a = s.billetera("100", "0");
        s.transacciones.recargar(a, v("0.10"));
        s.transacciones.recargar(a, v("0.20"));
        dinero("0.30", s.saldo(a));
        s.transacciones.recargar(a, v("100.5"));
        dinero("100.80", s.saldo(a));
        dinero("100.80", s.reiniciar().saldo(a));
    }

    @Test
    void erroresDeSolicitudNoRegistranNada() {
        String a = s.billetera("100", "100");
        int antes = s.transacciones.todas().tamanio();
        lanza(SolicitudInvalidaException.class, "VALOR_INVALIDO", () -> s.transacciones.recargar(a, v("0")));
        lanza(SolicitudInvalidaException.class, "VALOR_INVALIDO", () -> s.transacciones.retirar(a, v("-5")));
        lanza(SolicitudInvalidaException.class, "VALOR_INVALIDO", () -> s.transacciones.recargar(a, null));
        lanza(SolicitudInvalidaException.class, "MISMA_BILLETERA", () -> s.transacciones.transferir(a, a, v("1")));
        lanza(SolicitudInvalidaException.class, "BILLETERA_REQUERIDA", () -> s.transacciones.recargar(null, v("1")));
        lanza(RecursoNoEncontradoException.class, "BILLETERA_NO_ENCONTRADA",
                () -> s.transacciones.transferir(a, "BIL-000999", v("1")));
        assertEquals(antes, s.transacciones.todas().tamanio());
        dinero("100", s.saldo(a));
    }

    @Test
    void saldoInsuficienteQuedaRegistradoComoRechazadaYNotifica() {
        String a = s.billetera("100", "100");
        String b = s.billetera("200", "0");
        long puntos = s.puntosDe("100");
        lanza(ConflictoNegocioException.class, "SALDO_INSUFICIENTE", () -> s.transacciones.transferir(a, b, v("100.01")));
        Transaccion rechazada = s.transacciones.todas().obtener(s.transacciones.todas().tamanio() - 1);
        assertEquals(EstadoTransaccion.RECHAZADA, rechazada.estado());
        assertEquals(0, rechazada.puntosGenerados());
        dinero("100", s.saldo(a));
        dinero("0", s.saldo(b));
        assertEquals(puntos, s.puntosDe("100"));
        assertEquals(1, s.notificaciones("100", TipoNotificacion.OPERACION_RECHAZADA));
        SistemaPrueba r = s.reiniciar();
        assertEquals(EstadoTransaccion.RECHAZADA, r.transacciones.buscar(rechazada.id()).estado());
        assertEquals(1, r.notificaciones("100", TipoNotificacion.OPERACION_RECHAZADA));
    }

    @Test
    void billeteraInactivaRechazaYRegistra() {
        String a = s.billetera("100", "0");
        s.billeteras.desactivar(a);
        lanza(ConflictoNegocioException.class, "BILLETERA_INACTIVA", () -> s.transacciones.recargar(a, v("50")));
        assertEquals(EstadoTransaccion.RECHAZADA, s.transacciones.historialDeBilletera(a).obtener(0).estado());
        dinero("0", s.saldo(a));
    }

    @Test
    void retiroExactoDejaSaldoCeroYAvisaSaldoBajo() {
        String a = s.billetera("100", "20000");
        s.transacciones.retirar(a, v("15000"));
        assertEquals(1, s.notificaciones("100", TipoNotificacion.SALDO_BAJO)); // 5000 < 10000 (provisional)
        s.transacciones.retirar(a, v("5000"));
        dinero("0", s.saldo(a));
        assertEquals(2, s.notificaciones("100", TipoNotificacion.SALDO_BAJO));
    }

    @Test
    void cruzarUmbralDePuntosAsciendeDeNivelYNotifica() {
        String a = s.billetera("100", "50000"); // 500 puntos: aún BRONCE
        assertEquals(NivelUsuario.BRONCE, s.usuarios.buscar("100").nivel());
        assertEquals(0, s.notificaciones("100", TipoNotificacion.ASCENSO_NIVEL));
        s.transacciones.recargar(a, v("100")); // 501 → PLATA
        assertEquals(NivelUsuario.PLATA, s.usuarios.buscar("100").nivel());
        assertEquals(1, s.notificaciones("100", TipoNotificacion.ASCENSO_NIVEL));
        assertEquals(NivelUsuario.PLATA, s.reiniciar().usuarios.buscar("100").nivel());
    }

    @Test
    void todoUnMovimientoQuedaEnArchivosYElDiarioVacio() {
        String a = s.billetera("100", "500");
        String b = s.billetera("200", "0");
        Transaccion t = s.transacciones.transferir(a, b, v("200"));
        assertTrue(s.entorno.diario.pendientes().estaVacia());
        SistemaPrueba r = s.reiniciar();
        assertEquals(t, r.transacciones.buscar(t.id()));
        dinero("300", r.saldo(a));
        dinero("200", r.saldo(b));
        assertEquals(s.puntosDe("100"), r.puntosDe("100"));
    }

    @Test
    void historialDeUsuarioInexistenteEs404() {
        lanza(RecursoNoEncontradoException.class, "USUARIO_NO_ENCONTRADO", () -> s.transacciones.historialDeUsuario("9"));
        lanza(RecursoNoEncontradoException.class, "TRANSACCION_NO_ENCONTRADA",
                () -> s.transacciones.buscar("TRX-000999"));
    }

    @Test
    void fechaDeLaTransaccionVieneDelRelojInyectado() {
        String a = s.billetera("100", "0");
        s.reloj.avanzar(Duration.ofHours(3));
        Transaccion t = s.transacciones.recargar(a, v("10"));
        assertEquals(s.reloj.ahora(), t.fecha());
    }
}
