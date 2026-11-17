package co.edu.uniquindio.fintech.dto;

import co.edu.uniquindio.fintech.model.NivelUsuario;
import co.edu.uniquindio.fintech.model.Usuario;

/** Usuario con sus puntos y nivel (consultas de usuario y de puntos). */
public record UsuarioRespuesta(String identificacion, String nombre, String correo, long puntosAcumulados,
                               NivelUsuario nivel) {

    public static UsuarioRespuesta desde(Usuario usuario) {
        return new UsuarioRespuesta(usuario.identificacion(), usuario.nombre(), usuario.correo(),
                usuario.puntosAcumulados(), usuario.nivel());
    }
}
