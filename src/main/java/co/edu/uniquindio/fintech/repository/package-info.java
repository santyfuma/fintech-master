/**
 * Repositorios en memoria sobre las estructuras propias, con persistencia delegada a
 * {@code persistence}.
 *
 * <p>Cada repositorio indexa sus registros en una tabla hash por identificador y en los
 * índices secundarios que necesita (listas por usuario o billetera, ABB, colas). Los
 * índices guardan identificadores, no copias, así que nunca quedan versiones viejas.
 * Las estructuras se obtienen de {@link co.edu.uniquindio.fintech.repository.FabricaEstructuras}
 * (regla 7: el repositorio no conoce implementaciones concretas). No hay reglas de
 * negocio: validar saldos, permisos o estados es tarea de {@code service}.
 */
package co.edu.uniquindio.fintech.repository;
