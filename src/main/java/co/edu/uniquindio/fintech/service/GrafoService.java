package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.fintech.exception.SolicitudInvalidaException;
import co.edu.uniquindio.fintech.model.EstadoTransaccion;
import co.edu.uniquindio.fintech.repository.FabricaEstructuras;
import co.edu.uniquindio.fintech.repository.RepositorioTransacciones;
import co.edu.uniquindio.fintech.structures.interfaces.Arista;
import co.edu.uniquindio.fintech.structures.interfaces.ColaPrioridad;
import co.edu.uniquindio.fintech.structures.interfaces.GrafoDirigidoPonderado;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;

import java.math.BigDecimal;

/**
 * Grafo de transferencias entre usuarios (RF12; enunciado 4.8, 5.6 y requisito adicional
 * de ciclos y rutas frecuentes), construido con el repositorio sobre
 * {@code GrafoDirigidoPonderadoImpl}.
 *
 * <p><b>Política</b> (pendiente en el enunciado): vértices = usuarios; arista u → v si u
 * transfirió a v. Cuentan solo las transferencias EXITOSAS (no las rechazadas ni las
 * revertidas). Las transferencias entre billeteras del mismo usuario <b>no</b> forman
 * aristas: el grafo describe relaciones entre usuarios. El peso es la cantidad de
 * transferencias o el monto acumulado según {@link CriterioGrafo}; nunca se mezclan.
 */
public final class GrafoService {

    /** Rutas entre dos usuarios. */
    public record Rutas(Lista<String> menosSaltos, Lista<String> menorPeso, BigDecimal pesoMenor) {
    }

    private final RepositorioTransacciones transacciones;
    private final FabricaEstructuras fabrica;
    private final CriterioGrafo porDefecto;

    public GrafoService(RepositorioTransacciones transacciones, FabricaEstructuras fabrica, CriterioGrafo porDefecto) {
        this.transacciones = transacciones;
        this.fabrica = fabrica;
        this.porDefecto = porDefecto;
    }

    public CriterioGrafo criterio(CriterioGrafo solicitado) {
        return solicitado == null ? porDefecto : solicitado;
    }

    public GrafoDirigidoPonderado<String> grafo(CriterioGrafo criterio) {
        CriterioGrafo efectivo = criterio(criterio);
        return transacciones.construirGrafoTransferencias((t, origen, destino) -> {
            if (t.estado() != EstadoTransaccion.EXITOSA || origen.equals(destino)) {
                return null;
            }
            return efectivo == CriterioGrafo.CANTIDAD ? BigDecimal.ONE : t.valor();
        });
    }

    public Rutas rutas(String origen, String destino, CriterioGrafo criterio) {
        GrafoDirigidoPonderado<String> g = grafo(criterio);
        if (!g.contieneVertice(origen) || !g.contieneVertice(destino)) {
            throw new RecursoNoEncontradoException("USUARIO_SIN_TRANSFERENCIAS",
                    "Ambos usuarios deben participar en transferencias para buscar rutas");
        }
        return new Rutas(g.rutaMenosSaltos(origen, destino), g.rutaMenorPeso(origen, destino),
                g.pesoRutaMenor(origen, destino));
    }

    /** @return las relaciones (aristas) de mayor peso, de mayor a menor */
    public Lista<Arista<String>> relacionesFrecuentes(int cantidad, CriterioGrafo criterio) {
        if (cantidad < 1 || cantidad > AnaliticaService.LIMITE_CANTIDAD) {
            throw new SolicitudInvalidaException("CANTIDAD_INVALIDA", "La cantidad debe estar entre 1 y "
                    + AnaliticaService.LIMITE_CANTIDAD);
        }
        ColaPrioridad<Arista<String>> cola = fabrica.nuevaColaPrioridad((a, b) -> {
            int porPeso = b.peso().compareTo(a.peso());
            if (porPeso != 0) {
                return porPeso;
            }
            int porOrigen = a.origen().compareTo(b.origen());
            return porOrigen != 0 ? porOrigen : a.destino().compareTo(b.destino());
        });
        for (Arista<String> arista : grafo(criterio).aristas()) {
            cola.insertar(arista);
        }
        Lista<Arista<String>> resultado = fabrica.nuevaLista();
        while (!cola.estaVacia() && resultado.tamanio() < cantidad) {
            resultado.agregar(cola.extraer());
        }
        return resultado;
    }

    public boolean tieneCiclo() {
        return grafo(CriterioGrafo.CANTIDAD).tieneCiclo();
    }

    public Lista<Lista<String>> ciclosQueIncluyen(String usuario, int longitudMaxima) {
        if (longitudMaxima < 1 || longitudMaxima > 10) {
            throw new SolicitudInvalidaException("LONGITUD_INVALIDA", "La longitud máxima de ciclo debe estar entre 1 y 10");
        }
        GrafoDirigidoPonderado<String> g = grafo(CriterioGrafo.CANTIDAD);
        if (!g.contieneVertice(usuario)) {
            return fabrica.nuevaLista();
        }
        return g.ciclosQueIncluyen(usuario, longitudMaxima);
    }
}
