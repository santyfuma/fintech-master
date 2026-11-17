package co.edu.uniquindio.fintech.config;

import co.edu.uniquindio.fintech.persistence.GeneradorIdentificadores;
import co.edu.uniquindio.fintech.repository.RepositorioAuditoria;
import co.edu.uniquindio.fintech.repository.RepositorioBeneficios;
import co.edu.uniquindio.fintech.repository.RepositorioBilleteras;
import co.edu.uniquindio.fintech.repository.RepositorioNotificaciones;
import co.edu.uniquindio.fintech.repository.RepositorioOperacionesProgramadas;
import co.edu.uniquindio.fintech.repository.RepositorioTransacciones;
import co.edu.uniquindio.fintech.repository.RepositorioUsuarios;

/**
 * Resultado del arranque de datos: repositorios ya cargados e indexados y el generador
 * con sus consecutivos recuperados. Solo {@link InicializadorDatos} lo crea, así que no
 * existe un repositorio sin inicializar.
 */
public final class ContextoDatos {

    private final RepositorioUsuarios usuarios;
    private final RepositorioBilleteras billeteras;
    private final RepositorioTransacciones transacciones;
    private final RepositorioOperacionesProgramadas operacionesProgramadas;
    private final RepositorioNotificaciones notificaciones;
    private final RepositorioAuditoria auditoria;
    private final RepositorioBeneficios beneficios;
    private final GeneradorIdentificadores generador;
    private final int operacionesRecuperadas;

    ContextoDatos(RepositorioUsuarios usuarios, RepositorioBilleteras billeteras,
                  RepositorioTransacciones transacciones, RepositorioOperacionesProgramadas operacionesProgramadas,
                  RepositorioNotificaciones notificaciones, RepositorioAuditoria auditoria,
                  RepositorioBeneficios beneficios, GeneradorIdentificadores generador, int operacionesRecuperadas) {
        this.usuarios = usuarios;
        this.billeteras = billeteras;
        this.transacciones = transacciones;
        this.operacionesProgramadas = operacionesProgramadas;
        this.notificaciones = notificaciones;
        this.auditoria = auditoria;
        this.beneficios = beneficios;
        this.generador = generador;
        this.operacionesRecuperadas = operacionesRecuperadas;
    }

    public RepositorioUsuarios usuarios() {
        return usuarios;
    }

    public RepositorioBilleteras billeteras() {
        return billeteras;
    }

    public RepositorioTransacciones transacciones() {
        return transacciones;
    }

    public RepositorioOperacionesProgramadas operacionesProgramadas() {
        return operacionesProgramadas;
    }

    public RepositorioNotificaciones notificaciones() {
        return notificaciones;
    }

    public RepositorioAuditoria auditoria() {
        return auditoria;
    }

    public RepositorioBeneficios beneficios() {
        return beneficios;
    }

    public GeneradorIdentificadores generador() {
        return generador;
    }

    /** @return entradas del diario rehechas en este arranque */
    public int operacionesRecuperadas() {
        return operacionesRecuperadas;
    }
}
