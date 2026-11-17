package co.edu.uniquindio.fintech.exception;

import co.edu.uniquindio.fintech.dto.SolicitudTransferencia;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba el manejador dentro de Spring MVC real (MockMvc), con Jackson 3 y Bean
 * Validation reales. El controlador es interno a la prueba: no es un endpoint de la
 * aplicación.
 */
class ManejadorGlobalExcepcionesMvcTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-15T23:00:00Z"), ZoneId.of("America/Bogota"));

    private MockMvc mvc;

    @RestController
    @RequestMapping("/prueba")
    static class ControladorDePrueba {

        @PostMapping("/transferencias")
        public String transferir(@Valid @RequestBody SolicitudTransferencia solicitud) {
            return "ok";
        }

        @GetMapping("/no-encontrado")
        public String noEncontrado() {
            throw new RecursoNoEncontradoException("USUARIO_NO_ENCONTRADO", "No existe el usuario 123");
        }

        @GetMapping("/conflicto")
        public String conflicto() {
            throw new ConflictoNegocioException("SALDO_INSUFICIENTE", "Saldo insuficiente para realizar la operación");
        }

        @GetMapping("/persistencia")
        public String persistencia() {
            throw new PersistenciaException("No se pudo escribir /srv/secreto/usuarios.json",
                    new IllegalStateException("disco lleno"));
        }

        @GetMapping("/inesperado")
        public String inesperado() {
            throw new IllegalArgumentException("detalle interno");
        }

        @GetMapping("/cantidad")
        public String cantidad(@RequestParam int cantidad) {
            return "ok";
        }
    }

    @BeforeEach
    void configurar() {
        mvc = MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
                .setControllerAdvice(new ManejadorGlobalExcepciones(RELOJ))
                .build();
    }

    @Test
    void cuerpoValidoPasa() throws Exception {
        mvc.perform(post("/prueba/transferencias").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"billeteraOrigen\":\"BIL-000001\",\"billeteraDestino\":\"BIL-000007\",\"valor\":25000}"))
                .andExpect(status().isOk());
    }

    @Test
    void erroresDeValidacionSon400ConElFormatoDeLaApi() throws Exception {
        mvc.perform(post("/prueba/transferencias").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"billeteraOrigen\":\"BIL-000001\",\"billeteraDestino\":\"BIL-000001\",\"valor\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.error").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.mensaje").value(
                        "billeterasDistintas: La billetera origen y la destino deben ser distintas; "
                                + "valor: El valor debe ser mayor que cero"))
                .andExpect(jsonPath("$.fecha").value("2026-11-15T18:00:00"));
    }

    @Test
    void jsonMalFormadoEs400() throws Exception {
        mvc.perform(post("/prueba/transferencias").contentType(MediaType.APPLICATION_JSON).content("{\"valor\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("SOLICITUD_MAL_FORMADA"));
    }

    @Test
    void recursoNoEncontradoEs404() throws Exception {
        mvc.perform(get("/prueba/no-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.error").value("USUARIO_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensaje").value("No existe el usuario 123"));
    }

    @Test
    void conflictoEs409() throws Exception {
        mvc.perform(get("/prueba/conflicto"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.error").value("SALDO_INSUFICIENTE"))
                .andExpect(jsonPath("$.mensaje").value("Saldo insuficiente para realizar la operación"))
                .andExpect(jsonPath("$.fecha").value("2026-11-15T18:00:00"));
    }

    @Test
    void persistenciaEs500SinDetallesTecnicos() throws Exception {
        mvc.perform(get("/prueba/persistencia"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("ERROR_PERSISTENCIA"))
                .andExpect(jsonPath("$.mensaje").value(not(containsString("secreto"))));
    }

    @Test
    void errorDeProgramacionEs500YNo400() throws Exception {
        mvc.perform(get("/prueba/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("ERROR_INTERNO"))
                .andExpect(jsonPath("$.mensaje").value(not(containsString("detalle"))));
    }

    @Test
    void parametrosInvalidosOFaltantesSon400() throws Exception {
        mvc.perform(get("/prueba/cantidad").param("cantidad", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PARAMETRO_INVALIDO"));
        mvc.perform(get("/prueba/cantidad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PARAMETRO_FALTANTE"))
                .andExpect(jsonPath("$.mensaje").value("Falta el parámetro obligatorio 'cantidad'"));
    }

    @Test
    void metodoNoPermitidoConservaSu405() throws Exception {
        mvc.perform(delete("/prueba/conflicto"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.estado").value(405))
                .andExpect(jsonPath("$.error").value("METODO_NO_PERMITIDO"));
    }

    @Test
    void tipoDeContenidoNoSoportadoConservaSu415() throws Exception {
        mvc.perform(post("/prueba/transferencias").contentType(MediaType.TEXT_PLAIN).content("hola"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value("TIPO_CONTENIDO_NO_SOPORTADO"));
    }
}
