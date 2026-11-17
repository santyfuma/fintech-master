package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.dto.SolicitudActualizacionUsuario;
import co.edu.uniquindio.fintech.dto.SolicitudRegistroUsuario;
import co.edu.uniquindio.fintech.exception.ConflictoNegocioException;
import co.edu.uniquindio.fintech.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.fintech.exception.SolicitudInvalidaException;
import co.edu.uniquindio.fintech.model.NivelUsuario;
import co.edu.uniquindio.fintech.model.Usuario;
import co.edu.uniquindio.fintech.persistence.CerrojoEscritura;
import co.edu.uniquindio.fintech.repository.RepositorioAuditoria;
import co.edu.uniquindio.fintech.repository.RepositorioBeneficios;
import co.edu.uniquindio.fintech.repository.RepositorioBilleteras;
import co.edu.uniquindio.fintech.repository.RepositorioNotificaciones;
import co.edu.uniquindio.fintech.repository.RepositorioUsuarios;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;

/**
 * Usuarios (RF1): registro, consulta, actualización, eliminación y consultas por puntos
 * (ABB del repositorio).
 *
 * <p><b>Política de eliminación</b> (el enunciado no la define): solo se elimina un usuario
 * que no tiene ningún registro asociado (billeteras —de cualquier estado—, canjes,
 * notificaciones ni eventos de auditoría). Las billeteras nunca se borran, así que un
 * usuario que alguna vez tuvo una billetera se conserva; esto garantiza que no queden
 * referencias huérfanas. Si tiene registros, se responde 409 {@code USUARIO_CON_REGISTROS}.
 */
public final class UsuarioService {

    private final RepositorioUsuarios usuarios;
    private final RepositorioBilleteras billeteras;
    private final RepositorioBeneficios beneficios;
    private final RepositorioNotificaciones notificaciones;
    private final RepositorioAuditoria auditoria;
    private final PuntosService puntos;
    private final EscrituraConDiario escritura;
    private final CerrojoEscritura cerrojo;

    public UsuarioService(RepositorioUsuarios usuarios, RepositorioBilleteras billeteras,
                          RepositorioBeneficios beneficios, RepositorioNotificaciones notificaciones,
                          RepositorioAuditoria auditoria, PuntosService puntos, EscrituraConDiario escritura) {
        this.usuarios = usuarios;
        this.billeteras = billeteras;
        this.beneficios = beneficios;
        this.notificaciones = notificaciones;
        this.auditoria = auditoria;
        this.puntos = puntos;
        this.escritura = escritura;
        this.cerrojo = escritura.cerrojo();
    }

    public Usuario registrar(SolicitudRegistroUsuario solicitud) {
        synchronized (cerrojo) {
            escritura.asegurarSinPendientes();
            String id = solicitud.identificacion().trim();
            if (usuarios.existe(id)) {
                throw new ConflictoNegocioException("USUARIO_YA_EXISTE", "Ya existe un usuario con la identificación " + id);
            }
            return usuarios.registrar(new Usuario(id, solicitud.nombre().trim(), solicitud.correo().trim(), 0,
                    NivelUsuario.BRONCE));
        }
    }

    public Usuario buscar(String identificacion) {
        Usuario usuario = usuarios.buscar(identificacion);
        if (usuario == null) {
            throw new RecursoNoEncontradoException("USUARIO_NO_ENCONTRADO", "No existe el usuario " + identificacion);
        }
        return usuario;
    }

    public Lista<Usuario> listar() {
        return usuarios.todos();
    }

    public Usuario actualizar(String identificacion, SolicitudActualizacionUsuario solicitud) {
        synchronized (cerrojo) {
            escritura.asegurarSinPendientes();
            Usuario actualizado = buscar(identificacion).conDatosBasicos(solicitud.nombre().trim(),
                    solicitud.correo().trim());
            usuarios.actualizar(actualizado);
            return actualizado;
        }
    }

    public void eliminar(String identificacion) {
        synchronized (cerrojo) {
            escritura.asegurarSinPendientes();
            buscar(identificacion);
            int asociados = billeteras.porPropietario(identificacion).tamanio()
                    + beneficios.porUsuario(identificacion).tamanio()
                    + notificaciones.porUsuario(identificacion).tamanio()
                    + auditoria.porUsuario(identificacion).tamanio();
            if (asociados > 0) {
                throw new ConflictoNegocioException("USUARIO_CON_REGISTROS", "El usuario " + identificacion
                        + " tiene billeteras, canjes, notificaciones o eventos de auditoría y no puede eliminarse");
            }
            usuarios.eliminar(identificacion);
        }
    }

    public Lista<Usuario> ordenadosPorPuntos() {
        return usuarios.ordenadosPorPuntos();
    }

    public Lista<Usuario> porRangoDePuntos(long minimo, long maximo) {
        if (minimo < 0 || maximo < minimo) {
            throw new SolicitudInvalidaException("RANGO_INVALIDO", "El rango de puntos debe cumplir 0 ≤ mínimo ≤ máximo");
        }
        return usuarios.porRangoDePuntos(minimo, maximo);
    }

    public Lista<Usuario> porNivel(NivelUsuario nivel) {
        return usuarios.porRangoDePuntos(nivel.puntosMinimos(), nivel.puntosMaximos());
    }

    public Usuario conMayorPuntaje() {
        Lista<Usuario> primero = usuarios.conMasPuntos(1);
        if (primero.estaVacia()) {
            throw new RecursoNoEncontradoException("SIN_USUARIOS", "No hay usuarios registrados");
        }
        return primero.obtener(0);
    }

    /** @return el siguiente nivel del usuario, o null si ya está en PLATINO */
    public NivelUsuario siguienteNivel(Usuario usuario) {
        return puntos.siguienteNivel(usuario.nivel());
    }

    /** @return puntos que le faltan para el siguiente nivel, o null si ya está en el máximo */
    public Long puntosParaSiguienteNivel(Usuario usuario) {
        NivelUsuario siguiente = siguienteNivel(usuario);
        return siguiente == null ? null : siguiente.puntosMinimos() - usuario.puntosAcumulados();
    }
}
