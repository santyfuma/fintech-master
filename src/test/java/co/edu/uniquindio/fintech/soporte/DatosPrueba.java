package co.edu.uniquindio.fintech.soporte;

import co.edu.uniquindio.fintech.model.BeneficioCanjeado;
import co.edu.uniquindio.fintech.model.Billetera;
import co.edu.uniquindio.fintech.model.EstadoBilletera;
import co.edu.uniquindio.fintech.model.EstadoCanje;
import co.edu.uniquindio.fintech.model.EstadoNotificacion;
import co.edu.uniquindio.fintech.model.EstadoOperacionProgramada;
import co.edu.uniquindio.fintech.model.EstadoTransaccion;
import co.edu.uniquindio.fintech.model.EventoAuditoria;
import co.edu.uniquindio.fintech.model.NivelRiesgo;
import co.edu.uniquindio.fintech.model.NivelUsuario;
import co.edu.uniquindio.fintech.model.Notificacion;
import co.edu.uniquindio.fintech.model.OperacionProgramada;
import co.edu.uniquindio.fintech.model.PatronInusual;
import co.edu.uniquindio.fintech.model.Periodicidad;
import co.edu.uniquindio.fintech.model.TipoBilletera;
import co.edu.uniquindio.fintech.model.TipoNotificacion;
import co.edu.uniquindio.fintech.model.TipoOperacionProgramada;
import co.edu.uniquindio.fintech.model.TipoTransaccion;
import co.edu.uniquindio.fintech.model.Transaccion;
import co.edu.uniquindio.fintech.model.Usuario;
import co.edu.uniquindio.fintech.persistence.ArchivoDominio;
import co.edu.uniquindio.fintech.persistence.GeneradorIdentificadores;
import co.edu.uniquindio.fintech.persistence.TipoIdentificador;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import co.edu.uniquindio.fintech.structures.propias.ListaEnlazada;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Registros de prueba. Solo existen en los tests: la aplicación no crea datos de ejemplo. */
public final class DatosPrueba {

    public static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 15, 18, 0);
    /** 2026-11-15T23:00Z = 2026-11-15T18:00 en Bogotá. */
    public static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-15T23:00:00Z"), ZoneId.of("America/Bogota"));

    private DatosPrueba() {
    }

    public static String bil(int n) {
        return GeneradorIdentificadores.formatear(TipoIdentificador.BILLETERA, n);
    }

    public static String trx(int n) {
        return GeneradorIdentificadores.formatear(TipoIdentificador.TRANSACCION, n);
    }

    public static String opr(int n) {
        return GeneradorIdentificadores.formatear(TipoIdentificador.OPERACION_PROGRAMADA, n);
    }

    public static String not(int n) {
        return GeneradorIdentificadores.formatear(TipoIdentificador.NOTIFICACION, n);
    }

    public static String aud(int n) {
        return GeneradorIdentificadores.formatear(TipoIdentificador.EVENTO_AUDITORIA, n);
    }

    public static String can(int n) {
        return GeneradorIdentificadores.formatear(TipoIdentificador.BENEFICIO_CANJEADO, n);
    }

    public static Usuario usuario(String identificacion, long puntos) {
        return new Usuario(identificacion, "Usuario " + identificacion, identificacion + "@correo.co", puntos,
                NivelUsuario.BRONCE);
    }

    public static Billetera billetera(int n, String propietario) {
        return new Billetera(bil(n), propietario, "Billetera " + n, TipoBilletera.AHORRO, new BigDecimal("1000.00"),
                EstadoBilletera.ACTIVA);
    }

    public static Transaccion recarga(int n, int destino, String valor) {
        return transaccion(n, TipoTransaccion.RECARGA, null, bil(destino), valor, null);
    }

    public static Transaccion retiro(int n, int origen, String valor) {
        return transaccion(n, TipoTransaccion.RETIRO, bil(origen), null, valor, null);
    }

    public static Transaccion transferencia(int n, int origen, int destino, String valor) {
        return transaccion(n, TipoTransaccion.TRANSFERENCIA, bil(origen), bil(destino), valor, null);
    }

    public static Transaccion reversion(int n, int revertida, Integer origen, Integer destino, String valor) {
        return transaccion(n, TipoTransaccion.REVERSION, origen == null ? null : bil(origen),
                destino == null ? null : bil(destino), valor, trx(revertida));
    }

    private static Transaccion transaccion(int n, TipoTransaccion tipo, String origen, String destino, String valor,
                                           String revertida) {
        return new Transaccion(trx(n), FECHA.plusMinutes(n), tipo, new BigDecimal(valor), origen, destino,
                EstadoTransaccion.EXITOSA, 0, NivelRiesgo.NINGUNO, revertida);
    }

    public static OperacionProgramada transferenciaProgramada(int n, int origen, int destino, LocalDateTime fecha) {
        return new OperacionProgramada(opr(n), TipoOperacionProgramada.TRANSFERENCIA, new BigDecimal("10.00"),
                bil(origen), bil(destino), fecha, Periodicidad.SEMANAL, EstadoOperacionProgramada.PENDIENTE);
    }

    public static OperacionProgramada recargaProgramada(int n, int destino, LocalDateTime fecha) {
        return new OperacionProgramada(opr(n), TipoOperacionProgramada.RECARGA, new BigDecimal("10.00"), null,
                bil(destino), fecha, Periodicidad.UNICA, EstadoOperacionProgramada.PENDIENTE);
    }

    public static Notificacion notificacion(int n, String usuario) {
        return new Notificacion(not(n), usuario, TipoNotificacion.SALDO_BAJO, "Mensaje " + n, FECHA.plusMinutes(n),
                EstadoNotificacion.PENDIENTE);
    }

    public static EventoAuditoria evento(int n, String usuario, String transaccion) {
        return new EventoAuditoria(aud(n), usuario, transaccion, PatronInusual.MONTO_INUSUALMENTE_ALTO,
                "Evento " + n, NivelRiesgo.ALTO, FECHA.plusMinutes(n));
    }

    public static BeneficioCanjeado canje(int n, String usuario) {
        return new BeneficioCanjeado(can(n), usuario, "Beneficio " + n, 100, FECHA.plusMinutes(n), EstadoCanje.REALIZADO);
    }

    @SafeVarargs
    public static <T> Lista<T> lista(T... elementos) {
        Lista<T> lista = new ListaEnlazada<>();
        for (T e : elementos) {
            lista.agregar(e);
        }
        return lista;
    }

    /** @return identificadores de los registros de dominio, p. ej. "[TRX-000001, TRX-000002]" */
    public static String ids(Iterable<?> registros) {
        StringBuilder sb = new StringBuilder("[");
        for (Object r : registros) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(ArchivoDominio.paraTipo(r.getClass()).identificador(r));
        }
        return sb.append(']').toString();
    }
}
