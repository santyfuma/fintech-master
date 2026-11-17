package co.edu.uniquindio.fintech.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnumeracionesTest {

    @Test
    void nivelesConLosRangosDelEnunciado() {
        assertEquals(4, NivelUsuario.values().length);
        assertEquals(0, NivelUsuario.BRONCE.puntosMinimos());
        assertEquals(500, NivelUsuario.BRONCE.puntosMaximos());
        assertEquals(501, NivelUsuario.PLATA.puntosMinimos());
        assertEquals(1000, NivelUsuario.PLATA.puntosMaximos());
        assertEquals(1001, NivelUsuario.ORO.puntosMinimos());
        assertEquals(5000, NivelUsuario.ORO.puntosMaximos());
        assertEquals(5001, NivelUsuario.PLATINO.puntosMinimos());
        assertEquals(Long.MAX_VALUE, NivelUsuario.PLATINO.puntosMaximos());
    }

    @Test
    void losRangosDeNivelSonContiguosYOrdenados() {
        NivelUsuario[] niveles = NivelUsuario.values();
        for (int i = 1; i < niveles.length; i++) {
            assertEquals(niveles[i - 1].puntosMaximos() + 1, niveles[i].puntosMinimos());
        }
    }

    @Test
    void cadaTipoProgramableCorrespondeAUnTipoDeTransaccionNoReversion() {
        assertEquals(TipoTransaccion.RECARGA, TipoOperacionProgramada.RECARGA.tipoTransaccion());
        assertEquals(TipoTransaccion.RETIRO, TipoOperacionProgramada.RETIRO.tipoTransaccion());
        assertEquals(TipoTransaccion.TRANSFERENCIA, TipoOperacionProgramada.TRANSFERENCIA.tipoTransaccion());
        for (TipoOperacionProgramada tipo : TipoOperacionProgramada.values()) {
            assertFalse(tipo.tipoTransaccion() == TipoTransaccion.REVERSION);
        }
    }

    @Test
    void formaDeLasBilleterasSegunElTipo() {
        assertTrue(TipoTransaccion.RECARGA.billeterasCoherentes(null, "BIL-000001"));
        assertFalse(TipoTransaccion.RECARGA.billeterasCoherentes("BIL-000001", "BIL-000002"));
        assertFalse(TipoTransaccion.RECARGA.billeterasCoherentes(null, null));

        assertTrue(TipoTransaccion.RETIRO.billeterasCoherentes("BIL-000001", null));
        assertFalse(TipoTransaccion.RETIRO.billeterasCoherentes(null, "BIL-000001"));

        assertTrue(TipoTransaccion.TRANSFERENCIA.billeterasCoherentes("BIL-000001", "BIL-000002"));
        assertFalse(TipoTransaccion.TRANSFERENCIA.billeterasCoherentes("BIL-000001", "BIL-000001"));
        assertFalse(TipoTransaccion.TRANSFERENCIA.billeterasCoherentes("BIL-000001", null));

        assertTrue(TipoTransaccion.REVERSION.billeterasCoherentes("BIL-000001", null));
        assertTrue(TipoTransaccion.REVERSION.billeterasCoherentes(null, "BIL-000001"));
        assertTrue(TipoTransaccion.REVERSION.billeterasCoherentes("BIL-000002", "BIL-000001"));
        assertFalse(TipoTransaccion.REVERSION.billeterasCoherentes(null, null));
    }

    @Test
    void valoresDeLasEnumeraciones() {
        assertEquals("[AHORRO, GASTOS_DIARIOS, COMPRAS, TRANSPORTE, INVERSION, SUSCRIPCIONES]",
                nombres(TipoBilletera.values()));
        assertEquals("[ACTIVA, INACTIVA]", nombres(EstadoBilletera.values()));
        assertEquals("[RECARGA, RETIRO, TRANSFERENCIA, REVERSION]", nombres(TipoTransaccion.values()));
        assertEquals("[EXITOSA, RECHAZADA, REVERTIDA]", nombres(EstadoTransaccion.values()));
        assertEquals("[NINGUNO, BAJO, MEDIO, ALTO]", nombres(NivelRiesgo.values()));
        assertEquals("[PENDIENTE, EJECUTADA, FALLIDA, CANCELADA]", nombres(EstadoOperacionProgramada.values()));
        assertEquals("[UNICA, SEMANAL, MENSUAL]", nombres(Periodicidad.values()));
        assertEquals("[SALDO_BAJO, OPERACION_PROGRAMADA_PROXIMA, OPERACION_RECHAZADA, ASCENSO_NIVEL, "
                + "CANJE_REALIZADO, ALERTA_SEGURIDAD]", nombres(TipoNotificacion.values()));
        assertEquals("[PENDIENTE, DESPACHADA]", nombres(EstadoNotificacion.values()));
        assertEquals("[REALIZADO, ANULADO]", nombres(EstadoCanje.values()));
        assertEquals(6, PatronInusual.values().length);
        assertEquals(PatronInusual.HORARIO_NO_HABITUAL, PatronInusual.valueOf("HORARIO_NO_HABITUAL"));
    }

    private static String nombres(Enum<?>[] valores) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < valores.length; i++) {
            sb.append(i == 0 ? "" : ", ").append(valores[i].name());
        }
        return sb.append(']').toString();
    }
}
