package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.model.EventoAuditoria;
import co.edu.uniquindio.fintech.model.NivelRiesgo;
import co.edu.uniquindio.fintech.model.PatronInusual;
import co.edu.uniquindio.fintech.model.TipoNotificacion;
import co.edu.uniquindio.fintech.model.Transaccion;
import co.edu.uniquindio.fintech.soporte.SistemaPrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Los seis patrones de actividad inusual con la política provisional y un reloj controlado. */
class DeteccionServiceTest {

    @TempDir
    Path directorio;

    private SistemaPrueba s;

    @BeforeEach
    void iniciar() {
        s = new SistemaPrueba(directorio);
        s.usuario("100");
        s.usuario("200");
    }

    private static BigDecimal v(String valor) {
        return new BigDecimal(valor);
    }

    private int eventos(PatronInusual patron) {
        int n = 0;
        for (EventoAuditoria e : s.datos.auditoria().porUsuario("100")) {
            if (e.patron() == patron) {
                n++;
            }
        }
        return n;
    }

    private void minutos(long m) {
        s.reloj.avanzar(Duration.ofMinutes(m));
    }

    @Test
    void actividadNormalNoGeneraHallazgos() {
        String a = s.billetera("100", "1000");
        String b = s.billetera("200", "0");
        minutos(30);
        Transaccion t = s.transacciones.transferir(a, b, v("100"));
        minutos(30);
        s.transacciones.retirar(a, v("50"));
        assertEquals(NivelRiesgo.NINGUNO, t.nivelRiesgo());
        assertTrue(s.datos.auditoria().porUsuario("100").estaVacia());
        assertEquals(0, s.notificaciones("100", TipoNotificacion.ALERTA_SEGURIDAD));
    }

    @Test
    void transferenciasConsecutivasRapidas() {
        String a = s.billetera("100", "1000");
        String b1 = s.billetera("200", "0");
        String b2 = s.billetera("200", "0");
        String b3 = s.billetera("200", "0");
        assertEquals(NivelRiesgo.NINGUNO, s.transacciones.transferir(a, b1, v("10")).nivelRiesgo());
        minutos(1);
        assertEquals(NivelRiesgo.NINGUNO, s.transacciones.transferir(a, b2, v("10")).nivelRiesgo());
        minutos(1);
        Transaccion tercera = s.transacciones.transferir(a, b3, v("10"));
        assertEquals(NivelRiesgo.MEDIO, tercera.nivelRiesgo());
        assertEquals(1, eventos(PatronInusual.TRANSFERENCIAS_CONSECUTIVAS_RAPIDAS));
        assertEquals(tercera.id(), s.datos.auditoria().porUsuario("100").obtener(0).transaccion());
        assertEquals(1, s.notificaciones("100", TipoNotificacion.ALERTA_SEGURIDAD));
        minutos(10); // fuera de la ventana de 5 minutos
        assertEquals(NivelRiesgo.NINGUNO, s.transacciones.transferir(a, b1, v("10")).nivelRiesgo());
    }

    @Test
    void montoInusualmenteAlto() {
        String a = s.billetera("100", "5000");
        for (int i = 0; i < 3; i++) {
            minutos(10);
            s.transacciones.retirar(a, v("100"));
        }
        minutos(10);
        Transaccion alto = s.transacciones.retirar(a, v("300.01")); // > 3 × promedio (100)
        assertEquals(NivelRiesgo.ALTO, alto.nivelRiesgo());
        assertEquals(1, eventos(PatronInusual.MONTO_INUSUALMENTE_ALTO));
    }

    @Test
    void montoIgualAlLimiteNoEsInusual() {
        String a = s.billetera("100", "5000");
        for (int i = 0; i < 3; i++) {
            minutos(10);
            s.transacciones.retirar(a, v("100"));
        }
        minutos(10);
        assertEquals(NivelRiesgo.NINGUNO, s.transacciones.retirar(a, v("300.00")).nivelRiesgo());
    }

    @Test
    void destinoRepetitivo() {
        String a = s.billetera("100", "1000");
        String b = s.billetera("200", "0");
        s.transacciones.transferir(a, b, v("10"));
        minutos(10);
        s.transacciones.transferir(a, b, v("10"));
        minutos(10);
        Transaccion t = s.transacciones.transferir(a, b, v("10"));
        assertEquals(NivelRiesgo.MEDIO, t.nivelRiesgo());
        assertEquals(1, eventos(PatronInusual.DESTINO_REPETITIVO));
        assertEquals(0, eventos(PatronInusual.TRANSFERENCIAS_CONSECUTIVAS_RAPIDAS));
    }

    @Test
    void fragmentacionEntreBilleterasPropias() {
        String a1 = s.billetera("100", "500");
        String a2 = s.billetera("100", "500");
        String b = s.billetera("200", "0");
        assertEquals(NivelRiesgo.NINGUNO, s.transacciones.transferir(a1, b, v("100")).nivelRiesgo());
        minutos(1);
        Transaccion t = s.transacciones.transferir(a2, b, v("100"));
        assertEquals(NivelRiesgo.ALTO, t.nivelRiesgo());
        assertEquals(1, eventos(PatronInusual.FRAGMENTACION_ENTRE_BILLETERAS));
    }

    @Test
    void cambioBruscoDeFrecuencia() {
        String a = s.billetera("100", "100"); // 1 transacción en la semana base
        s.reloj.avanzar(Duration.ofDays(7));
        Transaccion ultima = null;
        for (int i = 0; i < 5; i++) {
            ultima = s.transacciones.recargar(a, v("10"));
            if (i < 4) {
                assertEquals(NivelRiesgo.NINGUNO, ultima.nivelRiesgo());
            }
            s.reloj.avanzar(Duration.ofHours(1));
        }
        assertEquals(NivelRiesgo.MEDIO, ultima.nivelRiesgo());
        assertEquals(1, eventos(PatronInusual.CAMBIO_BRUSCO_DE_FRECUENCIA));
    }

    @Test
    void horarioNoHabitual() {
        String a = s.billetera("100", "100");
        for (int i = 0; i < 4; i++) {
            minutos(2);
            s.transacciones.recargar(a, v("10"));
        }
        // 5 transacciones previas, todas hacia las 18 h; la siguiente es a las 3 h.
        s.reloj.fijar(s.reloj.ahora().plusDays(1).withHour(3));
        Transaccion t = s.transacciones.recargar(a, v("10"));
        assertEquals(NivelRiesgo.BAJO, t.nivelRiesgo());
        assertEquals(1, eventos(PatronInusual.HORARIO_NO_HABITUAL));
    }

    @Test
    void variosPatronesTomanElMayorRiesgoYSeAuditanTodos() {
        String a1 = s.billetera("100", "500");
        String a2 = s.billetera("100", "500");
        String b = s.billetera("200", "0");
        s.transacciones.transferir(a1, b, v("10"));
        minutos(1);
        s.transacciones.transferir(a2, b, v("10"));
        minutos(1);
        // Consecutivas (MEDIO) + destino repetido (MEDIO) + fragmentación (ALTO).
        Transaccion t = s.transacciones.transferir(a1, b, v("10"));
        assertEquals(NivelRiesgo.ALTO, t.nivelRiesgo());
        assertEquals(1, eventos(PatronInusual.TRANSFERENCIAS_CONSECUTIVAS_RAPIDAS));
        assertEquals(1, eventos(PatronInusual.DESTINO_REPETITIVO));
        assertEquals(2, eventos(PatronInusual.FRAGMENTACION_ENTRE_BILLETERAS));
        assertEquals(4, s.reiniciar().datos.auditoria().porUsuario("100").tamanio());
    }

    @Test
    void lasRechazadasYLasReversionesNoCuentanComoHistorial() {
        String a = s.billetera("100", "10");
        String b1 = s.billetera("200", "0");
        String b2 = s.billetera("200", "0");
        try {
            s.transacciones.transferir(a, b1, v("1000"));
        } catch (RuntimeException esperado) {
            // RECHAZADA: no forma parte del historial efectivo
        }
        minutos(1);
        s.transacciones.transferir(a, b1, v("1"));
        minutos(1);
        assertEquals(NivelRiesgo.NINGUNO, s.transacciones.transferir(a, b2, v("1")).nivelRiesgo());
    }
}
