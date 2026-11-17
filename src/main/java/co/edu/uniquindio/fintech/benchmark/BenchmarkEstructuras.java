package co.edu.uniquindio.fintech.benchmark;

import co.edu.uniquindio.fintech.exception.SolicitudInvalidaException;
import co.edu.uniquindio.fintech.structures.adaptadores.ColaPrioridadAdaptador;
import co.edu.uniquindio.fintech.structures.adaptadores.TablaHashAdaptador;
import co.edu.uniquindio.fintech.structures.interfaces.ArbolBinarioBusqueda;
import co.edu.uniquindio.fintech.structures.interfaces.ClaveCompuesta;
import co.edu.uniquindio.fintech.structures.interfaces.ColaPrioridad;
import co.edu.uniquindio.fintech.structures.interfaces.GrafoDirigidoPonderado;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import co.edu.uniquindio.fintech.structures.interfaces.TablaHash;
import co.edu.uniquindio.fintech.structures.propias.ArbolBinarioBusquedaImpl;
import co.edu.uniquindio.fintech.structures.propias.GrafoDirigidoPonderadoImpl;
import co.edu.uniquindio.fintech.structures.propias.ListaEnlazada;

import java.math.BigDecimal;

/**
 * Benchmark de las estructuras del proyecto con {@code System.nanoTime()}.
 *
 * <p>Método: para cada estructura se ejecuta 1 repetición de calentamiento (descartada) y
 * luego {@code repeticiones} mediciones; cada repetición regenera los mismos datos con la
 * misma semilla, así todas las estructuras procesan exactamente la misma entrada. Se
 * informa promedio, mínimo y máximo, y una verificación del resultado que debe coincidir
 * entre las estructuras comparadas.
 *
 * <p>Limitaciones: es un micro-benchmark sin JMH; el JIT, el recolector de basura y la
 * carga del equipo influyen en los tiempos, por lo que solo sirven para comparar órdenes de
 * magnitud en una misma ejecución, no como cifras absolutas. Se miden las
 * implementaciones tal como están, sin ajustes.
 */
public final class BenchmarkEstructuras {

    public static final int MINIMO_ELEMENTOS = 10;
    public static final int MAXIMO_ELEMENTOS = 5_000;
    public static final int MAXIMO_REPETICIONES = 20;
    private static final long SEMILLA = 20261115L;

    /** Operación que se mide; devuelve la verificación de su resultado. */
    @FunctionalInterface
    private interface Prueba {
        String ejecutar(int elementos);
    }

    public Lista<MedicionBenchmark> ejecutar(ComparacionBenchmark comparacion, int elementos, int repeticiones) {
        if (comparacion == null) {
            throw new SolicitudInvalidaException("PARAMETRO_BENCHMARK_INVALIDO", "Debe indicarse la comparación");
        }
        if (elementos < MINIMO_ELEMENTOS || elementos > MAXIMO_ELEMENTOS) {
            throw new SolicitudInvalidaException("PARAMETRO_BENCHMARK_INVALIDO", "Los elementos deben estar entre " + MINIMO_ELEMENTOS + " y "
                    + MAXIMO_ELEMENTOS);
        }
        if (repeticiones < 1 || repeticiones > MAXIMO_REPETICIONES) {
            throw new SolicitudInvalidaException("PARAMETRO_BENCHMARK_INVALIDO", "Las repeticiones deben estar entre 1 y " + MAXIMO_REPETICIONES);
        }
        Lista<MedicionBenchmark> mediciones = new ListaEnlazada<>();
        switch (comparacion) {
            case BUSQUEDA -> {
                mediciones.agregar(medir("buscar n presentes y n ausentes", "TablaHash (HashMap)", elementos,
                        repeticiones, this::busquedaTablaHash));
                mediciones.agregar(medir("buscar n presentes y n ausentes", "ABB propio", elementos, repeticiones,
                        this::busquedaArbol));
            }
            case CONSULTA_ORDENADA -> {
                mediciones.agregar(medir("insertar n y obtener ordenados", "ListaEnlazada ordenada", elementos,
                        repeticiones, this::ordenLista));
                mediciones.agregar(medir("insertar n y obtener ordenados", "ABB propio + inorden", elementos,
                        repeticiones, this::ordenArbol));
            }
            case PRIORIDAD -> {
                mediciones.agregar(medir("insertar n y extraer por prioridad", "ColaPrioridad (montículo)", elementos,
                        repeticiones, this::prioridadCola));
                mediciones.agregar(medir("insertar n y extraer por prioridad", "ListaEnlazada (mínimo lineal)",
                        elementos, repeticiones, this::prioridadLista));
            }
            case RECORRIDO_GRAFO -> {
                mediciones.agregar(medir("recorrer vértices alcanzables", "Grafo propio - BFS", elementos,
                        repeticiones, n -> recorrido(n, true)));
                mediciones.agregar(medir("recorrer vértices alcanzables", "Grafo propio - DFS", elementos,
                        repeticiones, n -> recorrido(n, false)));
            }
        }
        return mediciones;
    }

    private static MedicionBenchmark medir(String operacion, String estructura, int elementos, int repeticiones,
                                           Prueba prueba) {
        String resultado = prueba.ejecutar(elementos); // calentamiento, descartado
        long total = 0;
        long minimo = Long.MAX_VALUE;
        long maximo = 0;
        for (int i = 0; i < repeticiones; i++) {
            long inicio = System.nanoTime();
            String r = prueba.ejecutar(elementos);
            long duracion = System.nanoTime() - inicio;
            if (!r.equals(resultado)) {
                throw new IllegalStateException("Resultado no determinista en " + estructura);
            }
            total += duracion;
            minimo = Math.min(minimo, duracion);
            maximo = Math.max(maximo, duracion);
        }
        return new MedicionBenchmark(operacion, estructura, elementos, repeticiones, total / repeticiones, minimo,
                maximo, resultado);
    }

    /** Claves distintas en orden pseudoaleatorio (los múltiplos de 2 están presentes; los impares no). */
    private static int[] clavesBarajadas(int n) {
        int[] claves = new int[n];
        for (int i = 0; i < n; i++) {
            claves[i] = i * 2;
        }
        GeneradorPseudoaleatorio azar = new GeneradorPseudoaleatorio(SEMILLA);
        for (int i = n - 1; i > 0; i--) {
            int j = azar.siguiente(i + 1);
            int tmp = claves[i];
            claves[i] = claves[j];
            claves[j] = tmp;
        }
        return claves;
    }

    private static int[] valoresAleatorios(int n) {
        GeneradorPseudoaleatorio azar = new GeneradorPseudoaleatorio(SEMILLA);
        int[] valores = new int[n];
        for (int i = 0; i < n; i++) {
            valores[i] = azar.siguiente(n * 4);
        }
        return valores;
    }

    private String busquedaTablaHash(int n) {
        TablaHash<Integer, Integer> tabla = new TablaHashAdaptador<>();
        for (int clave : clavesBarajadas(n)) {
            tabla.poner(clave, clave);
        }
        int encontrados = 0;
        for (int i = 0; i < 2 * n; i++) {
            if (tabla.contieneClave(i)) {
                encontrados++;
            }
        }
        return "encontrados=" + encontrados;
    }

    private String busquedaArbol(int n) {
        ArbolBinarioBusqueda<Integer, Integer> arbol = new ArbolBinarioBusquedaImpl<>();
        for (int clave : clavesBarajadas(n)) {
            arbol.insertar(clave, clave);
        }
        int encontrados = 0;
        for (int i = 0; i < 2 * n; i++) {
            if (arbol.contiene(i)) {
                encontrados++;
            }
        }
        return "encontrados=" + encontrados;
    }

    private String ordenLista(int n) {
        Lista<Integer> lista = new ListaEnlazada<>();
        for (int valor : valoresAleatorios(n)) {
            int posicion = 0;
            for (Integer existente : lista) {
                if (existente > valor) {
                    break;
                }
                posicion++;
            }
            lista.agregarEn(posicion, valor);
        }
        return firma(lista);
    }

    private String ordenArbol(int n) {
        ArbolBinarioBusqueda<ClaveCompuesta<Integer, Integer>, Integer> arbol = new ArbolBinarioBusquedaImpl<>();
        int[] valores = valoresAleatorios(n);
        for (int i = 0; i < valores.length; i++) {
            arbol.insertar(ClaveCompuesta.de(valores[i], i), valores[i]); // la clave compuesta admite repetidos
        }
        return firma(arbol.inOrden());
    }

    private String prioridadCola(int n) {
        ColaPrioridad<Integer> cola = new ColaPrioridadAdaptador<>(Integer::compare);
        for (int valor : valoresAleatorios(n)) {
            cola.insertar(valor);
        }
        Lista<Integer> salida = new ListaEnlazada<>();
        while (!cola.estaVacia()) {
            salida.agregar(cola.extraer());
        }
        return firma(salida);
    }

    private String prioridadLista(int n) {
        Lista<Integer> lista = new ListaEnlazada<>();
        for (int valor : valoresAleatorios(n)) {
            lista.agregar(valor);
        }
        Lista<Integer> salida = new ListaEnlazada<>();
        while (!lista.estaVacia()) {
            int indiceMinimo = 0;
            int minimo = Integer.MAX_VALUE;
            int i = 0;
            for (Integer valor : lista) {
                if (valor < minimo) {
                    minimo = valor;
                    indiceMinimo = i;
                }
                i++;
            }
            salida.agregar(lista.eliminarEn(indiceMinimo));
        }
        return firma(salida);
    }

    private String recorrido(int n, boolean anchura) {
        GrafoDirigidoPonderado<Integer> grafo = new GrafoDirigidoPonderadoImpl<>();
        GeneradorPseudoaleatorio azar = new GeneradorPseudoaleatorio(SEMILLA);
        for (int v = 0; v < n; v++) {
            grafo.agregarVertice(v);
        }
        for (int i = 0; i < 3 * n; i++) {
            int origen = azar.siguiente(n);
            int destino = azar.siguiente(n);
            if (origen != destino) {
                grafo.agregarArista(origen, destino, BigDecimal.ONE);
            }
        }
        Lista<Integer> visitados = anchura ? grafo.recorridoAnchura(0) : grafo.recorridoProfundidad(0);
        return "visitados=" + visitados.tamanio();
    }

    /** Verificación de una secuencia: tamaño, extremos y suma ponderada por posición. */
    private static String firma(Lista<Integer> valores) {
        long suma = 0;
        long posicion = 1;
        for (Integer v : valores) {
            suma += v * posicion++;
        }
        String primero = valores.estaVacia() ? "-" : String.valueOf(valores.obtener(0));
        String ultimo = valores.estaVacia() ? "-" : String.valueOf(valores.obtener(valores.tamanio() - 1));
        return "n=" + valores.tamanio() + " primero=" + primero + " ultimo=" + ultimo + " firma=" + suma;
    }
}
