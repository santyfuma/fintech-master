# Contrato de la API

> Contrato implementado en la fase 6 (paquete `controller`). Cada ruta indica el
> requisito del enunciado que atiende. Los ejemplos completos de solicitud y respuesta
> están en `ejemplos-api.md` y la secuencia ejecutable en `pruebas.http`.
> Simulación académica: no hay autenticación.

## Convenciones

- Prefijo: `/api`. Formato: JSON (`application/json`), campos en `camelCase`.
- Fechas y horas: ISO-8601 (`2026-11-15T18:00:00`). Montos: número decimal.
- Colecciones en las respuestas: arreglos JSON.
- Códigos: `200` consulta/actualización, `201` creación, `204` eliminación sin cuerpo,
  `400` validación, `404` recurso inexistente, `409` conflicto de negocio
  (saldo insuficiente, billetera inactiva, operación no reversible).
- Cuerpo de error uniforme (generado por el manejador global de `exception`):

```json
{ "estado": 409, "error": "SALDO_INSUFICIENTE", "mensaje": "…", "fecha": "2026-11-15T18:00:00" }
```

Códigos de error generales (fase 4, `ManejadorGlobalExcepciones`):

| HTTP | `error` | Cuándo |
|---|---|---|
| 400 | `VALIDACION_FALLIDA` | Bean Validation; `mensaje` lista `campo: motivo` separados por `; ` |
| 400 | `SOLICITUD_MAL_FORMADA` | cuerpo que no es JSON válido o con valores no permitidos (p. ej. un enum inexistente) |
| 400 | `PARAMETRO_FALTANTE` / `PARAMETRO_INVALIDO` | parámetro de consulta ausente o de tipo incorrecto |
| 400 | código propio o `SOLICITUD_INVALIDA` | `SolicitudInvalidaException` |
| 404 | código propio o `RECURSO_NO_ENCONTRADO` | `RecursoNoEncontradoException` o ruta inexistente |
| 405 / 406 / 415 | `METODO_NO_PERMITIDO` / `FORMATO_NO_ACEPTABLE` / `TIPO_CONTENIDO_NO_SOPORTADO` | errores estándar de Spring MVC |
| 409 | código propio (p. ej. `SALDO_INSUFICIENTE`) o `CONFLICTO_NEGOCIO` | `ConflictoNegocioException` |
| 500 | `ERROR_PERSISTENCIA` / `ERROR_INTERNO` | fallo de archivos o error no previsto; mensaje genérico, sin detalles técnicos |

Los códigos específicos de cada servicio aparecen en cada sección. Un rechazo de negocio
de un movimiento (409 `SALDO_INSUFICIENTE` o `BILLETERA_INACTIVA`) **sí** queda
registrado como transacción RECHAZADA; los errores 400 de una solicitud no registran nada.

## 1. Usuarios — RF1, 4.1, 4.5

| Método | Ruta | Cuerpo / parámetros | Respuesta | Errores |
|---|---|---|---|---|
| POST | `/api/usuarios` | `SolicitudRegistroUsuario {identificacion, nombre, correo}` | 201 `UsuarioRespuesta` | 400, 409 `USUARIO_YA_EXISTE` |
| GET | `/api/usuarios` | — | 200 `UsuarioRespuesta[]` (orden de registro) | |
| GET | `/api/usuarios/{id}` | — | 200 `UsuarioRespuesta` | 404 `USUARIO_NO_ENCONTRADO` |
| PUT | `/api/usuarios/{id}` | `SolicitudActualizacionUsuario {nombre, correo}` | 200 `UsuarioRespuesta` | 400, 404 |
| DELETE | `/api/usuarios/{id}` | — | 204 | 404, 409 `USUARIO_CON_REGISTROS` (tiene billeteras, canjes, notificaciones o auditoría) |
| GET | `/api/usuarios/{id}/puntos` | — | 200 `PuntosRespuesta {puntosAcumulados, nivel, siguienteNivel, puntosParaSiguienteNivel}` (null en PLATINO) | 404 |
| GET | `/api/usuarios/por-puntos` | `min`, `max` opcionales | 200 `UsuarioRespuesta[]` ascendente por puntos (ABB) | 400 `RANGO_INVALIDO` |
| GET | `/api/usuarios/por-nivel/{nivel}` | BRONCE, PLATA, ORO, PLATINO | 200 `UsuarioRespuesta[]` | 400 `PARAMETRO_INVALIDO` |
| GET | `/api/usuarios/mayor-puntaje` | — | 200 `UsuarioRespuesta` | 404 `SIN_USUARIOS` |
| GET | `/api/usuarios/{id}/saldo-total` | — | 200 `SaldoTotalRespuesta {identificacion, saldoTotal, billeteras}` | 404 |

## 2. Billeteras — RF2, 4.1

| Método | Ruta | Cuerpo / parámetros | Respuesta | Errores |
|---|---|---|---|---|
| POST | `/api/usuarios/{id}/billeteras` | `SolicitudCreacionBilletera {nombre, tipo}` | 201 `BilleteraRespuesta` (saldo 0, ACTIVA) | 400, 404 |
| GET | `/api/usuarios/{id}/billeteras` | — | 200 `BilleteraRespuesta[]` | 404 |
| GET | `/api/billeteras/{codigo}` | — | 200 `BilleteraRespuesta` | 404 `BILLETERA_NO_ENCONTRADA` |
| GET | `/api/billeteras/{codigo}/saldo` | — | 200 `SaldoRespuesta` | 404 |
| PUT | `/api/billeteras/{codigo}` | `SolicitudActualizacionBilletera {nombre, tipo, estado}` | 200 `BilleteraRespuesta` | 400, 404, 409 `BILLETERA_CON_SALDO` |
| DELETE | `/api/billeteras/{codigo}` | — | 200 `BilleteraRespuesta` (INACTIVA; nunca se borra) | 404, 409 `BILLETERA_CON_SALDO` |
| POST | `/api/billeteras/{codigo}/activacion` | — | 200 `BilleteraRespuesta` | 404 |

Tipos: AHORRO, GASTOS_DIARIOS, COMPRAS, TRANSPORTE, INVERSION, SUSCRIPCIONES.

## 3. Transacciones — RF3, RF4, 4.2

| Método | Ruta | Cuerpo | Respuesta | Errores |
|---|---|---|---|---|
| POST | `/api/transacciones/recargas` | `SolicitudRecarga {billeteraDestino, valor}` | 201 `TransaccionRespuesta` | 400 `VALIDACION_FALLIDA`/`VALOR_INVALIDO`, 404, 409 `BILLETERA_INACTIVA` |
| POST | `/api/transacciones/retiros` | `SolicitudRetiro {billeteraOrigen, valor}` | 201 | 400, 404, 409 `SALDO_INSUFICIENTE`/`BILLETERA_INACTIVA` |
| POST | `/api/transacciones/transferencias` | `SolicitudTransferencia {billeteraOrigen, billeteraDestino, valor}` | 201 | 400 (misma billetera), 404, 409 |
| GET | `/api/transacciones` | — | 200 `TransaccionRespuesta[]` | |
| GET | `/api/transacciones/{id}` | — | 200 | 404 `TRANSACCION_NO_ENCONTRADA` |
| GET | `/api/usuarios/{id}/transacciones` | — | 200 historial (toca alguna billetera del usuario) | 404 |
| GET | `/api/billeteras/{codigo}/transacciones` | — | 200 historial de la billetera | 404 |

`TransaccionRespuesta` incluye `puntosGenerados` y `nivelRiesgo` (detección, 4.9).

## 4. Reversiones — RF9, 4.6

| Método | Ruta | Respuesta | Errores |
|---|---|---|---|
| POST | `/api/transacciones/{id}/reversion` | 201 `TransaccionRespuesta` de tipo REVERSION (`transaccionRevertida` = id) | 404, 409 `OPERACION_NO_REVERSIBLE`, `TRANSACCION_YA_REVERTIDA`, `PLAZO_DE_REVERSION_VENCIDO`, `SALDO_INSUFICIENTE`, `BILLETERA_INACTIVA` |
| POST | `/api/usuarios/{id}/reversion/ultima` | 201 (cima de la pila del usuario) | 404, 409 `SIN_OPERACIONES_REVERSIBLES` y los anteriores |

## 5. Operaciones programadas — RF5, RF6, 4.3

| Método | Ruta | Cuerpo / parámetros | Respuesta | Errores |
|---|---|---|---|---|
| POST | `/api/operaciones-programadas` | `SolicitudOperacionProgramada {tipo, valor, billeteraOrigen, billeteraDestino, fechaEjecucion, periodicidad}` | 201 `OperacionProgramadaRespuesta` | 400 `FECHA_EJECUCION_PASADA`, `BILLETERAS_INCOHERENTES`, `RETIRO_PROGRAMADO_NO_PERMITIDO`; 404; 409 `BILLETERA_INACTIVA` |
| GET | `/api/operaciones-programadas` | `estado` opcional | 200 `OperacionProgramadaRespuesta[]` | 400 |
| GET | `/api/operaciones-programadas/proxima` | — | 200 la siguiente de la cola de prioridad | 404 `SIN_OPERACIONES_PENDIENTES` |
| GET | `/api/operaciones-programadas/{id}` | — | 200 | 404 `OPERACION_NO_ENCONTRADA` |
| DELETE | `/api/operaciones-programadas/{id}` | — | 200 (CANCELADA) | 404, 409 `OPERACION_NO_CANCELABLE` |
| GET | `/api/usuarios/{id}/operaciones-programadas` | — | 200 | 404 |
| POST | `/api/operaciones-programadas/procesar` | — | 200 `ProcesamientoRespuesta {procesadoHasta, ejecutadas, fallidas, resultados[]}` | |

`procesar` ejecuta las vencidas según el reloj de la aplicación (simulación manual). La
misma lógica la invoca una tarea periódica (`fintech.programadas.automatico`, cada
`fintech.programadas.intervalo-ms`). Una operación que no puede ejecutarse queda FALLIDA
con su transacción RECHAZADA; una periódica genera la siguiente ocurrencia.
*Cambio respecto a la propuesta de la fase 1:* no se acepta un parámetro `hasta`; el
tiempo lo fija el `Clock` para que la simulación sea coherente con el resto del sistema.

## 6. Recompensas — RF7, RF8, 4.4, 4.5

| Método | Ruta | Cuerpo | Respuesta | Errores |
|---|---|---|---|---|
| GET | `/api/niveles` | — | 200 `NivelRespuesta[] {nivel, puntosMinimos, puntosMaximos (null en PLATINO), ventajas[]}` | |
| GET | `/api/beneficios` | — | 200 `BeneficioCatalogoRespuesta[]` (vacío si no hay catálogo configurado) | |
| POST | `/api/usuarios/{id}/canjes` | `SolicitudCanje {beneficio}` | 201 `BeneficioCanjeadoRespuesta` | 400, 404 `BENEFICIO_NO_ENCONTRADO`, 409 `CATALOGO_NO_DEFINIDO`, `PUNTOS_INSUFICIENTES` |
| GET | `/api/usuarios/{id}/canjes` | — | 200 `BeneficioCanjeadoRespuesta[]` | 404 |

## 7. Notificaciones — RF10, 4.7

| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/api/usuarios/{id}/notificaciones` | 200 `NotificacionRespuesta[]`, la más reciente primero (límite configurable) |
| GET | `/api/notificaciones/pendientes` | 200 pendientes en orden FIFO |
| POST | `/api/notificaciones/despachar` | 200 las despachadas (simulación: cambia el estado a DESPACHADA) |

## 8. Auditoría — 4.9

| Método | Ruta | Parámetros | Respuesta |
|---|---|---|---|
| GET | `/api/auditoria` | `riesgo` opcional | 200 `EventoAuditoriaRespuesta[]` |
| GET | `/api/usuarios/{id}/auditoria` | `riesgo` opcional | 200; 404 si el usuario no existe |

## 9. Reportes — RF11, 4.8, 8

| Método | Ruta | Parámetros | Respuesta |
|---|---|---|---|
| GET | `/api/reportes/billeteras-mas-usadas` | `cantidad` (1–100, defecto 5) | `ConteoRespuesta[] {clave, cantidad}` |
| GET | `/api/reportes/usuarios-mas-transferencias` | `cantidad` | `ConteoRespuesta[]` |
| GET | `/api/reportes/categorias-activas` | — | `ConteoRespuesta[]` por tipo de billetera |
| GET | `/api/reportes/frecuencia-por-tipo` | — | `ConteoRespuesta[]` (las 4 claves de `TipoTransaccion`) |
| GET | `/api/reportes/monto-movilizado` | `desde`, `hasta` (ISO, obligatorios) | `MontoMovilizadoRespuesta {total, transacciones}` |
| GET | `/api/reportes/usuario-mas-activo` | `desde`, `hasta` | `ConteoRespuesta`; 404 `SIN_ACTIVIDAD` |
| GET | `/api/reportes/transacciones-mayor-valor` | `cantidad` | `TransaccionRespuesta[]` (ABB por valor) |
| GET | `/api/reportes/usuarios-por-puntos` | — | `UsuarioRespuesta[]` |

Criterios: los reportes cuentan solo transacciones **efectivas** (EXITOSA y no
reversión), excepto `frecuencia-por-tipo`, que cuenta todas las EXITOSA, incluidas las
reversiones, para mostrar cuántas hubo. Las RECHAZADA y las REVERTIDA no cuentan. Los
empates se resuelven por clave ascendente. Errores: 400 `CANTIDAD_INVALIDA`,
`PERIODO_INVALIDO`, `PARAMETRO_FALTANTE`.

## 10. Grafo de transferencias — RF12, 4.8, 8

Vértices: usuarios. Arista A → B: transferencias EXITOSAS de A a B (las internas de un
mismo usuario y las revertidas se excluyen). Peso: `criterio=CANTIDAD` (número de
transferencias, por defecto) o `MONTO` (suma).

| Método | Ruta | Parámetros | Respuesta |
|---|---|---|---|
| GET | `/api/grafo/transferencias` | `criterio` | `GrafoRespuesta {criterio, vertices[], aristas[], tieneCiclo}` |
| GET | `/api/grafo/rutas` | `origen`, `destino`, `criterio` | `RutaRespuesta {menosSaltos[], menorPeso[], pesoMenor}` (vacíos si no hay ruta); 404 `USUARIO_SIN_TRANSFERENCIAS` |
| GET | `/api/grafo/rutas-frecuentes` | `cantidad`, `criterio` | `AristaRespuesta[]` de mayor peso |
| GET | `/api/grafo/ciclos` | `usuario`, `longitudMaxima` (1–10, defecto 4) | `CiclosRespuesta {ciclos[][]}`; 400 `LONGITUD_INVALIDA` |

## 11. Benchmark — 8

| Método | Ruta | Parámetros | Respuesta |
|---|---|---|---|
| GET | `/api/benchmark` | `comparacion` (BUSQUEDA, CONSULTA_ORDENADA, PRIORIDAD, RECORRIDO_GRAFO), `elementos` (10–5000, defecto 1000), `repeticiones` (1–20, defecto 5) | `BenchmarkRespuesta {mediciones[], limitaciones}`; 400 `PARAMETRO_BENCHMARK_INVALIDO` |

## 12. Interfaz gráfica

`GET /` sirve `static/index.html` (ver `INFORME_TECNICO.md`, sección 13).
