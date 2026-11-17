package co.edu.uniquindio.fintech.repository;

import co.edu.uniquindio.fintech.structures.interfaces.ArbolBinarioBusqueda;
import co.edu.uniquindio.fintech.structures.interfaces.Cola;
import co.edu.uniquindio.fintech.structures.interfaces.ColaPrioridad;
import co.edu.uniquindio.fintech.structures.interfaces.Comparador;
import co.edu.uniquindio.fintech.structures.interfaces.GrafoDirigidoPonderado;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import co.edu.uniquindio.fintech.structures.interfaces.Pila;
import co.edu.uniquindio.fintech.structures.interfaces.TablaHash;

/**
 * Crea instancias de las estructuras propias. La implementación vive en {@code config},
 * de modo que los repositorios (y luego los servicios) dependen solo de los contratos de
 * {@code structures.interfaces} (regla 7 de CONTEXTO.md).
 */
public interface FabricaEstructuras {

    <T> Lista<T> nuevaLista();

    <T> Pila<T> nuevaPila();

    <T> Cola<T> nuevaCola();

    <T> ColaPrioridad<T> nuevaColaPrioridad(Comparador<T> comparador);

    <K, V> TablaHash<K, V> nuevaTablaHash();

    <K extends Comparable<? super K>, V> ArbolBinarioBusqueda<K, V> nuevoArbol();

    <V> GrafoDirigidoPonderado<V> nuevoGrafo();
}
