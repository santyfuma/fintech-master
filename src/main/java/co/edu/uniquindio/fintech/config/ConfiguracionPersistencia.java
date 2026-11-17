package co.edu.uniquindio.fintech.config;

import co.edu.uniquindio.fintech.persistence.AlmacenesJson;
import co.edu.uniquindio.fintech.persistence.CerrojoEscritura;
import co.edu.uniquindio.fintech.persistence.DiarioOperaciones;
import co.edu.uniquindio.fintech.persistence.DiarioOperacionesJson;
import co.edu.uniquindio.fintech.persistence.GeneradorIdentificadores;
import co.edu.uniquindio.fintech.repository.FabricaEstructuras;
import co.edu.uniquindio.fintech.repository.RepositorioAuditoria;
import co.edu.uniquindio.fintech.repository.RepositorioBeneficios;
import co.edu.uniquindio.fintech.repository.RepositorioBilleteras;
import co.edu.uniquindio.fintech.repository.RepositorioNotificaciones;
import co.edu.uniquindio.fintech.repository.RepositorioOperacionesProgramadas;
import co.edu.uniquindio.fintech.repository.RepositorioTransacciones;
import co.edu.uniquindio.fintech.repository.RepositorioUsuarios;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.time.Clock;

/**
 * Beans de persistencia y repositorios.
 *
 * <p>Propiedades:
 * <ul>
 *   <li>{@code fintech.datos.directorio} (por defecto {@code data}, relativo al
 *       directorio de trabajo): carpeta de los archivos JSON.</li>
 *   <li>{@code fintech.notificaciones.recientes-por-usuario} (por defecto 0 = sin límite):
 *       tamaño de la consulta de notificaciones recientes; valor definitivo pendiente.</li>
 * </ul>
 *
 * <p>Los repositorios se exponen desde {@link ContextoDatos}, que solo existe después de
 * que {@link InicializadorDatos} terminó el arranque: Spring no puede inyectar un
 * repositorio sin inicializar, y si la inicialización falla la aplicación no arranca.
 */
@Configuration
public class ConfiguracionPersistencia {

    @Bean
    public CerrojoEscritura cerrojoEscritura() {
        return new CerrojoEscritura();
    }

    @Bean
    public AlmacenesJson almacenesJson(@Value("${fintech.datos.directorio:data}") String directorio) {
        if (directorio == null || directorio.isBlank()) {
            throw new IllegalArgumentException("La propiedad fintech.datos.directorio no puede estar vacía");
        }
        return new AlmacenesJson(Path.of(directorio));
    }

    @Bean
    public DiarioOperaciones diarioOperaciones(AlmacenesJson almacenes, CerrojoEscritura cerrojo, Clock reloj) {
        return new DiarioOperacionesJson(almacenes, cerrojo, reloj);
    }

    @Bean
    public FabricaEstructuras fabricaEstructuras() {
        return new FabricaEstructurasPropias();
    }

    @Bean
    public InicializadorDatos inicializadorDatos(AlmacenesJson almacenes, DiarioOperaciones diario,
                                                 FabricaEstructuras fabrica, CerrojoEscritura cerrojo,
                                                 @Value("${fintech.notificaciones.recientes-por-usuario:0}")
                                                 int limiteNotificacionesRecientes) {
        return new InicializadorDatos(almacenes, diario, fabrica, cerrojo, limiteNotificacionesRecientes);
    }

    @Bean
    public ContextoDatos contextoDatos(InicializadorDatos inicializador) {
        return inicializador.inicializar();
    }

    @Bean
    public GeneradorIdentificadores generadorIdentificadores(ContextoDatos contexto) {
        return contexto.generador();
    }

    @Bean
    public RepositorioUsuarios repositorioUsuarios(ContextoDatos contexto) {
        return contexto.usuarios();
    }

    @Bean
    public RepositorioBilleteras repositorioBilleteras(ContextoDatos contexto) {
        return contexto.billeteras();
    }

    @Bean
    public RepositorioTransacciones repositorioTransacciones(ContextoDatos contexto) {
        return contexto.transacciones();
    }

    @Bean
    public RepositorioOperacionesProgramadas repositorioOperacionesProgramadas(ContextoDatos contexto) {
        return contexto.operacionesProgramadas();
    }

    @Bean
    public RepositorioNotificaciones repositorioNotificaciones(ContextoDatos contexto) {
        return contexto.notificaciones();
    }

    @Bean
    public RepositorioAuditoria repositorioAuditoria(ContextoDatos contexto) {
        return contexto.auditoria();
    }

    @Bean
    public RepositorioBeneficios repositorioBeneficios(ContextoDatos contexto) {
        return contexto.beneficios();
    }
}
