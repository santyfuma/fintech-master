package co.edu.uniquindio.fintech.controller;

import co.edu.uniquindio.fintech.dto.BilleteraRespuesta;
import co.edu.uniquindio.fintech.dto.SaldoRespuesta;
import co.edu.uniquindio.fintech.dto.SaldoTotalRespuesta;
import co.edu.uniquindio.fintech.dto.SolicitudActualizacionBilletera;
import co.edu.uniquindio.fintech.dto.SolicitudCreacionBilletera;
import co.edu.uniquindio.fintech.service.BilleteraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Billeteras (API.md §2). DELETE desactiva: nunca se borra físicamente. */
@RestController
public class BilleteraController {

    private final BilleteraService billeteras;

    public BilleteraController(BilleteraService billeteras) {
        this.billeteras = billeteras;
    }

    @PostMapping("/api/usuarios/{id}/billeteras")
    @ResponseStatus(HttpStatus.CREATED)
    public BilleteraRespuesta crear(@PathVariable("id") String id, @Valid @RequestBody SolicitudCreacionBilletera s) {
        return BilleteraRespuesta.desde(billeteras.crear(id, s));
    }

    @GetMapping("/api/usuarios/{id}/billeteras")
    public BilleteraRespuesta[] dePropietario(@PathVariable("id") String id) {
        return Arreglos.de(billeteras.porPropietario(id), BilleteraRespuesta::desde, BilleteraRespuesta[]::new);
    }

    @GetMapping("/api/usuarios/{id}/saldo-total")
    public SaldoTotalRespuesta saldoTotal(@PathVariable("id") String id) {
        return new SaldoTotalRespuesta(id, billeteras.saldoTotal(id), billeteras.porPropietario(id).tamanio());
    }

    @GetMapping("/api/billeteras/{codigo}")
    public BilleteraRespuesta buscar(@PathVariable("codigo") String codigo) {
        return BilleteraRespuesta.desde(billeteras.buscar(codigo));
    }

    @GetMapping("/api/billeteras/{codigo}/saldo")
    public SaldoRespuesta saldo(@PathVariable("codigo") String codigo) {
        return SaldoRespuesta.desde(billeteras.buscar(codigo));
    }

    @PutMapping("/api/billeteras/{codigo}")
    public BilleteraRespuesta actualizar(@PathVariable("codigo") String codigo,
                                         @Valid @RequestBody SolicitudActualizacionBilletera s) {
        return BilleteraRespuesta.desde(billeteras.actualizar(codigo, s));
    }

    @DeleteMapping("/api/billeteras/{codigo}")
    public BilleteraRespuesta desactivar(@PathVariable("codigo") String codigo) {
        return BilleteraRespuesta.desde(billeteras.desactivar(codigo));
    }

    @PostMapping("/api/billeteras/{codigo}/activacion")
    public BilleteraRespuesta activar(@PathVariable("codigo") String codigo) {
        return BilleteraRespuesta.desde(billeteras.activar(codigo));
    }
}
