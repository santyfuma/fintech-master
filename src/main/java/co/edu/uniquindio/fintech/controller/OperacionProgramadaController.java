package co.edu.uniquindio.fintech.controller;

import co.edu.uniquindio.fintech.dto.OperacionProgramadaRespuesta;
import co.edu.uniquindio.fintech.dto.ProcesamientoRespuesta;
import co.edu.uniquindio.fintech.dto.ResultadoEjecucionRespuesta;
import co.edu.uniquindio.fintech.dto.SolicitudOperacionProgramada;
import co.edu.uniquindio.fintech.model.EstadoOperacionProgramada;
import co.edu.uniquindio.fintech.service.OperacionProgramadaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;

/** Operaciones programadas (API.md §5). {@code procesar} usa el reloj de la aplicación. */
@RestController
public class OperacionProgramadaController {

    private final OperacionProgramadaService programadas;
    private final Clock reloj;

    public OperacionProgramadaController(OperacionProgramadaService programadas, Clock reloj) {
        this.programadas = programadas;
        this.reloj = reloj;
    }

    @PostMapping("/api/operaciones-programadas")
    @ResponseStatus(HttpStatus.CREATED)
    public OperacionProgramadaRespuesta programar(@Valid @RequestBody SolicitudOperacionProgramada s) {
        return OperacionProgramadaRespuesta.desde(programadas.programar(s));
    }

    @GetMapping("/api/operaciones-programadas")
    public OperacionProgramadaRespuesta[] listar(
            @RequestParam(name = "estado", required = false) EstadoOperacionProgramada estado) {
        return Arreglos.de(programadas.listar(estado), OperacionProgramadaRespuesta::desde,
                OperacionProgramadaRespuesta[]::new);
    }

    @GetMapping("/api/operaciones-programadas/proxima")
    public OperacionProgramadaRespuesta proxima() {
        return OperacionProgramadaRespuesta.desde(programadas.siguientePendiente());
    }

    @GetMapping("/api/operaciones-programadas/{id}")
    public OperacionProgramadaRespuesta buscar(@PathVariable("id") String id) {
        return OperacionProgramadaRespuesta.desde(programadas.buscar(id));
    }

    @DeleteMapping("/api/operaciones-programadas/{id}")
    public OperacionProgramadaRespuesta cancelar(@PathVariable("id") String id) {
        return OperacionProgramadaRespuesta.desde(programadas.cancelar(id));
    }

    @GetMapping("/api/usuarios/{id}/operaciones-programadas")
    public OperacionProgramadaRespuesta[] deUsuario(@PathVariable("id") String id) {
        return Arreglos.de(programadas.porUsuario(id), OperacionProgramadaRespuesta::desde,
                OperacionProgramadaRespuesta[]::new);
    }

    /** Ejecución manual de las vencidas (la automática la hace una tarea periódica). */
    @PostMapping("/api/operaciones-programadas/procesar")
    public ProcesamientoRespuesta procesar() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        ResultadoEjecucionRespuesta[] r = Arreglos.de(programadas.procesarVencidas(),
                x -> new ResultadoEjecucionRespuesta(x.operacion(), x.transaccion(), x.estado(), x.motivo(),
                        x.siguienteOperacion()), ResultadoEjecucionRespuesta[]::new);
        int fallidas = 0;
        for (ResultadoEjecucionRespuesta x : r) {
            if (x.estado() == EstadoOperacionProgramada.FALLIDA) {
                fallidas++;
            }
        }
        return new ProcesamientoRespuesta(ahora, r.length - fallidas, fallidas, r);
    }
}
