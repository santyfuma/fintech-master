package co.edu.uniquindio.fintech.dto;

import java.math.BigDecimal;

/** Relación dirigida entre dos usuarios del grafo de transferencias. */
public record AristaRespuesta(String origen, String destino, BigDecimal peso) {
}
