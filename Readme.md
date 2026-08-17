<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Apache Fineract Loan Origination System

A standalone loan origination service for Apache Fineract. Manages the complete pre-disbursement workflow from customer application through credit assessment, multi-stage approval, and loan creation in Fineract.

The project was started in May 2026 as part of the Google Summer of Code (GSoC) program under [FINERACT-2442](https://issues.apache.org/jira/browse/FINERACT-2442). It has two components:

* **Backend:** A Spring Boot service providing REST APIs, state machine workflow, credit scoring, and Fineract integration
* **Frontend:** An Angular client with role-based customer and staff portals

The LOS is the policy enforcement point for loan applications. It keeps its own state (applications, profiles, approvals, scores) in PostgreSQL; Fineract remains the system of record for loan accounts after disbursement.

## Requirements

* Java >= 21
* Node.js >= 20 and npm >= 10 (for the frontend)
* PostgreSQL >= 15 (the LOS keeps its own database, separate from Fineract's)
* Docker and Docker Compose
* Apache Fineract (a running Fineract instance is required for production integration)

The bundled Docker Compose stack provides PostgreSQL for local development. For Fineract integration, see [Apache Fineract](https://github.com/apache/fineract).

## Security

The LOS enforces separate customer and staff authentication with JWT tokens, role-based access control mapped to workflow stages, and state machine validation. Credit scoring is server-side only.

See [docs/security/](docs/security/) for the complete security model, authentication, authorization, and threat analysis.

## Project Layout

The LOS is organized by feature and bounded context:

```
fineract-loan-origination/
├── src/main/java/org/apache/fineract/los/
│   ├── api/              REST controllers
│   ├── bridge/           Fineract integration (mock and real adapters)
│   ├── domain/           JPA entities
│   ├── scoring/          Credit scoring engine
│   ├── security/         Authentication and authorization
│   ├── service/          Business logic
│   ├── statemachine/     Application state machine
│   └── workflow/         Multi-stage approval routing
├── frontend/             Angular client
│   └── src/app/
│       ├── features/     Customer and staff modules
│       ├── core/         Services and guards
│       └── shared/       Reusable components
├── docs/                 Documentation
└── docker/               Docker Compose stack
```

See [docs/architecture/](docs/architecture/) for detailed component descriptions and integration patterns.

## Instructions

### Backend

Start PostgreSQL:

```bash
docker compose -f docker/docker-compose.yml up -d
```

Start the Spring Boot application:

```bash
./mvnw spring-boot:run
```

On Windows:

```cmd
.\mvnw.cmd spring-boot:run
```

The backend starts on **http://localhost:8082**. Verify:

```bash
curl http://localhost:8082/actuator/health
```

### Frontend

```bash
cd frontend
npm install
npm start
```

The frontend runs on **http://localhost:4200** and proxies API requests to port 8082.

### Fineract Integration

The LOS integrates with Fineract via REST APIs. Configure in `src/main/resources/application.yml`:

```yaml
los:
  fineract:
    base-url: ${FINERACT_BASE_URL:https://localhost:8443}
    mock-enabled: false
```

Set `mock-enabled: true` for offline development. See [docs/architecture/integration.adoc](docs/architecture/integration.adoc) for details.

For Fineract setup, follow the [Apache Fineract Quick Start](https://github.com/apache/fineract#quick-start).

## Configuration

Key configuration is in `src/main/resources/application.yml`:

* `spring.datasource.url` — LOS PostgreSQL connection
* `los.fineract.base-url` — Fineract API endpoint
* `los.jwt.secret` — JWT signing key (override via `JWT_SECRET` environment variable)
* `los.scoring.weights.*` — Credit scoring factor weights
* `los.workflow.stages` — Approval routing stages

Use environment variables to override defaults. Never commit secrets to version control.

See [docs/development/configuration.adoc](docs/development/configuration.adoc) for complete reference.

## Testing

Backend unit tests:

```bash
./mvnw test
```

Backend integration tests (uses Testcontainers for PostgreSQL):

```bash
./mvnw verify
```

Frontend tests:

```bash
cd frontend
npm test
```

See [docs/development/testing.adoc](docs/development/testing.adoc) for testing strategy and patterns.

## API

The backend exposes a REST API under `/api`. Interactive Swagger documentation is available at `http://localhost:8082/swagger-ui/index.html` when running.

Key endpoints include customer registration and authentication, staff authentication, application submission and review, approval workflow, and disbursement. All authenticated endpoints require `Authorization: Bearer <JWT_TOKEN>` header.

See [docs/api/](docs/api/) for complete REST API reference.

## Workflows

The LOS implements a six-state application lifecycle: `DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED / REJECTED / REFERRED → DISBURSED`. Invalid transitions are rejected by the state machine. Configurable multi-stage approval routing validates progression through workflow stages before disbursement.

See [docs/workflows/](docs/workflows/) for detailed documentation on loan origination lifecycle, approval workflow, credit scoring, and disbursement.

## Documentation

* **[Architecture](docs/architecture/)** — System design, backend, frontend, and Fineract integration
* **[API](docs/api/)** — REST API reference
* **[Security](docs/security/)** — Authentication, authorization, and security architecture
* **[Development](docs/development/)** — Setup, configuration, testing, and contribution workflow
* **[Workflows](docs/workflows/)** — Loan lifecycle, approval, scoring, and disbursement
* **[ADRs](docs/adr/)** — Architecture decision records

See [docs/index.adoc](docs/index.adoc) for the complete documentation index.

## Community

This project is part of the Apache Fineract community. If you are interested in contributing, please read [CONTRIBUTING.md](CONTRIBUTING.md) and join the [developer mailing list](mailto:dev@fineract.apache.org) or the [Fineract Matrix channel](https://matrix.to/#/#apache-fineract-gsoc:matrix.org).

Issues are tracked on [Apache JIRA (FINERACT-2442)](https://issues.apache.org/jira/browse/FINERACT-2442).

This project follows the [Apache Software Foundation Code of Conduct](https://www.apache.org/foundation/policies/conduct.html).

## License

Licensed under the Apache License, Version 2.0. See [LICENSE.md](LICENSE.md) for details.


