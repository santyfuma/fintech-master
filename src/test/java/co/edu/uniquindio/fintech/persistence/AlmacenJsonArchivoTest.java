package co.edu.uniquindio.fintech.persistence;

import co.edu.uniquindio.fintech.exception.PersistenciaException;
import co.edu.uniquindio.fintech.model.BeneficioCanjeado;
import co.edu.uniquindio.fintech.model.Billetera;
import co.edu.uniquindio.fintech.model.EventoAuditoria;
import co.edu.uniquindio.fintech.model.Notificacion;
import co.edu.uniquindio.fintech.model.OperacionProgramada;
import co.edu.uniquindio.fintech.model.Transaccion;
import co.edu.uniquindio.fintech.model.Usuario;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static co.edu.uniquindio.fintech.soporte.DatosPrueba.FECHA;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.billetera;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.canje;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.evento;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.ids;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.lista;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.notificacion;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.recarga;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.recargaProgramada;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.reversion;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.trx;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Persistencia de un archivo con Jackson 3 real ({@code tools.jackson}). */
class AlmacenJsonArchivoTest {

    @TempDir
    Path directorio;

    private <T> AlmacenJsonArchivo<T> almacen(Path dir, String archivo, Class<T[]> tipo) {
        return new AlmacenJsonArchivo<>(dir, archivo, tipo, MapeadorJson.crear(), new EscrituraAtomica());
    }

    private AlmacenJsonArchivo<Usuario> usuarios() {
        return almacen(directorio, "usuarios.json", Usuario[].class);
    }

    private void escribir(String archivo, String contenido) throws IOException {
        Files.writeString(directorio.resolve(archivo), contenido);
    }

    private static void sinTemporales(Path dir) throws IOException {
        if (Files.isDirectory(dir)) {
            try (var archivos = Files.list(dir)) {
                assertEquals(0, archivos.filter(p -> p.getFileName().toString().startsWith(".fintech-")).count());
            }
        }
    }

    // ------------------------------------------------------------------ lectura

    @Test
    void archivoInexistenteDevuelveListaVaciaSinCrearlo() {
        assertTrue(usuarios().cargar().estaVacia());
        assertFalse(Files.exists(directorio.resolve("usuarios.json")));
        assertEquals("usuarios.json", usuarios().nombreArchivo());
    }

    @Test
    void archivoVacioOConSoloEspaciosDevuelveListaVacia() throws IOException {
        escribir("usuarios.json", "");
        assertTrue(usuarios().cargar().estaVacia());
        escribir("usuarios.json", "  \n\t\r\n");
        assertTrue(usuarios().cargar().estaVacia());
    }

    @Test
    void arregloJsonVacioDevuelveListaVacia() throws IOException {
        escribir("usuarios.json", "[]");
        assertTrue(usuarios().cargar().estaVacia());
        escribir("usuarios.json", "  [ ]\n");
        assertTrue(usuarios().cargar().estaVacia());
    }

    @Test
    void leeRegistrosEscritosAMano() throws IOException {
        escribir("usuarios.json", "[{\"identificacion\":\"1\",\"nombre\":\"Ana\",\"correo\":\"a@b.co\","
                + "\"puntosAcumulados\":750,\"nivel\":\"PLATA\"}]");
        Lista<Usuario> leidos = usuarios().cargar();
        assertEquals(1, leidos.tamanio());
        assertEquals("Ana", leidos.obtener(0).nombre());
        assertEquals(750, leidos.obtener(0).puntosAcumulados());
    }

    // ------------------------------------------------------------------ escritura

    @Test
    void escribeYLeeConservandoElOrden() throws IOException {
        usuarios().guardar(lista(usuario("c", 1), usuario("a", 2), usuario("b", 3)));
        assertEquals("[c, a, b]", ids(usuarios().cargar()));
        assertTrue(Files.readString(directorio.resolve("usuarios.json")).trim().startsWith("["));
        sinTemporales(directorio);
    }

    @Test
    void guardarSobrescribeElArchivoCompleto() {
        usuarios().guardar(lista(usuario("a", 1), usuario("b", 2), usuario("c", 3)));
        usuarios().guardar(lista(usuario("b", 9)));
        Lista<Usuario> leidos = usuarios().cargar();
        assertEquals("[b]", ids(leidos));
        assertEquals(9, leidos.obtener(0).puntosAcumulados());
    }

    @Test
    void guardarUnaListaVaciaDejaUnArregloVacio() {
        usuarios().guardar(lista(usuario("a", 1)));
        usuarios().guardar(lista());
        assertTrue(Files.exists(directorio.resolve("usuarios.json")));
        assertTrue(usuarios().cargar().estaVacia());
    }

    @Test
    void creaElDirectorioDeDatosAutomaticamente() {
        Path anidado = directorio.resolve("no/existe/aun");
        AlmacenJsonArchivo<Usuario> almacen = almacen(anidado, "usuarios.json", Usuario[].class);
        almacen.guardar(lista(usuario("a", 1)));
        assertTrue(Files.isRegularFile(anidado.resolve("usuarios.json")));
        assertEquals(anidado.resolve("usuarios.json"), almacen.ruta());
    }

    @Test
    void laRutaEsConfigurableYCadaDirectorioEsIndependiente() {
        AlmacenJsonArchivo<Usuario> uno = almacen(directorio.resolve("uno"), "usuarios.json", Usuario[].class);
        AlmacenJsonArchivo<Usuario> dos = almacen(directorio.resolve("dos"), "usuarios.json", Usuario[].class);
        uno.guardar(lista(usuario("a", 1)));
        assertEquals("[a]", ids(uno.cargar()));
        assertTrue(dos.cargar().estaVacia());
    }

    // ------------------------------------------------------------------ errores

    @Test
    void jsonCorruptoLanzaPersistenciaConArchivoYCausa() throws IOException {
        escribir("usuarios.json", "[{\"identificacion\":");
        PersistenciaException ex = assertThrows(PersistenciaException.class, () -> usuarios().cargar());
        assertTrue(ex.getMessage().contains("usuarios.json"), ex.getMessage());
        assertNotNull(ex.getCause());
        assertEquals("ERROR_PERSISTENCIA", ex.codigo());
    }

    @Test
    void contenidoQueNoCorrespondeAlTipoLanzaPersistencia() throws IOException {
        String[] invalidos = {
                "{\"identificacion\":\"1\"}",                  // objeto en vez de arreglo
                "[\"texto\"]",                                  // elemento que no es un usuario
                "[1, 2]",
                "null",
                "[null]",
                "[{\"identificacion\":\"1\",\"puntosAcumulados\":0,\"nivel\":\"BRONCE\"}]", // faltan campos
                "[{\"identificacion\":\"1\",\"nombre\":\"A\",\"correo\":\"a@b\",\"puntosAcumulados\":0,"
                        + "\"nivel\":\"DIAMANTE\"}]"           // enum inexistente
        };
        for (String contenido : invalidos) {
            escribir("usuarios.json", contenido);
            PersistenciaException ex = assertThrows(PersistenciaException.class, () -> usuarios().cargar(), contenido);
            assertTrue(ex.getMessage().contains("usuarios.json"), ex.getMessage());
        }
    }

    @Test
    void identificadorConFormatoInvalidoLanzaPersistencia() throws IOException {
        escribir("billeteras.json", "[{\"codigo\":\"BIL-1\",\"identificacionPropietario\":\"1\",\"nombre\":\"A\","
                + "\"tipo\":\"AHORRO\",\"saldo\":1,\"estado\":\"ACTIVA\"}]");
        AlmacenJsonArchivo<Billetera> billeteras = almacen(directorio, "billeteras.json", Billetera[].class);
        PersistenciaException ex = assertThrows(PersistenciaException.class, billeteras::cargar);
        assertTrue(ex.getMessage().contains("billeteras.json"), ex.getMessage());
    }

    @Test
    void errorDeLecturaLanzaPersistencia() throws IOException {
        Files.createDirectories(directorio.resolve("usuarios.json")); // existe pero no se puede leer como archivo
        PersistenciaException ex = assertThrows(PersistenciaException.class, () -> usuarios().cargar());
        assertTrue(ex.getCause() instanceof IOException);
        assertTrue(ex.getMessage().contains("usuarios.json"));
    }

    @Test
    void errorDeEscrituraNoDejaTemporalesNiArchivoParcial() throws IOException {
        Path bloqueado = directorio.resolve("usuarios.json");
        Files.createDirectories(bloqueado);
        Files.writeString(bloqueado.resolve("dentro.txt"), "x");
        PersistenciaException ex = assertThrows(PersistenciaException.class,
                () -> usuarios().guardar(lista(usuario("a", 1))));
        assertTrue(ex.getMessage().contains("usuarios.json"));
        assertTrue(Files.isDirectory(bloqueado));
        assertEquals("x", Files.readString(bloqueado.resolve("dentro.txt")));
        sinTemporales(directorio);
    }

    @Test
    void elDirectorioDeDatosNoPuedeSerUnArchivo() throws IOException {
        Path archivo = directorio.resolve("no-es-directorio");
        Files.writeString(archivo, "x");
        AlmacenJsonArchivo<Usuario> almacen = almacen(archivo, "usuarios.json", Usuario[].class);
        assertThrows(PersistenciaException.class, () -> almacen.guardar(lista(usuario("a", 1))));
    }

    @Test
    void validaSusParametros() {
        assertThrows(IllegalArgumentException.class,
                () -> new AlmacenJsonArchivo<>(directorio, " ", Usuario[].class, MapeadorJson.crear(),
                        new EscrituraAtomica()));
        assertThrows(NullPointerException.class, () -> usuarios().guardar(null));
    }

    // ------------------------------------------------------------------ las siete entidades

    private <T> T idaYVuelta(T registro, String archivo, Class<T[]> tipo) {
        AlmacenJsonArchivo<T> almacen = almacen(directorio, archivo, tipo);
        almacen.guardar(lista(registro));
        Lista<T> leidos = almacen.cargar();
        assertEquals(1, leidos.tamanio());
        return leidos.obtener(0);
    }

    @Test
    void lasSieteEntidadesSeSerializanYDeserializanSinPerdida() {
        Usuario usuario = usuario("1094000111", 750);
        assertEquals(usuario, idaYVuelta(usuario, "usuarios.json", Usuario[].class));

        Billetera billetera = billetera(1, "1094000111");
        Billetera billeteraLeida = idaYVuelta(billetera, "billeteras.json", Billetera[].class);
        assertEquals(billetera, billeteraLeida);
        assertEquals(2, billeteraLeida.saldo().scale());

        Transaccion recarga = recarga(1, 1, "150000.75");
        assertEquals(recarga, idaYVuelta(recarga, "transacciones.json", Transaccion[].class));
        Transaccion reversion = reversion(2, 1, 1, null, "150000.75");
        Transaccion reversionLeida = idaYVuelta(reversion, "transacciones.json", Transaccion[].class);
        assertEquals(reversion, reversionLeida);
        assertEquals(trx(1), reversionLeida.transaccionRevertida());

        OperacionProgramada operacion = recargaProgramada(1, 1, FECHA.plusDays(3));
        assertEquals(operacion, idaYVuelta(operacion, "operaciones-programadas.json", OperacionProgramada[].class));

        Notificacion notificacion = notificacion(1, "1094000111");
        assertEquals(notificacion, idaYVuelta(notificacion, "notificaciones.json", Notificacion[].class));

        EventoAuditoria evento = evento(1, "1094000111", null);
        assertEquals(evento, idaYVuelta(evento, "auditoria.json", EventoAuditoria[].class));

        BeneficioCanjeado canje = canje(1, "1094000111");
        assertEquals(canje, idaYVuelta(canje, "beneficios.json", BeneficioCanjeado[].class));
    }
}
