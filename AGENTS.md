<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements. See the NOTICE file
distributed with this work for additional information
regarding copyright ownership. The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied. See the License for the
specific language governing permissions and limitations
under the License.
-->

# AGENTS.md

Context for AI coding agents working in this repository, and for the people who direct them.
It is vendor-neutral: any tool that reads `AGENTS.md` can use it. Using AI tools is optional;
everything here is also true for contributors who do not use them.

This file summarizes and points to the human-maintained references. When it disagrees with the
code, the code wins. When it disagrees with [CONTRIBUTING.md](CONTRIBUTING.md), CONTRIBUTING.md
wins.

## AI-assisted contributions

This repository follows the AI Policy in
[Apache Fineract's CONTRIBUTING.md](https://github.com/apache/fineract/blob/develop/CONTRIBUTING.md)
and the [ASF Generative Tooling Guidance](https://www.apache.org/legal/generative-tooling.html):

- AI tools may assist contribution work. They must not replace contributor accountability.
- The human submitter is responsible for the correctness, safety, performance, and
  maintainability of every submitted change, and must be able to explain any line of it in review.
- Disclose AI tool use with an `Assisted-By: TOOL-MODEL-VERSION` trailer in the commit message.
- Submitted material must be compatible with the Apache License 2.0. If generated output looks
  like it was reproduced from another project, do not submit it until its provenance is known.
- Review replies, design rationale, and risk analysis in PRs and on the mailing list are written
  by the human contributor.

What this means for an agent:

- Do not commit, push, open pull requests, or post comments unless the human asks you to.
  Commits must be signed by the contributor (`main` requires signed commits, see
  [docs/development/gpg-commit-signing.adoc](docs/development/gpg-commit-signing.adoc)). Never
  bypass signing or hooks.
- One logical change per PR. Do not produce sweeping rewrites or PRs touching dozens of unrelated
  files.
- Before adding code, search this repository and the existing dependencies. Prefer a maintained
  library over new hand-written infrastructure.
- An AI review of AI-written code does not replace verification. Run the checks below and leave
  the diff small enough for the human to read in full.
- If a requirement is ambiguous, ask instead of guessing. Design-level changes (new modules,
  frameworks, or workflow redesigns) are discussed on
  [dev@fineract.apache.org](mailto:dev@fineract.apache.org) or in a
  [GitHub issue](https://github.com/apache/fineract-loan-origination/issues) before they are
  implemented. GitHub issues are where this repository tracks its work.

## Project

A standalone Loan Origination System (LOS) for Apache Fineract, started in Google Summer of Code
2026. It is a proof of concept and not production-ready. The LOS owns the pre-disbursement
workflow (application, credit scoring, multi-stage approval) and keeps its own PostgreSQL
database. Fineract stays the system of record for loan accounts and is reached only through its
REST API.

- **Backend:** Java 21, Spring Boot 4.1, **Maven** (not Gradle like Fineract core), PostgreSQL 15,
  Flyway, Spring Security with JWT, Lombok, springdoc-openapi.
- **Frontend (`frontend/`):** Angular 22 with standalone components, TypeScript, Vitest, ESLint,
  and Prettier. It has separate customer and staff portals.

## Start here

| Read                                                                     | For                                                                   |
| ------------------------------------------------------------------------ | --------------------------------------------------------------------- |
| [CONTRIBUTING.md](CONTRIBUTING.md)                                       | Coding conventions (Lombok, logging, errors) and PR and commit rules   |
| [SECURITY.md](SECURITY.md), [docs/security/](docs/security/)             | Threat model, trust boundaries, and the authentication model          |
| [docs/adr/](docs/adr/)                                                   | ADR-001 external service, ADR-002 state machine, ADR-003 scoring       |
| [docs/architecture/](docs/architecture/), [docs/workflows/](docs/workflows/) | Components, integration, lifecycle, approval, and disbursement    |
| [docs/development/](docs/development/)                                   | Setup, configuration, and testing                                     |

## Commands

Run these from the repository root. Use the Maven wrapper (`./mvnw`, or `.\mvnw.cmd` on Windows).

| Task                                                | Command                                                  |
| --------------------------------------------------- | -------------------------------------------------------- |
| Format Java (google-java-format via Spotless)       | `./mvnw spotless:apply`                                  |
| Unit tests                                          | `./mvnw test`                                            |
| One test class                                      | `./mvnw test -Dtest=LoanOriginationStateMachineTest`     |
| Full CI build: Spotless check, unit and Cucumber tests | `./mvnw clean verify` (needs Docker for Testcontainers) |
| License header audit                                | `./mvnw apache-rat:check`                                |
| Local database only                                 | `docker compose -f docker/docker-compose.yml up -d los-db` |
| Run the backend (port 8082)                         | `./mvnw spring-boot:run`                                 |

Frontend commands run from `frontend/`: `npm ci`, `npm start` (port 4200), `npm test`,
`npm run lint`, and `npm run format:check` (or `npm run format` to fix). CI uses Node 22.

Before handing back a change, run the checks for the side you touched:

- Backend: `./mvnw spotless:apply`, `./mvnw clean verify`, and `./mvnw apache-rat:check`
- Frontend: `npm run lint`, `npm run format:check`, and `npm test`

Local setup notes:

- The Compose database listens on host port **5433** as `los_user` / `los_password`, while
  `application.yml` defaults to port 5432. Point the app at it with the `DB_URL`, `DB_USERNAME`,
  and `DB_PASSWORD` environment variables. Do not edit the committed defaults to match your
  machine.
- The `los-app` Compose service needs a `Dockerfile` that does not exist yet, so start `los-db`
  only.
- `los.fineract.mock-enabled` defaults to `true`, so no Fineract instance is needed for local
  work or tests.

## Repository map

```
src/main/java/org/apache/fineract/los/
  api/            REST controllers; response DTOs in api/dto/response
  dto/request/    Request DTOs with Bean Validation
  service/        Business logic and transaction boundaries
  statemachine/   LoanOriginationStateMachine, LoanStateTransitionValidator
  scoring/        CreditScoringStrategy, ScoringFactor, factors/, model/
  workflow/       ApprovalWorkflowProperties (stage order, role mapping)
  bridge/         FineractLoanApiClient and its Rest/Mock implementations, DisbursementBridgeService
  security/       JWT, Fineract-delegated staff login, rate limiting, XSS sanitizing
  crypto/         RSA-AES payload decryption for the login endpoints
  domain/         JPA entities and enums
  repository/     Spring Data repositories
  exception/      Domain exceptions, GlobalExceptionHandler, LosErrorConstants
  config/         Spring configuration
src/main/resources/application.yml         All backend configuration (env var overrides)
src/main/resources/db/migration/           Flyway migrations
src/test/java/org/apache/fineract/los/     Unit tests mirror main packages; cucumber/ holds BDD glue
src/test/resources/features/               Cucumber feature files
frontend/src/app/                          core/ (services, guards, interceptors, models), features/, layout/, shared/
```

## Architecture rules

These rules hold the design together. Change one only after maintainers agree.

1. **Status changes go through the state machine.** Only
   `LoanOriginationStateMachine.transition(...)` may call `LoanApplication.setStatus`. The one
   exception is setting a newly created application to `DRAFT`. Permitted transitions are defined
   only in `LoanStateTransitionValidator`. Add new ones there, with a test in
   `LoanStateTransitionValidatorTest`. `REJECTED` and `DISBURSED` are terminal.
2. **Credit scoring is server-side and pluggable** (ADR-003). Clients never supply a score. A
   new factor implements `ScoringFactor` as a pure, stateless `@Component` whose points never
   exceed `maxPoints()`. Its weight goes in `ScoringWeightsProperties`, including the
   sum-to-100 startup check, and in `application.yml`. `DefaultCreditScoringStrategy` does not
   change when a factor is added.
3. **Fineract is behind `FineractLoanApiClient`** (ADR-001). Services depend on the interface.
   `los.fineract.mock-enabled` selects `MockFineractLoanApiClient` or
   `RestFineractLoanApiClient`. Fineract payloads stay in `bridge/dto` and never reach the LOS
   API. The LOS never changes Fineract core or reads Fineract's database.
4. **Tenant isolation.** Entities carry a `tenantId`, and repository queries filter by it. Every
   new query takes `tenantId`.
5. **The approval workflow comes from configuration.** Stage order and the mapping from Fineract
   roles to stages come from `los.workflow.*`. Do not hard-code stage names in services.
6. **Layering.** Controllers delegate to services and hold no business logic. They return DTOs,
   never entities. Domain entities do not depend on scoring or workflow model classes, and vice
   versa.
7. **Errors.** Throw a specific exception from `exception/`, handle it in
   `GlobalExceptionHandler`, and keep error codes in `LosErrorConstants`. Invalid transitions use
   `LoanStateTransitionException`. No generic `RuntimeException` and no empty catch blocks.

## Conventions

- **Java:** follow "How We Code" in CONTRIBUTING.md. In short: on entities use
  `@Getter`/`@Setter`/`@NoArgsConstructor` (never `@Data`). Use `@RequiredArgsConstructor` for
  injection and `@Slf4j` with placeholder logging (never `System.out` or `printStackTrace()`). Use
  `@Builder` immutables for scoring models. Never use `@SneakyThrows`. Formatting is
  google-java-format; run Spotless instead of formatting by hand.
- **Database:** every schema change is a new Flyway migration, `V<n>__<description>.sql`. Never
  edit a migration that has been merged. Version 8 is already used twice, so use the next unused
  number (currently `V10`). Write the migration even though `ddl-auto` is set to `update`.
- **License headers:** every new file RAT scans (Java, TypeScript, HTML, SCSS, SQL, YAML,
  Markdown, and so on) needs the ASF license header. Copy it from a neighbouring file of the same
  type.
- **Dependencies:** explain every new dependency in the PR. Its license must be ASF Category A
  (CI rejects AGPL, SSPL, and BSL). GitHub Actions must be on the
  [ASF allowlist](https://github.com/apache/infrastructure-actions) and pinned to a full commit
  SHA.
- **Frontend:** use standalone components (no NgModules) and signals for state. Prettier sets
  single quotes and a print width of 100. Customer and staff code stays separate: each has its
  own auth service, guard, and interceptor. The API base URL is set in `src/environments/`. The
  backend enforces authorization; route guards only shape the UI.

## Testing

- **Unit tests** use JUnit 5 and AssertJ. Service tests also use Mockito
  (`@ExtendWith(MockitoExtension.class)`). The state machine and scoring factors are tested
  without a Spring context. Name tests `<ClassName>Test` and put them in the same package as the
  class under test.
- **BDD tests:** Cucumber features in `src/test/resources/features/` with glue in
  `org.apache.fineract.los.cucumber`. Failsafe runs them through `CucumberIT` during `verify`.
  `CucumberSpringConfig` starts the application with a Testcontainers PostgreSQL and the mock
  Fineract client.
- Changed behaviour needs a test, and a bug fix needs a test that fails without the fix.
- Never delete, weaken, or disable a test to make the build pass.

## Security

Read [SECURITY.md](SECURITY.md) before security-sensitive work. Report vulnerabilities privately
through the [ASF security process](https://www.apache.org/security/), never in public issues or
PRs.

- Never commit secrets, real credentials, tokens, or personal data. Configuration comes from
  environment variables (`JWT_SECRET`, `DB_*`, `FINERACT_*`), and the defaults in
  `application.yml` are for local development only.
- Staff passwords are never stored: they are validated against Fineract at login. Customer
  passwords are hashed through Spring's `DelegatingPasswordEncoder` (Argon2 by default).
- Endpoints require authentication unless `SecurityConfig` explicitly permits them. Staff and
  admin operations use `@PreAuthorize`. A new endpoint needs a deliberate access rule and a test
  that an unauthorized caller is rejected.
- Never log credentials, tokens, decrypted payloads, or applicant personal data. Test data must
  be synthetic.

## Where the docs and the code disagree

These gaps were known when this file was written. Trust the code. Fix the doc in the same PR if
your change touches the area.

- The architecture docs name `FineractIntegrationPort`, `RealFineractAdapter`, and
  `MockFineractAdapter`. The code uses `FineractLoanApiClient`, `RestFineractLoanApiClient`, and
  `MockFineractLoanApiClient`.
- `docs/development/testing.adoc` mentions `AbstractIntegrationTest`, a `bdd/` package, and
  JaCoCo. None of these exist.
- The backend runs on port 8082 (`application.yml`); CONTRIBUTING.md says 8080.
- SECURITY.md says the LOS has no authentication of its own. The code has customer and staff
  JWT authentication; see [docs/security/](docs/security/).
- The code allows `REFERRED → SUBMITTED`, which ADR-002 does not list.
- `application.yml` configures two approval stages. `ApprovalWorkflowProperties` and the Cucumber
  tests default to three (`LOAN_OFFICER`, `CREDIT_COMMITTEE`, `BRANCH_MANAGER`).
- CONTRIBUTING.md names controllers `*ApiResource`, but every existing controller is named
  `*Controller`.

## Keeping this file current

When a PR changes a command, a directory, or one of the rules above, update this file in the
same PR. Keep it short. Detailed how-tos belong in `docs/`.
