/**
 * Reglas de negocio. Es la única capa que calcula saldos, puntos, niveles, riesgos o
 * decide si una operación procede.
 *
 * <p>Todas las operaciones que escriben se ejecutan bajo el {@code CerrojoEscritura}
 * compartido (generar identificadores, calcular y escribir sin intercalarse), y las que
 * modifican más de un registro pasan por el diario de operaciones.
 *
 * <p>Los valores que el enunciado no fija están centralizados en las políticas
 * ({@code PoliticaPuntos}, {@code PoliticaReversion}, {@code PoliticaDeteccion},
 * {@code PoliticaNotificaciones}, {@code PoliticaProgramacion}, {@code CatalogoBeneficios},
 * {@code CriterioGrafo}), que se configuran en {@code application.properties}.
 */
package co.edu.uniquindio.fintech.service;
