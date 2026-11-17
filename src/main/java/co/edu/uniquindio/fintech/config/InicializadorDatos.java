package co.edu.uniquindio.fintech.config;

import co.edu.uniquindio.fintech.persistence.AlmacenesJson;
import co.edu.uniquindio.fintech.persistence.CerrojoEscritura;
import co.edu.uniquindio.fintech.persistence.DatosCargados;
import co.edu.uniquindio.fintech.persistence.DiarioOperaciones;
import co.edu.uniquindio.fintech.persistence.GeneradorIdentificadores;
import co.edu.uniquindio.fintech.persistence.VerificadorIntegridad;
import co.edu.uniquindio.fintech.repository.FabricaEstructuras;
import co.edu.uniquindio.fintech.repository.RepositorioAuditoria;
import co.edu.uniquindio.fintech.repository.RepositorioBeneficios;
import co.edu.uniquindio.fintech.repository.RepositorioBilleteras;
import co.edu.uniquindio.fintech.repository.RepositorioNotificaciones;
import co.edu.uniquindio.fintech.repository.RepositorioOperacionesProgramadas;
import co.edu.uniquindio.fintech.repository.RepositorioTransacciones;
import co.edu.uniquindio.fintech.repository.RepositorioUsuarios;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordina el arranque de los datos en el orden de CONTEXTO.md 7.6:
 * <ol>
 *   <li>Borrar temporales de escrituras interrumpidas (descartables).</li>
 *   <li>Recuperar el diario: rehacer y retirar las entradas PREPARADAS.</li>
 *   <li>Cargar los siete archivos JSON.</li>
 *   <li>Verificar registros, identificadores y referencias.</li>
 *   <li>Recuperar los consecutivos.</li>
 *   <li>Construir los repositorios con sus índices (tablas hash, listas por usuario y
 *       billetera, ABB, cola de prioridad y cola FIFO).</li>
 * </ol>
 * Si cualquier paso falla se lanza la excepción y no se crea ningún repositorio: Spring
 * no arranca con datos parcialmente recuperados.
 *
 * <p>La pila de reversibles y el grafo de transferencias no se construyen aquí: sus
 * reglas están pendientes y los repositorios ofrecen su reconstrucción a pedido.
 */
public class InicializadorDatos {

    private static final Logger LOG = LoggerFactory.getLogger(InicializadorDatos.class);

    private final AlmacenesJson almacenes;
    private final DiarioOperaciones diario;
    private final FabricaEstructuras fabrica;
    private final CerrojoEscritura cerrojo;
    private final int limiteNotificacionesRecientes;

    public InicializadorDatos(AlmacenesJson almacenes, DiarioOperaciones diario, FabricaEstructuras fabrica,
                              CerrojoEscritura cerrojo, int limiteNotificacionesRecientes) {
        if (almacenes == null || diario == null || fabrica == null || cerrojo == null) {
            throw new NullPointerException("almacenes, diario, fabrica y cerrojo son obligatorios");
        }
        this.almacenes = almacenes;
        this.diario = diario;
        this.fabrica = fabrica;
        this.cerrojo = cerrojo;
        this.limiteNotificacionesRecientes = limiteNotificacionesRecientes;
    }

    public ContextoDatos inicializar() {
        synchronized (cerrojo) {
            LOG.info("Directorio de datos: {}", almacenes.directorio().toAbsolutePath());
            int temporales = almacenes.limpiarTemporales();
            int recuperadas = diario.recuperarPendientes();
            DatosCargados datos = DatosCargados.cargar(almacenes);
            VerificadorIntegridad.verificar(datos);
            GeneradorIdentificadores generador = GeneradorIdentificadores.desde(datos);

            RepositorioUsuarios usuarios = new RepositorioUsuarios(almacenes.usuarios(), fabrica, cerrojo,
                    datos.usuarios());
            RepositorioBilleteras billeteras = new RepositorioBilleteras(almacenes.billeteras(), fabrica, cerrojo,
                    datos.billeteras());
            RepositorioTransacciones transacciones = new RepositorioTransacciones(almacenes.transacciones(), fabrica,
                    cerrojo, billeteras, datos.transacciones());
            RepositorioOperacionesProgramadas operaciones = new RepositorioOperacionesProgramadas(
                    almacenes.operacionesProgramadas(), fabrica, cerrojo, billeteras, datos.operacionesProgramadas());
            RepositorioNotificaciones notificaciones = new RepositorioNotificaciones(almacenes.notificaciones(),
                    fabrica, cerrojo, datos.notificaciones(), limiteNotificacionesRecientes);
            RepositorioAuditoria auditoria = new RepositorioAuditoria(almacenes.auditoria(), fabrica, cerrojo,
                    transacciones, datos.auditoria());
            RepositorioBeneficios beneficios = new RepositorioBeneficios(almacenes.beneficios(), fabrica, cerrojo,
                    datos.beneficios());

            LOG.info("Datos cargados: {} usuarios, {} billeteras, {} transacciones, {} operaciones programadas, "
                            + "{} notificaciones, {} eventos de auditoría, {} canjes; {} operaciones recuperadas del "
                            + "diario, {} temporales descartados",
                    usuarios.cantidad(), billeteras.cantidad(), transacciones.cantidad(), operaciones.cantidad(),
                    notificaciones.cantidad(), auditoria.cantidad(), beneficios.cantidad(), recuperadas, temporales);
            return new ContextoDatos(usuarios, billeteras, transacciones, operaciones, notificaciones, auditoria,
                    beneficios, generador, recuperadas);
        }
    }
}
