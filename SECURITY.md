# SECURITY.md

## Apache Fineract Loan Origination System (LOS) Security Model

> Status: Draft (Initial LOS implementation) — codebase is not release ready.

### Overview

The Loan Origination System (LOS) is a standalone service responsible for loan application intake,
credit scoring, approval workflow, and disbursement orchestration. It integrates with Apache
Fineract through an abstraction layer while remaining independent of the core lending engine.

### Scope

Included:
- Loan Application REST APIs
- Applicant Profiles
- Credit Scoring
- Approval Workflow
- State Machine
- Fineract Integration
- PostgreSQL Persistence

Out of Scope:
- OAuth2/OIDC
- KYC/KYB
- Credit Bureau Integration
- Notification Services


### Protected Assets

- Loan Applications
- Applicant Profiles
- Credit Scores
- Approval Decisions
- Workflow Configuration
- Tenant IDs
- Fineract Loan IDs
- Correlation IDs

### Trust Boundaries

1. External Client → REST API
2. REST API → Service Layer
3. Service Layer → Database
4. Service Layer → Apache Fineract
5. Future External Providers

### Security Principles

- Validate all input.
- Business rules remain in the service layer.
- State changes only through the state machine.
- Tenant-aware repository queries.
- External integrations behind interfaces.
- No hard-coded secrets.

### Threat Model

| Threat | Mitigation |
|---------|------------|
| Invalid request payload | Bean Validation |
| Invalid state transition | State Machine |
| Duplicate approval | DuplicateApprovalException |
| Unknown stage | Workflow validation |
| Disbursement before approval | Service validation |
| Replay disbursement | Reject already disbursed applications |
| Credit score tampering | Server-side scoring |
| Cross-tenant access | Tenant-aware queries |

### Authentication

The current implementation intentionally does not introduce a separate authentication system.
Authentication is expected to be provided by Apache Fineract Back Office.

### Authorization

Future authorization may include:
- Loan Officer
- Branch Manager
- Credit Committee
- Administrator

### API Security

Current:
- Bean Validation
- Global Exception Handling
- Correlation IDs
- Transactional Services

Future:
- OAuth2/JWT
- Rate Limiting
- API Gateway

### Credit Scoring

Scores are computed on the server using configurable scoring factors and cannot be supplied by clients.

### Approval Workflow

Workflow stages are configuration-driven and protected against duplicate approvals and invalid stage progression.

### Fineract Integration

The LOS integrates through Mock and REST adapters to isolate business logic from external communication.

### Database Security

- Flyway migrations
- Transaction boundaries
- Repository abstraction
- Tenant-aware access

### Secure Development

CI checks include:
- Spotless
- Apache RAT
- CodeQL
- GitHub Actions
- Unit Tests

### Secrets

Credentials should be provided through environment variables:
- DB_USERNAME
- DB_PASSWORD
- FINERACT_USERNAME
- FINERACT_PASSWORD

### Future Work

- OAuth2/OIDC
- Identity Verification
- Credit Bureau Integration
- Audit Logging
- Document Management
- Notifications

## Reporting Security Issues

The Apache Software Foundation takes security issues seriously.

If you believe you have identified a security vulnerability in the Apache Fineract Loan Origination System, please report it privately following the Apache Software Foundation Security Team guidelines.

For reporting instructions, see the **[Apache Security Team](https://www.apache.org/security/)**.

Please do not disclose potential vulnerabilities publicly until they have been assessed and addressed by the project maintainers.

