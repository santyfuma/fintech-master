package co.edu.uniquindio.fintech.repository;

import co.edu.uniquindio.fintech.model.Billetera;
import co.edu.uniquindio.fintech.persistence.AlmacenJson;
import co.edu.uniquindio.fintech.persistence.CerrojoEscritura;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;

/**
 * Billeteras por código (tabla hash) y por propietario (lista de códigos por
 * identificación, en orden de creación). No hay eliminación: una billetera se desactiva
 * actualizándola con estado INACTIVA.
 */
public final class RepositorioBilleteras extends RepositorioEnMemoria<Billetera> {

    private final IndicePorClave porPropietario = new IndicePorClave(fabrica);

    public RepositorioBilleteras(AlmacenJson<Billetera> almacen, FabricaEstructuras fabrica, CerrojoEscritura cerrojo,
                                 Lista<Billetera> iniciales) {
        super(almacen, fabrica, cerrojo);
        cargarIniciales(iniciales);
    }

    @Override
    protected String identificadorDe(Billetera billetera) {
        return billetera.codigo();
    }

    @Override
    protected void indexar(Billetera billetera) {
        porPropietario.agregar(billetera.identificacionPropietario(), billetera.codigo());
    }

    @Override
    protected void desindexar(Billetera billetera) {
        porPropietario.eliminar(billetera.identificacionPropietario(), billetera.codigo());
    }

    @Override
    protected void reindexar(Billetera anterior, Billetera nueva) {
        if (!anterior.identificacionPropietario().equals(nueva.identificacionPropietario())) {
            desindexar(anterior);
            indexar(nueva);
        }
    }

    /** @throws IllegalStateException si el código ya existe */
    public Billetera registrar(Billetera billetera) {
        return registrarRegistro(billetera);
    }

    /**
     * @return la versión anterior
     * @throws IllegalStateException si la billetera no existe
     */
    public Billetera actualizar(Billetera billetera) {
        return actualizarRegistro(billetera);
    }

    /** @return billeteras del usuario, en orden de creación */
    public Lista<Billetera> porPropietario(String identificacionPropietario) {
        requerido(identificacionPropietario, "identificacionPropietario");
        synchronized (cerrojo()) {
            return resolver(porPropietario.identificadores(identificacionPropietario));
        }
    }

    /** @return identificación del propietario, o null si la billetera no existe */
    public String propietarioDe(String codigo) {
        Billetera billetera = buscar(codigo);
        return billetera == null ? null : billetera.identificacionPropietario();
    }
}
