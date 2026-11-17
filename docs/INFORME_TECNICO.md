# Informe técnico — Plataforma Fintech de billeteras digitales

Proyecto final de Estructuras de Datos. Este documento describe el sistema tal como está
implementado al cierre de la fase 6. Sus fuentes son el enunciado del profesor
(*Proyecto_Final_estructuras_de_datos_noche_2026-2.pdf*, citado como "enunciado" con su
numeración), el código y las pruebas. `CONTEXTO.md` conserva el detalle de diseño de
las fases 1 a 5. `API.md` es el contrato HTTP. `diagrama-clases.md` contiene el diagrama
de clases y `ejemplos-api.md` los casos de ejecución.

---

## 1. Introducción

El proyecto es una plataforma académica de billeteras digitales: usuarios con varias
billeteras, recargas, retiros, transferencias, operaciones programadas, puntos y
niveles, reversiones, alertas, detección de actividad inusual y analítica. El objetivo
del curso no es construir un banco. Es aplicar estructuras de datos a problemas
concretos y justificar cada elección. Por eso cada funcionalidad se apoya en una
estructura definida en el paquete `structures`: lista enlazada, pila, cola, cola de
prioridad, tabla hash, árbol binario de búsqueda y grafo dirigido ponderado.

El sistema es una **simulación académica**:
- No tiene autenticación: cualquier cliente puede operar sobre cualquier usuario.
- No mueve dinero real.
- No está preparado para producción.

## 2. Descripción del problema

Una billetera digital debe registrar movimientos de dinero sin perder ni duplicar
saldo, incluso si una escritura falla a mitad de camino. Además debe:

- **Priorizar** operaciones futuras por fecha (enunciado 4.3).
- **Deshacer** operaciones recientes (4.6).
- **Atender en orden** las notificaciones (4.7).
- **Buscar** rápidamente usuarios y billeteras (5).
- **Ordenar** por puntos o valor (4.5, 8).
- **Analizar** las relaciones entre usuarios como una red (4.8, 8).
- **Detectar** patrones inusuales a partir del historial de cada usuario (4.9).

Cada necesidad tiene una estructura natural, y el proyecto la hace explícita.

## 3. Objetivos

- Implementar los 12 requisitos funcionales del enunciado (sección 6) y los requisitos
  adicionales (sección 8).
- Usar estructuras propias detrás de interfaces propias. La única excepción son los
  adaptadores sobre `java.util`, permitidos solo dentro de `structures`.
- Mantener la integridad financiera:
  - Montos en `BigDecimal`.
  - Operaciones atómicas sobre varios archivos mediante un diario de rehacer.
  - Validaciones de saldo, existencia y consistencia.
- Exponer todo por una API REST y una interfaz gráfica web.
- Documentar cada estructura con su justificación y su uso real.

## 4. Requisitos funcionales

| # | Requisito (enunciado 6) | Dónde se cumple |
|---|---|---|
| 1 | Registrar, modificar, buscar y eliminar usuarios | `UsuarioService`, `/api/usuarios` |
| 2 | Crear y administrar múltiples billeteras | `BilleteraService`, `/api/usuarios/{id}/billeteras`, `/api/billeteras/{codigo}` |
| 3 | Recargas, retiros y transferencias | `TransaccionService`, `/api/transacciones/*` |
| 4 | Historial de movimientos | `/api/usuarios/{id}/transacciones`, `/api/billeteras/{codigo}/transacciones` |
| 5 | Programar operaciones futuras | `OperacionProgramadaService.programar` |
| 6 | Procesar automáticamente las programadas | `EjecucionAutomaticaProgramadas` (tarea periódica) y `POST /api/operaciones-programadas/procesar` |
| 7 | Acumular y descontar puntos | `PuntosService`, reversión y canje |
| 8 | Asignar nivel según puntos | `PuntosService.nivelPara` (rangos del enunciado 4.5) |
| 9 | Revertir operaciones permitidas | `TransaccionService.revertir` / `revertirUltima` |
| 10 | Alertas automáticas | `NotificacionService` + generación dentro de cada operación |
| 11 | Reportes e indicadores | `AnaliticaService`, `/api/reportes/*` |
| 12 | Relaciones de transacción entre usuarios | `GrafoService`, `/api/grafo/*` |

Requisitos adicionales (enunciado 8):

| Requisito | Dónde se cumple |
|---|---|
| Recálculo de puntos al revertir | `TransaccionService.revertir` |
| Usuario más activo en un período | `AnaliticaService.usuarioMasActivo` |
| Ciclos y rutas frecuentes del grafo | `GrafoService.ciclosQueIncluyen` y `relacionesFrecuentes` |
| Transacciones de mayor valor con un ABB | `RepositorioTransacciones.mayoresPorValor` |
| Simulación de la ejecución automática | Reloj inyectado + procesamiento manual o periódico |
| Comparación de rendimiento | paquete `benchmark` |

## 5. Arquitectura

Aplicación Spring Boot 4.1.2 (Java 21, Maven) en capas:

```
static (HTML/CSS/JS) ──HTTP/JSON──► controller ──► service ──► repository ──► persistence (JSON)
                                         │             │             │
                                        dto          model      structures (interfaces)
                                                                     ▲
                                              config: FabricaEstructurasPropias (implementaciones)
```

- **controller**: es delgado. Valida la entrada con `@Valid`, llama a un servicio y
  convierte los resultados a DTO (`record` con arreglos). No accede a repositorios ni a
  persistencia.
- **service**: concentra todas las reglas de negocio y las políticas configurables. Toda
  modificación pasa por `EscrituraConDiario`.
- **repository**: guarda los datos en memoria en estructuras propias, indexados, y los
  persiste. No contiene lógica financiera.
- **persistence**:
  - Archivos JSON con escritura atómica.
  - Diario de rehacer (`diario-operaciones.json`).
  - Generador de identificadores.
  - Verificador de integridad.
- **structures**: interfaces propias, implementaciones propias (lista enlazada, ABB,
  grafo) y adaptadores. Es el único paquete que usa `java.util`, y no depende del
  modelo.
- **config**: cablea los beans, lee las políticas desde `application.properties`, provee
  el `Clock` y carga y recupera los datos al arrancar.

Reglas verificadas automáticamente por `ArquitecturaTest` y `ArquitecturaCapasTest`:
- `java.util` solo en `structures`.
- `service` y `repository` dependen de interfaces, no de implementaciones.
- `controller` no accede a `repository` ni a `persistence`.
- `dto` solo depende de `model`, validación y `java.math`/`java.time`.

El paquete `benchmark` usa implementaciones concretas a propósito, porque su función es
compararlas.

**Concurrencia.** Todas las modificaciones toman el mismo monitor (`CerrojoEscritura`).
Así los identificadores se generan y escriben en orden, y dos operaciones nunca leen el
mismo saldo para modificarlo. Las consultas leen la memoria sin bloquear. Es una
decisión deliberada de simplicidad: la carga esperada es académica.

## 6. Modelo de dominio

Las entidades son `record` inmutables, que validan sus invariantes en el constructor
compacto y se modifican con métodos `con…` que devuelven copias.

| Entidad | Datos principales |
|---|---|
| `Usuario` | identificación, nombre, correo, puntos, nivel |
| `Billetera` | código `BIL-NNNNNN`, propietario, nombre, tipo (6 categorías del enunciado), saldo `BigDecimal`, estado |
| `Transaccion` | `TRX-NNNNNN`, fecha, tipo (RECARGA, RETIRO, TRANSFERENCIA, REVERSION), valor, billeteras, estado (EXITOSA, RECHAZADA, REVERTIDA), puntos, nivel de riesgo, transacción revertida |
| `OperacionProgramada` | `OPR-NNNNNN`, tipo, valor, billeteras, fecha de ejecución, periodicidad, estado |
| `Notificacion` | `NOT-NNNNNN`, usuario, tipo (los 5 del enunciado 4.7 + alerta de seguridad), estado |
| `EventoAuditoria` | `AUD-NNNNNN`, usuario, transacción, patrón (los 6 del enunciado 4.9), riesgo |
| `BeneficioCanjeado` | `BEN-NNNNNN`, usuario, beneficio, puntos descontados |

Las relaciones entre entidades se expresan por identificador, no por referencia de
objeto. Así cada archivo JSON es independiente y el verificador de integridad puede
comprobar las referencias al arrancar.

## 7. Estructuras de datos utilizadas

| Estructura | Implementación | Tipo |
|---|---|---|
| Lista | `ListaEnlazada` (doblemente enlazada) | propia |
| Pila | `PilaAdaptador` (sobre `ArrayDeque`) | adaptador |
| Cola | `ColaAdaptador` (sobre `ArrayDeque`) | adaptador |
| Cola de prioridad | `ColaPrioridadAdaptador` (montículo de `PriorityQueue`, con `Comparador` propio) | adaptador |
| Tabla hash | `TablaHashAdaptador` (sobre `HashMap`) | adaptador |
| Árbol binario de búsqueda | `ArbolBinarioBusquedaImpl` (iterativo, sin balanceo) con `ClaveCompuesta` | propia |
| Grafo dirigido ponderado | `GrafoDirigidoPonderadoImpl` (listas de adyacencia, pesos `BigDecimal`) | propia |

Las implementaciones se obtienen por medio de `FabricaEstructuras`; los servicios y
repositorios solo conocen las interfaces. Las complejidades detalladas de cada operación
están en `CONTEXTO.md` 6.2.1 y 6.3.1.

## 8. Justificación de cada estructura

La complejidad se expresa con:
- *n*: número de elementos.
- *h*: altura del ABB. Es *O(log n)* en promedio y *O(n)* en el peor caso, porque el
  árbol no se balancea.
- *V* y *E*: vértices y aristas del grafo.

### 8.1 Lista enlazada (`ListaEnlazada`)
1. **Requisito.** Historiales y resultados de consultas (enunciado 4.2, 6.4), y orden de
   inserción de cada repositorio.
2. **Datos.** Transacciones de un usuario o billetera, resultados de reportes, rutas y
   ciclos del grafo, orden de los registros.
3. **Operaciones.**
   - `agregar` al final: *O(1)*.
   - Recorrido: *O(n)*.
   - `obtener(i)`: *O(min(i, n−i))*, porque recorre desde el extremo más cercano.
4. **Por qué es apropiada.** Los historiales crecen por el final y se leen completos y en
   orden. No se necesita acceso aleatorio frecuente, y la lista doble evita copiar
   arreglos al crecer.
5. **Uso real.**
   - `RepositorioEnMemoria.orden`.
   - Listas de `IndicePorClave` (transacciones por billetera y por usuario).
   - Todo resultado de servicio que devuelve `Lista`.

### 8.2 Pila (`PilaAdaptador`)
1. **Requisito.** Deshacer la última operación reversible (enunciado 4.6 y 5, "manejar
   operaciones reversibles").
2. **Datos.** Las transacciones reversibles del usuario, en orden cronológico.
3. **Operaciones.**
   - `apilar`, `cima` y `desapilar`: *O(1)*.
   - Reconstrucción: *O(k)* sobre el historial del usuario.
4. **Por qué es apropiada.** "Deshacer lo último" es exactamente LIFO.
5. **Uso real.** `RepositorioTransacciones.reconstruirPilaReversibles`, usada por
   `TransaccionService.revertirUltima`.
   - La pila se reconstruye en cada consulta con la `PoliticaReversion` vigente.
   - Así refleja reversiones ya hechas y plazos vencidos, y sobrevive a los reinicios sin
     persistirse (`CONTEXTO.md` 7.5).

### 8.3 Cola (`ColaAdaptador`)
1. **Requisito.** "Atender notificaciones pendientes de envío" (enunciado 5).
2. **Datos.** Identificadores de notificaciones PENDIENTE.
3. **Operaciones.** `encolar`, `frente` y `desencolar`: *O(1)*.
4. **Por qué es apropiada.** Las notificaciones se despachan en el orden en que se
   generaron (FIFO).
5. **Uso real.** `RepositorioNotificaciones.colaPendientes`, consumida por
   `NotificacionService.despacharPendientes`. Una tabla hash `encoladas` evita duplicados.

### 8.4 Cola de prioridad (`ColaPrioridadAdaptador`)
1. **Requisito.** Procesar las programadas "respetando la prioridad según la fecha y hora
   de ejecución" (enunciado 4.3). También sirve para los "top N" de los reportes.
2. **Datos.** Operaciones PENDIENTE (prioridad: fecha y luego identificador) y conteos de
   los reportes.
3. **Operaciones.**
   - `insertar` y `extraer`: *O(log n)*.
   - `consultar`: *O(1)*.
4. **Por qué es apropiada.** El servicio solo necesita la próxima operación vencida.
   - Un montículo la entrega sin ordenar todo el conjunto.
   - En los reportes se extraen solo los primeros *k*, en *O(n + k log n)*.
5. **Uso real.**
   - `RepositorioOperacionesProgramadas.pendientes`.
   - `OperacionProgramadaService.procesarVencidas`.
   - `AnaliticaService.primeros`.
   - `GrafoService.relacionesFrecuentes`.

### 8.5 Tabla hash (`TablaHashAdaptador`)
1. **Requisito.** "Consultar saldos, puntos o configuraciones de forma rápida"
   (enunciado 5).
2. **Datos.**
   - Cada entidad por su identificador.
   - Índices secundarios (billeteras por propietario, transacciones por billetera y por
     usuario).
   - Conteos de analítica.
   - Catálogo de beneficios.
3. **Operaciones.** `poner`, `obtener` y `contieneClave`: *O(1)* promedio.
4. **Por qué es apropiada.** Casi todas las operaciones empiezan buscando un usuario, una
   billetera o una transacción por su clave exacta.
5. **Uso real.**
   - `RepositorioEnMemoria.porIdentificador`.
   - `IndicePorClave`.
   - `AnaliticaService` (conteos).
   - `DeteccionService` (billeteras origen distintas).
   - `CatalogoBeneficios`.

### 8.6 Árbol binario de búsqueda (`ArbolBinarioBusquedaImpl`)
1. **Requisito.**
   - "Organizar usuarios según puntos acumulados" y "generar reportes ordenados"
     (enunciado 5).
   - "Transacciones de mayor valor usando estructuras ordenadas" (enunciado 8).
2. **Datos.**
   - Usuarios con clave `(puntos, identificación)`.
   - Transacciones con clave `(valor, id)`.
   - `ClaveCompuesta` desempata valores iguales.
3. **Operaciones.**
   - `insertar`, `buscar` y `eliminar`: *O(h)*.
   - `inOrden`: *O(n)*.
   - `rango(a, b)`: *O(h + k)*.
   - `mayores(k)`: *O(h + k)*.
4. **Por qué es apropiada.** Mantiene el orden mientras se inserta. Las consultas por
   rango de puntos y los "top N" por valor no necesitan reordenar cada vez.
5. **Uso real.**
   - `RepositorioUsuarios.porPuntos`, que alimenta `ordenadosPorPuntos`,
     `porRangoDePuntos` y `conMasPuntos`.
   - `RepositorioTransacciones.porValor`, que alimenta `mayoresPorValor` y
     `porRangoDeValor`.
   - Cuando cambian los puntos de un usuario, se elimina su clave y se reinserta.

### 8.7 Grafo dirigido ponderado (`GrafoDirigidoPonderadoImpl`)
1. **Requisito.** "Patrones de movimiento entre usuarios" (4.8), "analizar relaciones de
   transacción" (6.12) y "ciclos o rutas frecuentes" (8).
2. **Datos.**
   - Vértices: usuarios.
   - Aristas: transferencias EXITOSAS entre usuarios distintos.
   - Peso: cantidad de transferencias o monto acumulado (`CriterioGrafo`).
3. **Operaciones.**
   - `incrementarPeso`: *O(grado)*.
   - BFS (menos saltos): *O(V + E)*.
   - Dijkstra (menor peso): *O(V² + E)*.
   - Detección de ciclo (DFS): *O(V + E)*.
   - Ciclos que incluyen un vértice: búsqueda acotada por longitud máxima.
4. **Por qué es apropiada.** Las preguntas son de red: quién envía a quién, si hay ruta y
   si el dinero vuelve al origen. Una tabla no las responde de forma natural.
5. **Uso real.**
   - `RepositorioTransacciones.construirGrafoTransferencias` construye el grafo bajo
     demanda, con un criterio de peso.
   - `GrafoService` lo usa en rutas, relaciones frecuentes y ciclos.

## 9. Persistencia

- Hay un archivo JSON por entidad (`usuarios.json`, `billeteras.json`, …) en el
  directorio `fintech.datos.directorio` (por defecto `data/`). No se usan bases de datos,
  JPA ni Hibernate.
- **Escritura atómica.** Cada archivo se reescribe en un temporal del mismo directorio,
  se fuerza a disco y se reemplaza con `ATOMIC_MOVE`.
- **Diario de rehacer.** Las operaciones que cambian varios archivos se registran
  primero en `diario-operaciones.json`. Por ejemplo, una transferencia cambia dos
  billeteras, una transacción, el usuario, varias notificaciones y eventos de auditoría.
  - Cada entrada guarda la imagen final de cada registro como texto JSON exacto, para
    conservar la escala de los `BigDecimal`.
  - Luego las imágenes se aplican a cada archivo y la entrada se confirma.
  - Aplicar una imagen es idempotente: reemplaza el registro por su identificador. Por
    eso repetir una entrada interrumpida es seguro.
- **Fase 6: `EscrituraConDiario`.** Agrega dos garantías que el diario solo no daba:
  - Si la escritura falla **después** de registrar la entrada, la operación está
    comprometida. La memoria se actualiza y se informa un error 500 genérico.
  - Mientras quede una entrada pendiente, ninguna otra modificación se acepta sin
    reaplicarla antes. Esto evita que una operación posterior confirmada sea luego pisada
    por imágenes antiguas al recuperar el diario.
- **Arranque.** `InicializadorDatos` recupera el diario y verifica la integridad: formato
  de identificadores, referencias entre archivos y orden de identificadores. Después
  construye los repositorios y el generador de identificadores.

## 10. Reglas de negocio

Las reglas viven solo en `service`. Cada valor que el enunciado no fija es una política
configurable en `application.properties`. Sus valores por defecto (provisionales) están
en un único lugar: el método `provisional()` de cada política. La sección 17 y la tabla
de decisiones lo detallan.

- **Movimientos.** El valor debe ser mayor que 0.
  - **Errores de la solicitud: 400, no se registran.** Valor inválido, misma billetera o
    billetera faltante.
  - **Billetera inexistente: 404.**
  - **Rechazos de negocio: se registran.** Billetera inactiva o saldo insuficiente
    generan una transacción RECHAZADA y una notificación OPERACION_RECHAZADA, y luego se
    responde 409.
- **Puntos (enunciado 4.4).**
  - Recarga: 1 punto por cada 100.
  - Retiro: 2 por cada 100.
  - Transferencia: 3 por cada 100.
  - Se usa división entera: 250.99 da 2 puntos (provisional).
  - Bono por programada exitosa: 10 (provisional).
  - Solo suma puntos quien origina la operación.
- **Niveles (enunciado 4.5).** Bronce 0–500, Plata 501–1000, Oro 1001–5000, Platino
  más de 5000.
  - El nivel se recalcula siempre con los puntos actuales.
  - Al subir de nivel se notifica ASCENSO_NIVEL.
  - Un canje o una reversión pueden bajar el nivel.
- **Reversión (enunciado 4.6).**
  - Reversibles por defecto: recargas y transferencias EXITOSAS dentro de 24 horas
    (provisional).
  - Una reversión mueve el dinero en sentido contrario y marca la original REVERTIDA.
  - Crea una transacción REVERSION que la referencia.
  - Descuenta los puntos que la original generó, sin dejar puntos negativos. La reversión
    registra como `puntosGenerados` el negativo de lo descontado realmente.
  - Si la billetera que recibió el dinero ya no lo tiene, se responde 409.
- **Programadas (enunciado 4.3).**
  - La fecha debe ser futura según el `Clock`.
  - Las vencidas se ejecutan por fecha y luego por identificador.
  - Cada ejecución es una entrada del diario: la transacción y el nuevo estado de la
    operación quedan juntos.
  - Una periódica genera la siguiente ocurrencia (+1 semana o +1 mes), aunque la actual
    falle.
  - Se avisa una sola vez de las que vencen dentro de 24 horas (provisional).
- **Detección (enunciado 4.9).** Seis patrones sobre el historial efectivo propio del
  usuario. Las rechazadas y las reversiones no cuentan.
  - La transacción toma el mayor riesgo encontrado.
  - Cada patrón genera un `EventoAuditoria` y una notificación ALERTA_SEGURIDAD.
  - Los umbrales son provisionales (`PoliticaDeteccion`).
- **Alertas.**
  - Saldo bajo: la billetera de origen queda por debajo de 10 000 (provisional).
  - Programada próxima.
  - Operación rechazada.
  - Ascenso de nivel.
  - Canje realizado.
  - Alerta de seguridad.
- **Usuarios y billeteras.**
  - No se elimina un usuario con billeteras, canjes, notificaciones o auditoría.
  - No se desactiva una billetera con saldo.
  - Las billeteras nunca se borran físicamente.

## 11. Servicios implementados

| Servicio | Responsabilidad |
|---|---|
| `UsuarioService` | CRUD de usuarios, consultas por puntos y nivel, siguiente nivel |
| `BilleteraService` | Crear, consultar, actualizar, activar y desactivar billeteras |
| `TransaccionService` | Recarga, retiro, transferencia, reversión, deshacer la última (pila), historial, ejecución de programadas |
| `OperacionProgramadaService` | Programar, cancelar, consultar y procesar vencidas (manual o periódica); avisos de próximas |
| `PuntosService` | Cálculo de puntos, nivel por puntos, ajuste sin negativos |
| `RecompensaService` | Ventajas por nivel, catálogo, canje de puntos |
| `NotificacionService` | Notificaciones, historial reciente, despacho FIFO |
| `DeteccionService` | Seis patrones de actividad inusual y consulta de auditoría |
| `AnaliticaService` | Reportes del enunciado 4.8 y 8 |
| `GrafoService` | Grafo de transferencias, rutas, relaciones frecuentes, ciclos |
| `EscrituraConDiario`, `ActualizadorMemoria` | Escritura atómica de varios registros y su reflejo en memoria |
| `BenchmarkEstructuras` (paquete `benchmark`) | Comparación de estructuras |

## 12. API REST

El contrato completo está en `API.md`. Hay 55 endpoints bajo `/api`, agrupados en
usuarios, billeteras, transacciones y reversiones, programadas, recompensas,
notificaciones, auditoría, reportes, grafo y benchmark.

- Todas las respuestas son JSON con colecciones como arreglos.
- Los errores tienen el cuerpo uniforme `{estado, error, mensaje, fecha}` generado por
  `ManejadorGlobalExcepciones`. El código `error` es estable (p. ej.
  `SALDO_INSUFICIENTE`), y los errores 500 no exponen detalles internos.
- `docs/pruebas.http` contiene una secuencia ejecutable de solicitudes.

## 13. Interfaz gráfica

Es una aplicación web en `src/main/resources/static`, en HTML, CSS y JavaScript sin
frameworks, servida por la misma aplicación en `http://localhost:8080/`. Se comunica solo
por `fetch` con la API y no contiene reglas financieras: muestra lo que calcula el
servidor.

- **Estructura.**
  - Dashboard con saldo total (calculado por el servidor), puntos, nivel y actividad.
  - Barra lateral con las 10 secciones.
  - Barra superior con el **usuario seleccionado**, que guía la navegación entre sus
    billeteras y movimientos.
  - Tarjetas, tablas, formularios en modales, mensajes y cargador.
  - Diseño adaptable a móvil.
- **Pantallas.** Dashboard, Usuarios, Billeteras, Transacciones, Programadas, Puntos y
  beneficios, Notificaciones, Auditoría, Analítica y grafo (incluye rutas y ciclos) y
  Benchmark.
- **Envíos dobles.** Cada botón se deshabilita mientras su solicitud está en curso
  (`Ui.conBloqueo`).
- **Seguridad del contenido.** El contenido se inserta con `textContent`, nunca con
  `innerHTML` de datos, para evitar inyección de HTML.
- **Aviso.** La barra lateral indica que es una simulación académica sin autenticación.

## 14. Pruebas realizadas

**Entorno de esta sesión.**
- Maven no pudo descargar dependencias: el repositorio central está bloqueado por la red
  del entorno.
- La compilación se hizo con `javac` (Java 21) contra **sustitutos mínimos** de JUnit,
  Spring y Jackson 3. Los sustitutos de Jackson 3 delegan en Jackson 2.16.
- Las pruebas se ejecutaron con un ejecutor propio que emula `@Test`, `@BeforeEach` y
  `@TempDir`.
- Esos resultados demuestran la lógica, pero **no** sustituyen `./mvnw clean test`, que
  debe correrse en un equipo con acceso a Maven Central.

| Conjunto | Pruebas (`@Test`) | Resultado en esta sesión |
|---|---|---|
| Fases 1–5 (estructuras, modelo, DTO, persistencia, repositorios, arquitectura) | 326 | 325 pasan; 1 requiere Hibernate Validator real |
| Fase 6: servicios (`service/*Test`) | 85 | 85 pasan |
| Fase 6: benchmark | 3 | 3 pasan |
| Datos de demostración (`DatosDemoTest`) | 3 | 3 pasan |
| Fase 6: controladores invocados como objetos y generación de ejemplos | 2 | 2 pasan |
| Fase 6: MockMvc (`ControladoresMvcTest`) | 6 | **no ejecutadas aquí** (requieren Spring MVC real) |
| Fase 6: HTTP de punta a punta (`ApiHttpIntegracionTest`) | 1 | **no ejecutada aquí** (requiere Tomcat y Spring Boot reales) |
| Fases 4–5 con Spring/Hibernate Validator reales (`FintechApplicationTests`, `ArranqueAplicacionIntegracionTest`, `ValidacionSolicitudesTest`, `ManejadorGlobalExcepcionesMvcTest`) | 21 | **no ejecutadas aquí** |
| **Total** | **447** | **418 pasan, 1 requiere validador real, 28 no ejecutables aquí** |

**Ejecución con Maven real (entorno del equipo, Java 23, Maven 3.10.0).**
- Primera ejecución de `mvnw.cmd clean test`: 444 pruebas, 3 fallidas, 0 errores, 0
  omitidas.
- Las tres fallas tenían la misma causa, en el código de las pruebas y no en la
  aplicación:
  - `ArranqueAplicacionIntegracionTest` y `ApiHttpIntegracionTest` configuraban el
    directorio de datos con `SpringApplicationBuilder.properties(...)`.
  - Ese método define propiedades por defecto, que pierden frente a
    `src/test/resources/application.properties`.
  - Por eso la aplicación arrancaba sobre `target/datos-pruebas-contexto`, compartido
    entre pruebas, y no sobre el directorio temporal de cada una. El diario corrupto y la
    entrada pendiente quedaban en un directorio que nadie leía. En la prueba HTTP,
    `BIL-000001` era una billetera con saldo creada por otra prueba.
- Corrección: las propiedades se pasan como argumentos de línea de comandos, que tienen
  la máxima prioridad. No se cambió ninguna aserción.
- Queda pendiente confirmar el resultado con una nueva ejecución de Maven.

Qué cubren las pruebas de la fase 6:

- **Reglas financieras.**
  - Montos decimales: 0.10 + 0.20 = 0.30 y conservación de escala tras reiniciar.
  - Saldo exacto.
  - Rechazos registrados.
  - Errores de solicitud que no registran nada.
  - Puntos por tipo con división entera y bono.
  - Límites exactos de nivel (500/501, 1000/1001, 5000/5001).
  - Ascenso con notificación.
  - Saldo bajo.
- **Reversiones.**
  - Recarga y transferencia.
  - Retiro no reversible.
  - Doble reversión.
  - Reversión de una reversión.
  - Plazo vencido (con reloj ajustable).
  - Saldo insuficiente para devolver.
  - Billetera inactiva.
  - Puntos nunca negativos.
  - Orden de la pila, incluso tras reiniciar.
  - Pilas separadas por usuario.
- **Diario y fallos de persistencia.**
  - Fallo después de registrar: la operación queda comprometida y bloquea las
    siguientes; tras desbloquear, todo queda consistente.
  - Fallo antes de registrar: nada cambia.
  - Recuperación al reiniciar.
  - Fallo en escritura directa de un repositorio.
- **Programadas.**
  - No se ejecutan antes de tiempo.
  - Bono.
  - FALLIDA sin excepción.
  - Periódica semanal y mensual (31 de enero → 28 de febrero).
  - Orden por fecha y desempate por identificador.
  - Cancelación.
  - Aviso único de "próxima".
- **Detección.** Cada uno de los seis patrones con su riesgo:
  - Límite exacto del monto alto.
  - Combinación de patrones (gana el mayor riesgo).
  - Exclusión de rechazadas.
- **Notificaciones.** FIFO, despacho único, destinatario correcto.
- **Analítica y grafo.**
  - Solo cuentan las transacciones efectivas.
  - Desempates.
  - Períodos inválidos.
  - Pesos por cantidad y por monto.
  - Exclusión de transferencias internas y revertidas.
  - Rutas, ciclos y límites de los parámetros.
- **Concurrencia.**
  - 8 retiros simultáneos sobre 100: exactamente 3 exitosos y saldo final 10.
  - Transferencias cruzadas que conservan el total.
- **Controladores (MockMvc, escritas para el entorno real).**
  - Rutas, códigos 201/204/400/404/409.
  - `@Valid`, JSON mal formado, enum inválido, parámetros faltantes o mal tipados.
- **Integración HTTP.**
  - Arranque real en un puerto libre y flujo por HTTP.
  - Servicio del `index.html`.
  - Persistencia tras reiniciar.

**Verificación adicional:**
- Se aplicaron 5 mutaciones deliberadas a reglas críticas: las dos guardas del diario,
  la validación de saldo insuficiente, el umbral de fragmentación y la condición de
  fecha vencida. Las 5 fueron detectadas: 4 porque hacen fallar pruebas (entre 1 y 10
  cada una) y 1 porque vuelve infinito el procesamiento de periódicas.
- La interfaz se probó con Chrome sin interfaz (Playwright) contra una **API simulada**:
  - Las 10 pantallas cargan sin errores de JavaScript.
  - El doble clic envía una sola solicitud.
  - Los errores 409 se muestran con su código.
  - La vista móvil se ve correctamente.
- Esa prueba no reemplaza el uso contra el servidor real.

## 15. Resultados del benchmark

`GET /api/benchmark` compara estructuras en operaciones equivalentes:
- Mide con `System.nanoTime`.
- Descarta una ejecución de calentamiento.
- Repite *r* veces y reporta promedio, mínimo y máximo.
- Comprueba que todas las estructuras produzcan el mismo resultado.
- Los datos se generan con un generador congruencial propio con semilla fija, por lo que
  son reproducibles.

Medición en esta sesión: OpenJDK 21.0.12, 2 núcleos, 10 repeticiones. Tiempos promedio
en microsegundos por ejecución completa de *n* operaciones.

| Comparación | Estructura | n = 100 | n = 1000 | n = 5000 |
|---|---|---:|---:|---:|
| Búsqueda (n presentes + n ausentes) | Tabla hash | 116 | 240 | 793 |
| | ABB propio | 122 | 783 | 1 617 |
| Consulta ordenada (insertar n y listar) | Lista enlazada ordenada | 161 | 2 392 | 38 936 |
| | ABB + inorden | 187 | 296 | 1 717 |
| Prioridad (insertar n y extraer todo) | Cola de prioridad (montículo) | 115 | 347 | 1 108 |
| | Lista con mínimo lineal | 530 | 3 132 | 47 676 |
| Recorrido de grafo | BFS | 368 | 3 313 | 3 719 |
| | DFS | 275 | 1 423 | 1 867 |

Interpretación:
- Con *n* pequeño las diferencias se pierden en el costo fijo.
- Al crecer *n* se confirma lo esperado:
  - La tabla hash supera al ABB en búsqueda exacta (*O(1)* frente a *O(log n)*).
  - El ABB supera a la lista ordenada en inserción ordenada (*O(n log n)* frente a
    *O(n²)*).
  - El montículo supera a la búsqueda lineal del mínimo (*O(n log n)* frente a *O(n²)*).
- BFS y DFS son ambos *O(V + E)*. La diferencia observada se debe a constantes de
  implementación.

**Limitaciones:**
- Se mide en la misma JVM que atiende la API, con efectos de JIT y del recolector de
  basura.
- No es JMH.
- Los valores varían entre ejecuciones y equipos.
- Sirven para comparar órdenes de magnitud, no como cifras absolutas.

## 16. Casos de ejecución

`docs/ejemplos-api.md` contiene 14 casos con solicitudes y respuestas JSON:
1. Registro.
2. Billeteras.
3. Recarga.
4. Retiro con rechazo.
5. Transferencia.
6. Reversión.
7. Programación.
8. Ejecución simulada.
9. Puntos y canje.
10. Ascenso de nivel.
11. Detección de comportamiento inusual.
12. Analítica.
13. Rutas y ciclos.
14. Recuperación de una operación interrumpida.

Sobre su origen:
- Las respuestas las produjo la prueba `EjemplosApiTest`, que invoca en un mismo proceso
  los controladores, servicios, persistencia y manejador de errores reales, con reloj
  fijo. Son reproducibles: se regeneran en `target/ejemplos-api.md`.
- **No se capturaron por HTTP**, porque en esta sesión no fue posible arrancar Spring.
- `docs/pruebas.http` permite repetir los mismos pasos contra el servidor.

### Datos de demostración

`data/` viene precargada con datos ficticios generados por `GeneradorDatosDemo` con los
servicios reales: 8 usuarios en los cuatro niveles, 65 transacciones, rechazos,
reversiones, programadas en todos sus estados, los seis patrones de detección, canjes y
un ciclo en el grafo. `datos-demo/` guarda la copia original y `DatosDemoTest` (3
pruebas) verifica que se cargan sin errores. La guía está en `DATOS_DEMO.md`.

## 17. Limitaciones

- **Pruebas.** Las pruebas con Spring, MockMvc y Tomcat reales (28) no se ejecutaron en
  esta sesión: Maven no tenía acceso a la red. Deben ejecutarse con `./mvnw clean test`
  (o `mvnw.cmd clean test` en Windows) antes de dar por cerrada la entrega.
- **Seguridad.** No hay autenticación ni autorización (simulación académica). Cualquier
  cliente puede operar sobre cualquier usuario.
- **Valores provisionales.** Varias reglas usan valores no definidos por el enunciado
  (ver la tabla de decisiones):
  - Plazo y tipos de reversión.
  - Umbrales de detección.
  - Bono de programadas.
  - Umbral de saldo bajo.
  - Catálogo de beneficios y ventajas por nivel: traen valores de demostración, no
    definidos por el enunciado.
- **Comisiones y límites.** No se implementan comisiones ni límites por nivel: el
  enunciado los menciona solo como ejemplos de ventajas.
- **Rendimiento de los datos.**
  - Toda la información se mantiene en memoria y cada cambio reescribe el archivo
    completo. Es adecuado para volumen académico, no para grandes volúmenes.
  - El ABB no está balanceado: con inserciones ordenadas puede degradarse a *O(n)*.
- **Concurrencia.** Un único cerrojo serializa todas las escrituras. Es simple y
  correcto, pero no escala.
- **Detección.** Las heurísticas son deliberadamente simples (ventanas y factores fijos)
  y pueden dar falsos positivos.
  - Ejemplo: dos billeteras propias que envían al mismo destino en 10 minutos se marcan
    como fragmentación.
- **Zona horaria.** El `Clock` usa la zona del sistema.

## 18. Conclusiones

El sistema cubre los requisitos funcionales y adicionales del enunciado con una
arquitectura en capas en la que cada estructura de datos resuelve un problema concreto:
- La tabla hash da acceso directo.
- El ABB mantiene el orden por puntos y por valor.
- La cola de prioridad respeta la prioridad temporal de las programadas.
- La pila permite deshacer.
- La cola atiende las notificaciones en orden.
- El grafo modela la red de transferencias.
- La lista enlazada guarda los historiales.

El benchmark confirma con mediciones las diferencias de complejidad que motivaron esas
elecciones.

En integridad financiera, el uso de `BigDecimal`, el diario de rehacer con imágenes
exactas, la escritura atómica y la regla de no aceptar cambios con entradas pendientes
evitan estados a medias. Las pruebas de fallo y de concurrencia lo verifican.

Queda pendiente confirmar con el profesor los valores marcados como provisionales y
ejecutar la batería completa con Maven, Spring y Jackson reales.

---

## Anexo: decisiones

Fuente: "Enunciado" significa que el PDF lo define; "Propia" significa que el enunciado
no lo define y es una política del equipo, configurable cuando aplica.

| Decisión | Fuente | Estado | Valor y dónde se configura |
|---|---|---|---|
| Puntos por tipo (1/2/3 por cada 100) | Enunciado 4.4 | Confirmada | `fintech.puntos.*` |
| Redondeo de puntos | Propia | Provisional | División entera (250.99 → 2) |
| Bono por programada exitosa | Enunciado 4.4 (existencia); valor propio | Provisional | 10 puntos, `fintech.puntos.bono-programada` |
| Rangos de niveles | Enunciado 4.5 | Confirmada | `NivelUsuario` |
| Ventajas de cada nivel | Enunciado (solo ejemplos) | Pendiente | Textos de demostración, `fintech.niveles.ventajas.*` |
| Catálogo de beneficios | No definido | Pendiente | 4 beneficios de demostración, `fintech.beneficios.catalogo` |
| Canje: el nivel se recalcula con los puntos restantes | Propia | Provisional | `PuntosService.ajustar` |
| Operaciones reversibles | Enunciado 4.6 (ejemplos) | Provisional | Recargas y transferencias, no retiros; `fintech.reversion.*` |
| Plazo de reversión | No definido | Provisional | 24 h, `fintech.reversion.plazo-horas` |
| Signo de los puntos en una reversión | Propia | Provisional | Negativo de lo descontado realmente; nunca deja puntos negativos |
| Umbrales de detección (6 patrones) | Enunciado 4.9 (patrones); valores propios | Provisional | `fintech.deteccion.*` |
| Nivel de riesgo por patrón | Propia | Provisional | `PoliticaDeteccion.provisional()` |
| Umbral de saldo bajo | No definido | Provisional | 10 000, `fintech.notificaciones.umbral-saldo-bajo` |
| Anticipación de "programada próxima" | No definido | Provisional | 24 h |
| Tamaño del historial de notificaciones | No definido | Provisional | 0 = sin límite, `fintech.notificaciones.recientes-por-usuario` |
| Modalidad de ejecución programada | Enunciado 6.6 y 8 | Confirmada (ambas) | Tarea periódica de 60 s (`fintech.programadas.automatico`) y disparo manual |
| Periodicidad | Propia (enunciado da ejemplos) | Provisional | UNICA, SEMANAL, MENSUAL; la periódica crea un registro nuevo |
| Retiros programables | Enunciado 4.3 ("pagos recurrentes") | Provisional | Permitidos, `fintech.programadas.permitir-retiros` |
| Peso del grafo | No definido | Provisional | CANTIDAD por defecto; MONTO por parámetro (`fintech.grafo.peso-por-defecto`) |
| Transferencias internas y revertidas en el grafo | Propia | Provisional | Excluidas |
| Usuarios eliminados con registros asociados | No definido | Provisional | Se rechaza (409 `USUARIO_CON_REGISTROS`) |
| Desactivar billetera con saldo | No definido | Provisional | Se rechaza (409 `BILLETERA_CON_SALDO`) |
| Zona horaria | No definido | Provisional | La del sistema (`ConfiguracionReloj`) |
| Autenticación | No pedida | Confirmada (no se implementa) | Simulación académica |
| Comisiones y límites por nivel | Enunciado (solo ejemplos de ventajas) | Pendiente | No implementados |
