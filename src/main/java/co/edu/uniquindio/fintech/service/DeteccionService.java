package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.model.EstadoTransaccion;
import co.edu.uniquindio.fintech.model.EventoAuditoria;
import co.edu.uniquindio.fintech.model.NivelRiesgo;
import co.edu.uniquindio.fintech.model.PatronInusual;
import co.edu.uniquindio.fintech.model.TipoTransaccion;
import co.edu.uniquindio.fintech.model.Transaccion;
import co.edu.uniquindio.fintech.repository.FabricaEstructuras;
import co.edu.uniquindio.fintech.repository.RepositorioAuditoria;
import co.edu.uniquindio.fintech.repository.RepositorioTransacciones;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import co.edu.uniquindio.fintech.structures.interfaces.TablaHash;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Detección de comportamiento financiero inusual (enunciado 4.9), con los seis patrones de
 * {@link PatronInusual} y los umbrales de {@link PoliticaDeteccion} (provisionales).
 *
 * <p>Se analiza una transacción <b>antes</b> de registrarla, contra el historial del
 * usuario que la origina (sus operaciones exitosas o revertidas, sin reversiones). No
 * bloquea la operación: devuelve hallazgos con los que el servicio de transacciones marca
 * el nivel de riesgo, crea el evento de auditoría y la alerta de seguridad dentro de la
 * misma operación del diario.
 */
public final class DeteccionService {

    /** Patrón detectado en una transacción. */
    public record Hallazgo(PatronInusual patron, NivelRiesgo riesgo, String descripcion) {
    }

    private final PoliticaDeteccion politica;
    private final RepositorioTransacciones transacciones;
    private final RepositorioAuditoria auditoria;
    private final FabricaEstructuras fabrica;

    public DeteccionService(PoliticaDeteccion politica, RepositorioTransacciones transacciones,
                            RepositorioAuditoria auditoria, FabricaEstructuras fabrica) {
        this.politica = politica;
        this.transacciones = transacciones;
        this.auditoria = auditoria;
        this.fabrica = fabrica;
    }

    public PoliticaDeteccion politica() {
        return politica;
    }

    /**
     * @param candidata transacción aún no registrada
     * @param usuario   usuario que la origina
     * @return hallazgos (vacío si no hay nada inusual); nunca repite un patrón
     */
    public Lista<Hallazgo> analizar(Transaccion candidata, String usuario) {
        Lista<Transaccion> previas = historialPropio(usuario, candidata.fecha());
        Lista<Hallazgo> hallazgos = fabrica.nuevaLista();
        LocalDateTime fecha = candidata.fecha();
        boolean esTransferencia = candidata.tipo() == TipoTransaccion.TRANSFERENCIA;

        if (esTransferencia) {
            int seguidas = 1 + contar(previas, TipoTransaccion.TRANSFERENCIA, fecha.minus(politica.ventanaConsecutivas()),
                    null);
            if (seguidas >= politica.minimoConsecutivas()) {
                agregar(hallazgos, PatronInusual.TRANSFERENCIAS_CONSECUTIVAS_RAPIDAS, seguidas
                        + " transferencias en " + politica.ventanaConsecutivas().toMinutes() + " minutos");
            }
            int alMismoDestino = 1 + contar(previas, TipoTransaccion.TRANSFERENCIA,
                    fecha.minus(politica.ventanaDestino()), candidata.billeteraDestino());
            if (alMismoDestino >= politica.minimoDestino()) {
                agregar(hallazgos, PatronInusual.DESTINO_REPETITIVO, alMismoDestino + " envíos a "
                        + candidata.billeteraDestino() + " en " + politica.ventanaDestino().toMinutes() + " minutos");
            }
            TablaHash<String, Boolean> origenes = fabrica.nuevaTablaHash();
            origenes.poner(candidata.billeteraOrigen(), Boolean.TRUE);
            LocalDateTime desde = fecha.minus(politica.ventanaFragmentacion());
            for (Transaccion t : previas) {
                if (t.tipo() == TipoTransaccion.TRANSFERENCIA && t.fecha().isAfter(desde)
                        && candidata.billeteraDestino().equals(t.billeteraDestino())) {
                    origenes.poner(t.billeteraOrigen(), Boolean.TRUE);
                }
            }
            if (origenes.tamanio() >= politica.minimoBilleterasOrigen()) {
                agregar(hallazgos, PatronInusual.FRAGMENTACION_ENTRE_BILLETERAS, origenes.tamanio()
                        + " billeteras distintas enviando a " + candidata.billeteraDestino() + " en "
                        + politica.ventanaFragmentacion().toMinutes() + " minutos");
            }
        }

        if (candidata.tipo() == TipoTransaccion.RETIRO || esTransferencia) {
            int cantidad = 0;
            BigDecimal suma = BigDecimal.ZERO;
            for (Transaccion t : previas) {
                if (t.tipo() == TipoTransaccion.RETIRO || t.tipo() == TipoTransaccion.TRANSFERENCIA) {
                    cantidad++;
                    suma = suma.add(t.valor());
                }
            }
            if (cantidad >= politica.minimoHistorialMonto()) {
                BigDecimal promedio = suma.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);
                if (candidata.valor().compareTo(promedio.multiply(politica.factorMontoAlto())) > 0) {
                    agregar(hallazgos, PatronInusual.MONTO_INUSUALMENTE_ALTO, "Valor " + candidata.valor()
                            + " mayor que " + politica.factorMontoAlto() + " veces el promedio " + promedio);
                }
            }
        }

        int ultimoDia = 1 + contar(previas, null, fecha.minusHours(24), null);
        int base = contar(previas, null, fecha.minusDays(8), null) - (ultimoDia - 1);
        if (base > 0 && ultimoDia >= politica.minimoFrecuencia()
                && (long) ultimoDia * 7 >= (long) politica.factorFrecuencia() * base) {
            agregar(hallazgos, PatronInusual.CAMBIO_BRUSCO_DE_FRECUENCIA, ultimoDia + " transacciones en 24 horas frente a "
                    + base + " en los 7 días anteriores");
        }

        if (previas.tamanio() >= politica.minimoHistorialHorario()) {
            int hora = fecha.getHour();
            boolean habitual = false;
            for (Transaccion t : previas) {
                int diferencia = Math.abs(t.fecha().getHour() - hora);
                if (Math.min(diferencia, 24 - diferencia) <= politica.toleranciaHoras()) {
                    habitual = true;
                    break;
                }
            }
            if (!habitual) {
                agregar(hallazgos, PatronInusual.HORARIO_NO_HABITUAL, "Operación a las " + hora
                        + " h, fuera del horario habitual del usuario");
            }
        }
        return hallazgos;
    }

    /** @return eventos de auditoría (todos o de un nivel de riesgo), en orden de registro */
    public Lista<EventoAuditoria> eventos(NivelRiesgo riesgo) {
        return filtrar(auditoria.todos(), riesgo);
    }

    /** @return eventos de auditoría de un usuario (todos o de un nivel de riesgo) */
    public Lista<EventoAuditoria> eventosDe(String usuario, NivelRiesgo riesgo) {
        return filtrar(auditoria.porUsuario(usuario), riesgo);
    }

    private Lista<EventoAuditoria> filtrar(Lista<EventoAuditoria> origen, NivelRiesgo riesgo) {
        Lista<EventoAuditoria> resultado = fabrica.nuevaLista();
        for (EventoAuditoria e : origen) {
            if (riesgo == null || e.nivelRiesgo() == riesgo) {
                resultado.agregar(e);
            }
        }
        return resultado;
    }

    /** @return el mayor nivel de riesgo de los hallazgos (NINGUNO si no hay) */
    public static NivelRiesgo riesgoMaximo(Lista<Hallazgo> hallazgos) {
        NivelRiesgo maximo = NivelRiesgo.NINGUNO;
        for (Hallazgo h : hallazgos) {
            if (h.riesgo().ordinal() > maximo.ordinal()) {
                maximo = h.riesgo();
            }
        }
        return maximo;
    }

    /** @return si ya hay un evento de auditoría para esa transacción y patrón (evita duplicados) */
    public boolean yaRegistrado(String usuario, String transaccion, PatronInusual patron) {
        for (EventoAuditoria e : auditoria.porUsuario(usuario)) {
            if (transaccion.equals(e.transaccion()) && e.patron() == patron) {
                return true;
            }
        }
        return false;
    }

    private void agregar(Lista<Hallazgo> hallazgos, PatronInusual patron, String descripcion) {
        hallazgos.agregar(new Hallazgo(patron, politica.riesgo(patron), descripcion));
    }

    private Lista<Transaccion> historialPropio(String usuario, LocalDateTime hasta) {
        Lista<Transaccion> propias = fabrica.nuevaLista();
        for (Transaccion t : transacciones.historialDeUsuario(usuario)) {
            boolean efectiva = t.estado() == EstadoTransaccion.EXITOSA || t.estado() == EstadoTransaccion.REVERTIDA;
            if (efectiva && !t.esReversion() && !t.fecha().isAfter(hasta)
                    && usuario.equals(transacciones.usuarioQueOrigino(t))) {
                propias.agregar(t);
            }
        }
        return propias;
    }

    /** Cuenta previas posteriores a {@code desde}, opcionalmente de un tipo y hacia un destino. */
    private static int contar(Lista<Transaccion> previas, TipoTransaccion tipo, LocalDateTime desde, String destino) {
        int cantidad = 0;
        for (Transaccion t : previas) {
            if (t.fecha().isAfter(desde) && (tipo == null || t.tipo() == tipo)
                    && (destino == null || destino.equals(t.billeteraDestino()))) {
                cantidad++;
            }
        }
        return cantidad;
    }
}
