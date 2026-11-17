package co.edu.uniquindio.fintech.soporte;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Reloj de prueba que se puede adelantar o fijar (simula el paso del tiempo). */
public final class RelojAjustable extends Clock {

    public static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private Instant ahora;

    public RelojAjustable(LocalDateTime inicio) {
        this.ahora = inicio.atZone(ZONA).toInstant();
    }

    public RelojAjustable() {
        this(DatosPrueba.FECHA);
    }

    public synchronized void avanzar(Duration duracion) {
        ahora = ahora.plus(duracion);
    }

    public synchronized void fijar(LocalDateTime momento) {
        ahora = momento.atZone(ZONA).toInstant();
    }

    public LocalDateTime ahora() {
        return LocalDateTime.ofInstant(instant(), ZONA);
    }

    @Override
    public ZoneId getZone() {
        return ZONA;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized Instant instant() {
        return ahora;
    }
}
