# Hexagonal Organization Guide

## Objective

Keep the microservice focused on generating, persisting, exporting and delivering financial statements, while isolating:

- accounting integration
- report calculation
- export/document rendering
- persistence
- transport concerns

## Recommended Package Shape

```text
com.unicauca.edu.co.financial_statements
├── domain
│   ├── model
│   │   ├── statement
│   │   ├── template
│   │   ├── schedule
│   │   └── external
│   ├── service
│   │   ├── calculation
│   │   ├── classification
│   │   └── validation
│   └── enum
├── application
│   ├── port
│   │   ├── in
│   │   └── out
│   └── usecase
│       └── financialstatement
├── infrastructure
│   ├── in
│   │   └── rest
│   └── out
│       ├── persistence
│       ├── clients
│       │   └── accountinginfo
│       ├── export
│       │   └── jasper
│       ├── mail
│       └── security
└── shared
```

## Incremental Rules

1. `application.useCase` should orchestrate, not calculate.
2. Accounting rules belong in dedicated calculators/classifiers, not in controllers or exporters.
3. `infrastructure.out.clients.accountinginfo` should keep external DTOs and HTTP concerns isolated from domain models.
4. `infrastructure.out.export` should expose only generic export contracts.
5. Jasper-specific classes belong under `infrastructure.out.export.jasper`.
6. Persistence entities stay in `infrastructure.out.persistence.entity`; snapshot mapping stays outside entities.

## Current Direction Implemented

- Generic export orchestration remains in `infrastructure.out.export`.
- Jasper runtime is isolated in `infrastructure.out.export.jasper`.
- The base report layout is externalized in `src/main/resources/jasper`.
- Financial statement calculations were already extracted from the main use case into dedicated builders/calculators.

## Next Safe Moves

1. Rename `domain.models` to `domain.model` and `application.useCase` to `application.usecase` only if you can do it in one controlled refactor.
2. Group statement-specific builders under `application.useCase.financialStatement.builder`.
3. Group calculation services under `application.useCase.financialStatement.calculator`.
4. Move snapshot mappers under `application.useCase.financialStatement.snapshot`.
5. Move accounting integration mapping under `infrastructure.out.clients.accountinginfo.mapper`.

## What Not To Do

- Do not move everything at once.
- Do not couple Jasper templates to domain calculations.
- Do not let persistence entities become the domain model.
- Do not hardcode accounting balances in templates or renderers.
