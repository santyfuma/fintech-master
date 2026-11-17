package co.edu.uniquindio.fintech.persistence;

/**
 * Estado de una entrada del diario. Solo existe PREPARADA: una entrada se retira del
 * diario en cuanto se confirma, así que en el archivo nunca hay entradas terminadas.
 */
public enum EstadoEntradaDiario {
    PREPARADA
}
