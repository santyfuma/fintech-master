package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.dto.SolicitudOperacionProgramada;
import co.edu.uniquindio.fintech.exception.ConflictoNegocioException;
import co.edu.uniquindio.fintech.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.fintech.exception.SolicitudInvalidaException;
import co.edu.uniquindio.fintech.model.Billetera;
import co.edu.uniquindio.fintech.model.EstadoBilletera;
import co.edu.uniquindio.fintech.model.EstadoOperacionProgramada;
import co.edu.uniquindio.fintech.model.EstadoTransaccion;
import co.edu.uniquindio.fintech.model.OperacionProgramada;
import co.edu.uniquindio.fintech.model.Periodicidad;
import co.edu.uniquindio.fintech.model.TipoNotificacion;
import co.edu.uniquindio.fintech.model.TipoOperacionProgramada;
import co.edu.uniquindio.fintech.persistence.CerrojoEscritura;
import co.edu.uniquindio.fintech.persistence.GeneradorIdentificadores;
import co.edu.uniquindio.fintech.persistence.TipoIdentificador;
import co.edu.uniquindio.fintech.repository.FabricaEstructuras;
import co.edu.uniquindio.fintech.repository.RepositorioBilleteras;
import co.edu.uniquindio.fintech.repository.RepositorioOperacionesProgramadas;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Operaciones programadas (RF5, RF6; enunciado 4.3).
 *
 * <p>Se procesan en el orden de la cola de prioridad del repositorio (fecha de ejecución y,
 * si empatan, identificador). {@link #procesarVencidas()} es la única lógica de ejecución:
 * la usan el disparo manual (API) y la tarea periódica opcional, así no se duplica. Cada
 * ejecución pasa por {@link TransaccionService#ejecutarProgramada}: la transacción, el
 * nuevo estado de la operación y, si es periódica, la siguiente operación se escriben en
 * la misma entrada del diario.
 *
 * <p>Periódicas (<b>provisional</b>, ver {@link PoliticaProgramacion}): la ejecutada
 * queda EJECUTADA o FALLIDA y se crea una nueva PENDIENTE para la siguiente fecha.
 */
public final class OperacionProgramadaService {

    /** Resultado de ejecutar una operación programada. */
    public record ResultadoEjecucion(String operacion, String transaccion, EstadoOperacionProgramada estado,
                                     String motivo, String siguienteOperacion) {
    }

    private final RepositorioOperacionesProgramadas operaciones;
    private final RepositorioBilleteras billeteras;
    private final TransaccionService transacciones;
    private final NotificacionService notificaciones;
    private final GeneradorIdentificadores generador;
    private final PoliticaProgramacion politica;
    private final PoliticaNotificaciones politicaNotificaciones;
    private final FabricaEstructuras fabrica;
    private final EscrituraConDiario escritura;
    private final CerrojoEscritura cerrojo;
    private final Clock reloj;

    public OperacionProgramadaService(RepositorioOperacionesProgramadas operaciones, RepositorioBilleteras billeteras,
                                      TransaccionService transacciones, NotificacionService notificaciones,
                                      GeneradorIdentificadores generador, PoliticaProgramacion politica,
                                      PoliticaNotificaciones politicaNotificaciones, FabricaEstructuras fabrica,
                                      EscrituraConDiario escritura, Clock reloj) {
        this.operaciones = operaciones;
        this.billeteras = billeteras;
        this.transacciones = transacciones;
        this.notificaciones = notificaciones;
        this.generador = generador;
        this.politica = politica;
        this.politicaNotificaciones = politicaNotificaciones;
        this.fabrica = fabrica;
        this.escritura = escritura;
        this.cerrojo = escritura.cerrojo();
        this.reloj = reloj;
    }

    public OperacionProgramada programar(SolicitudOperacionProgramada s) {
        synchronized (cerrojo) {
            escritura.asegurarSinPendientes();
            // Defensa en el servicio: la capa HTTP ya valida esto con Bean Validation.
            if (s.tipo() == null || s.periodicidad() == null || s.fechaEjecucion() == null) {
                throw new SolicitudInvalidaException("SOLICITUD_INCOMPLETA",
                        "Tipo, periodicidad y fecha de ejecución son obligatorios");
            }
            if (s.valor() == null || s.valor().signum() <= 0) {
                throw new SolicitudInvalidaException("VALOR_INVALIDO", "El valor debe ser mayor que cero");
            }
            if (!s.isBilleterasCoherentesConElTipo()) {
                throw new SolicitudInvalidaException("BILLETERAS_INCOHERENTES", "Las billeteras no corresponden al tipo "
                        + s.tipo() + " (recarga: solo destino; retiro: solo origen; transferencia: dos distintas)");
            }
            if (s.tipo() == TipoOperacionProgramada.RETIRO && !politica.permitirRetiros()) {
                throw new SolicitudInvalidaException("RETIRO_PROGRAMADO_NO_PERMITIDO",
                        "La política vigente no permite programar retiros");
            }
            if (!s.fechaEjecucion().isAfter(LocalDateTime.now(reloj))) {
                throw new SolicitudInvalidaException("FECHA_EJECUCION_PASADA", "La fecha de ejecución debe ser futura");
            }
            for (String codigo : new String[]{s.billeteraOrigen(), s.billeteraDestino()}) {
                if (codigo != null) {
                    Billetera b = billeteras.buscar(codigo);
                    if (b == null) {
                        throw new RecursoNoEncontradoException("BILLETERA_NO_ENCONTRADA", "No existe la billetera " + codigo);
                    }
                    if (b.estado() != EstadoBilletera.ACTIVA) {
                        throw new ConflictoNegocioException("BILLETERA_INACTIVA", "La billetera " + codigo + " está inactiva");
                    }
                }
            }
            OperacionProgramada operacion = new OperacionProgramada(generador.siguiente(TipoIdentificador.OPERACION_PROGRAMADA),
                    s.tipo(), s.valor(), s.billeteraOrigen(), s.billeteraDestino(), s.fechaEjecucion(), s.periodicidad(),
                    EstadoOperacionProgramada.PENDIENTE);
            return operaciones.registrar(operacion);
        }
    }

    public OperacionProgramada buscar(String id) {
        OperacionProgramada o = operaciones.buscar(id);
        if (o == null) {
            throw new RecursoNoEncontradoException("OPERACION_NO_ENCONTRADA", "No existe la operación programada " + id);
        }
        return o;
    }

    public Lista<OperacionProgramada> porUsuario(String usuario) {
        return operaciones.porUsuario(usuario);
    }

    /** @return todas, o solo las del estado indicado (p. ej. EJECUTADA o FALLIDA) */
    public Lista<OperacionProgramada> listar(EstadoOperacionProgramada estado) {
        Lista<OperacionProgramada> resultado = fabrica.nuevaLista();
        for (OperacionProgramada o : operaciones.todos()) {
            if (estado == null || o.estado() == estado) {
                resultado.agregar(o);
            }
        }
        return resultado;
    }

    public OperacionProgramada siguientePendiente() {
        OperacionProgramada siguiente = operaciones.siguientePendiente();
        if (siguiente == null) {
            throw new RecursoNoEncontradoException("SIN_OPERACIONES_PENDIENTES", "No hay operaciones pendientes");
        }
        return siguiente;
    }

    public OperacionProgramada cancelar(String id) {
        synchronized (cerrojo) {
            escritura.asegurarSinPendientes();
            OperacionProgramada o = buscar(id);
            if (o.estado() != EstadoOperacionProgramada.PENDIENTE) {
                throw new ConflictoNegocioException("OPERACION_NO_CANCELABLE", "La operación " + id + " está "
                        + o.estado() + " y no puede cancelarse");
            }
            OperacionProgramada cancelada = o.conEstado(EstadoOperacionProgramada.CANCELADA);
            operaciones.actualizar(cancelada);
            return cancelada;
        }
    }

    /**
     * Ejecuta, en orden de prioridad, todas las operaciones pendientes cuya fecha ya llegó
     * según el {@link Clock}, y luego avisa las próximas a ejecutarse.
     */
    public Lista<ResultadoEjecucion> procesarVencidas() {
        synchronized (cerrojo) {
            escritura.asegurarSinPendientes();
            LocalDateTime ahora = LocalDateTime.now(reloj);
            Lista<ResultadoEjecucion> resultados = fabrica.nuevaLista();
            OperacionProgramada siguiente = operaciones.siguientePendiente();
            while (siguiente != null && !siguiente.fechaEjecucion().isAfter(ahora)) {
                resultados.agregar(ejecutar(siguiente));
                siguiente = operaciones.siguientePendiente();
            }
            avisarProximas(ahora);
            return resultados;
        }
    }

    private ResultadoEjecucion ejecutar(OperacionProgramada operacion) {
        String[] siguienteId = new String[1];
        TransaccionService.Resultado resultado = transacciones.ejecutarProgramada(operacion, transaccion -> {
            EstadoOperacionProgramada estado = transaccion.estado() == EstadoTransaccion.EXITOSA
                    ? EstadoOperacionProgramada.EJECUTADA : EstadoOperacionProgramada.FALLIDA;
            OperacionProgramada terminada = operacion.conEstado(estado);
            if (operacion.periodicidad() == Periodicidad.UNICA) {
                return new Object[]{terminada};
            }
            OperacionProgramada proxima = new OperacionProgramada(generador.siguiente(TipoIdentificador.OPERACION_PROGRAMADA),
                    operacion.tipo(), operacion.valor(), operacion.billeteraOrigen(), operacion.billeteraDestino(),
                    siguienteFecha(operacion.fechaEjecucion(), operacion.periodicidad()), operacion.periodicidad(),
                    EstadoOperacionProgramada.PENDIENTE);
            siguienteId[0] = proxima.id();
            return new Object[]{terminada, proxima};
        });
        EstadoOperacionProgramada estado = resultado.rechazada() ? EstadoOperacionProgramada.FALLIDA
                : EstadoOperacionProgramada.EJECUTADA;
        return new ResultadoEjecucion(operacion.id(), resultado.transaccion().id(), estado, resultado.motivoRechazo(),
                siguienteId[0]);
    }

    /** Notifica (una sola vez) las operaciones pendientes que vencen dentro de la anticipación configurada. */
    private void avisarProximas(LocalDateTime ahora) {
        LocalDateTime limite = ahora.plus(politicaNotificaciones.anticipacionProgramadas());
        for (OperacionProgramada o : operaciones.todos()) {
            if (o.estado() == EstadoOperacionProgramada.PENDIENTE && o.fechaEjecucion().isAfter(ahora)
                    && !o.fechaEjecucion().isAfter(limite)) {
                String usuario = billeteras.propietarioDe(o.billeteraOrigen() != null ? o.billeteraOrigen()
                        : o.billeteraDestino());
                if (!notificaciones.existeAviso(usuario, TipoNotificacion.OPERACION_PROGRAMADA_PROXIMA, "La operación " + o.id() + " ")) {
                    notificaciones.crear(usuario, TipoNotificacion.OPERACION_PROGRAMADA_PROXIMA, "La operación "
                            + o.id() + " se ejecutará el " + o.fechaEjecucion());
                }
            }
        }
    }

    static LocalDateTime siguienteFecha(LocalDateTime fecha, Periodicidad periodicidad) {
        return switch (periodicidad) {
            case SEMANAL -> fecha.plusWeeks(1);
            case MENSUAL -> fecha.plusMonths(1);
            case UNICA -> fecha;
        };
    }
}
