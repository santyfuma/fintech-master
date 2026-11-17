package co.edu.uniquindio.fintech.config;

import co.edu.uniquindio.fintech.benchmark.BenchmarkEstructuras;
import co.edu.uniquindio.fintech.model.NivelRiesgo;
import co.edu.uniquindio.fintech.model.NivelUsuario;
import co.edu.uniquindio.fintech.model.PatronInusual;
import co.edu.uniquindio.fintech.persistence.CerrojoEscritura;
import co.edu.uniquindio.fintech.persistence.DiarioOperaciones;
import co.edu.uniquindio.fintech.persistence.GeneradorIdentificadores;
import co.edu.uniquindio.fintech.repository.FabricaEstructuras;
import co.edu.uniquindio.fintech.repository.RepositorioAuditoria;
import co.edu.uniquindio.fintech.repository.RepositorioBeneficios;
import co.edu.uniquindio.fintech.repository.RepositorioBilleteras;
import co.edu.uniquindio.fintech.repository.RepositorioNotificaciones;
import co.edu.uniquindio.fintech.repository.RepositorioOperacionesProgramadas;
import co.edu.uniquindio.fintech.repository.RepositorioTransacciones;
import co.edu.uniquindio.fintech.repository.RepositorioUsuarios;
import co.edu.uniquindio.fintech.service.ActualizadorMemoria;
import co.edu.uniquindio.fintech.service.AnaliticaService;
import co.edu.uniquindio.fintech.service.BilleteraService;
import co.edu.uniquindio.fintech.service.CatalogoBeneficios;
import co.edu.uniquindio.fintech.service.CriterioGrafo;
import co.edu.uniquindio.fintech.service.DeteccionService;
import co.edu.uniquindio.fintech.service.EscrituraConDiario;
import co.edu.uniquindio.fintech.service.GrafoService;
import co.edu.uniquindio.fintech.service.NotificacionService;
import co.edu.uniquindio.fintech.service.OperacionProgramadaService;
import co.edu.uniquindio.fintech.service.PoliticaDeteccion;
import co.edu.uniquindio.fintech.service.PoliticaNotificaciones;
import co.edu.uniquindio.fintech.service.PoliticaProgramacion;
import co.edu.uniquindio.fintech.service.PoliticaPuntos;
import co.edu.uniquindio.fintech.service.PoliticaReversion;
import co.edu.uniquindio.fintech.service.PuntosService;
import co.edu.uniquindio.fintech.service.RecompensaService;
import co.edu.uniquindio.fintech.service.TransaccionService;
import co.edu.uniquindio.fintech.service.UsuarioService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;

/**
 * Servicios y políticas de negocio. Todos los valores que el enunciado no fija se leen de
 * {@code application.properties} (prefijo {@code fintech.}) con los valores provisionales
 * documentados en cada política; así ningún valor queda disperso en los servicios.
 */
@Configuration
public class ConfiguracionNegocio {

    // ---------------------------------------------------------------- políticas

    @Bean
    public PoliticaPuntos politicaPuntos(Environment env) {
        PoliticaPuntos p = PoliticaPuntos.provisional();
        return new PoliticaPuntos(
                env.getProperty("fintech.puntos.recarga", Long.class, p.puntosRecarga()),
                env.getProperty("fintech.puntos.retiro", Long.class, p.puntosRetiro()),
                env.getProperty("fintech.puntos.transferencia", Long.class, p.puntosTransferencia()),
                env.getProperty("fintech.puntos.unidad", BigDecimal.class, p.unidad()),
                env.getProperty("fintech.puntos.bono-programada", Long.class, p.bonoProgramada()));
    }

    @Bean
    public PoliticaReversion politicaReversion(Environment env, Clock reloj) {
        return new PoliticaReversion(
                env.getProperty("fintech.reversion.recargas", Boolean.class, true),
                env.getProperty("fintech.reversion.retiros", Boolean.class, false),
                env.getProperty("fintech.reversion.transferencias", Boolean.class, true),
                Duration.ofHours(env.getProperty("fintech.reversion.plazo-horas", Long.class, 24L)), reloj);
    }

    @Bean
    public PoliticaDeteccion politicaDeteccion(Environment env) {
        PoliticaDeteccion p = PoliticaDeteccion.provisional();
        NivelRiesgo[] riesgos = p.riesgos();
        for (PatronInusual patron : PatronInusual.values()) {
            String clave = "fintech.deteccion.riesgo." + patron.name().toLowerCase().replace('_', '-');
            riesgos[patron.ordinal()] = env.getProperty(clave, NivelRiesgo.class, riesgos[patron.ordinal()]);
        }
        return new PoliticaDeteccion(
                minutos(env, "fintech.deteccion.ventana-consecutivas-minutos", p.ventanaConsecutivas()),
                env.getProperty("fintech.deteccion.minimo-consecutivas", Integer.class, p.minimoConsecutivas()),
                env.getProperty("fintech.deteccion.factor-monto-alto", BigDecimal.class, p.factorMontoAlto()),
                env.getProperty("fintech.deteccion.minimo-historial-monto", Integer.class, p.minimoHistorialMonto()),
                minutos(env, "fintech.deteccion.ventana-destino-minutos", p.ventanaDestino()),
                env.getProperty("fintech.deteccion.minimo-destino", Integer.class, p.minimoDestino()),
                minutos(env, "fintech.deteccion.ventana-fragmentacion-minutos", p.ventanaFragmentacion()),
                env.getProperty("fintech.deteccion.minimo-billeteras-origen", Integer.class, p.minimoBilleterasOrigen()),
                env.getProperty("fintech.deteccion.minimo-frecuencia", Integer.class, p.minimoFrecuencia()),
                env.getProperty("fintech.deteccion.factor-frecuencia", Integer.class, p.factorFrecuencia()),
                env.getProperty("fintech.deteccion.minimo-historial-horario", Integer.class, p.minimoHistorialHorario()),
                env.getProperty("fintech.deteccion.tolerancia-horas", Integer.class, p.toleranciaHoras()),
                riesgos);
    }

    @Bean
    public PoliticaNotificaciones politicaNotificaciones(Environment env) {
        PoliticaNotificaciones p = PoliticaNotificaciones.provisional();
        return new PoliticaNotificaciones(
                env.getProperty("fintech.notificaciones.umbral-saldo-bajo", BigDecimal.class, p.umbralSaldoBajo()),
                Duration.ofHours(env.getProperty("fintech.notificaciones.anticipacion-programadas-horas", Long.class,
                        p.anticipacionProgramadas().toHours())));
    }

    @Bean
    public PoliticaProgramacion politicaProgramacion(Environment env) {
        return new PoliticaProgramacion(env.getProperty("fintech.programadas.permitir-retiros", Boolean.class, true));
    }

    @Bean
    public CatalogoBeneficios catalogoBeneficios(Environment env, FabricaEstructuras fabrica) {
        return new CatalogoBeneficios(env.getProperty("fintech.beneficios.catalogo", ""), fabrica);
    }

    // ---------------------------------------------------------------- servicios

    @Bean
    public ActualizadorMemoria actualizadorMemoria(RepositorioUsuarios u, RepositorioBilleteras b,
                                                   RepositorioTransacciones t, RepositorioOperacionesProgramadas o,
                                                   RepositorioNotificaciones n, RepositorioAuditoria a,
                                                   RepositorioBeneficios c) {
        return new ActualizadorMemoria(u, b, t, o, n, a, c);
    }

    @Bean
    public EscrituraConDiario escrituraConDiario(DiarioOperaciones d, ActualizadorMemoria m, CerrojoEscritura c) {
        return new EscrituraConDiario(d, m, c);
    }

    @Bean
    public PuntosService puntosService(PoliticaPuntos politica) {
        return new PuntosService(politica);
    }

    @Bean
    public NotificacionService notificacionService(RepositorioNotificaciones n, GeneradorIdentificadores g,
                                                   EscrituraConDiario e, FabricaEstructuras f, Clock reloj) {
        return new NotificacionService(n, g, e, f, reloj);
    }

    @Bean
    public DeteccionService deteccionService(PoliticaDeteccion p, RepositorioTransacciones t, RepositorioAuditoria a,
                                             FabricaEstructuras f) {
        return new DeteccionService(p, t, a, f);
    }

    @Bean
    public UsuarioService usuarioService(RepositorioUsuarios u, RepositorioBilleteras b, RepositorioBeneficios c,
                                         RepositorioNotificaciones n, RepositorioAuditoria a, PuntosService p,
                                         EscrituraConDiario e) {
        return new UsuarioService(u, b, c, n, a, p, e);
    }

    @Bean
    public BilleteraService billeteraService(RepositorioBilleteras b, UsuarioService u, GeneradorIdentificadores g,
                                             EscrituraConDiario e) {
        return new BilleteraService(b, u, g, e);
    }

    @Bean
    public TransaccionService transaccionService(RepositorioUsuarios u, RepositorioBilleteras b,
                                                 RepositorioTransacciones t, GeneradorIdentificadores g, PuntosService p, DeteccionService det,
                                                 NotificacionService n, PoliticaReversion pr,
                                                 PoliticaNotificaciones pn, FabricaEstructuras f, EscrituraConDiario e,
                                                 Clock reloj) {
        return new TransaccionService(u, b, t, g, p, det, n, pr, pn, f, e, reloj);
    }

    @Bean
    public OperacionProgramadaService operacionProgramadaService(RepositorioOperacionesProgramadas o,
                                                                 RepositorioBilleteras b, TransaccionService t,
                                                                 NotificacionService n, GeneradorIdentificadores g,
                                                                 PoliticaProgramacion pp, PoliticaNotificaciones pn,
                                                                 FabricaEstructuras f, EscrituraConDiario e, Clock reloj) {
        return new OperacionProgramadaService(o, b, t, n, g, pp, pn, f, e, reloj);
    }

    @Bean
    public RecompensaService recompensaService(Environment env, UsuarioService u, RepositorioBeneficios b,
                                               CatalogoBeneficios catalogo, PuntosService p, NotificacionService n,
                                               GeneradorIdentificadores g, FabricaEstructuras f, EscrituraConDiario e,
                                               Clock reloj) {
        String[][] ventajas = new String[NivelUsuario.values().length][];
        for (NivelUsuario nivel : NivelUsuario.values()) {
            String valor = env.getProperty("fintech.niveles.ventajas." + nivel.name().toLowerCase(), "");
            ventajas[nivel.ordinal()] = valor.isBlank() ? new String[0] : valor.split(";");
        }
        return new RecompensaService(u, b, catalogo, ventajas, p, n, g, f, e, reloj);
    }

    @Bean
    public AnaliticaService analiticaService(RepositorioTransacciones t, RepositorioBilleteras b, FabricaEstructuras f) {
        return new AnaliticaService(t, b, f);
    }

    @Bean
    public GrafoService grafoService(Environment env, RepositorioTransacciones t, FabricaEstructuras f) {
        return new GrafoService(t, f, env.getProperty("fintech.grafo.peso-por-defecto", CriterioGrafo.class,
                CriterioGrafo.CANTIDAD));
    }

    @Bean
    public BenchmarkEstructuras benchmarkEstructuras() {
        return new BenchmarkEstructuras();
    }

    private static Duration minutos(Environment env, String clave, Duration porDefecto) {
        return Duration.ofMinutes(env.getProperty(clave, Long.class, porDefecto.toMinutes()));
    }
}
