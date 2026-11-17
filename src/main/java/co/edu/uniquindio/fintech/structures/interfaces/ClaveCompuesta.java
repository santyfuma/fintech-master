package co.edu.uniquindio.fintech.structures.interfaces;

import java.util.Objects;

/**
 * Clave compuesta para {@link ArbolBinarioBusqueda} que permite repetir la parte
 * principal (por ejemplo, puntos) manteniendo claves únicas gracias a la parte de
 * desempate (por ejemplo, la identificación del usuario).
 *
 * <p>Orden: primero por {@code principal}; si coincide, por {@code desempate}.
 *
 * <p>Además de las claves normales ({@link #de}) existen <b>cotas</b>
 * ({@link #cotaInferior}, {@link #cotaSuperior}) que solo sirven como límites de
 * {@link ArbolBinarioBusqueda#rango}: una cota inferior es menor que cualquier clave
 * con la misma parte principal y una cota superior es mayor. Las cotas no deben
 * insertarse en el árbol.
 *
 * <p>Forma parte del contrato del ABB (no es lógica del árbol), por eso vive en
 * este paquete.
 *
 * @param <P> tipo de la parte principal (criterio de orden)
 * @param <D> tipo de la parte de desempate
 */
public final class ClaveCompuesta<P extends Comparable<? super P>, D extends Comparable<? super D>>
        implements Comparable<ClaveCompuesta<P, D>> {

    private static final int COTA_INFERIOR = -1;
    private static final int CLAVE = 0;
    private static final int COTA_SUPERIOR = 1;

    private final P principal;
    private final D desempate;
    private final int tipo;

    private ClaveCompuesta(P principal, D desempate, int tipo) {
        this.principal = Objects.requireNonNull(principal, "principal");
        this.desempate = desempate;
        this.tipo = tipo;
    }

    /** Crea una clave normal, apta para insertar en el árbol. */
    public static <P extends Comparable<? super P>, D extends Comparable<? super D>>
    ClaveCompuesta<P, D> de(P principal, D desempate) {
        return new ClaveCompuesta<>(principal, Objects.requireNonNull(desempate, "desempate"), CLAVE);
    }

    /** Crea una cota menor que toda clave con esa parte principal. Solo para consultas. */
    public static <P extends Comparable<? super P>, D extends Comparable<? super D>>
    ClaveCompuesta<P, D> cotaInferior(P principal) {
        return new ClaveCompuesta<>(principal, null, COTA_INFERIOR);
    }

    /** Crea una cota mayor que toda clave con esa parte principal. Solo para consultas. */
    public static <P extends Comparable<? super P>, D extends Comparable<? super D>>
    ClaveCompuesta<P, D> cotaSuperior(P principal) {
        return new ClaveCompuesta<>(principal, null, COTA_SUPERIOR);
    }

    public P principal() {
        return principal;
    }

    /** @return la parte de desempate, o {@code null} si es una cota */
    public D desempate() {
        return desempate;
    }

    /** @return {@code true} si es una cota de consulta y no una clave insertable */
    public boolean esCota() {
        return tipo != CLAVE;
    }

    @Override
    public int compareTo(ClaveCompuesta<P, D> otra) {
        int porPrincipal = principal.compareTo(otra.principal);
        if (porPrincipal != 0) {
            return porPrincipal;
        }
        if (tipo != otra.tipo) {
            return Integer.compare(tipo, otra.tipo);
        }
        if (tipo != CLAVE) {
            return 0;
        }
        return desempate.compareTo(otra.desempate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClaveCompuesta<?, ?> otra)) {
            return false;
        }
        return tipo == otra.tipo
                && principal.equals(otra.principal)
                && Objects.equals(desempate, otra.desempate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(principal, desempate, tipo);
    }

    @Override
    public String toString() {
        return switch (tipo) {
            case COTA_INFERIOR -> "(" + principal + ", -inf)";
            case COTA_SUPERIOR -> "(" + principal + ", +inf)";
            default -> "(" + principal + ", " + desempate + ")";
        };
    }
}
