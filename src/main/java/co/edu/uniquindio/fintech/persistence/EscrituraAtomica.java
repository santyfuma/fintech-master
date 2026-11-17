package co.edu.uniquindio.fintech.persistence;

import co.edu.uniquindio.fintech.exception.PersistenciaException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Escribe un archivo completo sin dejarlo nunca a medio escribir.
 *
 * <ol>
 *   <li>Crea el directorio si no existe.</li>
 *   <li>Escribe todo el contenido en un archivo temporal del <b>mismo directorio</b>
 *       ({@value #PREFIJO_TEMPORAL}&lt;nombre&gt;-…{@value #SUFIJO_TEMPORAL}) y fuerza su
 *       volcado a disco ({@code FileChannel.force}).</li>
 *   <li>Reemplaza el destino con {@code Files.move(ATOMIC_MOVE)}: en un mismo sistema de
 *       archivos es un renombrado atómico; quien lea verá el contenido anterior o el
 *       nuevo, nunca una mezcla.</li>
 *   <li>Si el sistema de archivos no admite movimiento atómico
 *       ({@link AtomicMoveNotSupportedException}), usa {@code Files.move(REPLACE_EXISTING)}.
 *       El temporal ya está completo, así que el destino tampoco queda parcial; lo que
 *       se pierde es la garantía de que el reemplazo sea indivisible (en un corte justo
 *       en ese instante el destino podría faltar, y el diario de operaciones permite
 *       rehacer la operación).</li>
 *   <li>Ante cualquier fallo borra el temporal y lanza {@link PersistenciaException};
 *       el destino conserva su contenido anterior.</li>
 * </ol>
 *
 * <p>No sincroniza el directorio tras el renombrado (Java no ofrece una forma portable),
 * por lo que tras un corte de energía el renombrado podría no haberse hecho persistente;
 * en ese caso el archivo conserva la versión anterior completa.
 *
 * <p>Los métodos de movimiento son {@code protected} para que las pruebas simulen fallos.
 */
public class EscrituraAtomica {

    public static final String PREFIJO_TEMPORAL = ".fintech-";
    public static final String SUFIJO_TEMPORAL = ".tmp";

    /** Escribe {@code contenido} en {@code destino} de forma atómica. */
    public void escribir(Path destino, byte[] contenido) {
        if (destino == null || contenido == null) {
            throw new NullPointerException("destino y contenido son obligatorios");
        }
        Path directorio = destino.toAbsolutePath().getParent();
        try {
            Files.createDirectories(directorio);
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo crear el directorio de datos " + directorio + ": " + e, e);
        }
        Path temporal = null;
        try {
            temporal = Files.createTempFile(directorio, PREFIJO_TEMPORAL + destino.getFileName() + "-", SUFIJO_TEMPORAL);
            try (FileChannel canal = FileChannel.open(temporal, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(contenido);
                while (buffer.hasRemaining()) {
                    canal.write(buffer);
                }
                canal.force(true);
            }
            reemplazar(temporal, destino);
            temporal = null;
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo escribir el archivo " + destino + ": " + e, e);
        } finally {
            if (temporal != null) {
                borrarSinFallar(temporal);
            }
        }
    }

    /**
     * Borra los temporales que haya dejado una escritura interrumpida. Son copias
     * descartables: su escritura nunca se confirmó (ver CONTEXTO.md 7.6).
     *
     * @return cantidad de temporales borrados (0 si el directorio no existe)
     */
    public int limpiarTemporales(Path directorio) {
        if (!Files.isDirectory(directorio)) {
            return 0;
        }
        int borrados = 0;
        try (DirectoryStream<Path> temporales =
                     Files.newDirectoryStream(directorio, PREFIJO_TEMPORAL + "*" + SUFIJO_TEMPORAL)) {
            for (Path temporal : temporales) {
                Files.deleteIfExists(temporal);
                borrados++;
            }
        } catch (IOException e) {
            throw new PersistenciaException("No se pudieron limpiar los temporales de " + directorio + ": " + e, e);
        }
        return borrados;
    }

    private void reemplazar(Path temporal, Path destino) throws IOException {
        try {
            moverAtomico(temporal, destino);
        } catch (AtomicMoveNotSupportedException e) {
            moverReemplazando(temporal, destino);
        }
    }

    /** Renombrado atómico del temporal sobre el destino. */
    protected void moverAtomico(Path temporal, Path destino) throws IOException {
        Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Alternativa cuando el sistema de archivos no admite movimiento atómico. */
    protected void moverReemplazando(Path temporal, Path destino) throws IOException {
        Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void borrarSinFallar(Path temporal) {
        try {
            Files.deleteIfExists(temporal);
        } catch (IOException ignorado) {
            // Se reintenta en el próximo arranque con limpiarTemporales.
        }
    }
}
