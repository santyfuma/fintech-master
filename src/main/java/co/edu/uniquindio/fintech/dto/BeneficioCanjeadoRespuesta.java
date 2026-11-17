package co.edu.uniquindio.fintech.dto;

import co.edu.uniquindio.fintech.model.BeneficioCanjeado;
import co.edu.uniquindio.fintech.model.EstadoCanje;

import java.time.LocalDateTime;

public record BeneficioCanjeadoRespuesta(String id, String identificacionUsuario, String beneficio,
                                         long puntosDescontados, LocalDateTime fecha, EstadoCanje estado) {

    public static BeneficioCanjeadoRespuesta desde(BeneficioCanjeado b) {
        return new BeneficioCanjeadoRespuesta(b.id(), b.identificacionUsuario(), b.beneficio(), b.puntosDescontados(),
                b.fecha(), b.estado());
    }
}
