package co.edu.uniquindio.fintech.repository;

import co.edu.uniquindio.fintech.model.BeneficioCanjeado;
import co.edu.uniquindio.fintech.persistence.AlmacenJson;
import co.edu.uniquindio.fintech.persistence.CerrojoEscritura;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;

/**
 * Beneficios canjeados por identificador y por usuario (orden cronológico). No se
 * eliminan; un canje deshecho se actualiza con estado ANULADO. No hay catálogo de
 * beneficios (pendiente).
 */
public final class RepositorioBeneficios extends RepositorioEnMemoria<BeneficioCanjeado> {

    private final IndicePorClave porUsuario = new IndicePorClave(fabrica);

    public RepositorioBeneficios(AlmacenJson<BeneficioCanjeado> almacen, FabricaEstructuras fabrica,
                                 CerrojoEscritura cerrojo, Lista<BeneficioCanjeado> iniciales) {
        super(almacen, fabrica, cerrojo);
        cargarIniciales(iniciales);
    }

    @Override
    protected String identificadorDe(BeneficioCanjeado beneficio) {
        return beneficio.id();
    }

    @Override
    protected void indexar(BeneficioCanjeado c) {
        porUsuario.agregar(c.identificacionUsuario(), c.id());
    }

    @Override
    protected void desindexar(BeneficioCanjeado c) {
        porUsuario.eliminar(c.identificacionUsuario(), c.id());
    }

    @Override
    protected void reindexar(BeneficioCanjeado anterior, BeneficioCanjeado nuevo) {
        if (!anterior.identificacionUsuario().equals(nuevo.identificacionUsuario())) {
            desindexar(anterior);
            indexar(nuevo);
        }
    }

    public BeneficioCanjeado registrar(BeneficioCanjeado beneficio) {
        return registrarRegistro(beneficio);
    }

    /** @return la versión anterior */
    public BeneficioCanjeado actualizar(BeneficioCanjeado beneficio) {
        return actualizarRegistro(beneficio);
    }

    /** @return canjes del usuario, en orden cronológico */
    public Lista<BeneficioCanjeado> porUsuario(String identificacionUsuario) {
        requerido(identificacionUsuario, "identificacionUsuario");
        synchronized (cerrojo()) {
            return resolver(porUsuario.identificadores(identificacionUsuario));
        }
    }
}
