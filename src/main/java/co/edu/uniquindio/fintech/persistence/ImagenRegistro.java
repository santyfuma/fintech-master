package co.edu.uniquindio.fintech.persistence;

/**
 * Imagen final completa de un registro dentro de una entrada del diario.
 *
 * <p>El contenido se guarda como <b>texto JSON</b> del registro, no como árbol
 * {@code JsonNode}: al convertir a árbol, Jackson normaliza los decimales
 * ({@code 1050.00} pasa a {@code 1050.0} o {@code 1050}, e incluso a {@code double}),
 * lo que en montos cambiaría la escala o perdería precisión. Con el texto, el registro
 * se reconstruye con {@code readValue} directamente a su tipo, igual que en los
 * archivos de datos, y los {@code BigDecimal} quedan exactos.
 *
 * @param archivo       archivo de dominio al que pertenece (p. ej. {@code billeteras.json})
 * @param identificador identificador del registro (identificación, código o id)
 * @param contenido     registro completo en JSON (un objeto), suficiente para reconstruirlo
 */
public record ImagenRegistro(String archivo, String identificador, String contenido) {

    public ImagenRegistro {
        if (archivo == null || ArchivoDominio.porNombre(archivo) == null) {
            throw new IllegalArgumentException("Archivo de dominio desconocido en el diario: " + archivo);
        }
        if (identificador == null || identificador.isBlank()) {
            throw new IllegalArgumentException("La imagen de " + archivo + " no tiene identificador");
        }
        if (contenido == null || !contenido.trim().startsWith("{")) {
            throw new IllegalArgumentException("La imagen de " + identificador + " no contiene un objeto JSON");
        }
    }
}
