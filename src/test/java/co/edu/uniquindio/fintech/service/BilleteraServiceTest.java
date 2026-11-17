package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.dto.SolicitudActualizacionBilletera;
import co.edu.uniquindio.fintech.dto.SolicitudCreacionBilletera;
import co.edu.uniquindio.fintech.exception.ConflictoNegocioException;
import co.edu.uniquindio.fintech.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.fintech.model.Billetera;
import co.edu.uniquindio.fintech.model.EstadoBilletera;
import co.edu.uniquindio.fintech.model.TipoBilletera;
import co.edu.uniquindio.fintech.soporte.SistemaPrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;

import static co.edu.uniquindio.fintech.soporte.Afirmaciones.dinero;
import static co.edu.uniquindio.fintech.soporte.Afirmaciones.lanza;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BilleteraServiceTest {

    @TempDir
    Path directorio;

    private SistemaPrueba s;

    @BeforeEach
    void iniciar() {
        s = new SistemaPrueba(directorio);
        s.usuario("100");
    }

    @Test
    void crearAsignaCodigoConsecutivoSaldoCeroYActiva() {
        Billetera a = s.billeteras.crear("100", new SolicitudCreacionBilletera("Ahorros", TipoBilletera.AHORRO));
        Billetera b = s.billeteras.crear("100", new SolicitudCreacionBilletera("Viaje", TipoBilletera.TRANSPORTE));
        assertEquals("BIL-000001", a.codigo());
        assertEquals("BIL-000002", b.codigo());
        assertEquals(0, a.saldo().signum());
        assertEquals(EstadoBilletera.ACTIVA, a.estado());
        assertEquals(2, s.billeteras.porPropietario("100").tamanio());
        assertEquals(b, s.reiniciar().billeteras.buscar("BIL-000002"));
    }

    @Test
    void crearParaUsuarioInexistenteEs404() {
        lanza(RecursoNoEncontradoException.class, "USUARIO_NO_ENCONTRADO", () -> s.billeteras.crear("999",
                new SolicitudCreacionBilletera("X", TipoBilletera.AHORRO)));
        lanza(RecursoNoEncontradoException.class, "BILLETERA_NO_ENCONTRADA", () -> s.billeteras.buscar("BIL-000777"));
        lanza(RecursoNoEncontradoException.class, "USUARIO_NO_ENCONTRADO", () -> s.billeteras.porPropietario("999"));
    }

    @Test
    void actualizarNombreYTipoConservaSaldo() {
        String c = s.billetera("100", "250.50");
        Billetera b = s.billeteras.actualizar(c, new SolicitudActualizacionBilletera("Gastos", TipoBilletera.GASTOS_DIARIOS,
                EstadoBilletera.ACTIVA));
        assertEquals("Gastos", b.nombre());
        assertEquals(TipoBilletera.GASTOS_DIARIOS, b.tipo());
        dinero("250.50", b.saldo());
        dinero("250.50", s.reiniciar().saldo(c));
    }

    @Test
    void desactivarConSaldoEsConflictoYSinSaldoFunciona() {
        String con = s.billetera("100", "10");
        String sin = s.billetera("100", "0");
        lanza(ConflictoNegocioException.class, "BILLETERA_CON_SALDO", () -> s.billeteras.desactivar(con));
        lanza(ConflictoNegocioException.class, "BILLETERA_CON_SALDO", () -> s.billeteras.actualizar(con,
                new SolicitudActualizacionBilletera("B", TipoBilletera.AHORRO, EstadoBilletera.INACTIVA)));
        assertEquals(EstadoBilletera.INACTIVA, s.billeteras.desactivar(sin).estado());
        assertEquals(EstadoBilletera.ACTIVA, s.billeteras.activar(sin).estado());
        assertEquals(EstadoBilletera.ACTIVA, s.reiniciar().billeteras.buscar(sin).estado());
    }

    @Test
    void saldoTotalSumaLasBilleterasDelUsuario() {
        s.billetera("100", "10.25");
        s.billetera("100", "0.75");
        s.usuario("200");
        s.billetera("200", "999");
        dinero("11.00", s.billeteras.saldoTotal("100"));
        lanza(RecursoNoEncontradoException.class, "USUARIO_NO_ENCONTRADO", () -> s.billeteras.saldoTotal("9"));
    }

    @Test
    void saldoConsultaConEscala() {
        String c = s.billetera("100", "0");
        s.transacciones.recargar(c, new BigDecimal("1050.00"));
        dinero("1050.00", s.saldo(c));
        dinero("1050.00", s.reiniciar().saldo(c));
    }
}
