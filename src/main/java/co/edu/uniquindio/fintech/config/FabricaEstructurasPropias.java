package co.edu.uniquindio.fintech.config;

import co.edu.uniquindio.fintech.repository.FabricaEstructuras;
import co.edu.uniquindio.fintech.structures.adaptadores.ColaAdaptador;
import co.edu.uniquindio.fintech.structures.adaptadores.ColaPrioridadAdaptador;
import co.edu.uniquindio.fintech.structures.adaptadores.PilaAdaptador;
import co.edu.uniquindio.fintech.structures.adaptadores.TablaHashAdaptador;
import co.edu.uniquindio.fintech.structures.interfaces.ArbolBinarioBusqueda;
import co.edu.uniquindio.fintech.structures.interfaces.Cola;
import co.edu.uniquindio.fintech.structures.interfaces.ColaPrioridad;
import co.edu.uniquindio.fintech.structures.interfaces.Comparador;
import co.edu.uniquindio.fintech.structures.interfaces.GrafoDirigidoPonderado;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import co.edu.uniquindio.fintech.structures.interfaces.Pila;
import co.edu.uniquindio.fintech.structures.interfaces.TablaHash;
import co.edu.uniquindio.fintech.structures.propias.ArbolBinarioBusquedaImpl;
import co.edu.uniquindio.fintech.structures.propias.GrafoDirigidoPonderadoImpl;
import co.edu.uniquindio.fintech.structures.propias.ListaEnlazada;

/** Implementación de {@link FabricaEstructuras} con las estructuras de las fases 2 y 3. */
public class FabricaEstructurasPropias implements FabricaEstructuras {

    @Override
    public <T> Lista<T> nuevaLista() {
        return new ListaEnlazada<>();
    }

    @Override
    public <T> Pila<T> nuevaPila() {
        return new PilaAdaptador<>();
    }

    @Override
    public <T> Cola<T> nuevaCola() {
        return new ColaAdaptador<>();
    }

    @Override
    public <T> ColaPrioridad<T> nuevaColaPrioridad(Comparador<T> comparador) {
        return new ColaPrioridadAdaptador<>(comparador);
    }

    @Override
    public <K, V> TablaHash<K, V> nuevaTablaHash() {
        return new TablaHashAdaptador<>();
    }

    @Override
    public <K extends Comparable<? super K>, V> ArbolBinarioBusqueda<K, V> nuevoArbol() {
        return new ArbolBinarioBusquedaImpl<>();
    }

    @Override
    public <V> GrafoDirigidoPonderado<V> nuevoGrafo() {
        return new GrafoDirigidoPonderadoImpl<>();
    }
}
