package co.edu.uniquindio.fintech;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifica las reglas de dependencia de docs/CONTEXTO.md leyendo el código fuente.
 * No usa java.util para cumplir la misma regla que exige.
 */
class ArquitecturaTest {

    private static final Path RAIZ = Path.of("src/main/java/co/edu/uniquindio/fintech");
    private static final String BASE = "co.edu.uniquindio.fintech.";

    @Test
    void lasReglasDeDependenciaSeCumplen() throws IOException {
        StringBuilder violaciones = new StringBuilder();
        try (var archivos = Files.walk(RAIZ)) {
            archivos.filter(p -> p.toString().endsWith(".java"))
                    .forEach(p -> revisar(p, violaciones));
        }
        assertEquals("", violaciones.toString(), "Violaciones de arquitectura:\n" + violaciones);
    }

    private static void revisar(Path archivo, StringBuilder violaciones) {
        String paquete = RAIZ.relativize(archivo.getParent()).toString().replace('\\', '/');
        boolean enStructures = paquete.equals("structures") || paquete.startsWith("structures/");
        String[] lineas;
        try {
            lineas = Files.readString(archivo).split("\\R");
        } catch (IOException e) {
            violaciones.append(archivo).append(": no se pudo leer\n");
            return;
        }
        for (int i = 0; i < lineas.length; i++) {
            String linea = lineas[i].trim();
            if (linea.startsWith("*") || linea.startsWith("/*") || linea.startsWith("//")) {
                continue;
            }
            String ubicacion = archivo + ":" + (i + 1) + " -> " + linea + "\n";

            // Regla 8: java.util solo dentro de structures.
            if (!enStructures && linea.contains("java.util")) {
                violaciones.append("[java.util fuera de structures] ").append(ubicacion);
            }
            // Reglas 1 y 2: structures no depende de ningún otro paquete de la aplicación.
            if (enStructures && linea.startsWith("import") && linea.contains(BASE)
                    && !linea.contains(BASE + "structures")) {
                violaciones.append("[structures depende del dominio] ").append(ubicacion);
            }
            // Regla 7: service y repository dependen de interfaces, no de implementaciones.
            if ((paquete.startsWith("service") || paquete.startsWith("repository"))
                    && (linea.contains(BASE + "structures.adaptadores")
                    || linea.contains(BASE + "structures.propias"))) {
                violaciones.append("[implementación concreta en service/repository] ").append(ubicacion);
            }
            // Regla 10: los controladores solo hablan con servicios.
            if (paquete.startsWith("controller")
                    && (linea.contains(BASE + "repository") || linea.contains(BASE + "persistence"))) {
                violaciones.append("[controller accede a repository/persistence] ").append(ubicacion);
            }
            // Regla 13: sin JPA ni Hibernate.
            if (linea.contains("jakarta.persistence") || linea.contains("org.hibernate")) {
                violaciones.append("[JPA/Hibernate prohibido] ").append(ubicacion);
            }
        }
    }
}
