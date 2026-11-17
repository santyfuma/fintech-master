package co.edu.uniquindio.fintech;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Complementa {@code ArquitecturaTest} (fase 1) con las reglas de las capas de la fase 4:
 * <ul>
 *   <li>{@code model} solo importa {@code java.math} y {@code java.time}: sin Spring,
 *       Jackson, Bean Validation, estructuras ni otras capas; y no tiene setters.</li>
 *   <li>{@code dto} solo importa {@code model}, {@code jakarta.validation},
 *       {@code java.math} y {@code java.time}, y todos sus tipos son records.</li>
 *   <li>{@code exception} no depende de service, repository, persistence, controller
 *       ni structures.</li>
 * </ul>
 */
class ArquitecturaCapasTest {

    private static final Path RAIZ = Path.of("src/main/java/co/edu/uniquindio/fintech");
    private static final String BASE = "co.edu.uniquindio.fintech.";

    @Test
    void modelSoloDependeDeJavaMathYJavaTime() throws IOException {
        StringBuilder violaciones = new StringBuilder();
        revisarImportaciones("model", new String[]{"java.math.", "java.time."}, violaciones);
        revisarContenido("model", "void set", "setter en una entidad", violaciones);
        assertEquals("", violaciones.toString(), "Violaciones:\n" + violaciones);
    }

    @Test
    void dtoSoloDependeDeModelValidacionYJava() throws IOException {
        StringBuilder violaciones = new StringBuilder();
        revisarImportaciones("dto", new String[]{BASE + "model.", "jakarta.validation.", "java.math.", "java.time."},
                violaciones);
        try (var archivos = Files.list(RAIZ.resolve("dto"))) {
            archivos.filter(p -> p.toString().endsWith(".java") && !p.endsWith("package-info.java"))
                    .forEach(p -> {
                        if (!leer(p).contains("public record ")) {
                            violaciones.append("[DTO que no es record] ").append(p).append('\n');
                        }
                    });
        }
        assertEquals("", violaciones.toString(), "Violaciones:\n" + violaciones);
    }

    @Test
    void exceptionNoDependeDeCapasSuperioresNiDeEstructuras() throws IOException {
        StringBuilder violaciones = new StringBuilder();
        for (String prohibido : new String[]{"service", "repository", "persistence", "controller", "structures"}) {
            revisarContenido("exception", "import " + BASE + prohibido, "exception depende de " + prohibido,
                    violaciones);
        }
        assertEquals("", violaciones.toString(), "Violaciones:\n" + violaciones);
    }

    private static void revisarImportaciones(String paquete, String[] permitidos, StringBuilder violaciones)
            throws IOException {
        try (var archivos = Files.list(RAIZ.resolve(paquete))) {
            archivos.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                for (String linea : leer(p).split("\\R")) {
                    String importacion = linea.trim();
                    if (!importacion.startsWith("import ")) {
                        continue;
                    }
                    String nombre = importacion.substring("import ".length()).replace("static ", "");
                    boolean permitido = false;
                    for (String prefijo : permitidos) {
                        permitido |= nombre.startsWith(prefijo);
                    }
                    if (!permitido) {
                        violaciones.append("[").append(paquete).append(" importa ").append(nombre).append("] ")
                                .append(p).append('\n');
                    }
                }
            });
        }
    }

    private static void revisarContenido(String paquete, String texto, String motivo, StringBuilder violaciones)
            throws IOException {
        try (var archivos = Files.list(RAIZ.resolve(paquete))) {
            archivos.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                if (leer(p).contains(texto)) {
                    violaciones.append("[").append(motivo).append("] ").append(p).append('\n');
                }
            });
        }
    }

    private static String leer(Path archivo) {
        try {
            return Files.readString(archivo);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + archivo, e);
        }
    }
}
