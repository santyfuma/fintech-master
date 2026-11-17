/**
 * Contratos (interfaces) propios de las estructuras de datos del proyecto.
 *
 * <p>Reglas del paquete:
 * <ul>
 *   <li>Es genérico e independiente del dominio financiero: no importa ningún
 *       paquete de la aplicación fuera de {@code structures}.</li>
 *   <li>Los servicios y repositorios dependen únicamente de estos contratos,
 *       nunca de las implementaciones de {@code adaptadores} o {@code propias}.</li>
 *   <li>Ninguna estructura acepta elementos, claves ni valores {@code null}.
 *       Las consultas de un elemento inexistente devuelven {@code null}; las
 *       operaciones sobre una estructura vacía lanzan {@link IllegalStateException}
 *       y los índices inválidos {@link IndexOutOfBoundsException}.</li>
 * </ul>
 */
package co.edu.uniquindio.fintech.structures.interfaces;
