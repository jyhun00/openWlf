# OpenWLF Architecture Document

## Overview

**OpenWLF (Open Watchlist Filtering System)**는 금융기관의 AML(Anti-Money Laundering) 컴플라이언스를 위한 오픈소스 감시목록 필터링 시스템입니다.

### Key Features
- 다양한 매칭 알고리즘 (9종) 지원
- OFAC, UN, EU 제재 리스트 자동 동기화
- 동적 규칙 설정 (YAML 기반)
- Alert/Case 관리 워크플로우
- REST API 및 Swagger UI

---

## Module Structure

```
openWLF/
├── api-module/        # REST API 컨트롤러
├── batch-module/      # 배치 처리 및 스케줄링
├── config-module/     # 규칙 설정 관리
├── core-module/       # 핵심 비즈니스 로직
└── data-module/       # 데이터 접근 계층
```

### Module Dependencies
```
api-module
    └── core-module
          ├── config-module
          └── data-module

batch-module
    └── data-module
```

---

## 1. Core Module - 핵심 비즈니스 로직

### Filtering Pipeline

```
CustomerInfo → FilteringService → RuleEngine → Evaluators → ScoringService → FilteringResult
```

| Component | Responsibility |
|-----------|---------------|
| `FilteringService` | 고객 정보를 감시목록과 대조 |
| `RuleEngine` | 설정된 규칙 동적 평가 |
| `RuleEvaluatorRegistry` | 평가기 등록 및 관리 |
| `ScoringService` | 점수 계산 및 Alert 판정 |
| `NormalizationService` | 데이터 정규화 |

### Matching Algorithms (9종)

| Type | Evaluator | Description | Max Score |
|------|-----------|-------------|-----------|
| `EXACT` | ExactMatchEvaluator | 정확한 문자열 일치 | 100 |
| `FUZZY` | FuzzyMatchEvaluator | Levenshtein 거리 기반 | 80 |
| `CONTAINS` | ContainsMatchEvaluator | 부분 문자열 포함 | 40 |
| `DATE_RANGE` | DateRangeMatchEvaluator | 날짜 범위 매칭 | 50 |
| `PHONETIC` | PhoneticMatchEvaluator | Soundex/Metaphone 발음 | 85 |
| `JARO_WINKLER` | JaroWinklerMatchEvaluator | 이름 특화 유사도 | 80 |
| `NGRAM` | NGramMatchEvaluator | Bigram/Trigram | 60 |
| `KOREAN` | KoreanNameMatchEvaluator | 한글 초성/자모 매칭 | 70 |
| `COMPOSITE` | CompositeMatchEvaluator | 가중 평균 조합 | 90 |

### Advanced Matching Service (Strategy Pattern)

```
AdvancedMatchingService (Facade)
    ├── SoundexMatchingStrategy
    ├── MetaphoneMatchingStrategy
    ├── JaroWinklerMatchingStrategy
    ├── NGramMatchingStrategy
    └── KoreanNameMatchingStrategy
```

**Composite Score Weights:**
```yaml
# 한글 포함 시
jaro-winkler: 0.3, metaphone: 0.2, ngram: 0.2, korean: 0.3

# 영문만
jaro-winkler: 0.4, metaphone: 0.3, ngram: 0.3
```

---

## 2. Data Module - 데이터 모델

### Entity Relationship

```
Alert (1) ←──→ (N) CaseAlert (N) ←──→ (1) Case
                                           │
                                           ├── CaseComment
                                           └── CaseActivity

SanctionsEntity (1) ←──→ (N) EntityName
                    ←──→ (N) EntityAddress
                    ←──→ (N) EntityDocument
```

### Alert Status Flow
```
NEW → IN_REVIEW → ESCALATED → CONFIRMED / FALSE_POSITIVE / CLOSED
```

### Case Status Flow
```
OPEN → IN_PROGRESS → PENDING_INFO → ESCALATED → CLOSED
```

### Key Entities

| Entity | Purpose |
|--------|---------|
| `AlertEntity` | 생성된 경보 (status, score, explanation) |
| `CaseEntity` | 조사 케이스 (priority, decision, assignedTo) |
| `WatchlistEntryEntity` | 감시목록 항목 |
| `SanctionsEntity` | 제재 대상 (비정규화) |
| `FilteringHistoryEntity` | 필터링 이력 |

---

## 3. API Module - REST Endpoints

### Filtering API
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/filter/customer` | 고객 필터링 (메인 API) |

### Alert Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/alerts` | Alert 목록 조회 |
| GET | `/api/alerts/{id}` | Alert 상세 |
| PUT | `/api/alerts/{id}/status` | 상태 변경 |
| PUT | `/api/alerts/{id}/assign` | 담당자 배정 |
| GET | `/api/alerts/stats` | 통계 |

### Case Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/cases/from-alert/{alertId}` | Alert → Case 생성 |
| GET | `/api/cases` | Case 목록 |
| PUT | `/api/cases/{id}/status` | 상태 변경 |
| POST | `/api/cases/{id}/decision` | 최종 결정 |
| POST | `/api/cases/{id}/comments` | 코멘트 추가 |

### Rule Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/rules` | 전체 규칙 조회 |
| GET | `/api/rules/enabled` | 활성화 규칙 |
| POST | `/api/rules/reload` | 규칙 리로드 |

### Sanctions & Watchlist
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v2/sanctions` | 제재 대상 목록 |
| GET | `/api/v2/sanctions/search` | 유사도 검색 |
| GET | `/api/watchlist` | 감시목록 조회 |
| POST | `/api/watchlist/cache/refresh` | 캐시 새로고침 |

---

## 4. Batch Module - 제재 리스트 동기화

### Supported Sources

| Source | URL | Format |
|--------|-----|--------|
| OFAC | sanctionslistservice.ofac.treas.gov | XML |
| UN | scsanctions.un.org | XML |
| EU | webgate.ec.europa.eu | XML |

### Sync Schedule
```
Cron: 0 0 2 * * *  (매일 02:00)

Flow:
1. Download XML from each source
2. Parse XML → ParsedSanctionsData
3. Sync to DB (INSERT/UPDATE/DEACTIVATE)
4. Log sync history
```

### Configuration
```yaml
sanctions:
  download:
    ofac-url: https://...
    un-url: https://...
    eu-token: ${SANCTIONS_EU_TOKEN:}  # 환경변수
    max-retries: 3
    download-timeout-ms: 300000
  sync:
    cron: "0 0 2 * * *"
```

---

## 5. Config Module - 규칙 설정

### Rule Definition (YAML)
```yaml
rules:
  - id: EXACT_NAME_MATCH
    name: "정확한 이름 일치"
    type: NAME
    enabled: true
    priority: 1
    condition:
      matchType: EXACT
      sourceField: name
      targetField: name
    score:
      exactMatch: 100.0
```

### Dynamic Rule Loading
- 런타임 중 규칙 리로드 가능 (`/api/rules/reload`)
- YAML 파일 변경 감지 (선택적)

---

## 6. Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     API Module (REST API)                   │
│  FilteringController │ AlertController │ CaseController     │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                  Core Module (Business Logic)               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ FilteringService → RuleEngine → RuleEvaluators (9)  │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ AdvancedMatchingService (Strategy Pattern)          │   │
│  │ Soundex │ Metaphone │ JaroWinkler │ NGram │ Korean  │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                  Data Module (JPA/Repository)               │
│  Alert │ Case │ Watchlist │ Sanctions │ FilteringHistory   │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                      Database (H2/PostgreSQL)               │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│              Batch Module (Scheduled Jobs)                  │
│  SanctionsSyncScheduler → OFAC/UN/EU Parsers → DB Sync     │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│              Config Module (Rule Configuration)             │
│  filtering-rules.yml → RuleConfigurationLoader             │
└─────────────────────────────────────────────────────────────┘
```

---

## 7. Tech Stack

| Category | Technology |
|----------|------------|
| Language | Java 17 |
| Framework | Spring Boot 4.0.1 |
| Build | Gradle 9.2+ |
| ORM | Spring Data JPA |
| Database | H2 (Dev) / PostgreSQL (Prod) |
| API Docs | SpringDoc OpenAPI 3.0 |
| Testing | JUnit 5, Mockito |
| Utilities | Lombok, Apache Commons Text |

---

## 8. Quick Start

```bash
# Build
./gradlew clean build

# Run API Server
./gradlew :api-module:bootRun

# Access
# API: http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html
# H2 Console: http://localhost:8080/h2-console
```

---

## 9. Design Patterns Applied

| Pattern | Location | Purpose |
|---------|----------|---------|
| **Strategy** | AdvancedMatchingService | 알고리즘 캡슐화 |
| **Template Method** | AbstractRuleEvaluator | 공통 로직 재사용 |
| **Facade** | AdvancedMatchingService | 복잡한 로직 단순화 |
| **Registry** | RuleEvaluatorRegistry | 평가기 관리 |
| **Builder** | Entity/DTO classes | 객체 생성 |

---

## 10. Configuration Reference

### application.yml
```yaml
# Thresholds
watchlist:
  threshold:
    alert: 70.0        # Alert 생성 임계값
    review: 50.0       # 검토 필요 임계값

# Matching Weights
matching:
  weights:
    with-korean:
      jaro-winkler: 0.3
      metaphone: 0.2
      ngram: 0.2
      korean: 0.3

# Sanctions Sync
sanctions:
  sync:
    cron: "0 0 2 * * *"
```

### Environment Variables
```bash
SANCTIONS_EU_TOKEN=your-token-here
```
