/**
 * DTO de petición ({@code Solicitud...}) y de respuesta ({@code ...Respuesta}),
 * todos como {@code record}.
 *
 * <ul>
 *   <li>Las peticiones validan forma con Bean Validation (campos obligatorios, formato
 *       de códigos, montos positivos) y solo contienen lo que el cliente puede decidir:
 *       nunca saldos, puntos, niveles, estados calculados ni identificadores generados.</li>
 *   <li>Las respuestas se construyen desde las entidades con {@code desde(...)}; separan
 *       el contrato de la API del modelo interno.</li>
 *   <li>Las colecciones en respuestas se representan con arreglos, no con listas.</li>
 * </ul>
 * Las reglas de negocio (saldo suficiente, existencia, estado) se validan en {@code service}.
 */
package co.edu.uniquindio.fintech.dto;
