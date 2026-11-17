package co.edu.uniquindio.fintech.repository;

import co.edu.uniquindio.fintech.config.ContextoDatos;
import co.edu.uniquindio.fintech.model.EstadoTransaccion;
import co.edu.uniquindio.fintech.model.TipoTransaccion;
import co.edu.uniquindio.fintech.model.Transaccion;
import co.edu.uniquindio.fintech.soporte.EntornoDatos;
import co.edu.uniquindio.fintech.structures.interfaces.GrafoDirigidoPonderado;
import co.edu.uniquindio.fintech.structures.interfaces.Pila;
import co.edu.uniquindio.fintech.structures.interfaces.TablaHash;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;

import static co.edu.uniquindio.fintech.soporte.DatosPrueba.billetera;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.ids;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.recarga;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.retiro;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.reversion;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.transferencia;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.trx;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Escenario: Ana tiene BIL-1 y BIL-2; Beto tiene BIL-3.
 * <pre>
 * TRX-1 recarga        → BIL-1  100.00
 * TRX-2 transferencia  BIL-1 → BIL-3   50.00  (Ana → Beto)
 * TRX-3 transferencia  BIL-1 → BIL-2   50.00  (interna de Ana)
 * TRX-4 retiro         BIL-3            20.00  (Beto)
 * TRX-5 recarga        → BIL-3   50.00  (Beto)
 * </pre>
 */
class RepositorioTransaccionesTest {

    @TempDir
    Path directorio;

    private ContextoDatos contexto;
    private RepositorioTransacciones transacciones;

    @BeforeEach
    void preparar() {
        contexto = new EntornoDatos(directorio).iniciar();
        contexto.usuarios().registrar(usuario("ana", 0));
        contexto.usuarios().registrar(usuario("beto", 0));
        contexto.billeteras().registrar(billetera(1, "ana"));
        contexto.billeteras().registrar(billetera(2, "ana"));
        contexto.billeteras().registrar(billetera(3, "beto"));
        transacciones = contexto.transacciones();
        transacciones.registrar(recarga(1, 1, "100.00"));
        transacciones.registrar(transferencia(2, 1, 3, "50.00"));
        transacciones.registrar(transferencia(3, 1, 2, "50.00"));
        transacciones.registrar(retiro(4, 3, "20.00"));
        transacciones.registrar(recarga(5, 3, "50.00"));
    }

    private static String t(int... numeros) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < numeros.length; i++) {
            sb.append(i == 0 ? "" : ", ").append(trx(numeros[i]));
        }
        return sb.append(']').toString();
    }

    /** Revierte TRX-2: registra TRX-6 (REVERSION) y deja TRX-2 en REVERTIDA. */
    private void revertirLaTransferenciaAUnTercero() {
        transacciones.registrar(reversion(6, 2, 3, 1, "50.00"));
        transacciones.actualizar(transacciones.buscar(trx(2)).conEstado(EstadoTransaccion.REVERTIDA));
    }

    @Test
    void registrarYBuscar() {
        assertEquals(5, transacciones.cantidad());
        assertEquals(TipoTransaccion.RETIRO, transacciones.buscar(trx(4)).tipo());
        assertNull(transacciones.buscar(trx(9)));
        assertEquals(t(1, 2, 3, 4, 5), ids(transacciones.todos()));
        assertThrows(IllegalStateException.class, () -> transacciones.registrar(recarga(1, 1, "1.00")));
    }

    @Test
    void historialPorUsuarioEnOrdenCronologicoSinDuplicados() {
        // TRX-3 es interna: aparece una sola vez en el historial de Ana.
        assertEquals(t(1, 2, 3), ids(transacciones.historialDeUsuario("ana")));
        // TRX-2 la recibió Beto: está en ambos historiales.
        assertEquals(t(2, 4, 5), ids(transacciones.historialDeUsuario("beto")));
        assertEquals("[]", ids(transacciones.historialDeUsuario("nadie")));
    }

    @Test
    void historialPorBilletera() {
        assertEquals(t(1, 2, 3), ids(transacciones.historialDeBilletera(billetera(1, "ana").codigo())));
        assertEquals(t(3), ids(transacciones.historialDeBilletera(billetera(2, "ana").codigo())));
        assertEquals(t(2, 4, 5), ids(transacciones.historialDeBilletera(billetera(3, "beto").codigo())));
    }

    @Test
    void valoresRepetidosEnElAbb() {
        assertEquals(t(4, 2, 3, 5, 1), ids(transacciones.ordenadasPorValor()));
        assertEquals(t(1, 5), ids(transacciones.mayoresPorValor(2)));
        assertEquals(t(2, 3, 5), ids(transacciones.porRangoDeValor(new BigDecimal("50"), new BigDecimal("50.0"))));
        assertEquals(t(4, 2, 3, 5), ids(transacciones.porRangoDeValor(new BigDecimal("0"), new BigDecimal("99.99"))));
        assertEquals("[]", ids(transacciones.porRangoDeValor(new BigDecimal("101"), new BigDecimal("500"))));
    }

    @Test
    void unCambioDeEstadoMantieneLosIndicesCoherentes() {
        revertirLaTransferenciaAUnTercero();
        assertEquals(EstadoTransaccion.REVERTIDA, transacciones.buscar(trx(2)).estado());
        assertEquals(t(1, 2, 3, 6), ids(transacciones.historialDeUsuario("ana")));
        assertEquals(t(2, 4, 5, 6), ids(transacciones.historialDeUsuario("beto")));
        assertEquals(6, transacciones.ordenadasPorValor().tamanio());
        assertEquals(EstadoTransaccion.REVERTIDA, transacciones.historialDeUsuario("ana").obtener(1).estado());
        assertEquals(t(1, 2, 3, 4, 5, 6), ids(transacciones.todos()));
    }

    @Test
    void integridadReferencial() {
        assertThrows(IllegalStateException.class, () -> transacciones.registrar(recarga(6, 9, "1.00")));
        assertThrows(IllegalStateException.class, () -> transacciones.registrar(reversion(6, 99, 1, null, "1.00")));
        assertEquals(5, transacciones.cantidad());
    }

    @Test
    void usuarioQueOrigino() {
        assertEquals("ana", transacciones.usuarioQueOrigino(transacciones.buscar(trx(1))));
        assertEquals("ana", transacciones.usuarioQueOrigino(transacciones.buscar(trx(2))));
        assertEquals("beto", transacciones.usuarioQueOrigino(transacciones.buscar(trx(4))));
        assertEquals("beto", transacciones.usuarioQueOrigino(transacciones.buscar(trx(5))));
    }

    private static String pila(Pila<Transaccion> pila) {
        return ids(pila); // la iteración va de la cima al fondo
    }

    @Test
    void pilaDeReversiblesConUnCriterioRecibido() {
        CriterioReversion todas = transaccion -> true;
        assertEquals(t(3, 2, 1), pila(transacciones.reconstruirPilaReversibles("ana", todas)));
        // TRX-2 entró a Beto pero la originó Ana: no está en la pila de Beto.
        assertEquals(t(5, 4), pila(transacciones.reconstruirPilaReversibles("beto", todas)));

        revertirLaTransferenciaAUnTercero();
        // Ni la revertida (TRX-2) ni la reversión (TRX-6) son candidatas.
        assertEquals(t(3, 1), pila(transacciones.reconstruirPilaReversibles("ana", todas)));

        CriterioReversion soloRecargas = transaccion -> transaccion.tipo() == TipoTransaccion.RECARGA;
        assertEquals(t(1), pila(transacciones.reconstruirPilaReversibles("ana", soloRecargas)));

        TablaHash<String, Pila<Transaccion>> pilas = transacciones.reconstruirPilasReversibles(todas);
        assertEquals(t(3, 1), pila(pilas.obtener("ana")));
        assertEquals(t(5, 4), pila(pilas.obtener("beto")));
    }

    @Test
    void grafoDeTransferenciasConUnCriterioRecibido() {
        CriterioPesoTransferencia conteoEntreUsuarios =
                (transaccion, origen, destino) -> origen.equals(destino) ? null : BigDecimal.ONE;
        GrafoDirigidoPonderado<String> conteo = transacciones.construirGrafoTransferencias(conteoEntreUsuarios);
        assertEquals(1, conteo.cantidadAristas());
        assertEquals(0, conteo.peso("ana", "beto").compareTo(BigDecimal.ONE));
        assertFalse(conteo.contieneArista("beto", "ana"));
        assertFalse(conteo.contieneArista("ana", "ana"));

        CriterioPesoTransferencia montos = (transaccion, origen, destino) -> transaccion.valor();
        GrafoDirigidoPonderado<String> porMonto = transacciones.construirGrafoTransferencias(montos);
        assertEquals(0, porMonto.peso("ana", "beto").compareTo(new BigDecimal("50.00")));
        assertTrue(porMonto.contieneArista("ana", "ana"));
    }

    @Test
    void reconstruccionDesdeJsonDaElMismoResultado() {
        revertirLaTransferenciaAUnTercero();
        RepositorioTransacciones reconstruido = new EntornoDatos(directorio).iniciar().transacciones();
        assertEquals(ids(transacciones.todos()), ids(reconstruido.todos()));
        assertEquals(ids(transacciones.historialDeUsuario("ana")), ids(reconstruido.historialDeUsuario("ana")));
        assertEquals(ids(transacciones.historialDeUsuario("beto")), ids(reconstruido.historialDeUsuario("beto")));
        assertEquals(ids(transacciones.historialDeBilletera(billetera(3, "beto").codigo())),
                ids(reconstruido.historialDeBilletera(billetera(3, "beto").codigo())));
        assertEquals(ids(transacciones.ordenadasPorValor()), ids(reconstruido.ordenadasPorValor()));
        assertEquals(EstadoTransaccion.REVERTIDA, reconstruido.buscar(trx(2)).estado());
        CriterioReversion todas = transaccion -> true;
        assertEquals(pila(transacciones.reconstruirPilaReversibles("ana", todas)),
                pila(reconstruido.reconstruirPilaReversibles("ana", todas)));
    }
}
