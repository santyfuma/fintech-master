package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.model.NivelUsuario;
import co.edu.uniquindio.fintech.model.TipoTransaccion;
import co.edu.uniquindio.fintech.model.Usuario;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuntosServiceTest {

    private final PuntosService puntos = new PuntosService(PoliticaPuntos.provisional());

    @Test
    void calculoPorTipoConDivisionEnteraYBono() {
        assertEquals(0, puntos.calcular(TipoTransaccion.RECARGA, new BigDecimal("99.99"), false));
        assertEquals(1, puntos.calcular(TipoTransaccion.RECARGA, new BigDecimal("100"), false));
        assertEquals(4, puntos.calcular(TipoTransaccion.RETIRO, new BigDecimal("250"), false));
        assertEquals(9, puntos.calcular(TipoTransaccion.TRANSFERENCIA, new BigDecimal("399.99"), false));
        assertEquals(19, puntos.calcular(TipoTransaccion.TRANSFERENCIA, new BigDecimal("300"), true));
        assertEquals(0, puntos.calcular(TipoTransaccion.REVERSION, new BigDecimal("1000"), false));
    }

    @Test
    void nivelesEnLosLimitesDelEnunciado() {
        assertEquals(NivelUsuario.BRONCE, puntos.nivelPara(0));
        assertEquals(NivelUsuario.BRONCE, puntos.nivelPara(500));
        assertEquals(NivelUsuario.PLATA, puntos.nivelPara(501));
        assertEquals(NivelUsuario.PLATA, puntos.nivelPara(1000));
        assertEquals(NivelUsuario.ORO, puntos.nivelPara(1001));
        assertEquals(NivelUsuario.ORO, puntos.nivelPara(5000));
        assertEquals(NivelUsuario.PLATINO, puntos.nivelPara(5001));
        assertEquals(NivelUsuario.PLATINO, puntos.nivelPara(Long.MAX_VALUE));
    }

    @Test
    void ajustarNoBajaDeCeroNiDesbordaYRecalculaNivel() {
        Usuario u = new Usuario("1", "A", "a@b.co", 600, NivelUsuario.PLATA);
        Usuario menos = puntos.ajustar(u, -1000);
        assertEquals(0, menos.puntosAcumulados());
        assertEquals(NivelUsuario.BRONCE, menos.nivel());
        Usuario mas = puntos.ajustar(u, Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, mas.puntosAcumulados());
        assertTrue(puntos.asciende(u, mas));
        assertFalse(puntos.asciende(u, menos));
        assertNull(puntos.siguienteNivel(NivelUsuario.PLATINO));
        assertEquals(NivelUsuario.PLATA, puntos.siguienteNivel(NivelUsuario.BRONCE));
    }
}
