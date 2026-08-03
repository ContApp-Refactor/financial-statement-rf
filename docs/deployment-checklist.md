# Checklist de Despliegue

## Git

- crear rama `develop`
- trabajar en ramas `feature/*`
- hacer merge a `develop`
- no subir secretos al repositorio

## Variables de entorno minimas

```env
PROFILE=prod
PORT=8081

DB_URL=jdbc:postgresql://<host>:5432/financial_statement
DB_USER=<user>
DB_PASSWORD=<password>
DB_HIBERNATE_DDL_AUTO=validate
DB_FLYWAY_ENABLED=true
DB_FLYWAY_BASELINE_ON_MIGRATE=true

EUREKA_ENABLED=true
EUREKA_REGISTER=true
EUREKA_FETCH=true
EUREKA_URL=http://<eureka-host>:8761/eureka/

JWT_ISSUER_URI=http://<keycloak-host>/auth/realms/<realm>
JWT_JWK_SET_URI=http://<keycloak-host>/auth/realms/<realm>/protocol/openid-connect/certs
JWT_RESOURCE_ID=microservices_client
SECURITY_AUTH_ENABLED=true

CORS_ALLOWED_ORIGINS=https://<frontend-host>

ACCOUNT_INFO_SERVICE_URL=http://<accounting-module-host>/api/accounting-entries

MAIL_PROVIDER=resend
MAIL_FROM=reportes@<dominio-verificado>
RESEND_API_KEY=<api-key>
RESEND_REPLY_TO=soporte@<dominio-verificado>

REPORT_EMAIL_SCHEDULER_DELAY_MS=60000
REPORT_EMAIL_SCHEDULER_DEFAULT_TIMEZONE=America/Bogota
REPORT_EMAIL_SCHEDULER_CLAIM_LEASE_MS=300000
REPORT_EMAIL_SCHEDULER_FAILURE_RETRY_DELAY_MS=300000
```

## Gateway

El frontend no deberia consumir este micro directamente en produccion. Debe entrar por el API Gateway.

Ruta sugerida en el gateway:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: financial-statement
          uri: lb://financial-statement
          predicates:
            - Path=/api/financial-statements/**
```

## Observabilidad

Este micro ya queda preparado para:

- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/prometheus`

Se agrego soporte para Micrometer con Prometheus.

## Migraciones

La estrategia recomendada para despliegue es:

- Flyway habilitado en `dev/staging/prod`
- Hibernate en `validate`, no en `update`
- migracion base en `src/main/resources/db/migration/V1__initial_schema.sql`
- `baseline-on-migrate=true` mientras existan bases inicializadas antes de Flyway

## Correo recomendado

Proveedor recomendado para despliegue: `Resend`

Motivos:

- menos friccion que SMTP tradicional
- mejor trazabilidad de errores
- mejor adaptacion a microservicios y pipelines
- no depende de credenciales de correo personal
- integra mejor con este micro usando el SDK oficial de Java

El destinatario del reporte no esta quemado en codigo. Se usa:

- `toEmail` para envio inmediato
- `recipientEmail` para envios programados

## Scheduler

El envio programado usa un claim atomico sobre `nextRunAt` para reducir duplicados cuando hay varias replicas.

Variables relevantes:

- `REPORT_EMAIL_SCHEDULER_CLAIM_LEASE_MS`: ventana temporal durante la cual una replica reclama un envio antes de procesarlo
- `REPORT_EMAIL_SCHEDULER_FAILURE_RETRY_DELAY_MS`: tiempo de reintento si el envio falla

## PDF y Excel

No es obligatorio usar JasperReports para desplegar correctamente este micro.

La implementacion actual con:

- Apache PDFBox para PDF
- Apache POI para Excel

es valida para despliegue y suficiente si:

- el formato del reporte ya esta controlado por codigo
- no necesitas un disenador visual de plantillas
- no dependes de reportes parametrizados por negocio en tiempo de ejecucion

JasperReports solo seria recomendable si el equipo va a estandarizar todos los reportes visuales en una misma herramienta o si necesitan editar plantillas fuera del codigo.

## Entregables para el equipo

- contrato del modulo contable: [docs/accounting-module-contract.md](./accounting-module-contract.md)
- README actualizado del microservicio
- configuracion de observabilidad y correo por variables de entorno
