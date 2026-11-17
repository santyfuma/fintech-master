package co.edu.uniquindio.fintech.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MontoMovilizadoRespuesta(LocalDateTime desde, LocalDateTime hasta, BigDecimal total, long transacciones) {
}
