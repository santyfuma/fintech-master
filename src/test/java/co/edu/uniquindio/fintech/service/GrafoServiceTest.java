package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.fintech.exception.SolicitudInvalidaException;
import co.edu.uniquindio.fintech.soporte.SistemaPrueba;
import co.edu.uniquindio.fintech.structures.interfaces.Arista;
import co.edu.uniquindio.fintech.structures.interfaces.GrafoDirigidoPonderado;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;

import static co.edu.uniquindio.fintech.soporte.Afirmaciones.dinero;
import static co.edu.uniquindio.fintech.soporte.Afirmaciones.lanza;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Grafo de transferencias entre usuarios: A→B (2 veces, 10 c/u), B→C (500), A→C (1), A→A interna. */
class GrafoServiceTest {

    @TempDir
    Path directorio;

    private SistemaPrueba s;
    private String a;
    private String a2;
    private String b;
    private String c;

    @BeforeEach
    void iniciar() {
        s = new SistemaPrueba(directorio);
        s.usuario("A");
        s.usuario("B");
        s.usuario("C");
        a = s.billetera("A", "1000");
        a2 = s.billetera("A", "0");
        b = s.billetera("B", "1000");
        c = s.billetera("C", "0");
        transferir(a, b, "10");
        transferir(a, b, "10");
        transferir(b, c, "500");
        transferir(a, c, "1");
        transferir(a, a2, "5"); // interna: no es relación entre usuarios
    }

    private void transferir(String o, String d, String v) {
        s.reloj.avanzar(Duration.ofMinutes(15));
        s.transacciones.transferir(o, d, new BigDecimal(v));
    }

    @Test
    void verticesYPesosSegunElCriterio() {
        GrafoDirigidoPonderado<String> cantidad = s.grafo.grafo(CriterioGrafo.CANTIDAD);
        assertEquals(3, cantidad.cantidadVertices());
        assertEquals(3, cantidad.cantidadAristas());
        assertFalse(cantidad.contieneArista("A", "A"));
        dinero("2", cantidad.peso("A", "B"));
        GrafoDirigidoPonderado<String> monto = s.grafo.grafo(CriterioGrafo.MONTO);
        dinero("20", monto.peso("A", "B"));
        dinero("500", monto.peso("B", "C"));
    }

    @Test
    void rutasPorSaltosYPorPeso() {
        GrafoService.Rutas porMonto = s.grafo.rutas("A", "C", CriterioGrafo.MONTO);
        assertEquals(2, porMonto.menosSaltos().tamanio());   // A → C directo
        assertEquals(2, porMonto.menorPeso().tamanio());     // peso 1 < 20 + 500
        dinero("1", porMonto.pesoMenor());
        GrafoService.Rutas sinRuta = s.grafo.rutas("C", "A", CriterioGrafo.CANTIDAD);
        assertTrue(sinRuta.menosSaltos().estaVacia());
        assertEquals(null, sinRuta.pesoMenor());
        lanza(RecursoNoEncontradoException.class, "USUARIO_SIN_TRANSFERENCIAS",
                () -> s.grafo.rutas("A", "Z", CriterioGrafo.CANTIDAD));
    }

    @Test
    void relacionesFrecuentesOrdenadasPorPeso() {
        Lista<Arista<String>> r = s.grafo.relacionesFrecuentes(2, CriterioGrafo.MONTO);
        assertEquals("B", r.obtener(0).origen());
        assertEquals("C", r.obtener(0).destino());
        assertEquals("A", r.obtener(1).origen());
        assertEquals("B", r.obtener(1).destino());
        lanza(SolicitudInvalidaException.class, "CANTIDAD_INVALIDA", () -> s.grafo.relacionesFrecuentes(0, null));
    }

    @Test
    void ciclosSoloConTransferenciasEfectivas() {
        assertFalse(s.grafo.tieneCiclo());
        s.usuario("D");
        transferir(c, b, "100");
        assertTrue(s.grafo.tieneCiclo()); // B → C → B
        Lista<Lista<String>> ciclos = s.grafo.ciclosQueIncluyen("B", 3);
        assertEquals(1, ciclos.tamanio());
        assertEquals("B", ciclos.obtener(0).obtener(0));
        assertTrue(s.grafo.ciclosQueIncluyen("D", 3).estaVacia());
        lanza(SolicitudInvalidaException.class, "LONGITUD_INVALIDA", () -> s.grafo.ciclosQueIncluyen("B", 0));
        lanza(SolicitudInvalidaException.class, "LONGITUD_INVALIDA", () -> s.grafo.ciclosQueIncluyen("B", 11));
        // Revertir la transferencia que cierra el ciclo lo elimina del grafo.
        s.transacciones.revertirUltima("C");
        assertFalse(s.grafo.tieneCiclo());
    }
}
