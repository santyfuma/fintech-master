package co.edu.uniquindio.fintech.persistence;

import co.edu.uniquindio.fintech.model.BeneficioCanjeado;
import co.edu.uniquindio.fintech.model.Billetera;
import co.edu.uniquindio.fintech.model.EventoAuditoria;
import co.edu.uniquindio.fintech.model.Notificacion;
import co.edu.uniquindio.fintech.model.OperacionProgramada;
import co.edu.uniquindio.fintech.model.Transaccion;
import co.edu.uniquindio.fintech.model.Usuario;

/**
 * Los siete archivos de dominio: nombre, tipo de registro y cómo obtener su
 * identificador (la identificación en usuarios; el código o id en los demás).
 */
public enum ArchivoDominio {

    USUARIOS("usuarios.json", Usuario.class, Usuario[].class) {
        @Override
        public String identificador(Object registro) {
            return ((Usuario) registro).identificacion();
        }
    },
    BILLETERAS("billeteras.json", Billetera.class, Billetera[].class) {
        @Override
        public String identificador(Object registro) {
            return ((Billetera) registro).codigo();
        }
    },
    TRANSACCIONES("transacciones.json", Transaccion.class, Transaccion[].class) {
        @Override
        public String identificador(Object registro) {
            return ((Transaccion) registro).id();
        }
    },
    OPERACIONES_PROGRAMADAS("operaciones-programadas.json", OperacionProgramada.class, OperacionProgramada[].class) {
        @Override
        public String identificador(Object registro) {
            return ((OperacionProgramada) registro).id();
        }
    },
    NOTIFICACIONES("notificaciones.json", Notificacion.class, Notificacion[].class) {
        @Override
        public String identificador(Object registro) {
            return ((Notificacion) registro).id();
        }
    },
    AUDITORIA("auditoria.json", EventoAuditoria.class, EventoAuditoria[].class) {
        @Override
        public String identificador(Object registro) {
            return ((EventoAuditoria) registro).id();
        }
    },
    BENEFICIOS("beneficios.json", BeneficioCanjeado.class, BeneficioCanjeado[].class) {
        @Override
        public String identificador(Object registro) {
            return ((BeneficioCanjeado) registro).id();
        }
    };

    private final String nombreArchivo;
    private final Class<?> tipo;
    private final Class<?> tipoArreglo;

    ArchivoDominio(String nombreArchivo, Class<?> tipo, Class<?> tipoArreglo) {
        this.nombreArchivo = nombreArchivo;
        this.tipo = tipo;
        this.tipoArreglo = tipoArreglo;
    }

    public String nombreArchivo() {
        return nombreArchivo;
    }

    public Class<?> tipo() {
        return tipo;
    }

    public Class<?> tipoArreglo() {
        return tipoArreglo;
    }

    /**
     * @return identificador del registro (debe ser de {@link #tipo()})
     * @throws ClassCastException si el registro no es de este tipo
     */
    public abstract String identificador(Object registro);

    /** @throws IllegalArgumentException si la clase no corresponde a ningún archivo */
    public static ArchivoDominio paraTipo(Class<?> tipo) {
        for (ArchivoDominio archivo : values()) {
            if (archivo.tipo.equals(tipo)) {
                return archivo;
            }
        }
        throw new IllegalArgumentException("No hay archivo de dominio para " + tipo);
    }

    /** @return el archivo con ese nombre, o null si no es un archivo de dominio */
    public static ArchivoDominio porNombre(String nombreArchivo) {
        for (ArchivoDominio archivo : values()) {
            if (archivo.nombreArchivo.equals(nombreArchivo)) {
                return archivo;
            }
        }
        return null;
    }
}
