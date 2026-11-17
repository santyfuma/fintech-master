package co.edu.uniquindio.fintech.config;

import co.edu.uniquindio.fintech.FintechApplication;
import co.edu.uniquindio.fintech.exception.PersistenciaException;
import co.edu.uniquindio.fintech.persistence.AlmacenesJson;
import co.edu.uniquindio.fintech.persistence.DiarioOperaciones;
import co.edu.uniquindio.fintech.persistence.GeneradorIdentificadores;
import co.edu.uniquindio.fintech.persistence.TipoIdentificador;
import co.edu.uniquindio.fintech.repository.RepositorioAuditoria;
import co.edu.uniquindio.fintech.repository.RepositorioBeneficios;
import co.edu.uniquindio.fintech.repository.RepositorioBilleteras;
import co.edu.uniquindio.fintech.repository.RepositorioNotificaciones;
import co.edu.uniquindio.fintech.repository.RepositorioOperacionesProgramadas;
import co.edu.uniquindio.fintech.repository.RepositorioTransacciones;
import co.edu.uniquindio.fintech.repository.RepositorioUsuarios;
import co.edu.uniquindio.fintech.soporte.EntornoDatos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static co.edu.uniquindio.fintech.soporte.DatosPrueba.bil;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.billetera;
import static co.edu.uniquindio.fintech.soporte.DatosPrueba.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Arranca Spring Boot de verdad (sin servidor web) sobre un directorio temporal, y lo
 * reinicia para comprobar la persistencia y la recuperación del diario.
 */
class ArranqueAplicacionIntegracionTest {

    @TempDir
    Path directorio;

    // Los valores se pasan como argumentos de línea de comandos (máxima prioridad).
    // SpringApplicationBuilder.properties(...) solo define propiedades por defecto, que
    // pierden frente a application.properties de src/test/resources: con ellas la
    // aplicación arrancaba sobre target/datos-pruebas-contexto y no sobre el @TempDir.
    private ConfigurableApplicationContext arrancar() {
        return new SpringApplicationBuilder(FintechApplication.class)
                .web(WebApplicationType.NONE)
                .run("--fintech.datos.directorio=" + directorio.toAbsolutePath(),
                        "--spring.main.banner-mode=off");
    }

    @Test
    void arrancaConTodosLosRepositoriosCargadosYSinDatosFicticios() throws IOException {
        try (ConfigurableApplicationContext contexto = arrancar()) {
            assertEquals(0, contexto.getBean(RepositorioUsuarios.class).cantidad());
            assertEquals(0, contexto.getBean(RepositorioBilleteras.class).cantidad());
            assertEquals(0, contexto.getBean(RepositorioTransacciones.class).cantidad());
            assertEquals(0, contexto.getBean(RepositorioOperacionesProgramadas.class).cantidad());
            assertEquals(0, contexto.getBean(RepositorioNotificaciones.class).cantidad());
            assertEquals(0, contexto.getBean(RepositorioAuditoria.class).cantidad());
            assertEquals(0, contexto.getBean(RepositorioBeneficios.class).cantidad());
            assertTrue(contexto.getBean(DiarioOperaciones.class).pendientes().estaVacia());
        }
        try (var archivos = Files.list(directorio)) {
            assertEquals(0, archivos.count(), "no se deben crear archivos al arrancar sin datos");
        }
    }

    @Test
    void unReinicioConservaLosRegistros() {
        try (ConfigurableApplicationContext contexto = arrancar()) {
            contexto.getBean(RepositorioUsuarios.class).registrar(usuario("u1", 10));
            GeneradorIdentificadores generador = contexto.getBean(GeneradorIdentificadores.class);
            String codigo = generador.siguiente(TipoIdentificador.BILLETERA);
            assertEquals(bil(1), codigo);
            contexto.getBean(RepositorioBilleteras.class).registrar(billetera(1, "u1"));
        }
        try (ConfigurableApplicationContext contexto = arrancar()) {
            assertEquals(usuario("u1", 10), contexto.getBean(RepositorioUsuarios.class).buscar("u1"));
            assertEquals(1, contexto.getBean(RepositorioBilleteras.class).porPropietario("u1").tamanio());
            assertEquals(bil(2), contexto.getBean(GeneradorIdentificadores.class).siguiente(TipoIdentificador.BILLETERA));
        }
    }

    @Test
    void elDiarioSeRecuperaAntesDeCargarLosRepositorios() {
        EntornoDatos entorno = new EntornoDatos(directorio);
        entorno.diario.registrar(entorno.diario.preparar("OP-1", usuario("u1", 0), billetera(1, "u1")));
        try (ConfigurableApplicationContext contexto = arrancar()) {
            assertTrue(contexto.getBean(RepositorioBilleteras.class).existe(bil(1)));
            assertTrue(contexto.getBean(RepositorioUsuarios.class).existe("u1"));
            assertEquals(1, contexto.getBean(ContextoDatos.class).operacionesRecuperadas());
            assertTrue(contexto.getBean(DiarioOperaciones.class).pendientes().estaVacia());
        }
    }

    @Test
    void conElDiarioCorruptoLaAplicacionNoArranca() {
        new EntornoDatos(directorio).escribir(AlmacenesJson.ARCHIVO_DIARIO, "[{");
        Exception ex = assertThrows(Exception.class, this::arrancar);
        boolean causaPersistencia = false;
        for (Throwable t = ex; t != null; t = t.getCause()) {
            causaPersistencia |= t instanceof PersistenciaException;
        }
        assertTrue(causaPersistencia, "la causa raíz debe ser una PersistenciaException");
        assertFalse(Files.exists(directorio.resolve("usuarios.json")));
    }
}
