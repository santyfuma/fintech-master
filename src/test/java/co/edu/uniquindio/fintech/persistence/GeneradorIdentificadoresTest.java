package co.edu.uniquindio.fintech.persistence;

import co.edu.uniquindio.fintech.exception.PersistenciaException;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static co.edu.uniquindio.fintech.soporte.DatosPrueba.billetera;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.canje;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.lista;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.notificacion;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.recarga;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneradorIdentificadoresTest {

    private static DatosCargados vacios() {
        return new DatosCargados(lista(), lista(), lista(), lista(), lista(), lista(), lista());
    }

    @Test
    void generacionInicialEIncremento() {
        GeneradorIdentificadores g = new GeneradorIdentificadores();
        assertEquals(0, g.ultimoConsecutivo(TipoIdentificador.BILLETERA));
        assertEquals("BIL-000001", g.siguiente(TipoIdentificador.BILLETERA));
        assertEquals("BIL-000002", g.siguiente(TipoIdentificador.BILLETERA));
        assertEquals("BIL-000003", g.siguiente(TipoIdentificador.BILLETERA));
        assertEquals(3, g.ultimoConsecutivo(TipoIdentificador.BILLETERA));
    }

    @Test
    void cadaPrefijoTieneSuPropioConsecutivo() {
        GeneradorIdentificadores g = new GeneradorIdentificadores();
        g.siguiente(TipoIdentificador.BILLETERA);
        g.siguiente(TipoIdentificador.BILLETERA);
        assertEquals("TRX-000001", g.siguiente(TipoIdentificador.TRANSACCION));
        assertEquals("OPR-000001", g.siguiente(TipoIdentificador.OPERACION_PROGRAMADA));
        assertEquals("NOT-000001", g.siguiente(TipoIdentificador.NOTIFICACION));
        assertEquals("AUD-000001", g.siguiente(TipoIdentificador.EVENTO_AUDITORIA));
        assertEquals("CAN-000001", g.siguiente(TipoIdentificador.BENEFICIO_CANJEADO));
        assertEquals("BIL-000003", g.siguiente(TipoIdentificador.BILLETERA));
    }

    @Test
    void formatoConSeisDigitosOMas() {
        assertEquals("TRX-000042", GeneradorIdentificadores.formatear(TipoIdentificador.TRANSACCION, 42));
        assertEquals("OPR-000003", GeneradorIdentificadores.formatear(TipoIdentificador.OPERACION_PROGRAMADA, 3));
        assertEquals("BIL-999999", GeneradorIdentificadores.formatear(TipoIdentificador.BILLETERA, 999_999));
        assertEquals("BIL-1000000", GeneradorIdentificadores.formatear(TipoIdentificador.BILLETERA, 1_000_000));
        assertThrows(IllegalArgumentException.class,
                () -> GeneradorIdentificadores.formatear(TipoIdentificador.BILLETERA, 0));
    }

    @Test
    void superarSeisDigitosAmpliaElAnchoSinReiniciar() {
        GeneradorIdentificadores g = new GeneradorIdentificadores();
        g.registrarExistente(TipoIdentificador.BILLETERA, "BIL-999999");
        assertEquals("BIL-1000000", g.siguiente(TipoIdentificador.BILLETERA));
        assertEquals("BIL-1000001", g.siguiente(TipoIdentificador.BILLETERA));
        g.registrarExistente(TipoIdentificador.TRANSACCION, "TRX-1234567");
        assertEquals("TRX-1234568", g.siguiente(TipoIdentificador.TRANSACCION));
        assertEquals(1_234_567, GeneradorIdentificadores.consecutivo(TipoIdentificador.TRANSACCION, "TRX-1234567"));
    }

    @Test
    void recuperaElMaximoExistenteDeCadaArchivo() {
        DatosCargados datos = new DatosCargados(lista(),
                lista(billetera(1, "u"), billetera(7, "u")),
                lista(recarga(3, 1, "1")),
                lista(), lista(notificacion(12, "u")), lista(), lista(canje(2, "u")));
        GeneradorIdentificadores g = GeneradorIdentificadores.desde(datos);
        assertEquals("BIL-000008", g.siguiente(TipoIdentificador.BILLETERA));
        assertEquals("TRX-000004", g.siguiente(TipoIdentificador.TRANSACCION));
        assertEquals("NOT-000013", g.siguiente(TipoIdentificador.NOTIFICACION));
        assertEquals("CAN-000003", g.siguiente(TipoIdentificador.BENEFICIO_CANJEADO));
        assertEquals("OPR-000001", g.siguiente(TipoIdentificador.OPERACION_PROGRAMADA));
        assertEquals("AUD-000001", g.siguiente(TipoIdentificador.EVENTO_AUDITORIA));
    }

    @Test
    void conArchivosVaciosEmpiezaEnUno() {
        GeneradorIdentificadores g = GeneradorIdentificadores.desde(vacios());
        for (TipoIdentificador tipo : TipoIdentificador.values()) {
            assertEquals(tipo.prefijo() + "-000001", g.siguiente(tipo));
        }
    }

    @Test
    void nuncaRetrocedeNiReutiliza() {
        GeneradorIdentificadores g = new GeneradorIdentificadores();
        g.registrarExistente(TipoIdentificador.BILLETERA, "BIL-000010");
        g.registrarExistente(TipoIdentificador.BILLETERA, "BIL-000002");
        assertEquals("BIL-000011", g.siguiente(TipoIdentificador.BILLETERA));
    }

    @Test
    void identificadoresInvalidosLanzanPersistencia() {
        GeneradorIdentificadores g = new GeneradorIdentificadores();
        String[] invalidos = {null, "", "BIL-12", "BIL-000000", "TRX-000001", "bil-000001", "BIL000001",
                "BIL-99999999999999999999"};
        for (String invalido : invalidos) {
            assertThrows(PersistenciaException.class,
                    () -> g.registrarExistente(TipoIdentificador.BILLETERA, invalido), String.valueOf(invalido));
        }
        assertEquals(0, g.ultimoConsecutivo(TipoIdentificador.BILLETERA));
    }

    @Test
    void generacionConcurrenteSinDuplicados() throws InterruptedException {
        GeneradorIdentificadores g = new GeneradorIdentificadores();
        int hilos = 8;
        int porHilo = 2_000;
        Set<String> generados = ConcurrentHashMap.newKeySet();
        CountDownLatch inicio = new CountDownLatch(1);
        Thread[] trabajadores = new Thread[hilos];
        for (int i = 0; i < hilos; i++) {
            trabajadores[i] = new Thread(() -> {
                try {
                    inicio.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int j = 0; j < porHilo; j++) {
                    generados.add(g.siguiente(TipoIdentificador.TRANSACCION));
                }
            });
            trabajadores[i].start();
        }
        inicio.countDown();
        for (Thread t : trabajadores) {
            t.join();
        }
        assertEquals(hilos * porHilo, generados.size());
        assertEquals(hilos * porHilo, g.ultimoConsecutivo(TipoIdentificador.TRANSACCION));
        assertTrue(generados.contains("TRX-016000"));
        assertTrue(generados.contains("TRX-000001"));
    }
}
