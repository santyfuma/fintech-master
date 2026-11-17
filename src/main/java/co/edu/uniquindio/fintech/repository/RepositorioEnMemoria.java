package co.edu.uniquindio.fintech.repository;

import co.edu.uniquindio.fintech.exception.PersistenciaException;
import co.edu.uniquindio.fintech.persistence.AlmacenJson;
import co.edu.uniquindio.fintech.persistence.CerrojoEscritura;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import co.edu.uniquindio.fintech.structures.interfaces.TablaHash;

/**
 * Base de los repositorios: tabla hash principal por identificador, orden de registro y
 * persistencia del archivo completo en cada cambio.
 *
 * <p><b>Coherencia:</b> en cada cambio primero se escribe el archivo con el estado
 * resultante y solo si eso funciona se modifica la memoria. Si la escritura falla,
 * memoria y archivo siguen como antes.
 *
 * <p><b>Mismo resultado al reconstruir:</b> la carga inicial ({@link #cargarIniciales})
 * pasa cada registro por el mismo método {@link #indexar} que un registro nuevo, en el
 * orden del archivo; los índices quedan idénticos a los de haber insertado uno por uno.
 *
 * <p><b>Concurrencia:</b> todos los métodos públicos se sincronizan con el
 * {@link CerrojoEscritura} compartido (lecturas incluidas, porque las estructuras no son
 * seguras entre hilos).
 *
 * <p>{@link #reflejar} actualiza solo la memoria; lo usan los servicios después de que el
 * diario de operaciones escribió el archivo.
 *
 * <p>Complejidad: buscar por identificador O(1) promedio; registrar, actualizar o
 * eliminar O(n) por la reescritura del archivo completo, más el costo de los índices.
 *
 * @param <T> tipo de registro
 */
public abstract class RepositorioEnMemoria<T> {

    protected final FabricaEstructuras fabrica;
    private final AlmacenJson<T> almacen;
    private final CerrojoEscritura cerrojo;
    private final TablaHash<String, T> porIdentificador;
    private final Lista<String> orden;

    protected RepositorioEnMemoria(AlmacenJson<T> almacen, FabricaEstructuras fabrica, CerrojoEscritura cerrojo) {
        if (almacen == null || fabrica == null || cerrojo == null) {
            throw new NullPointerException("almacen, fabrica y cerrojo son obligatorios");
        }
        this.almacen = almacen;
        this.fabrica = fabrica;
        this.cerrojo = cerrojo;
        this.porIdentificador = fabrica.nuevaTablaHash();
        this.orden = fabrica.nuevaLista();
    }

    // ---------------------------------------------------------------- ganchos de cada repositorio

    protected abstract String identificadorDe(T registro);

    /** Agrega el registro a los índices secundarios. */
    protected abstract void indexar(T registro);

    /** Retira el registro de los índices secundarios. */
    protected abstract void desindexar(T registro);

    /** Sustituye una versión por otra en los índices; por defecto, desindexar e indexar. */
    protected void reindexar(T anterior, T nuevo) {
        desindexar(anterior);
        indexar(nuevo);
    }

    /**
     * Integridad referencial que el repositorio puede comprobar (p. ej. que exista la
     * billetera). No incluye reglas de negocio.
     *
     * @throws IllegalStateException si el registro referencia algo inexistente
     */
    protected void verificar(T registro) {
    }

    // ---------------------------------------------------------------- carga inicial

    /**
     * Indexa los registros leídos del archivo. Lo llama el constructor de cada
     * repositorio cuando sus índices ya existen.
     *
     * @throws PersistenciaException si hay identificadores repetidos o referencias inválidas
     */
    protected final void cargarIniciales(Lista<T> registros) {
        synchronized (cerrojo) {
            for (T registro : registros) {
                String id = identificadorDe(registro);
                if (porIdentificador.contieneClave(id)) {
                    throw new PersistenciaException("Registro repetido " + id + " en " + almacen.nombreArchivo());
                }
                try {
                    verificar(registro);
                } catch (IllegalStateException e) {
                    throw new PersistenciaException(e.getMessage() + " (" + almacen.nombreArchivo() + ")", e);
                }
                agregarEnMemoria(registro);
            }
        }
    }

    // ---------------------------------------------------------------- consultas comunes

    /** @return el registro, o null si no existe */
    public T buscar(String identificador) {
        requerido(identificador, "identificador");
        synchronized (cerrojo) {
            return porIdentificador.obtener(identificador);
        }
    }

    public boolean existe(String identificador) {
        requerido(identificador, "identificador");
        synchronized (cerrojo) {
            return porIdentificador.contieneClave(identificador);
        }
    }

    public int cantidad() {
        synchronized (cerrojo) {
            return porIdentificador.tamanio();
        }
    }

    /** @return copia de todos los registros en orden de registro (cronológico) */
    public Lista<T> todos() {
        synchronized (cerrojo) {
            return copiaEnOrden();
        }
    }

    /** Actualiza solo la memoria (alta o reemplazo) tras una escritura hecha por el diario. */
    public void reflejar(T registro) {
        requerido(registro, "registro");
        synchronized (cerrojo) {
            verificar(registro);
            T anterior = porIdentificador.obtener(identificadorDe(registro));
            if (anterior == null) {
                agregarEnMemoria(registro);
            } else {
                porIdentificador.poner(identificadorDe(registro), registro);
                reindexar(anterior, registro);
            }
        }
    }

    // ---------------------------------------------------------------- operaciones para subclases

    /** @throws IllegalStateException si ya existe un registro con ese identificador */
    protected final T registrarRegistro(T registro) {
        requerido(registro, "registro");
        synchronized (cerrojo) {
            String id = identificadorDe(registro);
            if (porIdentificador.contieneClave(id)) {
                throw new IllegalStateException("Ya existe un registro " + id + " en " + almacen.nombreArchivo());
            }
            verificar(registro);
            Lista<T> resultado = copiaEnOrden();
            resultado.agregar(registro);
            almacen.guardar(resultado);
            agregarEnMemoria(registro);
            return registro;
        }
    }

    /**
     * @return la versión anterior
     * @throws IllegalStateException si el registro no existe
     */
    protected final T actualizarRegistro(T registro) {
        requerido(registro, "registro");
        synchronized (cerrojo) {
            String id = identificadorDe(registro);
            T anterior = porIdentificador.obtener(id);
            if (anterior == null) {
                throw new IllegalStateException("No existe el registro " + id + " en " + almacen.nombreArchivo());
            }
            verificar(registro);
            Lista<T> resultado = fabrica.nuevaLista();
            for (String clave : orden) {
                resultado.agregar(clave.equals(id) ? registro : porIdentificador.obtener(clave));
            }
            almacen.guardar(resultado);
            porIdentificador.poner(id, registro);
            reindexar(anterior, registro);
            return anterior;
        }
    }

    /** @return el registro eliminado, o null si no existía */
    protected final T eliminarRegistro(String identificador) {
        requerido(identificador, "identificador");
        synchronized (cerrojo) {
            T anterior = porIdentificador.obtener(identificador);
            if (anterior == null) {
                return null;
            }
            Lista<T> resultado = fabrica.nuevaLista();
            for (String clave : orden) {
                if (!clave.equals(identificador)) {
                    resultado.agregar(porIdentificador.obtener(clave));
                }
            }
            almacen.guardar(resultado);
            porIdentificador.eliminar(identificador);
            orden.eliminar(identificador);
            desindexar(anterior);
            return anterior;
        }
    }

    protected final CerrojoEscritura cerrojo() {
        return cerrojo;
    }

    /** Búsqueda interna (sin sincronizar: usar dentro del cerrojo). */
    protected final T registroPorIdentificador(String identificador) {
        return porIdentificador.obtener(identificador);
    }

    /** Convierte identificadores en registros, conservando el orden. */
    protected final Lista<T> resolver(Lista<String> identificadores) {
        Lista<T> registros = fabrica.nuevaLista();
        for (String id : identificadores) {
            T registro = porIdentificador.obtener(id);
            if (registro != null) {
                registros.agregar(registro);
            }
        }
        return registros;
    }

    protected final Lista<T> copiaEnOrden() {
        return resolver(orden);
    }

    protected static void requerido(Object valor, String nombre) {
        if (valor == null) {
            throw new NullPointerException(nombre);
        }
    }

    private void agregarEnMemoria(T registro) {
        String id = identificadorDe(registro);
        porIdentificador.poner(id, registro);
        orden.agregar(id);
        indexar(registro);
    }
}
