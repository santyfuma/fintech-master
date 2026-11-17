package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.model.NivelRiesgo;
import co.edu.uniquindio.fintech.model.PatronInusual;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Umbrales de la detección de comportamiento inusual (enunciado 4.9). El enunciado
 * enumera los seis patrones pero <b>no fija ningún umbral</b>: todos estos valores son
 * <b>provisionales</b>, configurables y pendientes de confirmación.
 *
 * @param ventanaConsecutivas      ventana de "transferencias consecutivas en poco tiempo"
 * @param minimoConsecutivas       transferencias (incluida la actual) que activan el patrón
 * @param factorMontoAlto          veces el promedio del usuario que se considera "inusualmente alto"
 * @param minimoHistorialMonto     transacciones previas necesarias para tener un promedio
 * @param ventanaDestino           ventana de "movimientos repetitivos hacia un mismo destino"
 * @param minimoDestino            envíos al mismo destino (incluido el actual) que activan el patrón
 * @param ventanaFragmentacion     ventana de "uso simultáneo de varias billeteras"
 * @param minimoBilleterasOrigen   billeteras origen distintas hacia un mismo destino
 * @param minimoFrecuencia         transacciones en 24 h necesarias para evaluar el cambio de frecuencia
 * @param factorFrecuencia         veces el promedio diario de los 7 días anteriores
 * @param minimoHistorialHorario   transacciones previas necesarias para conocer el horario habitual
 * @param toleranciaHoras          horas de diferencia que aún se consideran habituales
 * @param riesgos                  nivel de riesgo por patrón, en el orden de {@link PatronInusual}
 */
public record PoliticaDeteccion(Duration ventanaConsecutivas, int minimoConsecutivas, BigDecimal factorMontoAlto,
                                int minimoHistorialMonto, Duration ventanaDestino, int minimoDestino,
                                Duration ventanaFragmentacion, int minimoBilleterasOrigen, int minimoFrecuencia,
                                int factorFrecuencia, int minimoHistorialHorario, int toleranciaHoras,
                                NivelRiesgo[] riesgos) {

    public PoliticaDeteccion {
        if (riesgos == null || riesgos.length != PatronInusual.values().length) {
            throw new IllegalArgumentException("Debe indicarse un nivel de riesgo por cada patrón");
        }
        for (NivelRiesgo riesgo : riesgos) {
            if (riesgo == null || riesgo == NivelRiesgo.NINGUNO) {
                throw new IllegalArgumentException("Un patrón detectado debe tener riesgo BAJO, MEDIO o ALTO");
            }
        }
        riesgos = riesgos.clone();
    }

    public NivelRiesgo riesgo(PatronInusual patron) {
        return riesgos[patron.ordinal()];
    }

    @Override
    public NivelRiesgo[] riesgos() {
        return riesgos.clone();
    }

    /** Valores provisionales. */
    public static PoliticaDeteccion provisional() {
        return new PoliticaDeteccion(Duration.ofMinutes(5), 3, new BigDecimal("3"), 3, Duration.ofMinutes(60), 3,
                Duration.ofMinutes(10), 2, 5, 3, 5, 1,
                new NivelRiesgo[]{NivelRiesgo.MEDIO, NivelRiesgo.ALTO, NivelRiesgo.MEDIO, NivelRiesgo.ALTO,
                        NivelRiesgo.MEDIO, NivelRiesgo.BAJO});
    }
}
