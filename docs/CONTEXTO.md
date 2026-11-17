# CONTEXTO DEL PROYECTO

> Documento de referencia para cualquier persona o IA que continúe el proyecto.
> Resume las decisiones tomadas; ante una duda, este documento y el enunciado del
> profesor (`Proyecto_Final_estructuras_de_datos_noche_2026-2.pdf`) mandan.

## 1. Objetivo

Proyecto académico de Estructuras de Datos: **plataforma Fintech de gestión de
billeteras digitales y analítica de transacciones**.

Los usuarios administran varias billeteras (ahorro, gastos diarios, compras,
transporte, inversión…), hacen recargas, retiros y transferencias, programan
operaciones futuras, acumulan puntos y niveles, revierten operaciones, reciben
alertas y consultan analítica, incluida la detección de comportamiento inusual.

El propósito principal no es solo que funcione, sino **demostrar y justificar** el
uso de listas, pilas, colas, colas de prioridad, árboles, tablas hash y grafos para
resolver problemas reales del dominio, y comparar su rendimiento.

## 2. Tecnologías

| Elemento        | Valor                                                     |
|-----------------|-----------------------------------------------------------|
| Lenguaje        | Java 21                                                   |
| Framework       | Spring Boot 4.1.2                                         |
| Construcción    | Maven (wrapper `mvnw` incluido)                           |
| Group / Artifact| `co.edu.uniquindio` / `fintech`                           |
| Paquete base    | `co.edu.uniquindio.fintech`                               |
| Dependencias    | Spring Web MVC, Validation, DevTools (+ starters de test) |
| Frontend        | HTML, CSS y JavaScript en `src/main/resources/static`     |
| Persistencia    | Archivos JSON independientes (sin base de datos)          |

Prohibido: JPA, Hibernate, bases de datos relacionales y bibliotecas externas de
estructuras de datos. No se cambian versiones ni se agregan dependencias sin una
justificación técnica indispensable registrada en este documento.

Para JSON se usará el Jackson que ya trae `spring-boot-starter-webmvc` (en Spring
Boot 4 es Jackson 3, paquete `tools.jackson`); no hace falta una dependencia nueva.
Se confirmará al implementar la persistencia.

## 3. Arquitectura

MVC en capas sobre una API REST. El frontend estático consume la API con `fetch`.

```
 Frontend (static: HTML/CSS/JS)
        │  HTTP/JSON
        ▼
 controller ──► service ──► repository ──► persistence ──► data/*.json
     │             │             │               │
     └── dto       ├── model ◄───┴───────────────┘
                   └── structures.interfaces ◄── structures.adaptadores / propias
 exception: errores propios + manejador global
 config:    creación de beans (implementaciones de estructuras, Clock, datos semilla)
 benchmark: comparación de rendimiento entre estructuras
```

## 4. Paquetes y responsabilidades

Todos bajo `co.edu.uniquindio.fintech`.

| Paquete                   | Responsabilidad | Estado |
|---------------------------|-----------------|--------|
| `model`                   | Entidades del dominio y enumeraciones (usuario, billetera, transacción, operación programada, notificación, evento de auditoría, tipos, estados, niveles, riesgo). Sin reglas financieras. Detalle en 4.1. | **Fase 4: implementado** |
| `structures`              | Raíz de estructuras genéricas, sin dependencia del dominio. | Contiene subpaquetes |
| `structures.interfaces`   | Contratos propios: `Coleccion`, `Lista`, `Pila`, `Cola`, `ColaPrioridad`, `TablaHash`, `Comparador`, `ArbolBinarioBusqueda`, `ClaveCompuesta`, `GrafoDirigidoPonderado`, `Arista`. | **Fase 1: definido** |
| `structures.adaptadores`  | Implementaciones de los contratos comunes delegando en `java.util`: `PilaAdaptador`, `ColaAdaptador`, `ColaPrioridadAdaptador`, `TablaHashAdaptador`. | **Fase 2: implementado** |
| `structures.propias`      | Estructuras implementadas a mano: `ListaEnlazada` (fase 2), `ArbolBinarioBusquedaImpl` y `GrafoDirigidoPonderadoImpl` (fase 3). | **Fases 2 y 3: implementado** |
| `repository`              | Acceso a los datos en memoria usando estructuras propias (tablas hash por identificador y código, índices secundarios). Carga y guarda mediante `persistence`. Sin reglas de negocio. Detalle en 7.8. | **Fase 5: implementado** |
| `persistence`             | Conversión archivo JSON ⇄ registros, escritura atómica, diario de operaciones, generador de identificadores y verificación de integridad. Detalle en 7.7. | **Fase 5: implementado** |
| `service`                 | Toda regla de negocio: validaciones de saldo y existencia, puntos, niveles, reversión, programación, alertas, detección de anomalías, analítica. | Vacío |
| `controller`              | Endpoints REST: recibe la solicitud, invoca el servicio y devuelve la respuesta. Nada más. | Vacío |
| `dto`                     | Records de petición y respuesta, con anotaciones de Bean Validation. Detalle en 4.2. | **Fase 4: implementado** |
| `exception`               | Excepciones propias y manejador global (`@RestControllerAdvice`) que traduce errores a respuestas HTTP uniformes. Detalle en 4.3. | **Fase 4: implementado** |
| `config`                  | Configuración e inicialización: `Clock`, fábrica de estructuras concretas, beans de persistencia y repositorios, arranque de datos. Sin datos semilla. | **Fases 4 y 5: implementado** |
| `benchmark`               | Comparación de rendimiento entre estructuras en operaciones similares. | Vacío |

### 4.1 Modelo de dominio (fase 4)

**Records inmutables.** Las entidades son `record` porque:
- Jackson 3 los lee y escribe sin setters.
- El diario de rehacer (7.6) necesita preparar la imagen final de cada registro sin
  tocar el estado en memoria, y con objetos inmutables eso sale natural.
- No hay setters que permitan dejar un objeto incoherente.

Las actualizaciones futuras se representan con métodos `con...` que devuelven una
copia y no calculan nada: `Usuario.conPuntos`, `Billetera.conSaldo`,
`Transaccion.conEstado`, etc. Quién decide los valores y cuándo es `service`.

**Invariantes de forma.** Los constructores solo comprueban que el registro esté bien
formado:
- Campos obligatorios presentes y textos no vacíos (`NullPointerException` o
  `IllegalArgumentException`).
- Identificadores con el formato de 7.4 (`model.Identificadores` tiene los patrones).
- Billeteras coherentes con el tipo (`TipoTransaccion.billeterasCoherentes`): una
  recarga solo tiene destino, un retiro solo origen, una transferencia ambas y
  distintas, y una reversión al menos una.
- Referencia `transaccionRevertida` obligatoria solo en las reversiones.

No validan saldos, puntos, niveles ni límites: esas son reglas financieras de
`service`. Que falle un invariante indica un error de programación o un JSON corrupto;
por eso el manejador lo trata como 500, no como 400.

| Entidad | Campos | Cambios previstos |
|---|---|---|
| `Usuario` | `identificacion` (clave dada por el usuario), `nombre`, `correo`, `puntosAcumulados`, `nivel` | `conDatosBasicos`, `conPuntos` |
| `Billetera` | `codigo` (BIL), `identificacionPropietario`, `nombre`, `tipo`, `saldo` (`BigDecimal`), `estado` | `conDatos`, `conSaldo`, `conEstado` |
| `Transaccion` | `id` (TRX), `fecha`, `tipo`, `valor`, `billeteraOrigen`?, `billeteraDestino`?, `estado`, `puntosGenerados`, `nivelRiesgo`, `transaccionRevertida`? | `conEstado` |
| `OperacionProgramada` | `id` (OPR), `tipo`, `valor`, `billeteraOrigen`?, `billeteraDestino`?, `fechaEjecucion`, `periodicidad`, `estado` | `conEstado`, `conFechaEjecucion` |
| `Notificacion` | `id` (NOT), `identificacionUsuario`, `tipo`, `mensaje`, `fecha`, `estado` | `conEstado` |
| `EventoAuditoria` | `id` (AUD), `identificacionUsuario`, `transaccion`?, `patron`, `descripcion`, `nivelRiesgo`, `fecha` | ninguno (registro histórico) |
| `BeneficioCanjeado` | `id` (CAN), `identificacionUsuario`, `beneficio` (texto libre), `puntosDescontados`, `fecha`, `estado` | `conEstado` |

`?` = opcional (puede ser `null`). Las relaciones son siempre por identificador:
ninguna entidad contiene a otra ni una estructura de datos. Fechas con
`LocalDateTime` y montos con `BigDecimal`.

**Originales y reversiones.**
- Una reversión es una transacción de tipo `REVERSION` con `transaccionRevertida`
  apuntando a la original; ningún otro tipo puede llevar esa referencia.
- La original pasa a estado `REVERTIDA`.
- El historial muestra así ambos lados sin ambigüedad, y la reconstrucción de la pila
  (7.5) no necesita cruzar referencias.

**Enumeraciones.**

| Enumeración | Valores | Origen |
|---|---|---|
| `TipoBilletera` | AHORRO, GASTOS_DIARIOS, COMPRAS, TRANSPORTE, INVERSION, SUSCRIPCIONES | enunciado 1 y 4.1 |
| `EstadoBilletera` | ACTIVA, INACTIVA | 7.4 (desactivar en vez de borrar) |
| `TipoTransaccion` | RECARGA, RETIRO, TRANSFERENCIA, REVERSION | 4.2, 4.6 |
| `EstadoTransaccion` | EXITOSA, RECHAZADA, REVERTIDA | 4.2, 4.6, 4.7 |
| `NivelUsuario` | BRONCE 0–500, PLATA 501–1000, ORO 1001–5000, PLATINO 5001+ (solo los límites) | 4.5 |
| `NivelRiesgo` | NINGUNO, BAJO, MEDIO, ALTO | **provisional** |
| `TipoOperacionProgramada` | RECARGA, RETIRO, TRANSFERENCIA (cada uno indica su `TipoTransaccion`; una reversión no se programa) | 4.3; RETIRO **provisional** |
| `EstadoOperacionProgramada` | PENDIENTE, EJECUTADA, FALLIDA, CANCELADA | 4.3 |
| `Periodicidad` | UNICA, SEMANAL, MENSUAL | **provisional** |
| `TipoNotificacion` | SALDO_BAJO, OPERACION_PROGRAMADA_PROXIMA, OPERACION_RECHAZADA, ASCENSO_NIVEL, CANJE_REALIZADO, ALERTA_SEGURIDAD | 4.7, 4.9 |
| `EstadoNotificacion` | PENDIENTE, DESPACHADA | 4.7, 5.3 |
| `EstadoCanje` | REALIZADO, ANULADO | **provisional** |
| `PatronInusual` | los seis patrones de la lista del enunciado | 4.9 |

**JSON.**
- Los nombres de campo son los componentes de cada record; los enums se escriben por
  nombre, los montos como número exacto y las fechas en ISO-8601.
- Los métodos `con...` y `esReversion()` no generan propiedades.
- Al deserializar se ejecuta el constructor, así un registro mal formado en un archivo
  falla al cargarse.
- Una colección de registros se lee como arreglo (`Billetera[].class`), sin `List`.

### 4.2 DTO (fase 4)

Todos son `record`. Las peticiones (`Solicitud...`) solo traen lo que el cliente puede
decidir; nunca saldos, puntos, niveles, estados calculados ni identificadores
generados. Las respuestas (`...Respuesta`) se construyen con `desde(entidad)`.

| DTO | Uso | Validaciones |
|---|---|---|
| `SolicitudRegistroUsuario` | `POST /api/usuarios` | identificación, nombre y correo obligatorios; correo con formato |
| `SolicitudActualizacionUsuario` | `PUT /api/usuarios/{id}` | nombre y correo |
| `SolicitudCreacionBilletera` | `POST /api/usuarios/{id}/billeteras` | nombre y tipo |
| `SolicitudActualizacionBilletera` | `PUT /api/billeteras/{codigo}` | nombre, tipo y estado |
| `SolicitudRecarga` / `SolicitudRetiro` | recargas y retiros | billetera con formato `BIL-NNNNNN`; valor > 0 |
| `SolicitudTransferencia` | transferencias | ambas billeteras con formato y distintas; valor > 0 |
| `SolicitudOperacionProgramada` | programar | tipo, valor > 0, fecha, periodicidad; billeteras coherentes con el tipo |
| `UsuarioRespuesta` | usuario, puntos y nivel | — |
| `BilleteraRespuesta`, `SaldoRespuesta` | billetera y consulta de saldo | — |
| `TransaccionRespuesta` | transacciones e historial | — |
| `OperacionProgramadaRespuesta`, `NotificacionRespuesta`, `EventoAuditoriaRespuesta` | consultas | — |
| `RespuestaError` | errores (`estado`, `error`, `mensaje`, `fecha`) | — |

- La fecha futura de una operación programada no se valida con `@Future`, porque esa
  anotación usa el reloj del sistema y no el `Clock`; la valida `service`.
- Sin DTO todavía para reportes, canjes, benchmark ni grafo: sus parámetros siguen
  por definir (`API.md`).

### 4.3 Excepciones y manejador global (fase 4)

| Excepción (`exception`) | HTTP | Código por defecto |
|---|---|---|
| `RecursoNoEncontradoException` | 404 | `RECURSO_NO_ENCONTRADO` (o uno específico, p. ej. `USUARIO_NO_ENCONTRADO`) |
| `ConflictoNegocioException` | 409 | `CONFLICTO_NEGOCIO` (p. ej. `SALDO_INSUFICIENTE`) |
| `SolicitudInvalidaException` | 400 | `SOLICITUD_INVALIDA` |
| `PersistenciaException` | 500 | `ERROR_PERSISTENCIA`; el mensaje técnico solo va al log |

Todas heredan de `FintechException`, que obliga a un código en mayúsculas y un
mensaje. Cómo actúa `ManejadorGlobalExcepciones` (`@RestControllerAdvice`, extiende
`ResponseEntityExceptionHandler`):
- Bean Validation → 400 `VALIDACION_FALLIDA`, con los mensajes `campo: mensaje`
  ordenados.
- JSON mal formado → 400 `SOLICITUD_MAL_FORMADA`.
- Parámetro faltante o de tipo incorrecto → 400 `PARAMETRO_FALTANTE` o
  `PARAMETRO_INVALIDO`.
- Las demás excepciones de Spring MVC conservan su código HTTP (405, 406, 415, 404 de
  ruta inexistente) con este mismo formato.
- Cualquier otra excepción → 500 `ERROR_INTERNO` con mensaje genérico. Un error de
  programación nunca sale como 400.
- La fecha del error sale del `Clock` declarado en `config.ConfiguracionReloj`.

## 5. Reglas de dependencia (obligatorias)

1. `structures` no importa `model` ni ningún otro paquete de la aplicación.
2. Las estructuras son genéricas e independientes del dominio financiero.
3. Las estructuras comunes tienen interfaces propias en `structures.interfaces`.
4. Sus implementaciones (`structures.adaptadores`) pueden usar `java.util` por dentro.
   Excepción decidida en la fase 2: la `Lista` es propia (`structures.propias.ListaEnlazada`).
5. El ABB y el grafo son implementaciones propias (`structures.propias`), sin bibliotecas externas.
6. El ABB admite claves lógicas repetidas mediante `ClaveCompuesta`.
7. `service` y `repository` dependen solo de `structures.interfaces`, nunca de
   `adaptadores` ni `propias`. Las implementaciones concretas se crean en `config`
   y se inyectan.
8. `java.util` (incluidos `java.util.function` y `java.util.stream`) solo se importa
   dentro de `structures`.
9. Toda regla de negocio vive en `service`.
10. `controller` solo usa `service` y `dto`; no accede a `repository` ni a `persistence`.
11. Los DTO son `record`.
12. La persistencia usa un archivo JSON independiente por tipo de registro.
13. Sin JPA, Hibernate ni bases de datos relacionales.

Dirección permitida: `controller → service → repository → persistence`;
todas pueden usar `model`, `structures.interfaces` y `exception`; `controller` usa `dto`.

**Verificación automática:** `src/test/java/.../ArquitecturaTest.java` recorre el
código fuente y falla si se violan las reglas 1, 7, 8, 10 o 13.

### Consecuencias prácticas de la regla 8

Como fuera de `structures` no se puede usar `java.util`:

- Recorrer estructuras: todos los contratos lineales extienden `Iterable` (de
  `java.lang`), así que `for (T x : lista)` funciona sin importar nada.
- Ordenar: se usa `structures.interfaces.Comparador<T>` (lambda), no `java.util.Comparator`.
- Ausencia de valor: las búsquedas devuelven `null` en lugar de `Optional`.
- Colecciones en DTO y JSON: se usan **arreglos** (`T[]`) en lugar de `List`.
  Jackson lee y escribe arreglos directamente (`readValue(archivo, Usuario[].class)`).
- Identificadores: prefijo + consecutivo, sin `java.util.UUID` (ver 7.4).
- Nulos en el modelo: sin `Objects.requireNonNull` (es de `java.util`); se usan las
  comprobaciones de `model.Validaciones`.
- Fechas: `java.time` (permitido) con un `Clock` inyectado desde `config`, para poder
  simular el paso del tiempo en las operaciones programadas y en las pruebas.
- Dinero: `java.math.BigDecimal` (permitido).

## 6. Organización de las estructuras

### 6.1 Convenciones de todos los contratos

- No aceptan `null` como elemento, clave o valor.
- Buscar algo inexistente devuelve `null` (o `false`/`-1` según el método).
- Extraer de una estructura vacía lanza `IllegalStateException`.
- Índice inválido lanza `IndexOutOfBoundsException`.
- Pasar `null` a cualquier operación (también a consultas como `contiene` u
  `obtener`) lanza `NullPointerException` (decidido en la fase 2).
- Los iteradores son de solo lectura: `remove()` lanza `UnsupportedOperationException`.
- Los métodos que devuelven varias cosas devuelven `Lista` (copias, no vistas internas).
- Se usan excepciones de `java.lang` para que el código externo pueda capturarlas
  sin importar `java.util`.

### 6.2 Contratos comunes (`structures.interfaces`)

| Contrato | Operaciones | Implementación (fase 2) |
|----------|-------------|-----------------------------------|
| `Coleccion<T>` (base) | `tamanio`, `estaVacia`, `limpiar`, iteración | — |
| `Lista<T>` | `agregar`, `agregarEn`, `obtener`, `reemplazar`, `eliminarEn`, `eliminar`, `contiene`, `indiceDe` | propia: `ListaEnlazada` (nodos doblemente enlazados) |
| `Pila<T>` | `apilar`, `desapilar`, `cima` | `PilaAdaptador` sobre `ArrayDeque` |
| `Cola<T>` | `encolar`, `desencolar`, `frente` | `ColaAdaptador` sobre `ArrayDeque` |
| `ColaPrioridad<T>` | `insertar`, `extraer`, `consultar`, `eliminar`; orden por `Comparador` (menor = más prioritario) | `ColaPrioridadAdaptador` sobre `PriorityQueue` |
| `TablaHash<K,V>` | `poner`, `obtener`, `contieneClave`, `eliminar`, `tamanio`, `estaVacia`, `limpiar`, `claves`, `valores` | `TablaHashAdaptador` sobre `HashMap` |
| `Comparador<T>` | `comparar`, `invertido` | lambdas |

### 6.2.1 Complejidad temporal de las estructuras comunes

n = número de elementos; i = índice; c = capacidad interna de la tabla hash.
"Amortizado" significa que el costo O(n) del redimensionamiento interno se reparte
entre muchas inserciones. Las cifras están también en el Javadoc de cada clase.

| Estructura | Operación | Promedio | Peor caso |
|---|---|---|---|
| `ListaEnlazada` | `agregar` (al final) | O(1) | O(1) |
| | `agregarEn`, `obtener`, `reemplazar`, `eliminarEn` | O(min(i, n−i)) | O(n) |
| | `eliminar`, `contiene`, `indiceDe` | O(n) | O(n) |
| | `tamanio`, `estaVacia`, `limpiar` | O(1) | O(1) |
| | recorrido con `for-each` | O(n) | O(n) |
| `PilaAdaptador` | `apilar` | O(1) amortizado | O(n) (redimensión) |
| | `desapilar`, `cima`, `tamanio`, `estaVacia` | O(1) | O(1) |
| | `limpiar` | O(n) | O(n) |
| `ColaAdaptador` | `encolar` | O(1) amortizado | O(n) (redimensión) |
| | `desencolar`, `frente`, `tamanio`, `estaVacia` | O(1) | O(1) |
| | `limpiar` | O(n) | O(n) |
| `ColaPrioridadAdaptador` | `insertar` | O(log n) amortizado | O(n) (redimensión) |
| | `extraer` | O(log n) | O(log n) |
| | `consultar`, `tamanio`, `estaVacia` | O(1) | O(1) |
| | `eliminar(elemento)` | O(n) | O(n) |
| | `limpiar` | O(n) | O(n) |
| `TablaHashAdaptador` | `poner` | O(1) amortizado | O(n) |
| | `obtener`, `contieneClave`, `eliminar` | O(1) | O(n); O(log n) si las claves son `Comparable` |
| | `tamanio`, `estaVacia` | O(1) | O(1) |
| | `claves`, `valores` (copia) | O(n + c) | O(n + c) |
| | `limpiar` | O(c) | O(c) |

Notas:
- En `ListaEnlazada`, recorrer con `obtener(i)` para todos los índices cuesta O(n²);
  para recorrer se usa `for-each`.
- El O(1) promedio de la tabla hash supone un `hashCode` que reparta bien las claves.
  El peor caso aparece cuando muchas claves caen en la misma cubeta; `HashMap`
  convierte esas cubetas en árboles, pero eso solo baja el costo a O(log n) si las
  claves son `Comparable`.

### 6.3 Contratos especializados (implementación propia, fase 3)

Implementaciones: `structures.propias.ArbolBinarioBusquedaImpl` y
`structures.propias.GrafoDirigidoPonderadoImpl` (detalle en 6.3.1).

**`ArbolBinarioBusqueda<K extends Comparable, V>`**
- Inserción/búsqueda: `insertar` (reemplaza si la clave existe), `buscar`, `contiene`.
- Eliminación: `eliminar` (hoja, un hijo, dos hijos), `limpiar`.
- Extremos: `claveMinima`, `claveMaxima`.
- Recorridos: `inOrden`, `inOrdenInverso`, `preOrden`, `postOrden`, `porNiveles`, `claves`.
- Rango: `rango(desde, hasta)` cerrado y podando ramas; `mayores(n)`.
- Información: `tamanio`, `estaVacio`, `altura`.

**`ClaveCompuesta<P, D>`** — resuelve las claves repetidas del ABB.
`ClaveCompuesta.de(puntos, idUsuario)`: ordena por `puntos` y desempata por `idUsuario`.
`cotaInferior(p)` / `cotaSuperior(p)` sirven solo como límites de `rango`, para
consultar por la parte principal (p. ej., todos los usuarios Plata:
`rango(cotaInferior(501), cotaSuperior(1000))`).

**`GrafoDirigidoPonderado<V>`** (+ record `Arista<V>`)
- Vértices: `agregarVertice`, `eliminarVertice`, `contieneVertice`, `vertices`, `cantidadVertices`.
- Aristas y pesos: `agregarArista` (crea o reemplaza), `incrementarPeso` (acumula),
  `eliminarArista`, `contieneArista`, `peso`, `aristas`, `cantidadAristas`,
  `aristasSalientes`, `aristasEntrantes`, `gradoSalida`, `gradoEntrada`.
- Recorridos: `recorridoAnchura` (BFS), `recorridoProfundidad` (DFS).
- Rutas: `existeRuta`, `rutaMenosSaltos` (BFS), `rutaMenorPeso` y `pesoRutaMenor` (Dijkstra).
- Ciclos: `tieneCiclo`, `ciclosQueIncluyen(v, longitudMaxima)`.
- Pesos `java.math.BigDecimal` no negativos (exactos para montos; se comparan con
  `compareTo`). `pesoRutaMenor` devuelve `null` si no hay ruta. A lo sumo una arista
  por par ordenado. El significado del peso (monto o número de transferencias) lo
  decide `service`.

### 6.3.1 Implementación y complejidad (fase 3)

**`ArbolBinarioBusquedaImpl`** — ABB de nodos (clave, valor, hijo izquierdo y
derecho), **sin balanceo**. Todas las operaciones son iterativas, con `PilaAdaptador`
y `ColaAdaptador` como estructuras auxiliares, para que un árbol degenerado no
desborde la pila de llamadas. Los resultados se devuelven en `ListaEnlazada`.

- Eliminación con dos hijos: se copia el sucesor inorden (mínimo del subárbol
  derecho) y se elimina el nodo del sucesor.
- `rango`: inorden con poda; no baja por la izquierda de un nodo menor que `desde`
  y se detiene en la primera clave mayor que `hasta`.
- `porNiveles` y `altura` usan una cola (BFS por niveles).
- Decisiones sobre casos que el contrato no detallaba:
  - `mayores` con cantidad negativa lanza `IllegalArgumentException`.
  - `rango` con un límite `null` lanza `NullPointerException`.
  - `insertar` con una `ClaveCompuesta` de tipo cota lanza `IllegalArgumentException`.

Complejidad (n = nodos, h = altura, k = valores devueltos). Como no hay balanceo,
**h depende del orden de inserción**: h ≈ log₂ n si las claves llegan en orden
aleatorio (equilibrado por casualidad) y h = n − 1 si llegan ordenadas (degenerado).
No hay garantía de O(log n).

| Operación | Equilibrado por casualidad | Degenerado |
|---|---|---|
| `insertar`, `buscar`, `contiene`, `eliminar` | O(log n) | O(n) |
| `claveMinima`, `claveMaxima` | O(log n) | O(n) |
| `rango` | O(log n + k) | O(n) |
| `mayores(m)` | O(log n + m) | O(n) si crece hacia la derecha (inserción ascendente); O(m) si crece hacia la izquierda |
| `inOrden`, `inOrdenInverso`, `preOrden`, `postOrden`, `porNiveles`, `claves`, `altura` | O(n) | O(n) |
| `tamanio`, `estaVacio`, `limpiar` | O(1) | O(1) |

**`GrafoDirigidoPonderadoImpl`** — listas de adyacencia.

- Una `TablaHashAdaptador` relaciona cada vértice con su registro de adyacencia.
- Cada registro tiene dos `ListaEnlazada`: aristas salientes y aristas entrantes.
- Cada arista es un único objeto interno compartido por la lista de salida del origen
  y la de entrada del destino, así un cambio de peso se ve en ambas.
- Se permiten lazos (`A -> A`).

Algoritmos:
- `recorridoAnchura` y `rutaMenosSaltos`: BFS con `ColaAdaptador`.
- `recorridoProfundidad`: DFS iterativo con `PilaAdaptador`, en el mismo orden que la
  versión recursiva.
- `rutaMenorPeso` y `pesoRutaMenor`: Dijkstra con `ColaPrioridadAdaptador` (montículo
  binario) y eliminación perezosa de candidatos obsoletos, con parada temprana al
  fijar el destino.
- `tieneCiclo`: DFS iterativo con estados en curso / terminado; un ciclo es una
  arista hacia un vértice en curso.
- `ciclosQueIncluyen`: DFS desde el vértice sin repetir vértices del camino. Cada
  ciclo simple aparece una sola vez, porque todos empiezan en ese vértice y hay una
  arista por par ordenado. La recursión está acotada por `longitudMaxima`.

Vértices inexistentes:
- Lanzan `IllegalArgumentException` (según el contrato): adyacencia, grados,
  recorridos, `ciclosQueIncluyen` y `peso`.
- En consultas de aristas y rutas no hay arista ni ruta, así que se devuelve `false`,
  lista vacía o `null` según el método.

Complejidad. V = vértices, E = aristas, s(v) y e(v) = grados de salida y de entrada.
Se usa el costo promedio de la tabla hash (O(1)); en su peor caso cada acceso es O(V).

| Operación | Complejidad |
|---|---|
| `agregarVertice`, `contieneVertice`, `cantidadVertices`, `cantidadAristas`, `gradoSalida`, `gradoEntrada`, `estaVacio`, `limpiar` | O(1) |
| `agregarArista`, `incrementarPeso`, `contieneArista`, `peso` | O(s(origen)): búsqueda lineal en la lista de adyacencia |
| `eliminarArista` | O(s(origen) + e(destino)) |
| `eliminarVertice` | O(E) en el peor caso (ver nota) |
| `aristasSalientes` / `aristasEntrantes` | O(s(v)) / O(e(v)) |
| `vertices` / `aristas` | O(V) / O(V + E) |
| `recorridoAnchura`, `recorridoProfundidad`, `existeRuta`, `rutaMenosSaltos`, `tieneCiclo` | O(V + E) |
| `rutaMenorPeso`, `pesoRutaMenor` (Dijkstra) | O((V + E) log V), más el costo de sumar `BigDecimal` |
| `ciclosQueIncluyen(v, L)` | proporcional a los caminos simples desde v con menos de L aristas; O(V^L) en el peor caso |

Nota sobre `eliminarVertice(v)` (verificada contra el código en la fase 4):
- Por cada arista `v -> w` se busca y quita el enlace en la lista de entrada de `w`
  (búsqueda lineal: O(e(w))); por cada `u -> v`, en la lista de salida de `u` (O(s(u))).
- El costo es O(s(v) + e(v) + Σ e(w) + Σ s(u)).
- Como hay a lo sumo una arista por par ordenado, los `w` son distintos entre sí, y lo
  mismo los `u`. Por eso Σ e(w) ≤ E y Σ s(u) ≤ E, y el total es O(E).
- La cota O(E²) también es válida, pero no es ajustada: solo se alcanzaría si un mismo
  destino se recorriera una vez por cada arista, lo que la representación impide.

### 6.4 Relación requisito → estructura

| Requisito del enunciado | Estructura |
|---|---|
| Usuarios por identificación, billeteras por código, saldos/puntos/configuración rápidos | `TablaHash` |
| Historial de transacciones por usuario y por billetera, beneficios canjeados, operaciones de un período | `Lista` |
| Operaciones reversibles, historial de deshacer | `Pila` |
| Operaciones programadas y pagos automáticos según fecha y hora | `ColaPrioridad` |
| Notificaciones pendientes de despacho | `Cola` |
| Historial de notificaciones recientes por usuario | `Lista` (acotada; tamaño por definir) |
| Usuarios por puntos, niveles, búsqueda por rango de puntos, reportes ordenados | `ArbolBinarioBusqueda` + `ClaveCompuesta` |
| Transacciones de mayor valor con estructuras ordenadas | `ArbolBinarioBusqueda` (`mayores`) |
| Transferencias entre usuarios, relaciones entre billeteras, rutas frecuentes, ciclos | `GrafoDirigidoPonderado` |
| Comparar rendimiento de estructuras en operaciones similares | `benchmark` |

## 7. Persistencia

### 7.1 Persistente vs. en memoria

- **Persistente (fuente de verdad):** archivos JSON en el directorio `data/` (ruta
  configurable con la propiedad `fintech.datos.directorio`, ver 7.7). Cada
  archivo es un arreglo JSON con todos los registros de un tipo.
- **En memoria (derivado):** tablas hash, listas de historial, pilas de reversión,
  colas, ABB y grafo. Se reconstruyen desde los JSON al iniciar y **no se guardan**
  como estructura: lo que se guarda son los registros planos.
- Ciclo: cargar todo al iniciar → consultar siempre en memoria → tras cada operación
  que modifica datos, reescribir el archivo del tipo afectado (escritura en temporal
  y reemplazo, para no corromper el JSON).

### 7.2 Contrato

`persistence.AlmacenJson<T>`: `nombreArchivo()`, `cargar(): Lista<T>`,
`guardar(Lista<T>)`. Una instancia por archivo. Sin reglas de negocio ni índices.
Los errores de E/S se lanzan como `PersistenciaException` (no verificada). Implementación: `AlmacenJsonArchivo` (7.7).

### 7.3 Archivos

| Archivo | Contenido | Estructuras en memoria que alimenta |
|---|---|---|
| `usuarios.json` | Usuarios: identificación, datos básicos, puntos acumulados, nivel. | Tabla hash por identificación; ABB por puntos. |
| `billeteras.json` | Billeteras: código, usuario propietario, nombre, tipo, saldo disponible, estado. | Tabla hash por código; lista de billeteras por usuario. |
| `transacciones.json` | Transacciones: identificador, fecha, tipo, valor, billetera origen, billetera destino, estado, puntos generados, nivel de riesgo. | Listas de historial por usuario y billetera; pila de reversibles; ABB por valor; grafo de transferencias. |
| `operaciones-programadas.json` | Operaciones futuras: tipo, valor, billeteras, fecha y hora de ejecución, periodicidad, estado. | Cola de prioridad por fecha de ejecución. |
| `notificaciones.json` | Notificaciones por usuario: tipo de alerta, mensaje, fecha, estado de despacho. | Cola de pendientes; lista de recientes por usuario. |
| `auditoria.json` | Eventos de comportamiento inusual: transacción, usuario, patrón detectado, nivel de riesgo, fecha. | Lista de eventos (consultable posteriormente). |
| `beneficios.json` | Beneficios canjeados: identificador, usuario, beneficio, puntos descontados, fecha, estado. | Lista de beneficios canjeados por usuario. |

Los campos de cada archivo son los componentes de los records de `model` (sección 4.1).
Los de `transacciones.json` vienen del enunciado (4.2), más el nivel de riesgo (4.9) y
la referencia `transaccionRevertida`.

`beneficios.json` se agrega porque el enunciado exige conservar los beneficios
canjeados (5.1) y descontar puntos (RF7). El catálogo de beneficios canjeables
sigue pendiente (sección 11).

Archivo técnico adicional: `diario-operaciones.json` (ver 7.6). No es un registro
del dominio; solo existe para recuperar operaciones incompletas.

### 7.4 Identificadores

Formato `PREFIJO-NNNNNN`: prefijo de tres letras, guion y consecutivo de 6 dígitos
con ceros a la izquierda (crece en ancho si supera 999999). Un consecutivo
independiente por prefijo, empezando en 1.

| Registro | Prefijo | Ejemplo |
|---|---|---|
| Billetera | `BIL` | `BIL-000001` |
| Transacción | `TRX` | `TRX-000042` |
| Operación programada | `OPR` | `OPR-000003` |
| Notificación | `NOT` | `NOT-000120` |
| Evento de auditoría | `AUD` | `AUD-000007` |
| Beneficio canjeado | `CAN` | `CAN-000015` |

- **Usuarios:** su clave es la **identificación** que se registra (el enunciado pide
  acceso "por identificación"); no se genera.
- **Recuperación del consecutivo al iniciar:** no se guarda un archivo de contadores.
  Tras cargar cada JSON (y después de la recuperación de 7.6), se recorre su arreglo,
  se extrae la parte numérica de cada identificador y el siguiente consecutivo es
  `máximo + 1` (o 1 si el archivo está vacío). Los identificadores son así la única
  fuente de verdad y no puede haber contadores desincronizados.
- **Sin reutilización:** como el siguiente valor sale del máximo existente, los
  registros con identificador generado **no se borran físicamente**: se marcan con
  un estado (billetera `INACTIVA`, programada `CANCELADA`…). Por eso eliminar una
  billetera significa desactivarla.
- Un identificador con prefijo o formato inválido en un archivo detiene el arranque
  con un error de persistencia (dato corrupto).
- Un solo generador por prefijo; la asignación es sincronizada.
- Cada archivo se guarda en orden ascendente de consecutivo (los registros nuevos
  van al final). `transacciones.json` queda así en orden cronológico.

### 7.5 Reconstrucción de la pila de operaciones reversibles

La pila no se persiste. Hay una `Pila` por usuario (en una `TablaHash` por
identificación) y se reconstruye al iniciar desde `transacciones.json`:

1. Se recorren las transacciones en el orden del archivo (consecutivo ascendente =
   orden de ejecución).
2. Se apila en la pila del usuario que originó la transacción (dueño de la billetera
   origen; en recargas, de la billetera destino) cada transacción que cumpla:
   tipo reversible y estado `EXITOSA`. Las ya revertidas tienen estado `REVERTIDA`
   (decisión de la fase 4, ver 4.1) y no se apilan.
3. Las transacciones de tipo `REVERSION` no se apilan; su `transaccionRevertida`
   permite verificar la consistencia con el estado de la original.

Campos que debe tener la transacción para esto: tipo, estado, billeteras, y la
referencia `transaccionRevertida` (identificador de la original) en las reversiones.

**Eliminación perezosa:** si una transacción se revierte por su identificador sin
estar en la cima, no se saca de la pila (la `Pila` solo opera la cima). Al
desapilar, el servicio descarta las entradas que ya no sean reversibles (revertidas
o fuera de plazo) hasta encontrar una válida. Las reglas de qué tipos son
reversibles y el plazo siguen pendientes (sección 11) y se aplican igual al
reconstruir y al desapilar.

### 7.6 Recuperación de operaciones incompletas (diario de operaciones)

Una operación financiera puede modificar varios archivos (una transferencia toca
`billeteras.json`, `transacciones.json`, `usuarios.json` por los puntos y el nivel,
y puede tocar `notificaciones.json` y `auditoria.json`). Cada archivo se escribe de
forma atómica, pero el conjunto no. Se usa un **diario de rehacer** (redo log) en
`diario-operaciones.json`:

1. **Preparar:** el servicio valida y calcula en memoria el estado final de todos
   los registros afectados, sin modificar todavía las estructuras.
2. **Registrar:** se agrega al diario una entrada con: identificador de la operación
   (el de la transacción), fecha, estado `PREPARADA` y, por cada archivo afectado,
   la **imagen final completa** de cada registro modificado o nuevo.
3. **Aplicar:** se reescribe cada archivo afectado (atómico), reemplazando por
   identificador los registros de la entrada.
4. **Confirmar:** se actualizan las estructuras en memoria, se elimina la entrada
   del diario y se responde éxito.

Mientras hay escrituras, las operaciones financieras se serializan (una a la vez),
para que el diario no tenga entradas que se pisen.

**Al iniciar**, antes de cargar los datos:

- Cada entrada `PREPARADA` del diario se rehace: se aplican otra vez sus imágenes a
  los archivos y luego se borra. Es idempotente (reemplazar un registro por
  identificador con su imagen final da lo mismo una o varias veces), así que no
  importa en qué paso se cortó.
- Si el corte ocurrió antes del paso 2, no se escribió nada y la operación no existe
  (el cliente nunca recibió confirmación).
- Una entrada ilegible (diario corrupto) detiene el arranque con un error explícito.

**Fallo de escritura en caliente** (paso 3): se reintenta aplicar la entrada; si
vuelve a fallar, la entrada queda en el diario, la memoria no se modifica y se
responde un error que indica que la operación quedó pendiente de recuperación con su
identificador. Se completará en el siguiente arranque.

**Orden de arranque:** recuperar el diario (7.6) → cargar los JSON → recuperar
consecutivos (7.4) → reconstruir estructuras en memoria (tablas hash, listas, ABB,
grafo, colas y pilas según 7.5).

El contrato Java del diario se definirá en la fase de persistencia; no se implementa
todavía.

### 7.7 Implementación de la persistencia (fase 5)

**Directorio de datos.**
- Propiedad `fintech.datos.directorio`; vale `data` por defecto, relativo al
  directorio de trabajo, y también acepta una ruta absoluta.
- Está en `application.properties`. Las pruebas que arrancan Spring sin indicar otro
  directorio usan `target/datos-pruebas-contexto` (`src/test/resources`).
- El directorio se crea solo en la primera escritura: arrancar sin datos no crea nada.

**Clases (`persistence`).**

| Clase | Responsabilidad |
|---|---|
| `AlmacenJsonArchivo<T>` | Única implementación de `AlmacenJson`, genérica: un arreglo JSON por archivo, leído con `readValue(byte[], T[].class)` (Jackson 3, `tools.jackson`). |
| `EscrituraAtomica` | Temporal `.fintech-<archivo>-….tmp` en el mismo directorio, `FileChannel.force`, `Files.move(ATOMIC_MOVE)`; si no se admite, `move(REPLACE_EXISTING)`. Ante un fallo borra el temporal y el destino queda como estaba. |
| `MapeadorJson` | `JsonMapper` de la persistencia (Jackson 3 por defecto + salida indentada). No es un bean, para no alterar el de la API REST. |
| `ArchivoDominio` | Los 7 archivos: nombre, tipo e identificador de cada registro. |
| `AlmacenesJson` | Los 7 almacenes de dominio y el del diario sobre el mismo directorio. |
| `DatosCargados` | Contenido de los 7 archivos leído al iniciar. |
| `VerificadorIntegridad` | Validación previa a los repositorios (ver abajo). |
| `TipoIdentificador`, `GeneradorIdentificadores` | Consecutivos por prefijo. |
| `DiarioOperaciones` (contrato), `DiarioOperacionesJson`, `EntradaDiario`, `ImagenRegistro`, `EstadoEntradaDiario` | Diario de rehacer (7.6). |
| `CerrojoEscritura` | Monitor único que serializa el diario y los repositorios. |

**Lectura de un archivo.**
- Si no existe, está vacío (0 bytes o solo espacios) o contiene `[]`, se obtiene una
  lista vacía.
- JSON corrupto, algo que no es un arreglo, elementos `null` o registros que violan los
  invariantes del modelo producen una `PersistenciaException` con la ruta del archivo
  y la causa original. Así, un identificador mal formado impide el arranque.

**Integridad al iniciar** (`VerificadorIntegridad`):
- Identificaciones de usuario únicas.
- Identificadores generados en orden estrictamente ascendente en cada archivo (sin
  repetidos).
- Billeteras existentes en transacciones y operaciones programadas; transacción
  existente en la auditoría; original anterior en cada reversión.
- Coherencia de reversiones: una reversión EXITOSA implica la original REVERTIDA, y
  viceversa (exactamente una).
- No se verifican referencias a usuarios, porque su borrado está permitido.
- Consecuencia: una operación que cambia varios registros relacionados (como una
  reversión) **debe** hacerse con el diario. Si se escribe en dos pasos sueltos, un
  corte entre ambos dejaría datos que impiden el arranque.

**Generador de identificadores.**
- Arreglo de consecutivos por `TipoIdentificador` y `siguiente()` sincronizado.
- `desde(DatosCargados)` toma el máximo de cada archivo después de recuperar el
  diario; si no hay registros, empieza en 1.
- El formato tiene al menos 6 dígitos y crece después (`BIL-1000000`); el relleno con
  ceros es manual, porque `String.format` depende del `Locale`.
- Un formato inválido o el consecutivo 0 producen `PersistenciaException`. Nunca
  retrocede.

**Diario** (`diario-operaciones.json`, arreglo de `EntradaDiario`).
- Cada entrada tiene `id`, `fecha`, `estado` (PREPARADA) y sus imágenes `{archivo,
  identificador, contenido}`.
- El `contenido` es el **texto JSON** del registro. No se usa un árbol `JsonNode`,
  porque al convertir a árbol Jackson normaliza los decimales (por ejemplo `1050.00` →
  `1050.0`, o a `double`). Así los montos quedan exactos; hay una prueba de regresión.
- Aplicar una entrada: por archivo, en el orden de `ArchivoDominio`, cada imagen
  reemplaza al registro con su identificador o se agrega al final. Es idempotente.
- `recuperarPendientes()` aplica y retira cada entrada en orden. Si una falla, esa y
  las siguientes quedan en el diario y el arranque se detiene.
- `ejecutar(entrada, actualizarMemoria)` hace el flujo completo de 7.6 bajo el cerrojo:
  registrar, aplicar (con un reintento), actualizar la memoria y confirmar.
- El diario permite rehacer una operación interrumpida; **no** garantiza atomicidad
  física entre varios archivos.

**Orden de arranque** (`config.InicializadorDatos`, ejecutado al crear el bean
`ContextoDatos`):
1. Descartar temporales.
2. Recuperar el diario.
3. Cargar los 7 archivos.
4. Verificar la integridad.
5. Recuperar los consecutivos.
6. Construir los repositorios e índices.

Los repositorios se exponen como beans **desde** `ContextoDatos`, así que no existe un
repositorio sin inicializar. Si un paso falla, Spring no arranca.

### 7.8 Repositorios en memoria (fase 5)

`RepositorioEnMemoria<T>` es la base común:
- Tabla hash por identificador y lista con el orden de registro.
- Ganchos `indexar`, `desindexar`, `reindexar` y `verificar`.
- Cada cambio escribe **primero** el archivo completo y solo después modifica la
  memoria; si la escritura falla, nada cambia.
- La carga inicial usa el mismo `indexar` que un alta, así los índices reconstruidos
  son idénticos a los de insertar uno por uno.
- `reflejar(registro)` actualiza solo la memoria, para después de que el diario
  escribió el archivo.
- Todos los métodos públicos se sincronizan con `CerrojoEscritura`.
- Las estructuras se piden a `FabricaEstructuras` (implementada en
  `config.FabricaEstructurasPropias`), porque la regla 7 impide al repositorio conocer
  implementaciones.
- Los índices secundarios (`IndicePorClave`) guardan **identificadores** en orden de
  consecutivo y sin repetidos, así que nunca hay copias viejas.

| Repositorio | Estructuras | Operaciones |
|---|---|---|
| `RepositorioUsuarios` | Tabla hash por identificación; ABB `(puntos, identificación)` | registrar, buscar, actualizar (reubica en el ABB si cambian los puntos), eliminar, todos, `ordenadosPorPuntos`, `conMasPuntos(n)`, `porRangoDePuntos` (cotas de `ClaveCompuesta`) |
| `RepositorioBilleteras` | Tabla hash por código; lista de códigos por propietario | registrar, buscar, actualizar, todos, `porPropietario`, `propietarioDe`. Sin eliminar (INACTIVA). |
| `RepositorioTransacciones` | Tabla hash; listas de historial por usuario y por billetera; ABB `(valor, id)` | registrar, buscar, actualizar, todos, `historialDeUsuario`, `historialDeBilletera`, `ordenadasPorValor`, `mayoresPorValor(n)`, `porRangoDeValor`, `usuarioQueOrigino`, `reconstruirPilaReversibles(usuario, criterio)`, `reconstruirPilasReversibles(criterio)`, `construirGrafoTransferencias(criterio)` |
| `RepositorioOperacionesProgramadas` | Tabla hash; lista por usuario; cola de prioridad `(fechaEjecucion, id)` solo con PENDIENTES | registrar, buscar, actualizar (sale de la cola si deja de estar pendiente y se reubica si cambia la fecha), todos, `porUsuario`, `siguientePendiente`, `cantidadPendientes` |
| `RepositorioNotificaciones` | Tabla hash; lista por usuario; cola FIFO de pendientes con descarte perezoso y tabla de encoladas (sin duplicados) | registrar, buscar, actualizar, `pendientes`, `siguientePendiente`, `porUsuario`, `recientes` |
| `RepositorioAuditoria` | Tabla hash; lista histórica; lista por usuario | registrar, buscar, todos, `porUsuario`. Sin actualizar ni eliminar. |
| `RepositorioBeneficios` | Tabla hash; lista por usuario | registrar, buscar, actualizar, todos, `porUsuario`. Sin eliminar. |

Reglas de pertenencia a usuario:
- El historial de un usuario incluye las transacciones de cualquiera de sus
  billeteras: una transferencia entre dos usuarios aparece en ambos, una interna una
  sola vez.
- Una operación programada pertenece al dueño de la billetera origen o, en recargas,
  de la destino.

**Complejidad** (n = registros del archivo, k = resultados, h = altura del ABB):

| Operación | Costo |
|---|---|
| buscar por identificador | O(1) promedio |
| registrar, actualizar, eliminar | O(n), por reescribir el archivo completo, más los índices: ABB O(h); lista por clave O(1) normal; cola de prioridad O(log n) al insertar y O(n) al retirar |
| consultas por usuario o billetera | O(k) |
| consultas por puntos o valor | O(h + k) |
| siguiente operación programada | O(1) |
| siguiente notificación pendiente | O(1) amortizado |
| reconstrucción al iniciar | O(n·h) para el ABB y O(n log n) para la cola de prioridad |

**Estructuras derivadas cuyas reglas siguen pendientes** (no se construyen al
iniciar; los repositorios las reconstruyen a pedido):
- **Pila de reversibles** (7.5): `reconstruirPilaReversibles` recibe un
  `CriterioReversion`. El repositorio solo aplica lo ya decidido: originadas por el
  usuario, no reversiones y EXITOSAS. Los tipos reversibles y el plazo los define el
  criterio en la fase de servicios.
- **Grafo de transferencias**: `construirGrafoTransferencias` recibe un
  `CriterioPesoTransferencia`, que devuelve el peso de cada transferencia, o `null`
  para omitirla. Se pasa el usuario origen y destino para que decida también sobre las
  transferencias internas. El significado del peso se decide en la fase de analítica.

## 8. Convenciones de desarrollo

- Idioma: nombres de clases, métodos, variables, endpoints y documentación en
  español, sin tildes ni ñ en identificadores (`tamanio`, `anio`).
- Clases en `PascalCase`, métodos y variables en `camelCase`, constantes en `MAYUSCULAS`.
- Rutas REST en minúsculas con guiones (`/api/operaciones-programadas`).
- JSON con nombres de campo en `camelCase`; fechas ISO-8601.
- Inyección por constructor; los campos inyectados son `final`.
- Validación de forma (campos obligatorios, positivos) en DTO con Bean Validation;
  validación de negocio (saldo, existencia, estado) en `service`.
- Errores: excepciones propias en `exception`, nunca códigos de error en el retorno.
- No crear clases vacías ni duplicar interfaces o paquetes.
- Cada estructura usada debe tener su justificación técnica (para el informe final).
- Pruebas: JUnit 5 (incluido en los starters de test). Cada estructura y cada
  servicio con pruebas unitarias. `ArquitecturaTest` debe pasar siempre.

## 9. Fases de implementación (propuesta)

| Fase | Contenido | Estado |
|------|-----------|--------|
| 1 | Arquitectura, contratos de estructuras, contrato de persistencia, propuesta de API. | **Terminada** |
| 2 | Estructuras comunes: lista propia y adaptadores de pila, cola, cola de prioridad y tabla hash + pruebas unitarias. | **Terminada** |
| 3 | ABB y grafo propios + pruebas unitarias. | **Terminada** |
| 4 | Modelo (entidades y enums), DTO, excepciones y manejador global. | **Terminada** |
| 5 | Persistencia JSON y repositorios en memoria. | **Terminada** |
| 6 | Servicios, reglas de negocio, programadas, notificaciones, detección, analítica, grafo, benchmark, controladores REST, frontend y entregables (fases 6–10 de la propuesta original, consolidadas). | **Terminada** (ver `INFORME_TECNICO.md`; pruebas con Spring real pendientes de ejecutar con Maven) |

## 10. Discrepancias encontradas y resueltas en la fase 1

| Discrepancia | Resolución |
|---|---|
| `structures/package-info.java` decía "Interfaces propias de estructuras de datos", pero la arquitectura acordada ubica las interfaces en `structures.interfaces`. | Se actualizó la descripción: la raíz agrupa los subpaquetes `interfaces`, `adaptadores` y `propias`. |
| `structures/propias/package-info.java` mencionaba "grafo, AVL"; lo acordado es un ABB. | Se cambió a "ABB con clave compuesta y grafo dirigido ponderado". Un AVL es un ABB balanceado y cumpliría el mismo contrato; si el equipo decide balancearlo, no cambia la interfaz. |
| No existía el paquete `persistence`, previsto en la arquitectura. | Se creó con su `package-info.java` y el contrato `AlmacenJson`. |
| La regla 8 impide `java.util` fuera de `structures` (afecta `UUID`, `List`, `Optional`, `Comparator`). | Se diseñaron los contratos para no necesitarlos (ver sección 5). Identificadores resueltos en 7.4. |

### Cierre de decisiones de la fase 1

- Pesos del grafo y de `Arista`: `BigDecimal`.
- `beneficios.json` para beneficios canjeados.
- Identificadores prefijo + consecutivo, recuperados del máximo existente (7.4).
- Pila de reversibles reconstruida desde transacciones, con eliminación perezosa (7.5).
- Diario de rehacer para operaciones que afectan varios archivos (7.6).

## 11. Decisiones pendientes

> **Fase 6:** todas las decisiones de esta sección se resolvieron como políticas
> configurables (`application.properties`, prefijo `fintech.`) con valores
> provisionales centralizados en los métodos `provisional()` de las políticas de
> `service`. La tabla de decisiones con fuente y estado está en el anexo de
> `INFORME_TECNICO.md`. La lista se conserva como historial.


- Redondeo de puntos ("1 punto por cada 100 unidades": se asume división entera, por confirmar) y valor del bono de pago programado.
- Ventajas concretas de cada nivel (el enunciado da ejemplos, no valores).
- Umbrales de la detección de comportamiento inusual (tiempo "muy corto", valor "inusualmente alto", horario "no habitual").
- Tamaño del historial de notificaciones recientes.
- Qué operaciones son reversibles y por cuánto tiempo.
- Ejecución de programadas: tarea periódica de Spring o disparo manual de simulación (el enunciado pide "simular ejecución automática").
- Catálogo de beneficios canjeables con puntos.

Pendientes de la fase 5:
- Política de reversión (tipos y plazo), que se entrega como `CriterioReversion`.
- Significado del peso del grafo, estados que cuentan y transferencias internas, que
  se entregan como `CriterioPesoTransferencia`.
- Valor de `fintech.notificaciones.recientes-por-usuario` (hoy 0 = sin límite; solo
  limita la consulta).
- Qué hace el servicio con los usuarios eliminados que tienen billeteras, canjes o
  notificaciones: el repositorio permite eliminar y no se validan esas referencias.
- Rendimiento: cada cambio reescribe el archivo completo, lo cual es aceptable para el
  volumen académico.

Decisiones **provisionales** de la fase 4, pendientes de confirmación (el modelo las
representa sin imponer reglas):
- Datos básicos del usuario: `nombre` y `correo`.
- Escala de `NivelRiesgo` (NINGUNO, BAJO, MEDIO, ALTO) y los umbrales que la asignan.
- `Periodicidad`: UNICA, SEMANAL, MENSUAL.
- RETIRO como operación programable (para pagos recurrentes externos).
- `EstadoCanje`: REALIZADO, ANULADO.
- Signo de `puntosGenerados` en una reversión (el modelo no lo restringe) y qué pasa
  si revertir deja puntos negativos.
- Escala decimal de los montos (no se limita la cantidad de decimales).
- Zona horaria del `Clock` (hoy la del sistema).
- Reprogramación de periódicas: actualizar `fechaEjecucion` o crear un registro nuevo.
- DTO de reportes, canjes, benchmark y grafo (cuando se fijen sus parámetros).

## 12. Fase 6: servicios, API y frontend

- **Servicios** (`service`): `UsuarioService`, `BilleteraService`, `TransaccionService`,
  `OperacionProgramadaService`, `PuntosService`, `RecompensaService`,
  `NotificacionService`, `DeteccionService`, `AnaliticaService`, `GrafoService`.
  Las políticas son `PoliticaPuntos`, `PoliticaReversion` (implementa
  `CriterioReversion`), `PoliticaDeteccion`, `PoliticaNotificaciones`,
  `PoliticaProgramacion`, `CatalogoBeneficios` y `CriterioGrafo`. Todas se crean en
  `config/ConfiguracionNegocio`.
- **Escritura** (`EscrituraConDiario`): toda modificación pasa por ella.
  - Las operaciones de varios registros son una sola entrada del diario.
  - Si la escritura falla después de registrar la entrada, la memoria refleja la
    operación comprometida.
  - Con entradas pendientes, ninguna modificación se acepta sin reaplicarlas antes (7.6).
- **Programadas.** La ejecución automática se hace en
  `config/EjecucionAutomaticaProgramadas` (`@Scheduled`, desactivable). La manual usa
  `POST /api/operaciones-programadas/procesar`. Ambas usan
  `OperacionProgramadaService.procesarVencidas` y el `Clock` inyectado.
- **Controladores y DTO.** Los controladores (`controller`) son delgados; las rutas
  están en `API.md`. Los DTO nuevos son records con arreglos.
- **Benchmark** (`benchmark`): usa implementaciones concretas a propósito.
- **Frontend** (`static`): HTML, CSS y JS sin frameworks, con 10 pantallas. No contiene
  reglas financieras.
