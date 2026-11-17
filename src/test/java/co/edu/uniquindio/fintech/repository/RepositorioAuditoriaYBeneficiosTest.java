package co.edu.uniquindio.fintech.repository;

import co.edu.uniquindio.fintech.config.ContextoDatos;
import co.edu.uniquindio.fintech.model.EstadoCanje;
import co.edu.uniquindio.fintech.soporte.EntornoDatos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.Path;

import static co.edu.uniquindio.fintech.soporte.DatosPrueba.aud;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.billetera;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.can;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.canje;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.evento;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.ids;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.recarga;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.trx;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepositorioAuditoriaYBeneficiosTest {

    @TempDir
    Path directorio;

    private ContextoDatos contexto;

    @BeforeEach
    void preparar() {
        contexto = new EntornoDatos(directorio).iniciar();
        contexto.billeteras().registrar(billetera(1, "ana"));
        contexto.transacciones().registrar(recarga(1, 1, "10.00"));
    }

    @Test
    void auditoriaRegistraYConsultaEnOrdenHistorico() {
        RepositorioAuditoria auditoria = contexto.auditoria();
        auditoria.registrar(evento(1, "ana", trx(1)));
        auditoria.registrar(evento(2, "beto", null));
        auditoria.registrar(evento(3, "ana", null));
        assertEquals("[" + aud(1) + ", " + aud(2) + ", " + aud(3) + "]", ids(auditoria.todos()));
        assertEquals("[" + aud(1) + ", " + aud(3) + "]", ids(auditoria.porUsuario("ana")));
        assertEquals(trx(1), auditoria.buscar(aud(1)).transaccion());
        assertNull(auditoria.buscar(aud(9)));
        assertThrows(IllegalStateException.class, () -> auditoria.registrar(evento(1, "ana", null)));
        assertThrows(IllegalStateException.class, () -> auditoria.registrar(evento(4, "ana", trx(99))));

        RepositorioAuditoria reconstruido = new EntornoDatos(directorio).iniciar().auditoria();
        assertEquals(ids(auditoria.todos()), ids(reconstruido.todos()));
        assertEquals(ids(auditoria.porUsuario("ana")), ids(reconstruido.porUsuario("ana")));
    }

    @Test
    void losEventosDeAuditoriaNoSeModificanNiSeEliminan() {
        for (Method metodo : RepositorioAuditoria.class.getMethods()) {
            assertFalse(metodo.getName().startsWith("eliminar"), metodo.getName());
            assertFalse(metodo.getName().startsWith("actualizar"), metodo.getName());
        }
    }

    @Test
    void beneficiosPorUsuarioYActualizacionDeEstado() {
        RepositorioBeneficios beneficios = contexto.beneficios();
        beneficios.registrar(canje(1, "ana"));
        beneficios.registrar(canje(2, "beto"));
        beneficios.registrar(canje(3, "ana"));
        beneficios.actualizar(beneficios.buscar(can(1)).conEstado(EstadoCanje.ANULADO));
        assertEquals(EstadoCanje.ANULADO, beneficios.buscar(can(1)).estado());
        assertEquals("[" + can(1) + ", " + can(3) + "]", ids(beneficios.porUsuario("ana")));
        assertEquals(EstadoCanje.ANULADO, beneficios.porUsuario("ana").obtener(0).estado());

        RepositorioBeneficios reconstruido = new EntornoDatos(directorio).iniciar().beneficios();
        assertEquals(ids(beneficios.todos()), ids(reconstruido.todos()));
        assertEquals(EstadoCanje.ANULADO, reconstruido.buscar(can(1)).estado());
        for (Method metodo : RepositorioBeneficios.class.getMethods()) {
            assertFalse(metodo.getName().startsWith("eliminar"), metodo.getName());
        }
    }
}
