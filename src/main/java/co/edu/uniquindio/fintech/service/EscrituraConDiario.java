package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.exception.PersistenciaException;
import co.edu.uniquindio.fintech.persistence.CerrojoEscritura;
import co.edu.uniquindio.fintech.persistence.DiarioOperaciones;
import co.edu.uniquindio.fintech.persistence.EntradaDiario;

/**
 * Punto único por el que los servicios modifican datos (CONTEXTO.md 7.6).
 *
 * <p>Garantiza dos reglas que el diario por sí solo no cubre cuando una escritura falla
 * en mitad de la ejecución:
 * <ol>
 *   <li>Si la entrada ya quedó registrada en el diario, la operación está comprometida
 *       (se completará al reintentar o en el próximo arranque), así que la memoria se
 *       actualiza igual y el error se informa. Si ni siquiera se registró, la memoria no
 *       cambia.</li>
 *   <li>Mientras exista una entrada pendiente no se acepta ninguna otra modificación sin
 *       antes reaplicarla. Así nunca se confirma una operación posterior que luego sería
 *       pisada por las imágenes antiguas de la pendiente al recuperar el diario.</li>
 * </ol>
 * Quien llama debe tener el cerrojo (todos los servicios lo toman antes de validar).
 */
public final class EscrituraConDiario {

    private final DiarioOperaciones diario;
    private final ActualizadorMemoria memoria;
    private final CerrojoEscritura cerrojo;

    public EscrituraConDiario(DiarioOperaciones diario, ActualizadorMemoria memoria, CerrojoEscritura cerrojo) {
        this.diario = diario;
        this.memoria = memoria;
        this.cerrojo = cerrojo;
    }

    public CerrojoEscritura cerrojo() {
        return cerrojo;
    }

    /**
     * Reaplica las entradas pendientes del diario (idempotente) antes de una nueva
     * modificación. Si no se pueden aplicar, rechaza la modificación.
     */
    public void asegurarSinPendientes() {
        synchronized (cerrojo) {
            if (diario.pendientes().estaVacia()) {
                return;
            }
            try {
                diario.recuperarPendientes();
            } catch (PersistenciaException e) {
                throw new PersistenciaException("Hay operaciones pendientes en el diario que aún no se pueden "
                        + "escribir; no se aceptan nuevas modificaciones hasta completarlas", e);
            }
        }
    }

    /** Ejecuta una operación de varios registros como una sola entrada del diario. */
    public void ejecutar(String idOperacion, Object[] registros) {
        synchronized (cerrojo) {
            asegurarSinPendientes();
            EntradaDiario entrada = diario.preparar(idOperacion, registros);
            try {
                diario.ejecutar(entrada, () -> memoria.reflejar(registros));
            } catch (PersistenciaException e) {
                if (registrada(idOperacion)) {
                    memoria.reflejar(registros);
                }
                throw e;
            }
        }
    }

    private boolean registrada(String idOperacion) {
        try {
            for (EntradaDiario pendiente : diario.pendientes()) {
                if (pendiente.id().equals(idOperacion)) {
                    return true;
                }
            }
            return false;
        } catch (RuntimeException e) {
            // Si ni el diario se puede leer, se asume registrada: el arranque decidirá.
            return true;
        }
    }
}
