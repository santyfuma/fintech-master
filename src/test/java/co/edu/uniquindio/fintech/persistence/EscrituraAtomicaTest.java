package co.edu.uniquindio.fintech.persistence;

import co.edu.uniquindio.fintech.exception.PersistenciaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EscrituraAtomicaTest {

    @TempDir
    Path directorio;

    private static byte[] bytes(String texto) {
        return texto.getBytes(StandardCharsets.UTF_8);
    }

    private long temporales() throws IOException {
        try (var archivos = Files.list(directorio)) {
            return archivos.filter(p -> p.getFileName().toString().startsWith(EscrituraAtomica.PREFIJO_TEMPORAL)).count();
        }
    }

    @Test
    void escribeYReemplazaElContenidoCompleto() throws IOException {
        Path destino = directorio.resolve("datos.json");
        EscrituraAtomica escritura = new EscrituraAtomica();
        escritura.escribir(destino, bytes("[1]"));
        assertEquals("[1]", Files.readString(destino));
        escritura.escribir(destino, bytes("[2,3]"));
        assertEquals("[2,3]", Files.readString(destino));
        assertEquals(0, temporales());
    }

    @Test
    void siFallaElReemplazoElOriginalQuedaIntactoYSeBorraElTemporal() throws IOException {
        Path destino = directorio.resolve("datos.json");
        new EscrituraAtomica().escribir(destino, bytes("original"));
        EscrituraAtomica fallida = new EscrituraAtomica() {
            @Override
            protected void moverAtomico(Path temporal, Path destinoFinal) throws IOException {
                assertTrue(Files.exists(temporal), "el temporal debe estar completo antes de mover");
                assertEquals("nuevo", Files.readString(temporal));
                throw new IOException("fallo simulado del disco");
            }
        };
        PersistenciaException ex = assertThrows(PersistenciaException.class,
                () -> fallida.escribir(destino, bytes("nuevo")));
        assertTrue(ex.getMessage().contains("datos.json"));
        assertEquals("original", Files.readString(destino));
        assertEquals(0, temporales());
    }

    @Test
    void sinMovimientoAtomicoUsaLaAlternativaSegura() throws IOException {
        Path destino = directorio.resolve("datos.json");
        new EscrituraAtomica().escribir(destino, bytes("viejo"));
        boolean[] alternativaUsada = {false};
        EscrituraAtomica sinAtomico = new EscrituraAtomica() {
            @Override
            protected void moverAtomico(Path temporal, Path destinoFinal) throws IOException {
                throw new AtomicMoveNotSupportedException(temporal.toString(), destinoFinal.toString(), "no soportado");
            }

            @Override
            protected void moverReemplazando(Path temporal, Path destinoFinal) throws IOException {
                alternativaUsada[0] = true;
                super.moverReemplazando(temporal, destinoFinal);
            }
        };
        sinAtomico.escribir(destino, bytes("nuevo"));
        assertTrue(alternativaUsada[0]);
        assertEquals("nuevo", Files.readString(destino));
        assertEquals(0, temporales());
    }

    @Test
    void siTambienFallaLaAlternativaElOriginalQuedaIntacto() throws IOException {
        Path destino = directorio.resolve("datos.json");
        new EscrituraAtomica().escribir(destino, bytes("original"));
        EscrituraAtomica fallida = new EscrituraAtomica() {
            @Override
            protected void moverAtomico(Path temporal, Path destinoFinal) throws IOException {
                throw new AtomicMoveNotSupportedException(temporal.toString(), destinoFinal.toString(), "no soportado");
            }

            @Override
            protected void moverReemplazando(Path temporal, Path destinoFinal) throws IOException {
                throw new IOException("también falla");
            }
        };
        assertThrows(PersistenciaException.class, () -> fallida.escribir(destino, bytes("nuevo")));
        assertEquals("original", Files.readString(destino));
        assertEquals(0, temporales());
    }

    @Test
    void creaLosDirectoriosQueFaltan() throws IOException {
        Path destino = directorio.resolve("a/b/datos.json");
        new EscrituraAtomica().escribir(destino, bytes("[]"));
        assertEquals("[]", Files.readString(destino));
    }

    @Test
    void limpiarTemporalesSoloBorraLosPropios() throws IOException {
        Files.writeString(directorio.resolve(".fintech-usuarios.json-123.tmp"), "basura");
        Files.writeString(directorio.resolve("otro.tmp"), "ajeno");
        Files.writeString(directorio.resolve("usuarios.json"), "[]");
        assertEquals(1, new EscrituraAtomica().limpiarTemporales(directorio));
        assertFalse(Files.exists(directorio.resolve(".fintech-usuarios.json-123.tmp")));
        assertTrue(Files.exists(directorio.resolve("otro.tmp")));
        assertTrue(Files.exists(directorio.resolve("usuarios.json")));
        assertEquals(0, new EscrituraAtomica().limpiarTemporales(directorio.resolve("no-existe")));
    }
}
