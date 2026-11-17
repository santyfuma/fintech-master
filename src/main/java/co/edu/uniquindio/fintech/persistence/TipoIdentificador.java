package co.edu.uniquindio.fintech.persistence;

import co.edu.uniquindio.fintech.model.Identificadores;

/**
 * Tipos de identificador generado, con su prefijo y el patrón de
 * {@link Identificadores}. Los usuarios no están: su clave la proporcionan ellos.
 */
public enum TipoIdentificador {
    BILLETERA("BIL", Identificadores.PATRON_BILLETERA),
    TRANSACCION("TRX", Identificadores.PATRON_TRANSACCION),
    OPERACION_PROGRAMADA("OPR", Identificadores.PATRON_OPERACION_PROGRAMADA),
    NOTIFICACION("NOT", Identificadores.PATRON_NOTIFICACION),
    EVENTO_AUDITORIA("AUD", Identificadores.PATRON_EVENTO_AUDITORIA),
    BENEFICIO_CANJEADO("CAN", Identificadores.PATRON_BENEFICIO_CANJEADO);

    private final String prefijo;
    private final String patron;

    TipoIdentificador(String prefijo, String patron) {
        this.prefijo = prefijo;
        this.patron = patron;
    }

    public String prefijo() {
        return prefijo;
    }

    public String patron() {
        return patron;
    }
}
