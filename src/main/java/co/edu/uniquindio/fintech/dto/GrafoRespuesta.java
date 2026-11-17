package co.edu.uniquindio.fintech.dto;

/** Grafo de transferencias entre usuarios con el criterio de peso usado (CANTIDAD o MONTO). */
public record GrafoRespuesta(String criterio, String[] vertices, AristaRespuesta[] aristas, boolean tieneCiclo) {
}
