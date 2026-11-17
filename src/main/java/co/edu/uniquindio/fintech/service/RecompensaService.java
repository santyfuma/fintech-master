package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.exception.ConflictoNegocioException;
import co.edu.uniquindio.fintech.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.fintech.model.BeneficioCanjeado;
import co.edu.uniquindio.fintech.model.EstadoCanje;
import co.edu.uniquindio.fintech.model.NivelUsuario;
import co.edu.uniquindio.fintech.model.TipoNotificacion;
import co.edu.uniquindio.fintech.model.Usuario;
import co.edu.uniquindio.fintech.persistence.CerrojoEscritura;
import co.edu.uniquindio.fintech.persistence.GeneradorIdentificadores;
import co.edu.uniquindio.fintech.persistence.TipoIdentificador;
import co.edu.uniquindio.fintech.repository.FabricaEstructuras;
import co.edu.uniquindio.fintech.repository.RepositorioBeneficios;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Recompensas (enunciado 4.4, 4.5; RF7): niveles, ventajas, catálogo y canje de puntos.
 * El canje descuenta los puntos, recalcula el nivel, registra el beneficio canjeado y
 * notifica CANJE_REALIZADO, todo en una sola entrada del diario.
 */
public final class RecompensaService {

    private final UsuarioService usuarios;
    private final RepositorioBeneficios beneficios;
    private final CatalogoBeneficios catalogo;
    private final String[][] ventajasPorNivel;
    private final PuntosService puntos;
    private final NotificacionService notificaciones;
    private final GeneradorIdentificadores generador;
    private final FabricaEstructuras fabrica;
    private final EscrituraConDiario escritura;
    private final CerrojoEscritura cerrojo;
    private final Clock reloj;

    public RecompensaService(UsuarioService usuarios, RepositorioBeneficios beneficios, CatalogoBeneficios catalogo,
                             String[][] ventajasPorNivel, PuntosService puntos, NotificacionService notificaciones,
                             GeneradorIdentificadores generador,
                             FabricaEstructuras fabrica, EscrituraConDiario escritura, Clock reloj) {
        if (ventajasPorNivel == null || ventajasPorNivel.length != NivelUsuario.values().length) {
            throw new IllegalArgumentException("Se necesita una lista de ventajas (posiblemente vacía) por nivel");
        }
        this.usuarios = usuarios;
        this.beneficios = beneficios;
        this.catalogo = catalogo;
        this.ventajasPorNivel = ventajasPorNivel;
        this.puntos = puntos;
        this.notificaciones = notificaciones;
        this.generador = generador;
        this.fabrica = fabrica;
        this.escritura = escritura;
        this.cerrojo = escritura.cerrojo();
        this.reloj = reloj;
    }

    /** @return ventajas configuradas del nivel (vacío mientras no estén definidas) */
    public String[] ventajas(NivelUsuario nivel) {
        return ventajasPorNivel[nivel.ordinal()].clone();
    }

    public Lista<CatalogoBeneficios.Beneficio> catalogo() {
        return catalogo.todos(fabrica);
    }

    public BeneficioCanjeado canjear(String identificacion, String codigoBeneficio) {
        synchronized (cerrojo) {
            escritura.asegurarSinPendientes();
            Usuario usuario = usuarios.buscar(identificacion);
            if (!catalogo.estaDefinido()) {
                throw new ConflictoNegocioException("CATALOGO_NO_DEFINIDO",
                        "El catálogo de beneficios no está definido (fintech.beneficios.catalogo)");
            }
            CatalogoBeneficios.Beneficio beneficio = catalogo.buscar(codigoBeneficio);
            if (beneficio == null) {
                throw new RecursoNoEncontradoException("BENEFICIO_NO_ENCONTRADO", "No existe el beneficio "
                        + codigoBeneficio);
            }
            if (usuario.puntosAcumulados() < beneficio.costo()) {
                throw new ConflictoNegocioException("PUNTOS_INSUFICIENTES", "El beneficio " + beneficio.codigo()
                        + " cuesta " + beneficio.costo() + " puntos y el usuario tiene " + usuario.puntosAcumulados());
            }
            Usuario despues = puntos.ajustar(usuario, -beneficio.costo());
            BeneficioCanjeado canje = new BeneficioCanjeado(generador.siguiente(TipoIdentificador.BENEFICIO_CANJEADO),
                    identificacion, beneficio.codigo() + " - " + beneficio.descripcion(), beneficio.costo(),
                    LocalDateTime.now(reloj), EstadoCanje.REALIZADO);
            Object[] registros = {despues, canje, notificaciones.nueva(identificacion, TipoNotificacion.CANJE_REALIZADO,
                    "Canjeaste " + beneficio.descripcion() + " por " + beneficio.costo() + " puntos (" + canje.id() + ")")};
            escritura.ejecutar(canje.id(), registros);
            return canje;
        }
    }

    public Lista<BeneficioCanjeado> canjesDe(String identificacion) {
        usuarios.buscar(identificacion);
        return beneficios.porUsuario(identificacion);
    }
}
