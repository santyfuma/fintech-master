package co.edu.uniquindio.fintech.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransaccionTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 15, 18, 0);
    private static final BigDecimal VALOR = new BigDecimal("150000.75");

    private static Transaccion transaccion(String id, TipoTransaccion tipo, String origen, String destino,
                                           String revertida) {
        return new Transaccion(id, FECHA, tipo, VALOR, origen, destino, EstadoTransaccion.EXITOSA, 0,
                NivelRiesgo.NINGUNO, revertida);
    }

    @Test
    void transferenciaConTodosSusCampos() {
        Transaccion t = new Transaccion("TRX-000042", FECHA, TipoTransaccion.TRANSFERENCIA, VALOR,
                "BIL-000001", "BIL-000007", EstadoTransaccion.EXITOSA, 4500, NivelRiesgo.BAJO, null);
        assertEquals("TRX-000042", t.id());
        assertEquals(FECHA, t.fecha());
        assertEquals(TipoTransaccion.TRANSFERENCIA, t.tipo());
        assertEquals(new BigDecimal("150000.75"), t.valor());
        assertEquals("BIL-000001", t.billeteraOrigen());
        assertEquals("BIL-000007", t.billeteraDestino());
        assertEquals(EstadoTransaccion.EXITOSA, t.estado());
        assertEquals(4500, t.puntosGenerados());
        assertEquals(NivelRiesgo.BAJO, t.nivelRiesgo());
        assertNull(t.transaccionRevertida());
        assertFalse(t.esReversion());
    }

    @Test
    void unaRecargaNoTieneBilleteraOrigen() {
        Transaccion recarga = transaccion("TRX-000001", TipoTransaccion.RECARGA, null, "BIL-000001", null);
        assertNull(recarga.billeteraOrigen());
        assertEquals("BIL-000001", recarga.billeteraDestino());
        assertThrows(IllegalArgumentException.class,
                () -> transaccion("TRX-000001", TipoTransaccion.RECARGA, "BIL-000002", "BIL-000001", null));
        assertThrows(IllegalArgumentException.class,
                () -> transaccion("TRX-000001", TipoTransaccion.RECARGA, null, null, null));
    }

    @Test
    void unRetiroNoTieneBilleteraDestino() {
        Transaccion retiro = transaccion("TRX-000002", TipoTransaccion.RETIRO, "BIL-000001", null, null);
        assertEquals("BIL-000001", retiro.billeteraOrigen());
        assertNull(retiro.billeteraDestino());
        assertThrows(IllegalArgumentException.class,
                () -> transaccion("TRX-000002", TipoTransaccion.RETIRO, "BIL-000001", "BIL-000002", null));
    }

    @Test
    void unaTransferenciaRequiereDosBilleterasDistintas() {
        assertThrows(IllegalArgumentException.class,
                () -> transaccion("TRX-000003", TipoTransaccion.TRANSFERENCIA, "BIL-000001", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> transaccion("TRX-000003", TipoTransaccion.TRANSFERENCIA, null, "BIL-000001", null));
        assertThrows(IllegalArgumentException.class,
                () -> transaccion("TRX-000003", TipoTransaccion.TRANSFERENCIA, "BIL-000001", "BIL-000001", null));
    }

    @Test
    void lasReversionesReferencianSinAmbiguedadALaOriginal() {
        Transaccion original = transaccion("TRX-000010", TipoTransaccion.TRANSFERENCIA,
                "BIL-000001", "BIL-000002", null);
        Transaccion reversion = new Transaccion("TRX-000011", FECHA.plusMinutes(5), TipoTransaccion.REVERSION,
                VALOR, "BIL-000002", "BIL-000001", EstadoTransaccion.EXITOSA, 0, NivelRiesgo.NINGUNO,
                original.id());
        assertTrue(reversion.esReversion());
        assertEquals("TRX-000010", reversion.transaccionRevertida());
        assertEquals(EstadoTransaccion.REVERTIDA, original.conEstado(EstadoTransaccion.REVERTIDA).estado());
    }

    @Test
    void unaReversionDebeReferenciarAOtraTransaccionValida() {
        assertThrows(NullPointerException.class,
                () -> transaccion("TRX-000011", TipoTransaccion.REVERSION, "BIL-000001", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> transaccion("TRX-000011", TipoTransaccion.REVERSION, "BIL-000001", null, "BIL-000001"));
        assertThrows(IllegalArgumentException.class,
                () -> transaccion("TRX-000011", TipoTransaccion.REVERSION, "BIL-000001", null, "TRX-000011"));
        assertThrows(IllegalArgumentException.class,
                () -> transaccion("TRX-000011", TipoTransaccion.REVERSION, null, null, "TRX-000010"));
    }

    @Test
    void soloUnaReversionPuedeTenerTransaccionRevertida() {
        for (TipoTransaccion tipo : new TipoTransaccion[]{TipoTransaccion.RECARGA, TipoTransaccion.RETIRO,
                TipoTransaccion.TRANSFERENCIA}) {
            String origen = tipo == TipoTransaccion.RECARGA ? null : "BIL-000001";
            String destino = tipo == TipoTransaccion.RETIRO ? null : "BIL-000002";
            assertThrows(IllegalArgumentException.class,
                    () -> transaccion("TRX-000020", tipo, origen, destino, "TRX-000001"), tipo.name());
        }
    }

    @Test
    void unaTransaccionRechazadaSeRepresentaConSuEstado() {
        Transaccion rechazada = new Transaccion("TRX-000030", FECHA, TipoTransaccion.RETIRO, VALOR,
                "BIL-000001", null, EstadoTransaccion.RECHAZADA, 0, NivelRiesgo.NINGUNO, null);
        assertEquals(EstadoTransaccion.RECHAZADA, rechazada.estado());
        assertEquals(0, rechazada.puntosGenerados());
    }

    @Test
    void conEstadoNoModificaElOriginal() {
        Transaccion original = transaccion("TRX-000040", TipoTransaccion.RECARGA, null, "BIL-000001", null);
        Transaccion revertida = original.conEstado(EstadoTransaccion.REVERTIDA);
        assertEquals(EstadoTransaccion.EXITOSA, original.estado());
        assertEquals(EstadoTransaccion.REVERTIDA, revertida.estado());
        assertEquals(original.id(), revertida.id());
        assertEquals(original.valor(), revertida.valor());
    }

    @Test
    void validaFormatosYCamposObligatorios() {
        assertThrows(IllegalArgumentException.class,
                () -> transaccion("BIL-000001", TipoTransaccion.RECARGA, null, "BIL-000001", null));
        assertThrows(IllegalArgumentException.class,
                () -> transaccion("TRX-000001", TipoTransaccion.RECARGA, null, "billetera-1", null));
        assertThrows(NullPointerException.class, () -> new Transaccion("TRX-000001", null,
                TipoTransaccion.RECARGA, VALOR, null, "BIL-000001", EstadoTransaccion.EXITOSA, 0,
                NivelRiesgo.NINGUNO, null));
        assertThrows(NullPointerException.class, () -> new Transaccion("TRX-000001", FECHA,
                null, VALOR, null, "BIL-000001", EstadoTransaccion.EXITOSA, 0, NivelRiesgo.NINGUNO, null));
        assertThrows(NullPointerException.class, () -> new Transaccion("TRX-000001", FECHA,
                TipoTransaccion.RECARGA, null, null, "BIL-000001", EstadoTransaccion.EXITOSA, 0,
                NivelRiesgo.NINGUNO, null));
        assertThrows(NullPointerException.class, () -> new Transaccion("TRX-000001", FECHA,
                TipoTransaccion.RECARGA, VALOR, null, "BIL-000001", null, 0, NivelRiesgo.NINGUNO, null));
        assertThrows(NullPointerException.class, () -> new Transaccion("TRX-000001", FECHA,
                TipoTransaccion.RECARGA, VALOR, null, "BIL-000001", EstadoTransaccion.EXITOSA, 0, null, null));
    }
}
