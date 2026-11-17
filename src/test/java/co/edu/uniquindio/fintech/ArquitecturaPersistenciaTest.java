package co.edu.uniquindio.fintech;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Reglas de la fase 5 (complementa ArquitecturaTest y ArquitecturaCapasTest):
 * <ul>
 *   <li>{@code persistence} no depende de repository, service, controller ni config.</li>
 *   <li>{@code repository} no depende de service, controller ni config.</li>
 *   <li>Solo {@code persistence} usa Jackson, y solo Jackson 3 ({@code tools.jackson}).</li>
 * </ul>
 */
class ArquitecturaPersistenciaTest {

    private static final Path RAIZ = Path.of("src/main/java/co/edu/uniquindio/fintech");
    private static final String BASE = "co.edu.uniquindio.fintech.";

    @Test
    void dependenciasDePersistenciaYRepositorios() throws IOException {
        StringBuilder violaciones = new StringBuilder();
        prohibir("persistence", new String[]{"repository", "service", "controller", "config"}, violaciones);
        prohibir("repository", new String[]{"service", "controller", "config"}, violaciones);
        assertEquals("", violaciones.toString(), "Violaciones:\n" + violaciones);
    }

    @Test
    void soloPersistenciaUsaJacksonYEsJackson3() throws IOException {
        StringBuilder violaciones = new StringBuilder();
        try (var archivos = Files.walk(RAIZ)) {
            archivos.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                String paquete = RAIZ.relativize(p.getParent()).toString().replace('\\', '/');
                for (String linea : leer(p).split("\\R")) {
                    String l = linea.trim();
                    if (l.startsWith("import com.fasterxml.jackson.databind") || l.startsWith("import com.fasterxml.jackson.core")) {
                        violaciones.append("[Jackson 2] ").append(p).append(": ").append(l).append('\n');
                    }
                    if (l.startsWith("import tools.jackson") && !paquete.equals("persistence")) {
                        violaciones.append("[Jackson fuera de persistence] ").append(p).append(": ").append(l).append('\n');
                    }
                }
            });
        }
        assertEquals("", violaciones.toString(), "Violaciones:\n" + violaciones);
    }

    private static void prohibir(String paquete, String[] prohibidos, StringBuilder violaciones) throws IOException {
        try (var archivos = Files.list(RAIZ.resolve(paquete))) {
            archivos.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                for (String linea : leer(p).split("\\R")) {
                    for (String prohibido : prohibidos) {
                        if (linea.trim().startsWith("import " + BASE + prohibido + ".")) {
                            violaciones.append("[").append(paquete).append(" importa ").append(prohibido).append("] ")
                                    .append(p).append('\n');
                        }
                    }
                }
            });
        }
    }

    private static String leer(Path archivo) {
        try {
            return Files.readString(archivo);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
