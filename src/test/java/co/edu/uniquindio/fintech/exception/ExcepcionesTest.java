package co.edu.uniquindio.fintech.exception;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcepcionesTest {

    @Test
    void cadaCategoriaLlevaSuCodigoYMensaje() {
        RecursoNoEncontradoException noEncontrado =
                new RecursoNoEncontradoException("USUARIO_NO_ENCONTRADO", "No existe el usuario 123");
        assertEquals("USUARIO_NO_ENCONTRADO", noEncontrado.codigo());
        assertEquals("No existe el usuario 123", noEncontrado.getMessage());

        ConflictoNegocioException conflicto =
                new ConflictoNegocioException("SALDO_INSUFICIENTE", "Saldo insuficiente para realizar la operación");
        assertEquals("SALDO_INSUFICIENTE", conflicto.codigo());

        SolicitudInvalidaException invalida =
                new SolicitudInvalidaException("FECHA_EJECUCION_PASADA", "La fecha debe ser futura");
        assertEquals("FECHA_EJECUCION_PASADA", invalida.codigo());
    }

    @Test
    void codigosGeneralesPorDefecto() {
        assertEquals("RECURSO_NO_ENCONTRADO", new RecursoNoEncontradoException("m").codigo());
        assertEquals("CONFLICTO_NEGOCIO", new ConflictoNegocioException("m").codigo());
        assertEquals("SOLICITUD_INVALIDA", new SolicitudInvalidaException("m").codigo());
        assertEquals("ERROR_PERSISTENCIA", new PersistenciaException("m").codigo());
    }

    @Test
    void persistenciaConservaLaCausa() {
        IOException causa = new IOException("disco lleno");
        PersistenciaException ex = new PersistenciaException("No se pudo escribir usuarios.json", causa);
        assertSame(causa, ex.getCause());
        assertNull(new PersistenciaException("m").getCause());
    }

    @Test
    void todasSonNoVerificadasYDeLaMismaBase() {
        Object[] excepciones = {new RecursoNoEncontradoException("m"), new ConflictoNegocioException("m"),
                new SolicitudInvalidaException("m"), new PersistenciaException("m")};
        for (Object e : excepciones) {
            assertTrue(e instanceof FintechException);
            assertTrue(e instanceof RuntimeException);
        }
    }

    @Test
    void rechazaCodigosYMensajesInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new ConflictoNegocioException("saldo", "m"));
        assertThrows(IllegalArgumentException.class, () -> new ConflictoNegocioException("SALDO INSUFICIENTE", "m"));
        assertThrows(IllegalArgumentException.class, () -> new ConflictoNegocioException(null, "m"));
        assertThrows(IllegalArgumentException.class, () -> new ConflictoNegocioException("X", " "));
        assertThrows(IllegalArgumentException.class, () -> new RecursoNoEncontradoException((String) null));
    }
}
