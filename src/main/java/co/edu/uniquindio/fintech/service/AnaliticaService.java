package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.fintech.exception.SolicitudInvalidaException;
import co.edu.uniquindio.fintech.model.Billetera;
import co.edu.uniquindio.fintech.model.EstadoTransaccion;
import co.edu.uniquindio.fintech.model.TipoTransaccion;
import co.edu.uniquindio.fintech.model.Transaccion;
import co.edu.uniquindio.fintech.repository.FabricaEstructuras;
import co.edu.uniquindio.fintech.repository.RepositorioBilleteras;
import co.edu.uniquindio.fintech.repository.RepositorioTransacciones;
import co.edu.uniquindio.fintech.structures.interfaces.ColaPrioridad;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import co.edu.uniquindio.fintech.structures.interfaces.TablaHash;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Reportes e indicadores (RF11; enunciado 4.8 y requisitos adicionales).
 *
 * <p><b>Criterio de conteo</b> (el enunciado no lo define): una transacción es
 * <b>efectiva</b> si está EXITOSA y no es una REVERSION. No cuentan las RECHAZADAS (no
 * movieron dinero), las REVERTIDAS (su efecto se deshizo) ni las reversiones. La
 * frecuencia por tipo es la excepción: cuenta todas las EXITOSAS, reversiones incluidas,
 * para mostrar cuántas hubo. Los rangos de fechas son cerrados {@code [desde, hasta]}.
 *
 * <p>Los rankings se ordenan con una cola de prioridad (mayor cantidad primero y, si
 * empatan, clave alfabética: resultado determinista).
 */
public final class AnaliticaService {

    /** Una clave con su conteo. */
    public record Conteo(String clave, long cantidad) {
    }

    /** Monto movilizado en un período. */
    public record MontoPeriodo(LocalDateTime desde, LocalDateTime hasta, BigDecimal total, long transacciones) {
    }

    private final RepositorioTransacciones transacciones;
    private final RepositorioBilleteras billeteras;
    private final FabricaEstructuras fabrica;

    public AnaliticaService(RepositorioTransacciones transacciones, RepositorioBilleteras billeteras,
                            FabricaEstructuras fabrica) {
        this.transacciones = transacciones;
        this.billeteras = billeteras;
        this.fabrica = fabrica;
    }

    /** Máximo de elementos que puede pedir un reporte de "los primeros N". */
    public static final int LIMITE_CANTIDAD = 100;

    public static boolean efectiva(Transaccion t) {
        return t.estado() == EstadoTransaccion.EXITOSA && !t.esReversion();
    }

    /** Billeteras que más participan (origen o destino) en transacciones efectivas. */
    public Lista<Conteo> billeterasMasUsadas(int cantidad) {
        validarCantidad(cantidad);
        TablaHash<String, Long> conteos = fabrica.nuevaTablaHash();
        for (Transaccion t : transacciones.todos()) {
            if (efectiva(t)) {
                sumar(conteos, t.billeteraOrigen());
                sumar(conteos, t.billeteraDestino());
            }
        }
        return primeros(conteos, cantidad);
    }

    /** Usuarios que más transferencias efectivas realizaron (como origen). */
    public Lista<Conteo> usuariosConMasTransferencias(int cantidad) {
        validarCantidad(cantidad);
        TablaHash<String, Long> conteos = fabrica.nuevaTablaHash();
        for (Transaccion t : transacciones.todos()) {
            if (efectiva(t) && t.tipo() == TipoTransaccion.TRANSFERENCIA) {
                sumar(conteos, transacciones.usuarioQueOrigino(t));
            }
        }
        return primeros(conteos, cantidad);
    }

    /** Tipos de billetera más activos: participaciones de sus billeteras en transacciones efectivas. */
    public Lista<Conteo> categoriasMasActivas() {
        TablaHash<String, Long> conteos = fabrica.nuevaTablaHash();
        for (Transaccion t : transacciones.todos()) {
            if (efectiva(t)) {
                for (String codigo : new String[]{t.billeteraOrigen(), t.billeteraDestino()}) {
                    Billetera b = codigo == null ? null : billeteras.buscar(codigo);
                    if (b != null) {
                        sumar(conteos, b.tipo().name());
                    }
                }
            }
        }
        return primeros(conteos, Integer.MAX_VALUE);
    }

    /** Transacciones EXITOSAS por tipo (todos los tipos, aunque tengan 0). */
    public Lista<Conteo> frecuenciaPorTipo() {
        Lista<Conteo> resultado = fabrica.nuevaLista();
        for (TipoTransaccion tipo : TipoTransaccion.values()) {
            long cantidad = 0;
            for (Transaccion t : transacciones.todos()) {
                if (t.tipo() == tipo && t.estado() == EstadoTransaccion.EXITOSA) {
                    cantidad++;
                }
            }
            resultado.agregar(new Conteo(tipo.name(), cantidad));
        }
        return resultado;
    }

    /** Suma de valores de las transacciones efectivas del período. */
    public MontoPeriodo montoMovilizado(LocalDateTime desde, LocalDateTime hasta) {
        validarPeriodo(desde, hasta);
        BigDecimal total = BigDecimal.ZERO;
        long cantidad = 0;
        for (Transaccion t : transacciones.todos()) {
            if (efectiva(t) && dentro(t, desde, hasta)) {
                total = total.add(t.valor());
                cantidad++;
            }
        }
        return new MontoPeriodo(desde, hasta, total, cantidad);
    }

    /** Usuario que realizó más transacciones efectivas en el período (empate: identificación menor). */
    public Conteo usuarioMasActivo(LocalDateTime desde, LocalDateTime hasta) {
        validarPeriodo(desde, hasta);
        TablaHash<String, Long> conteos = fabrica.nuevaTablaHash();
        for (Transaccion t : transacciones.todos()) {
            if (efectiva(t) && dentro(t, desde, hasta)) {
                sumar(conteos, transacciones.usuarioQueOrigino(t));
            }
        }
        Lista<Conteo> primero = primeros(conteos, 1);
        if (primero.estaVacia()) {
            throw new RecursoNoEncontradoException("SIN_ACTIVIDAD", "No hay transacciones efectivas en el período");
        }
        return primero.obtener(0);
    }

    /** Las {@code cantidad} transacciones efectivas de mayor valor (ABB por valor del repositorio). */
    public Lista<Transaccion> transaccionesDeMayorValor(int cantidad) {
        validarCantidad(cantidad);
        Lista<Transaccion> resultado = fabrica.nuevaLista();
        for (Transaccion t : transacciones.mayoresPorValor(transacciones.cantidad())) {
            if (resultado.tamanio() >= cantidad) {
                break;
            }
            if (efectiva(t)) {
                resultado.agregar(t);
            }
        }
        return resultado;
    }

    private static boolean dentro(Transaccion t, LocalDateTime desde, LocalDateTime hasta) {
        return !t.fecha().isBefore(desde) && !t.fecha().isAfter(hasta);
    }

    private static void validarCantidad(int cantidad) {
        if (cantidad < 1 || cantidad > LIMITE_CANTIDAD) {
            throw new SolicitudInvalidaException("CANTIDAD_INVALIDA", "La cantidad debe estar entre 1 y " + LIMITE_CANTIDAD);
        }
    }

    private static void validarPeriodo(LocalDateTime desde, LocalDateTime hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new SolicitudInvalidaException("PERIODO_INVALIDO", "El período debe cumplir desde ≤ hasta");
        }
    }

    private static void sumar(TablaHash<String, Long> conteos, String clave) {
        if (clave != null) {
            Long actual = conteos.obtener(clave);
            conteos.poner(clave, actual == null ? 1L : actual + 1);
        }
    }

    private Lista<Conteo> primeros(TablaHash<String, Long> conteos, int cantidad) {
        ColaPrioridad<Conteo> cola = fabrica.nuevaColaPrioridad((a, b) -> {
            int porCantidad = Long.compare(b.cantidad(), a.cantidad());
            return porCantidad != 0 ? porCantidad : a.clave().compareTo(b.clave());
        });
        for (String clave : conteos.claves()) {
            cola.insertar(new Conteo(clave, conteos.obtener(clave)));
        }
        Lista<Conteo> resultado = fabrica.nuevaLista();
        while (!cola.estaVacia() && resultado.tamanio() < cantidad) {
            resultado.agregar(cola.extraer());
        }
        return resultado;
    }
}
