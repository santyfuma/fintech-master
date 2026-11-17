package co.edu.uniquindio.fintech.structures.propias;

import co.edu.uniquindio.fintech.structures.adaptadores.ColaAdaptador;
import co.edu.uniquindio.fintech.structures.adaptadores.ColaPrioridadAdaptador;
import co.edu.uniquindio.fintech.structures.adaptadores.PilaAdaptador;
import co.edu.uniquindio.fintech.structures.adaptadores.TablaHashAdaptador;
import co.edu.uniquindio.fintech.structures.interfaces.Arista;
import co.edu.uniquindio.fintech.structures.interfaces.Cola;
import co.edu.uniquindio.fintech.structures.interfaces.ColaPrioridad;
import co.edu.uniquindio.fintech.structures.interfaces.GrafoDirigidoPonderado;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import co.edu.uniquindio.fintech.structures.interfaces.Pila;
import co.edu.uniquindio.fintech.structures.interfaces.TablaHash;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Objects;

/**
 * Grafo dirigido ponderado propio, representado con <b>listas de adyacencia</b>.
 *
 * <p>Representación: una {@link TablaHash} de vértice a su registro interno. Cada
 * registro guarda una {@link ListaEnlazada} de enlaces salientes y otra de enlaces
 * entrantes. Cada arista es un único objeto mutable compartido por la lista de
 * salida del origen y la de entrada del destino, de modo que cambiar su peso
 * actualiza ambas vistas. Se permiten lazos ({@code A -> A}).
 *
 * <p>Pesos: {@link BigDecimal} no nulos y no negativos (cero es válido); se comparan
 * con {@code compareTo}. Los vértices y pesos {@code null} lanzan
 * {@link NullPointerException}; los pesos negativos {@link IllegalArgumentException}.
 *
 * <p>Comportamiento con vértices inexistentes:
 * <ul>
 *   <li>{@code aristasSalientes}, {@code aristasEntrantes}, {@code gradoSalida},
 *       {@code gradoEntrada}, recorridos, {@code ciclosQueIncluyen} y {@code peso}:
 *       {@link IllegalArgumentException}, como indica el contrato.</li>
 *   <li>Consultas booleanas y de rutas ({@code contieneArista}, {@code eliminarArista},
 *       {@code existeRuta}, {@code rutaMenosSaltos}, {@code rutaMenorPeso},
 *       {@code pesoRutaMenor}): no hay arista ni ruta, así que devuelven
 *       {@code false}, lista vacía o {@code null} según el método.</li>
 * </ul>
 *
 * <p>Complejidad temporal (V = vértices, E = aristas, s(v) = grado de salida,
 * e(v) = grado de entrada). Las operaciones sobre la tabla hash de vértices son O(1)
 * en promedio y O(V) en el peor caso; las cifras de abajo usan el promedio.
 * <ul>
 *   <li>{@code agregarVertice}, {@code contieneVertice}, {@code cantidadVertices},
 *       {@code cantidadAristas}, {@code gradoSalida}, {@code gradoEntrada},
 *       {@code estaVacio}, {@code limpiar}: O(1).</li>
 *   <li>{@code agregarArista}, {@code incrementarPeso}, {@code contieneArista},
 *       {@code peso}: O(s(origen)) — búsqueda lineal en la lista de adyacencia.</li>
 *   <li>{@code eliminarArista}: O(s(origen) + e(destino)).</li>
 *   <li>{@code eliminarVertice}: O(E) en el peor caso (se quita cada arista incidente
 *       de la lista del otro extremo).</li>
 *   <li>{@code aristasSalientes}: O(s(v)); {@code aristasEntrantes}: O(e(v)).</li>
 *   <li>{@code vertices}: O(V); {@code aristas}: O(V + E).</li>
 *   <li>{@code recorridoAnchura}, {@code recorridoProfundidad}, {@code existeRuta},
 *       {@code rutaMenosSaltos}, {@code tieneCiclo}: O(V + E).</li>
 *   <li>{@code rutaMenorPeso}, {@code pesoRutaMenor} (Dijkstra con montículo binario y
 *       eliminación perezosa): O((V + E) log V), más el costo de sumar {@code BigDecimal}.</li>
 *   <li>{@code ciclosQueIncluyen(v, L)}: proporcional al número de caminos simples desde
 *       {@code v} con menos de L aristas; en el peor caso O(V^L) (exponencial en L),
 *       razón por la que el contrato exige la cota L.</li>
 * </ul>
 *
 * @param <V> tipo de los vértices; deben implementar {@code equals} y {@code hashCode}
 */
public class GrafoDirigidoPonderadoImpl<V> implements GrafoDirigidoPonderado<V> {

    /** Arista interna mutable, compartida por la lista de salida y la de entrada. */
    private static final class Enlace<V> {
        private final V origen;
        private final V destino;
        private BigDecimal peso;

        private Enlace(V origen, V destino, BigDecimal peso) {
            this.origen = origen;
            this.destino = destino;
            this.peso = peso;
        }

        private Arista<V> comoArista() {
            return new Arista<>(origen, destino, peso);
        }
    }

    /** Registro de adyacencia de un vértice. */
    private static final class Adyacencia<V> {
        private final Lista<Enlace<V>> salientes = new ListaEnlazada<>();
        private final Lista<Enlace<V>> entrantes = new ListaEnlazada<>();
    }

    /** Entrada de la cola de prioridad de Dijkstra. */
    private record Candidato<V>(V vertice, BigDecimal distancia) {
    }

    /** Marco de la búsqueda en profundidad iterativa de {@link #tieneCiclo()}. */
    private static final class Marco<V> {
        private final V vertice;
        private final Iterator<Enlace<V>> pendientes;

        private Marco(V vertice, Iterator<Enlace<V>> pendientes) {
            this.vertice = vertice;
            this.pendientes = pendientes;
        }
    }

    private static final Boolean VISITADO = Boolean.TRUE;
    private static final int EN_CURSO = 1;
    private static final int TERMINADO = 2;

    private final TablaHash<V, Adyacencia<V>> adyacencias = new TablaHashAdaptador<>();
    private int cantidadAristas;

    // ---------------------------------------------------------------- vértices

    @Override
    public boolean agregarVertice(V vertice) {
        Objects.requireNonNull(vertice, "vertice");
        if (adyacencias.contieneClave(vertice)) {
            return false;
        }
        adyacencias.poner(vertice, new Adyacencia<>());
        return true;
    }

    @Override
    public boolean eliminarVertice(V vertice) {
        Objects.requireNonNull(vertice, "vertice");
        Adyacencia<V> propia = adyacencias.eliminar(vertice);
        if (propia == null) {
            return false;
        }
        int eliminadas = 0;
        for (Enlace<V> enlace : propia.salientes) {
            if (!enlace.destino.equals(vertice)) {
                adyacencias.obtener(enlace.destino).entrantes.eliminar(enlace);
            }
            eliminadas++;
        }
        for (Enlace<V> enlace : propia.entrantes) {
            if (!enlace.origen.equals(vertice)) {
                adyacencias.obtener(enlace.origen).salientes.eliminar(enlace);
                eliminadas++;
            } // un lazo ya se contó entre las salientes
        }
        cantidadAristas -= eliminadas;
        return true;
    }

    @Override
    public boolean contieneVertice(V vertice) {
        return adyacencias.contieneClave(Objects.requireNonNull(vertice, "vertice"));
    }

    @Override
    public Lista<V> vertices() {
        return adyacencias.claves();
    }

    @Override
    public int cantidadVertices() {
        return adyacencias.tamanio();
    }

    // ---------------------------------------------------------------- aristas y pesos

    @Override
    public void agregarArista(V origen, V destino, BigDecimal peso) {
        validarExtremos(origen, destino);
        validarPeso(peso, "peso");
        Enlace<V> existente = buscarEnlace(origen, destino);
        if (existente != null) {
            existente.peso = peso;
        } else {
            crearEnlace(origen, destino, peso);
        }
    }

    @Override
    public void incrementarPeso(V origen, V destino, BigDecimal incremento) {
        validarExtremos(origen, destino);
        validarPeso(incremento, "incremento");
        Enlace<V> existente = buscarEnlace(origen, destino);
        if (existente != null) {
            existente.peso = existente.peso.add(incremento);
        } else {
            crearEnlace(origen, destino, incremento);
        }
    }

    @Override
    public boolean eliminarArista(V origen, V destino) {
        validarExtremos(origen, destino);
        Enlace<V> enlace = buscarEnlace(origen, destino);
        if (enlace == null) {
            return false;
        }
        adyacencias.obtener(origen).salientes.eliminar(enlace);
        adyacencias.obtener(destino).entrantes.eliminar(enlace);
        cantidadAristas--;
        return true;
    }

    @Override
    public boolean contieneArista(V origen, V destino) {
        validarExtremos(origen, destino);
        return buscarEnlace(origen, destino) != null;
    }

    @Override
    public BigDecimal peso(V origen, V destino) {
        validarExtremos(origen, destino);
        Enlace<V> enlace = buscarEnlace(origen, destino);
        if (enlace == null) {
            throw new IllegalArgumentException("No existe la arista " + origen + " -> " + destino);
        }
        return enlace.peso;
    }

    @Override
    public Lista<Arista<V>> aristas() {
        Lista<Arista<V>> resultado = new ListaEnlazada<>();
        for (V vertice : adyacencias.claves()) {
            for (Enlace<V> enlace : adyacencias.obtener(vertice).salientes) {
                resultado.agregar(enlace.comoArista());
            }
        }
        return resultado;
    }

    @Override
    public int cantidadAristas() {
        return cantidadAristas;
    }

    @Override
    public Lista<Arista<V>> aristasSalientes(V vertice) {
        Lista<Arista<V>> resultado = new ListaEnlazada<>();
        for (Enlace<V> enlace : adyacenciaExistente(vertice).salientes) {
            resultado.agregar(enlace.comoArista());
        }
        return resultado;
    }

    @Override
    public Lista<Arista<V>> aristasEntrantes(V vertice) {
        Lista<Arista<V>> resultado = new ListaEnlazada<>();
        for (Enlace<V> enlace : adyacenciaExistente(vertice).entrantes) {
            resultado.agregar(enlace.comoArista());
        }
        return resultado;
    }

    @Override
    public int gradoSalida(V vertice) {
        return adyacenciaExistente(vertice).salientes.tamanio();
    }

    @Override
    public int gradoEntrada(V vertice) {
        return adyacenciaExistente(vertice).entrantes.tamanio();
    }

    // ---------------------------------------------------------------- recorridos

    @Override
    public Lista<V> recorridoAnchura(V inicio) {
        adyacenciaExistente(inicio);
        Lista<V> visitados = new ListaEnlazada<>();
        TablaHash<V, Boolean> marcados = new TablaHashAdaptador<>();
        Cola<V> cola = new ColaAdaptador<>();
        marcados.poner(inicio, VISITADO);
        cola.encolar(inicio);
        while (!cola.estaVacia()) {
            V actual = cola.desencolar();
            visitados.agregar(actual);
            for (Enlace<V> enlace : adyacencias.obtener(actual).salientes) {
                if (!marcados.contieneClave(enlace.destino)) {
                    marcados.poner(enlace.destino, VISITADO);
                    cola.encolar(enlace.destino);
                }
            }
        }
        return visitados;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Iterativo con una {@link Pila}; produce el mismo orden que la versión recursiva
     * (los vecinos se exploran en el orden en que se agregaron sus aristas).
     */
    @Override
    public Lista<V> recorridoProfundidad(V inicio) {
        adyacenciaExistente(inicio);
        Lista<V> visitados = new ListaEnlazada<>();
        TablaHash<V, Boolean> marcados = new TablaHashAdaptador<>();
        Pila<V> pila = new PilaAdaptador<>();
        pila.apilar(inicio);
        while (!pila.estaVacia()) {
            V actual = pila.desapilar();
            if (marcados.contieneClave(actual)) {
                continue;
            }
            marcados.poner(actual, VISITADO);
            visitados.agregar(actual);
            // Se apilan los vecinos al revés para que el primero quede en la cima.
            Pila<V> invertidos = new PilaAdaptador<>();
            for (Enlace<V> enlace : adyacencias.obtener(actual).salientes) {
                if (!marcados.contieneClave(enlace.destino)) {
                    invertidos.apilar(enlace.destino);
                }
            }
            while (!invertidos.estaVacia()) {
                pila.apilar(invertidos.desapilar());
            }
        }
        return visitados;
    }

    // ---------------------------------------------------------------- rutas

    @Override
    public boolean existeRuta(V origen, V destino) {
        validarExtremos(origen, destino);
        return !rutaMenosSaltos(origen, destino).estaVacia();
    }

    @Override
    public Lista<V> rutaMenosSaltos(V origen, V destino) {
        validarExtremos(origen, destino);
        if (!adyacencias.contieneClave(origen) || !adyacencias.contieneClave(destino)) {
            return new ListaEnlazada<>();
        }
        TablaHash<V, V> previo = new TablaHashAdaptador<>();
        TablaHash<V, Boolean> marcados = new TablaHashAdaptador<>();
        Cola<V> cola = new ColaAdaptador<>();
        marcados.poner(origen, VISITADO);
        cola.encolar(origen);
        while (!cola.estaVacia()) {
            V actual = cola.desencolar();
            if (actual.equals(destino)) {
                return reconstruir(previo, origen, destino);
            }
            for (Enlace<V> enlace : adyacencias.obtener(actual).salientes) {
                if (!marcados.contieneClave(enlace.destino)) {
                    marcados.poner(enlace.destino, VISITADO);
                    previo.poner(enlace.destino, actual);
                    cola.encolar(enlace.destino);
                }
            }
        }
        return new ListaEnlazada<>();
    }

    @Override
    public Lista<V> rutaMenorPeso(V origen, V destino) {
        validarExtremos(origen, destino);
        TablaHash<V, V> previo = new TablaHashAdaptador<>();
        BigDecimal distancia = dijkstra(origen, destino, previo);
        if (distancia == null) {
            return new ListaEnlazada<>();
        }
        return reconstruir(previo, origen, destino);
    }

    @Override
    public BigDecimal pesoRutaMenor(V origen, V destino) {
        validarExtremos(origen, destino);
        return dijkstra(origen, destino, new TablaHashAdaptador<>());
    }

    // ---------------------------------------------------------------- ciclos

    @Override
    public boolean tieneCiclo() {
        TablaHash<V, Integer> estado = new TablaHashAdaptador<>();
        for (V inicial : adyacencias.claves()) {
            if (estado.contieneClave(inicial)) {
                continue;
            }
            Pila<Marco<V>> pila = new PilaAdaptador<>();
            estado.poner(inicial, EN_CURSO);
            pila.apilar(new Marco<>(inicial, adyacencias.obtener(inicial).salientes.iterator()));
            while (!pila.estaVacia()) {
                Marco<V> marco = pila.cima();
                if (marco.pendientes.hasNext()) {
                    V siguiente = marco.pendientes.next().destino;
                    Integer estadoSiguiente = estado.obtener(siguiente);
                    if (estadoSiguiente == null) {
                        estado.poner(siguiente, EN_CURSO);
                        pila.apilar(new Marco<>(siguiente, adyacencias.obtener(siguiente).salientes.iterator()));
                    } else if (estadoSiguiente == EN_CURSO) {
                        return true; // arista hacia un vértice del camino actual
                    }
                } else {
                    estado.poner(marco.vertice, TERMINADO);
                    pila.desapilar();
                }
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Búsqueda en profundidad desde {@code vertice} que nunca repite un vértice del
     * camino actual; cada vez que una arista vuelve a {@code vertice} se registra un
     * ciclo. Como todos los ciclos empiezan en {@code vertice} y hay una sola arista por
     * par ordenado, cada ciclo simple aparece exactamente una vez. Un lazo
     * {@code A -> A} es el ciclo {@code [A, A]} de longitud 1. La profundidad de la
     * recursión está acotada por {@code longitudMaxima}.
     */
    @Override
    public Lista<Lista<V>> ciclosQueIncluyen(V vertice, int longitudMaxima) {
        adyacenciaExistente(vertice);
        if (longitudMaxima < 1) {
            throw new IllegalArgumentException("La longitud máxima debe ser al menos 1: " + longitudMaxima);
        }
        Lista<Lista<V>> ciclos = new ListaEnlazada<>();
        Lista<V> camino = new ListaEnlazada<>();
        TablaHash<V, Boolean> enCamino = new TablaHashAdaptador<>();
        camino.agregar(vertice);
        enCamino.poner(vertice, VISITADO);
        buscarCiclos(vertice, vertice, longitudMaxima, camino, enCamino, ciclos);
        return ciclos;
    }

    // ---------------------------------------------------------------- general

    @Override
    public boolean estaVacio() {
        return adyacencias.estaVacia();
    }

    @Override
    public void limpiar() {
        adyacencias.limpiar();
        cantidadAristas = 0;
    }

    // ---------------------------------------------------------------- auxiliares

    private void validarExtremos(V origen, V destino) {
        Objects.requireNonNull(origen, "origen");
        Objects.requireNonNull(destino, "destino");
    }

    private static void validarPeso(BigDecimal peso, String nombre) {
        Objects.requireNonNull(peso, nombre);
        if (peso.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El " + nombre + " no puede ser negativo: " + peso);
        }
    }

    private Adyacencia<V> adyacenciaExistente(V vertice) {
        Objects.requireNonNull(vertice, "vertice");
        Adyacencia<V> adyacencia = adyacencias.obtener(vertice);
        if (adyacencia == null) {
            throw new IllegalArgumentException("No existe el vértice " + vertice);
        }
        return adyacencia;
    }

    /** @return el enlace origen -> destino, o null si alguno de los vértices o la arista no existen */
    private Enlace<V> buscarEnlace(V origen, V destino) {
        Adyacencia<V> adyacencia = adyacencias.obtener(origen);
        if (adyacencia == null || !adyacencias.contieneClave(destino)) {
            return null;
        }
        for (Enlace<V> enlace : adyacencia.salientes) {
            if (enlace.destino.equals(destino)) {
                return enlace;
            }
        }
        return null;
    }

    /** Precondición: los extremos y el peso ya fueron validados y la arista no existe. */
    private void crearEnlace(V origen, V destino, BigDecimal peso) {
        agregarVertice(origen);
        agregarVertice(destino);
        Enlace<V> enlace = new Enlace<>(origen, destino, peso);
        adyacencias.obtener(origen).salientes.agregar(enlace);
        adyacencias.obtener(destino).entrantes.agregar(enlace);
        cantidadAristas++;
    }

    /**
     * Dijkstra desde {@code origen} con parada temprana al fijar {@code destino}.
     * La cola de prioridad admite candidatos repetidos de un mismo vértice; se
     * descartan los que salen después de que el vértice quedó fijado.
     *
     * @param previo tabla que se llena con el predecesor de cada vértice alcanzado
     * @return distancia mínima, o null si no hay ruta o algún vértice no existe
     */
    private BigDecimal dijkstra(V origen, V destino, TablaHash<V, V> previo) {
        if (!adyacencias.contieneClave(origen) || !adyacencias.contieneClave(destino)) {
            return null;
        }
        TablaHash<V, BigDecimal> distancias = new TablaHashAdaptador<>();
        TablaHash<V, Boolean> fijados = new TablaHashAdaptador<>();
        ColaPrioridad<Candidato<V>> cola =
                new ColaPrioridadAdaptador<>((a, b) -> a.distancia().compareTo(b.distancia()));
        distancias.poner(origen, BigDecimal.ZERO);
        cola.insertar(new Candidato<>(origen, BigDecimal.ZERO));
        while (!cola.estaVacia()) {
            Candidato<V> candidato = cola.extraer();
            V actual = candidato.vertice();
            if (fijados.contieneClave(actual)) {
                continue;
            }
            fijados.poner(actual, VISITADO);
            if (actual.equals(destino)) {
                return candidato.distancia();
            }
            for (Enlace<V> enlace : adyacencias.obtener(actual).salientes) {
                if (fijados.contieneClave(enlace.destino)) {
                    continue;
                }
                BigDecimal nueva = candidato.distancia().add(enlace.peso);
                BigDecimal conocida = distancias.obtener(enlace.destino);
                if (conocida == null || nueva.compareTo(conocida) < 0) {
                    distancias.poner(enlace.destino, nueva);
                    previo.poner(enlace.destino, actual);
                    cola.insertar(new Candidato<>(enlace.destino, nueva));
                }
            }
        }
        return null;
    }

    /** Reconstruye la ruta origen..destino siguiendo los predecesores hacia atrás. */
    private Lista<V> reconstruir(TablaHash<V, V> previo, V origen, V destino) {
        Lista<V> ruta = new ListaEnlazada<>();
        V actual = destino;
        ruta.agregar(actual);
        while (!actual.equals(origen)) {
            actual = previo.obtener(actual);
            ruta.agregarEn(0, actual);
        }
        return ruta;
    }

    private void buscarCiclos(V inicio, V actual, int longitudMaxima, Lista<V> camino,
                              TablaHash<V, Boolean> enCamino, Lista<Lista<V>> ciclos) {
        // El camino tiene camino.tamanio() vértices y tamanio-1 aristas; cerrarlo suma una.
        for (Enlace<V> enlace : adyacencias.obtener(actual).salientes) {
            V siguiente = enlace.destino;
            if (siguiente.equals(inicio)) {
                if (camino.tamanio() <= longitudMaxima) {
                    Lista<V> ciclo = new ListaEnlazada<>();
                    for (V v : camino) {
                        ciclo.agregar(v);
                    }
                    ciclo.agregar(inicio);
                    ciclos.agregar(ciclo);
                }
            } else if (!enCamino.contieneClave(siguiente) && camino.tamanio() + 1 <= longitudMaxima) {
                camino.agregar(siguiente);
                enCamino.poner(siguiente, VISITADO);
                buscarCiclos(inicio, siguiente, longitudMaxima, camino, enCamino, ciclos);
                camino.eliminarEn(camino.tamanio() - 1);
                enCamino.eliminar(siguiente);
            }
        }
    }
}
