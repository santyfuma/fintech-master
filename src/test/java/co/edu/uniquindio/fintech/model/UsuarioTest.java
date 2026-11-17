package co.edu.uniquindio.fintech.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UsuarioTest {

    private static Usuario ana() {
        return new Usuario("1094000111", "Ana Pérez", "ana@correo.co", 0, NivelUsuario.BRONCE);
    }

    @Test
    void construyeConTodosSusCampos() {
        Usuario usuario = ana();
        assertEquals("1094000111", usuario.identificacion());
        assertEquals("Ana Pérez", usuario.nombre());
        assertEquals("ana@correo.co", usuario.correo());
        assertEquals(0, usuario.puntosAcumulados());
        assertEquals(NivelUsuario.BRONCE, usuario.nivel());
    }

    @Test
    void conDatosBasicosDevuelveUnaCopiaSinCambiarElOriginal() {
        Usuario original = ana();
        Usuario cambiado = original.conDatosBasicos("Ana María Pérez", "ana.maria@correo.co");
        assertEquals("Ana María Pérez", cambiado.nombre());
        assertEquals("ana.maria@correo.co", cambiado.correo());
        assertEquals(original.identificacion(), cambiado.identificacion());
        assertEquals("Ana Pérez", original.nombre());
    }

    @Test
    void conPuntosNoCalculaElNivel() {
        // El modelo no decide el nivel: guarda lo que service le indique.
        Usuario conPuntos = ana().conPuntos(750, NivelUsuario.PLATA);
        assertEquals(750, conPuntos.puntosAcumulados());
        assertEquals(NivelUsuario.PLATA, conPuntos.nivel());
        assertEquals(0, ana().puntosAcumulados());
    }

    @Test
    void igualdadPorValor() {
        assertEquals(ana(), ana());
        assertEquals(ana().hashCode(), ana().hashCode());
        assertNotEquals(ana(), ana().conPuntos(1, NivelUsuario.BRONCE));
    }

    @Test
    void rechazaCamposObligatoriosAusentesOVacios() {
        assertThrows(NullPointerException.class,
                () -> new Usuario(null, "Ana", "a@b.co", 0, NivelUsuario.BRONCE));
        assertThrows(IllegalArgumentException.class,
                () -> new Usuario("  ", "Ana", "a@b.co", 0, NivelUsuario.BRONCE));
        assertThrows(IllegalArgumentException.class,
                () -> new Usuario("1", "", "a@b.co", 0, NivelUsuario.BRONCE));
        assertThrows(NullPointerException.class,
                () -> new Usuario("1", "Ana", null, 0, NivelUsuario.BRONCE));
        assertThrows(NullPointerException.class,
                () -> new Usuario("1", "Ana", "a@b.co", 0, null));
    }
}
