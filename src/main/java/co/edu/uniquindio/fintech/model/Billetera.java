package co.edu.uniquindio.fintech.model;

import java.math.BigDecimal;

/**
 * Billetera digital de un usuario.
 *
 * <p>Se relaciona con su propietario por {@code identificacionPropietario}. Nunca se
 * borra físicamente: desactivarla es cambiar {@code estado} a INACTIVA.
 *
 * <p>Inmutable: los cambios de saldo, estado o datos producen copias mediante los
 * métodos {@code con...}. Las validaciones de saldo (insuficiente, negativo) son
 * reglas de negocio de {@code service}.
 *
 * @param codigo                    código {@code BIL-NNNNNN}
 * @param identificacionPropietario identificación del usuario propietario
 * @param nombre                    nombre dado por el usuario
 * @param tipo                      propósito de la billetera
 * @param saldo                     saldo disponible
 * @param estado                    estado de la billetera
 */
public record Billetera(String codigo, String identificacionPropietario, String nombre, TipoBilletera tipo,
                        BigDecimal saldo, EstadoBilletera estado) {

    public Billetera {
        Validaciones.identificador(codigo, Identificadores.PATRON_BILLETERA, "codigo");
        Validaciones.texto(identificacionPropietario, "identificacionPropietario");
        Validaciones.texto(nombre, "nombre");
        Validaciones.requerido(tipo, "tipo");
        Validaciones.requerido(saldo, "saldo");
        Validaciones.requerido(estado, "estado");
    }

    /** @return copia con nombre y tipo nuevos */
    public Billetera conDatos(String nuevoNombre, TipoBilletera nuevoTipo) {
        return new Billetera(codigo, identificacionPropietario, nuevoNombre, nuevoTipo, saldo, estado);
    }

    /** @return copia con el saldo indicado (calculado por service) */
    public Billetera conSaldo(BigDecimal nuevoSaldo) {
        return new Billetera(codigo, identificacionPropietario, nombre, tipo, nuevoSaldo, estado);
    }

    /** @return copia con el estado indicado */
    public Billetera conEstado(EstadoBilletera nuevoEstado) {
        return new Billetera(codigo, identificacionPropietario, nombre, tipo, saldo, nuevoEstado);
    }
}
