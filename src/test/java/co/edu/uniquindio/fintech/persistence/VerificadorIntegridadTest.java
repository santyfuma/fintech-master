package co.edu.uniquindio.fintech.persistence;

import co.edu.uniquindio.fintech.exception.PersistenciaException;
import co.edu.uniquindio.fintech.model.EstadoTransaccion;
import co.edu.uniquindio.fintech.model.Transaccion;
import org.junit.jupiter.api.Test;

import static co.edu.uniquindio.fintech.soporte.DatosPrueba.billetera;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.canje;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.evento;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.lista;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.notificacion;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.recarga;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.reversion;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.transferencia;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.transferenciaProgramada;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.trx;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.usuario;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.FECHA;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificadorIntegridadTest {

    private static DatosCargados datos(co.edu.uniquindio.fintech.structures.interfaces.Lista<Transaccion> transacciones) {
        return new DatosCargados(lista(usuario("u1", 0), usuario("u2", 0)),
                lista(billetera(1, "u1"), billetera(2, "u2")), transacciones, lista(), lista(), lista(), lista());
    }

    private static void invalido(DatosCargados datos, String archivo) {
        PersistenciaException ex = assertThrows(PersistenciaException.class,
                () -> VerificadorIntegridad.verificar(datos));
        assertTrue(ex.getMessage().contains(archivo), ex.getMessage());
    }

    @Test
    void datosCoherentesPasan() {
        Transaccion original = transferencia(1, 1, 2, "10.00").conEstado(EstadoTransaccion.REVERTIDA);
        VerificadorIntegridad.verificar(new DatosCargados(lista(usuario("u1", 0)),
                lista(billetera(1, "u1"), billetera(2, "u2")),
                lista(original, reversion(2, 1, 2, 1, "10.00")),
                lista(transferenciaProgramada(1, 1, 2, FECHA)), lista(notificacion(1, "u-borrado")),
                lista(evento(1, "u1", trx(1))), lista(canje(1, "u1"))));
    }

    @Test
    void identificacionDeUsuarioRepetida() {
        invalido(new DatosCargados(lista(usuario("u1", 0), usuario("u1", 5)), lista(), lista(), lista(), lista(),
                lista(), lista()), "usuarios.json");
    }

    @Test
    void identificadoresRepetidosOFueraDeOrden() {
        invalido(new DatosCargados(lista(), lista(billetera(2, "u"), billetera(1, "u")), lista(), lista(), lista(),
                lista(), lista()), "billeteras.json");
        invalido(new DatosCargados(lista(), lista(billetera(1, "u"), billetera(1, "u")), lista(), lista(), lista(),
                lista(), lista()), "billeteras.json");
        invalido(new DatosCargados(lista(), lista(), lista(), lista(), lista(notificacion(3, "u"),
                notificacion(3, "u")), lista(), lista()), "notificaciones.json");
    }

    @Test
    void referenciasAInexistentes() {
        invalido(datos(lista(recarga(1, 9, "1.00"))), "transacciones.json");
        invalido(new DatosCargados(lista(), lista(billetera(1, "u")), lista(), lista(transferenciaProgramada(1, 1, 5,
                FECHA)), lista(), lista(), lista()), "operaciones-programadas.json");
        invalido(new DatosCargados(lista(), lista(), lista(), lista(), lista(), lista(evento(1, "u", trx(4))),
                lista()), "auditoria.json");
    }

    @Test
    void reversionesIncoherentes() {
        // Revierte a una transacción inexistente o posterior.
        invalido(datos(lista(reversion(1, 2, 1, null, "1.00"), recarga(2, 1, "1.00"))), "transacciones.json");
        // Reversión exitosa pero la original sigue EXITOSA.
        invalido(datos(lista(recarga(1, 1, "1.00"), reversion(2, 1, 1, null, "1.00"))), "transacciones.json");
        // Original REVERTIDA sin reversión.
        invalido(datos(lista(recarga(1, 1, "1.00").conEstado(EstadoTransaccion.REVERTIDA))), "transacciones.json");
        // Dos reversiones exitosas de la misma original.
        Transaccion revertida = recarga(1, 1, "1.00").conEstado(EstadoTransaccion.REVERTIDA);
        invalido(datos(lista(revertida, reversion(2, 1, 1, null, "1.00"), reversion(3, 1, 1, null, "1.00"))),
                "transacciones.json");
    }

    @Test
    void unaReversionRechazadaNoCambiaLaOriginal() {
        Transaccion rechazada = reversion(2, 1, 1, null, "1.00").conEstado(EstadoTransaccion.RECHAZADA);
        VerificadorIntegridad.verificar(datos(lista(recarga(1, 1, "1.00"), rechazada)));
    }
}
