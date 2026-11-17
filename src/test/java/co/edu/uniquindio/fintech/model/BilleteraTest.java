package co.edu.uniquindio.fintech.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BilleteraTest {

    private static Billetera ahorro() {
        return new Billetera("BIL-000001", "1094000111", "Mi ahorro", TipoBilletera.AHORRO,
                new BigDecimal("2500.50"), EstadoBilletera.ACTIVA);
    }

    @Test
    void construyeConTodosSusCampos() {
        Billetera billetera = ahorro();
        assertEquals("BIL-000001", billetera.codigo());
        assertEquals("1094000111", billetera.identificacionPropietario());
        assertEquals("Mi ahorro", billetera.nombre());
        assertEquals(TipoBilletera.AHORRO, billetera.tipo());
        assertEquals(EstadoBilletera.ACTIVA, billetera.estado());
    }

    @Test
    void conservaElValorMonetarioExacto() {
        BigDecimal saldo = ahorro().saldo();
        assertEquals(new BigDecimal("2500.50"), saldo);
        assertEquals(2, saldo.scale());
        assertEquals(0, new BigDecimal("0.1").add(new BigDecimal("0.2")).compareTo(
                ahorro().conSaldo(new BigDecimal("0.3")).saldo()));
    }

    @Test
    void seRelacionaConSuPropietarioPorIdentificacion() {
        Usuario propietario = new Usuario("1094000111", "Ana", "a@b.co", 0, NivelUsuario.BRONCE);
        assertEquals(propietario.identificacion(), ahorro().identificacionPropietario());
    }

    @Test
    void cambiosProducenCopias() {
        Billetera original = ahorro();
        Billetera inactiva = original.conEstado(EstadoBilletera.INACTIVA);
        Billetera recargada = original.conSaldo(new BigDecimal("3000.00"));
        Billetera renombrada = original.conDatos("Viajes", TipoBilletera.TRANSPORTE);

        assertEquals(EstadoBilletera.INACTIVA, inactiva.estado());
        assertEquals(original.saldo(), inactiva.saldo());
        assertEquals(new BigDecimal("3000.00"), recargada.saldo());
        assertEquals("Viajes", renombrada.nombre());
        assertEquals(TipoBilletera.TRANSPORTE, renombrada.tipo());
        assertEquals(original.codigo(), renombrada.codigo());
        assertEquals(EstadoBilletera.ACTIVA, original.estado());
        assertEquals(new BigDecimal("2500.50"), original.saldo());
    }

    @Test
    void unaBilleteraInactivaConservaSusDatos() {
        Billetera inactiva = ahorro().conEstado(EstadoBilletera.INACTIVA);
        assertEquals("BIL-000001", inactiva.codigo());
        assertEquals("Mi ahorro", inactiva.nombre());
    }

    @Test
    void validaElFormatoDelCodigo() {
        for (String invalido : new String[]{"BIL-1", "BIL-00001", "TRX-000001", "bil-000001", "BIL000001", ""}) {
            assertThrows(IllegalArgumentException.class, () -> new Billetera(invalido, "1", "n",
                    TipoBilletera.AHORRO, BigDecimal.ZERO, EstadoBilletera.ACTIVA), invalido);
        }
        // El consecutivo puede crecer más allá de 6 dígitos.
        assertEquals("BIL-1000000", new Billetera("BIL-1000000", "1", "n", TipoBilletera.AHORRO,
                BigDecimal.ZERO, EstadoBilletera.ACTIVA).codigo());
    }

    @Test
    void rechazaCamposObligatoriosAusentes() {
        assertThrows(NullPointerException.class, () -> new Billetera(null, "1", "n",
                TipoBilletera.AHORRO, BigDecimal.ZERO, EstadoBilletera.ACTIVA));
        assertThrows(NullPointerException.class, () -> new Billetera("BIL-000001", null, "n",
                TipoBilletera.AHORRO, BigDecimal.ZERO, EstadoBilletera.ACTIVA));
        assertThrows(IllegalArgumentException.class, () -> new Billetera("BIL-000001", "1", " ",
                TipoBilletera.AHORRO, BigDecimal.ZERO, EstadoBilletera.ACTIVA));
        assertThrows(NullPointerException.class, () -> new Billetera("BIL-000001", "1", "n",
                null, BigDecimal.ZERO, EstadoBilletera.ACTIVA));
        assertThrows(NullPointerException.class, () -> new Billetera("BIL-000001", "1", "n",
                TipoBilletera.AHORRO, null, EstadoBilletera.ACTIVA));
        assertThrows(NullPointerException.class, () -> new Billetera("BIL-000001", "1", "n",
                TipoBilletera.AHORRO, BigDecimal.ZERO, null));
    }
}
