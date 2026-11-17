package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.dto.SolicitudCreacionBilletera;
import co.edu.uniquindio.fintech.exception.PersistenciaException;
import co.edu.uniquindio.fintech.model.TipoBilletera;
import co.edu.uniquindio.fintech.persistence.AlmacenesJson;
import co.edu.uniquindio.fintech.soporte.SistemaPrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;

import static co.edu.uniquindio.fintech.soporte.Afirmaciones.dinero;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Fallos de escritura en mitad de una operación de servicio: nunca queda aplicada a medias. */
class FallosPersistenciaServiciosTest {

    @TempDir
    Path directorio;

    private SistemaPrueba s;
    private String a;

    @BeforeEach
    void iniciar() {
        s = new SistemaPrueba(directorio);
        s.usuario("100");
        a = s.billeteras.crear("100", new SolicitudCreacionBilletera("A", TipoBilletera.AHORRO)).codigo();
    }

    @Test
    void falloDespuesDeRegistrarEnElDiarioComprometeLaOperacionYBloqueaLasSiguientes() {
        s.entorno.bloquearEscritura("transacciones.json");
        assertThrows(PersistenciaException.class, () -> s.transacciones.recargar(a, new BigDecimal("100")));
        // Quedó en el diario: la operación está comprometida y la memoria la refleja.
        assertEquals(1, s.entorno.diario.pendientes().tamanio());
        dinero("100", s.saldo(a));
        // Mientras siga pendiente no se acepta otra modificación (ni de otro servicio).
        assertThrows(PersistenciaException.class, () -> s.transacciones.recargar(a, new BigDecimal("50")));
        assertThrows(PersistenciaException.class, () -> s.billeteras.desactivar(a));
        dinero("100", s.saldo(a));
        assertEquals(1, s.transacciones.todas().tamanio());

        s.desbloquear("transacciones.json");
        s.transacciones.recargar(a, new BigDecimal("50"));   // primero reaplica la pendiente
        assertTrue(s.entorno.diario.pendientes().estaVacia());
        SistemaPrueba r = s.reiniciar();
        dinero("150", r.saldo(a));
        assertEquals(2, r.transacciones.todas().tamanio());
        assertEquals(1, r.puntosDe("100"));
    }

    @Test
    void reinicioCompletaLaOperacionQueQuedoEnElDiario() {
        s.entorno.bloquearEscritura("transacciones.json");
        assertThrows(PersistenciaException.class, () -> s.transacciones.recargar(a, new BigDecimal("100")));
        s.desbloquear("transacciones.json");
        SistemaPrueba r = s.reiniciar(); // el arranque recupera el diario
        dinero("100", r.saldo(a));
        assertEquals(1, r.transacciones.todas().tamanio());
        assertTrue(r.entorno.diario.pendientes().estaVacia());
    }

    @Test
    void falloAntesDeRegistrarNoCambiaNada() {
        s.entorno.bloquearEscritura(AlmacenesJson.ARCHIVO_DIARIO);
        assertThrows(PersistenciaException.class, () -> s.transacciones.recargar(a, new BigDecimal("100")));
        dinero("0", s.saldo(a));
        assertEquals(0, s.transacciones.todas().tamanio());
        assertEquals(0, s.puntosDe("100"));
        s.desbloquear(AlmacenesJson.ARCHIVO_DIARIO);
        s.transacciones.recargar(a, new BigDecimal("100"));
        SistemaPrueba r = s.reiniciar();
        dinero("100", r.saldo(a));
        assertEquals(1, r.transacciones.todas().tamanio());
    }

    @Test
    void falloEnLaEscrituraDirectaDeUnRepositorioNoCambiaLaMemoria() {
        s.entorno.bloquearEscritura("usuarios.json");
        assertThrows(PersistenciaException.class, () -> s.usuario("300"));
        assertEquals(1, s.usuarios.listar().tamanio());
        assertEquals(0, s.entorno.temporales());
    }
}
