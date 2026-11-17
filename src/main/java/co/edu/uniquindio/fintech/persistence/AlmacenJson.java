package co.edu.uniquindio.fintech.persistence;

import co.edu.uniquindio.fintech.structures.interfaces.Lista;

/**
 * Contrato de almacenamiento de un tipo de registro en su propio archivo JSON.
 *
 * <p>Formato: el archivo contiene un arreglo JSON con todos los registros del tipo.
 * Habrá una implementación (o instancia configurada) por archivo:
 * usuarios.json, billeteras.json, transacciones.json, operaciones-programadas.json,
 * notificaciones.json, auditoria.json y beneficios.json.
 *
 * <p>Ciclo de uso previsto:
 * <ol>
 *   <li>Al iniciar la aplicación, el repositorio llama a {@link #cargar()} y construye
 *       sus estructuras en memoria.</li>
 *   <li>Toda consulta se resuelve en memoria, nunca leyendo el archivo.</li>
 *   <li>Después de cada operación que modifique datos, el repositorio llama a
 *       {@link #guardar(Lista)} con el estado completo de ese tipo.</li>
 * </ol>
 *
 * <p>Los errores de lectura o escritura se notificarán con una excepción no
 * verificada propia del paquete {@code exception} (se definirá en la fase de
 * persistencia).
 *
 * @param <T> tipo de registro almacenado
 */
public interface AlmacenJson<T> {

    /** @return nombre del archivo, por ejemplo {@code "usuarios.json"} */
    String nombreArchivo();

    /**
     * Lee todos los registros del archivo.
     *
     * @return registros en el orden del archivo; lista vacía si el archivo no existe
     *         o contiene un arreglo vacío
     */
    Lista<T> cargar();

    /**
     * Reescribe el archivo completo con los registros recibidos. La implementación
     * debe escribir primero en un archivo temporal y luego reemplazar el original,
     * para no dejar un JSON corrupto si la escritura falla a mitad.
     */
    void guardar(Lista<T> registros);
}
