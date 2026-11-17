package co.edu.uniquindio.fintech.exception;

/** Un recurso solicitado no existe (HTTP 404). Ejemplo: {@code USUARIO_NO_ENCONTRADO}. */
public class RecursoNoEncontradoException extends FintechException {

    private static final long serialVersionUID = 1L;

    public static final String CODIGO_GENERAL = "RECURSO_NO_ENCONTRADO";

    public RecursoNoEncontradoException(String codigo, String mensaje) {
        super(codigo, mensaje, null);
    }

    public RecursoNoEncontradoException(String mensaje) {
        this(CODIGO_GENERAL, mensaje);
    }
}
