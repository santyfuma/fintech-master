package co.edu.uniquindio.fintech.model;

/**
 * Estado de un beneficio canjeado.
 *
 * <p><b>Provisional, pendiente de confirmación:</b> el enunciado no define el ciclo de
 * vida de un canje. ANULADO permite representar un canje deshecho sin borrar el registro.
 */
public enum EstadoCanje {
    REALIZADO,
    ANULADO
}
