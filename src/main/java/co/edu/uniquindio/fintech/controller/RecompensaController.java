package co.edu.uniquindio.fintech.controller;

import co.edu.uniquindio.fintech.dto.BeneficioCanjeadoRespuesta;
import co.edu.uniquindio.fintech.dto.BeneficioCatalogoRespuesta;
import co.edu.uniquindio.fintech.dto.NivelRespuesta;
import co.edu.uniquindio.fintech.dto.SolicitudCanje;
import co.edu.uniquindio.fintech.model.NivelUsuario;
import co.edu.uniquindio.fintech.service.RecompensaService;
import co.edu.uniquindio.fintech.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Niveles, catálogo y canjes (API.md §6). Catálogo y ventajas son configuración provisional. */
@RestController
public class RecompensaController {

    private final RecompensaService recompensas;
    private final UsuarioService usuarios;

    public RecompensaController(RecompensaService recompensas, UsuarioService usuarios) {
        this.recompensas = recompensas;
        this.usuarios = usuarios;
    }

    @GetMapping("/api/niveles")
    public NivelRespuesta[] niveles() {
        NivelUsuario[] niveles = NivelUsuario.values();
        NivelRespuesta[] r = new NivelRespuesta[niveles.length];
        for (int i = 0; i < niveles.length; i++) {
            r[i] = NivelRespuesta.desde(niveles[i], recompensas.ventajas(niveles[i]));
        }
        return r;
    }

    @GetMapping("/api/beneficios")
    public BeneficioCatalogoRespuesta[] catalogo() {
        return Arreglos.de(recompensas.catalogo(),
                b -> new BeneficioCatalogoRespuesta(b.codigo(), b.descripcion(), b.costo()),
                BeneficioCatalogoRespuesta[]::new);
    }

    @PostMapping("/api/usuarios/{id}/canjes")
    @ResponseStatus(HttpStatus.CREATED)
    public BeneficioCanjeadoRespuesta canjear(@PathVariable("id") String id, @Valid @RequestBody SolicitudCanje s) {
        return BeneficioCanjeadoRespuesta.desde(recompensas.canjear(id, s.beneficio()));
    }

    @GetMapping("/api/usuarios/{id}/canjes")
    public BeneficioCanjeadoRespuesta[] canjes(@PathVariable("id") String id) {
        usuarios.buscar(id);
        return Arreglos.de(recompensas.canjesDe(id), BeneficioCanjeadoRespuesta::desde,
                BeneficioCanjeadoRespuesta[]::new);
    }
}
