package co.edu.uniquindio.fintech.repository;

import co.edu.uniquindio.fintech.model.EventoAuditoria;
import co.edu.uniquindio.fintech.persistence.AlmacenJson;
import co.edu.uniquindio.fintech.persistence.CerrojoEscritura;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;

/**
 * Historial de auditoría: tabla hash por identificador, lista histórica en orden
 * cronológico ({@link #todos()}) e índice por usuario. La transacción relacionada, si
 * se indica, debe existir. Los eventos son registros
 * históricos: no se actualizan ni se eliminan.
 */
public final class RepositorioAuditoria extends RepositorioEnMemoria<EventoAuditoria> {

    private final RepositorioTransacciones transacciones;
    private final IndicePorClave porUsuario = new IndicePorClave(fabrica);

    public RepositorioAuditoria(AlmacenJson<EventoAuditoria> almacen, FabricaEstructuras fabrica,
                                CerrojoEscritura cerrojo, RepositorioTransacciones transacciones,
                                Lista<EventoAuditoria> iniciales) {
        super(almacen, fabrica, cerrojo);
        requerido(transacciones, "transacciones");
        this.transacciones = transacciones;
        cargarIniciales(iniciales);
    }

    /** La transacción relacionada, si la hay, debe existir (las transacciones no se borran). */
    @Override
    protected void verificar(EventoAuditoria evento) {
        if (evento.transaccion() != null && !transacciones.existe(evento.transaccion())) {
            throw new IllegalStateException("El evento " + evento.id() + " referencia la transacción inexistente "
                    + evento.transaccion());
        }
    }

    @Override
    protected String identificadorDe(EventoAuditoria evento) {
        return evento.id();
    }

    @Override
    protected void indexar(EventoAuditoria e) {
        porUsuario.agregar(e.identificacionUsuario(), e.id());
    }

    @Override
    protected void desindexar(EventoAuditoria e) {
        porUsuario.eliminar(e.identificacionUsuario(), e.id());
    }

    public EventoAuditoria registrar(EventoAuditoria evento) {
        return registrarRegistro(evento);
    }

    /** @return eventos del usuario, en orden cronológico */
    public Lista<EventoAuditoria> porUsuario(String identificacionUsuario) {
        requerido(identificacionUsuario, "identificacionUsuario");
        synchronized (cerrojo()) {
            return resolver(porUsuario.identificadores(identificacionUsuario));
        }
    }
}
