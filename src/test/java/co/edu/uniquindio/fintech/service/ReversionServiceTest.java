package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.exception.ConflictoNegocioException;
import co.edu.uniquindio.fintech.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.fintech.model.EstadoTransaccion;
import co.edu.uniquindio.fintech.model.TipoTransaccion;
import co.edu.uniquindio.fintech.model.Transaccion;
import co.edu.uniquindio.fintech.soporte.RelojAjustable;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReversionServiceTest {

    @TempDir
    Path directorio;

    private SistemaPrueba s;

    @BeforeEach
    void iniciar() {
        s = new SistemaPrueba(directorio, new RelojAjustable(), "CAFE|Bono café|8");
        s.usuario("100");
        s.usuario("200");
    }

    private static BigDecimal v(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    void revertirRecargaDevuelveSaldoYPuntosYReferenciaLaOriginal() {
        String a = s.billetera("100", "0");
        Transaccion recarga = s.transacciones.recargar(a, v("1000"));
        Transaccion reversion = s.transacciones.revertir(recarga.id());
        assertEquals(TipoTransaccion.REVERSION, reversion.tipo());
        assertEquals(EstadoTransaccion.EXITOSA, reversion.estado());
        assertEquals(recarga.id(), reversion.transaccionRevertida());
        assertEquals(a, reversion.billeteraOrigen());
        assertEquals(-10, reversion.puntosGenerados());
        assertEquals(EstadoTransaccion.REVERTIDA, s.transacciones.buscar(recarga.id()).estado());
        dinero("0", s.saldo(a));
        assertEquals(0, s.puntosDe("100"));
        SistemaPrueba r = s.reiniciar();
        assertEquals(EstadoTransaccion.REVERTIDA, r.transacciones.buscar(recarga.id()).estado());
        assertEquals(reversion, r.transacciones.buscar(reversion.id()));
        dinero("0", r.saldo(a));
    }

    @Test
    void revertirTransferenciaDevuelveElDineroAlOrigen() {
        String a = s.billetera("100", "500");
        String b = s.billetera("200", "0");
        Transaccion t = s.transacciones.transferir(a, b, v("200.25"));
        s.transacciones.revertir(t.id());
        dinero("500.00", s.saldo(a));
        dinero("0.00", s.saldo(b));
        assertEquals(5, s.puntosDe("100")); // queda solo lo de la recarga inicial
    }

    @Test
    void reglasDeReversion() {
        String a = s.billetera("100", "1000");
        Transaccion retiro = s.transacciones.retirar(a, v("100"));
        lanza(ConflictoNegocioException.class, "OPERACION_NO_REVERSIBLE", () -> s.transacciones.revertir(retiro.id()));
        Transaccion recarga = s.transacciones.recargar(a, v("100"));
        Transaccion reversion = s.transacciones.revertir(recarga.id());
        lanza(ConflictoNegocioException.class, "TRANSACCION_YA_REVERTIDA", () -> s.transacciones.revertir(recarga.id()));
        lanza(ConflictoNegocioException.class, "OPERACION_NO_REVERSIBLE", () -> s.transacciones.revertir(reversion.id()));
        lanza(RecursoNoEncontradoException.class, "TRANSACCION_NO_ENCONTRADA",
                () -> s.transacciones.revertir("TRX-000999"));
    }

    @Test
    void unaRechazadaNoSeRevierte() {
        String a = s.billetera("100", "10");
        lanza(ConflictoNegocioException.class, "SALDO_INSUFICIENTE", () -> s.transacciones.retirar(a, v("20")));
        Transaccion rechazada = s.transacciones.historialDeBilletera(a).obtener(1);
        assertEquals(EstadoTransaccion.RECHAZADA, rechazada.estado());
        lanza(ConflictoNegocioException.class, "OPERACION_NO_REVERSIBLE", () -> s.transacciones.revertir(rechazada.id()));
    }

    @Test
    void fueraDelPlazoNoSeRevierte() {
        String a = s.billetera("100", "0");
        Transaccion recarga = s.transacciones.recargar(a, v("100"));
        s.reloj.avanzar(Duration.ofHours(24));
        Transaccion dentro = s.transacciones.recargar(a, v("1"));
        s.reloj.avanzar(Duration.ofSeconds(1));
        lanza(ConflictoNegocioException.class, "PLAZO_DE_REVERSION_VENCIDO", () -> s.transacciones.revertir(recarga.id()));
        s.transacciones.revertir(dentro.id());
        dinero("100", s.saldo(a));
    }

    @Test
    void sinSaldoParaDevolverNoSeRevierteNiCambiaNada() {
        String a = s.billetera("100", "0");
        Transaccion recarga = s.transacciones.recargar(a, v("100"));
        s.transacciones.retirar(a, v("80"));
        lanza(ConflictoNegocioException.class, "SALDO_INSUFICIENTE", () -> s.transacciones.revertir(recarga.id()));
        dinero("20", s.saldo(a));
        assertEquals(EstadoTransaccion.EXITOSA, s.transacciones.buscar(recarga.id()).estado());
    }

    @Test
    void billeteraInactivaImpideLaReversion() {
        String a = s.billetera("100", "100");
        String b = s.billetera("100", "0");
        Transaccion t = s.transacciones.transferir(a, b, v("100"));
        s.billeteras.desactivar(a);
        lanza(ConflictoNegocioException.class, "BILLETERA_INACTIVA", () -> s.transacciones.revertir(t.id()));
        dinero("100", s.saldo(b));
    }

    @Test
    void losPuntosNuncaQuedanNegativos() {
        String a = s.billetera("100", "0");
        Transaccion recarga = s.transacciones.recargar(a, v("1000")); // 10 puntos
        s.recompensas.canjear("100", "CAFE");                          // quedan 2
        Transaccion reversion = s.transacciones.revertir(recarga.id());
        assertEquals(0, s.puntosDe("100"));
        assertEquals(-2, reversion.puntosGenerados()); // solo se descuenta lo que había
    }

    @Test
    void revertirUltimaSigueElOrdenDeLaPilaInclusoTrasReiniciar() {
        String a = s.billetera("100", "0");
        String b = s.billetera("200", "0");
        Transaccion recarga = s.transacciones.recargar(a, v("300"));
        s.reloj.avanzar(Duration.ofMinutes(1));
        Transaccion transferencia = s.transacciones.transferir(a, b, v("100"));
        s.reloj.avanzar(Duration.ofMinutes(1));
        s.transacciones.recargar(b, v("50")); // la origina el usuario 200: no entra a la pila de 100

        assertEquals(transferencia.id(), s.transacciones.revertirUltima("100").transaccionRevertida());
        SistemaPrueba r = s.reiniciar(); // la pila se reconstruye desde el historial
        assertEquals(recarga.id(), r.transacciones.revertirUltima("100").transaccionRevertida());
        lanza(ConflictoNegocioException.class, "SIN_OPERACIONES_REVERSIBLES", () -> r.transacciones.revertirUltima("100"));
        lanza(RecursoNoEncontradoException.class, "USUARIO_NO_ENCONTRADO", () -> r.transacciones.revertirUltima("9"));
        dinero("0", r.saldo(a));
        dinero("50", r.saldo(b));
    }

    @Test
    void laPilaDeUnUsuarioNoIncluyeOperacionesDeOtro() {
        String a = s.billetera("100", "0");
        String b = s.billetera("200", "0");
        s.transacciones.recargar(a, v("100"));
        lanza(ConflictoNegocioException.class, "SIN_OPERACIONES_REVERSIBLES", () -> s.transacciones.revertirUltima("200"));
        s.transacciones.recargar(b, v("100"));
        assertEquals(b, s.transacciones.revertirUltima("200").billeteraOrigen());
        dinero("100", s.saldo(a));
    }
}
