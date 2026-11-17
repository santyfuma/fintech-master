package co.edu.uniquindio.fintech.soporte;

import co.edu.uniquindio.fintech.exception.FintechException;
import org.junit.jupiter.api.function.Executable;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Afirmaciones comunes de las pruebas de servicios. */
public final class Afirmaciones {

    private Afirmaciones() {
    }

    /** Verifica el tipo y el código de negocio de la excepción lanzada. */
    public static <T extends FintechException> T lanza(Class<T> tipo, String codigo, Executable accion) {
        T e = assertThrows(tipo, accion);
        assertEquals(codigo, e.codigo(), "código de la excepción");
        return e;
    }

    /** Igualdad exacta de BigDecimal incluida la escala (100.00 ≠ 100). */
    public static void dinero(String esperado, BigDecimal real) {
        assertEquals(new BigDecimal(esperado), real, "valor monetario (con escala)");
    }
}
