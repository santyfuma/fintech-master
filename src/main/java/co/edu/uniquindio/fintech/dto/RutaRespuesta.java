package co.edu.uniquindio.fintech.dto;

import java.math.BigDecimal;

/** Rutas entre dos usuarios; arreglos vacíos y peso null si no existe ruta. */
public record RutaRespuesta(String origen, String destino, String criterio, String[] menosSaltos, String[] menorPeso,
                            BigDecimal pesoMenor) {
}
