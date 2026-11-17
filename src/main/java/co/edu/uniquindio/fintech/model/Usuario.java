package co.edu.uniquindio.fintech.model;

/**
 * Usuario de la plataforma, propietario de una o varias billeteras.
 *
 * <p>La {@code identificacion} la proporciona el usuario al registrarse y es su clave
 * de búsqueda; no se genera otro identificador. Las billeteras y transacciones del
 * usuario no se guardan aquí: se relacionan por identificación en los repositorios.
 *
 * <p><b>Provisional, pendiente de confirmación:</b> los datos básicos son
 * {@code nombre} y {@code correo}; el enunciado solo dice "información básica".
 *
 * <p>Inmutable: un cambio de puntos o de datos produce una copia mediante los métodos
 * {@code con...}. Que {@code nivel} corresponda a {@code puntos} lo garantiza
 * {@code service} al asignarlos, no esta clase.
 *
 * @param identificacion   identificación del usuario (clave)
 * @param nombre           nombre completo
 * @param correo           correo electrónico
 * @param puntosAcumulados puntos de fidelización acumulados
 * @param nivel            nivel actual
 */
public record Usuario(String identificacion, String nombre, String correo, long puntosAcumulados,
                      NivelUsuario nivel) {

    public Usuario {
        Validaciones.texto(identificacion, "identificacion");
        Validaciones.texto(nombre, "nombre");
        Validaciones.texto(correo, "correo");
        Validaciones.requerido(nivel, "nivel");
    }

    /** @return copia con nombre y correo nuevos */
    public Usuario conDatosBasicos(String nuevoNombre, String nuevoCorreo) {
        return new Usuario(identificacion, nuevoNombre, nuevoCorreo, puntosAcumulados, nivel);
    }

    /** @return copia con los puntos y el nivel indicados (calculados por service) */
    public Usuario conPuntos(long nuevosPuntos, NivelUsuario nuevoNivel) {
        return new Usuario(identificacion, nombre, correo, nuevosPuntos, nuevoNivel);
    }
}
