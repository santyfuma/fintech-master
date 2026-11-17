package co.edu.uniquindio.fintech.model;

/**
 * Comprobaciones estructurales usadas por los constructores de las entidades.
 *
 * <p>Solo verifican que un registro esté bien formado: campos obligatorios presentes,
 * identificadores con el formato documentado y billeteras coherentes con el tipo de
 * movimiento. No contienen reglas financieras (saldos, puntos, niveles, límites), que
 * pertenecen a {@code service}.
 *
 * <p>Se lanza {@link NullPointerException} por un campo obligatorio ausente e
 * {@link IllegalArgumentException} por un valor mal formado. Ambas indican un error de
 * programación o un dato persistido corrupto, no un error del cliente: la entrada del
 * cliente se valida antes en los DTO.
 */
final class Validaciones {

    private Validaciones() {
    }

    static <T> T requerido(T valor, String campo) {
        if (valor == null) {
            throw new NullPointerException("El campo '" + campo + "' es obligatorio");
        }
        return valor;
    }

    static String texto(String valor, String campo) {
        requerido(valor, campo);
        if (valor.isBlank()) {
            throw new IllegalArgumentException("El campo '" + campo + "' no puede estar vacío");
        }
        return valor;
    }

    static String identificador(String valor, String patron, String campo) {
        requerido(valor, campo);
        if (!valor.matches(patron)) {
            throw new IllegalArgumentException(
                    "El campo '" + campo + "' no tiene el formato " + patron + ": " + valor);
        }
        return valor;
    }

    /** Valida un identificador opcional: puede ser null, pero si existe debe tener formato. */
    static String identificadorOpcional(String valor, String patron, String campo) {
        return valor == null ? null : identificador(valor, patron, campo);
    }

    /** Verifica que las billeteras correspondan al tipo de movimiento ({@link TipoTransaccion#billeterasCoherentes}). */
    static void billeterasSegunTipo(TipoTransaccion tipo, String origen, String destino) {
        if (!tipo.billeterasCoherentes(origen, destino)) {
            throw new IllegalArgumentException("Billeteras no válidas para una operación de tipo " + tipo
                    + ": origen=" + origen + ", destino=" + destino);
        }
    }
}
