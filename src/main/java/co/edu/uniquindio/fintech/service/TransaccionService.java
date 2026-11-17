package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.exception.ConflictoNegocioException;
import co.edu.uniquindio.fintech.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.fintech.exception.SolicitudInvalidaException;
import co.edu.uniquindio.fintech.model.Billetera;
import co.edu.uniquindio.fintech.model.EstadoBilletera;
import co.edu.uniquindio.fintech.model.EstadoTransaccion;
import co.edu.uniquindio.fintech.model.EventoAuditoria;
import co.edu.uniquindio.fintech.model.NivelRiesgo;
import co.edu.uniquindio.fintech.model.OperacionProgramada;
import co.edu.uniquindio.fintech.model.TipoNotificacion;
import co.edu.uniquindio.fintech.model.TipoTransaccion;
import co.edu.uniquindio.fintech.model.Transaccion;
import co.edu.uniquindio.fintech.model.Usuario;
import co.edu.uniquindio.fintech.persistence.CerrojoEscritura;
import co.edu.uniquindio.fintech.persistence.GeneradorIdentificadores;
import co.edu.uniquindio.fintech.persistence.TipoIdentificador;
import co.edu.uniquindio.fintech.repository.FabricaEstructuras;
import co.edu.uniquindio.fintech.repository.RepositorioBilleteras;
import co.edu.uniquindio.fintech.repository.RepositorioTransacciones;
import co.edu.uniquindio.fintech.repository.RepositorioUsuarios;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import co.edu.uniquindio.fintech.structures.interfaces.Pila;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Recargas, retiros, transferencias y reversiones (RF3, RF4, RF9; enunciado 4.2 y 4.6).
 *
 * <p><b>Integridad:</b> cada operación se calcula y se escribe bajo el cerrojo de
 * escritura, y todos los registros que cambia (billeteras, transacción, usuario con sus
 * puntos, notificaciones, eventos de auditoría y, si viene de una programación, la
 * operación programada) van en <b>una sola entrada del diario</b>
 * ({@link EscrituraConDiario#ejecutar}). Si la escritura falla antes de quedar en el diario,
 * nada cambia; si falla después, la operación ya está comprometida: la memoria la refleja
 * y los archivos se completan antes de la siguiente modificación o en el arranque. Nunca
 * queda aplicada a medias.
 *
 * <p><b>Rechazos:</b> los errores de la solicitud (valor no positivo, billetera
 * inexistente, misma billetera) se responden sin registrar nada. Los rechazos de negocio
 * sobre billeteras existentes (billetera inactiva, saldo insuficiente) quedan
 * <b>registrados</b> como transacción RECHAZADA con una notificación
 * OPERACION_RECHAZADA (enunciado 4.2 y 4.7) y luego se responde 409.
 */
public final class TransaccionService {

    /** Registros extra que se agregan a la misma entrada del diario según el resultado. */
    @FunctionalInterface
    public interface RegistrosAdicionales {
        Object[] para(Transaccion resultado);
    }

    /** Resultado de un movimiento: la transacción registrada y, si fue rechazada, el motivo. */
    public record Resultado(Transaccion transaccion, String codigoRechazo, String motivoRechazo) {
        public boolean rechazada() {
            return codigoRechazo != null;
        }
    }

    private static final Object[] SIN_ADICIONALES = new Object[0];

    private final RepositorioUsuarios usuarios;
    private final RepositorioBilleteras billeteras;
    private final RepositorioTransacciones transacciones;
    private final GeneradorIdentificadores generador;
    private final PuntosService puntos;
    private final DeteccionService deteccion;
    private final NotificacionService notificaciones;
    private final PoliticaReversion politicaReversion;
    private final PoliticaNotificaciones politicaNotificaciones;
    private final FabricaEstructuras fabrica;
    private final EscrituraConDiario escritura;
    private final CerrojoEscritura cerrojo;
    private final Clock reloj;

    public TransaccionService(RepositorioUsuarios usuarios, RepositorioBilleteras billeteras,
                              RepositorioTransacciones transacciones,
                              GeneradorIdentificadores generador, PuntosService puntos,
                              DeteccionService deteccion, NotificacionService notificaciones,
                              PoliticaReversion politicaReversion, PoliticaNotificaciones politicaNotificaciones,
                              FabricaEstructuras fabrica, EscrituraConDiario escritura, Clock reloj) {
        this.usuarios = usuarios;
        this.billeteras = billeteras;
        this.transacciones = transacciones;
        this.generador = generador;
        this.puntos = puntos;
        this.deteccion = deteccion;
        this.notificaciones = notificaciones;
        this.politicaReversion = politicaReversion;
        this.politicaNotificaciones = politicaNotificaciones;
        this.fabrica = fabrica;
        this.escritura = escritura;
        this.cerrojo = escritura.cerrojo();
        this.reloj = reloj;
    }

    // ---------------------------------------------------------------- operaciones

    public Transaccion recargar(String billeteraDestino, BigDecimal valor) {
        return exigirExito(mover(TipoTransaccion.RECARGA, null, billeteraDestino, valor, false, null));
    }

    public Transaccion retirar(String billeteraOrigen, BigDecimal valor) {
        return exigirExito(mover(TipoTransaccion.RETIRO, billeteraOrigen, null, valor, false, null));
    }

    public Transaccion transferir(String billeteraOrigen, String billeteraDestino, BigDecimal valor) {
        return exigirExito(mover(TipoTransaccion.TRANSFERENCIA, billeteraOrigen, billeteraDestino, valor, false, null));
    }

    /**
     * Ejecuta una operación programada vencida como un movimiento con bono de puntos. No
     * lanza excepción por rechazo de negocio: devuelve el resultado, y los registros que
     * entregue {@code adicionales} (p. ej. la operación con su nuevo estado) se escriben en
     * la misma entrada del diario.
     */
    public Resultado ejecutarProgramada(OperacionProgramada operacion, RegistrosAdicionales adicionales) {
        return mover(operacion.tipo().tipoTransaccion(), operacion.billeteraOrigen(), operacion.billeteraDestino(),
                operacion.valor(), true, adicionales);
    }

    /**
     * Revierte una transacción (enunciado 4.6): crea una REVERSION que la referencia, deja
     * la original REVERTIDA, devuelve los saldos y descuenta los puntos que generó (sin
     * dejar puntos negativos). Todo en una sola entrada del diario.
     */
    public Transaccion revertir(String idTransaccion) {
        synchronized (cerrojo) {
            escritura.asegurarSinPendientes();
            Transaccion original = buscar(idTransaccion);
            if (original.esReversion()) {
                throw new ConflictoNegocioException("OPERACION_NO_REVERSIBLE", "Una reversión no se puede revertir");
            }
            if (original.estado() == EstadoTransaccion.REVERTIDA) {
                throw new ConflictoNegocioException("TRANSACCION_YA_REVERTIDA", "La transacción " + idTransaccion
                        + " ya fue revertida");
            }
            if (original.estado() != EstadoTransaccion.EXITOSA) {
                throw new ConflictoNegocioException("OPERACION_NO_REVERSIBLE", "Solo se revierten transacciones exitosas");
            }
            if (!politicaReversion.tipoPermitido(original.tipo())) {
                throw new ConflictoNegocioException("OPERACION_NO_REVERSIBLE", "Las transacciones de tipo "
                        + original.tipo() + " no son reversibles según la política vigente");
            }
            if (!politicaReversion.dentroDelPlazo(original)) {
                throw new ConflictoNegocioException("PLAZO_DE_REVERSION_VENCIDO", "La transacción " + idTransaccion
                        + " superó el plazo de reversión de " + politicaReversion.plazo().toHours() + " horas");
            }
            // El dinero vuelve: sale de la billetera que lo recibió y entra a la que lo entregó.
            String debitar = original.billeteraDestino();
            String acreditar = original.billeteraOrigen();
            Billetera debito = debitar == null ? null : billeteraExistente(debitar);
            Billetera credito = acreditar == null ? null : billeteraExistente(acreditar);
            for (Billetera b : new Billetera[]{debito, credito}) {
                if (b != null && b.estado() != EstadoBilletera.ACTIVA) {
                    throw new ConflictoNegocioException("BILLETERA_INACTIVA", "La billetera " + b.codigo()
                            + " está inactiva y no permite la reversión");
                }
            }
            if (debito != null && debito.saldo().compareTo(original.valor()) < 0) {
                throw new ConflictoNegocioException("SALDO_INSUFICIENTE", "La billetera " + debito.codigo()
                        + " ya no tiene saldo suficiente para devolver " + original.valor());
            }

            Lista<Object> registros = fabrica.nuevaLista();
            Billetera debitoFinal = null;
            if (debito != null) {
                debitoFinal = debito.conSaldo(debito.saldo().subtract(original.valor()));
                registros.agregar(debitoFinal);
            }
            if (credito != null) {
                registros.agregar(credito.conSaldo(credito.saldo().add(original.valor())));
            }
            String origino = transacciones.usuarioQueOrigino(original);
            Usuario antes = usuarios.buscar(origino);
            long delta = 0;
            if (antes != null) {
                Usuario despues = puntos.ajustar(antes, -original.puntosGenerados());
                delta = despues.puntosAcumulados() - antes.puntosAcumulados();
                registros.agregar(despues);
            }
            String id = generador.siguiente(TipoIdentificador.TRANSACCION);
            registros.agregar(original.conEstado(EstadoTransaccion.REVERTIDA));
            Transaccion reversion = new Transaccion(id, LocalDateTime.now(reloj), TipoTransaccion.REVERSION,
                    original.valor(), debitar, acreditar, EstadoTransaccion.EXITOSA, delta, NivelRiesgo.NINGUNO,
                    original.id());
            registros.agregar(reversion);
            if (debitoFinal != null) {
                avisoSaldoBajo(debitoFinal, registros);
            }
            ejecutarEnDiario(id, registros);
            return reversion;
        }
    }

    /** Revierte la última operación reversible del usuario (cima de su pila, CONTEXTO.md 7.5). */
    public Transaccion revertirUltima(String identificacionUsuario) {
        synchronized (cerrojo) {
            escritura.asegurarSinPendientes();
            if (!usuarios.existe(identificacionUsuario)) {
                throw new RecursoNoEncontradoException("USUARIO_NO_ENCONTRADO", "No existe el usuario "
                        + identificacionUsuario);
            }
            Pila<Transaccion> pila = transacciones.reconstruirPilaReversibles(identificacionUsuario, politicaReversion);
            if (pila.estaVacia()) {
                throw new ConflictoNegocioException("SIN_OPERACIONES_REVERSIBLES", "El usuario "
                        + identificacionUsuario + " no tiene operaciones reversibles");
            }
            return revertir(pila.cima().id());
        }
    }

    // ---------------------------------------------------------------- consultas

    public Transaccion buscar(String id) {
        Transaccion t = transacciones.buscar(id);
        if (t == null) {
            throw new RecursoNoEncontradoException("TRANSACCION_NO_ENCONTRADA", "No existe la transacción " + id);
        }
        return t;
    }

    public Lista<Transaccion> historialDeUsuario(String identificacion) {
        if (!usuarios.existe(identificacion)) {
            throw new RecursoNoEncontradoException("USUARIO_NO_ENCONTRADO", "No existe el usuario " + identificacion);
        }
        return transacciones.historialDeUsuario(identificacion);
    }

    public Lista<Transaccion> historialDeBilletera(String codigo) {
        billeteraExistente(codigo);
        return transacciones.historialDeBilletera(codigo);
    }

    public Lista<Transaccion> todas() {
        return transacciones.todos();
    }

    // ---------------------------------------------------------------- núcleo

    private Resultado mover(TipoTransaccion tipo, String origen, String destino, BigDecimal valor, boolean programada,
                            RegistrosAdicionales adicionales) {
        if (valor == null || valor.signum() <= 0) {
            throw new SolicitudInvalidaException("VALOR_INVALIDO", "El valor debe ser mayor que cero");
        }
        if (origen != null && origen.equals(destino)) {
            throw new SolicitudInvalidaException("MISMA_BILLETERA", "La billetera origen y la destino deben ser distintas");
        }
        if (!tipo.billeterasCoherentes(origen, destino)) {
            throw new SolicitudInvalidaException("BILLETERA_REQUERIDA", "Falta la billetera que exige la operación "
                    + tipo);
        }
        synchronized (cerrojo) {
            escritura.asegurarSinPendientes();
            Billetera billeteraOrigen = origen == null ? null : billeteraExistente(origen);
            Billetera billeteraDestino = destino == null ? null : billeteraExistente(destino);
            String origino = billeteraOrigen != null ? billeteraOrigen.identificacionPropietario()
                    : billeteraDestino.identificacionPropietario();
            String id = generador.siguiente(TipoIdentificador.TRANSACCION);
            LocalDateTime ahora = LocalDateTime.now(reloj);

            String codigoRechazo = null;
            String motivo = null;
            for (Billetera b : new Billetera[]{billeteraOrigen, billeteraDestino}) {
                if (codigoRechazo == null && b != null && b.estado() != EstadoBilletera.ACTIVA) {
                    codigoRechazo = "BILLETERA_INACTIVA";
                    motivo = "La billetera " + b.codigo() + " está inactiva";
                }
            }
            if (codigoRechazo == null && billeteraOrigen != null && billeteraOrigen.saldo().compareTo(valor) < 0) {
                codigoRechazo = "SALDO_INSUFICIENTE";
                motivo = "Saldo insuficiente en " + billeteraOrigen.codigo() + " para " + tipo.name().toLowerCase()
                        + " de " + valor;
            }

            Lista<Object> registros = fabrica.nuevaLista();
            Transaccion transaccion;
            if (codigoRechazo != null) {
                transaccion = new Transaccion(id, ahora, tipo, valor, origen, destino, EstadoTransaccion.RECHAZADA, 0,
                        NivelRiesgo.NINGUNO, null);
                registros.agregar(transaccion);
                registros.agregar(notificaciones.nueva(origino, TipoNotificacion.OPERACION_RECHAZADA,
                        "Operación " + id + " rechazada: " + motivo));
            } else {
                Transaccion candidata = new Transaccion(id, ahora, tipo, valor, origen, destino,
                        EstadoTransaccion.EXITOSA, 0, NivelRiesgo.NINGUNO, null);
                Lista<DeteccionService.Hallazgo> hallazgos = deteccion.analizar(candidata, origino);
                long ganados = puntos.calcular(tipo, valor, programada);
                transaccion = new Transaccion(id, ahora, tipo, valor, origen, destino, EstadoTransaccion.EXITOSA,
                        ganados, DeteccionService.riesgoMaximo(hallazgos), null);

                Billetera origenFinal = null;
                if (billeteraOrigen != null) {
                    origenFinal = billeteraOrigen.conSaldo(billeteraOrigen.saldo().subtract(valor));
                    registros.agregar(origenFinal);
                }
                if (billeteraDestino != null) {
                    registros.agregar(billeteraDestino.conSaldo(billeteraDestino.saldo().add(valor)));
                }
                registros.agregar(transaccion);
                Usuario antes = usuarios.buscar(origino);
                if (antes != null) {
                    Usuario despues = puntos.ajustar(antes, ganados);
                    registros.agregar(despues);
                    if (puntos.asciende(antes, despues)) {
                        registros.agregar(notificaciones.nueva(origino, TipoNotificacion.ASCENSO_NIVEL,
                                "Subiste al nivel " + despues.nivel() + " con " + despues.puntosAcumulados() + " puntos"));
                    }
                }
                for (DeteccionService.Hallazgo h : hallazgos) {
                    if (!deteccion.yaRegistrado(origino, id, h.patron())) {
                        registros.agregar(new EventoAuditoria(generador.siguiente(TipoIdentificador.EVENTO_AUDITORIA),
                                origino, id, h.patron(), h.descripcion(), h.riesgo(), ahora));
                        registros.agregar(notificaciones.nueva(origino, TipoNotificacion.ALERTA_SEGURIDAD,
                                "Alerta de seguridad en " + id + ": " + h.descripcion()));
                    }
                }
                if (origenFinal != null) {
                    avisoSaldoBajo(origenFinal, registros);
                }
            }
            Object[] extra = adicionales == null ? SIN_ADICIONALES : adicionales.para(transaccion);
            for (Object registro : extra) {
                registros.agregar(registro);
            }
            ejecutarEnDiario(id, registros);
            return new Resultado(transaccion, codigoRechazo, motivo);
        }
    }

    private void avisoSaldoBajo(Billetera billetera, Lista<Object> registros) {
        if (billetera.saldo().compareTo(politicaNotificaciones.umbralSaldoBajo()) < 0) {
            registros.agregar(notificaciones.nueva(billetera.identificacionPropietario(), TipoNotificacion.SALDO_BAJO,
                    "La billetera " + billetera.codigo() + " quedó con saldo bajo: " + billetera.saldo()));
        }
    }

    private void ejecutarEnDiario(String idOperacion, Lista<Object> registros) {
        Object[] arreglo = new Object[registros.tamanio()];
        int i = 0;
        for (Object registro : registros) {
            arreglo[i++] = registro;
        }
        escritura.ejecutar(idOperacion, arreglo);
    }

    private Billetera billeteraExistente(String codigo) {
        Billetera b = billeteras.buscar(codigo);
        if (b == null) {
            throw new RecursoNoEncontradoException("BILLETERA_NO_ENCONTRADA", "No existe la billetera " + codigo);
        }
        return b;
    }

    private static Transaccion exigirExito(Resultado resultado) {
        if (resultado.rechazada()) {
            throw new ConflictoNegocioException(resultado.codigoRechazo(), resultado.motivoRechazo()
                    + " (registrada como " + resultado.transaccion().id() + " RECHAZADA)");
        }
        return resultado.transaccion();
    }
}
