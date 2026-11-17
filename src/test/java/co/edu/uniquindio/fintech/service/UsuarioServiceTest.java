package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.dto.SolicitudActualizacionUsuario;
import co.edu.uniquindio.fintech.dto.SolicitudRegistroUsuario;
import co.edu.uniquindio.fintech.exception.ConflictoNegocioException;
import co.edu.uniquindio.fintech.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.fintech.exception.SolicitudInvalidaException;
import co.edu.uniquindio.fintech.model.NivelUsuario;
import co.edu.uniquindio.fintech.model.Usuario;
import co.edu.uniquindio.fintech.soporte.SistemaPrueba;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;

import static co.edu.uniquindio.fintech.soporte.Afirmaciones.lanza;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioServiceTest {

    @TempDir
    Path directorio;

    private SistemaPrueba s;

    @BeforeEach
    void iniciar() {
        s = new SistemaPrueba(directorio);
    }

    @Test
    void registrarCreaUsuarioBronceConCeroPuntosYPersiste() {
        Usuario u = s.usuarios.registrar(new SolicitudRegistroUsuario("100", "Ana", "ana@correo.co"));
        assertEquals(0, u.puntosAcumulados());
        assertEquals(NivelUsuario.BRONCE, u.nivel());
        assertEquals(u, s.reiniciar().usuarios.buscar("100"));
    }

    @Test
    void identificacionDuplicadaEsConflicto() {
        s.usuario("100");
        lanza(ConflictoNegocioException.class, "USUARIO_YA_EXISTE", () -> s.usuario("100"));
        assertEquals(1, s.usuarios.listar().tamanio());
    }

    @Test
    void buscarInexistenteEs404() {
        lanza(RecursoNoEncontradoException.class, "USUARIO_NO_ENCONTRADO", () -> s.usuarios.buscar("999"));
    }

    @Test
    void actualizarCambiaNombreYCorreoSinTocarPuntos() {
        s.usuario("100");
        String bil = s.billetera("100", "500");
        Usuario antes = s.usuarios.buscar("100");
        Usuario u = s.usuarios.actualizar("100", new SolicitudActualizacionUsuario("Ana María", "am@correo.co"));
        assertEquals("Ana María", u.nombre());
        assertEquals(antes.puntosAcumulados(), u.puntosAcumulados());
        assertEquals(u, s.reiniciar().usuarios.buscar("100"));
        assertTrue(bil.startsWith("BIL-"));
    }

    @Test
    void eliminarSinRegistrosLoQuitaYConBilleterasEsConflicto() {
        s.usuario("100");
        s.usuario("200");
        s.billetera("200", "0");
        s.usuarios.eliminar("100");
        lanza(RecursoNoEncontradoException.class, "USUARIO_NO_ENCONTRADO", () -> s.usuarios.buscar("100"));
        lanza(ConflictoNegocioException.class, "USUARIO_CON_REGISTROS", () -> s.usuarios.eliminar("200"));
        assertFalse(s.reiniciar().datos.usuarios().existe("100"));
    }

    @Test
    void ordenYRangoPorPuntosUsanElArbol() {
        s.usuario("1");
        s.usuario("2");
        s.usuario("3");
        s.billetera("1", "300");   // 3 puntos
        s.billetera("2", "1000");  // 10 puntos
        s.billetera("3", "50");    // 0 puntos
        Lista<Usuario> orden = s.usuarios.ordenadosPorPuntos();
        assertEquals("3", orden.obtener(0).identificacion());
        assertEquals("2", orden.obtener(2).identificacion());
        Lista<Usuario> rango = s.usuarios.porRangoDePuntos(1, 5);
        assertEquals(1, rango.tamanio());
        assertEquals("1", rango.obtener(0).identificacion());
        assertEquals("2", s.usuarios.conMayorPuntaje().identificacion());
        lanza(SolicitudInvalidaException.class, "RANGO_INVALIDO", () -> s.usuarios.porRangoDePuntos(5, 1));
        lanza(SolicitudInvalidaException.class, "RANGO_INVALIDO", () -> s.usuarios.porRangoDePuntos(-1, 3));
    }

    @Test
    void conMayorPuntajeSinUsuariosEs404() {
        lanza(RecursoNoEncontradoException.class, "SIN_USUARIOS", () -> s.usuarios.conMayorPuntaje());
    }

    @Test
    void porNivelYSiguienteNivel() {
        s.usuario("1");
        s.billetera("1", "60000"); // 600 puntos → PLATA
        Usuario u = s.usuarios.buscar("1");
        assertEquals(NivelUsuario.PLATA, u.nivel());
        assertEquals(1, s.usuarios.porNivel(NivelUsuario.PLATA).tamanio());
        assertEquals(0, s.usuarios.porNivel(NivelUsuario.BRONCE).tamanio());
        assertEquals(NivelUsuario.ORO, s.usuarios.siguienteNivel(u));
        assertEquals(Long.valueOf(401), s.usuarios.puntosParaSiguienteNivel(u));
    }

    @Test
    void platinoNoTieneSiguienteNivel() {
        s.usuario("1");
        s.billetera("1", "600000"); // 6000 puntos
        Usuario u = s.usuarios.buscar("1");
        assertEquals(NivelUsuario.PLATINO, u.nivel());
        assertNull(s.usuarios.siguienteNivel(u));
        assertNull(s.usuarios.puntosParaSiguienteNivel(u));
        assertEquals(new BigDecimal("600000"), s.saldo(s.billeteras.porPropietario("1").obtener(0).codigo()));
    }
}
