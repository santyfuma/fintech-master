package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.exception.ConflictoNegocioException;
import co.edu.uniquindio.fintech.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.fintech.model.EstadoNotificacion;
import co.edu.uniquindio.fintech.model.Notificacion;
import co.edu.uniquindio.fintech.model.TipoNotificacion;
import co.edu.uniquindio.fintech.persistence.CerrojoEscritura;
import co.edu.uniquindio.fintech.persistence.GeneradorIdentificadores;
import co.edu.uniquindio.fintech.persistence.TipoIdentificador;
import co.edu.uniquindio.fintech.repository.FabricaEstructuras;
import co.edu.uniquindio.fintech.repository.RepositorioNotificaciones;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Notificaciones (enunciado 4.7): creación, consulta y despacho FIFO.
 *
 * <p>El despacho se <b>simula</b> cambiando el estado a DESPACHADA (no hay canal externo
 * en el enunciado). La cola del repositorio garantiza el orden de llegada y que una
 * notificación despachada no vuelva a salir.
 */
public final class NotificacionService {

    private final RepositorioNotificaciones notificaciones;
    private final GeneradorIdentificadores generador;
    private final EscrituraConDiario escritura;
    private final CerrojoEscritura cerrojo;
    private final FabricaEstructuras fabrica;
    private final Clock reloj;

    public NotificacionService(RepositorioNotificaciones notificaciones, GeneradorIdentificadores generador,
                               EscrituraConDiario escritura, FabricaEstructuras fabrica, Clock reloj) {
        this.notificaciones = notificaciones;
        this.generador = generador;
        this.escritura = escritura;
        this.cerrojo = escritura.cerrojo();
        this.fabrica = fabrica;
        this.reloj = reloj;
    }

    /**
     * Construye una notificación PENDIENTE con identificador nuevo, <b>sin guardarla</b>:
     * la usan las operaciones compuestas para incluirla en su entrada del diario. Debe
     * llamarse con el cerrojo tomado.
     */
    Notificacion nueva(String usuario, TipoNotificacion tipo, String mensaje) {
        return new Notificacion(generador.siguiente(TipoIdentificador.NOTIFICACION), usuario, tipo, mensaje,
                LocalDateTime.now(reloj), EstadoNotificacion.PENDIENTE);
    }

    /** Crea y guarda una notificación independiente. */
    public Notificacion crear(String usuario, TipoNotificacion tipo, String mensaje) {
        synchronized (cerrojo) {
            escritura.asegurarSinPendientes();
            Notificacion notificacion = nueva(usuario, tipo, mensaje);
            notificaciones.registrar(notificacion);
            return notificacion;
        }
    }

    public Notificacion buscar(String id) {
        Notificacion n = notificaciones.buscar(id);
        if (n == null) {
            throw new RecursoNoEncontradoException("NOTIFICACION_NO_ENCONTRADA", "No existe la notificación " + id);
        }
        return n;
    }

    public Lista<Notificacion> porUsuario(String usuario) {
        return notificaciones.porUsuario(usuario);
    }

    /** @return las más recientes primero, hasta el límite configurado (0 = todas) */
    public Lista<Notificacion> recientes(String usuario) {
        return notificaciones.recientes(usuario);
    }

    public Lista<Notificacion> pendientes() {
        return notificaciones.pendientes();
    }

    /** Despacha (simula el envío de) todas las pendientes en orden FIFO. */
    public Lista<Notificacion> despacharPendientes() {
        synchronized (cerrojo) {
            escritura.asegurarSinPendientes();
            Lista<Notificacion> despachadas = fabrica.nuevaLista();
            Notificacion siguiente = notificaciones.siguientePendiente();
            while (siguiente != null) {
                Notificacion despachada = siguiente.conEstado(EstadoNotificacion.DESPACHADA);
                notificaciones.actualizar(despachada);
                despachadas.agregar(despachada);
                siguiente = notificaciones.siguientePendiente();
            }
            return despachadas;
        }
    }

    /** Despacha una notificación concreta. */
    public Notificacion despachar(String id) {
        synchronized (cerrojo) {
            escritura.asegurarSinPendientes();
            Notificacion n = buscar(id);
            if (n.estado() == EstadoNotificacion.DESPACHADA) {
                throw new ConflictoNegocioException("NOTIFICACION_YA_DESPACHADA", "La notificación " + id
                        + " ya fue despachada");
            }
            Notificacion despachada = n.conEstado(EstadoNotificacion.DESPACHADA);
            notificaciones.actualizar(despachada);
            return despachada;
        }
    }

    /** @return si el usuario ya tiene una notificación de ese tipo cuyo mensaje menciona la referencia */
    boolean existeAviso(String usuario, TipoNotificacion tipo, String referencia) {
        for (Notificacion n : notificaciones.porUsuario(usuario)) {
            if (n.tipo() == tipo && n.mensaje().contains(referencia)) {
                return true;
            }
        }
        return false;
    }
}
