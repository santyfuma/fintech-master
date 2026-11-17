package co.edu.uniquindio.fintech.persistence;

import co.edu.uniquindio.fintech.structures.interfaces.Lista;

/**
 * Diario de rehacer para operaciones que modifican varios archivos (CONTEXTO.md 7.6).
 *
 * <p>Flujo que usarán los servicios ({@link #ejecutar} lo hace completo):
 * <ol>
 *   <li>Preparar: calcular en memoria las imágenes finales ({@link #preparar}).</li>
 *   <li>Registrar la entrada en {@code diario-operaciones.json} ({@link #registrar}).</li>
 *   <li>Aplicar las imágenes a cada archivo ({@link #aplicar}).</li>
 *   <li>Actualizar la memoria y confirmar, retirando la entrada ({@link #confirmar}).</li>
 * </ol>
 * Al iniciar, {@link #recuperarPendientes} rehace las entradas que quedaron sin confirmar.
 *
 * <p><b>Alcance:</b> no ofrece atomicidad física entre varios archivos. Cada archivo se
 * escribe de forma atómica y el diario permite <b>rehacer</b> una operación
 * interrumpida, porque aplicar una imagen (reemplazar un registro por su identificador o
 * agregarlo) es idempotente.
 */
public interface DiarioOperaciones {

    /**
     * Construye una entrada PREPARADA con la imagen de cada registro (entidades de
     * {@code model}); el archivo se deduce del tipo de cada registro.
     */
    EntradaDiario preparar(String idOperacion, Object... registros);

    /**
     * Agrega la entrada al diario.
     *
     * @throws IllegalStateException si ya hay una entrada con el mismo identificador
     */
    void registrar(EntradaDiario entrada);

    /** Aplica las imágenes a sus archivos (idempotente). No modifica el diario. */
    void aplicar(EntradaDiario entrada);

    /** Retira la entrada del diario. Si no existe, no hace nada. */
    void confirmar(String idOperacion);

    /**
     * Flujo completo: registrar, aplicar (con un reintento), ejecutar
     * {@code actualizarMemoria} y confirmar, todo bajo el cerrojo de escritura. Si la
     * aplicación falla dos veces, la entrada queda en el diario, la memoria no se toca y
     * se lanza una {@code PersistenciaException} que indica que la operación se
     * completará en el próximo arranque.
     */
    void ejecutar(EntradaDiario entrada, Runnable actualizarMemoria);

    /**
     * Rehace y retira, en orden, todas las entradas pendientes. Se llama al iniciar,
     * antes de cargar los repositorios.
     *
     * @return cantidad de entradas recuperadas
     * @throws co.edu.uniquindio.fintech.exception.PersistenciaException si el diario está
     *         corrupto o alguna entrada no se puede aplicar (esa y las siguientes quedan
     *         en el diario)
     */
    int recuperarPendientes();

    /** @return entradas pendientes, en orden */
    Lista<EntradaDiario> pendientes();
}
