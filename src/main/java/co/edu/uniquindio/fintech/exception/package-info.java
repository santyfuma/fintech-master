/**
 * Excepciones propias y manejador global de errores REST.
 *
 * <p>Cuatro categorías, cada una asociada a un código HTTP por el manejador:
 * {@link co.edu.uniquindio.fintech.exception.RecursoNoEncontradoException} (404),
 * {@link co.edu.uniquindio.fintech.exception.ConflictoNegocioException} (409),
 * {@link co.edu.uniquindio.fintech.exception.SolicitudInvalidaException} (400) y
 * {@link co.edu.uniquindio.fintech.exception.PersistenciaException} (500).
 * Cada excepción lleva un código de error en mayúsculas (p. ej. {@code SALDO_INSUFICIENTE})
 * que se devuelve en el campo {@code error} de la respuesta.
 */
package co.edu.uniquindio.fintech.exception;
