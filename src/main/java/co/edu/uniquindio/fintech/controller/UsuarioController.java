package co.edu.uniquindio.fintech.controller;

import co.edu.uniquindio.fintech.dto.PuntosRespuesta;
import co.edu.uniquindio.fintech.dto.SolicitudActualizacionUsuario;
import co.edu.uniquindio.fintech.dto.SolicitudRegistroUsuario;
import co.edu.uniquindio.fintech.dto.UsuarioRespuesta;
import co.edu.uniquindio.fintech.model.NivelUsuario;
import co.edu.uniquindio.fintech.model.Usuario;
import co.edu.uniquindio.fintech.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Usuarios, puntos y niveles (API.md §1). */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarios;

    public UsuarioController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioRespuesta registrar(@Valid @RequestBody SolicitudRegistroUsuario solicitud) {
        return UsuarioRespuesta.desde(usuarios.registrar(solicitud));
    }

    @GetMapping
    public UsuarioRespuesta[] listar() {
        return Arreglos.de(usuarios.listar(), UsuarioRespuesta::desde, UsuarioRespuesta[]::new);
    }

    @GetMapping("/{id}")
    public UsuarioRespuesta buscar(@PathVariable("id") String id) {
        return UsuarioRespuesta.desde(usuarios.buscar(id));
    }

    @PutMapping("/{id}")
    public UsuarioRespuesta actualizar(@PathVariable("id") String id,
                                       @Valid @RequestBody SolicitudActualizacionUsuario solicitud) {
        return UsuarioRespuesta.desde(usuarios.actualizar(id, solicitud));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable("id") String id) {
        usuarios.eliminar(id);
    }

    @GetMapping("/{id}/puntos")
    public PuntosRespuesta puntos(@PathVariable("id") String id) {
        Usuario u = usuarios.buscar(id);
        return new PuntosRespuesta(u.identificacion(), u.puntosAcumulados(), u.nivel(), usuarios.siguienteNivel(u),
                usuarios.puntosParaSiguienteNivel(u));
    }

    /** Sin rango: todos ordenados por puntos (ABB). Con rango: solo los de [min, max]. */
    @GetMapping("/por-puntos")
    public UsuarioRespuesta[] porPuntos(@RequestParam(name = "min", required = false) Long min,
                                        @RequestParam(name = "max", required = false) Long max) {
        if (min == null && max == null) {
            return Arreglos.de(usuarios.ordenadosPorPuntos(), UsuarioRespuesta::desde, UsuarioRespuesta[]::new);
        }
        return Arreglos.de(usuarios.porRangoDePuntos(min == null ? 0 : min, max == null ? Long.MAX_VALUE : max),
                UsuarioRespuesta::desde, UsuarioRespuesta[]::new);
    }

    @GetMapping("/por-nivel/{nivel}")
    public UsuarioRespuesta[] porNivel(@PathVariable("nivel") NivelUsuario nivel) {
        return Arreglos.de(usuarios.porNivel(nivel), UsuarioRespuesta::desde, UsuarioRespuesta[]::new);
    }

    @GetMapping("/mayor-puntaje")
    public UsuarioRespuesta mayorPuntaje() {
        return UsuarioRespuesta.desde(usuarios.conMayorPuntaje());
    }
}
