package co.edu.uniquindio.fintech.controller;

import co.edu.uniquindio.fintech.FintechApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de punta a punta: arranca la aplicación real con Tomcat en un puerto libre,
 * opera por HTTP y la reinicia sobre el mismo directorio. Requiere el entorno Maven real.
 */
class ApiHttpIntegracionTest {

    @TempDir
    Path directorio;

    private final HttpClient cliente = HttpClient.newHttpClient();

    // Los valores se pasan como argumentos de línea de comandos (máxima prioridad).
    // SpringApplicationBuilder.properties(...) solo define propiedades por defecto, que
    // pierden frente a application.properties de src/test/resources: con ellas la
    // aplicación arrancaba sobre target/datos-pruebas-contexto y no sobre el @TempDir.
    private ConfigurableApplicationContext arrancar() {
        return new SpringApplicationBuilder(FintechApplication.class)
                .run("--server.port=0",
                        "--fintech.datos.directorio=" + directorio.toAbsolutePath(),
                        "--fintech.programadas.automatico=false",
                        "--spring.main.banner-mode=off");
    }

    private HttpResponse<String> enviar(String base, String metodo, String ruta, String cuerpo) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + ruta)).header("Content-Type", "application/json");
        b = cuerpo == null ? b.method(metodo, HttpRequest.BodyPublishers.noBody())
                : b.method(metodo, HttpRequest.BodyPublishers.ofString(cuerpo));
        return cliente.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String base(ConfigurableApplicationContext c) {
        return "http://localhost:" + c.getEnvironment().getProperty("local.server.port");
    }

    @Test
    void flujoCompletoPorHttpYPersistenciaTrasReiniciar() throws Exception {
        try (ConfigurableApplicationContext c = arrancar()) {
            String b = base(c);
            assertEquals(201, enviar(b, "POST", "/api/usuarios",
                    "{\"identificacion\":\"100\",\"nombre\":\"Ana\",\"correo\":\"ana@correo.co\"}").statusCode());
            assertEquals(201, enviar(b, "POST", "/api/usuarios/100/billeteras",
                    "{\"nombre\":\"Ahorros\",\"tipo\":\"AHORRO\"}").statusCode());
            assertEquals(201, enviar(b, "POST", "/api/transacciones/recargas",
                    "{\"billeteraDestino\":\"BIL-000001\",\"valor\":150.75}").statusCode());
            HttpResponse<String> rechazo = enviar(b, "POST", "/api/transacciones/retiros",
                    "{\"billeteraOrigen\":\"BIL-000001\",\"valor\":1000}");
            assertEquals(409, rechazo.statusCode());
            assertTrue(rechazo.body().contains("\"error\":\"SALDO_INSUFICIENTE\""), rechazo.body());
            HttpResponse<String> invalida = enviar(b, "POST", "/api/transacciones/recargas", "{\"valor\":-1}");
            assertEquals(400, invalida.statusCode());
            assertTrue(invalida.body().contains("VALIDACION_FALLIDA"), invalida.body());
            HttpResponse<String> index = enviar(b, "GET", "/", null);
            assertEquals(200, index.statusCode());
            assertTrue(index.body().contains("<html"), "el frontend estático se sirve desde /");
        }
        try (ConfigurableApplicationContext c = arrancar()) {
            HttpResponse<String> saldo = enviar(base(c), "GET", "/api/billeteras/BIL-000001/saldo", null);
            assertEquals(200, saldo.statusCode());
            assertTrue(saldo.body().contains("150.75"), saldo.body());
            HttpResponse<String> historial = enviar(base(c), "GET", "/api/billeteras/BIL-000001/transacciones", null);
            assertTrue(historial.body().contains("RECHAZADA"), historial.body());
        }
    }
}
