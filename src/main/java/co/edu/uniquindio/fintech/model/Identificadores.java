package co.edu.uniquindio.fintech.model;

/**
 * Formatos de los identificadores generados (CONTEXTO.md 7.4): prefijo de tres letras,
 * guion y consecutivo de al menos 6 dígitos.
 *
 * <p>Las constantes son expresiones regulares y pueden usarse en anotaciones de
 * validación de los DTO. Esta clase no genera identificadores; el generador
 * corresponde a una fase posterior.
 */
public final class Identificadores {

    public static final String PATRON_BILLETERA = "BIL-[0-9]{6,}";
    public static final String PATRON_TRANSACCION = "TRX-[0-9]{6,}";
    public static final String PATRON_OPERACION_PROGRAMADA = "OPR-[0-9]{6,}";
    public static final String PATRON_NOTIFICACION = "NOT-[0-9]{6,}";
    public static final String PATRON_EVENTO_AUDITORIA = "AUD-[0-9]{6,}";
    public static final String PATRON_BENEFICIO_CANJEADO = "CAN-[0-9]{6,}";

    private Identificadores() {
    }
}
