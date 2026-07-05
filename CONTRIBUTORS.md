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

# Contributing

Hello there!

First of all, **Thank You** for contributing to the Apache Fineract Loan Origination project! We are grateful for your interest.

This project provides a standalone Loan Origination Service designed to integrate with Apache Fineract. It was initiated during **Google Summer of Code (GSoC) 2026** under **[FINERACT-2442](https://issues.apache.org/jira/browse/FINERACT-2442)**, and is intended to evolve as a community driven Apache project.

Please join the [Developer mailing list](mailto:dev@fineract.apache.org), if you have not already done so say *Hi* there! This project follows [the Apache Software Foundation (ASF) Code of Conduct](https://www.apache.org/foundation/policies/conduct.html). Violations of the code of conduct may be reported directly to the ASF or to the Apache Fineract Project Management Committee at [private@fineract.apache.org](mailto:private@fineract.apache.org).

The [JIRA Dashboard](https://issues.apache.org/jira/browse/FINERACT-2442) shows what's going on for this component. You don't need to be a committer to provide pull requests, but [Becoming a Committer](https://cwiki.apache.org/confluence/display/FINERACT/Becoming+a+Committer) explains the process of becoming one just in case.

---

## Important : This Module Uses Maven, Not Gradle

Apache Fineract core is built with **Gradle**. This module, `fineract-loan-origination`, is a standalone Spring Boot service built with **Maven**. If you're coming from the main Fineract repository, the commands below will look different from what you're used to and that's expected.

---

# Developer How-To's

## Run the Tests

### Unit Tests

Unit tests use **JUnit 5** and **AssertJ** and do not require any external services.

```bash
./mvnw test
```

### Integration Tests

Integration tests use **Testcontainers** to automatically provision a PostgreSQL instance. No manual database setup is required.

```bash
./mvnw verify
```

### Full CI Build

To run the complete build exactly as the CI pipeline does, including license checks and formatting validation:

```bash
./mvnw clean verify
```

---

## Run the Service Locally

Start PostgreSQL using Docker Compose:

```bash
docker compose -f docker/docker-compose.yml up -d
```

Then start the Spring Boot application:

```bash
./mvnw spring-boot:run
```

The application starts on **http://localhost:8080** and connects to the PostgreSQL instance started by Docker Compose.

---

## Run and Debug in IntelliJ IDEA

1. Open **IntelliJ IDEA**.
2. Select **Open** and choose the project's root directory (`fineract-loan-origination`).
3. IntelliJ will automatically detect the `pom.xml` and import the project as a Maven project.
4. Ensure the project SDK is set to **Java 21**.
5. Run or debug `LoanOriginationApplication` as a Java Application.

---

## Run and Debug in Eclipse IDE

1. Select **File → Import → Maven → Existing Maven Projects**.
2. Choose the project root directory.
3. Eclipse will automatically resolve dependencies from `pom.xml`.
4. Run or debug `LoanOriginationApplication` as a Java Application.

---

## Run Apache RAT (Release Audit Tool)

Execute:

```bash
./mvnw apache-rat:check
```

The generated report is available at:

```
target/rat.txt
```

All source files must include the Apache License header. Files excluded from the audit are configured in `.rat-excludes`.


---

## How We Code

### Spotless

This project enforces code formatting using [Spotless](https://github.com/diffplug/spotless), configured to run automatically during the Maven build and fail on violations. To automatically fix formatting violations:

```bash
./mvnw spotless:apply
```

To check without applying fixes (already included in a regular build):

```bash
./mvnw spotless:check
```

### Code Coverage

Changed or added code should have test coverage. Unit tests for business logic (state machine, scoring factors) should not require Spring context and should run in milliseconds. Integration tests that need a database use Testcontainers.

### Lombok

This project uses [Lombok](https://projectlombok.org/) to reduce boilerplate:

- Use `@Getter` / `@Setter` / `@NoArgsConstructor` on JPA entities. Never use `@Data` on entities — it causes JPA lazy loading issues.
- Use `@RequiredArgsConstructor` on service classes and Spring components for constructor-based dependency injection.
- Use `@Slf4j` for logging instead of manually declaring loggers.
- Use `@Builder` for immutable model classes (scoring inputs/outputs) — never expose public setters on these.
- Never use `@SneakyThrows` — handle exceptions explicitly.

### Error Handling

- When catching exceptions, either rethrow with context or log them — always include the root cause.
- Empty catch blocks are not acceptable. If an exception is genuinely expected and not an error, use an API that returns `Optional` or a result object instead of throwing.
- State machine transition failures use the dedicated `LoanStateTransitionException` — never generic `RuntimeException`.

### Logging

- We use [SLF4J](http://www.slf4j.org) via Lombok's `@Slf4j`.
- Never use `System.out`, `System.err`, or `printStackTrace()`.
- Use placeholder logging (`log.error("...", value, exception)`), never string concatenation.
- `log.info()` is used for state transitions and startup validation (e.g. scoring weight validation).
- `log.warn()` is used for rejected transitions — invalid but not unexpected.
- `log.debug()` is used for detailed troubleshooting, not enabled by default.

---

## Change Process

### Dependency Upgrades

This project uses [Dependabot](https://github.com/dependabot) to automatically raise pull requests when new dependency versions are available. Major version bumps (e.g. a framework's X.0 → Y.0) should be reviewed carefully against release notes before merging — minor and patch bumps are generally safe to merge once CI passes.

### Pull Requests

We request that your commit message include the FINERACT JIRA issue followed by a concise description of the change, starting with an imperative verb.

**Example:** **[FINERACT-2442: Add modular credit scoring factor implementations](https://github.com/apache/fineract-loan-origination/pull/29)**

Keep pull requests small and focused, one logical change per pull request. This makes reviews faster and rollbacks safer if a change needs to be reverted.

Before opening a pull request, ensure the following all pass locally:

```bash
./mvnw spotless:apply
./mvnw apache-rat:check
./mvnw clean verify
```

If CI fails on your PR:

1. Check whether the failure is caused by your change or is an unrelated flaky test/check.
2. If it's a genuine issue with your code, fix it and push — don't wait for a maintainer to point it out.
3. If you believe it's unrelated to your change, mention this clearly in the PR description.

### Coding Conventions Specific to This Module

- Package root: `org.apache.fineract.los`
- REST controllers follow the `ApiResource` naming convention (e.g. `LoanApplicationApiResource`), consistent with Fineract core
- Pluggable components (credit scoring, Fineract integration adapters, workflow stages) are defined as interfaces — new implementations should extend the abstraction rather than modify existing ones
- Domain entities never depend on scoring or workflow model classes, and vice versa — keep persistence concerns separate from business logic models

---

## Getting Help

- Apache Fineract Developer Mailing List: <dev@fineract.apache.org>
- Matrix: <https://matrix.to/#/#apache-fineract-gsoc:matrix.org>
- JIRA: <https://issues.apache.org/jira/browse/FINERACT-2442>

---

## Acknowledgements

This project was initiated during **Google Summer of Code 2026** under **[FINERACT-2442](https://issues.apache.org/jira/browse/FINERACT-2442)**.

Special thanks to everyone who has helped guide the project:

- **[James Dailey](https://github.com/jdailey)** — Primary Mentor
- **[Tofunmi Oguntibeju](https://github.com/paultofunmi)** — Co-mentor
- **[Aman Mittal](https://github.com/Aman-Mittal)** — Reviewer
- **[Attila Budai](https://github.com/budaidev)** — Reviewer

Initial implementation by **[Sujan Kumar MV (KRYSTALM7)](https://github.com/KRYSTALM7)** as part of Google Summer of Code 2026.