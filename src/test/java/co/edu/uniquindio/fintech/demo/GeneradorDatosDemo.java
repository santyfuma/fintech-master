package co.edu.uniquindio.fintech.demo;

import co.edu.uniquindio.fintech.dto.SolicitudCreacionBilletera;
import co.edu.uniquindio.fintech.dto.SolicitudOperacionProgramada;
import co.edu.uniquindio.fintech.dto.SolicitudRegistroUsuario;
import co.edu.uniquindio.fintech.exception.ConflictoNegocioException;
import co.edu.uniquindio.fintech.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.fintech.model.Periodicidad;
import co.edu.uniquindio.fintech.model.TipoBilletera;
import co.edu.uniquindio.fintech.model.TipoOperacionProgramada;
import co.edu.uniquindio.fintech.model.Transaccion;
import co.edu.uniquindio.fintech.soporte.RelojAjustable;
import co.edu.uniquindio.fintech.soporte.SistemaPrueba;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Genera el conjunto de datos de demostración ({@code datos-demo/}) ejecutando una
 * historia ficticia de seis semanas con los servicios reales y un reloj controlado.
 * Así los archivos JSON cumplen todas las reglas de negocio y el verificador de
 * integridad. Todas las personas, correos y montos son inventados.
 *
 * <p>Uso: {@code GeneradorDatosDemo <directorio-vacío>}. Debe usar el mismo catálogo que
 * {@code application.properties} ({@link #CATALOGO}).
 */
public final class GeneradorDatosDemo {

    /** Catálogo de demostración (ficticio; el enunciado no define costos). */
    public static final String CATALOGO = "BONO_CAFE|Bono de café|500;DESC_COMISION|Descuento del 50% en una comisión|2000;"
            + "CINE|Entrada de cine|3500;MILLAS|1000 millas de viaje|8000";

    private final SistemaPrueba s;

    private GeneradorDatosDemo(Path directorio) {
        this.s = new SistemaPrueba(directorio, new RelojAjustable(LocalDateTime.of(2026, 10, 1, 8, 0)), CATALOGO);
    }

    public static void main(String[] args) throws Exception {
        Path destino = Path.of(args.length > 0 ? args[0] : "datos-demo");
        if (Files.exists(destino)) {
            try (var contenido = Files.list(destino)) {
                if (contenido.findAny().isPresent()) {
                    throw new IllegalStateException("El directorio debe estar vacío: " + destino.toAbsolutePath());
                }
            }
        }
        Files.createDirectories(destino);
        new GeneradorDatosDemo(destino).generar();
        System.out.println("Datos de demostración generados en " + destino.toAbsolutePath());
    }

    // ------------------------------------------------------------------ atajos

    private void a(int dia, int hora, int minuto) {
        LocalDateTime objetivo = LocalDateTime.of(2026, dia >= 100 ? 11 : 10, dia >= 100 ? dia - 100 : dia, hora, minuto);
        if (objetivo.isBefore(s.reloj.ahora())) {
            throw new IllegalStateException("El guion retrocede en el tiempo: " + objetivo);
        }
        // Como la tarea periódica: cada programada se ejecuta a su hora, no a la del siguiente evento.
        LocalDateTime proxima = siguienteProgramada();
        while (proxima != null && !proxima.isAfter(objetivo)) {
            if (proxima.isAfter(s.reloj.ahora())) {
                s.reloj.fijar(proxima);
            }
            s.programadas.procesarVencidas();
            proxima = siguienteProgramada();
        }
        s.reloj.fijar(objetivo);
        s.programadas.procesarVencidas();
    }

    private LocalDateTime siguienteProgramada() {
        try {
            return s.programadas.siguientePendiente().fechaEjecucion();
        } catch (RecursoNoEncontradoException ninguna) {
            return null;
        }
    }

    private static BigDecimal v(String valor) {
        return new BigDecimal(valor);
    }

    private void usuario(String id, String nombre, String correo) {
        s.usuarios.registrar(new SolicitudRegistroUsuario(id, nombre, correo));
    }

    private String billetera(String usuario, String nombre, TipoBilletera tipo) {
        return s.billeteras.crear(usuario, new SolicitudCreacionBilletera(nombre, tipo)).codigo();
    }

    private Transaccion recargar(String b, String valor) {
        return s.transacciones.recargar(b, v(valor));
    }

    private Transaccion retirar(String b, String valor) {
        return s.transacciones.retirar(b, v(valor));
    }

    private Transaccion transferir(String o, String d, String valor) {
        return s.transacciones.transferir(o, d, v(valor));
    }

    /** Operación que el sistema debe rechazar: queda registrada como RECHAZADA. */
    private void rechazada(Runnable operacion) {
        try {
            operacion.run();
            throw new IllegalStateException("Se esperaba un rechazo");
        } catch (ConflictoNegocioException esperado) {
            // registrada como RECHAZADA con su notificación
        }
    }

    private String programar(TipoOperacionProgramada tipo, String valor, String origen, String destino,
                             LocalDateTime fecha, Periodicidad periodicidad) {
        return s.programadas.programar(new SolicitudOperacionProgramada(tipo, v(valor), origen, destino, fecha,
                periodicidad)).id();
    }

    // ------------------------------------------------------------------ historia

    private void generar() {
        // 1 de octubre: registro, billeteras y recargas iniciales.
        usuario("1001", "Laura Restrepo", "laura.restrepo@ejemplo.co");
        usuario("1002", "Andrés Muñoz", "andres.munoz@ejemplo.co");
        usuario("1003", "Camila Ospina", "camila.ospina@ejemplo.co");
        usuario("1004", "Santiago Giraldo", "santiago.giraldo@ejemplo.co");
        usuario("1005", "Valentina Cardona", "valentina.cardona@ejemplo.co");
        usuario("1006", "Mateo Quintero", "mateo.quintero@ejemplo.co");
        usuario("1007", "Daniela Henao", "daniela.henao@ejemplo.co");
        usuario("1008", "Julián Arango", "julian.arango@ejemplo.co"); // sin billeteras: se puede eliminar

        String lauraAhorro = billetera("1001", "Ahorro principal", TipoBilletera.AHORRO);
        String lauraInversion = billetera("1001", "Inversiones", TipoBilletera.INVERSION);
        String lauraGastos = billetera("1001", "Gastos del mes", TipoBilletera.GASTOS_DIARIOS);
        String andresAhorro = billetera("1002", "Ahorros", TipoBilletera.AHORRO);
        String andresTransporte = billetera("1002", "Transporte", TipoBilletera.TRANSPORTE);
        String camilaCompras = billetera("1003", "Compras", TipoBilletera.COMPRAS);
        String camilaSuscripciones = billetera("1003", "Suscripciones", TipoBilletera.SUSCRIPCIONES);
        String santiagoGastos = billetera("1004", "Gastos", TipoBilletera.GASTOS_DIARIOS);
        String valentinaAhorro = billetera("1005", "Ahorros", TipoBilletera.AHORRO);
        String valentinaCompras = billetera("1005", "Compras en línea", TipoBilletera.COMPRAS);
        String mateoAhorro = billetera("1006", "Ahorros", TipoBilletera.AHORRO);
        String danielaGastos = billetera("1007", "Gastos", TipoBilletera.GASTOS_DIARIOS);

        a(1, 9, 0);
        recargar(lauraAhorro, "2500000");       // PLATINO de entrada
        a(1, 9, 30);
        recargar(lauraInversion, "1000000");
        a(1, 10, 0);
        recargar(lauraGastos, "400000");
        a(1, 10, 30);
        recargar(andresAhorro, "120000");      // ORO
        a(1, 11, 0);
        recargar(andresTransporte, "40000");
        a(1, 11, 30);
        recargar(camilaCompras, "45000");      // BRONCE por ahora
        a(1, 12, 0);
        recargar(santiagoGastos, "20000");
        a(1, 14, 0);
        recargar(valentinaAhorro, "120000");
        a(1, 14, 30);
        recargar(valentinaCompras, "50000");
        a(1, 15, 0);
        recargar(mateoAhorro, "150000");
        a(1, 16, 0);
        recargar(danielaGastos, "40000");

        // Programadas: recarga semanal de transporte (se ejecuta cada semana y deja la
        // siguiente pendiente) y transferencia mensual de Laura a Camila.
        a(2, 9, 0);
        programar(TipoOperacionProgramada.RECARGA, "10000", null, andresTransporte,
                LocalDateTime.of(2026, 10, 6, 8, 0), Periodicidad.SEMANAL);
        programar(TipoOperacionProgramada.TRANSFERENCIA, "100000", lauraAhorro, camilaCompras,
                LocalDateTime.of(2026, 10, 10, 9, 0), Periodicidad.MENSUAL);
        // Retiro programado que fallará por saldo insuficiente.
        programar(TipoOperacionProgramada.RETIRO, "500000", santiagoGastos, null,
                LocalDateTime.of(2026, 10, 25, 10, 0), Periodicidad.UNICA);

        // Red de transferencias Laura → Andrés → Camila → Laura (ciclo), pagos de transporte
        // de Andrés y, en paralelo, los retiros normales de Mateo (base para MONTO ALTO).
        a(2, 12, 0);
        transferir(lauraGastos, andresAhorro, "50000");
        a(2, 13, 0);
        retirar(andresTransporte, "8000");

        // Billetera inactiva: Camila desactiva Suscripciones (saldo 0) y una transferencia
        // hacia ella es rechazada.
        a(3, 10, 0);
        s.billeteras.desactivar(camilaSuscripciones);
        a(4, 11, 0);
        rechazada(() -> transferir(lauraGastos, camilaSuscripciones, "20000"));

        a(5, 10, 0);
        retirar(mateoAhorro, "5000");
        a(5, 12, 0);
        transferir(lauraGastos, andresAhorro, "50000");
        a(5, 13, 0);
        retirar(andresTransporte, "8000");

        a(6, 14, 0); // a las 8:00 ya se ejecutó la recarga semanal programada
        transferir(andresAhorro, camilaCompras, "10000");

        a(8, 10, 0);
        retirar(mateoAhorro, "5000");
        a(8, 12, 0);
        transferir(lauraGastos, andresAhorro, "50000");
        a(8, 13, 0);
        retirar(andresTransporte, "8000");

        a(9, 14, 0);
        transferir(andresAhorro, camilaCompras, "10000");

        // Santiago: retiro que deja saldo bajo (la transferencia mensual de Laura ya se
        // ejecutó a las 9:00).
        a(10, 17, 0);
        retirar(santiagoGastos, "12000");

        a(11, 10, 0);
        retirar(mateoAhorro, "5000");
        a(11, 12, 0);
        transferir(lauraGastos, andresAhorro, "50000");
        a(11, 13, 0);
        retirar(andresTransporte, "8000");
        // Santiago intenta retirar más de lo que tiene: RECHAZADA.
        a(11, 17, 30);
        rechazada(() -> retirar(santiagoGastos, "20000"));

        // Camila cierra el ciclo; con esa transferencia sube de BRONCE a PLATA.
        a(12, 16, 0);
        transferir(camilaCompras, lauraGastos, "10000");
        a(12, 18, 0);
        recargar(camilaCompras, "10000");

        // Daniela: actividad base en la semana previa.
        a(13, 11, 0);
        recargar(danielaGastos, "10000");

        // Mateo: retiro muy superior a su promedio (MONTO INUSUALMENTE ALTO).
        a(14, 10, 30);
        retirar(mateoAhorro, "60000");
        a(14, 12, 0);
        transferir(lauraGastos, andresAhorro, "50000");
        a(14, 13, 0);
        retirar(andresTransporte, "8000");

        // Reversión por identificador: Laura recarga dos veces lo mismo y revierte la duplicada.
        a(15, 10, 0);
        recargar(lauraGastos, "200000");
        a(15, 10, 5);
        Transaccion duplicada = recargar(lauraGastos, "200000");
        a(15, 10, 20);
        s.transacciones.revertir(duplicada.id());

        // Valentina: fragmenta un pago entre sus dos billeteras (FRAGMENTACIÓN) y hace tres
        // transferencias en menos de cinco minutos (CONSECUTIVAS RÁPIDAS).
        a(16, 14, 0);
        transferir(valentinaAhorro, santiagoGastos, "25000");
        a(16, 14, 3);
        transferir(valentinaCompras, santiagoGastos, "25000");
        a(16, 14, 4);
        transferir(valentinaAhorro, danielaGastos, "5000");

        // Andrés: tres transferencias a Camila en menos de una hora (DESTINO REPETITIVO).
        a(17, 9, 0);
        transferir(andresAhorro, camilaCompras, "5000");
        a(17, 9, 20);
        transferir(andresAhorro, camilaCompras, "5000");
        a(17, 9, 40);
        transferir(andresAhorro, camilaCompras, "5000");

        // Mateo: operación de madrugada, lejos de su horario habitual (HORARIO NO HABITUAL).
        a(18, 3, 0);
        recargar(mateoAhorro, "20000");

        // Canjes de puntos.
        a(18, 12, 0);
        s.recompensas.canjear("1001", "MILLAS");
        a(18, 12, 30);
        s.recompensas.canjear("1002", "BONO_CAFE");

        // Andrés transfiere a Santiago por error y deshace su última operación (pila).
        a(19, 13, 0);
        transferir(andresAhorro, santiagoGastos, "15000");
        a(19, 13, 30);
        s.transacciones.revertirUltima("1002");

        // Daniela: cinco retiros en un día frente a una sola operación la semana anterior
        // (CAMBIO BRUSCO DE FRECUENCIA).
        for (int h = 9; h <= 13; h++) {
            a(20, h, 0);
            retirar(danielaGastos, "2000");
        }

        // Despacho de las notificaciones acumuladas hasta aquí (FIFO).
        a(21, 8, 0);
        s.notificaciones.despacharPendientes();

        // Programadas futuras y una cancelada.
        a(22, 10, 0);
        String cancelada = programar(TipoOperacionProgramada.RECARGA, "20000", null, danielaGastos,
                LocalDateTime.of(2026, 11, 25, 9, 0), Periodicidad.UNICA);
        s.programadas.cancelar(cancelada);
        programar(TipoOperacionProgramada.TRANSFERENCIA, "15000", camilaCompras, danielaGastos,
                LocalDateTime.of(2026, 11, 30, 10, 0), Periodicidad.UNICA);
        programar(TipoOperacionProgramada.RECARGA, "100000", null, mateoAhorro,
                LocalDateTime.of(2026, 12, 1, 9, 0), Periodicidad.UNICA);

        // Semanas siguientes: actividad moderada (el reloj ejecuta las programadas).
        a(25, 10, 30); // vence el retiro programado de Santiago: FALLIDA
        a(28, 11, 0);
        transferir(lauraAhorro, valentinaAhorro, "80000");
        a(102, 14, 30);
        transferir(valentinaAhorro, lauraAhorro, "10000");
        a(105, 9, 0);
        retirar(lauraInversion, "150000");
        a(109, 17, 0);
        transferir(santiagoGastos, andresAhorro, "2000");
        a(112, 12, 0);
        transferir(lauraGastos, andresAhorro, "50000");
        a(113, 14, 0);
        retirar(valentinaCompras, "10000");
        a(115, 18, 0);
    }
}
