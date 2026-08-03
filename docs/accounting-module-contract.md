# Contrato de Integracion con el Modulo Contable

Este microservicio no debe consultar la base de datos del modulo contable. La integracion esperada es por API HTTP autenticada.

## Objetivo

Entregar al microservicio `financial-statement` los movimientos contables necesarios para calcular:

- Estado de situacion financiera
- Estado de resultados
- Estado de cambios en el patrimonio

## Endpoint esperado

`GET /api/accounting-entries`

La URL final puede exponerse:

- por DNS interno del micro contable, por ejemplo `http://accounting-module/api/accounting-entries`
- o por el API Gateway, por ejemplo `http://api-gateway/api/accounting-entries`

La ruta concreta puede cambiar, pero el contrato funcional debe mantenerse.

## Headers

- `Authorization: Bearer <jwt>`
- `Accept: application/json`

## Query params

- `entId` obligatorio para produccion
- `startDate` opcional, formato `yyyy-MM-dd`
- `endDate` opcional, formato `yyyy-MM-dd`

Ejemplo:

```http
GET /api/accounting-entries?entId=50a69a75-7134-4189-9c62-fc82c53ff679&startDate=2025-01-01&endDate=2025-03-29
Authorization: Bearer eyJ...
Accept: application/json
```

## Respuesta esperada

```json
[
  {
    "entId": "50a69a75-7134-4189-9c62-fc82c53ff679",
    "date": "2025-03-29",
    "voucher": {
      "number": 1010,
      "type": "NC"
    },
    "account": {
      "code": "110505",
      "nature": "DEBITO",
      "name": "Activo corriente - Caja"
    },
    "thirdPartyId": 1,
    "accountingMovement": {
      "description": "Saldo caja actual",
      "debit": 150000000,
      "credit": 0
    },
    "costCenter": {
      "code": 1,
      "name": "General"
    }
  }
]
```

## Reglas del contrato

- `date` debe llegar en formato ISO `yyyy-MM-dd`
- `account.code` debe llegar y procesarse como `String`, conservando el codigo contable real sin perder ceros significativos
- `account.nature` debe ser `DEBITO` o `CREDITO`
- `accountingMovement.debit` y `accountingMovement.credit` deben llegar siempre, aunque uno sea `0`
- la API debe devolver solo movimientos reales de la empresa consultada
- la API no debe mezclar empresas ni reescribir `entId`

## Compatibilidad local de mocks

El microservicio soporta un modo mock explicito solo para desarrollo local. Ese modo:

- permite reintentar contra `json-server` sin filtros server-side
- permite aceptar fechas legacy `dd-MM-yyyy` del mock local
- no debe activarse en `dev`, `test` ni `prod`

Propiedades:

```env
ACCOUNT_INFO_SERVICE_MOCK_MODE_ENABLED=true
ACCOUNT_INFO_SERVICE_ACCEPT_LEGACY_DATE_FORMAT=true
```

## Codigos de respuesta

- `200 OK`: respuesta valida
- `400 Bad Request`: parametros invalidos
- `401 Unauthorized`: token invalido o ausente
- `403 Forbidden`: el usuario no puede consultar esa empresa
- `404 Not Found`: ruta inexistente
- `500 Internal Server Error`: error interno del modulo contable

## Responsabilidades de financial-statement

El microservicio de estados financieros:

- consulta la API contable
- transforma los movimientos a su modelo interno
- genera el reporte
- almacena historial, configuracion y programaciones de correo
- exporta PDF y Excel

## Responsabilidades del modulo contable

El modulo contable:

- conserva los asientos y movimientos reales
- expone la API anterior
- filtra correctamente por empresa y fechas
- garantiza consistencia de codigos, naturalezas y valores

## Variable de entorno en financial-statement

La integracion se configura con:

```env
ACCOUNT_INFO_SERVICE_URL=http://accounting-module/api/accounting-entries
```

Durante desarrollo local puede apuntar al mock:

```env
ACCOUNT_INFO_SERVICE_URL=http://localhost:4001/accountInfo
```
