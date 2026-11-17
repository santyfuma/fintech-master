package co.edu.uniquindio.fintech.repository;

import co.edu.uniquindio.fintech.model.EstadoOperacionProgramada;
import co.edu.uniquindio.fintech.model.OperacionProgramada;
import co.edu.uniquindio.fintech.persistence.AlmacenJson;
import co.edu.uniquindio.fintech.persistence.CerrojoEscritura;
import co.edu.uniquindio.fintech.structures.interfaces.ColaPrioridad;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;

/**
 * Operaciones programadas por identificador, por usuario (propietario de la billetera
 * origen o, en recargas, de la destino) y en una cola de prioridad por fecha de
 * ejecución (empates por identificador) que contiene <b>solo</b> las PENDIENTES.
 *
 * <p>Al pasar a EJECUTADA, FALLIDA o CANCELADA la operación sale de la cola; al
 * reprogramarla (otra fecha) se reubica. Insertar y retirar de la cola cuestan
 * O(log n) y O(n) respectivamente; consultar la siguiente, O(1).
 */
public final class RepositorioOperacionesProgramadas extends RepositorioEnMemoria<OperacionProgramada> {

    private final RepositorioBilleteras billeteras;
    private final IndicePorClave porUsuario = new IndicePorClave(fabrica);
    private final ColaPrioridad<OperacionProgramada> pendientes = fabrica.nuevaColaPrioridad((a, b) -> {
        int porFecha = a.fechaEjecucion().compareTo(b.fechaEjecucion());
        return porFecha != 0 ? porFecha : IndicePorClave.compararConsecutivos(a.id(), b.id());
    });

    public RepositorioOperacionesProgramadas(AlmacenJson<OperacionProgramada> almacen, FabricaEstructuras fabrica,
                                             CerrojoEscritura cerrojo, RepositorioBilleteras billeteras,
                                             Lista<OperacionProgramada> iniciales) {
        super(almacen, fabrica, cerrojo);
        requerido(billeteras, "billeteras");
        this.billeteras = billeteras;
        cargarIniciales(iniciales);
    }

    @Override
    protected String identificadorDe(OperacionProgramada operacion) {
        return operacion.id();
    }

    @Override
    protected void verificar(OperacionProgramada o) {
        for (String codigo : new String[]{o.billeteraOrigen(), o.billeteraDestino()}) {
            if (codigo != null && !billeteras.existe(codigo)) {
                throw new IllegalStateException("La operación " + o.id() + " referencia la billetera inexistente "
                        + codigo);
            }
        }
    }

    @Override
    protected void indexar(OperacionProgramada o) {
        porUsuario.agregar(propietario(o), o.id());
        if (o.estado() == EstadoOperacionProgramada.PENDIENTE) {
            pendientes.insertar(o);
        }
    }

    @Override
    protected void desindexar(OperacionProgramada o) {
        porUsuario.eliminar(propietario(o), o.id());
        if (o.estado() == EstadoOperacionProgramada.PENDIENTE) {
            pendientes.eliminar(o);
        }
    }

    @Override
    protected void reindexar(OperacionProgramada anterior, OperacionProgramada nueva) {
        String propietarioAnterior = propietario(anterior);
        String propietarioNuevo = propietario(nueva);
        if (!propietarioAnterior.equals(propietarioNuevo)) {
            porUsuario.eliminar(propietarioAnterior, anterior.id());
            porUsuario.agregar(propietarioNuevo, nueva.id());
        }
        if (anterior.estado() == EstadoOperacionProgramada.PENDIENTE) {
            pendientes.eliminar(anterior);
        }
        if (nueva.estado() == EstadoOperacionProgramada.PENDIENTE) {
            pendientes.insertar(nueva);
        }
    }

    public OperacionProgramada registrar(OperacionProgramada operacion) {
        return registrarRegistro(operacion);
    }

    /** @return la versión anterior */
    public OperacionProgramada actualizar(OperacionProgramada operacion) {
        return actualizarRegistro(operacion);
    }

    /** @return la operación PENDIENTE con la fecha de ejecución más próxima, o null si no hay */
    public OperacionProgramada siguientePendiente() {
        synchronized (cerrojo()) {
            return pendientes.estaVacia() ? null : pendientes.consultar();
        }
    }

    public int cantidadPendientes() {
        synchronized (cerrojo()) {
            return pendientes.tamanio();
        }
    }

    /** @return operaciones del usuario (de cualquier estado), en orden de creación */
    public Lista<OperacionProgramada> porUsuario(String identificacionUsuario) {
        requerido(identificacionUsuario, "identificacionUsuario");
        synchronized (cerrojo()) {
            return resolver(porUsuario.identificadores(identificacionUsuario));
        }
    }

    private String propietario(OperacionProgramada o) {
        return billeteras.propietarioDe(o.billeteraOrigen() != null ? o.billeteraOrigen() : o.billeteraDestino());
    }
}
