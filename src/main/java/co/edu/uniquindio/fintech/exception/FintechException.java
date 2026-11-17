package co.edu.uniquindio.fintech.exception;

/**
 * Base de las excepciones propias de la aplicación.
 *
 * <p>Lleva un código de error estable (mayúsculas, dígitos y guiones bajos) que
 * identifica el caso concreto, y un mensaje para el cliente. No contiene reglas de
 * negocio: quien la lanza decide el código y el mensaje.
 */
public abstract class FintechException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private static final String FORMATO_CODIGO = "[A-Z][A-Z0-9_]*";

    private final String codigo;

    protected FintechException(String codigo, String mensaje, Throwable causa) {
        super(mensaje, causa);
        if (codigo == null || !codigo.matches(FORMATO_CODIGO)) {
            throw new IllegalArgumentException("Código de error inválido: " + codigo);
        }
        if (mensaje == null || mensaje.isBlank()) {
            throw new IllegalArgumentException("El mensaje de error es obligatorio");
        }
        this.codigo = codigo;
    }

    /** @return código del error, p. ej. {@code USUARIO_NO_ENCONTRADO} */
    public String codigo() {
        return codigo;
    }
}
