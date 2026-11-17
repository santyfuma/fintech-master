package co.edu.uniquindio.fintech.exception;

/**
 * Error al leer o escribir los archivos JSON (HTTP 500). El mensaje y la causa son
 * técnicos: se registran en el log y nunca se envían al cliente.
 */
public class PersistenciaException extends FintechException {

    private static final long serialVersionUID = 1L;

    public static final String CODIGO_GENERAL = "ERROR_PERSISTENCIA";

    public PersistenciaException(String mensaje, Throwable causa) {
        super(CODIGO_GENERAL, mensaje, causa);
    }

    public PersistenciaException(String mensaje) {
        this(mensaje, null);
    }
}
