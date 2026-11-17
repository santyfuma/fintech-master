package co.edu.uniquindio.fintech.persistence;

import co.edu.uniquindio.fintech.exception.PersistenciaException;
import co.edu.uniquindio.fintech.model.BeneficioCanjeado;
import co.edu.uniquindio.fintech.model.Billetera;
import co.edu.uniquindio.fintech.model.EstadoTransaccion;
import co.edu.uniquindio.fintech.model.EventoAuditoria;
import co.edu.uniquindio.fintech.model.Notificacion;
import co.edu.uniquindio.fintech.model.OperacionProgramada;
import co.edu.uniquindio.fintech.model.Transaccion;
import co.edu.uniquindio.fintech.model.Usuario;
import co.edu.uniquindio.fintech.structures.adaptadores.TablaHashAdaptador;
import co.edu.uniquindio.fintech.structures.interfaces.TablaHash;

/**
 * Verifica la integridad de los datos cargados antes de construir los repositorios
 * (paso 3 del arranque). Cualquier problema detiene el arranque con
 * {@link PersistenciaException}.
 *
 * <p>Comprueba:
 * <ul>
 *   <li>identificaciones de usuario únicas;</li>
 *   <li>identificadores generados con formato válido y en orden estrictamente ascendente
 *       dentro de cada archivo (lo que además garantiza que no se repiten);</li>
 *   <li>referencias que nunca pueden quedar huérfanas, porque su destino no se borra:
 *       billeteras de transacciones y operaciones programadas, transacción de un evento
 *       de auditoría y transacción original de una reversión (que debe ser anterior);</li>
 *   <li>coherencia de las reversiones (CONTEXTO.md 4.1): una reversión EXITOSA deja a la
 *       original en estado REVERTIDA, y cada transacción REVERTIDA tiene exactamente una
 *       reversión EXITOSA.</li>
 * </ul>
 * No se verifican las referencias a usuarios (billetera → propietario, notificación,
 * auditoría o canje → usuario), porque el borrado de usuarios está permitido y sus
 * reglas las define la capa de servicios.
 */
public final class VerificadorIntegridad {

    private VerificadorIntegridad() {
    }

    public static void verificar(DatosCargados datos) {
        TablaHash<String, Boolean> usuarios = new TablaHashAdaptador<>();
        for (Usuario u : datos.usuarios()) {
            if (usuarios.poner(u.identificacion(), Boolean.TRUE) != null) {
                fallar("usuarios.json", "identificación repetida " + u.identificacion());
            }
        }

        TablaHash<String, Boolean> billeteras = new TablaHashAdaptador<>();
        long anterior = 0;
        for (Billetera b : datos.billeteras()) {
            anterior = ascendente("billeteras.json", TipoIdentificador.BILLETERA, b.codigo(), anterior);
            billeteras.poner(b.codigo(), Boolean.TRUE);
        }

        TablaHash<String, Transaccion> transacciones = new TablaHashAdaptador<>();
        TablaHash<String, Integer> reversionesExitosas = new TablaHashAdaptador<>();
        anterior = 0;
        for (Transaccion t : datos.transacciones()) {
            anterior = ascendente("transacciones.json", TipoIdentificador.TRANSACCION, t.id(), anterior);
            billeteraExistente("transacciones.json", t.id(), t.billeteraOrigen(), billeteras);
            billeteraExistente("transacciones.json", t.id(), t.billeteraDestino(), billeteras);
            if (t.esReversion()) {
                Transaccion original = transacciones.obtener(t.transaccionRevertida());
                if (original == null) {
                    fallar("transacciones.json", t.id() + " revierte a " + t.transaccionRevertida()
                            + ", que no existe o no es anterior");
                }
                if (t.estado() == EstadoTransaccion.EXITOSA) {
                    Integer previas = reversionesExitosas.obtener(original.id());
                    reversionesExitosas.poner(original.id(), previas == null ? 1 : previas + 1);
                }
            }
            transacciones.poner(t.id(), t);
        }
        for (Transaccion t : datos.transacciones()) {
            Integer reversiones = reversionesExitosas.obtener(t.id());
            int cantidad = reversiones == null ? 0 : reversiones;
            if (t.estado() == EstadoTransaccion.REVERTIDA && cantidad != 1) {
                fallar("transacciones.json", t.id() + " está REVERTIDA pero tiene " + cantidad
                        + " reversiones exitosas");
            }
            if (t.estado() != EstadoTransaccion.REVERTIDA && cantidad > 0) {
                fallar("transacciones.json", t.id() + " tiene una reversión exitosa pero su estado es " + t.estado());
            }
        }

        anterior = 0;
        for (OperacionProgramada o : datos.operacionesProgramadas()) {
            anterior = ascendente("operaciones-programadas.json", TipoIdentificador.OPERACION_PROGRAMADA, o.id(),
                    anterior);
            billeteraExistente("operaciones-programadas.json", o.id(), o.billeteraOrigen(), billeteras);
            billeteraExistente("operaciones-programadas.json", o.id(), o.billeteraDestino(), billeteras);
        }
        anterior = 0;
        for (Notificacion n : datos.notificaciones()) {
            anterior = ascendente("notificaciones.json", TipoIdentificador.NOTIFICACION, n.id(), anterior);
        }
        anterior = 0;
        for (EventoAuditoria e : datos.auditoria()) {
            anterior = ascendente("auditoria.json", TipoIdentificador.EVENTO_AUDITORIA, e.id(), anterior);
            if (e.transaccion() != null && !transacciones.contieneClave(e.transaccion())) {
                fallar("auditoria.json", e.id() + " referencia la transacción inexistente " + e.transaccion());
            }
        }
        anterior = 0;
        for (BeneficioCanjeado c : datos.beneficios()) {
            anterior = ascendente("beneficios.json", TipoIdentificador.BENEFICIO_CANJEADO, c.id(), anterior);
        }
    }

    private static long ascendente(String archivo, TipoIdentificador tipo, String id, long anterior) {
        long actual;
        try {
            actual = GeneradorIdentificadores.consecutivo(tipo, id);
        } catch (PersistenciaException e) {
            throw new PersistenciaException("Datos inválidos en " + archivo + ": " + e.getMessage(), e);
        }
        if (actual <= anterior) {
            fallar(archivo, "el identificador " + id + " está repetido o fuera de orden (los registros deben estar "
                    + "en orden ascendente de consecutivo)");
        }
        return actual;
    }

    private static void billeteraExistente(String archivo, String id, String billetera,
                                           TablaHash<String, Boolean> billeteras) {
        if (billetera != null && !billeteras.contieneClave(billetera)) {
            fallar(archivo, id + " referencia la billetera inexistente " + billetera);
        }
    }

    private static void fallar(String archivo, String detalle) {
        throw new PersistenciaException("Datos inválidos en " + archivo + ": " + detalle);
    }
}
