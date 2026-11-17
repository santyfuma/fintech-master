package co.edu.uniquindio.fintech.structures.propias;

import co.edu.uniquindio.fintech.structures.interfaces.Arista;
import co.edu.uniquindio.fintech.structures.interfaces.GrafoDirigidoPonderado;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrafoDirigidoPonderadoImplTest {

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

    /** Compara pesos con compareTo (ignora la escala). */
    private static void assertPeso(String esperado, BigDecimal real) {
        assertTrue(real != null && bd(esperado).compareTo(real) == 0,
                "peso esperado " + esperado + " pero fue " + real);
    }

    private static String texto(Lista<?> lista) {
        StringBuilder sb = new StringBuilder("[");
        for (Object e : lista) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(e instanceof Lista<?> anidada ? texto(anidada) : e);
        }
        return sb.append(']').toString();
    }

    private static String textoAristas(Lista<Arista<String>> aristas) {
        StringBuilder sb = new StringBuilder("[");
        for (Arista<String> a : aristas) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(a.origen()).append("->").append(a.destino()).append(':')
                    .append(a.peso().stripTrailingZeros().toPlainString());
        }
        return sb.append(']').toString();
    }

    private static GrafoDirigidoPonderado<String> grafo(String... aristas) {
        GrafoDirigidoPonderado<String> g = new GrafoDirigidoPonderadoImpl<>();
        for (String a : aristas) {
            String[] partes = a.split("[>:]");
            g.agregarArista(partes[0], partes[1], bd(partes[2]));
        }
        return g;
    }

    /**
     * Ejemplo para rutas, calculado a mano:
     * A->B 4, A->C 1, C->B 2, B->D 1, C->D 5.
     * Menor peso A..D = A,C,B,D = 1+2+1 = 4 (A,B,D = 5; A,C,D = 6).
     * Menos saltos A..D = A,B,D (2 aristas; B se descubre antes que C).
     */
    private static GrafoDirigidoPonderado<String> grafoDeRutas() {
        return grafo("A>B:4", "A>C:1", "C>B:2", "B>D:1", "C>D:5");
    }

    // ------------------------------------------------------------------ vacío y un vértice

    @Test
    void grafoVacio() {
        GrafoDirigidoPonderado<String> g = new GrafoDirigidoPonderadoImpl<>();
        assertTrue(g.estaVacio());
        assertEquals(0, g.cantidadVertices());
        assertEquals(0, g.cantidadAristas());
        assertTrue(g.vertices().estaVacia());
        assertTrue(g.aristas().estaVacia());
        assertFalse(g.tieneCiclo());
        assertFalse(g.contieneVertice("A"));
        assertFalse(g.contieneArista("A", "B"));
        assertFalse(g.eliminarVertice("A"));
        assertFalse(g.eliminarArista("A", "B"));
        assertFalse(g.existeRuta("A", "B"));
        assertNull(g.pesoRutaMenor("A", "B"));
        assertThrows(IllegalArgumentException.class, () -> g.recorridoAnchura("A"));
    }

    @Test
    void grafoConUnSoloVertice() {
        GrafoDirigidoPonderado<String> g = new GrafoDirigidoPonderadoImpl<>();
        assertTrue(g.agregarVertice("A"));
        assertFalse(g.estaVacio());
        assertEquals(1, g.cantidadVertices());
        assertEquals(0, g.gradoSalida("A"));
        assertEquals(0, g.gradoEntrada("A"));
        assertEquals("[A]", texto(g.recorridoAnchura("A")));
        assertEquals("[A]", texto(g.recorridoProfundidad("A")));
        assertFalse(g.tieneCiclo());
        assertTrue(g.ciclosQueIncluyen("A", 5).estaVacia());
        assertEquals("[A]", texto(g.rutaMenosSaltos("A", "A")));
        assertEquals("[A]", texto(g.rutaMenorPeso("A", "A")));
        assertPeso("0", g.pesoRutaMenor("A", "A"));
    }

    // ------------------------------------------------------------------ vértices

    @Test
    void agregarVerticeRepetidoDevuelveFalse() {
        GrafoDirigidoPonderado<String> g = new GrafoDirigidoPonderadoImpl<>();
        assertTrue(g.agregarVertice("A"));
        assertFalse(g.agregarVertice("A"));
        assertFalse(g.agregarVertice(new String("A")));
        assertEquals(1, g.cantidadVertices());
    }

    @Test
    void verticesDevuelveTodos() {
        GrafoDirigidoPonderado<String> g = grafo("A>B:1", "C>D:1");
        g.agregarVertice("E");
        Lista<String> vertices = g.vertices();
        assertEquals(5, vertices.tamanio());
        for (String v : new String[]{"A", "B", "C", "D", "E"}) {
            assertTrue(vertices.contiene(v));
        }
    }

    @Test
    void eliminarVerticeQuitaSusAristasEntrantesYSalientes() {
        GrafoDirigidoPonderado<String> g = grafo("A>B:1", "B>C:2", "C>B:3", "D>B:4", "A>C:5");
        assertEquals(5, g.cantidadAristas());
        assertTrue(g.eliminarVertice("B"));
        assertFalse(g.contieneVertice("B"));
        assertEquals(3, g.cantidadVertices());
        assertEquals(1, g.cantidadAristas());
        assertEquals("[A->C:5]", textoAristas(g.aristas()));
        assertEquals(1, g.gradoSalida("A"));
        assertEquals(0, g.gradoSalida("C"));
        assertEquals(0, g.gradoSalida("D"));
        assertEquals(1, g.gradoEntrada("C"));
        assertFalse(g.contieneArista("A", "B"));
        assertFalse(g.eliminarVertice("B"));
    }

    @Test
    void eliminarVerticeConLazo() {
        GrafoDirigidoPonderado<String> g = grafo("A>A:1", "A>B:2", "C>A:3");
        assertEquals(3, g.cantidadAristas());
        assertEquals(2, g.gradoSalida("A"));
        assertEquals(2, g.gradoEntrada("A"));
        assertTrue(g.eliminarVertice("A"));
        assertEquals(0, g.cantidadAristas());
        assertEquals(0, g.gradoEntrada("B"));
        assertEquals(0, g.gradoSalida("C"));
        assertTrue(g.aristas().estaVacia());
    }

    // ------------------------------------------------------------------ aristas y pesos

    @Test
    void agregarAristaCreaLosVerticesQueNoExisten() {
        GrafoDirigidoPonderado<String> g = new GrafoDirigidoPonderadoImpl<>();
        g.agregarArista("A", "B", bd("10"));
        assertTrue(g.contieneVertice("A"));
        assertTrue(g.contieneVertice("B"));
        assertEquals(2, g.cantidadVertices());
        assertEquals(1, g.cantidadAristas());
        assertPeso("10", g.peso("A", "B"));
    }

    @Test
    void agregarAristaExistenteReemplazaElPeso() {
        GrafoDirigidoPonderado<String> g = grafo("A>B:10");
        g.agregarArista("A", "B", bd("3.5"));
        assertPeso("3.5", g.peso("A", "B"));
        assertEquals(1, g.cantidadAristas());
        assertEquals(1, g.gradoSalida("A"));
        assertEquals(1, g.gradoEntrada("B"));
        assertEquals("[A->B:3.5]", textoAristas(g.aristasEntrantes("B")));
    }

    @Test
    void incrementarPesoAcumulaOCreaLaArista() {
        GrafoDirigidoPonderado<String> g = new GrafoDirigidoPonderadoImpl<>();
        g.incrementarPeso("A", "B", bd("1.5"));
        assertPeso("1.5", g.peso("A", "B"));
        assertEquals(1, g.cantidadAristas());
        g.incrementarPeso("A", "B", bd("2.25"));
        g.incrementarPeso("A", "B", bd("0"));
        assertPeso("3.75", g.peso("A", "B"));
        assertEquals(1, g.cantidadAristas());
        // La lista de entrada del destino ve el mismo peso actualizado.
        assertEquals("[A->B:3.75]", textoAristas(g.aristasEntrantes("B")));
    }

    @Test
    void pesosConDistintaEscalaSeComparanPorValor() {
        GrafoDirigidoPonderado<String> g = grafo("A>B:2.50");
        assertEquals(0, g.peso("A", "B").compareTo(bd("2.5")));
    }

    @Test
    void elPesoCeroEsValido() {
        GrafoDirigidoPonderado<String> g = grafo("A>B:0");
        assertPeso("0", g.peso("A", "B"));
        g.incrementarPeso("B", "C", BigDecimal.ZERO);
        assertTrue(g.contieneArista("B", "C"));
    }

    @Test
    void eliminarArista() {
        GrafoDirigidoPonderado<String> g = grafo("A>B:1", "A>C:2", "C>B:3");
        assertTrue(g.eliminarArista("A", "B"));
        assertFalse(g.contieneArista("A", "B"));
        assertFalse(g.eliminarArista("A", "B"));
        assertFalse(g.eliminarArista("A", "Z"));
        assertFalse(g.eliminarArista("Z", "A"));
        assertEquals(2, g.cantidadAristas());
        assertEquals(1, g.gradoSalida("A"));
        assertEquals(1, g.gradoEntrada("B"));
        assertTrue(g.contieneVertice("B"));
        assertThrows(IllegalArgumentException.class, () -> g.peso("A", "B"));
    }

    @Test
    void pesoDeAristaInexistenteLanzaIllegalArgument() {
        GrafoDirigidoPonderado<String> g = grafo("A>B:1");
        assertThrows(IllegalArgumentException.class, () -> g.peso("B", "A"));
        assertThrows(IllegalArgumentException.class, () -> g.peso("A", "Z"));
        assertThrows(IllegalArgumentException.class, () -> g.peso("Z", "A"));
    }

    @Test
    void elGrafoEsDirigido() {
        GrafoDirigidoPonderado<String> g = grafo("A>B:1");
        assertTrue(g.contieneArista("A", "B"));
        assertFalse(g.contieneArista("B", "A"));
        assertTrue(g.existeRuta("A", "B"));
        assertFalse(g.existeRuta("B", "A"));
        g.agregarArista("B", "A", bd("7"));
        assertEquals(2, g.cantidadAristas());
        assertPeso("1", g.peso("A", "B"));
        assertPeso("7", g.peso("B", "A"));
    }

    @Test
    void gradosYAristasEntrantesYSalientes() {
        GrafoDirigidoPonderado<String> g = grafo("A>B:1", "A>C:2", "B>C:3", "D>C:4");
        assertEquals(2, g.gradoSalida("A"));
        assertEquals(0, g.gradoEntrada("A"));
        assertEquals(1, g.gradoSalida("B"));
        assertEquals(1, g.gradoEntrada("B"));
        assertEquals(0, g.gradoSalida("C"));
        assertEquals(3, g.gradoEntrada("C"));
        assertEquals("[A->B:1, A->C:2]", textoAristas(g.aristasSalientes("A")));
        assertEquals("[A->C:2, B->C:3, D->C:4]", textoAristas(g.aristasEntrantes("C")));
        assertTrue(g.aristasSalientes("C").estaVacia());
        assertEquals(4, g.aristas().tamanio());
        assertEquals(4, g.cantidadAristas());
    }

    @Test
    void lasAristasDevueltasSonCopias() {
        GrafoDirigidoPonderado<String> g = grafo("A>B:1");
        Lista<Arista<String>> salientes = g.aristasSalientes("A");
        salientes.limpiar();
        assertEquals(1, g.gradoSalida("A"));
        Arista<String> antes = g.aristasSalientes("A").obtener(0);
        g.agregarArista("A", "B", bd("9"));
        assertPeso("1", antes.peso());
    }

    @Test
    void consultasDeAdyacenciaSobreVerticeInexistente() {
        GrafoDirigidoPonderado<String> g = grafo("A>B:1");
        assertThrows(IllegalArgumentException.class, () -> g.aristasSalientes("Z"));
        assertThrows(IllegalArgumentException.class, () -> g.aristasEntrantes("Z"));
        assertThrows(IllegalArgumentException.class, () -> g.gradoSalida("Z"));
        assertThrows(IllegalArgumentException.class, () -> g.gradoEntrada("Z"));
        assertThrows(IllegalArgumentException.class, () -> g.recorridoAnchura("Z"));
        assertThrows(IllegalArgumentException.class, () -> g.recorridoProfundidad("Z"));
        assertThrows(IllegalArgumentException.class, () -> g.ciclosQueIncluyen("Z", 3));
    }

    // ------------------------------------------------------------------ validaciones

    @Test
    void pesosNegativosSeRechazanSinModificarElGrafo() {
        GrafoDirigidoPonderado<String> g = new GrafoDirigidoPonderadoImpl<>();
        assertThrows(IllegalArgumentException.class, () -> g.agregarArista("A", "B", bd("-1")));
        assertThrows(IllegalArgumentException.class, () -> g.incrementarPeso("A", "B", bd("-0.01")));
        assertTrue(g.estaVacio());

        GrafoDirigidoPonderado<String> conArista = grafo("A>B:5");
        assertThrows(IllegalArgumentException.class, () -> conArista.agregarArista("A", "B", bd("-5")));
        assertThrows(IllegalArgumentException.class, () -> conArista.incrementarPeso("A", "B", bd("-1")));
        assertPeso("5", conArista.peso("A", "B"));
    }

    @Test
    void pesosNulosSeRechazan() {
        GrafoDirigidoPonderado<String> g = new GrafoDirigidoPonderadoImpl<>();
        assertThrows(NullPointerException.class, () -> g.agregarArista("A", "B", null));
        assertThrows(NullPointerException.class, () -> g.incrementarPeso("A", "B", null));
        assertTrue(g.estaVacio());
    }

    @Test
    void verticesYExtremosNulosSeRechazan() {
        GrafoDirigidoPonderado<String> g = grafo("A>B:1");
        assertThrows(NullPointerException.class, () -> g.agregarVertice(null));
        assertThrows(NullPointerException.class, () -> g.eliminarVertice(null));
        assertThrows(NullPointerException.class, () -> g.contieneVertice(null));
        assertThrows(NullPointerException.class, () -> g.agregarArista(null, "B", bd("1")));
        assertThrows(NullPointerException.class, () -> g.agregarArista("A", null, bd("1")));
        assertThrows(NullPointerException.class, () -> g.incrementarPeso(null, "B", bd("1")));
        assertThrows(NullPointerException.class, () -> g.eliminarArista("A", null));
        assertThrows(NullPointerException.class, () -> g.contieneArista(null, "B"));
        assertThrows(NullPointerException.class, () -> g.peso("A", null));
        assertThrows(NullPointerException.class, () -> g.aristasSalientes(null));
        assertThrows(NullPointerException.class, () -> g.gradoEntrada(null));
        assertThrows(NullPointerException.class, () -> g.recorridoAnchura(null));
        assertThrows(NullPointerException.class, () -> g.recorridoProfundidad(null));
        assertThrows(NullPointerException.class, () -> g.existeRuta(null, "B"));
        assertThrows(NullPointerException.class, () -> g.rutaMenosSaltos("A", null));
        assertThrows(NullPointerException.class, () -> g.rutaMenorPeso(null, "B"));
        assertThrows(NullPointerException.class, () -> g.pesoRutaMenor("A", null));
        assertThrows(NullPointerException.class, () -> g.ciclosQueIncluyen(null, 3));
        assertEquals(1, g.cantidadAristas());
    }

    // ------------------------------------------------------------------ recorridos

    /**
     * A->B, A->C, B->D, C->E, D->F, E->F, F->A, y G aislado.
     * BFS(A) = A, B, C, D, E, F. DFS(A) = A, B, D, F, C, E.
     */
    private static GrafoDirigidoPonderado<String> grafoRamificado() {
        GrafoDirigidoPonderado<String> g = grafo("A>B:1", "A>C:1", "B>D:1", "C>E:1", "D>F:1", "E>F:1", "F>A:1");
        g.agregarVertice("G");
        return g;
    }

    @Test
    void recorridoEnAnchura() {
        GrafoDirigidoPonderado<String> g = grafoRamificado();
        assertEquals("[A, B, C, D, E, F]", texto(g.recorridoAnchura("A")));
        assertEquals("[D, F, A, B, C, E]", texto(g.recorridoAnchura("D")));
        assertEquals("[G]", texto(g.recorridoAnchura("G")));
    }

    @Test
    void recorridoEnProfundidad() {
        GrafoDirigidoPonderado<String> g = grafoRamificado();
        assertEquals("[A, B, D, F, C, E]", texto(g.recorridoProfundidad("A")));
        assertEquals("[C, E, F, A, B, D]", texto(g.recorridoProfundidad("C")));
        assertEquals("[G]", texto(g.recorridoProfundidad("G")));
    }

    @Test
    void profundidadConVecinoAlcanzadoPorDosCaminosCoincideConLaVersionRecursiva() {
        // Recursivo: A, B, C, D (C se alcanza primero desde B, no desde A).
        GrafoDirigidoPonderado<String> g = grafo("A>B:1", "A>C:1", "B>C:1", "C>D:1");
        assertEquals("[A, B, C, D]", texto(g.recorridoProfundidad("A")));
        assertEquals("[A, B, C, D]", texto(g.recorridoAnchura("A")));
    }

    // ------------------------------------------------------------------ rutas

    @Test
    void rutaConMenosSaltos() {
        GrafoDirigidoPonderado<String> g = grafoDeRutas();
        assertEquals("[A, B, D]", texto(g.rutaMenosSaltos("A", "D")));
        assertEquals("[A, C]", texto(g.rutaMenosSaltos("A", "C")));
        // C->D existe directamente (1 arista), aunque C,B,D pese menos.
        assertEquals("[C, D]", texto(g.rutaMenosSaltos("C", "D")));
        assertEquals("[]", texto(g.rutaMenosSaltos("D", "A")));
    }

    @Test
    void dijkstraEligeElMenorPesoTotalYNoLaMenorCantidadDeAristas() {
        GrafoDirigidoPonderado<String> g = grafoDeRutas();
        assertEquals("[A, C, B, D]", texto(g.rutaMenorPeso("A", "D")));
        assertPeso("4", g.pesoRutaMenor("A", "D"));
        assertEquals("[A, B, D]", texto(g.rutaMenosSaltos("A", "D")));
        assertEquals("[A, C, B]", texto(g.rutaMenorPeso("A", "B")));
        assertPeso("3", g.pesoRutaMenor("A", "B"));
        assertEquals("[C, B, D]", texto(g.rutaMenorPeso("C", "D")));
        assertPeso("3", g.pesoRutaMenor("C", "D"));
    }

    /**
     * S->A 2, S->B 5, A->B 1, A->C 7, B->C 2, B->T 6, C->T 1.
     * Distancias: S=0, A=2, B=3 (S,A,B), C=5 (S,A,B,C), T=6 (S,A,B,C,T).
     */
    @Test
    void dijkstraConVariasRutasPosibles() {
        GrafoDirigidoPonderado<String> g = grafo("S>A:2", "S>B:5", "A>B:1", "A>C:7", "B>C:2", "B>T:6", "C>T:1");
        assertEquals("[S, A, B, C, T]", texto(g.rutaMenorPeso("S", "T")));
        assertPeso("6", g.pesoRutaMenor("S", "T"));
        assertEquals("[S, A, B]", texto(g.rutaMenorPeso("S", "B")));
        assertPeso("3", g.pesoRutaMenor("S", "B"));
        assertPeso("5", g.pesoRutaMenor("S", "C"));
        assertPeso("2", g.pesoRutaMenor("S", "A"));
        assertEquals("[S, B, T]", texto(g.rutaMenosSaltos("S", "T")));
    }

    @Test
    void dijkstraConPesosDecimales() {
        // X,Y,Z = 0.10 + 0.20 = 0.30 < X,Z = 0.35
        GrafoDirigidoPonderado<String> g = grafo("X>Y:0.10", "Y>Z:0.20", "X>Z:0.35");
        assertEquals("[X, Y, Z]", texto(g.rutaMenorPeso("X", "Z")));
        assertPeso("0.3", g.pesoRutaMenor("X", "Z"));
    }

    @Test
    void dijkstraConPesosCero() {
        GrafoDirigidoPonderado<String> g = grafo("A>B:0", "B>C:0", "A>C:0.01");
        assertEquals("[A, B, C]", texto(g.rutaMenorPeso("A", "C")));
        assertPeso("0", g.pesoRutaMenor("A", "C"));
        assertEquals("[A, C]", texto(g.rutaMenosSaltos("A", "C")));
    }

    @Test
    void rutasInexistentesYGrafoDesconectado() {
        GrafoDirigidoPonderado<String> g = grafoDeRutas();
        g.agregarArista("E", "F", bd("1"));
        g.agregarVertice("G");
        assertFalse(g.existeRuta("A", "E"));
        assertFalse(g.existeRuta("D", "A"));
        assertFalse(g.existeRuta("A", "G"));
        assertTrue(g.rutaMenosSaltos("A", "F").estaVacia());
        assertTrue(g.rutaMenorPeso("A", "F").estaVacia());
        assertNull(g.pesoRutaMenor("A", "F"));
        assertNull(g.pesoRutaMenor("D", "A"));
        assertTrue(g.existeRuta("E", "F"));
        assertEquals("[A, B, C, D]", texto(g.recorridoAnchura("A")));
    }

    @Test
    void rutasConVerticesInexistentesNoLanzanExcepcion() {
        GrafoDirigidoPonderado<String> g = grafoDeRutas();
        assertFalse(g.existeRuta("A", "Z"));
        assertFalse(g.existeRuta("Z", "A"));
        assertTrue(g.rutaMenosSaltos("Z", "A").estaVacia());
        assertTrue(g.rutaMenorPeso("A", "Z").estaVacia());
        assertNull(g.pesoRutaMenor("Z", "Z"));
        assertFalse(g.existeRuta("Z", "Z"));
    }

    @Test
    void origenIgualADestino() {
        GrafoDirigidoPonderado<String> g = grafoDeRutas();
        assertTrue(g.existeRuta("A", "A"));
        assertEquals("[A]", texto(g.rutaMenosSaltos("A", "A")));
        assertEquals("[A]", texto(g.rutaMenorPeso("A", "A")));
        assertPeso("0", g.pesoRutaMenor("A", "A"));
        assertEquals(BigDecimal.ZERO, g.pesoRutaMenor("A", "A"));
        // Un lazo no cambia la ruta trivial de peso cero.
        g.agregarArista("D", "D", bd("3"));
        assertEquals("[D]", texto(g.rutaMenorPeso("D", "D")));
        assertPeso("0", g.pesoRutaMenor("D", "D"));
    }

    /** Compara Dijkstra y BFS contra Bellman-Ford sobre grafos aleatorios. */
    @Test
    void rutasCoincidenConBellmanFordEnGrafosAleatorios() {
        Random azar = new Random(20261115L);
        for (int caso = 0; caso < 20; caso++) {
            int n = 12;
            GrafoDirigidoPonderado<Integer> g = new GrafoDirigidoPonderadoImpl<>();
            BigDecimal[][] peso = new BigDecimal[n][n];
            for (int v = 0; v < n; v++) {
                g.agregarVertice(v);
            }
            for (int u = 0; u < n; u++) {
                for (int v = 0; v < n; v++) {
                    if (u != v && azar.nextInt(100) < 20) {
                        peso[u][v] = BigDecimal.valueOf(azar.nextInt(1000), 2); // 0.00 a 9.99
                        g.agregarArista(u, v, peso[u][v]);
                    }
                }
            }
            for (int origen = 0; origen < n; origen++) {
                BigDecimal[] distancia = bellmanFord(peso, origen, false);
                BigDecimal[] saltos = bellmanFord(peso, origen, true);
                for (int destino = 0; destino < n; destino++) {
                    BigDecimal obtenido = g.pesoRutaMenor(origen, destino);
                    Lista<Integer> ruta = g.rutaMenorPeso(origen, destino);
                    Lista<Integer> rutaCorta = g.rutaMenosSaltos(origen, destino);
                    if (distancia[destino] == null) {
                        assertNull(obtenido);
                        assertTrue(ruta.estaVacia());
                        assertTrue(rutaCorta.estaVacia());
                        assertFalse(g.existeRuta(origen, destino));
                        continue;
                    }
                    assertEquals(0, distancia[destino].compareTo(obtenido));
                    assertTrue(g.existeRuta(origen, destino));
                    // La ruta empieza y termina donde debe y su peso suma lo informado.
                    assertEquals(origen, (int) ruta.obtener(0));
                    assertEquals(destino, (int) ruta.obtener(ruta.tamanio() - 1));
                    BigDecimal suma = BigDecimal.ZERO;
                    for (int i = 0; i + 1 < ruta.tamanio(); i++) {
                        BigDecimal p = peso[ruta.obtener(i)][ruta.obtener(i + 1)];
                        assertTrue(p != null, "la ruta usa una arista inexistente");
                        suma = suma.add(p);
                    }
                    assertEquals(0, suma.compareTo(obtenido));
                    // La ruta de menos saltos tiene exactamente la cantidad mínima de aristas.
                    assertEquals(saltos[destino].intValue(), rutaCorta.tamanio() - 1);
                    assertEquals(origen, (int) rutaCorta.obtener(0));
                    assertEquals(destino, (int) rutaCorta.obtener(rutaCorta.tamanio() - 1));
                    for (int i = 0; i + 1 < rutaCorta.tamanio(); i++) {
                        assertTrue(peso[rutaCorta.obtener(i)][rutaCorta.obtener(i + 1)] != null);
                    }
                }
            }
        }
    }

    private static BigDecimal[] bellmanFord(BigDecimal[][] peso, int origen, boolean contarSaltos) {
        int n = peso.length;
        BigDecimal[] distancia = new BigDecimal[n];
        distancia[origen] = BigDecimal.ZERO;
        for (int iteracion = 0; iteracion < n - 1; iteracion++) {
            for (int u = 0; u < n; u++) {
                for (int v = 0; v < n; v++) {
                    if (peso[u][v] == null || distancia[u] == null) {
                        continue;
                    }
                    BigDecimal nueva = distancia[u].add(contarSaltos ? BigDecimal.ONE : peso[u][v]);
                    if (distancia[v] == null || nueva.compareTo(distancia[v]) < 0) {
                        distancia[v] = nueva;
                    }
                }
            }
        }
        return distancia;
    }

    // ------------------------------------------------------------------ ciclos

    @Test
    void tieneCiclo() {
        assertFalse(grafo("A>B:1", "A>C:1", "B>D:1", "C>D:1").tieneCiclo());
        assertTrue(grafo("A>B:1", "A>C:1", "B>D:1", "C>D:1", "D>A:1").tieneCiclo());
        assertTrue(grafo("A>A:1").tieneCiclo());
        assertTrue(grafo("A>B:1", "B>A:1").tieneCiclo());
        // Desconectado: el ciclo está en la segunda componente.
        assertTrue(grafo("A>B:1", "X>Y:1", "Y>Z:1", "Z>X:1").tieneCiclo());
        assertFalse(grafo("A>B:1", "X>Y:1", "Y>Z:1").tieneCiclo());
    }

    @Test
    void eliminarUnaAristaPuedeRomperElCiclo() {
        GrafoDirigidoPonderado<String> g = grafo("A>B:1", "B>C:1", "C>A:1");
        assertTrue(g.tieneCiclo());
        g.eliminarArista("C", "A");
        assertFalse(g.tieneCiclo());
    }

    /**
     * A->B, B->C, C->A, B->A, C->D, D->A, X->Y, Y->X.
     * Ciclos que pasan por A: [A,B,C,A] (3), [A,B,C,D,A] (4), [A,B,A] (2).
     */
    private static GrafoDirigidoPonderado<String> grafoDeCiclos() {
        return grafo("A>B:1", "B>C:1", "C>A:1", "B>A:1", "C>D:1", "D>A:1", "X>Y:1", "Y>X:1");
    }

    @Test
    void ciclosMultiplesQuePasanPorUnVertice() {
        GrafoDirigidoPonderado<String> g = grafoDeCiclos();
        assertEquals("[[A, B, C, A], [A, B, C, D, A], [A, B, A]]", texto(g.ciclosQueIncluyen("A", 10)));
        assertEquals("[[D, A, B, C, D]]", texto(g.ciclosQueIncluyen("D", 10)));
        assertEquals("[[X, Y, X]]", texto(g.ciclosQueIncluyen("X", 10)));
    }

    @Test
    void limiteDeLongitudDeCiclos() {
        GrafoDirigidoPonderado<String> g = grafoDeCiclos();
        assertEquals("[]", texto(g.ciclosQueIncluyen("A", 1)));
        assertEquals("[[A, B, A]]", texto(g.ciclosQueIncluyen("A", 2)));
        assertEquals("[[A, B, C, A], [A, B, A]]", texto(g.ciclosQueIncluyen("A", 3)));
        assertEquals("[[A, B, C, A], [A, B, C, D, A], [A, B, A]]", texto(g.ciclosQueIncluyen("A", 4)));
        assertEquals("[]", texto(g.ciclosQueIncluyen("D", 3)));
        assertEquals("[[D, A, B, C, D]]", texto(g.ciclosQueIncluyen("D", 4)));
    }

    @Test
    void longitudMaximaInvalidaLanzaExcepcion() {
        GrafoDirigidoPonderado<String> g = grafoDeCiclos();
        assertThrows(IllegalArgumentException.class, () -> g.ciclosQueIncluyen("A", 0));
        assertThrows(IllegalArgumentException.class, () -> g.ciclosQueIncluyen("A", -3));
    }

    @Test
    void soloDevuelveCiclosSimples() {
        // A,B,C,B,A repite B: no es simple y no debe aparecer.
        GrafoDirigidoPonderado<String> g = grafo("A>B:1", "B>C:1", "C>B:1", "B>A:1");
        assertEquals("[[A, B, A]]", texto(g.ciclosQueIncluyen("A", 10)));
        assertEquals("[[B, C, B], [B, A, B]]", texto(g.ciclosQueIncluyen("B", 10)));
    }

    @Test
    void lazoEsUnCicloDeLongitudUno() {
        GrafoDirigidoPonderado<String> g = grafo("A>A:1", "A>B:1", "B>A:1");
        assertEquals("[[A, A], [A, B, A]]", texto(g.ciclosQueIncluyen("A", 5)));
        assertEquals("[[A, A]]", texto(g.ciclosQueIncluyen("A", 1)));
        assertEquals("[[B, A, B]]", texto(g.ciclosQueIncluyen("B", 5)));
    }

    @Test
    void verticeSinCiclosDevuelveListaVacia() {
        GrafoDirigidoPonderado<String> g = grafoDeCiclos();
        g.agregarArista("D", "E", bd("1"));
        assertTrue(g.ciclosQueIncluyen("E", 10).estaVacia());
    }

    @Test
    void noRepiteCiclosEnUnGrafoCompleto() {
        // Grafo completo dirigido de 4 vértices: los ciclos simples por un vértice son
        // 3 de longitud 2 + 6 de longitud 3 + 6 de longitud 4 = 15.
        GrafoDirigidoPonderado<Integer> g = new GrafoDirigidoPonderadoImpl<>();
        for (int u = 0; u < 4; u++) {
            for (int v = 0; v < 4; v++) {
                if (u != v) {
                    g.agregarArista(u, v, BigDecimal.ONE);
                }
            }
        }
        Lista<Lista<Integer>> ciclos = g.ciclosQueIncluyen(0, 4);
        assertEquals(15, ciclos.tamanio());
        Lista<String> vistos = new ListaEnlazada<>();
        for (Lista<Integer> ciclo : ciclos) {
            String clave = texto(ciclo);
            assertFalse(vistos.contiene(clave), "ciclo repetido: " + clave);
            vistos.agregar(clave);
            assertEquals(0, (int) ciclo.obtener(0));
            assertEquals(0, (int) ciclo.obtener(ciclo.tamanio() - 1));
            // Simple: sin vértices repetidos salvo el inicio/fin.
            for (int i = 1; i < ciclo.tamanio() - 1; i++) {
                for (int j = i + 1; j < ciclo.tamanio() - 1; j++) {
                    assertFalse(ciclo.obtener(i).equals(ciclo.obtener(j)));
                }
                assertFalse(ciclo.obtener(i).equals(0));
            }
        }
        assertEquals(3, g.ciclosQueIncluyen(0, 2).tamanio());
        assertEquals(9, g.ciclosQueIncluyen(0, 3).tamanio());
    }

    // ------------------------------------------------------------------ general

    @Test
    void limpiar() {
        GrafoDirigidoPonderado<String> g = grafoDeCiclos();
        g.limpiar();
        assertTrue(g.estaVacio());
        assertEquals(0, g.cantidadVertices());
        assertEquals(0, g.cantidadAristas());
        assertFalse(g.tieneCiclo());
        g.agregarArista("A", "B", bd("1"));
        assertEquals(1, g.cantidadAristas());
    }
}
