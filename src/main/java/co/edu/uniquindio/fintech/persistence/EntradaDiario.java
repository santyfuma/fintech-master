package co.edu.uniquindio.fintech.persistence;

import java.time.LocalDateTime;

/**
 * Operación preparada en el diario de rehacer (CONTEXTO.md 7.6).
 *
 * @param id       identificador de la operación (normalmente el de su transacción)
 * @param fecha    fecha y hora de preparación
 * @param estado   siempre PREPARADA mientras está en el diario
 * @param imagenes imágenes finales de los registros modificados o nuevos, en el orden
 *                 en que deben aplicarse
 */
public record EntradaDiario(String id, LocalDateTime fecha, EstadoEntradaDiario estado, ImagenRegistro[] imagenes) {

    public EntradaDiario {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("La entrada del diario no tiene identificador");
        }
        if (fecha == null || estado == null) {
            throw new IllegalArgumentException("La entrada " + id + " no tiene fecha o estado");
        }
        if (imagenes == null || imagenes.length == 0) {
            throw new IllegalArgumentException("La entrada " + id + " no tiene imágenes");
        }
        for (ImagenRegistro imagen : imagenes) {
            if (imagen == null) {
                throw new IllegalArgumentException("La entrada " + id + " tiene una imagen null");
            }
        }
        imagenes = imagenes.clone();
    }

    /** @return copia de las imágenes (el arreglo interno no se expone) */
    @Override
    public ImagenRegistro[] imagenes() {
        return imagenes.clone();
    }
}
