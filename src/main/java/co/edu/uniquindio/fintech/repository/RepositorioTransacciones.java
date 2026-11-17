package co.edu.uniquindio.fintech.repository;

import co.edu.uniquindio.fintech.model.EstadoTransaccion;
import co.edu.uniquindio.fintech.model.TipoTransaccion;
import co.edu.uniquindio.fintech.model.Transaccion;
import co.edu.uniquindio.fintech.persistence.AlmacenJson;
import co.edu.uniquindio.fintech.persistence.CerrojoEscritura;
import co.edu.uniquindio.fintech.structures.interfaces.ArbolBinarioBusqueda;
import co.edu.uniquindio.fintech.structures.interfaces.ClaveCompuesta;
import co.edu.uniquindio.fintech.structures.interfaces.GrafoDirigidoPonderado;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import co.edu.uniquindio.fintech.structures.interfaces.Pila;
import co.edu.uniquindio.fintech.structures.interfaces.TablaHash;

import java.math.BigDecimal;

/**
 * Transacciones por identificador, con:
 * <ul>
 *   <li>historial por billetera (origen y destino) y por usuario (propietarios de ambas
 *       billeteras; una transferencia entre dos usuarios aparece en los dos historiales,
 *       una interna una sola vez), en orden cronológico;</li>
 *   <li>ABB por valor con clave {@code (valor, id)} para valores repetidos;</li>
 *   <li>reconstrucción de la pila de reversibles con un {@link CriterioReversion} y del
 *       grafo de transferencias con un {@link CriterioPesoTransferencia} (ambas
 *       políticas pendientes; las define la capa de servicios).</li>
 * </ul>
 * Los índices guardan identificadores: un cambio de estado no los altera, y el historial
 * conserva su orden. Las transacciones no se eliminan.
 */
public final class RepositorioTransacciones extends RepositorioEnMemoria<Transaccion> {

    private final RepositorioBilleteras billeteras;
    private final IndicePorClave porUsuario = new IndicePorClave(fabrica);
    private final IndicePorClave porBilletera = new IndicePorClave(fabrica);
    private final ArbolBinarioBusqueda<ClaveCompuesta<BigDecimal, String>, String> porValor = fabrica.nuevoArbol();

    public RepositorioTransacciones(AlmacenJson<Transaccion> almacen, FabricaEstructuras fabrica,
                                    CerrojoEscritura cerrojo, RepositorioBilleteras billeteras,
                                    Lista<Transaccion> iniciales) {
        super(almacen, fabrica, cerrojo);
        requerido(billeteras, "billeteras");
        this.billeteras = billeteras;
        cargarIniciales(iniciales);
    }

    @Override
    protected String identificadorDe(Transaccion transaccion) {
        return transaccion.id();
    }

    @Override
    protected void verificar(Transaccion t) {
        verificarBilletera(t, t.billeteraOrigen());
        verificarBilletera(t, t.billeteraDestino());
        if (t.esReversion() && registroPorIdentificador(t.transaccionRevertida()) == null) {
            throw new IllegalStateException("La reversión " + t.id() + " referencia a la transacción inexistente "
                    + t.transaccionRevertida());
        }
    }

    private void verificarBilletera(Transaccion t, String codigo) {
        if (codigo != null && !billeteras.existe(codigo)) {
            throw new IllegalStateException("La transacción " + t.id() + " referencia la billetera inexistente " + codigo);
        }
    }

    @Override
    protected void indexar(Transaccion t) {
        if (t.billeteraOrigen() != null) {
            porBilletera.agregar(t.billeteraOrigen(), t.id());
        }
        if (t.billeteraDestino() != null) {
            porBilletera.agregar(t.billeteraDestino(), t.id());
        }
        String propietarioOrigen = propietario(t.billeteraOrigen());
        String propietarioDestino = propietario(t.billeteraDestino());
        if (propietarioOrigen != null) {
            porUsuario.agregar(propietarioOrigen, t.id());
        }
        if (propietarioDestino != null) {
            porUsuario.agregar(propietarioDestino, t.id()); // IndicePorClave ignora el repetido
        }
        porValor.insertar(ClaveCompuesta.de(t.valor(), t.id()), t.id());
    }

    @Override
    protected void desindexar(Transaccion t) {
        if (t.billeteraOrigen() != null) {
            porBilletera.eliminar(t.billeteraOrigen(), t.id());
            porUsuario.eliminar(propietario(t.billeteraOrigen()), t.id());
        }
        if (t.billeteraDestino() != null) {
            porBilletera.eliminar(t.billeteraDestino(), t.id());
            porUsuario.eliminar(propietario(t.billeteraDestino()), t.id());
        }
        porValor.eliminar(ClaveCompuesta.de(t.valor(), t.id()));
    }

    @Override
    protected void reindexar(Transaccion anterior, Transaccion nueva) {
        boolean mismosIndices = iguales(anterior.billeteraOrigen(), nueva.billeteraOrigen())
                && iguales(anterior.billeteraDestino(), nueva.billeteraDestino())
                && anterior.valor().compareTo(nueva.valor()) == 0;
        if (!mismosIndices) {
            desindexar(anterior);
            indexar(nueva);
        }
    }

    /** @throws IllegalStateException si ya existe o referencia billeteras o una original inexistentes */
    public Transaccion registrar(Transaccion transaccion) {
        return registrarRegistro(transaccion);
    }

    /**
     * @return la versión anterior
     * @throws IllegalStateException si la transacción no existe
     */
    public Transaccion actualizar(Transaccion transaccion) {
        return actualizarRegistro(transaccion);
    }

    /** @return transacciones en las que participa alguna billetera del usuario, en orden cronológico */
    public Lista<Transaccion> historialDeUsuario(String identificacionUsuario) {
        requerido(identificacionUsuario, "identificacionUsuario");
        synchronized (cerrojo()) {
            return resolver(porUsuario.identificadores(identificacionUsuario));
        }
    }

    /** @return transacciones con la billetera como origen o destino, en orden cronológico */
    public Lista<Transaccion> historialDeBilletera(String codigoBilletera) {
        requerido(codigoBilletera, "codigoBilletera");
        synchronized (cerrojo()) {
            return resolver(porBilletera.identificadores(codigoBilletera));
        }
    }

    /** @return todas las transacciones de menor a mayor valor (empates por id) */
    public Lista<Transaccion> ordenadasPorValor() {
        synchronized (cerrojo()) {
            return resolver(porValor.inOrden());
        }
    }

    /** @return las {@code cantidad} transacciones de mayor valor, de mayor a menor */
    public Lista<Transaccion> mayoresPorValor(int cantidad) {
        synchronized (cerrojo()) {
            return resolver(porValor.mayores(cantidad));
        }
    }

    /** @return transacciones con valor en {@code [desde, hasta]}, de menor a mayor */
    public Lista<Transaccion> porRangoDeValor(BigDecimal desde, BigDecimal hasta) {
        requerido(desde, "desde");
        requerido(hasta, "hasta");
        synchronized (cerrojo()) {
            return resolver(porValor.rango(ClaveCompuesta.cotaInferior(desde), ClaveCompuesta.cotaSuperior(hasta)));
        }
    }

    /**
     * Usuario que originó la transacción (CONTEXTO.md 7.5): el propietario de la billetera
     * origen o, si no hay (recarga), el de la destino.
     */
    public String usuarioQueOrigino(Transaccion transaccion) {
        requerido(transaccion, "transaccion");
        synchronized (cerrojo()) {
            return transaccion.billeteraOrigen() != null
                    ? propietario(transaccion.billeteraOrigen())
                    : propietario(transaccion.billeteraDestino());
        }
    }

    /**
     * Reconstruye la pila de operaciones reversibles de un usuario (CONTEXTO.md 7.5):
     * recorre su historial en orden y apila las transacciones que él originó, que no son
     * reversiones, están EXITOSAS y el criterio acepta. La cima es la más reciente.
     */
    public Pila<Transaccion> reconstruirPilaReversibles(String identificacionUsuario, CriterioReversion criterio) {
        requerido(identificacionUsuario, "identificacionUsuario");
        requerido(criterio, "criterio");
        synchronized (cerrojo()) {
            Pila<Transaccion> pila = fabrica.nuevaPila();
            for (Transaccion t : resolver(porUsuario.identificadores(identificacionUsuario))) {
                if (esCandidataAReversion(t) && identificacionUsuario.equals(usuarioQueOrigino(t))
                        && criterio.esReversible(t)) {
                    pila.apilar(t);
                }
            }
            return pila;
        }
    }

    /** Igual que {@link #reconstruirPilaReversibles} para todos los usuarios a la vez (una pila por usuario). */
    public TablaHash<String, Pila<Transaccion>> reconstruirPilasReversibles(CriterioReversion criterio) {
        requerido(criterio, "criterio");
        synchronized (cerrojo()) {
            TablaHash<String, Pila<Transaccion>> pilas = fabrica.nuevaTablaHash();
            for (Transaccion t : copiaEnOrden()) {
                if (esCandidataAReversion(t) && criterio.esReversible(t)) {
                    String usuario = usuarioQueOrigino(t);
                    Pila<Transaccion> pila = pilas.obtener(usuario);
                    if (pila == null) {
                        pila = fabrica.nuevaPila();
                        pilas.poner(usuario, pila);
                    }
                    pila.apilar(t);
                }
            }
            return pilas;
        }
    }

    /**
     * Construye el grafo dirigido de transferencias entre usuarios (vértices =
     * identificaciones). Cada TRANSFERENCIA suma a la arista origen → destino el peso
     * que devuelva el criterio; si devuelve null, se omite.
     */
    public GrafoDirigidoPonderado<String> construirGrafoTransferencias(CriterioPesoTransferencia criterio) {
        requerido(criterio, "criterio");
        synchronized (cerrojo()) {
            GrafoDirigidoPonderado<String> grafo = fabrica.nuevoGrafo();
            for (Transaccion t : copiaEnOrden()) {
                if (t.tipo() == TipoTransaccion.TRANSFERENCIA) {
                    String origen = propietario(t.billeteraOrigen());
                    String destino = propietario(t.billeteraDestino());
                    BigDecimal peso = criterio.peso(t, origen, destino);
                    if (peso != null) {
                        grafo.incrementarPeso(origen, destino, peso);
                    }
                }
            }
            return grafo;
        }
    }

    private static boolean esCandidataAReversion(Transaccion t) {
        return !t.esReversion() && t.estado() == EstadoTransaccion.EXITOSA;
    }

    private String propietario(String codigoBilletera) {
        return codigoBilletera == null ? null : billeteras.propietarioDe(codigoBilletera);
    }

    private static boolean iguales(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
