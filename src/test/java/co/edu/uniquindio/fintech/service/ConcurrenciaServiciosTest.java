package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.exception.ConflictoNegocioException;
import co.edu.uniquindio.fintech.soporte.SistemaPrueba;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static co.edu.uniquindio.fintech.soporte.Afirmaciones.dinero;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Operaciones simultáneas sobre las mismas billeteras: sin dinero perdido ni saldos negativos. */
class ConcurrenciaServiciosTest {

    @TempDir
    Path directorio;

    @Test
    void retirosSimultaneosNuncaDejanSaldoNegativo() throws Exception {
        SistemaPrueba s = new SistemaPrueba(directorio);
        s.usuario("100");
        String a = s.billetera("100", "100");
        int hilos = 8;
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger rechazos = new AtomicInteger();
        Thread[] t = new Thread[hilos];
        for (int i = 0; i < hilos; i++) {
            t[i] = new Thread(() -> {
                try {
                    salida.await();
                    s.transacciones.retirar(a, new BigDecimal("30"));
                    exitos.incrementAndGet();
                } catch (ConflictoNegocioException e) {
                    rechazos.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            t[i].start();
        }
        salida.countDown();
        for (Thread x : t) {
            x.join();
        }
        assertEquals(3, exitos.get());
        assertEquals(hilos - 3, rechazos.get());
        dinero("10", s.saldo(a));
        SistemaPrueba r = s.reiniciar(); // el verificador de integridad acepta los archivos
        dinero("10", r.saldo(a));
        assertEquals(1 + hilos, r.transacciones.todas().tamanio());
    }

    @Test
    void transferenciasCruzadasConservanElTotal() throws Exception {
        SistemaPrueba s = new SistemaPrueba(directorio);
        s.usuario("1");
        s.usuario("2");
        String a = s.billetera("1", "1000");
        String b = s.billetera("2", "1000");
        Thread ida = new Thread(() -> {
            for (int i = 0; i < 20; i++) {
                s.transacciones.transferir(a, b, new BigDecimal("7.25"));
            }
        });
        Thread vuelta = new Thread(() -> {
            for (int i = 0; i < 20; i++) {
                s.transacciones.transferir(b, a, new BigDecimal("3.10"));
            }
        });
        ida.start();
        vuelta.start();
        ida.join();
        vuelta.join();
        dinero("917.00", s.saldo(a));
        dinero("1083.00", s.saldo(b));
        SistemaPrueba r = s.reiniciar();
        assertEquals(0, r.saldo(a).add(r.saldo(b)).compareTo(new BigDecimal("2000")));
    }
}
