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
- **Intelligent Matching**:
  - Exact name / Fuzzy (Levenshtein) / Alias matching
  - Soundex, Double Metaphone (phonetic)
  - Jaro-Winkler (typo-tolerant)
  - N-Gram (substring)
  - Korean Name Matching (Chosung/Jamo)
  - Composite (weighted combination)
- **Alert Management**: Auto-generated alerts with status lifecycle, assignment, statistics
- **Case Management**: Full case lifecycle - creation from alerts, investigation, decision, closure
- **Member Registration Screening**: UI workflow for member onboarding with watchlist check
- **Risk Scoring**: Weighted scoring system with configurable thresholds
- **Audit Logging**: 10 specialized audit log types for regulatory compliance
- **OpenAPI Documentation**: Interactive Swagger UI

## Architecture

```
openWLF/
├── config-module/        # Shared configuration
├── core-module/          # Business logic (Normalization, RuleEngine, Scoring, Matching)
├── data-module/          # Data layer (Entities, Repositories, Services)
├── batch-module/         # Batch processing (Sanctions sync scheduler, XML parsers)
└── api-module/           # REST APIs, Page Controllers, Thymeleaf UI
```

### Technology Stack

| Category | Technology |
|----------|-----------|
| Framework | Spring Boot 4.0.1 |
| Build Tool | Gradle 9.2 |
| Database | H2 (Dev) / PostgreSQL, MySQL (Prod-ready) |
| Template Engine | Thymeleaf |
| API Docs | SpringDoc OpenAPI 3.0 |
| Testing | JUnit 5, MockMvc, Mockito |

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
| Member Registration UI | http://localhost:8080/member/register |
| Case Management UI | http://localhost:8080/cases |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| H2 Console | http://localhost:8080/h2-console |

H2 Console: JDBC URL `jdbc:h2:mem:watchlistdb`, Username `sa`, Password (empty)

## Web UI

### Member Registration (`/member/register`)

Thymeleaf-based member onboarding form with real-time watchlist screening.

1. Enter member info (name, DOB, nationality, contact)
2. System performs watchlist filtering
3. Result page shows risk score, matched rules, and approval/rejection

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

# Alert statistics
curl http://localhost:8080/api/alerts/stats
```

### Case Management

```bash
# Create case from alert
curl -X POST http://localhost:8080/api/cases/from-alert/1 \
  -H "Content-Type: application/json" \
  -d '{"title": "Investigation Case", "caseType": "SANCTIONS", "createdBy": "admin"}'

# List open cases
curl http://localhost:8080/api/cases/open

# Get case detail
curl http://localhost:8080/api/cases/1

# Make decision
curl -X POST http://localhost:8080/api/cases/1/decision \
  -H "Content-Type: application/json" \
  -d '{"decision": "TRUE_POSITIVE", "rationale": "Confirmed match", "decidedBy": "analyst01"}'

# Add comment
curl -X POST http://localhost:8080/api/cases/1/comments \
  -H "Content-Type: application/json" \
  -d '{"content": "Reviewed transaction history", "commentType": "ANALYSIS", "createdBy": "analyst01"}'

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

## Scoring System

### Rule Weights

| Rule Type | Max Score | Description |
|-----------|-----------|-------------|
| Exact Name Match | 100 | Perfect name match |
| Phonetic Match (Metaphone) | 85 | Pronunciation-based matching |
| Fuzzy Name Match | 80 | Levenshtein similarity (>80%) |
| Jaro-Winkler Match | 80 | Prefix-weighted similarity |
| Alias Match | 90 | Matches known alias |
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
- Batch insert/update/deactivate
- Sync history with error logging
- Manual sync via admin API

## Audit Logging

10 specialized audit log types for regulatory compliance:

| Log Type | Tracks |
|----------|--------|
| API Access | All API calls (endpoint, method, status, user) |
| Authentication | Login/logout events |
| Alert | Alert create, status change, assignment, resolution |
| Case | Case lifecycle (create, assign, decision, close) |
| Filtering | Watchlist screening operations and results |
| Sensitive Data Access | PII data access (customer names, DOB, contact) |
| Rule Change | Rule configuration modifications |
| Watchlist Change | Watchlist entry add/remove/modify |
| Data Export | Data export operations |
| System Config | System configuration changes |

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

### Test Coverage

| Module | Tests | Coverage |
|--------|-------|----------|
| core-module | Normalization, RuleEngine, Scoring, Matching algorithms | Unit tests |
| data-module | AlertService, CaseService, WatchlistDataService | Unit tests (Mockito) |
| api-module | AlertController, CaseController, CasePageController, MemberPageController, MemberFilteringController, FilteringController | Integration tests (MockMvc) |
| batch-module | XML Parsers, SyncService, Scheduler, SyncHistory | Unit tests |

## Configuration

Edit `api-module/src/main/resources/application.yml`:

```yaml
watchlist:
  threshold:
    alert: 70.0
    review: 50.0

sanctions:
  sync:
    cron: "0 0 2 * * *"     # Daily at 2 AM
    batch-size: 500
```

## Sample Data

The system initializes with sample watchlist entries:
- OFAC SDN List entries
- UN Sanctions List entries
- EU Sanctions List entries
- Test entries for development

## Future Enhancements

- [x] ~~Advanced fuzzy matching algorithms (Soundex, Metaphone)~~
- [x] ~~Alert management system~~
- [x] ~~Case management with decision workflow~~
- [x] ~~Member registration UI with watchlist screening~~
- [x] ~~Sanctions batch sync (OFAC, UN, EU)~~
- [x] ~~Audit logging system~~
- [ ] Machine Learning-based scoring optimization
- [ ] Dashboard with analytics and charts
- [ ] User authentication and role-based access control
- [ ] Email/Slack notification on high-risk alerts
- [ ] Performance optimization for large-scale screening
- [ ] Kubernetes deployment configuration
- [ ] CI/CD pipeline with GitHub Actions

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Author

**AML Compliance Team**
- Portfolio Project
- Contact: compliance@openwlf.com

---

**Disclaimer**: This is a portfolio/educational project. For production use in financial institutions, proper regulatory compliance review and testing is required.
