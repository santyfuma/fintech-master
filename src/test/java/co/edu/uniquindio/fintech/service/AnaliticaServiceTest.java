package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.dto.SolicitudCreacionBilletera;
import co.edu.uniquindio.fintech.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.fintech.exception.SolicitudInvalidaException;
import co.edu.uniquindio.fintech.model.TipoBilletera;
import co.edu.uniquindio.fintech.model.Transaccion;
import co.edu.uniquindio.fintech.soporte.SistemaPrueba;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;

import static co.edu.uniquindio.fintech.soporte.Afirmaciones.dinero;
import static co.edu.uniquindio.fintech.soporte.Afirmaciones.lanza;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AnaliticaServiceTest {

    @TempDir
    Path directorio;

    private SistemaPrueba s;
    private String a;
    private String b;
    private String c;
    private LocalDateTime inicio;

    @BeforeEach
    void iniciar() {
        s = new SistemaPrueba(directorio);
        inicio = s.reloj.ahora();
        s.usuario("100");
        s.usuario("200");
        a = s.billetera("100", "1000");                                   // AHORRO, recarga 1000
        b = s.billeteras.crear("200", new SolicitudCreacionBilletera("C", TipoBilletera.COMPRAS)).codigo();
        c = s.billetera("200", "0");
        s.reloj.avanzar(Duration.ofMinutes(10));
        s.transacciones.transferir(a, b, new BigDecimal("100"));
        s.reloj.avanzar(Duration.ofMinutes(10));
        s.transacciones.transferir(a, c, new BigDecimal("50.50"));
        s.reloj.avanzar(Duration.ofMinutes(10));
        Transaccion revertida = s.transacciones.recargar(b, new BigDecimal("999"));
        s.transacciones.revertir(revertida.id());                         // no cuenta como efectiva
        try {
            s.transacciones.retirar(c, new BigDecimal("1000"));            // rechazada: no cuenta
        } catch (RuntimeException esperado) {
            // registrada como RECHAZADA
        }
    }

    @Test
    void billeterasMasUsadasCuentaSoloEfectivasYDesempataPorCodigo() {
        Lista<AnaliticaService.Conteo> r = s.analitica.billeterasMasUsadas(3);
        assertEquals(new AnaliticaService.Conteo(a, 3), r.obtener(0));
        assertEquals(new AnaliticaService.Conteo(b, 1), r.obtener(1));
        assertEquals(new AnaliticaService.Conteo(c, 1), r.obtener(2));
        assertEquals(1, s.analitica.billeterasMasUsadas(1).tamanio());
        lanza(SolicitudInvalidaException.class, "CANTIDAD_INVALIDA", () -> s.analitica.billeterasMasUsadas(0));
    }

    @Test
    void usuariosConMasTransferencias() {
        Lista<AnaliticaService.Conteo> r = s.analitica.usuariosConMasTransferencias(5);
        assertEquals(1, r.tamanio());
        assertEquals(new AnaliticaService.Conteo("100", 2), r.obtener(0));
    }

    @Test
    void categoriasYFrecuenciaPorTipo() {
        Lista<AnaliticaService.Conteo> cat = s.analitica.categoriasMasActivas();
        assertEquals(new AnaliticaService.Conteo("AHORRO", 4), cat.obtener(0)); // a: 3, c: 1
        assertEquals(new AnaliticaService.Conteo("COMPRAS", 1), cat.obtener(1));
        Lista<AnaliticaService.Conteo> f = s.analitica.frecuenciaPorTipo();
        assertEquals(new AnaliticaService.Conteo("RECARGA", 1), f.obtener(0)); // la revertida ya no es EXITOSA
        assertEquals(new AnaliticaService.Conteo("RETIRO", 0), f.obtener(1));
        assertEquals(new AnaliticaService.Conteo("TRANSFERENCIA", 2), f.obtener(2));
        assertEquals(new AnaliticaService.Conteo("REVERSION", 1), f.obtener(3));
    }

    @Test
    void montoMovilizadoPorPeriodoYUsuarioMasActivo() {
        AnaliticaService.MontoPeriodo todo = s.analitica.montoMovilizado(inicio, s.reloj.ahora());
        dinero("1150.50", todo.total());
        assertEquals(3, todo.transacciones());
        AnaliticaService.MontoPeriodo parte = s.analitica.montoMovilizado(inicio.plusMinutes(5), inicio.plusMinutes(15));
        dinero("100", parte.total());
        assertEquals("100", s.analitica.usuarioMasActivo(inicio, s.reloj.ahora()).clave());
        lanza(SolicitudInvalidaException.class, "PERIODO_INVALIDO",
                () -> s.analitica.montoMovilizado(s.reloj.ahora(), inicio));
        lanza(RecursoNoEncontradoException.class, "SIN_ACTIVIDAD",
                () -> s.analitica.usuarioMasActivo(inicio.minusDays(2), inicio.minusDays(1)));
    }

    @Test
    void transaccionesDeMayorValorExcluyeLasNoEfectivas() {
        Lista<Transaccion> r = s.analitica.transaccionesDeMayorValor(2);
        dinero("1000", r.obtener(0).valor());
        dinero("100", r.obtener(1).valor());
        assertEquals(3, s.analitica.transaccionesDeMayorValor(10).tamanio());
    }
}
