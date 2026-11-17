package co.edu.uniquindio.fintech.structures.interfaces;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaveCompuestaTest {

    @Test
    void ordenaPrimeroPorPrincipalYLuegoPorDesempate() {
        assertTrue(ClaveCompuesta.de(100, "b").compareTo(ClaveCompuesta.de(200, "a")) < 0);
        assertTrue(ClaveCompuesta.de(100, "a").compareTo(ClaveCompuesta.de(100, "b")) < 0);
        assertEquals(0, ClaveCompuesta.de(100, "a").compareTo(ClaveCompuesta.de(100, "a")));
    }

    @Test
    void permiteRepetirLaPartePrincipalConClavesDistintas() {
        ClaveCompuesta<Integer, String> ana = ClaveCompuesta.de(500, "ana");
        ClaveCompuesta<Integer, String> luis = ClaveCompuesta.de(500, "luis");
        assertNotEquals(ana, luis);
        assertNotEquals(0, ana.compareTo(luis));
    }

    @Test
    void lasCotasEncierranTodasLasClavesDeSuPartePrincipal() {
        ClaveCompuesta<Integer, String> inferior = ClaveCompuesta.cotaInferior(500);
        ClaveCompuesta<Integer, String> superior = ClaveCompuesta.cotaSuperior(500);
        ClaveCompuesta<Integer, String> clave = ClaveCompuesta.de(500, "zzz");

        assertTrue(inferior.compareTo(clave) < 0);
        assertTrue(superior.compareTo(clave) > 0);
        assertTrue(superior.compareTo(ClaveCompuesta.de(501, "")) < 0);
        assertTrue(inferior.compareTo(ClaveCompuesta.de(499, "zzz")) > 0);
        assertTrue(inferior.esCota());
        assertFalse(clave.esCota());
    }

    @Test
    void rechazaNulos() {
        assertThrows(NullPointerException.class, () -> ClaveCompuesta.de(null, "a"));
        assertThrows(NullPointerException.class, () -> ClaveCompuesta.de(1, null));
    }
}
