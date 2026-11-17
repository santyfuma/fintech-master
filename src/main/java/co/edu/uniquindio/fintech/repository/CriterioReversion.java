package co.edu.uniquindio.fintech.repository;

import co.edu.uniquindio.fintech.model.Transaccion;

/**
 * Política que decide si una transacción exitosa puede revertirse (tipos reversibles y
 * plazo). <b>Pendiente</b> (CONTEXTO.md, sección 11): la define la capa de servicios en
 * la fase 6; el repositorio solo la recibe al reconstruir la pila.
 */
@FunctionalInterface
public interface CriterioReversion {

    boolean esReversible(Transaccion transaccion);
}
