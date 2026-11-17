package co.edu.uniquindio.fintech.persistence;

import co.edu.uniquindio.fintech.exception.PersistenciaException;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import co.edu.uniquindio.fintech.structures.propias.ListaEnlazada;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Implementación genérica de {@link AlmacenJson}: un archivo con un arreglo JSON de
 * registros de tipo {@code T}. La misma clase sirve para los siete archivos de dominio
 * y para el diario; solo cambian el nombre del archivo y el tipo.
 *
 * <ul>
 *   <li>Archivo inexistente, vacío (0 bytes o solo espacios) o {@code []} → lista vacía.</li>
 *   <li>JSON corrupto, que no sea un arreglo, con elementos {@code null} o con registros
 *       que no cumplen los invariantes del modelo → {@link PersistenciaException} con la
 *       ruta del archivo y la causa original.</li>
 *   <li>{@link #guardar} reescribe el archivo completo, conservando el orden, mediante
 *       {@link EscrituraAtomica}.</li>
 * </ul>
 *
 * <p>Se lee el archivo completo en memoria y se convierte con
 * {@code readValue(byte[], T[].class)}; las colecciones se manejan como arreglos, sin
 * listas de {@code java.util}.
 *
 * @param <T> tipo de registro
 */
public class AlmacenJsonArchivo<T> implements AlmacenJson<T> {

    private final Path archivo;
    private final Class<T[]> tipoArreglo;
    private final JsonMapper mapper;
    private final EscrituraAtomica escritura;

    public AlmacenJsonArchivo(Path directorio, String nombreArchivo, Class<T[]> tipoArreglo, JsonMapper mapper,
                              EscrituraAtomica escritura) {
        if (directorio == null || nombreArchivo == null || nombreArchivo.isBlank() || tipoArreglo == null
                || mapper == null || escritura == null) {
            throw new IllegalArgumentException("Todos los parámetros del almacén son obligatorios");
        }
        if (!tipoArreglo.isArray()) {
            throw new IllegalArgumentException("Se esperaba un tipo arreglo: " + tipoArreglo);
        }
        this.archivo = directorio.resolve(nombreArchivo);
        this.tipoArreglo = tipoArreglo;
        this.mapper = mapper;
        this.escritura = escritura;
    }

    @Override
    public String nombreArchivo() {
        return archivo.getFileName().toString();
    }

    /** @return ruta completa del archivo */
    public Path ruta() {
        return archivo;
    }

    @Override
    public Lista<T> cargar() {
        Lista<T> registros = new ListaEnlazada<>();
        if (!Files.exists(archivo)) {
            return registros;
        }
        byte[] contenido;
        try {
            contenido = Files.readAllBytes(archivo);
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo leer el archivo " + archivo + ": " + e, e);
        }
        if (estaEnBlanco(contenido)) {
            return registros;
        }
        T[] leidos;
        try {
            leidos = mapper.readValue(contenido, tipoArreglo);
        } catch (RuntimeException e) {
            throw new PersistenciaException("El archivo " + archivo + " está corrupto o no contiene un arreglo válido de "
                    + tipoArreglo.getComponentType().getSimpleName() + ": " + e.getMessage(), e);
        }
        if (leidos == null) {
            throw new PersistenciaException("El archivo " + archivo + " contiene null en lugar de un arreglo");
        }
        for (int i = 0; i < leidos.length; i++) {
            if (leidos[i] == null) {
                throw new PersistenciaException("El archivo " + archivo + " tiene un registro null en la posición " + i);
            }
            registros.agregar(leidos[i]);
        }
        return registros;
    }

    @Override
    public void guardar(Lista<T> registros) {
        if (registros == null) {
            throw new NullPointerException("registros");
        }
        Object[] arreglo = new Object[registros.tamanio()];
        int i = 0;
        for (T registro : registros) {
            if (registro == null) {
                throw new IllegalArgumentException("No se puede guardar un registro null en " + nombreArchivo());
            }
            arreglo[i++] = registro;
        }
        byte[] json;
        try {
            json = mapper.writeValueAsBytes(arreglo);
        } catch (RuntimeException e) {
            throw new PersistenciaException("No se pudo convertir a JSON el contenido de " + archivo + ": "
                    + e.getMessage(), e);
        }
        escritura.escribir(archivo, json);
    }

    private static boolean estaEnBlanco(byte[] contenido) {
        for (byte b : contenido) {
            if (b != ' ' && b != '\n' && b != '\r' && b != '\t') {
                return false;
            }
        }
        return true;
    }
}
