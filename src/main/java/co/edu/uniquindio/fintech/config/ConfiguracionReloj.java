package co.edu.uniquindio.fintech.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Declara el {@link Clock} de la aplicación. Todo el código que necesite la fecha actual
 * debe recibirlo inyectado (no llamar a {@code LocalDateTime.now()} sin reloj), para que
 * las pruebas y la simulación de operaciones programadas puedan fijar el tiempo.
 *
 * <p><b>Provisional:</b> usa la zona horaria del sistema; si se requiere una zona fija
 * (p. ej. America/Bogota) se cambia solo aquí.
 */
@Configuration
public class ConfiguracionReloj {

    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }
}
