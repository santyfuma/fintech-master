package co.edu.uniquindio.fintech.repository;

import co.edu.uniquindio.fintech.model.EstadoNotificacion;
import co.edu.uniquindio.fintech.model.Notificacion;
import co.edu.uniquindio.fintech.persistence.AlmacenJson;
import co.edu.uniquindio.fintech.persistence.CerrojoEscritura;
import co.edu.uniquindio.fintech.structures.interfaces.Cola;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import co.edu.uniquindio.fintech.structures.interfaces.TablaHash;

/**
 * Notificaciones por identificador, por usuario (orden cronológico) y en una cola FIFO
 * de pendientes de despacho.
 *
 * <p>La cola guarda identificadores. Como una {@link Cola} solo retira por el frente, una
 * notificación que deja de estar PENDIENTE se descarta de forma <b>perezosa</b> cuando
 * llega al frente; las consultas nunca la devuelven. Una tabla de los identificadores
 * encolados evita duplicados.
 *
 * <p>Notificaciones recientes: el límite por usuario es configurable
 * ({@code fintech.notificaciones.recientes-por-usuario}); 0 significa sin límite.
 * <b>Pendiente</b> fijar su valor (CONTEXTO.md, sección 11). El límite solo afecta a la
 * consulta: el historial completo se conserva en el archivo.
 */
public final class RepositorioNotificaciones extends RepositorioEnMemoria<Notificacion> {

    private final int limiteRecientes;
    private final IndicePorClave porUsuario = new IndicePorClave(fabrica);
    private final Cola<String> colaPendientes = fabrica.nuevaCola();
    private final TablaHash<String, Boolean> encoladas = fabrica.nuevaTablaHash();

    public RepositorioNotificaciones(AlmacenJson<Notificacion> almacen, FabricaEstructuras fabrica,
                                     CerrojoEscritura cerrojo, Lista<Notificacion> iniciales, int limiteRecientes) {
        super(almacen, fabrica, cerrojo);
        if (limiteRecientes < 0) {
            throw new IllegalArgumentException("El límite de notificaciones recientes no puede ser negativo");
        }
        this.limiteRecientes = limiteRecientes;
        cargarIniciales(iniciales);
    }

    @Override
    protected String identificadorDe(Notificacion notificacion) {
        return notificacion.id();
    }

    @Override
    protected void indexar(Notificacion n) {
        porUsuario.agregar(n.identificacionUsuario(), n.id());
        encolarSiPendiente(n);
    }

    @Override
    protected void desindexar(Notificacion n) {
        porUsuario.eliminar(n.identificacionUsuario(), n.id());
    }

    @Override
    protected void reindexar(Notificacion anterior, Notificacion nueva) {
        if (!anterior.identificacionUsuario().equals(nueva.identificacionUsuario())) {
            porUsuario.eliminar(anterior.identificacionUsuario(), anterior.id());
            porUsuario.agregar(nueva.identificacionUsuario(), nueva.id());
        }
        encolarSiPendiente(nueva);
    }

    public Notificacion registrar(Notificacion notificacion) {
        return registrarRegistro(notificacion);
    }

    /** Actualiza la notificación (p. ej. a DESPACHADA). @return la versión anterior */
    public Notificacion actualizar(Notificacion notificacion) {
        return actualizarRegistro(notificacion);
    }

    /** @return la notificación pendiente más antigua, o null si no hay */
    public Notificacion siguientePendiente() {
        synchronized (cerrojo()) {
            descartarNoPendientesDelFrente();
            return colaPendientes.estaVacia() ? null : registroPorIdentificador(colaPendientes.frente());
        }
    }

    /** @return notificaciones pendientes en orden de llegada (FIFO) */
    public Lista<Notificacion> pendientes() {
        synchronized (cerrojo()) {
            Lista<Notificacion> resultado = fabrica.nuevaLista();
            for (String id : colaPendientes) {
                Notificacion n = registroPorIdentificador(id);
                if (n != null && n.estado() == EstadoNotificacion.PENDIENTE) {
                    resultado.agregar(n);
                }
            }
            return resultado;
        }
    }

    /** @return todas las notificaciones del usuario, en orden cronológico */
    public Lista<Notificacion> porUsuario(String identificacionUsuario) {
        requerido(identificacionUsuario, "identificacionUsuario");
        synchronized (cerrojo()) {
            return resolver(porUsuario.identificadores(identificacionUsuario));
        }
    }

    /** @return notificaciones del usuario de la más reciente a la más antigua, hasta el límite configurado */
    public Lista<Notificacion> recientes(String identificacionUsuario) {
        requerido(identificacionUsuario, "identificacionUsuario");
        synchronized (cerrojo()) {
            Lista<Notificacion> invertidas = fabrica.nuevaLista();
            for (Notificacion n : resolver(porUsuario.identificadores(identificacionUsuario))) {
                invertidas.agregarEn(0, n);
            }
            while (limiteRecientes > 0 && invertidas.tamanio() > limiteRecientes) {
                invertidas.eliminarEn(invertidas.tamanio() - 1);
            }
            return invertidas;
        }
    }

    public int limiteRecientes() {
        return limiteRecientes;
    }

    private void encolarSiPendiente(Notificacion n) {
        if (n.estado() == EstadoNotificacion.PENDIENTE && !encoladas.contieneClave(n.id())) {
            colaPendientes.encolar(n.id());
            encoladas.poner(n.id(), Boolean.TRUE);
        }
    }

    private void descartarNoPendientesDelFrente() {
        while (!colaPendientes.estaVacia()) {
            Notificacion frente = registroPorIdentificador(colaPendientes.frente());
            if (frente != null && frente.estado() == EstadoNotificacion.PENDIENTE) {
                return;
            }
            encoladas.eliminar(colaPendientes.desencolar());
        }
    }
}
