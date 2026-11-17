package co.edu.uniquindio.fintech.soporte;

import co.edu.uniquindio.fintech.config.ContextoDatos;
import co.edu.uniquindio.fintech.config.FabricaEstructurasPropias;
import co.edu.uniquindio.fintech.config.InicializadorDatos;
import co.edu.uniquindio.fintech.persistence.AlmacenesJson;
import co.edu.uniquindio.fintech.persistence.CerrojoEscritura;
import co.edu.uniquindio.fintech.persistence.DiarioOperacionesJson;
import co.edu.uniquindio.fintech.repository.FabricaEstructuras;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Componentes reales de persistencia sobre un directorio temporal de prueba. Crear otro
 * entorno sobre el mismo directorio equivale a reiniciar la aplicación.
 */
public final class EntornoDatos {

    public final Path directorio;
    public final AlmacenesJson almacenes;
    public final CerrojoEscritura cerrojo = new CerrojoEscritura();
    public final FabricaEstructuras fabrica = new FabricaEstructurasPropias();
    public final DiarioOperacionesJson diario;

    public EntornoDatos(Path directorio) {
        this(directorio, DatosPrueba.RELOJ);
    }

    public EntornoDatos(Path directorio, java.time.Clock reloj) {
        this.directorio = directorio;
        this.almacenes = new AlmacenesJson(directorio);
        this.diario = new DiarioOperacionesJson(almacenes, cerrojo, reloj);
    }

    /** Ejecuta el arranque completo de los datos (sin Spring). */
    public ContextoDatos iniciar() {
        return iniciar(0);
    }

    public ContextoDatos iniciar(int limiteNotificacionesRecientes) {
        return new InicializadorDatos(almacenes, diario, fabrica, cerrojo, limiteNotificacionesRecientes).inicializar();
    }

    public void escribir(String archivo, String contenido) {
        try {
            Files.createDirectories(directorio);
            Files.writeString(directorio.resolve(archivo), contenido);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String leer(String archivo) {
        try {
            return Files.readString(directorio.resolve(archivo));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public boolean existe(String archivo) {
        return Files.exists(directorio.resolve(archivo));
    }

    /** Convierte el archivo en un directorio no vacío: cualquier escritura posterior fallará. */
    public void bloquearEscritura(String archivo) {
        try {
            Path ruta = directorio.resolve(archivo);
            Files.deleteIfExists(ruta);
            Files.createDirectories(ruta);
            Files.writeString(ruta.resolve("bloqueo.txt"), "impide reemplazar el archivo");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** @return cantidad de temporales de escritura atómica en el directorio */
    public int temporales() {
        if (!Files.isDirectory(directorio)) {
            return 0;
        }
        try (var archivos = Files.list(directorio)) {
            return (int) archivos.filter(p -> p.getFileName().toString().startsWith(".fintech-")).count();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
