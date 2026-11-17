package co.edu.uniquindio.fintech.persistence;

import co.edu.uniquindio.fintech.exception.PersistenciaException;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import co.edu.uniquindio.fintech.structures.propias.ListaEnlazada;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Diario de operaciones guardado en {@value AlmacenesJson#ARCHIVO_DIARIO} (arreglo JSON de
 * {@link EntradaDiario}), escrito con la misma escritura atómica que los demás archivos.
 *
 * <p>Aplicar una entrada: por cada archivo afectado (en el orden de
 * {@link ArchivoDominio}) se leen sus registros, cada imagen reemplaza al registro con el
 * mismo identificador o se agrega al final si es nuevo, y se reescribe el archivo. Es
 * idempotente: repetirlo produce exactamente el mismo archivo.
 *
 * <p>Todas las operaciones se sincronizan con el {@link CerrojoEscritura} compartido.
 */
public class DiarioOperacionesJson implements DiarioOperaciones {

    private final AlmacenesJson almacenes;
    private final AlmacenJson<EntradaDiario> archivoDiario;
    private final CerrojoEscritura cerrojo;
    private final Clock reloj;
    private final JsonMapper mapper;
    /** Mapper sin indentación para el texto de cada imagen. */
    private final JsonMapper mapperImagenes = JsonMapper.builder().build();

    public DiarioOperacionesJson(AlmacenesJson almacenes, CerrojoEscritura cerrojo, Clock reloj) {
        if (almacenes == null || cerrojo == null || reloj == null) {
            throw new NullPointerException("almacenes, cerrojo y reloj son obligatorios");
        }
        this.almacenes = almacenes;
        this.archivoDiario = almacenes.diario();
        this.cerrojo = cerrojo;
        this.reloj = reloj;
        this.mapper = almacenes.mapper();
    }

    @Override
    public EntradaDiario preparar(String idOperacion, Object... registros) {
        if (registros == null || registros.length == 0) {
            throw new IllegalArgumentException("Una operación debe incluir al menos un registro");
        }
        ImagenRegistro[] imagenes = new ImagenRegistro[registros.length];
        for (int i = 0; i < registros.length; i++) {
            Object registro = registros[i];
            if (registro == null) {
                throw new IllegalArgumentException("Registro null en la operación " + idOperacion);
            }
            ArchivoDominio archivo = ArchivoDominio.paraTipo(registro.getClass());
            String contenido;
            try {
                contenido = mapperImagenes.writeValueAsString(registro);
            } catch (RuntimeException e) {
                throw new PersistenciaException("No se pudo convertir a JSON el registro "
                        + archivo.identificador(registro) + " de la operación " + idOperacion, e);
            }
            imagenes[i] = new ImagenRegistro(archivo.nombreArchivo(), archivo.identificador(registro), contenido);
        }
        return new EntradaDiario(idOperacion, LocalDateTime.now(reloj), EstadoEntradaDiario.PREPARADA, imagenes);
    }

    @Override
    public void registrar(EntradaDiario entrada) {
        if (entrada == null) {
            throw new NullPointerException("entrada");
        }
        synchronized (cerrojo) {
            Lista<EntradaDiario> entradas = archivoDiario.cargar();
            for (EntradaDiario existente : entradas) {
                if (existente.id().equals(entrada.id())) {
                    throw new IllegalStateException("Ya hay una entrada pendiente con el id " + entrada.id());
                }
            }
            entradas.agregar(entrada);
            archivoDiario.guardar(entradas);
        }
    }

    @Override
    public void aplicar(EntradaDiario entrada) {
        if (entrada == null) {
            throw new NullPointerException("entrada");
        }
        synchronized (cerrojo) {
            ImagenRegistro[] imagenes = entrada.imagenes();
            for (ArchivoDominio archivo : ArchivoDominio.values()) {
                Lista<ImagenRegistro> delArchivo = new ListaEnlazada<>();
                for (ImagenRegistro imagen : imagenes) {
                    if (imagen.archivo().equals(archivo.nombreArchivo())) {
                        delArchivo.agregar(imagen);
                    }
                }
                if (!delArchivo.estaVacia()) {
                    aplicarEnArchivo(entrada.id(), archivo, delArchivo);
                }
            }
        }
    }

    @Override
    public void confirmar(String idOperacion) {
        synchronized (cerrojo) {
            Lista<EntradaDiario> entradas = archivoDiario.cargar();
            Lista<EntradaDiario> restantes = new ListaEnlazada<>();
            boolean encontrada = false;
            for (EntradaDiario entrada : entradas) {
                if (entrada.id().equals(idOperacion)) {
                    encontrada = true;
                } else {
                    restantes.agregar(entrada);
                }
            }
            if (encontrada) {
                archivoDiario.guardar(restantes);
            }
        }
    }

    @Override
    public void ejecutar(EntradaDiario entrada, Runnable actualizarMemoria) {
        if (actualizarMemoria == null) {
            throw new NullPointerException("actualizarMemoria");
        }
        synchronized (cerrojo) {
            registrar(entrada);
            try {
                aplicar(entrada);
            } catch (PersistenciaException primerFallo) {
                try {
                    aplicar(entrada);
                } catch (PersistenciaException segundoFallo) {
                    segundoFallo.addSuppressed(primerFallo);
                    throw new PersistenciaException("La operación " + entrada.id() + " no pudo escribirse en los "
                            + "archivos; quedó en el diario y se completará en el próximo arranque", segundoFallo);
                }
            }
            actualizarMemoria.run();
            confirmar(entrada.id());
        }
    }

    @Override
    public int recuperarPendientes() {
        synchronized (cerrojo) {
            Lista<EntradaDiario> entradas = archivoDiario.cargar();
            int recuperadas = 0;
            for (EntradaDiario entrada : entradas) {
                try {
                    aplicar(entrada);
                } catch (RuntimeException e) {
                    throw new PersistenciaException("Falló la recuperación de la operación " + entrada.id()
                            + " del diario; no se iniciarán los repositorios: " + e.getMessage(), e);
                }
                confirmar(entrada.id());
                recuperadas++;
            }
            return recuperadas;
        }
    }

    @Override
    public Lista<EntradaDiario> pendientes() {
        synchronized (cerrojo) {
            return archivoDiario.cargar();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void aplicarEnArchivo(String idOperacion, ArchivoDominio archivo, Lista<ImagenRegistro> imagenes) {
        AlmacenJson<T> almacen = (AlmacenJson<T>) almacenes.de(archivo);
        Class<T> tipo = (Class<T>) archivo.tipo();
        Lista<T> registros = almacen.cargar();
        for (ImagenRegistro imagen : imagenes) {
            T registro;
            try {
                registro = mapper.readValue(imagen.contenido(), tipo);
            } catch (RuntimeException e) {
                throw new PersistenciaException("La imagen " + imagen.identificador() + " de la operación " + idOperacion
                        + " no es un registro válido de " + archivo.nombreArchivo() + ": " + e.getMessage(), e);
            }
            if (registro == null || !archivo.identificador(registro).equals(imagen.identificador())) {
                throw new PersistenciaException("La imagen " + imagen.identificador() + " de la operación "
                        + idOperacion + " no coincide con su contenido");
            }
            reemplazarOAgregar(archivo, registros, registro);
        }
        almacen.guardar(registros);
    }

    private static <T> void reemplazarOAgregar(ArchivoDominio archivo, Lista<T> registros, T registro) {
        String id = archivo.identificador(registro);
        int indice = 0;
        for (T existente : registros) {
            if (archivo.identificador(existente).equals(id)) {
                registros.reemplazar(indice, registro);
                return;
            }
            indice++;
        }
        registros.agregar(registro);
    }
}
