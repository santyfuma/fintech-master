package co.edu.uniquindio.fintech.persistence;

import co.edu.uniquindio.fintech.exception.PersistenciaException;
import co.edu.uniquindio.fintech.model.BeneficioCanjeado;
import co.edu.uniquindio.fintech.model.Billetera;
import co.edu.uniquindio.fintech.model.EventoAuditoria;
import co.edu.uniquindio.fintech.model.Notificacion;
import co.edu.uniquindio.fintech.model.OperacionProgramada;
import co.edu.uniquindio.fintech.model.Transaccion;

/**
 * Genera identificadores {@code PREFIJO-NNNNNN} con un consecutivo independiente por
 * prefijo (CONTEXTO.md 7.4).
 *
 * <ul>
 *   <li>Sin registros, el primero es {@code PREFIJO-000001}.</li>
 *   <li>Al iniciar se recupera el máximo consecutivo existente en cada archivo
 *       ({@link #desde}); no hay archivo de contadores.</li>
 *   <li>Nunca reutiliza un número: solo avanza, y los registros con identificador
 *       generado no se borran físicamente.</li>
 *   <li>Con más de seis dígitos el ancho crece ({@code BIL-1000000}).</li>
 *   <li>Es seguro entre hilos: {@link #siguiente} es {@code synchronized}.</li>
 * </ul>
 */
public final class GeneradorIdentificadores {

    private final long[] ultimos = new long[TipoIdentificador.values().length];

    /** Recupera los consecutivos a partir de los registros ya cargados (después del diario). */
    public static GeneradorIdentificadores desde(DatosCargados datos) {
        GeneradorIdentificadores generador = new GeneradorIdentificadores();
        for (Billetera b : datos.billeteras()) {
            generador.registrarExistente(TipoIdentificador.BILLETERA, b.codigo());
        }
        for (Transaccion t : datos.transacciones()) {
            generador.registrarExistente(TipoIdentificador.TRANSACCION, t.id());
        }
        for (OperacionProgramada o : datos.operacionesProgramadas()) {
            generador.registrarExistente(TipoIdentificador.OPERACION_PROGRAMADA, o.id());
        }
        for (Notificacion n : datos.notificaciones()) {
            generador.registrarExistente(TipoIdentificador.NOTIFICACION, n.id());
        }
        for (EventoAuditoria e : datos.auditoria()) {
            generador.registrarExistente(TipoIdentificador.EVENTO_AUDITORIA, e.id());
        }
        for (BeneficioCanjeado c : datos.beneficios()) {
            generador.registrarExistente(TipoIdentificador.BENEFICIO_CANJEADO, c.id());
        }
        return generador;
    }

    /** @return el siguiente identificador del tipo, p. ej. {@code TRX-000043} */
    public synchronized String siguiente(TipoIdentificador tipo) {
        int i = tipo.ordinal();
        ultimos[i] = Math.addExact(ultimos[i], 1);
        return formatear(tipo, ultimos[i]);
    }

    /**
     * Informa un identificador existente para que el generador no lo repita.
     *
     * @throws PersistenciaException si el identificador no tiene el formato del tipo
     */
    public synchronized void registrarExistente(TipoIdentificador tipo, String identificador) {
        long consecutivo = consecutivo(tipo, identificador);
        int i = tipo.ordinal();
        if (consecutivo > ultimos[i]) {
            ultimos[i] = consecutivo;
        }
    }

    /** @return último consecutivo emitido o recuperado (0 si no hay ninguno) */
    public synchronized long ultimoConsecutivo(TipoIdentificador tipo) {
        return ultimos[tipo.ordinal()];
    }

    /** @return {@code PREFIJO-} seguido del consecutivo con al menos 6 dígitos */
    public static String formatear(TipoIdentificador tipo, long consecutivo) {
        if (consecutivo < 1) {
            throw new IllegalArgumentException("El consecutivo debe ser al menos 1: " + consecutivo);
        }
        // Relleno manual: String.format dependería del Locale (y Locale es de java.util).
        StringBuilder digitos = new StringBuilder(Long.toString(consecutivo));
        while (digitos.length() < 6) {
            digitos.insert(0, '0');
        }
        return tipo.prefijo() + "-" + digitos;
    }

    /**
     * @return la parte numérica del identificador
     * @throws PersistenciaException si no tiene el formato del tipo o el consecutivo es 0
     */
    public static long consecutivo(TipoIdentificador tipo, String identificador) {
        if (identificador == null || !identificador.matches(tipo.patron())) {
            throw new PersistenciaException("Identificador con formato inválido para " + tipo.prefijo() + ": "
                    + identificador);
        }
        long numero;
        try {
            numero = Long.parseLong(identificador.substring(tipo.prefijo().length() + 1));
        } catch (NumberFormatException e) {
            throw new PersistenciaException("Consecutivo fuera de rango en " + identificador, e);
        }
        if (numero < 1) {
            throw new PersistenciaException("El consecutivo debe empezar en 1: " + identificador);
        }
        return numero;
    }
}
