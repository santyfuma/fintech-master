package co.edu.uniquindio.fintech.exception;

import co.edu.uniquindio.fintech.dto.RespuestaError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Traduce las excepciones a respuestas HTTP con el formato {@link RespuestaError}.
 *
 * <ul>
 *   <li>{@link RecursoNoEncontradoException} → 404, {@link ConflictoNegocioException} → 409,
 *       {@link SolicitudInvalidaException} → 400, con su código y mensaje.</li>
 *   <li>Bean Validation (cuerpo con {@code @Valid}, parámetros validados y
 *       {@link ConstraintViolationException}) → 400 {@code VALIDACION_FALLIDA}, con los
 *       mensajes de cada campo.</li>
 *   <li>Excepciones estándar de Spring MVC (JSON mal formado, parámetro faltante o de
 *       tipo incorrecto, método no permitido, tipo de contenido no soportado, ruta
 *       inexistente…) → el código HTTP que les asigna Spring, gracias a
 *       {@link ResponseEntityExceptionHandler}, pero con este formato de cuerpo.</li>
 *   <li>{@link PersistenciaException} y cualquier otra excepción no prevista → 500 con un
 *       mensaje genérico; el detalle técnico solo va al log. Los errores de programación
 *       nunca se presentan como 400.</li>
 * </ul>
 *
 * <p>La fecha de cada error se toma del {@link Clock} inyectado.
 */
@RestControllerAdvice
public class ManejadorGlobalExcepciones extends ResponseEntityExceptionHandler {

    static final String VALIDACION_FALLIDA = "VALIDACION_FALLIDA";
    static final String SOLICITUD_MAL_FORMADA = "SOLICITUD_MAL_FORMADA";
    static final String PARAMETRO_INVALIDO = "PARAMETRO_INVALIDO";
    static final String PARAMETRO_FALTANTE = "PARAMETRO_FALTANTE";
    static final String ERROR_INTERNO = "ERROR_INTERNO";
    static final String MENSAJE_ERROR_INTERNO = "Ocurrió un error interno. Intente más tarde.";
    static final String MENSAJE_ERROR_PERSISTENCIA = "No fue posible acceder a los datos. Intente más tarde.";

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

    private final Clock reloj;

    public ManejadorGlobalExcepciones(Clock reloj) {
        if (reloj == null) {
            throw new NullPointerException("reloj");
        }
        this.reloj = reloj;
    }

    // ---------------------------------------------------------------- excepciones propias

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<RespuestaError> manejarRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        return responder(HttpStatus.NOT_FOUND, ex.codigo(), ex.getMessage());
    }

    @ExceptionHandler(ConflictoNegocioException.class)
    public ResponseEntity<RespuestaError> manejarConflictoNegocio(ConflictoNegocioException ex) {
        return responder(HttpStatus.CONFLICT, ex.codigo(), ex.getMessage());
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<RespuestaError> manejarSolicitudInvalida(SolicitudInvalidaException ex) {
        return responder(HttpStatus.BAD_REQUEST, ex.codigo(), ex.getMessage());
    }

    @ExceptionHandler(PersistenciaException.class)
    public ResponseEntity<RespuestaError> manejarPersistencia(PersistenciaException ex) {
        LOG.error("Error de persistencia: {}", ex.getMessage(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, ex.codigo(), MENSAJE_ERROR_PERSISTENCIA);
    }

    // ---------------------------------------------------------------- Bean Validation

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<RespuestaError> manejarViolacionRestricciones(ConstraintViolationException ex) {
        String[] partes = new String[ex.getConstraintViolations().size()];
        int cantidad = 0;
        for (ConstraintViolation<?> violacion : ex.getConstraintViolations()) {
            partes[cantidad++] = violacion.getPropertyPath() + ": " + violacion.getMessage();
        }
        return responder(HttpStatus.BAD_REQUEST, VALIDACION_FALLIDA, unir(partes, cantidad));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        RespuestaError cuerpo = error(HttpStatus.BAD_REQUEST, VALIDACION_FALLIDA,
                mensajeDeCampos(ex.getBindingResult()));
        return handleExceptionInternal(ex, cuerpo, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        String[] partes = new String[ex.getAllErrors().size()];
        int cantidad = 0;
        for (MessageSourceResolvable error : ex.getAllErrors()) {
            partes[cantidad++] = mensajeOGenerico(error.getDefaultMessage());
        }
        RespuestaError cuerpo = error(HttpStatus.BAD_REQUEST, VALIDACION_FALLIDA, unir(partes, cantidad));
        return handleExceptionInternal(ex, cuerpo, headers, HttpStatus.BAD_REQUEST, request);
    }

    // ---------------------------------------------------------------- demás excepciones de Spring MVC

    /**
     * Punto común de {@link ResponseEntityExceptionHandler}: conserva el código HTTP que
     * Spring asignó y reemplaza el cuerpo por {@link RespuestaError} con un mensaje sin
     * detalles técnicos.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        RespuestaError cuerpo = body instanceof RespuestaError propio ? propio : traducir(ex, statusCode);
        if (statusCode.is5xxServerError()) {
            LOG.error("Error interno en Spring MVC", ex);
        }
        return super.handleExceptionInternal(ex, cuerpo, headers, statusCode, request);
    }

    // ---------------------------------------------------------------- no previstas

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> manejarNoPrevista(Exception ex) {
        LOG.error("Error no previsto", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_INTERNO, MENSAJE_ERROR_INTERNO);
    }

    // ---------------------------------------------------------------- auxiliares

    private RespuestaError traducir(Exception ex, HttpStatusCode estado) {
        if (ex instanceof HttpMessageNotReadableException) {
            return error(estado, SOLICITUD_MAL_FORMADA,
                    "El cuerpo de la solicitud no es un JSON válido o contiene valores no permitidos");
        }
        if (ex instanceof MissingServletRequestParameterException faltante) {
            return error(estado, PARAMETRO_FALTANTE,
                    "Falta el parámetro obligatorio '" + faltante.getParameterName() + "'");
        }
        if (ex instanceof TypeMismatchException tipo) {
            String nombre = tipo.getPropertyName();
            return error(estado, PARAMETRO_INVALIDO, nombre == null
                    ? "Un parámetro tiene un valor no válido"
                    : "El parámetro '" + nombre + "' tiene un valor no válido");
        }
        return switch (estado.value()) {
            case 400 -> error(estado, "SOLICITUD_INVALIDA", "La solicitud no es válida");
            case 404 -> error(estado, RecursoNoEncontradoException.CODIGO_GENERAL, "No existe el recurso solicitado");
            case 405 -> error(estado, "METODO_NO_PERMITIDO", "Método HTTP no permitido para este recurso");
            case 406 -> error(estado, "FORMATO_NO_ACEPTABLE",
                    "No se puede generar la respuesta en el formato solicitado");
            case 415 -> error(estado, "TIPO_CONTENIDO_NO_SOPORTADO",
                    "Tipo de contenido no soportado; use application/json");
            default -> estado.is5xxServerError()
                    ? error(estado, ERROR_INTERNO, MENSAJE_ERROR_INTERNO)
                    : error(estado, "SOLICITUD_INVALIDA", "La solicitud no pudo procesarse");
        };
    }

    private ResponseEntity<RespuestaError> responder(HttpStatus estado, String codigo, String mensaje) {
        return ResponseEntity.status(estado).body(error(estado, codigo, mensaje));
    }

    private RespuestaError error(HttpStatusCode estado, String codigo, String mensaje) {
        return new RespuestaError(estado.value(), codigo, mensaje, LocalDateTime.now(reloj));
    }

    private static String mensajeDeCampos(BindingResult resultado) {
        String[] partes = new String[resultado.getErrorCount()];
        int cantidad = 0;
        for (FieldError error : resultado.getFieldErrors()) {
            partes[cantidad++] = error.getField() + ": " + mensajeOGenerico(error.getDefaultMessage());
        }
        for (ObjectError error : resultado.getGlobalErrors()) {
            partes[cantidad++] = mensajeOGenerico(error.getDefaultMessage());
        }
        return unir(partes, cantidad);
    }

    private static String mensajeOGenerico(String mensaje) {
        return mensaje == null || mensaje.isBlank() ? "valor no válido" : mensaje;
    }

    /** Ordena alfabéticamente (inserción; son pocos mensajes) y une con "; " para un orden estable. */
    private static String unir(String[] partes, int cantidad) {
        for (int i = 1; i < cantidad; i++) {
            String actual = partes[i];
            int j = i - 1;
            while (j >= 0 && partes[j].compareTo(actual) > 0) {
                partes[j + 1] = partes[j];
                j--;
            }
            partes[j + 1] = actual;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cantidad; i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(partes[i]);
        }
        return sb.length() == 0 ? "La solicitud no es válida" : sb.toString();
    }
}
