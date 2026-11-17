package co.edu.uniquindio.fintech.repository;

import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import co.edu.uniquindio.fintech.structures.interfaces.TablaHash;

/**
 * Índice secundario: una lista de identificadores por clave (p. ej. historial de
 * transacciones de una billetera), en una tabla hash.
 *
 * <p>Cada lista se mantiene en orden ascendente de consecutivo (= orden cronológico) y
 * sin repetidos. Como los identificadores llegan casi siempre en orden, insertar es O(1)
 * en el caso normal (se compara con el último) y O(k) en el peor caso.
 */
final class IndicePorClave {

    private final FabricaEstructuras fabrica;
    private final TablaHash<String, Lista<String>> listas;

    IndicePorClave(FabricaEstructuras fabrica) {
        this.fabrica = fabrica;
        this.listas = fabrica.nuevaTablaHash();
    }

    void agregar(String clave, String identificador) {
        Lista<String> lista = listas.obtener(clave);
        if (lista == null) {
            lista = fabrica.nuevaLista();
            listas.poner(clave, lista);
        }
        int posicion = lista.tamanio();
        while (posicion > 0) {
            int comparacion = compararConsecutivos(lista.obtener(posicion - 1), identificador);
            if (comparacion == 0) {
                return; // ya estaba: sin duplicados
            }
            if (comparacion < 0) {
                break;
            }
            posicion--;
        }
        lista.agregarEn(posicion, identificador);
    }

    void eliminar(String clave, String identificador) {
        Lista<String> lista = listas.obtener(clave);
        if (lista != null) {
            lista.eliminar(identificador);
            if (lista.estaVacia()) {
                listas.eliminar(clave);
            }
        }
    }

    /** @return copia de los identificadores de la clave, en orden cronológico */
    Lista<String> identificadores(String clave) {
        Lista<String> copia = fabrica.nuevaLista();
        Lista<String> lista = listas.obtener(clave);
        if (lista != null) {
            for (String id : lista) {
                copia.agregar(id);
            }
        }
        return copia;
    }

    /**
     * Compara identificadores generados del mismo prefijo por su consecutivo: un número
     * con más dígitos es mayor; con igual cantidad, el orden alfabético coincide con el
     * numérico porque están rellenos con ceros.
     */
    static int compararConsecutivos(String a, String b) {
        if (a.length() != b.length()) {
            return Integer.compare(a.length(), b.length());
        }
        return a.compareTo(b);
    }
}
