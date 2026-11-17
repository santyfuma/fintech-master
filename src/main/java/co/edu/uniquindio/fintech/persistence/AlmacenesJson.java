package co.edu.uniquindio.fintech.persistence;

import co.edu.uniquindio.fintech.model.BeneficioCanjeado;
import co.edu.uniquindio.fintech.model.Billetera;
import co.edu.uniquindio.fintech.model.EventoAuditoria;
import co.edu.uniquindio.fintech.model.Notificacion;
import co.edu.uniquindio.fintech.model.OperacionProgramada;
import co.edu.uniquindio.fintech.model.Transaccion;
import co.edu.uniquindio.fintech.model.Usuario;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;

/**
 * Los almacenes de los siete archivos de dominio y el del diario, todos en el mismo
 * directorio de datos y con el mismo {@link JsonMapper} y {@link EscrituraAtomica}.
 */
public final class AlmacenesJson {

    public static final String ARCHIVO_DIARIO = "diario-operaciones.json";

    private final Path directorio;
    private final JsonMapper mapper;
    private final EscrituraAtomica escritura;
    private final AlmacenJson<Usuario> usuarios;
    private final AlmacenJson<Billetera> billeteras;
    private final AlmacenJson<Transaccion> transacciones;
    private final AlmacenJson<OperacionProgramada> operacionesProgramadas;
    private final AlmacenJson<Notificacion> notificaciones;
    private final AlmacenJson<EventoAuditoria> auditoria;
    private final AlmacenJson<BeneficioCanjeado> beneficios;
    private final AlmacenJson<EntradaDiario> diario;

    public AlmacenesJson(Path directorio) {
        this(directorio, MapeadorJson.crear(), new EscrituraAtomica());
    }

    public AlmacenesJson(Path directorio, JsonMapper mapper, EscrituraAtomica escritura) {
        if (directorio == null || mapper == null || escritura == null) {
            throw new NullPointerException("directorio, mapper y escritura son obligatorios");
        }
        this.directorio = directorio;
        this.mapper = mapper;
        this.escritura = escritura;
        this.usuarios = crear(ArchivoDominio.USUARIOS.nombreArchivo(), Usuario[].class);
        this.billeteras = crear(ArchivoDominio.BILLETERAS.nombreArchivo(), Billetera[].class);
        this.transacciones = crear(ArchivoDominio.TRANSACCIONES.nombreArchivo(), Transaccion[].class);
        this.operacionesProgramadas = crear(ArchivoDominio.OPERACIONES_PROGRAMADAS.nombreArchivo(),
                OperacionProgramada[].class);
        this.notificaciones = crear(ArchivoDominio.NOTIFICACIONES.nombreArchivo(), Notificacion[].class);
        this.auditoria = crear(ArchivoDominio.AUDITORIA.nombreArchivo(), EventoAuditoria[].class);
        this.beneficios = crear(ArchivoDominio.BENEFICIOS.nombreArchivo(), BeneficioCanjeado[].class);
        this.diario = crear(ARCHIVO_DIARIO, EntradaDiario[].class);
    }

    private <T> AlmacenJson<T> crear(String nombre, Class<T[]> tipoArreglo) {
        return new AlmacenJsonArchivo<>(directorio, nombre, tipoArreglo, mapper, escritura);
    }

    public Path directorio() {
        return directorio;
    }

    public JsonMapper mapper() {
        return mapper;
    }

    public AlmacenJson<Usuario> usuarios() {
        return usuarios;
    }

    public AlmacenJson<Billetera> billeteras() {
        return billeteras;
    }

    public AlmacenJson<Transaccion> transacciones() {
        return transacciones;
    }

    public AlmacenJson<OperacionProgramada> operacionesProgramadas() {
        return operacionesProgramadas;
    }

    public AlmacenJson<Notificacion> notificaciones() {
        return notificaciones;
    }

    public AlmacenJson<EventoAuditoria> auditoria() {
        return auditoria;
    }

    public AlmacenJson<BeneficioCanjeado> beneficios() {
        return beneficios;
    }

    public AlmacenJson<EntradaDiario> diario() {
        return diario;
    }

    /** @return el almacén del archivo de dominio indicado */
    public AlmacenJson<?> de(ArchivoDominio archivo) {
        return switch (archivo) {
            case USUARIOS -> usuarios;
            case BILLETERAS -> billeteras;
            case TRANSACCIONES -> transacciones;
            case OPERACIONES_PROGRAMADAS -> operacionesProgramadas;
            case NOTIFICACIONES -> notificaciones;
            case AUDITORIA -> auditoria;
            case BENEFICIOS -> beneficios;
        };
    }

    /** Borra temporales de escrituras interrumpidas (ver {@link EscrituraAtomica#limpiarTemporales}). */
    public int limpiarTemporales() {
        return escritura.limpiarTemporales(directorio);
    }
}
