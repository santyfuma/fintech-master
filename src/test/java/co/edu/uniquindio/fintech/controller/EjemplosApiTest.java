package co.edu.uniquindio.fintech.controller;

import co.edu.uniquindio.fintech.dto.SolicitudCanje;
import co.edu.uniquindio.fintech.dto.SolicitudCreacionBilletera;
import co.edu.uniquindio.fintech.dto.SolicitudOperacionProgramada;
import co.edu.uniquindio.fintech.dto.SolicitudRecarga;
import co.edu.uniquindio.fintech.dto.SolicitudRegistroUsuario;
import co.edu.uniquindio.fintech.dto.SolicitudRetiro;
import co.edu.uniquindio.fintech.dto.SolicitudTransferencia;
import co.edu.uniquindio.fintech.exception.ConflictoNegocioException;
import co.edu.uniquindio.fintech.exception.FintechException;
import co.edu.uniquindio.fintech.exception.ManejadorGlobalExcepciones;
import co.edu.uniquindio.fintech.exception.PersistenciaException;
import co.edu.uniquindio.fintech.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.fintech.exception.SolicitudInvalidaException;
import co.edu.uniquindio.fintech.model.Periodicidad;
import co.edu.uniquindio.fintech.model.TipoBilletera;
import co.edu.uniquindio.fintech.model.TipoOperacionProgramada;
import co.edu.uniquindio.fintech.persistence.MapeadorJson;
import co.edu.uniquindio.fintech.soporte.RelojAjustable;
import co.edu.uniquindio.fintech.soporte.SistemaPrueba;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Genera {@code target/ejemplos-api.md}: los casos de ejecución de docs/ejemplos-api.md.
 * Invoca los controladores reales (en proceso, sin servidor HTTP) sobre los servicios
 * reales, con un reloj fijo, y serializa solicitudes y respuestas con el mismo mapeador
 * JSON de la aplicación. Los errores pasan por el manejador global real. Es reproducible:
 * mismos datos, mismos identificadores y mismas fechas en cada ejecución.
 */
class EjemplosApiTest {

    @TempDir
    Path directorio;

    private final JsonMapper json = MapeadorJson.crear();
    private final StringBuilder md = new StringBuilder();
    private SistemaPrueba s;
    private ManejadorGlobalExcepciones manejador;
    private UsuarioController usuarios;
    private BilleteraController billeteras;
    private TransaccionController transacciones;
    private OperacionProgramadaController programadas;
    private RecompensaController recompensas;
    private NotificacionAuditoriaController avisos;
    private AnaliticaController analitica;
    private int casos;

    interface Llamada {
        Object ejecutar();
    }

    private void arrancar() {
        s = s == null ? new SistemaPrueba(directorio, new RelojAjustable(), "CAFE|Bono de café|20") : s.reiniciar();
        manejador = new ManejadorGlobalExcepciones(s.reloj);
        usuarios = new UsuarioController(s.usuarios);
        billeteras = new BilleteraController(s.billeteras);
        transacciones = new TransaccionController(s.transacciones);
        programadas = new OperacionProgramadaController(s.programadas, s.reloj);
        recompensas = new RecompensaController(s.recompensas, s.usuarios);
        avisos = new NotificacionAuditoriaController(s.notificaciones, s.deteccion, s.usuarios);
        analitica = new AnaliticaController(s.analitica, s.grafo, s.usuarios, s.benchmark);
    }

    private void seccion(String titulo, String explicacion) {
        md.append("\n## ").append(titulo).append("\n\n").append(explicacion).append("\n");
    }

    private int llamar(String metodo, String ruta, Object cuerpo, int estadoExito, Llamada llamada) {
        md.append("\n```http\n").append(metodo).append(' ').append(ruta).append('\n');
        if (cuerpo != null) {
            md.append("Content-Type: application/json\n\n").append(json.writeValueAsString(cuerpo)).append('\n');
        }
        md.append("```\n");
        int estado;
        Object respuesta;
        try {
            respuesta = llamada.ejecutar();
            estado = estadoExito;
        } catch (FintechException e) {
            ResponseEntity<?> r = switch (e) {
                case RecursoNoEncontradoException x -> manejador.manejarRecursoNoEncontrado(x);
                case ConflictoNegocioException x -> manejador.manejarConflictoNegocio(x);
                case SolicitudInvalidaException x -> manejador.manejarSolicitudInvalida(x);
                case PersistenciaException x -> manejador.manejarPersistencia(x);
                default -> throw e;
            };
            estado = r.getStatusCode().value();
            respuesta = r.getBody();
        }
        md.append("\nRespuesta `").append(estado).append("`");
        if (respuesta != null) {
            md.append(":\n\n```json\n").append(json.writeValueAsString(respuesta)).append("\n```\n");
        } else {
            md.append(" (sin cuerpo)\n");
        }
        casos++;
        return estado;
    }

    private static BigDecimal v(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    void generaLosCasosDeEjecucion() throws IOException {
        arrancar();
        md.append("# Casos de ejecución de la API\n\n")
                .append("> Generado por `EjemplosApiTest` (src/test/java/.../controller). Las respuestas salen de los\n")
                .append("> controladores, servicios, persistencia y manejador de errores **reales**, invocados en el\n")
                .append("> mismo proceso (sin servidor HTTP) y serializadas con el `JsonMapper` de la aplicación.\n")
                .append("> El reloj está fijo en 2026-11-15T18:00 (America/Bogota) y avanza solo donde se indica.\n")
                .append("> Para reproducirlos contra el servidor use `docs/pruebas.http` sobre un directorio de datos vacío;\n")
                .append("> allí las fechas serán las del reloj real. Las validaciones de Bean Validation (400\n")
                .append("> `VALIDACION_FALLIDA`) solo ocurren por HTTP y se ilustran en `docs/pruebas.http`.\n");

        seccion("1. Registro de usuario", "Se registran dos usuarios; repetir la identificación es un conflicto.");
        assertEquals(201, llamar("POST", "/api/usuarios", new SolicitudRegistroUsuario("1001", "Ana Gómez", "ana@correo.co"),
                201, () -> usuarios.registrar(new SolicitudRegistroUsuario("1001", "Ana Gómez", "ana@correo.co"))));
        llamar("POST", "/api/usuarios", new SolicitudRegistroUsuario("2002", "Luis Pérez", "luis@correo.co"), 201,
                () -> usuarios.registrar(new SolicitudRegistroUsuario("2002", "Luis Pérez", "luis@correo.co")));
        assertEquals(409, llamar("POST", "/api/usuarios", new SolicitudRegistroUsuario("1001", "Otra", "otra@correo.co"),
                201, () -> usuarios.registrar(new SolicitudRegistroUsuario("1001", "Otra", "otra@correo.co"))));

        seccion("2. Creación de billeteras", "El código es consecutivo; el saldo inicial es 0 y la billetera queda ACTIVA.");
        SolicitudCreacionBilletera ahorro = new SolicitudCreacionBilletera("Ahorros", TipoBilletera.AHORRO);
        llamar("POST", "/api/usuarios/1001/billeteras", ahorro, 201, () -> billeteras.crear("1001", ahorro));
        SolicitudCreacionBilletera compras = new SolicitudCreacionBilletera("Compras", TipoBilletera.COMPRAS);
        llamar("POST", "/api/usuarios/1001/billeteras", compras, 201, () -> billeteras.crear("1001", compras));
        llamar("POST", "/api/usuarios/2002/billeteras", ahorro, 201, () -> billeteras.crear("2002", ahorro));

        seccion("3. Recarga", "1 punto por cada 100 completos (política provisional): 1500.50 → 15 puntos.");
        SolicitudRecarga recarga = new SolicitudRecarga("BIL-000001", v("1500.50"));
        llamar("POST", "/api/transacciones/recargas", recarga, 201, () -> transacciones.recargar(recarga));

        seccion("4. Retiro", "2 puntos por cada 100. Un retiro mayor que el saldo responde 409 y queda RECHAZADO en el historial.");
        SolicitudRetiro retiro = new SolicitudRetiro("BIL-000001", v("200"));
        llamar("POST", "/api/transacciones/retiros", retiro, 201, () -> transacciones.retirar(retiro));
        s.reloj.avanzar(Duration.ofMinutes(10));
        SolicitudRetiro excesivo = new SolicitudRetiro("BIL-000002", v("10"));
        assertEquals(409, llamar("POST", "/api/transacciones/retiros", excesivo, 201, () -> transacciones.retirar(excesivo)));
        llamar("GET", "/api/billeteras/BIL-000002/transacciones", null, 200, () -> transacciones.deBilletera("BIL-000002"));

        seccion("5. Transferencia", "Entre usuarios distintos; 3 puntos por cada 100 para quien envía.");
        s.reloj.avanzar(Duration.ofMinutes(10));
        SolicitudTransferencia transferencia = new SolicitudTransferencia("BIL-000001", "BIL-000003", v("300.25"));
        llamar("POST", "/api/transacciones/transferencias", transferencia, 201, () -> transacciones.transferir(transferencia));
        llamar("GET", "/api/billeteras/BIL-000001/saldo", null, 200, () -> billeteras.saldo("BIL-000001"));

        seccion("6. Reversión", "Deshace la última operación reversible del usuario (cima de su pila): la transferencia. "
                + "Devuelve el dinero, marca la original REVERTIDA y descuenta sus puntos. Revertirla otra vez es un conflicto, "
                + "y un retiro no es reversible con la política provisional.");
        s.reloj.avanzar(Duration.ofMinutes(10));
        llamar("POST", "/api/usuarios/1001/reversion/ultima", null, 201, () -> transacciones.revertirUltima("1001"));
        llamar("POST", "/api/transacciones/TRX-000004/reversion", null, 201, () -> transacciones.revertir("TRX-000004"));
        llamar("POST", "/api/transacciones/TRX-000002/reversion", null, 201, () -> transacciones.revertir("TRX-000002"));

        seccion("7. Programación de una operación", "Transferencia semanal de 1001 a 2002 dentro de una hora.");
        SolicitudOperacionProgramada programada = new SolicitudOperacionProgramada(TipoOperacionProgramada.TRANSFERENCIA,
                v("100"), "BIL-000001", "BIL-000003", s.reloj.ahora().plusHours(1), Periodicidad.SEMANAL);
        llamar("POST", "/api/operaciones-programadas", programada, 201, () -> programadas.programar(programada));
        llamar("GET", "/api/operaciones-programadas/proxima", null, 200, programadas::proxima);

        seccion("8. Ejecución simulada", "Se adelanta el reloj 2 horas y se procesan las vencidas: se ejecuta con bono "
                + "de 10 puntos (provisional) y se crea la siguiente ocurrencia semanal.");
        s.reloj.avanzar(Duration.ofHours(2));
        llamar("POST", "/api/operaciones-programadas/procesar", null, 200, programadas::procesar);

        seccion("9. Acumulación de puntos", "Puntos y nivel actuales del usuario 1001.");
        llamar("GET", "/api/usuarios/1001/puntos", null, 200, () -> usuarios.puntos("1001"));
        SolicitudCanje canje = new SolicitudCanje("CAFE");
        llamar("POST", "/api/usuarios/1001/canjes", canje, 201, () -> recompensas.canjear("1001", canje));

        seccion("10. Ascenso de nivel", "Una recarga de 50100 suma 501 puntos: el usuario 2002 pasa de BRONCE (0–500) a PLATA "
                + "(501–1000) y recibe una notificación ASCENSO_NIVEL.");
        SolicitudRecarga grande = new SolicitudRecarga("BIL-000003", v("50100"));
        llamar("POST", "/api/transacciones/recargas", grande, 201, () -> transacciones.recargar(grande));
        llamar("GET", "/api/usuarios/2002/notificaciones", null, 200, () -> avisos.deUsuario("2002"));

        seccion("11. Detección de comportamiento inusual", "Tres transferencias en menos de 5 minutos (política "
                + "provisional): la tercera queda con riesgo MEDIO, se audita y se notifica ALERTA_SEGURIDAD.");
        for (String destino : new String[]{"BIL-000001", "BIL-000002"}) {
            s.reloj.avanzar(Duration.ofMinutes(1));
            SolicitudTransferencia t = new SolicitudTransferencia("BIL-000003", destino, v("10"));
            llamar("POST", "/api/transacciones/transferencias", t, 201, () -> transacciones.transferir(t));
        }
        s.reloj.avanzar(Duration.ofMinutes(1));
        SolicitudTransferencia tercera = new SolicitudTransferencia("BIL-000003", "BIL-000001", v("15"));
        llamar("POST", "/api/transacciones/transferencias", tercera, 201, () -> transacciones.transferir(tercera));
        llamar("GET", "/api/auditoria", null, 200, () -> avisos.auditoria(null));

        seccion("12. Consulta de analítica", "Reportes sobre transacciones efectivas (EXITOSA y no reversión).");
        llamar("GET", "/api/reportes/billeteras-mas-usadas?cantidad=3", null, 200, () -> analitica.billeterasMasUsadas(3));
        llamar("GET", "/api/reportes/frecuencia-por-tipo", null, 200, analitica::frecuenciaPorTipo);
        llamar("GET", "/api/reportes/monto-movilizado?desde=2026-11-15T00:00:00&hasta=2026-11-16T00:00:00", null, 200,
                () -> analitica.montoMovilizado(s.reloj.ahora().withHour(0).withMinute(0),
                        s.reloj.ahora().withHour(0).withMinute(0).plusDays(1)));

        seccion("13. Rutas y ciclos", "Grafo entre usuarios (peso = cantidad de transferencias exitosas; la revertida no cuenta). "
                + "1001 → 2002 (programada) y 2002 → 1001 forman un ciclo.");
        llamar("GET", "/api/grafo/transferencias", null, 200, () -> analitica.transferencias(null));
        llamar("GET", "/api/grafo/rutas?origen=2002&destino=1001", null, 200, () -> analitica.rutas("2002", "1001", null));
        llamar("GET", "/api/grafo/ciclos?usuario=1001&longitudMaxima=4", null, 200, () -> analitica.ciclos("1001", 4));

        seccion("14. Recuperación de una operación interrumpida", "Se simula que `transacciones.json` no se puede "
                + "escribir. La operación ya quedó en el diario: responde 500 (mensaje genérico) y no se aceptan otras "
                + "modificaciones hasta completarla. Al reiniciar la aplicación con el archivo disponible, el diario la "
                + "completa y el saldo y el historial quedan consistentes.");
        // bloquearEscritura reemplaza el archivo por un directorio: se guarda su contenido para
        // restaurarlo tal como estaba antes de la escritura fallida.
        String previo = s.entorno.leer("transacciones.json");
        s.entorno.bloquearEscritura("transacciones.json");
        SolicitudRecarga interrumpida = new SolicitudRecarga("BIL-000002", v("75.50"));
        assertEquals(500, llamar("POST", "/api/transacciones/recargas", interrumpida, 201,
                () -> transacciones.recargar(interrumpida)));
        md.append("\n*(reinicio de la aplicación con `transacciones.json` de nuevo escribible)*\n");
        s.desbloquear("transacciones.json");
        s.entorno.escribir("transacciones.json", previo);
        assertEquals(1, s.entorno.diario.pendientes().tamanio());
        arrancar();
        llamar("GET", "/api/billeteras/BIL-000002/saldo", null, 200, () -> billeteras.saldo("BIL-000002"));
        llamar("GET", "/api/billeteras/BIL-000002/transacciones", null, 200, () -> transacciones.deBilletera("BIL-000002"));
        assertTrue(s.entorno.diario.pendientes().estaVacia());

        Path destino = Path.of("target", "ejemplos-api.md");
        Files.createDirectories(destino.getParent());
        Files.writeString(destino, md.toString());
        assertTrue(casos >= 30);
    }
}
