package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.model.NivelUsuario;
import co.edu.uniquindio.fintech.model.TipoTransaccion;
import co.edu.uniquindio.fintech.model.Usuario;

import java.math.BigDecimal;

/**
 * Cálculo de puntos y asignación de niveles (enunciado 4.4 y 4.5).
 *
 * <ul>
 *   <li>Puntos = (valor ÷ unidad, división entera) × factor del tipo, más el bono si es
 *       una operación programada ejecutada con éxito ({@link PoliticaPuntos}).</li>
 *   <li>Los puntos los gana el usuario que realiza la operación: el propietario de la
 *       billetera origen o, en una recarga, de la destino. Quien recibe una
 *       transferencia no gana puntos.</li>
 *   <li>Los puntos nunca quedan negativos: un ajuste que bajaría de 0 se detiene en 0.</li>
 *   <li>El nivel se deduce siempre de los puntos con los rangos de {@link NivelUsuario}.</li>
 * </ul>
 * Es un componente de cálculo: no escribe. Los servicios incluyen el usuario resultante
 * en la operación del diario, lo que actualiza el ABB de puntos del repositorio.
 */
public final class PuntosService {

    private final PoliticaPuntos politica;

    public PuntosService(PoliticaPuntos politica) {
        if (politica == null) {
            throw new NullPointerException("politica");
        }
        this.politica = politica;
    }

    public PoliticaPuntos politica() {
        return politica;
    }

    /** @return puntos que genera un movimiento exitoso */
    public long calcular(TipoTransaccion tipo, BigDecimal valor, boolean programada) {
        long factor = switch (tipo) {
            case RECARGA -> politica.puntosRecarga();
            case RETIRO -> politica.puntosRetiro();
            case TRANSFERENCIA -> politica.puntosTransferencia();
            case REVERSION -> 0;
        };
        if (tipo == TipoTransaccion.REVERSION) {
            return 0;
        }
        BigDecimal unidades = valor.divideToIntegralValue(politica.unidad());
        BigDecimal base = unidades.multiply(BigDecimal.valueOf(factor));
        BigDecimal limite = BigDecimal.valueOf(Long.MAX_VALUE / 2);
        long puntos = base.min(limite).longValue();
        return programada ? puntos + politica.bonoProgramada() : puntos;
    }

    /** @return el nivel que corresponde a los puntos */
    public NivelUsuario nivelPara(long puntos) {
        for (NivelUsuario nivel : NivelUsuario.values()) {
            if (puntos >= nivel.puntosMinimos() && puntos <= nivel.puntosMaximos()) {
                return nivel;
            }
        }
        return NivelUsuario.BRONCE;
    }

    /** @return el usuario con los puntos ajustados (sin bajar de 0) y su nivel recalculado */
    public Usuario ajustar(Usuario usuario, long delta) {
        long actual = usuario.puntosAcumulados();
        long nuevos = delta >= 0 ? (Long.MAX_VALUE - actual < delta ? Long.MAX_VALUE : actual + delta)
                : Math.max(0, actual + delta);
        return usuario.conPuntos(nuevos, nivelPara(nuevos));
    }

    /** @return si el cambio subió de nivel al usuario */
    public boolean asciende(Usuario antes, Usuario despues) {
        return despues.nivel().ordinal() > antes.nivel().ordinal();
    }

    /** @return el nivel siguiente, o null si ya es el máximo */
    public NivelUsuario siguienteNivel(NivelUsuario nivel) {
        NivelUsuario[] niveles = NivelUsuario.values();
        return nivel.ordinal() + 1 < niveles.length ? niveles[nivel.ordinal() + 1] : null;
    }
}
