package co.edu.uniquindio.fintech.benchmark;

/** Comparaciones disponibles; cada una mide la misma operación sobre los mismos datos. */
public enum ComparacionBenchmark {
    /** Buscar n claves presentes y n ausentes: tabla hash frente a ABB. */
    BUSQUEDA,
    /** Insertar n valores y obtenerlos ordenados: lista ordenada por inserción frente a ABB + inorden. */
    CONSULTA_ORDENADA,
    /** Insertar n prioridades y extraerlas en orden: cola de prioridad (montículo) frente a lista con búsqueda lineal del mínimo. */
    PRIORIDAD,
    /** Recorrer los vértices alcanzables de un grafo aleatorio: BFS frente a DFS. */
    RECORRIDO_GRAFO
}
