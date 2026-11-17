package co.edu.uniquindio.fintech.controller;

import co.edu.uniquindio.fintech.dto.SolicitudRecarga;
import co.edu.uniquindio.fintech.dto.SolicitudRetiro;
import co.edu.uniquindio.fintech.dto.SolicitudTransferencia;
import co.edu.uniquindio.fintech.dto.TransaccionRespuesta;
import co.edu.uniquindio.fintech.service.TransaccionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Movimientos, historial y reversiones (API.md §3 y §4). Un rechazo de negocio responde
 * 409, pero la transacción RECHAZADA queda registrada en el historial.
 */
@RestController
public class TransaccionController {

    private final TransaccionService transacciones;

    public TransaccionController(TransaccionService transacciones) {
        this.transacciones = transacciones;
    }

    @PostMapping("/api/transacciones/recargas")
    @ResponseStatus(HttpStatus.CREATED)
    public TransaccionRespuesta recargar(@Valid @RequestBody SolicitudRecarga s) {
        return TransaccionRespuesta.desde(transacciones.recargar(s.billeteraDestino(), s.valor()));
    }

    @PostMapping("/api/transacciones/retiros")
    @ResponseStatus(HttpStatus.CREATED)
    public TransaccionRespuesta retirar(@Valid @RequestBody SolicitudRetiro s) {
        return TransaccionRespuesta.desde(transacciones.retirar(s.billeteraOrigen(), s.valor()));
    }

    @PostMapping("/api/transacciones/transferencias")
    @ResponseStatus(HttpStatus.CREATED)
    public TransaccionRespuesta transferir(@Valid @RequestBody SolicitudTransferencia s) {
        return TransaccionRespuesta.desde(transacciones.transferir(s.billeteraOrigen(), s.billeteraDestino(), s.valor()));
    }

    @GetMapping("/api/transacciones")
    public TransaccionRespuesta[] todas() {
        return Arreglos.de(transacciones.todas(), TransaccionRespuesta::desde, TransaccionRespuesta[]::new);
    }

    @GetMapping("/api/transacciones/{id}")
    public TransaccionRespuesta buscar(@PathVariable("id") String id) {
        return TransaccionRespuesta.desde(transacciones.buscar(id));
    }

    @GetMapping("/api/usuarios/{id}/transacciones")
    public TransaccionRespuesta[] deUsuario(@PathVariable("id") String id) {
        return Arreglos.de(transacciones.historialDeUsuario(id), TransaccionRespuesta::desde, TransaccionRespuesta[]::new);
    }

    @GetMapping("/api/billeteras/{codigo}/transacciones")
    public TransaccionRespuesta[] deBilletera(@PathVariable("codigo") String codigo) {
        return Arreglos.de(transacciones.historialDeBilletera(codigo), TransaccionRespuesta::desde,
                TransaccionRespuesta[]::new);
    }

    @PostMapping("/api/transacciones/{id}/reversion")
    @ResponseStatus(HttpStatus.CREATED)
    public TransaccionRespuesta revertir(@PathVariable("id") String id) {
        return TransaccionRespuesta.desde(transacciones.revertir(id));
    }

    @PostMapping("/api/usuarios/{id}/reversion/ultima")
    @ResponseStatus(HttpStatus.CREATED)
    public TransaccionRespuesta revertirUltima(@PathVariable("id") String id) {
        return TransaccionRespuesta.desde(transacciones.revertirUltima(id));
    }
}
