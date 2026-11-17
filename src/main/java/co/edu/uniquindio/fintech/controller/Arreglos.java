package co.edu.uniquindio.fintech.controller;

import co.edu.uniquindio.fintech.structures.interfaces.Lista;

/** Convierte las listas de los servicios en los arreglos que usan los DTO de respuesta. */
final class Arreglos {

    interface Conversion<T, R> {
        R aplicar(T elemento);
    }

    interface Creacion<R> {
        R[] crear(int tamanio);
    }

    private Arreglos() {
    }

    static <T, R> R[] de(Lista<T> lista, Conversion<T, R> conversion, Creacion<R> creacion) {
        R[] resultado = creacion.crear(lista.tamanio());
        int i = 0;
        for (T elemento : lista) {
            resultado[i++] = conversion.aplicar(elemento);
        }
        return resultado;
    }

    static String[] textos(Lista<String> lista) {
        return de(lista, x -> x, String[]::new);
    }
}
