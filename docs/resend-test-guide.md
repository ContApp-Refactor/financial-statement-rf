# Guia de Prueba con Resend

## 1. Requisitos

Necesitas:

- una cuenta de Resend
- un dominio o remitente verificado en Resend
- una API key valida

Importante:

- `MAIL_FROM` debe ser un remitente valido en Resend
- el correo destino es el que mandes en `toEmail` o `recipientEmail`
- este micro usa el SDK oficial de Resend para Java

## 2. Variables minimas para probar local

PowerShell:

```powershell
$env:PROFILE='dev'
$env:MAIL_PROVIDER='resend'
$env:MAIL_FROM='reportes@tudominio.com'
$env:RESEND_API_KEY='re_xxxxxxxxxxxxxxxxx'
$env:RESEND_REPLY_TO='soporte@tudominio.com'
$env:EUREKA_ENABLED='false'
$env:EUREKA_REGISTER='false'
$env:EUREKA_FETCH='false'
$env:SPRING_CLOUD_DISCOVERY_ENABLED='false'
.\mvnw.cmd spring-boot:run
```

Si quieres probar con `local` tambien funciona, pero `dev` es mas limpio para estas pruebas.

## 3. Generar un reporte persistido

Primero genera el reporte para obtener `reportId`.

Ejemplo:

```http
POST /api/financial-statements/register
Content-Type: application/json
```

```json
{
  "entId": "50a69a75-7134-4189-9c62-fc82c53ff679",
  "type": "STATEMENT_FINANCIAL_POSITION",
  "criteria": {
    "criteriaType": "SUB_ACCOUNT",
    "currentCutoffDate": "2025-03-29",
    "previousCutoffDate": "2024-03-29"
  }
}
```

De la respuesta toma:

- `data.financialStatement.reportId`

## 4. Probar envio inmediato por correo

Luego llama:

```http
POST /api/financial-statements/export/email
Content-Type: application/json
```

```json
{
  "reportId": "REEMPLAZA_AQUI_EL_REPORT_ID",
  "format": "PDF",
  "toEmail": "destinatario@dominio.com"
}
```

## 5. Ejemplo en PowerShell

```powershell
$body = @{
  reportId = 'REEMPLAZA_AQUI_EL_REPORT_ID'
  format   = 'PDF'
  toEmail  = 'destinatario@dominio.com'
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri 'http://localhost:8081/api/financial-statements/export/email' `
  -ContentType 'application/json' `
  -Body $body
```

## 6. Como saber si quedo bien

Exito:

- respuesta `200 OK`
- mensaje: `Financial statement email sent successfully`
- el correo llega al destinatario
- aparece evento de envio en logs/historial del reporte
- el dashboard de Resend muestra el envio

Error tipico:

- `Resend is selected but mail.resend.api-key is empty`
- `mail.from is required for Resend delivery`
- `Resend is selected but the Resend client is not configured`
- error `403` o `422` desde Resend por remitente no verificado

## 7. Probar envio programado

Tambien puedes probar la programacion:

```http
POST /api/financial-statements/email-schedules
Content-Type: application/json
```

```json
{
  "reportId": "REEMPLAZA_AQUI_EL_REPORT_ID",
  "recipientEmail": "destinatario@dominio.com",
  "format": "PDF",
  "frequency": "DAILY",
  "hourOfDay": 23,
  "minuteOfHour": 59,
  "timezone": "America/Bogota"
}
```

Cuando llegue `nextRunAt`, el scheduler intentara enviarlo.

## 8. Recomendacion

Para validar primero:

1. prueba envio inmediato con `POST /export/email`
2. confirma que llega el correo
3. luego prueba programacion

Es la forma mas rapida de aislar si el problema es Resend o el scheduler.
