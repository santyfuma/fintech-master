package co.edu.uniquindio.fintech.structures.propias;

import co.edu.uniquindio.fintech.structures.interfaces.ArbolBinarioBusqueda;
import co.edu.uniquindio.fintech.structures.interfaces.ClaveCompuesta;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArbolBinarioBusquedaImplTest {

    /**
     * Árbol de referencia (insertando 50, 30, 70, 20, 40, 60, 80):
     * <pre>
     *          50
     *        /    \
     *      30      70
     *     /  \    /  \
     *    20  40  60  80
     * </pre>
     */
    private static ArbolBinarioBusqueda<Integer, String> arbolCon(int... claves) {
        ArbolBinarioBusqueda<Integer, String> arbol = new ArbolBinarioBusquedaImpl<>();
        for (int c : claves) {
            arbol.insertar(c, "v" + c);
        }
        return arbol;
    }

    private static ArbolBinarioBusqueda<Integer, String> arbolDeReferencia() {
        return arbolCon(50, 30, 70, 20, 40, 60, 80);
    }

    private static String texto(Lista<?> lista) {
        StringBuilder sb = new StringBuilder("[");
        for (Object e : lista) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(e);
        }
        return sb.append(']').toString();
    }

    // ------------------------------------------------------------------ vacío y un nodo

    @Test
    void arbolVacio() {
        ArbolBinarioBusqueda<Integer, String> arbol = new ArbolBinarioBusquedaImpl<>();
        assertTrue(arbol.estaVacio());
        assertEquals(0, arbol.tamanio());
        assertEquals(-1, arbol.altura());
        assertNull(arbol.buscar(1));
        assertFalse(arbol.contiene(1));
        assertNull(arbol.eliminar(1));
        assertEquals("[]", texto(arbol.inOrden()));
        assertEquals("[]", texto(arbol.inOrdenInverso()));
        assertEquals("[]", texto(arbol.preOrden()));
        assertEquals("[]", texto(arbol.postOrden()));
        assertEquals("[]", texto(arbol.porNiveles()));
        assertEquals("[]", texto(arbol.claves()));
        assertEquals("[]", texto(arbol.rango(0, 100)));
        assertEquals("[]", texto(arbol.mayores(3)));
        assertThrows(IllegalStateException.class, arbol::claveMinima);
        assertThrows(IllegalStateException.class, arbol::claveMaxima);
    }

    @Test
    void arbolDeUnSoloNodo() {
        ArbolBinarioBusqueda<Integer, String> arbol = arbolCon(10);
        assertEquals(1, arbol.tamanio());
        assertEquals(0, arbol.altura());
        assertEquals(10, (int) arbol.claveMinima());
        assertEquals(10, (int) arbol.claveMaxima());
        assertEquals("[v10]", texto(arbol.inOrden()));
        assertEquals("[v10]", texto(arbol.preOrden()));
        assertEquals("[v10]", texto(arbol.postOrden()));
        assertEquals("[v10]", texto(arbol.porNiveles()));
        assertEquals("[v10]", texto(arbol.mayores(5)));
    }

    // ------------------------------------------------------------------ inserción y búsqueda

    @Test
    void insertarYBuscar() {
        ArbolBinarioBusqueda<Integer, String> arbol = arbolDeReferencia();
        assertEquals(7, arbol.tamanio());
        assertFalse(arbol.estaVacio());
        for (int c : new int[]{50, 30, 70, 20, 40, 60, 80}) {
            assertEquals("v" + c, arbol.buscar(c));
            assertTrue(arbol.contiene(c));
        }
        assertNull(arbol.buscar(55));
        assertFalse(arbol.contiene(10));
        assertFalse(arbol.contiene(90));
    }

    @Test
    void insertarNuevaClaveDevuelveNull() {
        ArbolBinarioBusqueda<Integer, String> arbol = new ArbolBinarioBusquedaImpl<>();
        assertNull(arbol.insertar(5, "a"));
        assertNull(arbol.insertar(3, "b"));
    }

    @Test
    void claveExistenteReemplazaElValorYDevuelveElAnterior() {
        ArbolBinarioBusqueda<Integer, String> arbol = arbolDeReferencia();
        assertEquals("v40", arbol.insertar(40, "nuevo"));
        assertEquals("nuevo", arbol.insertar(40, "otro"));
        assertEquals("otro", arbol.buscar(40));
        assertEquals(7, arbol.tamanio());
        assertEquals("[v50, v30, v20, otro, v70, v60, v80]", texto(arbol.preOrden()));
    }

    /** Clave cuyo compareTo solo mira el orden; la etiqueta no interviene. */
    private record ClaveParcial(int orden, String etiqueta) implements Comparable<ClaveParcial> {
        @Override
        public int compareTo(ClaveParcial otra) {
            return Integer.compare(orden, otra.orden);
        }
    }

    @Test
    void lasClavesSonUnicasSegunCompareTo() {
        ArbolBinarioBusqueda<ClaveParcial, String> arbol = new ArbolBinarioBusquedaImpl<>();
        assertNull(arbol.insertar(new ClaveParcial(1, "x"), "primero"));
        assertEquals("primero", arbol.insertar(new ClaveParcial(1, "y"), "segundo"));
        assertEquals(1, arbol.tamanio());
        assertEquals("segundo", arbol.buscar(new ClaveParcial(1, "z")));
    }

    @Test
    void insercionAscendenteProduceArbolDegenerado() {
        ArbolBinarioBusqueda<Integer, String> arbol = new ArbolBinarioBusquedaImpl<>();
        for (int i = 1; i <= 2000; i++) {
            arbol.insertar(i, "v" + i);
        }
        assertEquals(2000, arbol.tamanio());
        assertEquals(1999, arbol.altura());
        assertEquals(1, (int) arbol.claveMinima());
        assertEquals(2000, (int) arbol.claveMaxima());
        Lista<Integer> claves = arbol.claves();
        for (int i = 0; i < 2000; i++) {
            assertEquals(i + 1, (int) claves.obtener(i));
        }
        assertEquals("[v2000, v1999]", texto(arbol.mayores(2)));
        assertEquals("[v1000, v1001]", texto(arbol.rango(1000, 1001)));
        // Los recorridos son iterativos: no desbordan la pila con 2000 niveles.
        assertEquals(2000, arbol.preOrden().tamanio());
        assertEquals(2000, arbol.postOrden().tamanio());
        assertEquals(2000, arbol.porNiveles().tamanio());
        assertEquals(2000, arbol.inOrdenInverso().tamanio());
        for (int i = 1; i <= 2000; i++) {
            assertEquals("v" + i, arbol.eliminar(i));
        }
        assertTrue(arbol.estaVacio());
    }

    @Test
    void insercionDescendenteProduceArbolDegenerado() {
        ArbolBinarioBusqueda<Integer, String> arbol = arbolCon(5, 4, 3, 2, 1);
        assertEquals(4, arbol.altura());
        assertEquals("[v1, v2, v3, v4, v5]", texto(arbol.inOrden()));
        assertEquals("[v5, v4, v3, v2, v1]", texto(arbol.preOrden()));
        assertEquals("[v1, v2, v3, v4, v5]", texto(arbol.postOrden()));
        assertEquals(1, (int) arbol.claveMinima());
        assertEquals(5, (int) arbol.claveMaxima());
    }

    // ------------------------------------------------------------------ recorridos

    @Test
    void losCincoRecorridosDelArbolDeReferencia() {
        ArbolBinarioBusqueda<Integer, String> arbol = arbolDeReferencia();
        assertEquals("[v20, v30, v40, v50, v60, v70, v80]", texto(arbol.inOrden()));
        assertEquals("[v80, v70, v60, v50, v40, v30, v20]", texto(arbol.inOrdenInverso()));
        assertEquals("[v50, v30, v20, v40, v70, v60, v80]", texto(arbol.preOrden()));
        assertEquals("[v20, v40, v30, v60, v80, v70, v50]", texto(arbol.postOrden()));
        assertEquals("[v50, v30, v70, v20, v40, v60, v80]", texto(arbol.porNiveles()));
        assertEquals("[20, 30, 40, 50, 60, 70, 80]", texto(arbol.claves()));
    }

    @Test
    void recorridosDeUnArbolIrregular() {
        // 8 -> izq 3 (izq 1, der 6 (izq 4, der 7)), der 10 (der 14 (izq 13))
        ArbolBinarioBusqueda<Integer, String> arbol = arbolCon(8, 3, 10, 1, 6, 14, 4, 7, 13);
        assertEquals("[v1, v3, v4, v6, v7, v8, v10, v13, v14]", texto(arbol.inOrden()));
        assertEquals("[v8, v3, v1, v6, v4, v7, v10, v14, v13]", texto(arbol.preOrden()));
        assertEquals("[v1, v4, v7, v6, v3, v13, v14, v10, v8]", texto(arbol.postOrden()));
        assertEquals("[v8, v3, v10, v1, v6, v14, v4, v7, v13]", texto(arbol.porNiveles()));
        assertEquals(3, arbol.altura());
    }

    @Test
    void losRecorridosDevuelvenCopias() {
        ArbolBinarioBusqueda<Integer, String> arbol = arbolDeReferencia();
        Lista<String> recorrido = arbol.inOrden();
        recorrido.limpiar();
        assertEquals(7, arbol.inOrden().tamanio());
    }

    // ------------------------------------------------------------------ altura, mínimo y máximo

    @Test
    void altura() {
        assertEquals(2, arbolDeReferencia().altura());
        assertEquals(1, arbolCon(2, 1).altura());
        assertEquals(1, arbolCon(2, 1, 3).altura());
        assertEquals(3, arbolCon(10, 5, 1, 0).altura());
    }

    @Test
    void minimoYMaximo() {
        ArbolBinarioBusqueda<Integer, String> arbol = arbolDeReferencia();
        assertEquals(20, (int) arbol.claveMinima());
        assertEquals(80, (int) arbol.claveMaxima());
        arbol.eliminar(20);
        arbol.eliminar(80);
        assertEquals(30, (int) arbol.claveMinima());
        assertEquals(70, (int) arbol.claveMaxima());
    }

    // ------------------------------------------------------------------ eliminación

    @Test
    void eliminarUnaHoja() {
        ArbolBinarioBusqueda<Integer, String> arbol = arbolDeReferencia();
        assertEquals("v20", arbol.eliminar(20));
        assertEquals(6, arbol.tamanio());
        assertFalse(arbol.contiene(20));
        assertEquals("[v50, v30, v40, v70, v60, v80]", texto(arbol.preOrden()));
    }

    @Test
    void eliminarUnNodoConUnHijo() {
        ArbolBinarioBusqueda<Integer, String> arbol = arbolDeReferencia();
        arbol.eliminar(20); // 30 queda solo con el hijo derecho 40
        assertEquals("v30", arbol.eliminar(30));
        assertEquals("[v50, v40, v70, v60, v80]", texto(arbol.preOrden()));
        assertEquals("[v40, v50, v60, v70, v80]", texto(arbol.inOrden()));

        ArbolBinarioBusqueda<Integer, String> conHijoIzquierdo = arbolCon(50, 30, 20);
        assertEquals("v30", conHijoIzquierdo.eliminar(30));
        assertEquals("[v50, v20]", texto(conHijoIzquierdo.preOrden()));
    }

    @Test
    void eliminarUnNodoConDosHijos() {
        ArbolBinarioBusqueda<Integer, String> arbol = arbolDeReferencia();
        // El sucesor de 70 es 80 (mínimo de su subárbol derecho).
        assertEquals("v70", arbol.eliminar(70));
        assertEquals("[v50, v30, v20, v40, v80, v60]", texto(arbol.preOrden()));
        assertEquals("[v20, v30, v40, v50, v60, v80]", texto(arbol.inOrden()));
        assertEquals(6, arbol.tamanio());
    }

    @Test
    void eliminarConDosHijosCuandoElSucesorTieneHijoDerecho() {
        // 50 -> 30, 70; 70 -> 60, 80; 60 -> der 65. El sucesor de 50 es 60.
        ArbolBinarioBusqueda<Integer, String> arbol = arbolCon(50, 30, 70, 60, 80, 65);
        assertEquals("[v50, v30, v70, v60, v65, v80]", texto(arbol.preOrden()));
        assertEquals("v50", arbol.eliminar(50));
        assertEquals("[v60, v30, v70, v65, v80]", texto(arbol.preOrden()));
        assertEquals("[v30, v60, v65, v70, v80]", texto(arbol.inOrden()));
        assertEquals("v60", arbol.buscar(60));
        assertEquals(5, arbol.tamanio());
    }

    @Test
    void eliminarLaRaiz() {
        ArbolBinarioBusqueda<Integer, String> arbol = arbolDeReferencia();
        // Raíz con dos hijos: la reemplaza su sucesor 60.
        assertEquals("v50", arbol.eliminar(50));
        assertEquals("[v60, v30, v20, v40, v70, v80]", texto(arbol.preOrden()));

        ArbolBinarioBusqueda<Integer, String> unHijo = arbolCon(10, 20, 30);
        assertEquals("v10", unHijo.eliminar(10));
        assertEquals("[v20, v30]", texto(unHijo.preOrden()));
        assertEquals(1, unHijo.altura());

        ArbolBinarioBusqueda<Integer, String> soloRaiz = arbolCon(1);
        assertEquals("v1", soloRaiz.eliminar(1));
        assertTrue(soloRaiz.estaVacio());
        assertEquals(-1, soloRaiz.altura());
        soloRaiz.insertar(2, "v2");
        assertEquals("[v2]", texto(soloRaiz.inOrden()));
    }

    @Test
    void eliminarUnaClaveInexistenteNoModificaElArbol() {
        ArbolBinarioBusqueda<Integer, String> arbol = arbolDeReferencia();
        assertNull(arbol.eliminar(55));
        assertNull(arbol.eliminar(10));
        assertNull(arbol.eliminar(90));
        assertEquals(7, arbol.tamanio());
        assertEquals("[v50, v30, v20, v40, v70, v60, v80]", texto(arbol.preOrden()));
    }

    @Test
    void eliminarDosVecesLaMismaClave() {
        ArbolBinarioBusqueda<Integer, String> arbol = arbolDeReferencia();
        assertEquals("v30", arbol.eliminar(30));
        assertNull(arbol.eliminar(30));
        assertEquals(6, arbol.tamanio());
    }

    @Test
    void limpiar() {
        ArbolBinarioBusqueda<Integer, String> arbol = arbolDeReferencia();
        arbol.limpiar();
        assertTrue(arbol.estaVacio());
        assertEquals(-1, arbol.altura());
        assertNull(arbol.buscar(50));
        arbol.insertar(1, "v1");
        assertEquals(1, arbol.tamanio());
    }

    // ------------------------------------------------------------------ rango

    @Test
    void rangoConLimitesExactosEsCerrado() {
        ArbolBinarioBusqueda<Integer, String> arbol = arbolDeReferencia();
        assertEquals("[v30, v40, v50, v60]", texto(arbol.rango(30, 60)));
        assertEquals("[v20, v30, v40, v50, v60, v70, v80]", texto(arbol.rango(20, 80)));
        assertEquals("[v50]", texto(arbol.rango(50, 50)));
    }

    @Test
    void rangoConLimitesQueNoSonClaves() {
        ArbolBinarioBusqueda<Integer, String> arbol = arbolDeReferencia();
        assertEquals("[v30, v40, v50]", texto(arbol.rango(25, 55)));
        assertEquals("[v20, v30, v40, v50, v60, v70, v80]", texto(arbol.rango(0, 1000)));
        assertEquals("[]", texto(arbol.rango(41, 49)));
        assertEquals("[]", texto(arbol.rango(81, 100)));
        assertEquals("[]", texto(arbol.rango(0, 19)));
        assertEquals("[]", texto(arbol.rango(55, 55)));
    }

    @Test
    void rangoInvertidoDevuelveListaVacia() {
        ArbolBinarioBusqueda<Integer, String> arbol = arbolDeReferencia();
        assertEquals("[]", texto(arbol.rango(60, 30)));
    }

    /** Clave que cuenta cuántas comparaciones se hacen. */
    private record ClaveContada(int valor) implements Comparable<ClaveContada> {
        private static int comparaciones;

        @Override
        public int compareTo(ClaveContada otra) {
            comparaciones++;
            return Integer.compare(valor, otra.valor);
        }
    }

    private static void insertarEquilibrado(ArbolBinarioBusqueda<ClaveContada, Integer> arbol, int desde, int hasta) {
        if (desde > hasta) {
            return;
        }
        int medio = (desde + hasta) / 2;
        arbol.insertar(new ClaveContada(medio), medio);
        insertarEquilibrado(arbol, desde, medio - 1);
        insertarEquilibrado(arbol, medio + 1, hasta);
    }

    @Test
    void rangoSoloVisitaLasRamasNecesarias() {
        ArbolBinarioBusqueda<ClaveContada, Integer> arbol = new ArbolBinarioBusquedaImpl<>();
        insertarEquilibrado(arbol, 0, 1022); // 1023 nodos, altura 9
        assertEquals(9, arbol.altura());
        ClaveContada.comparaciones = 0;
        Lista<Integer> resultado = arbol.rango(new ClaveContada(500), new ClaveContada(503));
        assertEquals("[500, 501, 502, 503]", texto(resultado));
        // Sin poda se compararían ~2 x 1023 claves; con poda solo un camino más los resultados.
        assertTrue(ClaveContada.comparaciones < 60, "comparaciones: " + ClaveContada.comparaciones);
    }

    // ------------------------------------------------------------------ mayores

    @Test
    void mayores() {
        ArbolBinarioBusqueda<Integer, String> arbol = arbolDeReferencia();
        assertEquals("[]", texto(arbol.mayores(0)));
        assertEquals("[v80]", texto(arbol.mayores(1)));
        assertEquals("[v80, v70, v60]", texto(arbol.mayores(3)));
        assertEquals("[v80, v70, v60, v50, v40, v30, v20]", texto(arbol.mayores(7)));
        assertEquals("[v80, v70, v60, v50, v40, v30, v20]", texto(arbol.mayores(100)));
    }

    @Test
    void mayoresConCantidadNegativaLanzaExcepcion() {
        ArbolBinarioBusqueda<Integer, String> arbol = arbolDeReferencia();
        assertThrows(IllegalArgumentException.class, () -> arbol.mayores(-1));
        assertThrows(IllegalArgumentException.class, () -> new ArbolBinarioBusquedaImpl<Integer, String>().mayores(-5));
    }

    // ------------------------------------------------------------------ nulos

    @Test
    void rechazaClavesYValoresNulos() {
        ArbolBinarioBusqueda<Integer, String> arbol = arbolDeReferencia();
        assertThrows(NullPointerException.class, () -> arbol.insertar(null, "x"));
        assertThrows(NullPointerException.class, () -> arbol.insertar(1, null));
        assertThrows(NullPointerException.class, () -> arbol.buscar(null));
        assertThrows(NullPointerException.class, () -> arbol.contiene(null));
        assertThrows(NullPointerException.class, () -> arbol.eliminar(null));
        assertThrows(NullPointerException.class, () -> arbol.rango(null, 10));
        assertThrows(NullPointerException.class, () -> arbol.rango(10, null));
        assertEquals(7, arbol.tamanio());
        assertThrows(NullPointerException.class, () -> new ArbolBinarioBusquedaImpl<Integer, String>().insertar(null, "x"));
    }

    // ------------------------------------------------------------------ ClaveCompuesta

    private static ArbolBinarioBusqueda<ClaveCompuesta<Integer, String>, String> usuariosPorPuntos() {
        ArbolBinarioBusqueda<ClaveCompuesta<Integer, String>, String> arbol = new ArbolBinarioBusquedaImpl<>();
        arbol.insertar(ClaveCompuesta.de(500, "ana"), "ana");
        arbol.insertar(ClaveCompuesta.de(501, "luis"), "luis");
        arbol.insertar(ClaveCompuesta.de(800, "carla"), "carla");
        arbol.insertar(ClaveCompuesta.de(800, "beto"), "beto");
        arbol.insertar(ClaveCompuesta.de(1000, "dora"), "dora");
        arbol.insertar(ClaveCompuesta.de(1001, "eva"), "eva");
        arbol.insertar(ClaveCompuesta.de(800, "abel"), "abel");
        return arbol;
    }

    @Test
    void claveCompuestaPermitePuntosRepetidosConIdentificacionDistinta() {
        ArbolBinarioBusqueda<ClaveCompuesta<Integer, String>, String> arbol = usuariosPorPuntos();
        assertEquals(7, arbol.tamanio());
        assertEquals("carla", arbol.buscar(ClaveCompuesta.de(800, "carla")));
        assertEquals("beto", arbol.buscar(ClaveCompuesta.de(800, "beto")));
        assertNull(arbol.buscar(ClaveCompuesta.de(800, "zoe")));
    }

    @Test
    void claveCompuestaOrdenaPorPrincipalYLuegoPorDesempate() {
        ArbolBinarioBusqueda<ClaveCompuesta<Integer, String>, String> arbol = usuariosPorPuntos();
        assertEquals("[ana, luis, abel, beto, carla, dora, eva]", texto(arbol.inOrden()));
        assertEquals("[eva, dora, carla, beto, abel, luis, ana]", texto(arbol.inOrdenInverso()));
        assertEquals("[eva, dora]", texto(arbol.mayores(2)));
        assertEquals(ClaveCompuesta.de(500, "ana"), arbol.claveMinima());
        assertEquals(ClaveCompuesta.de(1001, "eva"), arbol.claveMaxima());
    }

    @Test
    void mismaClaveCompuestaReemplazaElValor() {
        ArbolBinarioBusqueda<ClaveCompuesta<Integer, String>, String> arbol = usuariosPorPuntos();
        assertEquals("beto", arbol.insertar(ClaveCompuesta.de(800, "beto"), "beto-actualizado"));
        assertEquals(7, arbol.tamanio());
    }

    @Test
    void cotasConsultanRangosPorLaPartePrincipal() {
        ArbolBinarioBusqueda<ClaveCompuesta<Integer, String>, String> arbol = usuariosPorPuntos();
        // Nivel Plata: 501 a 1000 puntos, ambos incluidos.
        assertEquals("[luis, abel, beto, carla, dora]",
                texto(arbol.rango(ClaveCompuesta.cotaInferior(501), ClaveCompuesta.cotaSuperior(1000))));
        // Exactamente 800 puntos.
        assertEquals("[abel, beto, carla]",
                texto(arbol.rango(ClaveCompuesta.cotaInferior(800), ClaveCompuesta.cotaSuperior(800))));
        // Bronce: 0 a 500.
        assertEquals("[ana]",
                texto(arbol.rango(ClaveCompuesta.cotaInferior(0), ClaveCompuesta.cotaSuperior(500))));
        // Sin usuarios.
        assertEquals("[]",
                texto(arbol.rango(ClaveCompuesta.cotaInferior(600), ClaveCompuesta.cotaSuperior(799))));
        // Cotas invertidas para el mismo valor principal.
        assertEquals("[]",
                texto(arbol.rango(ClaveCompuesta.cotaSuperior(800), ClaveCompuesta.cotaInferior(800))));
    }

    @Test
    void lasCotasNoSePuedenInsertarNiSeEncuentran() {
        ArbolBinarioBusqueda<ClaveCompuesta<Integer, String>, String> arbol = usuariosPorPuntos();
        assertThrows(IllegalArgumentException.class,
                () -> arbol.insertar(ClaveCompuesta.cotaInferior(800), "x"));
        assertThrows(IllegalArgumentException.class,
                () -> arbol.insertar(ClaveCompuesta.cotaSuperior(800), "x"));
        assertEquals(7, arbol.tamanio());
        assertNull(arbol.buscar(ClaveCompuesta.cotaInferior(800)));
        assertNull(arbol.eliminar(ClaveCompuesta.cotaSuperior(800)));
    }

    @Test
    void eliminarUsuarioConPuntosRepetidosNoAfectaALosDemas() {
        ArbolBinarioBusqueda<ClaveCompuesta<Integer, String>, String> arbol = usuariosPorPuntos();
        assertEquals("beto", arbol.eliminar(ClaveCompuesta.de(800, "beto")));
        assertEquals("[abel, carla]",
                texto(arbol.rango(ClaveCompuesta.cotaInferior(800), ClaveCompuesta.cotaSuperior(800))));
    }

    // ------------------------------------------------------------------ aleatoria

    @Test
    void secuenciaAleatoriaCoincideConTreeMap() {
        Random azar = new Random(20261115L);
        ArbolBinarioBusqueda<Integer, String> arbol = new ArbolBinarioBusquedaImpl<>();
        TreeMap<Integer, String> referencia = new TreeMap<>();
        for (int paso = 0; paso < 20_000; paso++) {
            int clave = azar.nextInt(400);
            int operacion = azar.nextInt(4);
            if (operacion <= 1) {
                String valor = "v" + paso;
                assertEquals(referencia.put(clave, valor), arbol.insertar(clave, valor));
            } else if (operacion == 2) {
                assertEquals(referencia.remove(clave), arbol.eliminar(clave));
            } else {
                assertEquals(referencia.get(clave), arbol.buscar(clave));
            }
            assertEquals(referencia.size(), arbol.tamanio());
        }
        assertEquals(texto(listaDe(referencia.values())), texto(arbol.inOrden()));
        assertEquals(texto(listaDe(referencia.keySet())), texto(arbol.claves()));
        assertEquals(texto(listaDe(referencia.descendingMap().values())), texto(arbol.inOrdenInverso()));
        assertEquals(referencia.firstKey(), arbol.claveMinima());
        assertEquals(referencia.lastKey(), arbol.claveMaxima());
        for (int desde = -10; desde < 420; desde += 37) {
            int hasta = desde + 50;
            assertEquals(texto(listaDe(referencia.subMap(desde, true, hasta, true).values())),
                    texto(arbol.rango(desde, hasta)));
        }
        Lista<String> diez = new ListaEnlazada<>();
        int i = 0;
        for (Map.Entry<Integer, String> e : referencia.descendingMap().entrySet()) {
            if (i++ == 10) {
                break;
            }
            diez.agregar(e.getValue());
        }
        assertEquals(texto(diez), texto(arbol.mayores(10)));
    }

    private static <T> Lista<T> listaDe(Iterable<T> elementos) {
        Lista<T> lista = new ListaEnlazada<>();
        for (T e : elementos) {
            lista.agregar(e);
        }
        return lista;
    }
}
