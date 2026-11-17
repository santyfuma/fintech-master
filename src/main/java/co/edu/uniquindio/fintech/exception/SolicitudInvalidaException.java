package co.edu.uniquindio.fintech.exception;

/**
 * La solicitud está mal formada o es incoherente y no lo detectó Bean Validation
 * (HTTP 400). Ejemplo: {@code FECHA_EJECUCION_PASADA}.
 */
public class SolicitudInvalidaException extends FintechException {

    private static final long serialVersionUID = 1L;

    public static final String CODIGO_GENERAL = "SOLICITUD_INVALIDA";

    public SolicitudInvalidaException(String codigo, String mensaje) {
        super(codigo, mensaje, null);
    }

    public SolicitudInvalidaException(String mensaje) {
        this(CODIGO_GENERAL, mensaje);
    }
}
