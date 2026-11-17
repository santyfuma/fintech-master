package co.edu.uniquindio.fintech.model;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compatibilidad de las entidades con Jackson 3 (incluido en Spring Boot 4): ida y vuelta
 * sin pérdida, nombres de campo iguales a los del record, enums por nombre, montos
 * exactos, fechas ISO-8601 y validación de los constructores al deserializar.
 */
class SerializacionJsonModeloTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 15, 18, 0);

    private static <T> T idaYVuelta(T valor, Class<T> tipo) {
        return JSON.readValue(JSON.writeValueAsString(valor), tipo);
    }

    @Test
    void usuario() {
        Usuario usuario = new Usuario("1094000111", "Ana Pérez", "ana@correo.co", 750, NivelUsuario.PLATA);
        String json = JSON.writeValueAsString(usuario);
        assertTrue(json.contains("\"identificacion\":\"1094000111\""), json);
        assertTrue(json.contains("\"puntosAcumulados\":750"), json);
        assertTrue(json.contains("\"nivel\":\"PLATA\""), json);
        assertEquals(usuario, idaYVuelta(usuario, Usuario.class));
    }

    @Test
    void billeteraConservaElMontoExacto() {
        Billetera billetera = new Billetera("BIL-000001", "1094000111", "Ahorro", TipoBilletera.AHORRO,
                new BigDecimal("2500.50"), EstadoBilletera.ACTIVA);
        String json = JSON.writeValueAsString(billetera);
        assertTrue(json.contains("\"saldo\":2500.50"), json);
        assertFalse(json.contains("conSaldo"), json);
        Billetera leida = idaYVuelta(billetera, Billetera.class);
        assertEquals(0, new BigDecimal("2500.50").compareTo(leida.saldo()));
        assertEquals(billetera.codigo(), leida.codigo());
        assertEquals(billetera.estado(), leida.estado());
    }

    @Test
    void transaccionConCamposOpcionalesYFechaIso() {
        Transaccion recarga = new Transaccion("TRX-000001", FECHA, TipoTransaccion.RECARGA, new BigDecimal("100.00"),
                null, "BIL-000001", EstadoTransaccion.EXITOSA, 1, NivelRiesgo.NINGUNO, null);
        String json = JSON.writeValueAsString(recarga);
        assertTrue(json.contains("\"fecha\":\"2026-11-15T18:00:00\""), json);
        assertTrue(json.contains("\"tipo\":\"RECARGA\""), json);
        assertFalse(json.contains("esReversion"), json);
        Transaccion leida = idaYVuelta(recarga, Transaccion.class);
        assertEquals(recarga.id(), leida.id());
        assertEquals(FECHA, leida.fecha());
        assertEquals(null, leida.billeteraOrigen());
        assertEquals("BIL-000001", leida.billeteraDestino());
        assertEquals(0, recarga.valor().compareTo(leida.valor()));

        Transaccion reversion = new Transaccion("TRX-000002", FECHA, TipoTransaccion.REVERSION,
                new BigDecimal("100.00"), "BIL-000001", null, EstadoTransaccion.EXITOSA, -1, NivelRiesgo.NINGUNO,
                "TRX-000001");
        assertEquals("TRX-000001", idaYVuelta(reversion, Transaccion.class).transaccionRevertida());
    }

    @Test
    void operacionNotificacionAuditoriaYCanje() {
        OperacionProgramada operacion = new OperacionProgramada("OPR-000001", TipoOperacionProgramada.TRANSFERENCIA,
                new BigDecimal("50000"), "BIL-000001", "BIL-000002", FECHA, Periodicidad.SEMANAL,
                EstadoOperacionProgramada.PENDIENTE);
        assertEquals(operacion, idaYVuelta(operacion, OperacionProgramada.class));

        Notificacion notificacion = new Notificacion("NOT-000001", "1094000111", TipoNotificacion.ASCENSO_NIVEL,
                "Subiste a PLATA", FECHA, EstadoNotificacion.PENDIENTE);
        assertEquals(notificacion, idaYVuelta(notificacion, Notificacion.class));

        EventoAuditoria evento = new EventoAuditoria("AUD-000001", "1094000111", null,
                PatronInusual.HORARIO_NO_HABITUAL, "Retiro a las 3:00", NivelRiesgo.MEDIO, FECHA);
        assertEquals(evento, idaYVuelta(evento, EventoAuditoria.class));

        BeneficioCanjeado canje = new BeneficioCanjeado("CAN-000001", "1094000111", "Bono", 200, FECHA,
                EstadoCanje.REALIZADO);
        assertEquals(canje, idaYVuelta(canje, BeneficioCanjeado.class));
    }

    @Test
    void unArregloDeRegistrosSeLeeSinUsarListas() {
        Billetera[] billeteras = {
                new Billetera("BIL-000001", "1", "A", TipoBilletera.AHORRO, BigDecimal.ONE, EstadoBilletera.ACTIVA),
                new Billetera("BIL-000002", "1", "B", TipoBilletera.COMPRAS, BigDecimal.TEN, EstadoBilletera.INACTIVA)
        };
        Billetera[] leidas = JSON.readValue(JSON.writeValueAsString(billeteras), Billetera[].class);
        assertEquals(2, leidas.length);
        assertEquals("BIL-000002", leidas[1].codigo());
        assertEquals(EstadoBilletera.INACTIVA, leidas[1].estado());
    }

    @Test
    void unRegistroPersistidoMalFormadoNoSeAcepta() {
        String codigoInvalido = "{\"codigo\":\"X-1\",\"identificacionPropietario\":\"1\",\"nombre\":\"A\","
                + "\"tipo\":\"AHORRO\",\"saldo\":1,\"estado\":\"ACTIVA\"}";
        assertThrows(RuntimeException.class, () -> JSON.readValue(codigoInvalido, Billetera.class));
        String recargaConOrigen = "{\"id\":\"TRX-000001\",\"fecha\":\"2026-11-15T18:00:00\",\"tipo\":\"RECARGA\","
                + "\"valor\":1,\"billeteraOrigen\":\"BIL-000001\",\"billeteraDestino\":\"BIL-000002\","
                + "\"estado\":\"EXITOSA\",\"puntosGenerados\":0,\"nivelRiesgo\":\"NINGUNO\"}";
        assertThrows(RuntimeException.class, () -> JSON.readValue(recargaConOrigen, Transaccion.class));
    }
}
