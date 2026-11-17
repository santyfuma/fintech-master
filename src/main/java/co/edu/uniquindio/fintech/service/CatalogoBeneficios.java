package co.edu.uniquindio.fintech.service;

import co.edu.uniquindio.fintech.repository.FabricaEstructuras;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import co.edu.uniquindio.fintech.structures.interfaces.TablaHash;

/**
 * Catálogo de beneficios canjeables con puntos y ventajas descriptivas por nivel.
 *
 * <p>El enunciado (4.4, 4.5) solo da ejemplos ("reducción en comisiones", "bonos de
 * puntos"…) sin costos ni condiciones, así que <b>no se inventa un catálogo</b>: por
 * defecto está vacío y el canje responde que no hay catálogo definido. Se configura con
 * {@code fintech.beneficios.catalogo} con el formato
 * {@code CODIGO|Descripción|costo;CODIGO2|Descripción 2|costo2}.
 */
public final class CatalogoBeneficios {

    /** Beneficio del catálogo. */
    public record Beneficio(String codigo, String descripcion, long costo) {
        public Beneficio {
            if (codigo == null || !codigo.matches("[A-Z0-9_]+")) {
                throw new IllegalArgumentException("Código de beneficio inválido: " + codigo);
            }
            if (descripcion == null || descripcion.isBlank() || costo <= 0) {
                throw new IllegalArgumentException("El beneficio " + codigo + " necesita descripción y costo positivo");
            }
        }
    }

    private final Lista<Beneficio> beneficios;
    private final TablaHash<String, Beneficio> porCodigo;

    public CatalogoBeneficios(String definicion, FabricaEstructuras fabrica) {
        this.beneficios = fabrica.nuevaLista();
        this.porCodigo = fabrica.nuevaTablaHash();
        if (definicion == null || definicion.isBlank()) {
            return;
        }
        for (String entrada : definicion.split(";")) {
            if (entrada.isBlank()) {
                continue;
            }
            String[] partes = entrada.split("\\|");
            if (partes.length != 3) {
                throw new IllegalArgumentException("Entrada de catálogo inválida (CODIGO|Descripción|costo): " + entrada);
            }
            Beneficio beneficio;
            try {
                beneficio = new Beneficio(partes[0].trim(), partes[1].trim(), Long.parseLong(partes[2].trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Costo inválido en el catálogo: " + entrada, e);
            }
            if (porCodigo.poner(beneficio.codigo(), beneficio) != null) {
                throw new IllegalArgumentException("Beneficio repetido en el catálogo: " + beneficio.codigo());
            }
            beneficios.agregar(beneficio);
        }
    }

    public boolean estaDefinido() {
        return !beneficios.estaVacia();
    }

    /** @return el beneficio, o null si no existe */
    public Beneficio buscar(String codigo) {
        return codigo == null ? null : porCodigo.obtener(codigo);
    }

    /** @return copia del catálogo en el orden configurado */
    public Lista<Beneficio> todos(FabricaEstructuras fabrica) {
        Lista<Beneficio> copia = fabrica.nuevaLista();
        for (Beneficio b : beneficios) {
            copia.agregar(b);
        }
        return copia;
    }
}
