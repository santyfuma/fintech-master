# Fintech: billeteras digitales (proyecto final de Estructuras de Datos)

Simulación académica de una plataforma de billeteras digitales, con Java 21, Spring
Boot 4.1.2 y Maven. Persiste en archivos JSON y tiene interfaz web propia. 

## Requisitos
- JDK 21.
- Acceso a Maven Central la primera vez, para descargar dependencias.

## Ejecutar
```bash
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```
Abra http://localhost:8080/. Los datos se guardan en `data/`, que viene precargada con
**datos de demostración ficticios** (8 usuarios, 12 billeteras, 65 transacciones y los
casos de cada requisito). La guía está en `docs/DATOS_DEMO.md`, que también explica cómo
restaurarlos desde `datos-demo/`.
Para otra carpeta:
`./mvnw spring-boot:run -Dspring-boot.run.arguments=--fintech.datos.directorio=otra`.

## Pruebas
```bash
./mvnw clean test               # Windows: mvnw.cmd clean test
```

## Documentación (`docs/`)
| Archivo | Contenido |
|---|---|
| `INFORME_TECNICO.md` | Informe final: arquitectura, estructuras y su justificación, reglas, pruebas, benchmark, limitaciones y tabla de decisiones |
| `API.md` | Contrato REST |
| `ejemplos-api.md` | 14 casos de ejecución con JSON |
| `pruebas.http` | Secuencia ejecutable |
| `diagrama-clases.md` | Diagrama de clases (Mermaid) |
| `CONTEXTO.md` | Decisiones de diseño por fase |
| `DATOS_DEMO.md` | Guía de los datos de demostración |

## Configuración
Las reglas que el enunciado no fija son políticas provisionales en
`src/main/resources/application.properties` (prefijo `fintech.`):
- Puntos.
- Reversión.
- Umbrales de detección.
- Alertas.
- Programadas.
- Catálogo de beneficios y ventajas por nivel. Traen valores de demostración, porque el
  enunciado no los define.
