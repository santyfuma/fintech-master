package co.edu.uniquindio.fintech.dto;

/** Ciclos (cada uno empieza y termina en el usuario) con a lo sumo {@code longitudMaxima} aristas. */
public record CiclosRespuesta(String usuario, int longitudMaxima, String[][] ciclos) {
}
