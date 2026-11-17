package co.edu.uniquindio.fintech.persistence;

import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Crea el {@link JsonMapper} (Jackson 3) que usan los archivos de datos.
 *
 * <p>Es propio de la persistencia y no se registra como bean, para no reemplazar el
 * mapper que Spring Boot configura para la API REST. Usa los valores por defecto de
 * Jackson 3 (fechas ISO-8601, {@code java.time} incluido) más salida indentada para que
 * los archivos sean legibles.
 */
public final class MapeadorJson {

    private MapeadorJson() {
    }

    public static JsonMapper crear() {
        return JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
    }
}
