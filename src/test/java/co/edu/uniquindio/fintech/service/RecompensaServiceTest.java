package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.exception.ConflictoNegocioException;
import co.edu.uniquindio.fintech.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.fintech.model.BeneficioCanjeado;
import co.edu.uniquindio.fintech.model.EstadoCanje;
import co.edu.uniquindio.fintech.model.NivelUsuario;
import co.edu.uniquindio.fintech.model.TipoNotificacion;
import co.edu.uniquindio.fintech.soporte.RelojAjustable;
import co.edu.uniquindio.fintech.soporte.SistemaPrueba;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static co.edu.uniquindio.fintech.soporte.Afirmaciones.lanza;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecompensaServiceTest {

    @TempDir
    Path directorio;

    private SistemaPrueba conCatalogo() {
        SistemaPrueba s = new SistemaPrueba(directorio, new RelojAjustable(), "CAFE|Bono café|8; CINE|Entrada de cine|500");
        s.usuario("100");
        return s;
    }

    @Test
    void canjeDescuentaPuntosRegistraYNotificaEnUnaOperacion() {
        SistemaPrueba s = conCatalogo();
        s.billetera("100", "2000"); // 20 puntos
        BeneficioCanjeado c = s.recompensas.canjear("100", "CAFE");
        assertEquals(EstadoCanje.REALIZADO, c.estado());
        assertEquals(8, c.puntosDescontados());
        assertEquals(12, s.puntosDe("100"));
        assertEquals(1, s.notificaciones("100", TipoNotificacion.CANJE_REALIZADO));
        assertTrue(s.entorno.diario.pendientes().estaVacia());
        SistemaPrueba r = s.reiniciar();
        assertEquals(12, r.puntosDe("100"));
        assertEquals(c, r.recompensas.canjesDe("100").obtener(0));
    }

    @Test
    void erroresDeCanje() {
        SistemaPrueba s = conCatalogo();
        s.billetera("100", "500"); // 5 puntos
        lanza(ConflictoNegocioException.class, "PUNTOS_INSUFICIENTES", () -> s.recompensas.canjear("100", "CAFE"));
        lanza(RecursoNoEncontradoException.class, "BENEFICIO_NO_ENCONTRADO", () -> s.recompensas.canjear("100", "NADA"));
        lanza(RecursoNoEncontradoException.class, "USUARIO_NO_ENCONTRADO", () -> s.recompensas.canjear("9", "CAFE"));
        assertEquals(5, s.puntosDe("100"));
        assertTrue(s.recompensas.canjesDe("100").estaVacia());
    }

    @Test
    void sinCatalogoDefinidoNoSePuedeCanjear() {
        SistemaPrueba s = new SistemaPrueba(directorio);
        s.usuario("100");
        assertTrue(s.recompensas.catalogo().estaVacia());
        lanza(ConflictoNegocioException.class, "CATALOGO_NO_DEFINIDO", () -> s.recompensas.canjear("100", "CAFE"));
        assertEquals(0, s.recompensas.ventajas(NivelUsuario.ORO).length);
    }

    @Test
    void elNivelSeRecalculaConLosPuntosRestantes() {
        SistemaPrueba s = conCatalogo();
        s.billetera("100", "60000"); // 600 puntos: PLATA
        assertEquals(NivelUsuario.PLATA, s.usuarios.buscar("100").nivel());
        s.recompensas.canjear("100", "CINE");
        assertEquals(100, s.puntosDe("100"));
        assertEquals(NivelUsuario.BRONCE, s.usuarios.buscar("100").nivel());
    }

    @Test
    void catalogoMalFormadoSeRechazaAlArrancar() {
        assertThrows(IllegalArgumentException.class, () -> new SistemaPrueba(directorio, new RelojAjustable(), "CAFE|x"));
        assertThrows(IllegalArgumentException.class,
                () -> new SistemaPrueba(directorio, new RelojAjustable(), "CAFE|x|0"));
        assertThrows(IllegalArgumentException.class,
                () -> new SistemaPrueba(directorio, new RelojAjustable(), "CAFE|x|1;CAFE|y|2"));
    }
}
