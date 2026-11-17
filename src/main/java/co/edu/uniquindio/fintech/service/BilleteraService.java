package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.dto.SolicitudActualizacionBilletera;
import co.edu.uniquindio.fintech.dto.SolicitudCreacionBilletera;
import co.edu.uniquindio.fintech.exception.ConflictoNegocioException;
import co.edu.uniquindio.fintech.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.fintech.model.Billetera;
import co.edu.uniquindio.fintech.model.EstadoBilletera;
import co.edu.uniquindio.fintech.persistence.CerrojoEscritura;
import co.edu.uniquindio.fintech.persistence.GeneradorIdentificadores;
import co.edu.uniquindio.fintech.persistence.TipoIdentificador;
import co.edu.uniquindio.fintech.repository.RepositorioBilleteras;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;

import java.math.BigDecimal;

/**
 * Billeteras (RF2). Se crean con saldo 0 y estado ACTIVA; el saldo solo cambia mediante
 * transacciones. No se eliminan: se desactivan, y solo si su saldo es 0 (si no, el dinero
 * quedaría inaccesible, porque una billetera inactiva no participa en operaciones).
 */
public final class BilleteraService {

    private final RepositorioBilleteras billeteras;
    private final UsuarioService usuarios;
    private final GeneradorIdentificadores generador;
    private final EscrituraConDiario escritura;
    private final CerrojoEscritura cerrojo;

    public BilleteraService(RepositorioBilleteras billeteras, UsuarioService usuarios,
                            GeneradorIdentificadores generador, EscrituraConDiario escritura) {
        this.billeteras = billeteras;
        this.usuarios = usuarios;
        this.generador = generador;
        this.escritura = escritura;
        this.cerrojo = escritura.cerrojo();
    }

    public Billetera crear(String propietario, SolicitudCreacionBilletera solicitud) {
        synchronized (cerrojo) {
            escritura.asegurarSinPendientes();
            usuarios.buscar(propietario);
            Billetera billetera = new Billetera(generador.siguiente(TipoIdentificador.BILLETERA), propietario,
                    solicitud.nombre().trim(), solicitud.tipo(), BigDecimal.ZERO, EstadoBilletera.ACTIVA);
            return billeteras.registrar(billetera);
        }
    }

    public Billetera buscar(String codigo) {
        Billetera billetera = billeteras.buscar(codigo);
        if (billetera == null) {
            throw new RecursoNoEncontradoException("BILLETERA_NO_ENCONTRADA", "No existe la billetera " + codigo);
        }
        return billetera;
    }

    public Lista<Billetera> porPropietario(String propietario) {
        usuarios.buscar(propietario);
        return billeteras.porPropietario(propietario);
    }

    public BigDecimal saldo(String codigo) {
        return buscar(codigo).saldo();
    }

    /** Suma de los saldos de las billeteras activas del usuario (las inactivas tienen saldo 0). */
    public BigDecimal saldoTotal(String propietario) {
        BigDecimal total = BigDecimal.ZERO;
        for (Billetera b : porPropietario(propietario)) {
            total = total.add(b.saldo());
        }
        return total;
    }

    /** Cambia nombre, tipo y estado (aplicando las reglas de activación). */
    public Billetera actualizar(String codigo, SolicitudActualizacionBilletera solicitud) {
        synchronized (cerrojo) {
            escritura.asegurarSinPendientes();
            Billetera actual = buscar(codigo);
            validarCambioDeEstado(actual, solicitud.estado());
            Billetera nueva = actual.conDatos(solicitud.nombre().trim(), solicitud.tipo()).conEstado(solicitud.estado());
            billeteras.actualizar(nueva);
            return nueva;
        }
    }

    public Billetera desactivar(String codigo) {
        return cambiarEstado(codigo, EstadoBilletera.INACTIVA);
    }

    public Billetera activar(String codigo) {
        return cambiarEstado(codigo, EstadoBilletera.ACTIVA);
    }

    private Billetera cambiarEstado(String codigo, EstadoBilletera estado) {
        synchronized (cerrojo) {
            escritura.asegurarSinPendientes();
            Billetera actual = buscar(codigo);
            validarCambioDeEstado(actual, estado);
            if (actual.estado() == estado) {
                return actual;
            }
            Billetera nueva = actual.conEstado(estado);
            billeteras.actualizar(nueva);
            return nueva;
        }
    }

    private static void validarCambioDeEstado(Billetera actual, EstadoBilletera nuevo) {
        if (nuevo == EstadoBilletera.INACTIVA && actual.estado() == EstadoBilletera.ACTIVA
                && actual.saldo().signum() > 0) {
            throw new ConflictoNegocioException("BILLETERA_CON_SALDO", "La billetera " + actual.codigo()
                    + " tiene saldo " + actual.saldo() + "; debe quedar en 0 antes de desactivarla");
        }
    }
}
