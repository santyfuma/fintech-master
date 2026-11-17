# Datos de demostración

El proyecto incluye datos **ficticios** para mostrar la aplicación funcionando. Ninguna
persona, correo ni monto es real.

- `data/`: la carpeta que la aplicación carga al arrancar. Viene precargada con los datos
  de demostración y cambia a medida que se usa la aplicación.
- `datos-demo/`: copia original que no se modifica. Sirve para restaurar la
  demostración.

## Cómo se generaron

Los JSON no se escribieron a mano: los produce `GeneradorDatosDemo`
(`src/test/java/.../demo/`). El generador ejecuta una historia del 1 de octubre al 15 de
noviembre de 2026 con los **servicios reales** y un reloj controlado. Por eso los saldos,
puntos, niveles, notificaciones, auditoría e identificadores cumplen las mismas reglas
que en el uso normal, y el verificador de integridad los acepta al arrancar.

`DatosDemoTest` comprueba tres cosas en cada `mvnw test`:
- los archivos cargan sin errores;
- contienen los casos de esta guía;
- regenerarlos produce exactamente los mismos registros.

El catálogo de beneficios y las ventajas por nivel de `application.properties` son
**valores de demostración**. El enunciado no los define y deben reemplazarse si el
profesor fija otros.

## Usuarios

| Identificación | Nombre | Nivel final | Qué ilustra |
|---|---|---|---|
| 1001 | Laura Restrepo | PLATINO | Usuaria principal: 3 billeteras, recarga duplicada revertida, canje de millas, transferencia mensual programada |
| 1002 | Andrés Muñoz | ORO | Recarga semanal programada, destino repetitivo, "deshacer la última" (pila), canje |
| 1003 | Camila Ospina | PLATA | Ascenso de nivel; billetera desactivada (Suscripciones); cierra el ciclo del grafo |
| 1004 | Santiago Giraldo | BRONCE | Saldo bajo, retiro rechazado, retiro programado fallido |
| 1005 | Valentina Cardona | ORO | Fragmentación entre billeteras propias y transferencias consecutivas rápidas |
| 1006 | Mateo Quintero | ORO | Monto inusualmente alto y horario no habitual |
| 1007 | Daniela Henao | PLATA | Cambio brusco de frecuencia |
| 1008 | Julián Arango | BRONCE | Sin billeteras ni registros: es el único que se puede eliminar |

## Casos que se pueden ver en la interfaz

Seleccione el usuario en la barra superior y abra la pantalla indicada.

| Requisito | Dónde verlo |
|---|---|
| Varias billeteras por usuario, de tipos distintos | Billeteras (1001: Ahorro, Inversión y Gastos) |
| Recargas, retiros y transferencias | Transacciones (65 en total) |
| Rechazo por billetera inactiva | TRX-000014: transferencia de Laura a BIL-000007, que estaba desactivada |
| Rechazo por saldo insuficiente | TRX-000029: retiro de Santiago |
| Saldo bajo | Santiago queda con menos de 10 000 el 10 de octubre (notificación SALDO_BAJO) |
| Reversión por identificador | TRX-000038 (recarga duplicada de Laura), revertida por TRX-000039 |
| Reversión con la pila ("deshacer la última") | TRX-000047 de Andrés, revertida por TRX-000048 |
| Programada semanal ejecutada varias veces | Programadas de Andrés: recarga de transporte cada martes; la próxima vence el 17 de noviembre |
| Programada mensual | Laura → Camila el día 10 de cada mes; la próxima vence el 10 de diciembre |
| Programada fallida | OPR-000003: retiro de 500 000 de Santiago (TRX-000055 RECHAZADA) |
| Programada cancelada y pendientes futuras | OPR-000008 (cancelada), OPR-000009 y OPR-000010 (pendientes) |
| Puntos y niveles | Puntos y beneficios; usuarios en los cuatro niveles |
| Ascenso de nivel | Notificaciones ASCENSO_NIVEL (por ejemplo, Camila y Daniela a PLATA) |
| Canje de beneficios | Laura (MILLAS) y Andrés (BONO_CAFE) |
| Los 6 patrones de actividad inusual | Auditoría: AUD-000001 a AUD-000006, uno por patrón |
| Notificaciones despachadas y pendientes | Notificaciones: las anteriores al 21 de octubre ya se despacharon (FIFO) |
| Analítica | Analítica: billeteras más usadas, frecuencia por tipo, mayor valor; monto movilizado entre 2026-10-01 y 2026-11-16 |
| Grafo, rutas y ciclos | Analítica: ciclo 1001 → 1002 → 1003 → 1001; ruta de varios saltos 1004 → 1002 → 1003 → 1001; sin ruta desde 1007 (nunca ha transferido) |
| Eliminación permitida y no permitida | Usuarios: 1008 se puede eliminar; 1004 no (tiene registros) |

## Notas al usarla

- **Programadas.** Al arrancar con la fecha real, la tarea automática ejecuta las
  programadas que ya hayan vencido. Por ejemplo, la recarga semanal de Andrés del 17 de
  noviembre se ejecuta sola ese día o en el siguiente arranque.
- **Reversiones.** Las transacciones de la historia tienen más de 24 horas, el plazo
  provisional de reversión. Para probar "Revertir", haga primero una operación nueva.

## Restaurar o regenerar

Detenga la aplicación y reemplace `data/` por una copia de `datos-demo/`. En Windows:

```powershell
Remove-Item -Recurse -Force data
Copy-Item -Recurse datos-demo data
```

Para empezar sin datos, borre `data/`: se crea de nuevo vacía al usar la aplicación.
