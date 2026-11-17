package co.edu.uniquindio.fintech.config;

import co.edu.uniquindio.fintech.service.OperacionProgramadaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Simulación de la ejecución automática de operaciones programadas (requisito adicional
 * del enunciado). Llama periódicamente a la misma lógica que el disparo manual
 * ({@code POST /api/operaciones-programadas/procesar}), sin duplicarla.
 *
 * <p>Se activa con {@code fintech.programadas.automatico=true} y el intervalo se fija con
 * {@code fintech.programadas.intervalo-ms}. <b>Provisional:</b> el enunciado no define la
 * modalidad, por eso existen ambas.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "fintech.programadas.automatico", havingValue = "true")
public class EjecucionAutomaticaProgramadas {

    private static final Logger LOG = LoggerFactory.getLogger(EjecucionAutomaticaProgramadas.class);

    private final OperacionProgramadaService operaciones;

    public EjecucionAutomaticaProgramadas(OperacionProgramadaService operaciones) {
        this.operaciones = operaciones;
    }

    @Scheduled(fixedDelayString = "${fintech.programadas.intervalo-ms:60000}",
            initialDelayString = "${fintech.programadas.intervalo-ms:60000}")
    public void procesar() {
        try {
            int ejecutadas = operaciones.procesarVencidas().tamanio();
            if (ejecutadas > 0) {
                LOG.info("Ejecución automática: {} operaciones programadas procesadas", ejecutadas);
            }
        } catch (RuntimeException e) {
            LOG.error("Falló la ejecución automática de operaciones programadas", e);
        }
    }
}
