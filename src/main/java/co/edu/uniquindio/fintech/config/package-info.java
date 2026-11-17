/**
 * Configuración e inicialización.
 *
 * <ul>
 *   <li>{@link co.edu.uniquindio.fintech.config.ConfiguracionReloj}: el {@code Clock}.</li>
 *   <li>{@link co.edu.uniquindio.fintech.config.ConfiguracionPersistencia}: directorio de
 *       datos, almacenes JSON, diario, fábrica de estructuras y repositorios.</li>
 *   <li>{@link co.edu.uniquindio.fintech.config.InicializadorDatos}: orden de arranque de
 *       los datos (diario → carga → verificación → consecutivos → repositorios).</li>
 *   <li>{@link co.edu.uniquindio.fintech.config.FabricaEstructurasPropias}: único lugar,
 *       junto con {@code persistence}, que instancia implementaciones concretas de
 *       {@code structures}.</li>
 * </ul>
 * No se cargan datos de ejemplo.
 */
package co.edu.uniquindio.fintech.config;
