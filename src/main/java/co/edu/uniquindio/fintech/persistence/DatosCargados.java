package co.edu.uniquindio.fintech.persistence;

import co.edu.uniquindio.fintech.model.BeneficioCanjeado;
import co.edu.uniquindio.fintech.model.Billetera;
import co.edu.uniquindio.fintech.model.EventoAuditoria;
import co.edu.uniquindio.fintech.model.Notificacion;
import co.edu.uniquindio.fintech.model.OperacionProgramada;
import co.edu.uniquindio.fintech.model.Transaccion;
import co.edu.uniquindio.fintech.model.Usuario;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;

/** Contenido de los siete archivos de dominio leído al iniciar, en el orden de cada archivo. */
public record DatosCargados(Lista<Usuario> usuarios, Lista<Billetera> billeteras, Lista<Transaccion> transacciones,
                            Lista<OperacionProgramada> operacionesProgramadas, Lista<Notificacion> notificaciones,
                            Lista<EventoAuditoria> auditoria, Lista<BeneficioCanjeado> beneficios) {

    /** Lee los siete archivos. */
    public static DatosCargados cargar(AlmacenesJson almacenes) {
        return new DatosCargados(almacenes.usuarios().cargar(), almacenes.billeteras().cargar(),
                almacenes.transacciones().cargar(), almacenes.operacionesProgramadas().cargar(),
                almacenes.notificaciones().cargar(), almacenes.auditoria().cargar(), almacenes.beneficios().cargar());
    }
}
