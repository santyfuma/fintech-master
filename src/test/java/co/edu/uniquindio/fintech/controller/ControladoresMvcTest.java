package co.edu.uniquindio.fintech.controller;

import co.edu.uniquindio.fintech.exception.ManejadorGlobalExcepciones;
import co.edu.uniquindio.fintech.soporte.RelojAjustable;
import co.edu.uniquindio.fintech.soporte.SistemaPrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;
import java.time.Duration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controladores reales sobre los servicios reales (directorio temporal, reloj ajustable)
 * con MockMvc standalone y el manejador global de excepciones. Requiere Spring MVC,
 * Jackson y Hibernate Validator reales ({@code ./mvnw test}).
 */
class ControladoresMvcTest {

    @TempDir
    Path directorio;

    private SistemaPrueba s;
    private MockMvc mvc;

    @BeforeEach
    void iniciar() {
        s = new SistemaPrueba(directorio, new RelojAjustable(), "CAFE|Bono café|5");
        mvc = MockMvcBuilders.standaloneSetup(
                        new UsuarioController(s.usuarios),
                        new BilleteraController(s.billeteras),
                        new TransaccionController(s.transacciones),
                        new OperacionProgramadaController(s.programadas, s.reloj),
                        new RecompensaController(s.recompensas, s.usuarios),
                        new NotificacionAuditoriaController(s.notificaciones, s.deteccion, s.usuarios),
                        new AnaliticaController(s.analitica, s.grafo, s.usuarios, s.benchmark))
                .setControllerAdvice(new ManejadorGlobalExcepciones(s.reloj))
                .build();
    }

    private void json(String metodo, String url, String cuerpo, int estado) throws Exception {
        var peticion = switch (metodo) {
            case "POST" -> post(url);
            case "PUT" -> put(url);
            default -> throw new IllegalArgumentException(metodo);
        };
        var r = mvc.perform(peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo));
        switch (estado) {
            case 200 -> r.andExpect(status().isOk());
            case 201 -> r.andExpect(status().isCreated());
            case 400 -> r.andExpect(status().isBadRequest());
            case 404 -> r.andExpect(status().isNotFound());
            case 409 -> r.andExpect(status().isConflict());
            default -> throw new IllegalArgumentException("estado " + estado);
        }
    }

    private void escenarioBase() throws Exception {
        json("POST", "/api/usuarios", "{\"identificacion\":\"100\",\"nombre\":\"Ana\",\"correo\":\"ana@correo.co\"}", 201);
        json("POST", "/api/usuarios", "{\"identificacion\":\"200\",\"nombre\":\"Luis\",\"correo\":\"luis@correo.co\"}", 201);
        json("POST", "/api/usuarios/100/billeteras", "{\"nombre\":\"Ahorros\",\"tipo\":\"AHORRO\"}", 201);   // BIL-000001
        json("POST", "/api/usuarios/200/billeteras", "{\"nombre\":\"Compras\",\"tipo\":\"COMPRAS\"}", 201); // BIL-000002
        json("POST", "/api/transacciones/recargas", "{\"billeteraDestino\":\"BIL-000001\",\"valor\":1000.50}", 201);
    }

    @Test
    void usuariosCrudYConsultasPorPuntos() throws Exception {
        escenarioBase();
        mvc.perform(get("/api/usuarios/100")).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.nivel").value("BRONCE"))
                .andExpect(jsonPath("$.puntosAcumulados").value(10));
        mvc.perform(get("/api/usuarios")).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/usuarios/100/saldo-total")).andExpect(jsonPath("$.saldoTotal").value(1000.50))
                .andExpect(jsonPath("$.billeteras").value(1));
        mvc.perform(get("/api/usuarios/100/puntos")).andExpect(jsonPath("$.siguienteNivel").value("PLATA"))
                .andExpect(jsonPath("$.puntosParaSiguienteNivel").value(491));
        mvc.perform(get("/api/usuarios/por-puntos").param("min", "5").param("max", "20"))
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].identificacion").value("100"));
        mvc.perform(get("/api/usuarios/por-nivel/BRONCE")).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/usuarios/mayor-puntaje")).andExpect(jsonPath("$.identificacion").value("100"));
        json("PUT", "/api/usuarios/200", "{\"nombre\":\"Luis P\",\"correo\":\"lp@correo.co\"}", 200);
        mvc.perform(delete("/api/usuarios/200")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("USUARIO_CON_REGISTROS"));
        json("POST", "/api/usuarios", "{\"identificacion\":\"300\",\"nombre\":\"C\",\"correo\":\"c@correo.co\"}", 201);
        mvc.perform(delete("/api/usuarios/300")).andExpect(status().isNoContent());
        mvc.perform(get("/api/usuarios/300")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("USUARIO_NO_ENCONTRADO"));
    }

    @Test
    void validacionHttpDeLasSolicitudes() throws Exception {
        escenarioBase();
        json("POST", "/api/usuarios", "{\"identificacion\":\"\",\"nombre\":\"A\",\"correo\":\"no-es-correo\"}", 400);
        mvc.perform(post("/api/transacciones/recargas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"billeteraDestino\":\"BIL-000001\",\"valor\":0}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDACION_FALLIDA"));
        mvc.perform(post("/api/transacciones/transferencias").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"billeteraOrigen\":\"BIL-000001\",\"billeteraDestino\":\"BIL-000001\",\"valor\":5}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDACION_FALLIDA"));
        json("POST", "/api/usuarios/100/billeteras", "{\"nombre\":\"X\",\"tipo\":\"NO_EXISTE\"}", 400);
        json("POST", "/api/transacciones/recargas", "{ mal json", 400);
        mvc.perform(get("/api/usuarios/por-nivel/DIAMANTE")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/reportes/billeteras-mas-usadas").param("cantidad", "abc"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("PARAMETRO_INVALIDO"));
        mvc.perform(get("/api/reportes/monto-movilizado")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PARAMETRO_FALTANTE"));
    }

    @Test
    void movimientosRechazosYReversion() throws Exception {
        escenarioBase();
        json("POST", "/api/transacciones/transferencias",
                "{\"billeteraOrigen\":\"BIL-000001\",\"billeteraDestino\":\"BIL-000002\",\"valor\":200.25}", 201);
        mvc.perform(get("/api/billeteras/BIL-000001/saldo")).andExpect(jsonPath("$.saldo").value(800.25));
        mvc.perform(post("/api/transacciones/retiros").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"billeteraOrigen\":\"BIL-000002\",\"valor\":5000}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("SALDO_INSUFICIENTE"));
        mvc.perform(get("/api/billeteras/BIL-000002/transacciones")).andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].estado").value("RECHAZADA"));
        mvc.perform(post("/api/usuarios/100/reversion/ultima")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("REVERSION"))
                .andExpect(jsonPath("$.transaccionRevertida").value("TRX-000002"));
        mvc.perform(post("/api/transacciones/TRX-000002/reversion")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("TRANSACCION_YA_REVERTIDA"));
        mvc.perform(get("/api/transacciones/TRX-000999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/usuarios/100/transacciones")).andExpect(jsonPath("$", hasSize(3)));
        mvc.perform(delete("/api/billeteras/BIL-000002")).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVA"));
        mvc.perform(post("/api/billeteras/BIL-000002/activacion")).andExpect(jsonPath("$.estado").value("ACTIVA"));
    }

    @Test
    void programadasManualesConElReloj() throws Exception {
        escenarioBase();
        String fecha = s.reloj.ahora().plusHours(1).toString();
        json("POST", "/api/operaciones-programadas", "{\"tipo\":\"TRANSFERENCIA\",\"valor\":100,"
                + "\"billeteraOrigen\":\"BIL-000001\",\"billeteraDestino\":\"BIL-000002\",\"fechaEjecucion\":\"" + fecha
                + "\",\"periodicidad\":\"SEMANAL\"}", 201);
        json("POST", "/api/operaciones-programadas", "{\"tipo\":\"RECARGA\",\"valor\":10,\"billeteraDestino\":\"BIL-000001\","
                + "\"fechaEjecucion\":\"2020-01-01T00:00:00\",\"periodicidad\":\"UNICA\"}", 400);
        mvc.perform(get("/api/operaciones-programadas/proxima")).andExpect(jsonPath("$.id").value("OPR-000001"));
        mvc.perform(post("/api/operaciones-programadas/procesar")).andExpect(jsonPath("$.ejecutadas").value(0));
        s.reloj.avanzar(Duration.ofHours(2));
        mvc.perform(post("/api/operaciones-programadas/procesar")).andExpect(status().isOk())
                .andExpect(jsonPath("$.ejecutadas").value(1))
                .andExpect(jsonPath("$.resultados[0].siguienteOperacion").value("OPR-000002"));
        mvc.perform(get("/api/operaciones-programadas").param("estado", "PENDIENTE")).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(delete("/api/operaciones-programadas/OPR-000002")).andExpect(jsonPath("$.estado").value("CANCELADA"));
        mvc.perform(get("/api/usuarios/100/operaciones-programadas")).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void recompensasNotificacionesYAuditoria() throws Exception {
        escenarioBase();
        mvc.perform(get("/api/niveles")).andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[1].puntosMinimos").value(501));
        mvc.perform(get("/api/beneficios")).andExpect(jsonPath("$[0].codigo").value("CAFE"));
        json("POST", "/api/usuarios/100/canjes", "{\"beneficio\":\"CAFE\"}", 201);
        json("POST", "/api/usuarios/100/canjes", "{\"beneficio\":\"CAFE\"}", 201);
        json("POST", "/api/usuarios/100/canjes", "{\"beneficio\":\"CAFE\"}", 409); // quedan 0 puntos
        json("POST", "/api/usuarios/100/canjes", "{\"beneficio\":\"\"}", 400);
        mvc.perform(get("/api/usuarios/100/canjes")).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/usuarios/100/notificaciones")).andExpect(jsonPath("$[0].tipo").value("CANJE_REALIZADO"));
        mvc.perform(post("/api/notificaciones/despachar")).andExpect(status().isOk());
        mvc.perform(get("/api/notificaciones/pendientes")).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/auditoria")).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/usuarios/999/auditoria")).andExpect(status().isNotFound());
    }

    @Test
    void reportesGrafoYBenchmark() throws Exception {
        escenarioBase();
        json("POST", "/api/transacciones/transferencias",
                "{\"billeteraOrigen\":\"BIL-000001\",\"billeteraDestino\":\"BIL-000002\",\"valor\":100}", 201);
        mvc.perform(get("/api/reportes/billeteras-mas-usadas").param("cantidad", "1"))
                .andExpect(jsonPath("$[0].clave").value("BIL-000001")).andExpect(jsonPath("$[0].cantidad").value(2));
        mvc.perform(get("/api/reportes/frecuencia-por-tipo")).andExpect(jsonPath("$", hasSize(4)));
        mvc.perform(get("/api/reportes/monto-movilizado").param("desde", "2026-11-01T00:00:00")
                .param("hasta", "2026-12-01T00:00:00")).andExpect(jsonPath("$.total").value(1100.50));
        mvc.perform(get("/api/reportes/usuario-mas-activo").param("desde", "2026-12-01T00:00:00")
                .param("hasta", "2026-11-01T00:00:00")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PERIODO_INVALIDO"));
        mvc.perform(get("/api/grafo/transferencias").param("criterio", "MONTO"))
                .andExpect(jsonPath("$.aristas[0].peso").value(100)).andExpect(jsonPath("$.tieneCiclo").value(false));
        mvc.perform(get("/api/grafo/rutas").param("origen", "100").param("destino", "200"))
                .andExpect(jsonPath("$.menosSaltos", hasSize(2)));
        mvc.perform(get("/api/grafo/rutas").param("origen", "100").param("destino", "999"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/grafo/ciclos").param("usuario", "100").param("longitudMaxima", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/benchmark").param("comparacion", "BUSQUEDA").param("elementos", "20")
                .param("repeticiones", "1")).andExpect(jsonPath("$.mediciones", hasSize(2)));
        mvc.perform(get("/api/benchmark").param("comparacion", "BUSQUEDA").param("elementos", "999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PARAMETRO_BENCHMARK_INVALIDO"));
    }
}
