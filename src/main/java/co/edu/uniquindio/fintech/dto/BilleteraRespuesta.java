package co.edu.uniquindio.fintech.dto;

import co.edu.uniquindio.fintech.model.Billetera;
import co.edu.uniquindio.fintech.model.EstadoBilletera;
import co.edu.uniquindio.fintech.model.TipoBilletera;

import java.math.BigDecimal;

/** Datos de una billetera. */
public record BilleteraRespuesta(String codigo, String identificacionPropietario, String nombre, TipoBilletera tipo,
                                 BigDecimal saldo, EstadoBilletera estado) {

    public static BilleteraRespuesta desde(Billetera billetera) {
        return new BilleteraRespuesta(billetera.codigo(), billetera.identificacionPropietario(), billetera.nombre(),
                billetera.tipo(), billetera.saldo(), billetera.estado());
    }
}
