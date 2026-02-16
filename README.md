# OpenWLF - Open Watchlist Filtering System

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Enterprise-grade AML(Anti-Money Laundering) watchlist filtering system for financial compliance. Screens customers against OFAC, UN, EU, and other regulatory watchlists in real-time with full case management and audit trail.

## Project Overview

OpenWLF is a portfolio project demonstrating enterprise-level architecture for financial compliance systems. It covers the complete AML workflow: customer screening, alert generation, case investigation, and final decision-making.

### Key Features

- **Real-time Watchlist Filtering**: RESTful API and Thymeleaf UI for instant customer screening
- **Multi-source Sanctions Sync**: Automated batch sync from OFAC SDN, UN Security Council, EU Financial Sanctions
- **Intelligent Matching** (9 algorithms):
  - Exact name / Fuzzy (Levenshtein) / Alias matching
  - Soundex, Double Metaphone (phonetic)
  - Jaro-Winkler (typo-tolerant)
  - N-Gram (substring)
  - Korean Name Matching (Chosung/Jamo)
  - Composite (weighted combination)
- **Spring Security & JWT Authentication**: Dual filter chain (JWT for API, Session for web), role-based access control (ADMIN, MANAGER, ANALYST, VIEWER)
- **Alert Management**: Auto-generated alerts with status lifecycle, assignment, statistics, and Thymeleaf UI (list/detail)
- **Case Management**: Full case lifecycle - creation from alerts, investigation, decision, closure with Thymeleaf UI
- **Member Registration Screening**: UI workflow for member onboarding with watchlist check
- **Risk Scoring**: Weighted scoring system with configurable thresholds
- **Audit Logging**: 10 specialized audit log types for regulatory compliance
- **OpenAPI Documentation**: Interactive Swagger UI

## Architecture

```
openWLF/
├── config-module/        # YAML-based rule configuration, matching weights
├── core-module/          # Business logic (Normalization, RuleEngine, Scoring, Matching)
├── data-module/          # Data layer (20+ Entities, Repositories, Services)
├── batch-module/         # Batch processing (Sanctions sync scheduler, XML parsers)
└── api-module/           # REST APIs, Security, Page Controllers, Thymeleaf UI
```

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                       External Systems                          │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐                  │
│  │   OFAC   │    │    UN    │    │    EU    │  (Sanctions)      │
│  └────┬─────┘    └────┬─────┘    └────┬─────┘                  │
└───────┼───────────────┼───────────────┼─────────────────────────┘
        │               │               │
        ▼               ▼               ▼
┌─────────────────────────────────────────────────────────────────┐
│  batch-module                                                   │
│  SanctionsSyncScheduler → DownloadService → XmlParser → DB     │
└─────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────┐
│  data-module                                                    │
│  Entities │ Repositories │ AlertService │ CaseService           │
└─────────────────────────────────────────────────────────────────┘
        ▲
        │
┌─────────────────────────────────────────────────────────────────┐
│  core-module                                                    │
│  FilteringService → RuleEngine → ScoringService                 │
│  AdvancedMatchingService (9 algorithms)                         │
└─────────────────────────────────────────────────────────────────┘
        ▲
        │
┌─────────────────────────────────────────────────────────────────┐
│  api-module                                                     │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Spring Security (JWT + Session dual filter chain)       │   │
│  ├──────────────────────────────────────────────────────────┤   │
│  │  REST Controllers │ Page Controllers │ Thymeleaf UI      │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
        ▲
        │
  Client / Browser / Swagger UI
```

### Technology Stack

| Category | Technology |
|----------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 4.0.1 |
| Security | Spring Security, JWT (jjwt 0.12.6) |
| Build Tool | Gradle 9.2 |
| Database | H2 (Dev) / PostgreSQL, MySQL (Prod-ready) |
| ORM | JPA / Hibernate |
| Template Engine | Thymeleaf |
| API Docs | SpringDoc OpenAPI 3.0 |
| Testing | JUnit 5, MockMvc, Mockito |
| Utilities | Apache Commons Codec, Commons Text, Commons Lang3 |

## Quick Start

### Prerequisites

- Java 17+
- Gradle 9.2+

### Running the Application

```bash
git clone https://github.com/yourusername/openWLF.git
cd openWLF

./gradlew clean build

./gradlew :api-module:bootRun
```

The application starts on `http://localhost:8080`

### Accessing Services

| Service | URL |
|---------|-----|
| Login Page | http://localhost:8080/login |
| Member Registration UI | http://localhost:8080/member/register |
| Alert Management UI | http://localhost:8080/alerts |
| Case Management UI | http://localhost:8080/cases |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| H2 Console | http://localhost:8080/h2-console |

H2 Console: JDBC URL `jdbc:h2:mem:watchlistdb`, Username `sa`, Password (empty)

## Security

### Authentication & Authorization

OpenWLF implements a **dual security filter chain**:

| Channel | Auth Method | Usage |
|---------|------------|-------|
| REST API (`/api/**`) | Stateless JWT (Bearer Token) | API clients, external integrations |
| Web Pages (`/**`) | Session-based Form Login | Browser-based UI access |

### User Roles

| Role | Permissions |
|------|------------|
| `ROLE_ADMIN` | Full system access, rule management, user management, H2 console |
| `ROLE_MANAGER` | Case decisions, staff management, alert escalation |
| `ROLE_ANALYST` | Alert review, case creation, member filtering |
| `ROLE_VIEWER` | Read-only access to alerts, cases, and reports |

### Authentication API

```bash
# Login and get JWT tokens
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "password"}'

# Use access token for API calls
curl http://localhost:8080/api/alerts \
  -H "Authorization: Bearer <access_token>"

# Refresh expired access token
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "<refresh_token>"}'

# Get current user info
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <access_token>"
```

## Web UI

### Login Page (`/login`)

Form-based login for web UI access with session management.

### Member Registration (`/member/register`)

Thymeleaf-based member onboarding form with real-time watchlist screening.

1. Enter member info (name, DOB, nationality, contact)
2. System performs watchlist filtering
3. Result page shows risk score, matched rules, and approval/rejection

### Alert Management (`/alerts`)

Full alert monitoring and review workflow UI.

| Page | URL | Description |
|------|-----|-------------|
| Alert List | `/alerts` | Alert table with status/score filters, sorting, pagination |
| Alert Detail | `/alerts/{id}` | Alert info, matched rules, risk score, status update form |

**Alert Status Lifecycle**: NEW → IN_REVIEW → ESCALATED → CONFIRMED / FALSE_POSITIVE / CLOSED

### Case Management (`/cases`)

Full case investigation workflow UI.

| Page | URL | Description |
|------|-----|-------------|
| Case List | `/cases` | Open cases table with status/priority filters, pagination |
| Case Detail | `/cases/{id}` | Case info, linked alerts, comments, activity log, decision form |
| Decision Result | `/cases/{id}/decision/result` | Decision confirmation with color-coded status |

**Decision Types**: TRUE_POSITIVE, FALSE_POSITIVE, INCONCLUSIVE, ESCALATED_TO_LE, NO_ACTION_REQUIRED

## REST API

### Filtering

```bash
# Filter customer against watchlists
curl -X POST http://localhost:8080/api/filter/customer \
  -H "Content-Type: application/json" \
  -d '{"name": "John Smith", "dateOfBirth": "1975-05-15", "nationality": "US"}'

# Filter during member registration
curl -X POST http://localhost:8080/api/member/filter \
  -H "Content-Type: application/json" \
  -d '{"name": "John Smith", "dateOfBirth": "1975-05-15", "nationality": "US"}'
```

### Alert Management

```bash
# List alerts
curl http://localhost:8080/api/alerts?page=0&size=20

# Get alert by ID
curl http://localhost:8080/api/alerts/1

# Update alert status
curl -X PUT http://localhost:8080/api/alerts/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "IN_REVIEW", "updatedBy": "analyst01"}'

# Assign alert to analyst
curl -X POST http://localhost:8080/api/alerts/1/assign \
  -H "Content-Type: application/json" \
  -d '{"assignedTo": "analyst01"}'

# Alert statistics
curl http://localhost:8080/api/alerts/stats
```

### Case Management

```bash
# Create case from alert
curl -X POST http://localhost:8080/api/cases/from-alert/1 \
  -H "Content-Type: application/json" \
  -d '{"title": "Investigation Case", "caseType": "SANCTIONS", "createdBy": "admin"}'

# Create case from multiple alerts
curl -X POST http://localhost:8080/api/cases/from-alerts \
  -H "Content-Type: application/json" \
  -d '{"alertIds": [1, 2, 3], "title": "Multi-alert Case", "caseType": "SANCTIONS", "createdBy": "admin"}'

# List open cases
curl http://localhost:8080/api/cases/open

# Get case detail
curl http://localhost:8080/api/cases/1

# Update case status
curl -X PUT http://localhost:8080/api/cases/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "IN_REVIEW"}'

# Assign case to analyst
curl -X PUT http://localhost:8080/api/cases/1/assign \
  -H "Content-Type: application/json" \
  -d '{"assignedTo": "analyst01"}'

# Make decision
curl -X POST http://localhost:8080/api/cases/1/decision \
  -H "Content-Type: application/json" \
  -d '{"decision": "TRUE_POSITIVE", "rationale": "Confirmed match", "decidedBy": "analyst01"}'

# Add comment
curl -X POST http://localhost:8080/api/cases/1/comments \
  -H "Content-Type: application/json" \
  -d '{"content": "Reviewed transaction history", "commentType": "ANALYSIS", "createdBy": "analyst01"}'

# View case activities (audit trail)
curl http://localhost:8080/api/cases/1/activities

# Case statistics
curl http://localhost:8080/api/cases/stats
```

### Matching Algorithms

```bash
# Test name similarity
curl -X POST http://localhost:8080/api/matching/test \
  -H "Content-Type: application/json" \
  -d '{"name1": "Muhammad Ali", "name2": "Mohammed Ali"}'
```

### Sanctions Data

```bash
# List sanctions entities
curl http://localhost:8080/api/v2/sanctions?page=0&size=20

# Search by name similarity
curl http://localhost:8080/api/v2/sanctions/search?name=John+Smith&threshold=0.7

# Trigger manual sync
curl -X POST http://localhost:8080/api/v1/admin/sanctions-sync/all

# Sync history
curl http://localhost:8080/api/v1/admin/sanctions-sync/history
```

### Rules Management (ADMIN only)

```bash
# List all filtering rules
curl http://localhost:8080/api/rules

# Update a rule
curl -X PUT http://localhost:8080/api/rules/1 \
  -H "Content-Type: application/json" \
  -d '{"enabled": true, "weight": 85}'

# Get current rule configuration
curl http://localhost:8080/api/rules/config
```

## Scoring System

### Matching Algorithms & Weights

| Algorithm | Max Score | Description |
|-----------|-----------|-------------|
| Exact Name Match | 100 | Perfect name match after normalization |
| Alias Match | 90 | Matches known aliases |
| Phonetic Match (Metaphone) | 85 | Pronunciation-based matching (Soundex, Double Metaphone) |
| Fuzzy Name Match | 80 | Levenshtein distance similarity (>80%) |
| Jaro-Winkler Match | 80 | Prefix-weighted typo-tolerant similarity |
| N-Gram Match | 75 | Bigram/trigram substring comparison |
| Korean Name Match | 80 | Chosung/Jamo decomposition matching |
| Composite Match | 85 | Weighted combination of multiple algorithms |
| Date of Birth | 50 | Exact DOB match |
| Nationality | 30 | Matching nationality |

### Thresholds

| Range | Action |
|-------|--------|
| 70+ | Registration blocked, alert generated |
| 50-69 | Alert generated, manual review required |
| < 50 | Proceed normally |

## Batch Processing

### Sanctions Sync Scheduler

Automatically downloads and parses sanctions lists from external sources.

| Source | Format | Schedule |
|--------|--------|----------|
| OFAC SDN | XML | Daily 2:00 AM |
| UN Security Council | XML | Daily 2:00 AM |
| EU Financial Sanctions | XML | Daily 2:00 AM |

Features:
- Content hash comparison (detect actual changes)
- Batch insert/update/deactivate (500 records per batch)
- Retry logic (3 retries with 5-second delay)
- Sync history with error logging
- Manual sync via admin API
- 5-minute download timeout

## Audit Logging

10 specialized audit log types for regulatory compliance:

| Log Type | Tracks |
|----------|--------|
| API Access | All API calls (endpoint, method, status, user) |
| Authentication | Login/logout events, failed attempts, account lockout |
| Alert | Alert create, status change, assignment, resolution |
| Case | Case lifecycle (create, assign, decision, close) |
| Filtering | Watchlist screening operations and results |
| Sensitive Data Access | PII data access (customer names, DOB, contact) |
| Rule Change | Rule configuration modifications |
| Watchlist Change | Watchlist entry add/remove/modify |
| Data Export | Data export operations |
| System Config | System configuration changes |

Features:
- ThreadLocal-based `AuditContextHolder` with request ID, user ID, IP, role
- Filter-based context propagation
- Sensitive data masking for PII fields
- EventListener-based authentication audit logging

## Testing

```bash
# Run all tests
./gradlew test

# Run specific module tests
./gradlew :api-module:test
./gradlew :data-module:test
./gradlew :core-module:test
./gradlew :batch-module:test
```

### Test Coverage (37 test suites)

| Module | Tests | Type |
|--------|-------|------|
| **api-module** (13 tests) | AlertController, CaseController, CasePageController, FilteringController, MemberFilteringController, MemberPageController, AuthController, SecurityConfig, JwtTokenProvider, CustomUserDetailsService, CustomUserDetails, AccountLockout, LoginPageController | Integration (MockMvc) |
| **core-module** (8 tests) | AdvancedMatchingService, FilteringService, NormalizationService, ExactMatchEvaluator, FuzzyMatchEvaluator, RuleEngine, ScoringService, RuleConfigurationLoader | Unit tests |
| **data-module** (4 tests) | AlertService, AlertStatisticsService, CaseService, WatchlistDataService | Unit tests (Mockito) |
| **batch-module** (9 tests) | SanctionsSyncService, SanctionsDownloadService, SanctionsSyncHistoryService, OfacXmlParser, UnXmlParser, EuXmlParser, SanctionsSyncScheduler, SanctionsSyncController, SanctionsDownloadProperties | Unit tests |

## Design Patterns & Architecture

- **Strategy Pattern**: 9 matching algorithm strategies via `AdvancedMatchingService`
- **Registry Pattern**: `RuleEvaluatorRegistry` for extensible rule evaluators
- **Facade Pattern**: `FilteringService`, `CaseService` for simplified interfaces
- **Command/Query Separation**: `CaseCommandService` / `CaseQueryService`
- **Repository Pattern**: Spring Data JPA repositories
- **Dual Security Chain**: JWT for stateless API, Session for web UI
- **YAML-driven Configuration**: Dynamic rule loading without code changes

## Configuration

Edit `api-module/src/main/resources/application.yml`:

```yaml
# Filtering thresholds
watchlist:
  threshold:
    alert: 70.0
    review: 50.0

# JWT configuration
jwt:
  secret: your-secret-key
  access-token-expiration: 3600000    # 1 hour
  refresh-token-expiration: 86400000  # 24 hours

# Sanctions sync schedule
sanctions:
  sync:
    cron: "0 0 2 * * *"     # Daily at 2 AM
    batch-size: 500
```

## Sample Data

The system initializes with sample data for development:
- OFAC SDN List entries
- UN Sanctions List entries
- EU Sanctions List entries
- Default users with different roles (admin, manager, analyst, viewer)
- Test watchlist entries

## Future Enhancements

- [x] ~~Advanced fuzzy matching algorithms (Soundex, Metaphone)~~
- [x] ~~Alert management system~~
- [x] ~~Case management with decision workflow~~
- [x] ~~Member registration UI with watchlist screening~~
- [x] ~~Sanctions batch sync (OFAC, UN, EU)~~
- [x] ~~Audit logging system~~
- [x] ~~User authentication and role-based access control (JWT + Spring Security)~~
- [x] ~~Alert management UI (list/detail with Thymeleaf)~~
- [ ] Dashboard with analytics and charts
- [ ] Email/Slack notification on high-risk alerts
- [ ] Machine Learning-based scoring optimization
- [ ] Performance optimization for large-scale screening
- [ ] Kubernetes deployment configuration
- [x] ~~CI/CD pipeline with GitHub Actions~~

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Author

**AML Compliance Team**
- Portfolio Project
- Contact: compliance@openwlf.com

---

**Disclaimer**: This is a portfolio/educational project. For production use in financial institutions, proper regulatory compliance review and testing is required.
