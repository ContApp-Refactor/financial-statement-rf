# financial-statement

Microservice for generating financial statement reports.

## Architecture Target

Production flow:

- `Frontend -> API Gateway -> financial-statement`
- `financial-statement -> accounting module API`
- `financial-statement -> own database`
- `financial-statement -> email provider`

This microservice should not read the accounting database directly. It must consume accounting movements through an HTTP API exposed by the accounting module.

Supporting docs:

- accounting contract: [docs/accounting-module-contract.md](docs/accounting-module-contract.md)
- deployment checklist: [docs/deployment-checklist.md](docs/deployment-checklist.md)
- persistence model: [docs/persistence-model.md](docs/persistence-model.md)
- Resend test guide: [docs/resend-test-guide.md](docs/resend-test-guide.md)

## Requirements
- Java 17
- Maven Wrapper (included)
- PostgreSQL (for `dev` profile)
- SMTP or Resend account (for email export)

## Run With VSCode Play (No PowerShell Vars)
1. Create DB: `financial_statement` in PostgreSQL.
2. Copy `src/main/resources/application-local-private.example.yml` to `src/main/resources/application-local-private.yml`.
3. Set your local secrets in `application-local-private.yml` (DB password and email provider credentials).
4. Click Play on `FinancialStatementsApplication`.

`application-local-private.yml` is ignored by git, so your credentials are not committed.

## Environment variables (`dev`)
- `PROFILE=dev`
- `PORT=8081` (or any port)
- `DB_URL=jdbc:postgresql://localhost:5432/financial_statement`
- `DB_USER=postgres`
- `DB_PASSWORD=root`
- `DB_HIBERNATE_DDL_AUTO=update`
- `EUREKA_ENABLED=false`
- `EUREKA_REGISTER=false`
- `EUREKA_FETCH=false`
- `SPRING_CLOUD_DISCOVERY_ENABLED=false`
- `CORS_ALLOWED_ORIGINS=http://localhost:4200,http://localhost:4300`
- `MAIL_HOST=smtp.gmail.com`
- `MAIL_PORT=587`
- `MAIL_USERNAME=<your_email>`
- `MAIL_PASSWORD=<app_password>`
- `MAIL_FROM=<your_email>`
- `MAIL_PROVIDER=smtp` (`smtp` or `resend`)
- `RESEND_API_KEY=<your_resend_api_key>` (only if `MAIL_PROVIDER=resend`)
- `RESEND_REPLY_TO=<optional_reply_to_email>`
- `REPORT_EMAIL_SCHEDULER_DELAY_MS=60000`
- `REPORT_EMAIL_SCHEDULER_DEFAULT_TIMEZONE=America/Bogota`

## Run
```bash
./mvnw spring-boot:run
```

PowerShell example:
```powershell
$env:PROFILE='dev'
$env:PORT='8081'
$env:EUREKA_ENABLED='false'
$env:EUREKA_REGISTER='false'
$env:EUREKA_FETCH='false'
$env:SPRING_CLOUD_DISCOVERY_ENABLED='false'
.\mvnw.cmd spring-boot:run
```

## Production Profile (`prod`)
Use `PROFILE=prod` and configure values with environment variables (no secrets in repository):

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `EUREKA_URL`
- `EUREKA_ENABLED=true`
- `EUREKA_REGISTER=true`
- `EUREKA_FETCH=true`
- `JWT_ISSUER_URI`
- `JWT_JWK_SET_URI`
- `JWT_RESOURCE_ID`
- `ACCOUNT_INFO_SERVICE_URL` (real accounting endpoint, for example through Gateway: `http://<gateway-host>:8080/api/accountCatalogue/reporting/account-info`)
- `MAIL_PROVIDER=resend`
- `MAIL_FROM` (must be a verified sender/domain in Resend)
- `RESEND_API_KEY`
- `RESEND_REPLY_TO` (optional)
- `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` (only if you use `MAIL_PROVIDER=smtp`)
- `CORS_ALLOWED_ORIGINS`
- `SECURITY_AUTH_ENABLED=true`
- `management.endpoints.web.exposure.include=health,info,prometheus` (already configured in YAML)

PowerShell example:
```powershell
$env:PROFILE='prod'
$env:PORT='8081'
$env:DB_URL='jdbc:postgresql://localhost:5432/financial_statement'
$env:DB_USER='postgres'
$env:DB_PASSWORD='your_db_password'
$env:EUREKA_URL='http://localhost:8761/eureka/'
$env:JWT_ISSUER_URI='http://localhost:8090/auth/realms/oauth2-realm'
$env:JWT_JWK_SET_URI='http://localhost:8090/auth/realms/oauth2-realm/protocol/openid-connect/certs'
$env:ACCOUNT_INFO_SERVICE_URL='http://localhost:8080/api/accountCatalogue/reporting/account-info'
$env:MAIL_PROVIDER='resend'
$env:MAIL_FROM='noreply@yourdomain.com'
$env:RESEND_API_KEY='re_xxxxxxxxxxxxxxxxx'
$env:RESEND_REPLY_TO='support@yourdomain.com'
$env:CORS_ALLOWED_ORIGINS='http://localhost:4200'
.\mvnw.cmd spring-boot:run
```

## Frontend Integration (Dynamic Destination Email)
For sending to the email typed by the user in ContApp, call:

- `POST /api/financial-statements/export/email`

Request body:
```json
{
  "reportId": "61e1bbcc-cc20-4054-ab3e-c5d4bad67d64",
  "format": "PDF",
  "toEmail": "usuario@dominio.com"
}
```

Notes:
- The backend already uses the incoming `toEmail` value.
- Through Gateway, call this same path from the frontend base URL configured for API.
- In production, keep `SECURITY_AUTH_ENABLED=true`.
- For real accounting data, configure `ACCOUNT_INFO_SERVICE_URL` to the `account-catalogue` reporting endpoint and keep JWT auth enabled so the user token can be propagated.

## Email Provider Recommendation

Recommended provider for deployment: `Resend`

Why:

- API-first integration
- easier secret management than personal SMTP accounts
- better fit for microservices and CI/CD
- supports sending attachments cleanly
- this microservice now uses the official Resend Java SDK

The destination email is not hardcoded in code. The backend uses:

- `toEmail` for immediate export by email
- `recipientEmail` for scheduled report delivery

The sender address is configured with `MAIL_FROM`.

## PDF and Excel Strategy

JasperReports is not required for a good deployment of this microservice.

The current implementation uses:

- PDF: Apache PDFBox
- Excel: Apache POI

That is a valid production approach when the report layout is controlled in code.

Use JasperReports only if the team needs:

- visual report designers
- shared template editing outside code
- strong standardization around `.jrxml` templates

For the current scope, PDFBox + POI is enough.

## Swagger
- `http://localhost:8081/swagger-ui/index.html`

## Actuator and Metrics

- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/prometheus`

## Main Endpoints
- `POST /api/financial-statements/register`
- `GET /api/financial-statements/history?enterpriseId={id}&page=0&size=10&sort=createdAt,desc`
- `GET /api/financial-statements/logs?reportId={reportId}`
- `GET /api/financial-statements/{reportId}`
- `POST /api/financial-statements/export`
- `POST /api/financial-statements/export/email`
- `POST /api/financial-statements/email-schedules`
- `GET /api/financial-statements/email-schedules?reportId={reportId}`
- `PATCH /api/financial-statements/email-schedules/{scheduleId}/status`
- `POST /api/financial-statements/templates/default`
- `GET /api/financial-statements/templates/default?enterpriseId={id}`

## Persistence
- `FINANCIAL_STATEMENT` (solo datos reales del reporte generado: tipo, entidad y snapshot)
- `FINANCIAL_STATEMENT_HISTORY`
- `FINANCIAL_STATEMENT_LOG`
- `FINANCIAL_STATEMENT_TEMPLATE`
- `FINANCIAL_STATEMENT_EMAIL_SCHEDULE`
