package co.edu.uniquindio.fintech.controller;

import co.edu.uniquindio.fintech.benchmark.ComparacionBenchmark;
import co.edu.uniquindio.fintech.dto.GrafoRespuesta;
import co.edu.uniquindio.fintech.dto.NivelRespuesta;
import co.edu.uniquindio.fintech.dto.ProcesamientoRespuesta;
import co.edu.uniquindio.fintech.dto.PuntosRespuesta;
import co.edu.uniquindio.fintech.dto.SolicitudOperacionProgramada;
import co.edu.uniquindio.fintech.dto.SolicitudTransferencia;
import co.edu.uniquindio.fintech.model.NivelUsuario;
import co.edu.uniquindio.fintech.model.Periodicidad;
import co.edu.uniquindio.fintech.model.TipoOperacionProgramada;
import co.edu.uniquindio.fintech.service.CriterioGrafo;
import co.edu.uniquindio.fintech.soporte.SistemaPrueba;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Conversión de resultados a DTO en los controladores, invocados como objetos Java (sin
 * HTTP). La parte HTTP (rutas, @Valid, códigos) la cubren ControladoresMvcTest y ApiHttpIntegracionTest.
 */
class ControladoresDirectoTest {

    @TempDir
    Path directorio;

    @Test
    void mapeosDeRespuesta() {
        SistemaPrueba s = new SistemaPrueba(directorio);
        s.usuario("100");
        s.usuario("200");
        String a = s.billetera("100", "1000");
        String b = s.billetera("200", "0");
        UsuarioController usuarios = new UsuarioController(s.usuarios);
        TransaccionController transacciones = new TransaccionController(s.transacciones);
        AnaliticaController analitica = new AnaliticaController(s.analitica, s.grafo, s.usuarios, s.benchmark);
        OperacionProgramadaController programadas = new OperacionProgramadaController(s.programadas, s.reloj);
        RecompensaController recompensas = new RecompensaController(s.recompensas, s.usuarios);

        transacciones.transferir(new SolicitudTransferencia(a, b, new BigDecimal("100")));
        assertEquals(2, usuarios.listar().length);
        assertEquals(2, usuarios.porPuntos(null, null).length);
        assertEquals(1, usuarios.porPuntos(10L, null).length);
        PuntosRespuesta p = usuarios.puntos("100");
        assertEquals(13, p.puntosAcumulados());
        assertEquals(NivelUsuario.PLATA, p.siguienteNivel());

        NivelRespuesta[] niveles = recompensas.niveles();
        assertEquals(4, niveles.length);
        assertNull(niveles[3].puntosMaximos());
        assertEquals(Long.valueOf(500), niveles[0].puntosMaximos());

        GrafoRespuesta g = analitica.transferencias(CriterioGrafo.MONTO);
        assertEquals("MONTO", g.criterio());
        assertEquals(1, g.aristas().length);
        assertEquals(0, new BigDecimal("100").compareTo(g.aristas()[0].peso()));
        assertEquals("CANTIDAD", analitica.transferencias(null).criterio());
        assertEquals(2, analitica.rutas("100", "200", null).menosSaltos().length);
        assertEquals(0, analitica.ciclos("100", 4).ciclos().length);
        assertTrue(analitica.benchmark(ComparacionBenchmark.PRIORIDAD, 20, 1).limitaciones().contains("nanoTime"));

        programadas.programar(new SolicitudOperacionProgramada(TipoOperacionProgramada.RECARGA, BigDecimal.TEN, null, b,
                s.reloj.ahora().plusMinutes(5), Periodicidad.UNICA));
        s.reloj.avanzar(Duration.ofMinutes(10));
        ProcesamientoRespuesta r = programadas.procesar();
        assertEquals(1, r.ejecutadas());
        assertEquals(0, r.fallidas());
        assertEquals(s.reloj.ahora(), r.procesadoHasta());
    }
}
