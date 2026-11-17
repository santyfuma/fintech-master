package co.edu.uniquindio.fintech.structures.adaptadores;

import java.util.Iterator;

/**
 * Envuelve el iterador de una colección de {@code java.util} para que no permita
 * {@code remove()}. Así los adaptadores no exponen una vía para modificar la
 * estructura interna saltándose su contrato.
 */
final class IteradorSoloLectura<T> implements Iterator<T> {

    private final Iterator<T> interno;

    IteradorSoloLectura(Iterator<T> interno) {
        this.interno = interno;
    }

    @Override
    public boolean hasNext() {
        return interno.hasNext();
    }

    @Override
    public T next() {
        return interno.next();
    }
}
