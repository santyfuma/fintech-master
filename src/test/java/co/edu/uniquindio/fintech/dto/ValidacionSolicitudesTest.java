package co.edu.uniquindio.fintech.dto;

import co.edu.uniquindio.fintech.model.EstadoBilletera;
import co.edu.uniquindio.fintech.model.Periodicidad;
import co.edu.uniquindio.fintech.model.TipoBilletera;
import co.edu.uniquindio.fintech.model.TipoOperacionProgramada;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Bean Validation real (Hibernate Validator, incluido en spring-boot-starter-validation). */
class ValidacionSolicitudesTest {

    private static final ValidatorFactory FABRICA = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDADOR = FABRICA.getValidator();
    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 12, 1, 8, 0);

    @AfterAll
    static void cerrar() {
        FABRICA.close();
    }

    /** @return propiedades con error, ordenadas, p. ej. "[correo, valor]" */
    private static String invalidas(Object dto) {
        TreeSet<String> propiedades = new TreeSet<>();
        for (ConstraintViolation<Object> v : VALIDADOR.validate(dto)) {
            propiedades.add(v.getPropertyPath().toString());
        }
        return propiedades.toString();
    }

    private static String mensajes(Object dto) {
        TreeSet<String> mensajes = new TreeSet<>();
        for (ConstraintViolation<Object> v : VALIDADOR.validate(dto)) {
            mensajes.add(v.getMessage());
        }
        return mensajes.toString();
    }

    @Test
    void registroDeUsuario() {
        assertEquals("[]", invalidas(new SolicitudRegistroUsuario("1094000111", "Ana", "ana@correo.co")));
        assertEquals("[identificacion]", invalidas(new SolicitudRegistroUsuario(" ", "Ana", "ana@correo.co")));
        assertEquals("[nombre]", invalidas(new SolicitudRegistroUsuario("1", null, "ana@correo.co")));
        assertEquals("[correo]", invalidas(new SolicitudRegistroUsuario("1", "Ana", "no-es-correo")));
        assertEquals("[correo, identificacion, nombre]", invalidas(new SolicitudRegistroUsuario(null, "", null)));
        assertEquals("[El correo no tiene un formato válido]",
                mensajes(new SolicitudRegistroUsuario("1", "Ana", "no-es-correo")));
    }

    @Test
    void actualizacionDeUsuario() {
        assertEquals("[]", invalidas(new SolicitudActualizacionUsuario("Ana", "ana@correo.co")));
        assertEquals("[correo, nombre]", invalidas(new SolicitudActualizacionUsuario("", "x")));
    }

    @Test
    void creacionYActualizacionDeBilletera() {
        assertEquals("[]", invalidas(new SolicitudCreacionBilletera("Ahorro", TipoBilletera.AHORRO)));
        assertEquals("[nombre, tipo]", invalidas(new SolicitudCreacionBilletera(" ", null)));
        assertEquals("[]", invalidas(new SolicitudActualizacionBilletera("Ahorro", TipoBilletera.AHORRO,
                EstadoBilletera.INACTIVA)));
        assertEquals("[estado]", invalidas(new SolicitudActualizacionBilletera("Ahorro", TipoBilletera.AHORRO, null)));
    }

    @Test
    void recargaYRetiro() {
        assertEquals("[]", invalidas(new SolicitudRecarga("BIL-000001", CIEN)));
        assertEquals("[billeteraDestino]", invalidas(new SolicitudRecarga("BIL-1", CIEN)));
        assertEquals("[billeteraDestino]", invalidas(new SolicitudRecarga(null, CIEN)));
        assertEquals("[valor]", invalidas(new SolicitudRecarga("BIL-000001", BigDecimal.ZERO)));
        assertEquals("[valor]", invalidas(new SolicitudRecarga("BIL-000001", new BigDecimal("-5"))));
        assertEquals("[valor]", invalidas(new SolicitudRecarga("BIL-000001", null)));
        assertEquals("[]", invalidas(new SolicitudRecarga("BIL-000001", new BigDecimal("0.01"))));
        assertEquals("[]", invalidas(new SolicitudRetiro("BIL-000001", CIEN)));
        assertEquals("[billeteraOrigen, valor]", invalidas(new SolicitudRetiro("", BigDecimal.ZERO)));
        assertEquals("[El valor debe ser mayor que cero]", mensajes(new SolicitudRetiro("BIL-000001", BigDecimal.ZERO)));
    }

    @Test
    void transferencia() {
        assertEquals("[]", invalidas(new SolicitudTransferencia("BIL-000001", "BIL-000007", CIEN)));
        assertEquals("[billeterasDistintas]", invalidas(new SolicitudTransferencia("BIL-000001", "BIL-000001", CIEN)));
        // Si falta una billetera solo se informa ese campo, no además la regla de distintas.
        assertEquals("[billeteraDestino]", invalidas(new SolicitudTransferencia("BIL-000001", null, CIEN)));
        assertEquals("[billeteraOrigen, valor]", invalidas(new SolicitudTransferencia("XYZ", "BIL-000001", null)));
    }

    @Test
    void operacionProgramada() {
        assertEquals("[]", invalidas(new SolicitudOperacionProgramada(TipoOperacionProgramada.TRANSFERENCIA, CIEN,
                "BIL-000001", "BIL-000002", FECHA, Periodicidad.SEMANAL)));
        assertEquals("[]", invalidas(new SolicitudOperacionProgramada(TipoOperacionProgramada.RECARGA, CIEN,
                null, "BIL-000002", FECHA, Periodicidad.MENSUAL)));
        assertEquals("[]", invalidas(new SolicitudOperacionProgramada(TipoOperacionProgramada.RETIRO, CIEN,
                "BIL-000001", null, FECHA, Periodicidad.UNICA)));
        assertEquals("[billeterasCoherentesConElTipo]", invalidas(new SolicitudOperacionProgramada(
                TipoOperacionProgramada.RECARGA, CIEN, "BIL-000001", "BIL-000002", FECHA, Periodicidad.UNICA)));
        assertEquals("[billeterasCoherentesConElTipo]", invalidas(new SolicitudOperacionProgramada(
                TipoOperacionProgramada.TRANSFERENCIA, CIEN, "BIL-000001", null, FECHA, Periodicidad.UNICA)));
        assertEquals("[billeteraOrigen]", invalidas(new SolicitudOperacionProgramada(
                TipoOperacionProgramada.RETIRO, CIEN, "mal", null, FECHA, Periodicidad.UNICA)));
        assertEquals("[fechaEjecucion, periodicidad, tipo, valor]", invalidas(new SolicitudOperacionProgramada(
                null, null, null, "BIL-000002", null, null)));
    }
}
