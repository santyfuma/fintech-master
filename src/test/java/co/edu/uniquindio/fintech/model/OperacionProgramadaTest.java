package co.edu.uniquindio.fintech.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OperacionProgramadaTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 12, 1, 8, 30);

    private static OperacionProgramada ahorroSemanal() {
        return new OperacionProgramada("OPR-000003", TipoOperacionProgramada.TRANSFERENCIA, new BigDecimal("50000"),
                "BIL-000001", "BIL-000002", FECHA, Periodicidad.SEMANAL, EstadoOperacionProgramada.PENDIENTE);
    }

    @Test
    void construyeConTodosSusCampos() {
        OperacionProgramada o = ahorroSemanal();
        assertEquals("OPR-000003", o.id());
        assertEquals(TipoOperacionProgramada.TRANSFERENCIA, o.tipo());
        assertEquals(new BigDecimal("50000"), o.valor());
        assertEquals("BIL-000001", o.billeteraOrigen());
        assertEquals("BIL-000002", o.billeteraDestino());
        assertEquals(FECHA, o.fechaEjecucion());
        assertEquals(Periodicidad.SEMANAL, o.periodicidad());
        assertEquals(EstadoOperacionProgramada.PENDIENTE, o.estado());
    }

    @Test
    void representaTodosLosEstadosDelCicloDeVida() {
        OperacionProgramada pendiente = ahorroSemanal();
        for (EstadoOperacionProgramada estado : EstadoOperacionProgramada.values()) {
            assertEquals(estado, pendiente.conEstado(estado).estado());
        }
        assertEquals(EstadoOperacionProgramada.PENDIENTE, pendiente.estado());
    }

    @Test
    void reprogramarDevuelveUnaCopiaConNuevaFecha() {
        OperacionProgramada siguiente = ahorroSemanal().conFechaEjecucion(FECHA.plusWeeks(1));
        assertEquals(FECHA.plusWeeks(1), siguiente.fechaEjecucion());
        assertEquals(FECHA, ahorroSemanal().fechaEjecucion());
    }

    @Test
    void billeterasSegunElTipo() {
        OperacionProgramada recarga = new OperacionProgramada("OPR-000001", TipoOperacionProgramada.RECARGA,
                BigDecimal.TEN, null, "BIL-000001", FECHA, Periodicidad.MENSUAL, EstadoOperacionProgramada.PENDIENTE);
        assertNull(recarga.billeteraOrigen());
        OperacionProgramada retiro = new OperacionProgramada("OPR-000002", TipoOperacionProgramada.RETIRO,
                BigDecimal.TEN, "BIL-000001", null, FECHA, Periodicidad.UNICA, EstadoOperacionProgramada.PENDIENTE);
        assertNull(retiro.billeteraDestino());
        assertThrows(IllegalArgumentException.class, () -> new OperacionProgramada("OPR-000001",
                TipoOperacionProgramada.RECARGA, BigDecimal.TEN, "BIL-000002", "BIL-000001", FECHA,
                Periodicidad.UNICA, EstadoOperacionProgramada.PENDIENTE));
        assertThrows(IllegalArgumentException.class, () -> new OperacionProgramada("OPR-000001",
                TipoOperacionProgramada.TRANSFERENCIA, BigDecimal.TEN, "BIL-000001", "BIL-000001", FECHA,
                Periodicidad.UNICA, EstadoOperacionProgramada.PENDIENTE));
    }

    @Test
    void validaFormatosYCamposObligatorios() {
        assertThrows(IllegalArgumentException.class, () -> new OperacionProgramada("TRX-000001",
                TipoOperacionProgramada.RECARGA, BigDecimal.TEN, null, "BIL-000001", FECHA,
                Periodicidad.UNICA, EstadoOperacionProgramada.PENDIENTE));
        assertThrows(NullPointerException.class, () -> new OperacionProgramada("OPR-000001",
                null, BigDecimal.TEN, null, "BIL-000001", FECHA, Periodicidad.UNICA,
                EstadoOperacionProgramada.PENDIENTE));
        assertThrows(NullPointerException.class, () -> new OperacionProgramada("OPR-000001",
                TipoOperacionProgramada.RECARGA, BigDecimal.TEN, null, "BIL-000001", null,
                Periodicidad.UNICA, EstadoOperacionProgramada.PENDIENTE));
        assertThrows(NullPointerException.class, () -> new OperacionProgramada("OPR-000001",
                TipoOperacionProgramada.RECARGA, BigDecimal.TEN, null, "BIL-000001", FECHA,
                null, EstadoOperacionProgramada.PENDIENTE));
        assertThrows(NullPointerException.class, () -> new OperacionProgramada("OPR-000001",
                TipoOperacionProgramada.RECARGA, null, null, "BIL-000001", FECHA,
                Periodicidad.UNICA, EstadoOperacionProgramada.PENDIENTE));
    }
}
