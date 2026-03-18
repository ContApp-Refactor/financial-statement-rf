# Modelo de Persistencia

Este microservicio no persiste registros contables. Solo persiste lo necesario para operar el modulo de reportes.

## FINANCIAL_STATEMENT

Uso: guardar el reporte generado y su snapshot.

Columnas actuales:

- `id`: PK interna
- `reportId`: identificador publico del reporte
- `type`: tipo de reporte
- `entId`: empresa del reporte
- `createdAt`: fecha de generacion
- `reportSnapshot`: snapshot JSON del reporte generado

Convencion actual del snapshot:

- incluye `version`
- version actual: `1`
- snapshots historicos sin `version` se siguen leyendo por compatibilidad

Por que asi:

- las filas del reporte y los criterios quedan dentro de `reportSnapshot`
- evita duplicar fechas y criterios en columnas separadas
- permite reconstruir preview, descarga, exportacion y envio por correo desde el mismo snapshot
- deja base lista para evolucionar el formato del snapshot sin romper historicos

Estrategia de migracion:

- el esquema base ahora queda descrito en `src/main/resources/db/migration/V1__initial_schema.sql`
- `prod` y `dev` quedan preparados para usar Flyway
- `local` y `test` siguen con `ddl-auto=create-drop` para no romper el flujo de desarrollo rapido
- para bases ya existentes, la recomendacion inicial es `baseline-on-migrate=true` mientras se estabiliza la adopcion

No guarda:

- movimientos contables
- terceros
- plan de cuentas
- comprobantes

Eso pertenece al modulo contable.

## FINANCIAL_STATEMENT_HISTORY

Uso: historial funcional de entregas del reporte.

Columnas:

- `id`
- `state`
- `deliveryWay`
- `createdAt`
- `financial_statement_id`

Se usa para el endpoint de historial y para mostrar si el reporte fue generado, descargado o enviado.

## FINANCIAL_STATEMENT_LOG

Uso: bitacora funcional del reporte.

Columnas:

- `id`
- `eventType`
- `message`
- `icon`
- `color`
- `createdAt`
- `financial_statement_id`

Se usa para la trazabilidad visible en frontend.

## FINANCIAL_STATEMENT_TEMPLATE

Uso: configuracion visual por empresa para exportar PDF y Excel.

Columnas:

- `id`
- `entId`
- `name`
- `pathLogotype`
- `alignment`
- `font`
- `fontSize`
- `mainColor`
- `isDefault`
- `createdAt`

Se usa para personalizar exportes. No es informacion contable.

## FINANCIAL_STATEMENT_EMAIL_SCHEDULE

Uso: programacion de envios automaticos por correo.

Columnas:

- `id`
- `financial_statement_id`
- `recipientEmail`
- `format`
- `frequency`
- `hourOfDay`
- `minuteOfHour`
- `dayOfWeek`
- `dayOfMonth`
- `timezone`
- `active`
- `nextRunAt`
- `lastRunAt`
- `createdAt`
- `updatedAt`

Se usa para los envios programados. El destinatario sale de esta tabla, no esta quemado en codigo.

## Que no deberia persistir este micro

No deberia guardar:

- asientos contables
- catalogo de cuentas oficial
- saldos auxiliares
- movimientos por tercero
- datos maestros del modulo contable

Ese contrato debe venir del modulo contable por API.
