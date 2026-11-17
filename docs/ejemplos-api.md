# Casos de ejecución de la API

> Generado por `EjemplosApiTest` (src/test/java/.../controller). Las respuestas salen de los
> controladores, servicios, persistencia y manejador de errores **reales**, invocados en el
> mismo proceso (sin servidor HTTP) y serializadas con el `JsonMapper` de la aplicación.
> El reloj está fijo en 2026-11-15T18:00 (America/Bogota) y avanza solo donde se indica.
> Para reproducirlos contra el servidor use `docs/pruebas.http` sobre un directorio de datos vacío;
> allí las fechas serán las del reloj real. Las validaciones de Bean Validation (400
> `VALIDACION_FALLIDA`) solo ocurren por HTTP y se ilustran en `docs/pruebas.http`.

## 1. Registro de usuario

Se registran dos usuarios; repetir la identificación es un conflicto.

```http
POST /api/usuarios
Content-Type: application/json

{
  "identificacion" : "1001",
  "nombre" : "Ana Gómez",
  "correo" : "ana@correo.co"
}
```

Respuesta `201`:

```json
{
  "identificacion" : "1001",
  "nombre" : "Ana Gómez",
  "correo" : "ana@correo.co",
  "puntosAcumulados" : 0,
  "nivel" : "BRONCE"
}
```

```http
POST /api/usuarios
Content-Type: application/json

{
  "identificacion" : "2002",
  "nombre" : "Luis Pérez",
  "correo" : "luis@correo.co"
}
```

Respuesta `201`:

```json
{
  "identificacion" : "2002",
  "nombre" : "Luis Pérez",
  "correo" : "luis@correo.co",
  "puntosAcumulados" : 0,
  "nivel" : "BRONCE"
}
```

```http
POST /api/usuarios
Content-Type: application/json

{
  "identificacion" : "1001",
  "nombre" : "Otra",
  "correo" : "otra@correo.co"
}
```

Respuesta `409`:

```json
{
  "estado" : 409,
  "error" : "USUARIO_YA_EXISTE",
  "mensaje" : "Ya existe un usuario con la identificación 1001",
  "fecha" : "2026-11-15T18:00:00"
}
```

## 2. Creación de billeteras

El código es consecutivo; el saldo inicial es 0 y la billetera queda ACTIVA.

```http
POST /api/usuarios/1001/billeteras
Content-Type: application/json

{
  "nombre" : "Ahorros",
  "tipo" : "AHORRO"
}
```

Respuesta `201`:

```json
{
  "codigo" : "BIL-000001",
  "identificacionPropietario" : "1001",
  "nombre" : "Ahorros",
  "tipo" : "AHORRO",
  "saldo" : 0,
  "estado" : "ACTIVA"
}
```

```http
POST /api/usuarios/1001/billeteras
Content-Type: application/json

{
  "nombre" : "Compras",
  "tipo" : "COMPRAS"
}
```

Respuesta `201`:

```json
{
  "codigo" : "BIL-000002",
  "identificacionPropietario" : "1001",
  "nombre" : "Compras",
  "tipo" : "COMPRAS",
  "saldo" : 0,
  "estado" : "ACTIVA"
}
```

```http
POST /api/usuarios/2002/billeteras
Content-Type: application/json

{
  "nombre" : "Ahorros",
  "tipo" : "AHORRO"
}
```

Respuesta `201`:

```json
{
  "codigo" : "BIL-000003",
  "identificacionPropietario" : "2002",
  "nombre" : "Ahorros",
  "tipo" : "AHORRO",
  "saldo" : 0,
  "estado" : "ACTIVA"
}
```

## 3. Recarga

1 punto por cada 100 completos (política provisional): 1500.50 → 15 puntos.

```http
POST /api/transacciones/recargas
Content-Type: application/json

{
  "billeteraDestino" : "BIL-000001",
  "valor" : 1500.50
}
```

Respuesta `201`:

```json
{
  "id" : "TRX-000001",
  "fecha" : "2026-11-15T18:00:00",
  "tipo" : "RECARGA",
  "valor" : 1500.50,
  "billeteraOrigen" : null,
  "billeteraDestino" : "BIL-000001",
  "estado" : "EXITOSA",
  "puntosGenerados" : 15,
  "nivelRiesgo" : "NINGUNO",
  "transaccionRevertida" : null
}
```

## 4. Retiro

2 puntos por cada 100. Un retiro mayor que el saldo responde 409 y queda RECHAZADO en el historial.

```http
POST /api/transacciones/retiros
Content-Type: application/json

{
  "billeteraOrigen" : "BIL-000001",
  "valor" : 200
}
```

Respuesta `201`:

```json
{
  "id" : "TRX-000002",
  "fecha" : "2026-11-15T18:00:00",
  "tipo" : "RETIRO",
  "valor" : 200,
  "billeteraOrigen" : "BIL-000001",
  "billeteraDestino" : null,
  "estado" : "EXITOSA",
  "puntosGenerados" : 4,
  "nivelRiesgo" : "NINGUNO",
  "transaccionRevertida" : null
}
```

```http
POST /api/transacciones/retiros
Content-Type: application/json

{
  "billeteraOrigen" : "BIL-000002",
  "valor" : 10
}
```

Respuesta `409`:

```json
{
  "estado" : 409,
  "error" : "SALDO_INSUFICIENTE",
  "mensaje" : "Saldo insuficiente en BIL-000002 para retiro de 10 (registrada como TRX-000003 RECHAZADA)",
  "fecha" : "2026-11-15T18:10:00"
}
```

```http
GET /api/billeteras/BIL-000002/transacciones
```

Respuesta `200`:

```json
[ {
  "id" : "TRX-000003",
  "fecha" : "2026-11-15T18:10:00",
  "tipo" : "RETIRO",
  "valor" : 10,
  "billeteraOrigen" : "BIL-000002",
  "billeteraDestino" : null,
  "estado" : "RECHAZADA",
  "puntosGenerados" : 0,
  "nivelRiesgo" : "NINGUNO",
  "transaccionRevertida" : null
} ]
```

## 5. Transferencia

Entre usuarios distintos; 3 puntos por cada 100 para quien envía.

```http
POST /api/transacciones/transferencias
Content-Type: application/json

{
  "billeteraOrigen" : "BIL-000001",
  "billeteraDestino" : "BIL-000003",
  "valor" : 300.25,
  "billeterasDistintas" : true
}
```

Respuesta `201`:

```json
{
  "id" : "TRX-000004",
  "fecha" : "2026-11-15T18:20:00",
  "tipo" : "TRANSFERENCIA",
  "valor" : 300.25,
  "billeteraOrigen" : "BIL-000001",
  "billeteraDestino" : "BIL-000003",
  "estado" : "EXITOSA",
  "puntosGenerados" : 9,
  "nivelRiesgo" : "NINGUNO",
  "transaccionRevertida" : null
}
```

```http
GET /api/billeteras/BIL-000001/saldo
```

Respuesta `200`:

```json
{
  "codigoBilletera" : "BIL-000001",
  "saldo" : 1000.25
}
```

## 6. Reversión

Deshace la última operación reversible del usuario (cima de su pila): la transferencia. Devuelve el dinero, marca la original REVERTIDA y descuenta sus puntos. Revertirla otra vez es un conflicto, y un retiro no es reversible con la política provisional.

```http
POST /api/usuarios/1001/reversion/ultima
```

Respuesta `201`:

```json
{
  "id" : "TRX-000005",
  "fecha" : "2026-11-15T18:30:00",
  "tipo" : "REVERSION",
  "valor" : 300.25,
  "billeteraOrigen" : "BIL-000003",
  "billeteraDestino" : "BIL-000001",
  "estado" : "EXITOSA",
  "puntosGenerados" : -9,
  "nivelRiesgo" : "NINGUNO",
  "transaccionRevertida" : "TRX-000004"
}
```

```http
POST /api/transacciones/TRX-000004/reversion
```

Respuesta `409`:

```json
{
  "estado" : 409,
  "error" : "TRANSACCION_YA_REVERTIDA",
  "mensaje" : "La transacción TRX-000004 ya fue revertida",
  "fecha" : "2026-11-15T18:30:00"
}
```

```http
POST /api/transacciones/TRX-000002/reversion
```

Respuesta `409`:

```json
{
  "estado" : 409,
  "error" : "OPERACION_NO_REVERSIBLE",
  "mensaje" : "Las transacciones de tipo RETIRO no son reversibles según la política vigente",
  "fecha" : "2026-11-15T18:30:00"
}
```

## 7. Programación de una operación

Transferencia semanal de 1001 a 2002 dentro de una hora.

```http
POST /api/operaciones-programadas
Content-Type: application/json

{
  "tipo" : "TRANSFERENCIA",
  "valor" : 100,
  "billeteraOrigen" : "BIL-000001",
  "billeteraDestino" : "BIL-000003",
  "fechaEjecucion" : "2026-11-15T19:30:00",
  "periodicidad" : "SEMANAL",
  "billeterasCoherentesConElTipo" : true
}
```

Respuesta `201`:

```json
{
  "id" : "OPR-000001",
  "tipo" : "TRANSFERENCIA",
  "valor" : 100,
  "billeteraOrigen" : "BIL-000001",
  "billeteraDestino" : "BIL-000003",
  "fechaEjecucion" : "2026-11-15T19:30:00",
  "periodicidad" : "SEMANAL",
  "estado" : "PENDIENTE"
}
```

```http
GET /api/operaciones-programadas/proxima
```

Respuesta `200`:

```json
{
  "id" : "OPR-000001",
  "tipo" : "TRANSFERENCIA",
  "valor" : 100,
  "billeteraOrigen" : "BIL-000001",
  "billeteraDestino" : "BIL-000003",
  "fechaEjecucion" : "2026-11-15T19:30:00",
  "periodicidad" : "SEMANAL",
  "estado" : "PENDIENTE"
}
```

## 8. Ejecución simulada

Se adelanta el reloj 2 horas y se procesan las vencidas: se ejecuta con bono de 10 puntos (provisional) y se crea la siguiente ocurrencia semanal.

```http
POST /api/operaciones-programadas/procesar
```

Respuesta `200`:

```json
{
  "procesadoHasta" : "2026-11-15T20:30:00",
  "ejecutadas" : 1,
  "fallidas" : 0,
  "resultados" : [ {
    "operacion" : "OPR-000001",
    "transaccion" : "TRX-000006",
    "estado" : "EJECUTADA",
    "motivo" : null,
    "siguienteOperacion" : "OPR-000002"
  } ]
}
```

## 9. Acumulación de puntos

Puntos y nivel actuales del usuario 1001.

```http
GET /api/usuarios/1001/puntos
```

Respuesta `200`:

```json
{
  "identificacion" : "1001",
  "puntosAcumulados" : 32,
  "nivel" : "BRONCE",
  "siguienteNivel" : "PLATA",
  "puntosParaSiguienteNivel" : 469
}
```

```http
POST /api/usuarios/1001/canjes
Content-Type: application/json

{
  "beneficio" : "CAFE"
}
```

Respuesta `201`:

```json
{
  "id" : "CAN-000001",
  "identificacionUsuario" : "1001",
  "beneficio" : "CAFE - Bono de café",
  "puntosDescontados" : 20,
  "fecha" : "2026-11-15T20:30:00",
  "estado" : "REALIZADO"
}
```

## 10. Ascenso de nivel

Una recarga de 50100 suma 501 puntos: el usuario 2002 pasa de BRONCE (0–500) a PLATA (501–1000) y recibe una notificación ASCENSO_NIVEL.

```http
POST /api/transacciones/recargas
Content-Type: application/json

{
  "billeteraDestino" : "BIL-000003",
  "valor" : 50100
}
```

Respuesta `201`:

```json
{
  "id" : "TRX-000007",
  "fecha" : "2026-11-15T20:30:00",
  "tipo" : "RECARGA",
  "valor" : 50100,
  "billeteraOrigen" : null,
  "billeteraDestino" : "BIL-000003",
  "estado" : "EXITOSA",
  "puntosGenerados" : 501,
  "nivelRiesgo" : "NINGUNO",
  "transaccionRevertida" : null
}
```

```http
GET /api/usuarios/2002/notificaciones
```

Respuesta `200`:

```json
[ {
  "id" : "NOT-000007",
  "identificacionUsuario" : "2002",
  "tipo" : "ASCENSO_NIVEL",
  "mensaje" : "Subiste al nivel PLATA con 501 puntos",
  "fecha" : "2026-11-15T20:30:00",
  "estado" : "PENDIENTE"
}, {
  "id" : "NOT-000004",
  "identificacionUsuario" : "2002",
  "tipo" : "SALDO_BAJO",
  "mensaje" : "La billetera BIL-000003 quedó con saldo bajo: 0.00",
  "fecha" : "2026-11-15T18:30:00",
  "estado" : "PENDIENTE"
} ]
```

## 11. Detección de comportamiento inusual

Tres transferencias en menos de 5 minutos (política provisional): la tercera queda con riesgo MEDIO, se audita y se notifica ALERTA_SEGURIDAD.

```http
POST /api/transacciones/transferencias
Content-Type: application/json

{
  "billeteraOrigen" : "BIL-000003",
  "billeteraDestino" : "BIL-000001",
  "valor" : 10,
  "billeterasDistintas" : true
}
```

Respuesta `201`:

```json
{
  "id" : "TRX-000008",
  "fecha" : "2026-11-15T20:31:00",
  "tipo" : "TRANSFERENCIA",
  "valor" : 10,
  "billeteraOrigen" : "BIL-000003",
  "billeteraDestino" : "BIL-000001",
  "estado" : "EXITOSA",
  "puntosGenerados" : 0,
  "nivelRiesgo" : "NINGUNO",
  "transaccionRevertida" : null
}
```

```http
POST /api/transacciones/transferencias
Content-Type: application/json

{
  "billeteraOrigen" : "BIL-000003",
  "billeteraDestino" : "BIL-000002",
  "valor" : 10,
  "billeterasDistintas" : true
}
```

Respuesta `201`:

```json
{
  "id" : "TRX-000009",
  "fecha" : "2026-11-15T20:32:00",
  "tipo" : "TRANSFERENCIA",
  "valor" : 10,
  "billeteraOrigen" : "BIL-000003",
  "billeteraDestino" : "BIL-000002",
  "estado" : "EXITOSA",
  "puntosGenerados" : 0,
  "nivelRiesgo" : "NINGUNO",
  "transaccionRevertida" : null
}
```

```http
POST /api/transacciones/transferencias
Content-Type: application/json

{
  "billeteraOrigen" : "BIL-000003",
  "billeteraDestino" : "BIL-000001",
  "valor" : 15,
  "billeterasDistintas" : true
}
```

Respuesta `201`:

```json
{
  "id" : "TRX-000010",
  "fecha" : "2026-11-15T20:33:00",
  "tipo" : "TRANSFERENCIA",
  "valor" : 15,
  "billeteraOrigen" : "BIL-000003",
  "billeteraDestino" : "BIL-000001",
  "estado" : "EXITOSA",
  "puntosGenerados" : 0,
  "nivelRiesgo" : "MEDIO",
  "transaccionRevertida" : null
}
```

```http
GET /api/auditoria
```

Respuesta `200`:

```json
[ {
  "id" : "AUD-000001",
  "identificacionUsuario" : "2002",
  "transaccion" : "TRX-000010",
  "patron" : "TRANSFERENCIAS_CONSECUTIVAS_RAPIDAS",
  "descripcion" : "3 transferencias en 5 minutos",
  "nivelRiesgo" : "MEDIO",
  "fecha" : "2026-11-15T20:33:00"
} ]
```

## 12. Consulta de analítica

Reportes sobre transacciones efectivas (EXITOSA y no reversión).

```http
GET /api/reportes/billeteras-mas-usadas?cantidad=3
```

Respuesta `200`:

```json
[ {
  "clave" : "BIL-000001",
  "cantidad" : 5
}, {
  "clave" : "BIL-000003",
  "cantidad" : 5
}, {
  "clave" : "BIL-000002",
  "cantidad" : 1
} ]
```

```http
GET /api/reportes/frecuencia-por-tipo
```

Respuesta `200`:

```json
[ {
  "clave" : "RECARGA",
  "cantidad" : 2
}, {
  "clave" : "RETIRO",
  "cantidad" : 1
}, {
  "clave" : "TRANSFERENCIA",
  "cantidad" : 4
}, {
  "clave" : "REVERSION",
  "cantidad" : 1
} ]
```

```http
GET /api/reportes/monto-movilizado?desde=2026-11-15T00:00:00&hasta=2026-11-16T00:00:00
```

Respuesta `200`:

```json
{
  "desde" : "2026-11-15T00:00:00",
  "hasta" : "2026-11-16T00:00:00",
  "total" : 51935.50,
  "transacciones" : 7
}
```

## 13. Rutas y ciclos

Grafo entre usuarios (peso = cantidad de transferencias exitosas; la revertida no cuenta). 1001 → 2002 (programada) y 2002 → 1001 forman un ciclo.

```http
GET /api/grafo/transferencias
```

Respuesta `200`:

```json
{
  "criterio" : "CANTIDAD",
  "vertices" : [ "1001", "2002" ],
  "aristas" : [ {
    "origen" : "1001",
    "destino" : "2002",
    "peso" : 1
  }, {
    "origen" : "2002",
    "destino" : "1001",
    "peso" : 3
  } ],
  "tieneCiclo" : true
}
```

```http
GET /api/grafo/rutas?origen=2002&destino=1001
```

Respuesta `200`:

```json
{
  "origen" : "2002",
  "destino" : "1001",
  "criterio" : "CANTIDAD",
  "menosSaltos" : [ "2002", "1001" ],
  "menorPeso" : [ "2002", "1001" ],
  "pesoMenor" : 3
}
```

```http
GET /api/grafo/ciclos?usuario=1001&longitudMaxima=4
```

Respuesta `200`:

```json
{
  "usuario" : "1001",
  "longitudMaxima" : 4,
  "ciclos" : [ [ "1001", "2002", "1001" ] ]
}
```

## 14. Recuperación de una operación interrumpida

Se simula que `transacciones.json` no se puede escribir. La operación ya quedó en el diario: responde 500 (mensaje genérico) y no se aceptan otras modificaciones hasta completarla. Al reiniciar la aplicación con el archivo disponible, el diario la completa y el saldo y el historial quedan consistentes.

```http
POST /api/transacciones/recargas
Content-Type: application/json

{
  "billeteraDestino" : "BIL-000002",
  "valor" : 75.50
}
```

Respuesta `500`:

```json
{
  "estado" : 500,
  "error" : "ERROR_PERSISTENCIA",
  "mensaje" : "No fue posible acceder a los datos. Intente más tarde.",
  "fecha" : "2026-11-15T20:33:00"
}
```

*(reinicio de la aplicación con `transacciones.json` de nuevo escribible)*

```http
GET /api/billeteras/BIL-000002/saldo
```

Respuesta `200`:

```json
{
  "codigoBilletera" : "BIL-000002",
  "saldo" : 85.50
}
```

```http
GET /api/billeteras/BIL-000002/transacciones
```

Respuesta `200`:

```json
[ {
  "id" : "TRX-000003",
  "fecha" : "2026-11-15T18:10:00",
  "tipo" : "RETIRO",
  "valor" : 10,
  "billeteraOrigen" : "BIL-000002",
  "billeteraDestino" : null,
  "estado" : "RECHAZADA",
  "puntosGenerados" : 0,
  "nivelRiesgo" : "NINGUNO",
  "transaccionRevertida" : null
}, {
  "id" : "TRX-000009",
  "fecha" : "2026-11-15T20:32:00",
  "tipo" : "TRANSFERENCIA",
  "valor" : 10,
  "billeteraOrigen" : "BIL-000003",
  "billeteraDestino" : "BIL-000002",
  "estado" : "EXITOSA",
  "puntosGenerados" : 0,
  "nivelRiesgo" : "NINGUNO",
  "transaccionRevertida" : null
}, {
  "id" : "TRX-000011",
  "fecha" : "2026-11-15T20:33:00",
  "tipo" : "RECARGA",
  "valor" : 75.50,
  "billeteraOrigen" : null,
  "billeteraDestino" : "BIL-000002",
  "estado" : "EXITOSA",
  "puntosGenerados" : 0,
  "nivelRiesgo" : "NINGUNO",
  "transaccionRevertida" : null
} ]
```
