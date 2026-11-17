package co.edu.uniquindio.fintech.soporte;

import co.edu.uniquindio.fintech.benchmark.BenchmarkEstructuras;
import co.edu.uniquindio.fintech.config.ContextoDatos;
import co.edu.uniquindio.fintech.dto.SolicitudCreacionBilletera;
import co.edu.uniquindio.fintech.dto.SolicitudRegistroUsuario;
import co.edu.uniquindio.fintech.model.Billetera;
import co.edu.uniquindio.fintech.model.NivelUsuario;
import co.edu.uniquindio.fintech.model.TipoBilletera;
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

import java.math.BigDecimal;
import java.nio.file.Path;

/**
 * Todos los servicios reales cableados como en {@code ConfiguracionNegocio}, sobre un
 * directorio temporal y un reloj ajustable, con las políticas provisionales. Crear otro
 * sistema sobre el mismo directorio equivale a reiniciar la aplicación.
 */
public final class SistemaPrueba {

    public final Path directorio;
    public final RelojAjustable reloj;
    public final String catalogoDefinicion;
    public final EntornoDatos entorno;
    public final ContextoDatos datos;
    public final EscrituraConDiario escritura;
    public final PuntosService puntos;
    public final NotificacionService notificaciones;
    public final DeteccionService deteccion;
    public final UsuarioService usuarios;
    public final BilleteraService billeteras;
    public final TransaccionService transacciones;
    public final OperacionProgramadaService programadas;
    public final RecompensaService recompensas;
    public final AnaliticaService analitica;
    public final GrafoService grafo;
    public final BenchmarkEstructuras benchmark = new BenchmarkEstructuras();

    public SistemaPrueba(Path directorio) {
        this(directorio, new RelojAjustable(), "");
    }

    public SistemaPrueba(Path directorio, RelojAjustable reloj, String catalogo) {
        this.directorio = directorio;
        this.reloj = reloj;
        this.catalogoDefinicion = catalogo;
        this.entorno = new EntornoDatos(directorio, reloj);
        this.datos = entorno.iniciar();
        var f = entorno.fabrica;
        var c = entorno.cerrojo;
        var g = datos.generador();
        ActualizadorMemoria memoria = new ActualizadorMemoria(datos.usuarios(), datos.billeteras(),
                datos.transacciones(), datos.operacionesProgramadas(), datos.notificaciones(), datos.auditoria(),
                datos.beneficios());
        this.escritura = new EscrituraConDiario(entorno.diario, memoria, c);
        PoliticaNotificaciones politicaNotificaciones = PoliticaNotificaciones.provisional();
        this.puntos = new PuntosService(PoliticaPuntos.provisional());
        this.notificaciones = new NotificacionService(datos.notificaciones(), g, escritura, f, reloj);
        this.deteccion = new DeteccionService(PoliticaDeteccion.provisional(), datos.transacciones(), datos.auditoria(), f);
        this.usuarios = new UsuarioService(datos.usuarios(), datos.billeteras(), datos.beneficios(),
                datos.notificaciones(), datos.auditoria(), puntos, escritura);
        this.billeteras = new BilleteraService(datos.billeteras(), usuarios, g, escritura);
        this.transacciones = new TransaccionService(datos.usuarios(), datos.billeteras(), datos.transacciones(),
                g, puntos, deteccion, notificaciones, PoliticaReversion.provisional(reloj),
                politicaNotificaciones, f, escritura, reloj);
        this.programadas = new OperacionProgramadaService(datos.operacionesProgramadas(), datos.billeteras(),
                transacciones, notificaciones, g, PoliticaProgramacion.provisional(), politicaNotificaciones, f, escritura, reloj);
        String[][] sinVentajas = new String[NivelUsuario.values().length][0];
        this.recompensas = new RecompensaService(usuarios, datos.beneficios(), new CatalogoBeneficios(catalogo, f),
                sinVentajas, puntos, notificaciones, g, f, escritura, reloj);
        this.analitica = new AnaliticaService(datos.transacciones(), datos.billeteras(), f);
        this.grafo = new GrafoService(datos.transacciones(), f, CriterioGrafo.CANTIDAD);
    }

    /** Arranca de nuevo sobre el mismo directorio (mismo reloj y catálogo). */
    public SistemaPrueba reiniciar() {
        return new SistemaPrueba(directorio, reloj, catalogoDefinicion);
    }

    // ---------------------------------------------------------------- atajos de escenario

    public void usuario(String identificacion) {
        usuarios.registrar(new SolicitudRegistroUsuario(identificacion, "Usuario " + identificacion,
                identificacion + "@correo.co"));
    }

    /** Crea una billetera y la recarga con el saldo indicado (si es mayor que 0). */
    public String billetera(String propietario, String saldo) {
        Billetera b = billeteras.crear(propietario, new SolicitudCreacionBilletera("Billetera", TipoBilletera.AHORRO));
        if (new BigDecimal(saldo).signum() > 0) {
            transacciones.recargar(b.codigo(), new BigDecimal(saldo));
        }
        return b.codigo();
    }

    public BigDecimal saldo(String codigo) {
        return billeteras.saldo(codigo);
    }

    public int notificaciones(String usuario, co.edu.uniquindio.fintech.model.TipoNotificacion tipo) {
        int n = 0;
        for (co.edu.uniquindio.fintech.model.Notificacion x : notificaciones.porUsuario(usuario)) {
            if (x.tipo() == tipo) {
                n++;
            }
        }
        return n;
    }

    /** Desbloquea un archivo bloqueado con {@link EntornoDatos#bloquearEscritura}. */
    public void desbloquear(String archivo) {
        try {
            java.nio.file.Path ruta = directorio.resolve(archivo);
            java.nio.file.Files.deleteIfExists(ruta.resolve("bloqueo.txt"));
            java.nio.file.Files.deleteIfExists(ruta);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    public long puntosDe(String usuario) {
        return usuarios.buscar(usuario).puntosAcumulados();
    }
}
