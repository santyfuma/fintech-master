package co.edu.uniquindio.fintech.persistence;

/**
 * Monitor único que serializa los accesos a los datos: lo comparten el diario de
 * operaciones y todos los repositorios ({@code synchronized} sobre esta instancia,
 * que es reentrante). Así ninguna escritura de un repositorio puede intercalarse entre
 * la aplicación de una entrada del diario y la actualización de la memoria, y dos
 * operaciones simultáneas no pueden sobrescribir el diario.
 */
public final class CerrojoEscritura {
}
