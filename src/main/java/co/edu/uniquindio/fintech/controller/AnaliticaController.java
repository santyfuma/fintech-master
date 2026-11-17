package co.edu.uniquindio.fintech.controller;

import co.edu.uniquindio.fintech.benchmark.BenchmarkEstructuras;
import co.edu.uniquindio.fintech.benchmark.ComparacionBenchmark;
import co.edu.uniquindio.fintech.dto.AristaRespuesta;
import co.edu.uniquindio.fintech.dto.BenchmarkRespuesta;
import co.edu.uniquindio.fintech.dto.CiclosRespuesta;
import co.edu.uniquindio.fintech.dto.ConteoRespuesta;
import co.edu.uniquindio.fintech.dto.GrafoRespuesta;
import co.edu.uniquindio.fintech.dto.MedicionBenchmarkRespuesta;
import co.edu.uniquindio.fintech.dto.MontoMovilizadoRespuesta;
import co.edu.uniquindio.fintech.dto.RutaRespuesta;
import co.edu.uniquindio.fintech.dto.TransaccionRespuesta;
import co.edu.uniquindio.fintech.dto.UsuarioRespuesta;
import co.edu.uniquindio.fintech.service.AnaliticaService;
import co.edu.uniquindio.fintech.service.CriterioGrafo;
import co.edu.uniquindio.fintech.service.GrafoService;
import co.edu.uniquindio.fintech.service.UsuarioService;
import co.edu.uniquindio.fintech.structures.interfaces.Arista;
import co.edu.uniquindio.fintech.structures.interfaces.GrafoDirigidoPonderado;
import co.edu.uniquindio.fintech.structures.interfaces.Lista;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/** Reportes (API.md §9), grafo de transferencias (§10) y benchmark (§11). */
@RestController
public class AnaliticaController {

    static final String LIMITACIONES = "Medición con System.nanoTime en la misma JVM que atiende la API: incluye "
            + "efectos de JIT, recolector de basura y otros hilos. Se descarta una ejecución de calentamiento, pero "
            + "los tiempos sirven solo para comparar órdenes de magnitud, no como benchmark riguroso (no es JMH).";

    private final AnaliticaService analitica;
    private final GrafoService grafo;
    private final UsuarioService usuarios;
    private final BenchmarkEstructuras benchmark;

    public AnaliticaController(AnaliticaService analitica, GrafoService grafo, UsuarioService usuarios,
                               BenchmarkEstructuras benchmark) {
        this.analitica = analitica;
        this.grafo = grafo;
        this.usuarios = usuarios;
        this.benchmark = benchmark;
    }

    private static ConteoRespuesta[] conteos(Lista<AnaliticaService.Conteo> lista) {
        return Arreglos.de(lista, c -> new ConteoRespuesta(c.clave(), c.cantidad()), ConteoRespuesta[]::new);
    }

    // ---------------------------------------------------------------- reportes

    @GetMapping("/api/reportes/billeteras-mas-usadas")
    public ConteoRespuesta[] billeterasMasUsadas(@RequestParam(name = "cantidad", defaultValue = "5") int cantidad) {
        return conteos(analitica.billeterasMasUsadas(cantidad));
    }

    @GetMapping("/api/reportes/usuarios-mas-transferencias")
    public ConteoRespuesta[] usuariosMasTransferencias(
            @RequestParam(name = "cantidad", defaultValue = "5") int cantidad) {
        return conteos(analitica.usuariosConMasTransferencias(cantidad));
    }

    @GetMapping("/api/reportes/categorias-activas")
    public ConteoRespuesta[] categoriasActivas() {
        return conteos(analitica.categoriasMasActivas());
    }

    @GetMapping("/api/reportes/frecuencia-por-tipo")
    public ConteoRespuesta[] frecuenciaPorTipo() {
        return conteos(analitica.frecuenciaPorTipo());
    }

    @GetMapping("/api/reportes/monto-movilizado")
    public MontoMovilizadoRespuesta montoMovilizado(
            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        AnaliticaService.MontoPeriodo m = analitica.montoMovilizado(desde, hasta);
        return new MontoMovilizadoRespuesta(m.desde(), m.hasta(), m.total(), m.transacciones());
    }

    @GetMapping("/api/reportes/usuario-mas-activo")
    public ConteoRespuesta usuarioMasActivo(
            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        AnaliticaService.Conteo c = analitica.usuarioMasActivo(desde, hasta);
        return new ConteoRespuesta(c.clave(), c.cantidad());
    }

    @GetMapping("/api/reportes/transacciones-mayor-valor")
    public TransaccionRespuesta[] mayorValor(@RequestParam(name = "cantidad", defaultValue = "5") int cantidad) {
        return Arreglos.de(analitica.transaccionesDeMayorValor(cantidad), TransaccionRespuesta::desde,
                TransaccionRespuesta[]::new);
    }

    @GetMapping("/api/reportes/usuarios-por-puntos")
    public UsuarioRespuesta[] usuariosPorPuntos() {
        return Arreglos.de(usuarios.ordenadosPorPuntos(), UsuarioRespuesta::desde, UsuarioRespuesta[]::new);
    }

    // ---------------------------------------------------------------- grafo

    @GetMapping("/api/grafo/transferencias")
    public GrafoRespuesta transferencias(@RequestParam(name = "criterio", required = false) CriterioGrafo criterio) {
        CriterioGrafo efectivo = grafo.criterio(criterio);
        GrafoDirigidoPonderado<String> g = grafo.grafo(efectivo);
        return new GrafoRespuesta(efectivo.name(), Arreglos.textos(g.vertices()), aristas(g.aristas()), g.tieneCiclo());
    }

    @GetMapping("/api/grafo/rutas")
    public RutaRespuesta rutas(@RequestParam("origen") String origen, @RequestParam("destino") String destino,
                               @RequestParam(name = "criterio", required = false) CriterioGrafo criterio) {
        CriterioGrafo efectivo = grafo.criterio(criterio);
        GrafoService.Rutas r = grafo.rutas(origen, destino, efectivo);
        return new RutaRespuesta(origen, destino, efectivo.name(), Arreglos.textos(r.menosSaltos()),
                Arreglos.textos(r.menorPeso()), r.pesoMenor());
    }

    @GetMapping("/api/grafo/rutas-frecuentes")
    public AristaRespuesta[] rutasFrecuentes(@RequestParam(name = "cantidad", defaultValue = "5") int cantidad,
                                             @RequestParam(name = "criterio", required = false) CriterioGrafo criterio) {
        return aristas(grafo.relacionesFrecuentes(cantidad, criterio));
    }

    @GetMapping("/api/grafo/ciclos")
    public CiclosRespuesta ciclos(@RequestParam("usuario") String usuario,
                                  @RequestParam(name = "longitudMaxima", defaultValue = "4") int longitudMaxima) {
        String[][] ciclos = Arreglos.de(grafo.ciclosQueIncluyen(usuario, longitudMaxima), Arreglos::textos,
                String[][]::new);
        return new CiclosRespuesta(usuario, longitudMaxima, ciclos);
    }

    private static AristaRespuesta[] aristas(Lista<Arista<String>> lista) {
        return Arreglos.de(lista, a -> new AristaRespuesta(a.origen(), a.destino(), a.peso()), AristaRespuesta[]::new);
    }

    // ---------------------------------------------------------------- benchmark

    @GetMapping("/api/benchmark")
    public BenchmarkRespuesta benchmark(@RequestParam("comparacion") ComparacionBenchmark comparacion,
                                        @RequestParam(name = "elementos", defaultValue = "1000") int elementos,
                                        @RequestParam(name = "repeticiones", defaultValue = "5") int repeticiones) {
        MedicionBenchmarkRespuesta[] m = Arreglos.de(benchmark.ejecutar(comparacion, elementos, repeticiones),
                x -> new MedicionBenchmarkRespuesta(x.operacion(), x.estructura(), x.elementos(), x.repeticiones(),
                        x.promedioNanos(), x.minimoNanos(), x.maximoNanos(), x.resultado()),
                MedicionBenchmarkRespuesta[]::new);
        return new BenchmarkRespuesta(comparacion.name(), elementos, repeticiones, m, LIMITACIONES);
    }
}
