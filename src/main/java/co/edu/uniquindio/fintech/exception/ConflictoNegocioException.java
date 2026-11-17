package co.edu.uniquindio.fintech.exception;

/**
 * La solicitud es válida pero contradice el estado actual (HTTP 409). Ejemplos:
 * {@code SALDO_INSUFICIENTE}, {@code BILLETERA_INACTIVA}, {@code OPERACION_NO_REVERSIBLE}.
 */
public class ConflictoNegocioException extends FintechException {

    private static final long serialVersionUID = 1L;

    public static final String CODIGO_GENERAL = "CONFLICTO_NEGOCIO";

    public ConflictoNegocioException(String codigo, String mensaje) {
        super(codigo, mensaje, null);
    }

    public ConflictoNegocioException(String mensaje) {
        this(CODIGO_GENERAL, mensaje);
    }
}
