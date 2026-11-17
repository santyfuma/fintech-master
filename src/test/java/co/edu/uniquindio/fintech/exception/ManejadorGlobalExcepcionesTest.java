package co.edu.uniquindio.fintech.exception;

import co.edu.uniquindio.fintech.dto.RespuestaError;
import co.edu.uniquindio.fintech.dto.SolicitudTransferencia;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Correspondencia excepción → código HTTP y formato del cuerpo, invocando el manejador directamente. */
class ManejadorGlobalExcepcionesTest {

    /** 2026-11-15T23:00Z = 2026-11-15T18:00 en Bogotá. */
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-15T23:00:00Z"), ZoneId.of("America/Bogota"));
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 15, 18, 0);

    private final ManejadorGlobalExcepciones manejador = new ManejadorGlobalExcepciones(RELOJ);

    private static void verificar(ResponseEntity<RespuestaError> respuesta, int estado, String error, String mensaje) {
        assertEquals(estado, respuesta.getStatusCode().value());
        RespuestaError cuerpo = respuesta.getBody();
        assertEquals(estado, cuerpo.estado());
        assertEquals(error, cuerpo.error());
        assertEquals(mensaje, cuerpo.mensaje());
        assertEquals(AHORA, cuerpo.fecha());
    }

    @Test
    void recursoNoEncontradoEs404() {
        verificar(manejador.manejarRecursoNoEncontrado(
                        new RecursoNoEncontradoException("USUARIO_NO_ENCONTRADO", "No existe el usuario 123")),
                404, "USUARIO_NO_ENCONTRADO", "No existe el usuario 123");
    }

    @Test
    void conflictoDeNegocioEs409ConElFormatoDeLaApi() {
        verificar(manejador.manejarConflictoNegocio(new ConflictoNegocioException("SALDO_INSUFICIENTE",
                        "Saldo insuficiente para realizar la operación")),
                409, "SALDO_INSUFICIENTE", "Saldo insuficiente para realizar la operación");
    }

    @Test
    void solicitudInvalidaEs400() {
        verificar(manejador.manejarSolicitudInvalida(
                        new SolicitudInvalidaException("FECHA_EJECUCION_PASADA", "La fecha debe ser futura")),
                400, "FECHA_EJECUCION_PASADA", "La fecha debe ser futura");
    }

    @Test
    void persistenciaEs500SinDetallesTecnicos() {
        ResponseEntity<RespuestaError> respuesta = manejador.manejarPersistencia(new PersistenciaException(
                "No se pudo escribir /srv/datos/usuarios.json", new IllegalStateException("disco lleno")));
        verificar(respuesta, 500, "ERROR_PERSISTENCIA", ManejadorGlobalExcepciones.MENSAJE_ERROR_PERSISTENCIA);
        assertFalse(respuesta.getBody().mensaje().contains("usuarios.json"));
        assertFalse(respuesta.getBody().mensaje().contains("disco"));
    }

    @Test
    void lasExcepcionesDeProgramacionSon500YNo400() {
        Exception[] errores = {new IllegalArgumentException("detalle interno"), new NullPointerException("campo"),
                new IllegalStateException("estado"), new ArithmeticException("/ by zero")};
        for (Exception error : errores) {
            ResponseEntity<RespuestaError> respuesta = manejador.manejarNoPrevista(error);
            verificar(respuesta, 500, "ERROR_INTERNO", ManejadorGlobalExcepciones.MENSAJE_ERROR_INTERNO);
            assertFalse(respuesta.getBody().mensaje().contains(error.getMessage()));
        }
    }

    @Test
    void violacionesDeBeanValidationSon400ConLosMensajesOrdenados() {
        try (ValidatorFactory fabrica = Validation.buildDefaultValidatorFactory()) {
            SolicitudTransferencia invalida = new SolicitudTransferencia("BIL-000001", "BIL-000001", BigDecimal.ZERO);
            ConstraintViolationException ex =
                    new ConstraintViolationException(fabrica.getValidator().validate(invalida));
            ResponseEntity<RespuestaError> respuesta = manejador.manejarViolacionRestricciones(ex);
            verificar(respuesta, 400, "VALIDACION_FALLIDA",
                    "billeterasDistintas: La billetera origen y la destino deben ser distintas; "
                            + "valor: El valor debe ser mayor que cero");
        }
    }

    @Test
    void requiereUnReloj() {
        assertThrows(NullPointerException.class, () -> new ManejadorGlobalExcepciones(null));
    }

    @Test
    void laFechaSaleDelRelojInyectado() {
        Clock otro = Clock.fixed(Instant.parse("2030-01-01T05:00:00Z"), ZoneId.of("America/Bogota"));
        RespuestaError cuerpo = new ManejadorGlobalExcepciones(otro)
                .manejarConflictoNegocio(new ConflictoNegocioException("X", "m")).getBody();
        assertEquals(LocalDateTime.of(2030, 1, 1, 0, 0), cuerpo.fecha());
        assertTrue(cuerpo.fecha().isAfter(AHORA));
    }
}
