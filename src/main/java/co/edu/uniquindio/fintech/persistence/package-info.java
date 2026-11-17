/**
 * Persistencia en archivos JSON independientes (uno por tipo de registro) y
 * recuperación de operaciones interrumpidas.
 *
 * <ul>
 *   <li>{@link co.edu.uniquindio.fintech.persistence.AlmacenJsonArchivo}: implementación
 *       única y genérica de {@code AlmacenJson} (arreglo JSON por archivo).</li>
 *   <li>{@link co.edu.uniquindio.fintech.persistence.EscrituraAtomica}: archivo temporal en
 *       el mismo directorio + reemplazo atómico.</li>
 *   <li>{@link co.edu.uniquindio.fintech.persistence.AlmacenesJson}: los siete almacenes de
 *       dominio y el del diario, sobre un mismo directorio.</li>
 *   <li>{@link co.edu.uniquindio.fintech.persistence.DiarioOperaciones}: diario de rehacer
 *       (CONTEXTO.md 7.6).</li>
 *   <li>{@link co.edu.uniquindio.fintech.persistence.GeneradorIdentificadores}: consecutivos
 *       por prefijo (CONTEXTO.md 7.4).</li>
 * </ul>
 *
 * <p>Responsabilidad: convertir entre archivo JSON y registros. No contiene reglas de
 * negocio ni índices; los índices en memoria los construyen los repositorios.
 */
package co.edu.uniquindio.fintech.persistence;
