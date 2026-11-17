package co.edu.uniquindio.fintech.benchmark;

/**
 * Generador lineal congruencial con semilla fija: produce los mismos datos en cada
 * repetición y para cada estructura (no se usa java.util.Random, que está restringido
 * al paquete structures).
 */
final class GeneradorPseudoaleatorio {

    private long estado;

    GeneradorPseudoaleatorio(long semilla) {
        this.estado = semilla;
    }

    int siguiente(int limite) {
        estado = estado * 6364136223846793005L + 1442695040888963407L;
        return (int) Long.remainderUnsigned(estado >>> 16, limite);
    }
}
