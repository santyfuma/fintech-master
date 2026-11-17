package co.edu.uniquindio.fintech.structures.propias;

import co.edu.uniquindio.fintech.structures.adaptadores.ColaAdaptador;
import co.edu.uniquindio.fintech.structures.adaptadores.PilaAdaptador;
import co.edu.uniquindio.fintech.structures.interfaces.ArbolBinarioBusqueda;
import co.edu.uniquindio.fintech.structures.interfaces.ClaveCompuesta;
import co.edu.uniquindio.fintech.structures.interfaces.Cola;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import co.edu.uniquindio.fintech.structures.interfaces.Pila;

import java.util.Objects;

/**
 * Árbol binario de búsqueda propio, basado en nodos y <b>sin balanceo</b>.
 *
 * <p>Las claves son únicas según {@code compareTo}: insertar una clave que compara
 * igual a una existente reemplaza su valor. No acepta claves ni valores {@code null}
 * ({@link NullPointerException}). Si la clave es una {@link ClaveCompuesta} de tipo
 * cota, {@link #insertar} lanza {@link IllegalArgumentException}: las cotas solo sirven
 * como límites de {@link #rango}.
 *
 * <p>Todas las operaciones son iterativas (con {@link Pila} y {@link Cola} propias como
 * estructuras auxiliares), de modo que un árbol degenerado no desborda la pila de
 * llamadas de Java.
 *
 * <p>Complejidad temporal (n = nodos, h = altura, k = valores devueltos). Como el
 * árbol no se balancea, h depende del orden de inserción: h ≈ log₂ n si las claves
 * llegan en orden aleatorio (árbol equilibrado por casualidad) y h = n − 1 si llegan
 * ordenadas (árbol degenerado, equivalente a una lista).
 * <ul>
 *   <li>{@code insertar}, {@code buscar}, {@code contiene}, {@code eliminar},
 *       {@code claveMinima}, {@code claveMaxima}: O(h) — O(log n) equilibrado, O(n) degenerado.</li>
 *   <li>{@code rango}: O(h + k).</li>
 *   <li>{@code mayores(m)}: O(h + m).</li>
 *   <li>Recorridos, {@code claves} y {@code altura}: O(n).</li>
 *   <li>{@code tamanio}, {@code estaVacio}, {@code limpiar}: O(1).</li>
 * </ul>
 *
 * @param <K> tipo de la clave
 * @param <V> tipo del valor
 */
public class ArbolBinarioBusquedaImpl<K extends Comparable<? super K>, V>
        implements ArbolBinarioBusqueda<K, V> {

    private static final class Nodo<K, V> {
        private K clave;
        private V valor;
        private Nodo<K, V> izquierdo;
        private Nodo<K, V> derecho;

        private Nodo(K clave, V valor) {
            this.clave = clave;
            this.valor = valor;
        }
    }

    private Nodo<K, V> raiz;
    private int tamanio;

    // ---------------------------------------------------------------- inserción y búsqueda

    @Override
    public V insertar(K clave, V valor) {
        Objects.requireNonNull(clave, "clave");
        Objects.requireNonNull(valor, "valor");
        if (clave instanceof ClaveCompuesta<?, ?> compuesta && compuesta.esCota()) {
            throw new IllegalArgumentException("Una cota de consulta no puede insertarse como clave: " + clave);
        }
        if (raiz == null) {
            raiz = new Nodo<>(clave, valor);
            tamanio++;
            return null;
        }
        Nodo<K, V> actual = raiz;
        while (true) {
            int comparacion = clave.compareTo(actual.clave);
            if (comparacion == 0) {
                V anterior = actual.valor;
                actual.valor = valor;
                return anterior;
            }
            if (comparacion < 0) {
                if (actual.izquierdo == null) {
                    actual.izquierdo = new Nodo<>(clave, valor);
                    tamanio++;
                    return null;
                }
                actual = actual.izquierdo;
            } else {
                if (actual.derecho == null) {
                    actual.derecho = new Nodo<>(clave, valor);
                    tamanio++;
                    return null;
                }
                actual = actual.derecho;
            }
        }
    }

    @Override
    public V buscar(K clave) {
        Nodo<K, V> nodo = buscarNodo(clave);
        return nodo == null ? null : nodo.valor;
    }

    @Override
    public boolean contiene(K clave) {
        return buscarNodo(clave) != null;
    }

    // ---------------------------------------------------------------- eliminación

    @Override
    public V eliminar(K clave) {
        Objects.requireNonNull(clave, "clave");
        Nodo<K, V> padre = null;
        Nodo<K, V> actual = raiz;
        while (actual != null) {
            int comparacion = clave.compareTo(actual.clave);
            if (comparacion == 0) {
                break;
            }
            padre = actual;
            actual = comparacion < 0 ? actual.izquierdo : actual.derecho;
        }
        if (actual == null) {
            return null;
        }
        V eliminado = actual.valor;

        if (actual.izquierdo != null && actual.derecho != null) {
            // Dos hijos: se copia el sucesor inorden (mínimo del subárbol derecho) y se
            // elimina el nodo del sucesor, que tiene a lo sumo un hijo derecho.
            Nodo<K, V> padreSucesor = actual;
            Nodo<K, V> sucesor = actual.derecho;
            while (sucesor.izquierdo != null) {
                padreSucesor = sucesor;
                sucesor = sucesor.izquierdo;
            }
            actual.clave = sucesor.clave;
            actual.valor = sucesor.valor;
            padre = padreSucesor;
            actual = sucesor;
        }

        // Hoja o un hijo: el hijo (o null) ocupa el lugar del nodo.
        Nodo<K, V> hijo = actual.izquierdo != null ? actual.izquierdo : actual.derecho;
        if (padre == null) {
            raiz = hijo;
        } else if (padre.izquierdo == actual) {
            padre.izquierdo = hijo;
        } else {
            padre.derecho = hijo;
        }
        tamanio--;
        return eliminado;
    }

    @Override
    public void limpiar() {
        raiz = null;
        tamanio = 0;
    }

    // ---------------------------------------------------------------- extremos

    @Override
    public K claveMinima() {
        validarNoVacio();
        Nodo<K, V> actual = raiz;
        while (actual.izquierdo != null) {
            actual = actual.izquierdo;
        }
        return actual.clave;
    }

    @Override
    public K claveMaxima() {
        validarNoVacio();
        Nodo<K, V> actual = raiz;
        while (actual.derecho != null) {
            actual = actual.derecho;
        }
        return actual.clave;
    }

    // ---------------------------------------------------------------- recorridos

    @Override
    public Lista<V> inOrden() {
        Lista<V> resultado = new ListaEnlazada<>();
        Pila<Nodo<K, V>> pila = new PilaAdaptador<>();
        Nodo<K, V> actual = raiz;
        while (actual != null || !pila.estaVacia()) {
            while (actual != null) {
                pila.apilar(actual);
                actual = actual.izquierdo;
            }
            Nodo<K, V> nodo = pila.desapilar();
            resultado.agregar(nodo.valor);
            actual = nodo.derecho;
        }
        return resultado;
    }

    @Override
    public Lista<V> inOrdenInverso() {
        return mayoresQue(tamanio);
    }

    @Override
    public Lista<V> preOrden() {
        Lista<V> resultado = new ListaEnlazada<>();
        if (raiz == null) {
            return resultado;
        }
        Pila<Nodo<K, V>> pila = new PilaAdaptador<>();
        pila.apilar(raiz);
        while (!pila.estaVacia()) {
            Nodo<K, V> nodo = pila.desapilar();
            resultado.agregar(nodo.valor);
            // Se apila primero el derecho para visitar antes el izquierdo.
            if (nodo.derecho != null) {
                pila.apilar(nodo.derecho);
            }
            if (nodo.izquierdo != null) {
                pila.apilar(nodo.izquierdo);
            }
        }
        return resultado;
    }

    @Override
    public Lista<V> postOrden() {
        Lista<V> resultado = new ListaEnlazada<>();
        if (raiz == null) {
            return resultado;
        }
        // Se genera raíz-derecha-izquierda en una pila; al vaciarla sale izquierda-derecha-raíz.
        Pila<Nodo<K, V>> pendientes = new PilaAdaptador<>();
        Pila<Nodo<K, V>> salida = new PilaAdaptador<>();
        pendientes.apilar(raiz);
        while (!pendientes.estaVacia()) {
            Nodo<K, V> nodo = pendientes.desapilar();
            salida.apilar(nodo);
            if (nodo.izquierdo != null) {
                pendientes.apilar(nodo.izquierdo);
            }
            if (nodo.derecho != null) {
                pendientes.apilar(nodo.derecho);
            }
        }
        while (!salida.estaVacia()) {
            resultado.agregar(salida.desapilar().valor);
        }
        return resultado;
    }

    @Override
    public Lista<V> porNiveles() {
        Lista<V> resultado = new ListaEnlazada<>();
        if (raiz == null) {
            return resultado;
        }
        Cola<Nodo<K, V>> cola = new ColaAdaptador<>();
        cola.encolar(raiz);
        while (!cola.estaVacia()) {
            Nodo<K, V> nodo = cola.desencolar();
            resultado.agregar(nodo.valor);
            if (nodo.izquierdo != null) {
                cola.encolar(nodo.izquierdo);
            }
            if (nodo.derecho != null) {
                cola.encolar(nodo.derecho);
            }
        }
        return resultado;
    }

    @Override
    public Lista<K> claves() {
        Lista<K> resultado = new ListaEnlazada<>();
        Pila<Nodo<K, V>> pila = new PilaAdaptador<>();
        Nodo<K, V> actual = raiz;
        while (actual != null || !pila.estaVacia()) {
            while (actual != null) {
                pila.apilar(actual);
                actual = actual.izquierdo;
            }
            Nodo<K, V> nodo = pila.desapilar();
            resultado.agregar(nodo.clave);
            actual = nodo.derecho;
        }
        return resultado;
    }

    // ---------------------------------------------------------------- consultas por rango

    /**
     * {@inheritDoc}
     *
     * <p>Inorden iterativo con poda: no desciende por la izquierda de un nodo cuya
     * clave es menor que {@code desde} y se detiene en la primera clave mayor que
     * {@code hasta}.
     *
     * @throws NullPointerException si {@code desde} o {@code hasta} son {@code null}
     */
    @Override
    public Lista<V> rango(K desde, K hasta) {
        Objects.requireNonNull(desde, "desde");
        Objects.requireNonNull(hasta, "hasta");
        Lista<V> resultado = new ListaEnlazada<>();
        if (desde.compareTo(hasta) > 0) {
            return resultado;
        }
        Pila<Nodo<K, V>> pila = new PilaAdaptador<>();
        Nodo<K, V> actual = raiz;
        while (actual != null || !pila.estaVacia()) {
            while (actual != null) {
                if (actual.clave.compareTo(desde) < 0) {
                    // Este nodo y su subárbol izquierdo son menores que 'desde'.
                    actual = actual.derecho;
                } else {
                    pila.apilar(actual);
                    actual = actual.izquierdo;
                }
            }
            if (pila.estaVacia()) {
                break;
            }
            Nodo<K, V> nodo = pila.desapilar();
            if (nodo.clave.compareTo(hasta) > 0) {
                break; // todo lo que sigue en inorden es aún mayor
            }
            resultado.agregar(nodo.valor);
            actual = nodo.derecho;
        }
        return resultado;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Si {@code cantidad} es 0 o el árbol está vacío devuelve una lista vacía; si es
     * mayor que el tamaño devuelve todos los valores en orden descendente.
     *
     * @throws IllegalArgumentException si {@code cantidad} es negativa
     */
    @Override
    public Lista<V> mayores(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa: " + cantidad);
        }
        return mayoresQue(cantidad);
    }

    // ---------------------------------------------------------------- información

    @Override
    public int tamanio() {
        return tamanio;
    }

    @Override
    public boolean estaVacio() {
        return tamanio == 0;
    }

    @Override
    public int altura() {
        if (raiz == null) {
            return -1;
        }
        Cola<Nodo<K, V>> cola = new ColaAdaptador<>();
        cola.encolar(raiz);
        int altura = -1;
        while (!cola.estaVacia()) {
            int nodosDelNivel = cola.tamanio();
            for (int i = 0; i < nodosDelNivel; i++) {
                Nodo<K, V> nodo = cola.desencolar();
                if (nodo.izquierdo != null) {
                    cola.encolar(nodo.izquierdo);
                }
                if (nodo.derecho != null) {
                    cola.encolar(nodo.derecho);
                }
            }
            altura++;
        }
        return altura;
    }

    // ---------------------------------------------------------------- auxiliares

    private Nodo<K, V> buscarNodo(K clave) {
        Objects.requireNonNull(clave, "clave");
        Nodo<K, V> actual = raiz;
        while (actual != null) {
            int comparacion = clave.compareTo(actual.clave);
            if (comparacion == 0) {
                return actual;
            }
            actual = comparacion < 0 ? actual.izquierdo : actual.derecho;
        }
        return null;
    }

    /** Inorden inverso iterativo (derecha, raíz, izquierda) que se detiene tras {@code limite} valores. */
    private Lista<V> mayoresQue(int limite) {
        Lista<V> resultado = new ListaEnlazada<>();
        Pila<Nodo<K, V>> pila = new PilaAdaptador<>();
        Nodo<K, V> actual = raiz;
        while ((actual != null || !pila.estaVacia()) && resultado.tamanio() < limite) {
            while (actual != null) {
                pila.apilar(actual);
                actual = actual.derecho;
            }
            Nodo<K, V> nodo = pila.desapilar();
            resultado.agregar(nodo.valor);
            actual = nodo.izquierdo;
        }
        return resultado;
    }

    private void validarNoVacio() {
        if (raiz == null) {
            throw new IllegalStateException("El árbol está vacío");
        }
    }
}
