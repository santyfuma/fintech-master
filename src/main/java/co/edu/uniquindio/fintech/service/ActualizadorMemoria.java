package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.model.BeneficioCanjeado;
import co.edu.uniquindio.fintech.model.Billetera;
import co.edu.uniquindio.fintech.model.EventoAuditoria;
import co.edu.uniquindio.fintech.model.Notificacion;
import co.edu.uniquindio.fintech.model.OperacionProgramada;
import co.edu.uniquindio.fintech.model.Transaccion;
import co.edu.uniquindio.fintech.model.Usuario;
import co.edu.uniquindio.fintech.persistence.ArchivoDominio;
import co.edu.uniquindio.fintech.repository.RepositorioAuditoria;
import co.edu.uniquindio.fintech.repository.RepositorioBeneficios;
import co.edu.uniquindio.fintech.repository.RepositorioBilleteras;
import co.edu.uniquindio.fintech.repository.RepositorioNotificaciones;
import co.edu.uniquindio.fintech.repository.RepositorioOperacionesProgramadas;
import co.edu.uniquindio.fintech.repository.RepositorioTransacciones;
import co.edu.uniquindio.fintech.repository.RepositorioUsuarios;

/**
 * Lleva a la memoria (con {@code reflejar}) los registros de una operación que el diario
 * ya escribió en los archivos. Los procesa en el orden de {@link ArchivoDominio}
 * (usuarios, billeteras, transacciones, …) para que las referencias existan antes de
 * indexar lo que depende de ellas.
 */
public final class ActualizadorMemoria {

    private final RepositorioUsuarios usuarios;
    private final RepositorioBilleteras billeteras;
    private final RepositorioTransacciones transacciones;
    private final RepositorioOperacionesProgramadas operaciones;
    private final RepositorioNotificaciones notificaciones;
    private final RepositorioAuditoria auditoria;
    private final RepositorioBeneficios beneficios;

    public ActualizadorMemoria(RepositorioUsuarios usuarios, RepositorioBilleteras billeteras,
                               RepositorioTransacciones transacciones, RepositorioOperacionesProgramadas operaciones,
                               RepositorioNotificaciones notificaciones, RepositorioAuditoria auditoria,
                               RepositorioBeneficios beneficios) {
        this.usuarios = usuarios;
        this.billeteras = billeteras;
        this.transacciones = transacciones;
        this.operaciones = operaciones;
        this.notificaciones = notificaciones;
        this.auditoria = auditoria;
        this.beneficios = beneficios;
    }

    public void reflejar(Object[] registros) {
        for (ArchivoDominio archivo : ArchivoDominio.values()) {
            for (Object registro : registros) {
                if (archivo.tipo().isInstance(registro)) {
                    reflejarUno(registro);
                }
            }
        }
    }

    private void reflejarUno(Object registro) {
        if (registro instanceof Usuario u) {
            usuarios.reflejar(u);
        } else if (registro instanceof Billetera b) {
            billeteras.reflejar(b);
        } else if (registro instanceof Transaccion t) {
            transacciones.reflejar(t);
        } else if (registro instanceof OperacionProgramada o) {
            operaciones.reflejar(o);
        } else if (registro instanceof Notificacion n) {
            notificaciones.reflejar(n);
        } else if (registro instanceof EventoAuditoria e) {
            auditoria.reflejar(e);
        } else if (registro instanceof BeneficioCanjeado c) {
            beneficios.reflejar(c);
        }
    }
}
