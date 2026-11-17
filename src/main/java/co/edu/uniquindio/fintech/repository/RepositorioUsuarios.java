package co.edu.uniquindio.fintech.repository;

import co.edu.uniquindio.fintech.model.Usuario;
import co.edu.uniquindio.fintech.persistence.AlmacenJson;
import co.edu.uniquindio.fintech.persistence.CerrojoEscritura;
import co.edu.uniquindio.fintech.structures.interfaces.ArbolBinarioBusqueda;
import co.edu.uniquindio.fintech.structures.interfaces.ClaveCompuesta;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;

/**
 * Usuarios por identificación (tabla hash) y por puntos (ABB con clave compuesta
 * {@code (puntos, identificación)}, que admite usuarios con los mismos puntos).
 *
 * <p>Al cambiar los puntos se retira la clave anterior del ABB antes de insertar la
 * nueva, así que nunca hay claves viejas. Eliminar está disponible porque el enunciado
 * lo pide (RF1); <b>cuándo</b> se permite (saldo, billeteras activas) lo decide
 * {@code service}.
 *
 * <p>Complejidad de las consultas por puntos: O(h + k), con h la altura del ABB (sin
 * balanceo: O(log n) con puntos variados, hasta O(n) en el peor caso).
 */
public final class RepositorioUsuarios extends RepositorioEnMemoria<Usuario> {

    private final ArbolBinarioBusqueda<ClaveCompuesta<Long, String>, String> porPuntos = fabrica.nuevoArbol();

    public RepositorioUsuarios(AlmacenJson<Usuario> almacen, FabricaEstructuras fabrica, CerrojoEscritura cerrojo,
                               Lista<Usuario> iniciales) {
        super(almacen, fabrica, cerrojo);
        cargarIniciales(iniciales);
    }

    @Override
    protected String identificadorDe(Usuario usuario) {
        return usuario.identificacion();
    }

    @Override
    protected void indexar(Usuario usuario) {
        porPuntos.insertar(clave(usuario), usuario.identificacion());
    }

    @Override
    protected void desindexar(Usuario usuario) {
        porPuntos.eliminar(clave(usuario));
    }

    @Override
    protected void reindexar(Usuario anterior, Usuario nuevo) {
        if (anterior.puntosAcumulados() != nuevo.puntosAcumulados()) {
            desindexar(anterior);
            indexar(nuevo);
        }
    }

    /** @throws IllegalStateException si la identificación ya está registrada */
    public Usuario registrar(Usuario usuario) {
        return registrarRegistro(usuario);
    }

    /**
     * @return la versión anterior
     * @throws IllegalStateException si el usuario no existe
     */
    public Usuario actualizar(Usuario usuario) {
        return actualizarRegistro(usuario);
    }

    /** @return el usuario eliminado, o null si no existía */
    public Usuario eliminar(String identificacion) {
        return eliminarRegistro(identificacion);
    }

    /** @return usuarios de menor a mayor cantidad de puntos (empates por identificación) */
    public Lista<Usuario> ordenadosPorPuntos() {
        synchronized (cerrojo()) {
            return resolver(porPuntos.inOrden());
        }
    }

    /** @return los {@code cantidad} usuarios con más puntos, de mayor a menor */
    public Lista<Usuario> conMasPuntos(int cantidad) {
        synchronized (cerrojo()) {
            return resolver(porPuntos.mayores(cantidad));
        }
    }

    /** @return usuarios con puntos en {@code [minimo, maximo]}, de menor a mayor */
    public Lista<Usuario> porRangoDePuntos(long minimo, long maximo) {
        synchronized (cerrojo()) {
            return resolver(porPuntos.rango(ClaveCompuesta.cotaInferior(minimo), ClaveCompuesta.cotaSuperior(maximo)));
        }
    }

    private static ClaveCompuesta<Long, String> clave(Usuario usuario) {
        return ClaveCompuesta.de(usuario.puntosAcumulados(), usuario.identificacion());
    }
}
