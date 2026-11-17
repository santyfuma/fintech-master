package co.edu.uniquindio.fintech.persistence;

import co.edu.uniquindio.fintech.exception.PersistenciaException;
import co.edu.uniquindio.fintech.model.Billetera;
import co.edu.uniquindio.fintech.model.Transaccion;
import co.edu.uniquindio.fintech.model.Usuario;
import co.edu.uniquindio.fintech.soporte.EntornoDatos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static co.edu.uniquindio.fintech.soporte.DatosPrueba.bil;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.billetera;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.ids;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.lista;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.recarga;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.trx;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiarioOperacionesJsonTest {

    @TempDir
    Path directorio;

    private EntornoDatos entorno;

    @BeforeEach
    void preparar() {
        entorno = new EntornoDatos(directorio);
    }

    /** Simula reiniciar la aplicación: componentes nuevos sobre el mismo directorio. */
    private EntornoDatos reiniciar() {
        return new EntornoDatos(directorio);
    }

    private Billetera saldo(Billetera b, String saldo) {
        return b.conSaldo(new BigDecimal(saldo));
    }

    // ------------------------------------------------------------------ diario vacío

    @Test
    void diarioInexistenteNoRecuperaNadaNiCreaArchivos() {
        assertEquals(0, entorno.diario.recuperarPendientes());
        assertTrue(entorno.diario.pendientes().estaVacia());
        assertFalse(entorno.existe(AlmacenesJson.ARCHIVO_DIARIO));
    }

    @Test
    void diarioVacioNoRecuperaNada() {
        entorno.escribir(AlmacenesJson.ARCHIVO_DIARIO, "[]");
        assertEquals(0, entorno.diario.recuperarPendientes());
    }

    // ------------------------------------------------------------------ preparar

    @Test
    void prepararConstruyeLasImagenesFinales() {
        EntradaDiario entrada = entorno.diario.preparar(trx(1), billetera(1, "u1"), recarga(1, 1, "50.00"),
                usuario("u1", 3));
        assertEquals(trx(1), entrada.id());
        assertEquals(EstadoEntradaDiario.PREPARADA, entrada.estado());
        assertEquals(LocalDateTime.of(2026, 11, 15, 18, 0), entrada.fecha());
        ImagenRegistro[] imagenes = entrada.imagenes();
        assertEquals(3, imagenes.length);
        assertEquals("billeteras.json", imagenes[0].archivo());
        assertEquals(bil(1), imagenes[0].identificador());
        assertEquals("transacciones.json", imagenes[1].archivo());
        assertEquals(trx(1), imagenes[1].identificador());
        assertEquals("usuarios.json", imagenes[2].archivo());
        assertEquals("u1", imagenes[2].identificador());
        assertTrue(imagenes[0].contenido().startsWith("{"));
        assertTrue(imagenes[0].contenido().contains("\"saldo\":1000.00"), imagenes[0].contenido());
    }

    @Test
    void prepararRechazaOperacionesSinRegistrosOConTiposAjenos() {
        assertThrows(IllegalArgumentException.class, () -> entorno.diario.preparar(trx(1)));
        assertThrows(IllegalArgumentException.class, () -> entorno.diario.preparar(trx(1), "texto"));
        assertThrows(IllegalArgumentException.class, () -> entorno.diario.preparar(trx(1), (Object) null));
    }

    // ------------------------------------------------------------------ recuperación

    @Test
    void unaEntradaPreparadaSeRehaceAlRecuperar() {
        // Corte justo después de escribir el diario: ningún archivo de datos se tocó.
        entorno.diario.registrar(entorno.diario.preparar(trx(1), usuario("u1", 0), billetera(1, "u1"),
                recarga(1, 1, "50.00")));
        assertFalse(entorno.existe("billeteras.json"));

        EntornoDatos reiniciado = reiniciar();
        assertEquals(1, reiniciado.diario.recuperarPendientes());
        assertEquals("[u1]", ids(reiniciado.almacenes.usuarios().cargar()));
        assertEquals("[" + bil(1) + "]", ids(reiniciado.almacenes.billeteras().cargar()));
        assertEquals("[" + trx(1) + "]", ids(reiniciado.almacenes.transacciones().cargar()));
        assertTrue(reiniciado.diario.pendientes().estaVacia());
    }

    @Test
    void variasEntradasSeAplicanEnOrden() {
        Billetera b = billetera(1, "u1");
        entorno.diario.registrar(entorno.diario.preparar(trx(1), b, recarga(1, 1, "50.00")));
        entorno.diario.registrar(entorno.diario.preparar(trx(2), saldo(b, "1050.00"), recarga(2, 1, "1.00")));
        EntornoDatos reiniciado = reiniciar();
        assertEquals(2, reiniciado.diario.recuperarPendientes());
        Billetera final_ = reiniciado.almacenes.billeteras().cargar().obtener(0);
        assertEquals(new BigDecimal("1050.00"), final_.saldo());
        assertEquals(1, reiniciado.almacenes.billeteras().cargar().tamanio());
        assertEquals("[" + trx(1) + ", " + trx(2) + "]", ids(reiniciado.almacenes.transacciones().cargar()));
    }

    @Test
    void aplicarEsIdempotente() {
        EntradaDiario entrada = entorno.diario.preparar(trx(1), billetera(1, "u1"), recarga(1, 1, "50.00"));
        entorno.diario.aplicar(entrada);
        String billeteras = entorno.leer("billeteras.json");
        String transacciones = entorno.leer("transacciones.json");
        entorno.diario.aplicar(entrada);
        entorno.diario.aplicar(entrada);
        assertEquals(billeteras, entorno.leer("billeteras.json"));
        assertEquals(transacciones, entorno.leer("transacciones.json"));
        assertEquals(1, entorno.almacenes.transacciones().cargar().tamanio());
    }

    @Test
    void reemplazaPorIdentificadorConservandoElOrden() {
        entorno.almacenes.billeteras().guardar(lista(billetera(1, "u"), billetera(2, "u"), billetera(3, "u")));
        entorno.diario.aplicar(entorno.diario.preparar("OP-1", saldo(billetera(2, "u"), "5.00"), billetera(4, "u")));
        var billeteras = entorno.almacenes.billeteras().cargar();
        assertEquals("[" + bil(1) + ", " + bil(2) + ", " + bil(3) + ", " + bil(4) + "]", ids(billeteras));
        assertEquals(new BigDecimal("5.00"), billeteras.obtener(1).saldo());
    }

    @Test
    void corteDespuesDeActualizarSoloAlgunosArchivos() {
        Billetera b = saldo(billetera(1, "u1"), "1050.00");
        Transaccion t = recarga(1, 1, "50.00");
        entorno.almacenes.billeteras().guardar(lista(billetera(1, "u1")));
        EntradaDiario entrada = entorno.diario.preparar(trx(1), b, t);
        entorno.diario.registrar(entrada);
        // Se alcanzó a escribir billeteras.json pero no transacciones.json.
        entorno.almacenes.billeteras().guardar(lista(b));

        EntornoDatos reiniciado = reiniciar();
        assertEquals(1, reiniciado.diario.recuperarPendientes());
        assertEquals(1, reiniciado.almacenes.billeteras().cargar().tamanio());
        assertEquals(new BigDecimal("1050.00"), reiniciado.almacenes.billeteras().cargar().obtener(0).saldo());
        assertEquals("[" + trx(1) + "]", ids(reiniciado.almacenes.transacciones().cargar()));
        assertTrue(reiniciado.diario.pendientes().estaVacia());
    }

    @Test
    void diarioCorruptoDetieneLaRecuperacion() {
        entorno.escribir(AlmacenesJson.ARCHIVO_DIARIO, "[{\"id\":");
        PersistenciaException ex = assertThrows(PersistenciaException.class, entorno.diario::recuperarPendientes);
        assertTrue(ex.getMessage().contains(AlmacenesJson.ARCHIVO_DIARIO), ex.getMessage());
    }

    @Test
    void entradaConArchivoDesconocidoEsUnDiarioCorrupto() {
        entorno.escribir(AlmacenesJson.ARCHIVO_DIARIO, "[{\"id\":\"OP-1\",\"fecha\":\"2026-11-15T18:00:00\","
                + "\"estado\":\"PREPARADA\",\"imagenes\":[{\"archivo\":\"otro.json\",\"identificador\":\"x\","
                + "\"contenido\":\"{}\"}]}]");
        assertThrows(PersistenciaException.class, entorno.diario::recuperarPendientes);
    }

    @Test
    void losMontosConservanEscalaYPrecisionExactasAlPasarPorElDiario() {
        Billetera grande = saldo(billetera(1, "u1"), "12345678901234567.89");
        Billetera conCeros = saldo(billetera(2, "u1"), "1050.00");
        entorno.diario.registrar(entorno.diario.preparar("OP-1", grande, conCeros));
        reiniciar().diario.recuperarPendientes();
        var billeteras = reiniciar().almacenes.billeteras().cargar();
        assertEquals(new BigDecimal("12345678901234567.89"), billeteras.obtener(0).saldo());
        assertEquals(new BigDecimal("1050.00"), billeteras.obtener(1).saldo());
        assertEquals(grande, billeteras.obtener(0));
    }

    @Test
    void siFallaUnaEntradaNoSeRetiraYLasSiguientesQuedanPendientes() {
        // OP-1 trae una billetera cuyo código no cumple el formato; OP-2 es válida.
        ImagenRegistro invalida = new ImagenRegistro("billeteras.json", "BIL-000001",
                "{\"codigo\":\"MAL\",\"identificacionPropietario\":\"u\",\"nombre\":\"n\","
                        + "\"tipo\":\"AHORRO\",\"saldo\":1,\"estado\":\"ACTIVA\"}");
        EntradaDiario primera = new EntradaDiario("OP-1", LocalDateTime.of(2026, 11, 15, 18, 0),
                EstadoEntradaDiario.PREPARADA, new ImagenRegistro[]{invalida});
        entorno.almacenes.diario().guardar(lista(primera, entorno.diario.preparar("OP-2", usuario("u2", 0))));
        PersistenciaException ex = assertThrows(PersistenciaException.class, entorno.diario::recuperarPendientes);
        assertTrue(ex.getMessage().contains("OP-1"), ex.getMessage());
        assertEquals(2, entorno.diario.pendientes().tamanio());
        assertFalse(entorno.existe("usuarios.json"));
        assertFalse(entorno.existe("billeteras.json"));
    }

    @Test
    void imagenCuyoContenidoNoCoincideConSuIdentificador() {
        EntradaDiario entrada = new EntradaDiario("OP-1", LocalDateTime.of(2026, 1, 1, 0, 0),
                EstadoEntradaDiario.PREPARADA, new ImagenRegistro[]{new ImagenRegistro("usuarios.json", "otro",
                entorno.almacenes.mapper().writeValueAsString(usuario("u1", 0)))});
        assertThrows(PersistenciaException.class, () -> entorno.diario.aplicar(entrada));
        assertFalse(entorno.existe("usuarios.json"));
    }

    // ------------------------------------------------------------------ registrar, confirmar, ejecutar

    @Test
    void registrarDosVecesLaMismaOperacionFalla() {
        EntradaDiario entrada = entorno.diario.preparar("OP-1", usuario("u1", 0));
        entorno.diario.registrar(entrada);
        assertThrows(IllegalStateException.class, () -> entorno.diario.registrar(entrada));
        assertEquals(1, entorno.diario.pendientes().tamanio());
    }

    @Test
    void confirmarRetiraSoloLaEntradaIndicada() {
        entorno.diario.registrar(entorno.diario.preparar("OP-1", usuario("u1", 0)));
        entorno.diario.registrar(entorno.diario.preparar("OP-2", usuario("u2", 0)));
        entorno.diario.confirmar("OP-1");
        entorno.diario.confirmar("NO-EXISTE");
        assertEquals(1, entorno.diario.pendientes().tamanio());
        assertEquals("OP-2", entorno.diario.pendientes().obtener(0).id());
    }

    @Test
    void ejecutarAplicaActualizaLaMemoriaYConfirma() {
        boolean[] memoriaActualizada = {false};
        entorno.diario.ejecutar(entorno.diario.preparar(trx(1), billetera(1, "u1"), recarga(1, 1, "5.00")),
                () -> memoriaActualizada[0] = true);
        assertTrue(memoriaActualizada[0]);
        assertTrue(entorno.diario.pendientes().estaVacia());
        assertEquals(1, entorno.almacenes.billeteras().cargar().tamanio());
        assertEquals(1, entorno.almacenes.transacciones().cargar().tamanio());
    }

    @Test
    void siLaEscrituraFallaEnCalienteLaOperacionQuedaEnElDiario() {
        entorno.bloquearEscritura("transacciones.json");
        boolean[] memoriaActualizada = {false};
        EntradaDiario entrada = entorno.diario.preparar(trx(1), billetera(1, "u1"), recarga(1, 1, "5.00"));
        PersistenciaException ex = assertThrows(PersistenciaException.class,
                () -> entorno.diario.ejecutar(entrada, () -> memoriaActualizada[0] = true));
        assertTrue(ex.getMessage().contains("próximo arranque"), ex.getMessage());
        assertFalse(memoriaActualizada[0]);
        assertEquals(1, entorno.diario.pendientes().tamanio());
        assertEquals(0, entorno.temporales());
    }

    @Test
    void laEntradaDelDiarioNoExponeSuArregloInterno() {
        EntradaDiario entrada = entorno.diario.preparar("OP-1", usuario("u1", 0));
        entrada.imagenes()[0] = null;
        assertEquals("u1", entrada.imagenes()[0].identificador());
        assertThrows(IllegalArgumentException.class, () -> new EntradaDiario("OP-1", LocalDateTime.now(),
                EstadoEntradaDiario.PREPARADA, new ImagenRegistro[0]));
    }

    @Test
    void elContratoDeUsuarioSeRespetaAlRecuperar() {
        // Sanidad: lo recuperado es un Usuario válido del modelo.
        entorno.diario.registrar(entorno.diario.preparar("OP-1", usuario("u1", 42)));
        reiniciar().diario.recuperarPendientes();
        Usuario u = reiniciar().almacenes.usuarios().cargar().obtener(0);
        assertEquals(42, u.puntosAcumulados());
    }
}
