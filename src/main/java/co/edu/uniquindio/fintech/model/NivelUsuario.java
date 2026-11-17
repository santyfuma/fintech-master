package co.edu.uniquindio.fintech.model;

/**
 * Nivel de fidelización de un usuario, con los rangos de puntos del enunciado (4.5):
 * Bronce 0–500, Plata 501–1000, Oro 1001–5000 y Platino más de 5000.
 *
 * <p>Solo guarda los límites de cada rango (datos del enunciado). Asignar el nivel a un
 * usuario es una regla de negocio que corresponde a {@code service}. Las ventajas de
 * cada nivel siguen pendientes (CONTEXTO.md, sección 11).
 */
public enum NivelUsuario {
    BRONCE(0, 500),
    PLATA(501, 1000),
    ORO(1001, 5000),
    PLATINO(5001, Long.MAX_VALUE);

    private final long puntosMinimos;
    private final long puntosMaximos;

    NivelUsuario(long puntosMinimos, long puntosMaximos) {
        this.puntosMinimos = puntosMinimos;
        this.puntosMaximos = puntosMaximos;
    }

    /** @return límite inferior del rango, incluido */
    public long puntosMinimos() {
        return puntosMinimos;
    }

    /** @return límite superior del rango, incluido ({@link Long#MAX_VALUE} para PLATINO) */
    public long puntosMaximos() {
        return puntosMaximos;
    }
}
