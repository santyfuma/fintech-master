package co.edu.uniquindio.fintech.config;

import co.edu.uniquindio.fintech.exception.PersistenciaException;
import co.edu.uniquindio.fintech.persistence.AlmacenesJson;
import co.edu.uniquindio.fintech.persistence.TipoIdentificador;
import co.edu.uniquindio.fintech.soporte.EntornoDatos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

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

class InicializadorDatosTest {

    @TempDir
    Path raiz;

    @Test
    void conUnDirectorioVacioNoCreaDatosFicticios() {
        Path directorio = raiz.resolve("data");
        ContextoDatos contexto = new EntornoDatos(directorio).iniciar();
        assertEquals(0, contexto.usuarios().cantidad());
        assertEquals(0, contexto.billeteras().cantidad());
        assertEquals(0, contexto.transacciones().cantidad());
        assertEquals(0, contexto.operacionesProgramadas().cantidad());
        assertEquals(0, contexto.notificaciones().cantidad());
        assertEquals(0, contexto.auditoria().cantidad());
        assertEquals(0, contexto.beneficios().cantidad());
        assertEquals(0, contexto.operacionesRecuperadas());
        assertFalse(Files.exists(directorio), "el arranque no debe crear archivos ni directorios");
        assertEquals("BIL-000001", contexto.generador().siguiente(TipoIdentificador.BILLETERA));
    }

    @Test
    void recuperaElDiarioAntesDeCargarYLuegoLosConsecutivos() {
        EntornoDatos entorno = new EntornoDatos(raiz);
        entorno.almacenes.billeteras().guardar(lista(billetera(1, "u1"), billetera(2, "u1")));
        // Operación interrumpida tras escribir el diario: crea BIL-000005 y TRX-000001.
        entorno.diario.registrar(entorno.diario.preparar(trx(1), usuario("u1", 0), billetera(5, "u1"),
                recarga(1, 5, "10.00")));

        ContextoDatos contexto = new EntornoDatos(raiz).iniciar();
        assertEquals(1, contexto.operacionesRecuperadas());
        assertTrue(contexto.billeteras().existe(bil(5)));
        assertEquals("[" + trx(1) + "]", ids(contexto.transacciones().historialDeBilletera(bil(5))));
        assertEquals("[" + trx(1) + "]", ids(contexto.transacciones().historialDeUsuario("u1")));
        // Los consecutivos se recuperan después del diario: BIL-000005 ya cuenta.
        assertEquals("BIL-000006", contexto.generador().siguiente(TipoIdentificador.BILLETERA));
        assertEquals("TRX-000002", contexto.generador().siguiente(TipoIdentificador.TRANSACCION));
        assertTrue(entorno.diario.pendientes().estaVacia());
    }

    @Test
    void siElDiarioEstaCorruptoNoSeCarganLosRepositorios() {
        EntornoDatos entorno = new EntornoDatos(raiz);
        entorno.almacenes.usuarios().guardar(lista(usuario("u1", 0)));
        entorno.escribir(AlmacenesJson.ARCHIVO_DIARIO, "[{\"id\":\"OP-1\",");
        PersistenciaException ex = assertThrows(PersistenciaException.class, entorno::iniciar);
        assertTrue(ex.getMessage().contains(AlmacenesJson.ARCHIVO_DIARIO), ex.getMessage());
    }

    @Test
    void siFallaLaRecuperacionDeUnaEntradaNoSeArrancaConDatosParciales() {
        EntornoDatos entorno = new EntornoDatos(raiz);
        entorno.almacenes.usuarios().guardar(lista(usuario("u1", 0)));
        String antes = entorno.leer("usuarios.json");
        entorno.diario.registrar(entorno.diario.preparar("OP-1", usuario("u2", 0), billetera(1, "u2")));
        entorno.bloquearEscritura("billeteras.json");
        assertThrows(PersistenciaException.class, entorno::iniciar);
        assertEquals(1, entorno.diario.pendientes().tamanio(), "la entrada fallida sigue en el diario");
        assertFalse(antes.equals(entorno.leer("usuarios.json")), "usuarios.json se alcanzó a rehacer");
    }

    @Test
    void datosInvalidosDetienenElArranque() {
        EntornoDatos entorno = new EntornoDatos(raiz);
        entorno.almacenes.billeteras().guardar(lista(billetera(2, "u"), billetera(1, "u")));
        assertThrows(PersistenciaException.class, entorno::iniciar);

        entorno.almacenes.billeteras().guardar(lista(billetera(1, "u")));
        entorno.almacenes.transacciones().guardar(lista(recarga(1, 7, "1.00")));
        assertThrows(PersistenciaException.class, entorno::iniciar);

        entorno.escribir("transacciones.json", "[]");
        entorno.escribir("notificaciones.json", "[{\"id\":\"NOT-1\"}]");
        assertThrows(PersistenciaException.class, entorno::iniciar);
    }

    @Test
    void descartaTemporalesDeEscriturasInterrumpidas() {
        EntornoDatos entorno = new EntornoDatos(raiz);
        entorno.escribir(".fintech-usuarios.json-999.tmp", "[{\"incompleto\":");
        entorno.iniciar();
        assertEquals(0, entorno.temporales());
    }

    @Test
    void reiniciarConservaLosRegistros() {
        ContextoDatos primero = new EntornoDatos(raiz).iniciar();
        primero.usuarios().registrar(usuario("u1", 30));
        primero.billeteras().registrar(billetera(1, "u1"));
        primero.transacciones().registrar(recarga(1, 1, "10.00"));

        ContextoDatos segundo = new EntornoDatos(raiz).iniciar();
        assertEquals(usuario("u1", 30), segundo.usuarios().buscar("u1"));
        assertEquals("[" + bil(1) + "]", ids(segundo.billeteras().porPropietario("u1")));
        assertEquals("[" + trx(1) + "]", ids(segundo.transacciones().historialDeUsuario("u1")));
        assertEquals("BIL-000002", segundo.generador().siguiente(TipoIdentificador.BILLETERA));
        assertEquals(0, segundo.operacionesRecuperadas());
    }
}
