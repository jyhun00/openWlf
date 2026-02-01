# OpenWLF 기술 스펙

## 개요

**OpenWLF (Open Watchlist Filtering System)**는 금융 AML/KYC 컴플라이언스를 위한 감시목록 필터링 시스템입니다.

---

## 1. 프로젝트 구조 (멀티 모듈)

```
openWLF/
├── api-module/        # REST API 엔드포인트 및 컨트롤러 계층
├── batch-module/      # 배치 처리 및 정시 스케줄링 (제재리스트 동기화)
├── config-module/     # 규칙 설정 관리 (YAML 기반)
├── core-module/       # 핵심 비즈니스 로직 (필터링, 규칙엔진, 매칭)
└── data-module/       # 데이터 접근 계층 (JPA, Entity, Repository)
```

| 모듈 | 역할 |
|------|------|
| `api-module` | REST API 엔드포인트 및 컨트롤러 |
| `batch-module` | 제재리스트 동기화 배치 처리 |
| `config-module` | YAML 기반 규칙 설정 관리 |
| `core-module` | 핵심 비즈니스 로직 (필터링, 매칭) |
| `data-module` | JPA Entity, Repository |

### 모듈 의존성

- `api-module` → `core-module`, `config-module`, `data-module`, `batch-module`
- `batch-module` → `core-module`, `data-module`
- `core-module` → `config-module`
- `data-module` → (독립적)

---

## 2. 핵심 기술 스택

| 항목 | 버전/내용 |
|------|----------|
| **Java** | 17 (LTS) |
| **Spring Boot** | 4.0.1 |
| **빌드 도구** | Gradle 9.2+ |
| **ORM** | Spring Data JPA / Hibernate |
| **DB (개발)** | H2 (In-memory) |
| **DB (운영)** | PostgreSQL |
| **API 문서** | OpenAPI 3.0 / Swagger UI (springdoc 2.3.0) |
| **테스트** | JUnit 5, Mockito, MockMvc |

---

## 3. 주요 라이브러리

### Spring Boot 스타터

| 라이브러리 | 목적 |
|----------|------|
| `spring-boot-starter` | 기본 Spring Boot 자동설정 |
| `spring-boot-starter-web` | REST API 지원 (Tomcat 내장) |
| `spring-boot-starter-data-jpa` | JPA/Hibernate ORM |
| `spring-boot-starter-batch` | 배치 처리 (batch-module) |
| `spring-boot-starter-validation` | Bean Validation |
| `spring-boot-starter-test` | 테스트 프레임워크 |

### 문자열/텍스트 처리

| 라이브러리 | 버전 | 설명 |
|----------|------|------|
| Apache Commons Text | 1.11.0 | 텍스트 정규화 및 유사도 계산 |
| Apache Commons Lang3 | (Spring 관리) | 범용 유틸리티 |
| Commons Codec | 1.16.0 | Soundex, Metaphone 인코딩 |

### JSON/YAML 처리

| 라이브러리 | 설명 |
|----------|------|
| Jackson Databind | JSON 직렬화/역직렬화 |
| Jackson Datatype JSR310 | Java 8 DateTime 지원 |
| Jackson Dataformat YAML | YAML 파싱 (규칙설정용) |

### 기타

| 라이브러리 | 설명 |
|----------|------|
| Lombok | 보일러플레이트 코드 자동생성 |
| springdoc-openapi | Swagger UI 및 OpenAPI 3.0 |

---

## 4. 데이터베이스

### 개발 환경 (H2)

```yaml
datasource:
  url: jdbc:h2:mem:watchlistdb
  driver-class-name: org.h2.Driver
  username: sa
  password: ""
```

- **Console**: `http://localhost:8080/h2-console`
- **DDL**: `create-drop` (자동 생성/삭제)

### 프로덕션 환경 (PostgreSQL)

- 런타임 의존성으로 구성
- 외부 설정파일에서 지정 가능

### 주요 엔티티

| 엔티티 | 설명 |
|--------|------|
| `AlertEntity` | 생성된 경보 (score, status, explanation) |
| `CaseEntity` | 조사 케이스 (priority, decision, assignedTo) |
| `CaseAlertEntity` | Case ↔ Alert 매핑 |
| `CaseCommentEntity` | Case 내 코멘트 |
| `CaseActivityEntity` | Case 활동 로그 |
| `WatchlistEntryEntity` | 감시목록 항목 |
| `SanctionsEntity` | 제재 대상 |
| `SanctionsSyncHistoryEntity` | 동기화 이력 |
| `FilteringHistoryEntity` | 필터링 이력 |

---

## 5. REST API

### 서버 포트

- **API Module**: 8080
- **Batch Module**: 8081

### 주요 엔드포인트

#### 필터링 API (FilteringController)

```
POST /api/filter/customer    고객 필터링 (메인 API)
```

**요청 예시:**
```json
{
  "name": "John Smith",
  "dateOfBirth": "1975-05-15",
  "nationality": "US",
  "customerId": "CUST-12345"
}
```

**응답:**
```json
{
  "alert": true,
  "score": 100.0,
  "matchedRules": [...],
  "explanation": "Alert 설명",
  "customerInfo": {...}
}
```

#### Alert 관리 API (AlertController)

```
GET    /api/alerts                Alert 목록 조회
GET    /api/alerts/{id}           Alert 상세 조회
PUT    /api/alerts/{id}/status    상태 변경
PUT    /api/alerts/{id}/assign    담당자 배정
GET    /api/alerts/stats          통계 조회
```

#### Case 관리 API (CaseController)

```
POST   /api/cases/from-alert/{alertId}  Alert → Case 생성
GET    /api/cases                        Case 목록
PUT    /api/cases/{id}/status           상태 변경
POST   /api/cases/{id}/decision         최종 결정
POST   /api/cases/{id}/comments         코멘트 추가
GET    /api/cases/{id}                  Case 상세
```

#### 규칙 관리 API (RuleController)

```
GET    /api/rules                 전체 규칙 조회
GET    /api/rules/enabled         활성화된 규칙만 조회
POST   /api/rules/reload          규칙 리로드
```

#### 제재 리스트 API (SanctionsController)

```
GET    /api/v2/sanctions                     제재 대상 목록
GET    /api/v2/sanctions/search              유사도 기반 검색
GET    /api/watchlist                        감시목록 조회
POST   /api/watchlist/cache/refresh          캐시 갱신
```

### API 문서

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/api-docs`

---

## 6. 매칭 알고리즘

### 지원 알고리즘 (9종)

| 타입 | Evaluator | 설명 | 최고점수 |
|------|-----------|------|---------|
| EXACT | ExactMatchEvaluator | 정확한 문자열 일치 | 100 |
| FUZZY | FuzzyMatchEvaluator | Levenshtein 거리 기반 | 80 |
| CONTAINS | ContainsMatchEvaluator | 부분 문자열 포함 | 40 |
| DATE_RANGE | DateRangeMatchEvaluator | 날짜 범위 매칭 | 50 |
| PHONETIC | PhoneticMatchEvaluator | Soundex/Metaphone | 85 |
| JARO_WINKLER | JaroWinklerMatchEvaluator | 이름 특화 유사도 | 80 |
| NGRAM | NGramMatchEvaluator | Bigram/Trigram | 60 |
| KOREAN | KoreanNameMatchEvaluator | 한글 초성/자모 매칭 | 70 |
| COMPOSITE | CompositeMatchEvaluator | 가중 평균 조합 | 90 |

### 매칭 가중치 설정

```yaml
matching:
  weights:
    with-korean:
      jaro-winkler: 0.3
      metaphone: 0.2
      ngram: 0.2
      korean: 0.3
    without-korean:
      jaro-winkler: 0.4
      metaphone: 0.3
      ngram: 0.3
```

### 임계값 설정

```yaml
watchlist:
  threshold:
    alert: 70.0    # Alert 생성 임계값
    review: 50.0   # 검토 필요 임계값
```

---

## 7. 규칙 엔진

### 규칙 설정 파일

`config-module/src/main/resources/rules/filtering-rules.yml`

### 정의된 규칙 (14개)

| ID | 이름 | 타입 | 점수 |
|----|------|------|------|
| EXACT_NAME_MATCH | 정확한 이름 일치 | EXACT | 100 |
| FUZZY_NAME_MATCH | 유사 이름 일치 | FUZZY | 80 |
| EXACT_ALIAS_MATCH | 정확한 별칭 일치 | EXACT | 90 |
| FUZZY_ALIAS_MATCH | 유사 별칭 일치 | FUZZY | 75 |
| DOB_MATCH | 생년월일 일치 | DATE_RANGE | 50 |
| NATIONALITY_MATCH | 국적 일치 | EXACT | 30 |
| PHONETIC_NAME_MATCH | 음성 이름 일치 | PHONETIC | 85 |
| PHONETIC_ALIAS_MATCH | 음성 별칭 일치 | PHONETIC | 75 |
| JARO_WINKLER_NAME_MATCH | Jaro-Winkler 이름 | JARO_WINKLER | 80 |
| NGRAM_NAME_MATCH | N-gram 이름 | NGRAM | 60 |
| KOREAN_NAME_MATCH | 한글 이름 일치 | KOREAN | 70 |
| KOREAN_ALIAS_MATCH | 한글 별칭 일치 | KOREAN | 65 |
| COMPOSITE_NAME_MATCH | 복합 이름 일치 | COMPOSITE | 90 |
| PARTIAL_NAME_MATCH | 부분 이름 일치 | CONTAINS | 40 |

### 규칙 구조

```yaml
- id: RULE_ID
  name: "규칙명"
  type: NAME/ALIAS/DOB/NATIONALITY
  enabled: true/false
  priority: 1..15
  condition:
    matchType: EXACT/FUZZY/PHONETIC/...
    sourceField: name/dateOfBirth/nationality
    targetField: name/aliases/dateOfBirth/nationality
    parameters: {...}
  score:
    exactMatch: score
    partialMatch: score
    proportionalToSimilarity: true/false
    maxScore: score
```

---

## 8. 제재 리스트 동기화

### 지원 소스

| 소스 | URL |
|------|-----|
| OFAC (미국) | `https://sanctionslistservice.ofac.treas.gov/api/PublicationPreview/exports/SDN_ADVANCED.XML` |
| UN (유엔) | `https://scsanctions.un.org/resources/xml/en/consolidated.xml` |
| EU (유럽연합) | `https://webgate.ec.europa.eu/fsd/fsf/public/files/xmlFullSanctionsList_1_1/content` |

### 동기화 설정

```yaml
sanctions:
  download:
    ofac-url: ...
    un-url: ...
    eu-token: ${SANCTIONS_EU_TOKEN:}
    max-retries: 3
    download-timeout-ms: 300000
  sync:
    cron: "0 0 2 * * *"  # 매일 02:00
```

---

## 9. 워크플로우

### Alert 상태 흐름

```
NEW → IN_REVIEW → ESCALATED → CONFIRMED / FALSE_POSITIVE / CLOSED
```

### Case 상태 흐름

```
OPEN → IN_PROGRESS → PENDING_INFO → ESCALATED → CLOSED
```

---

## 10. 인프라

### Docker

**Dockerfile (Multi-stage build)**
```dockerfile
# Build stage
FROM gradle:9.2.1-jdk17 AS build
RUN ./gradlew :api-module:bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
COPY --from=build /app/api-module/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Docker Compose**
```yaml
services:
  watchlist-api:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - WATCHLIST_THRESHOLD_ALERT=70.0
    healthcheck:
      test: ["CMD", "wget", "--spider", "http://localhost:8080/api/filter/health"]
```

### CI/CD (GitHub Actions)

**파이프라인 단계:**
1. Checkout 코드
2. JDK 17 설정 (temurin)
3. Gradle 캐시
4. 빌드 (`./gradlew build`)
5. 테스트 (`./gradlew test`)
6. 커버리지 (`./gradlew jacocoTestReport`)
7. Docker 빌드 및 푸시 (main 브랜치)

**트리거:**
- Push: `main`, `develop` 브랜치
- PR: `main` 브랜치

---

## 11. 테스트

### 테스트 스택

- **프레임워크**: JUnit 5 (Jupiter)
- **Mocking**: Mockito
- **MVC 테스트**: MockMvc
- **배치 테스트**: spring-batch-test

### 테스트 실행

```bash
./gradlew test                      # 전체 테스트
./gradlew :core-module:test         # 모듈별 테스트
./gradlew jacocoTestReport          # 커버리지 리포트
```

---

## 12. 빌드 및 실행

### 빌드 명령어

```bash
./gradlew clean build          # 전체 빌드
./gradlew :api-module:bootJar  # API 모듈 JAR 생성
```

### 실행 명령어

```bash
./gradlew :api-module:bootRun  # API 서버 실행
./gradlew :batch-module:bootRun  # 배치 서버 실행
```

### Docker 실행

```bash
docker-compose up -d           # 컨테이너 실행
docker-compose logs -f         # 로그 확인
```

---

## 13. 주요 설정 파일

| 파일 | 위치 | 설명 |
|------|------|------|
| `build.gradle` | 루트 | 빌드 설정 |
| `settings.gradle` | 루트 | 프로젝트/모듈 선언 |
| `application.yml` | api-module/src/main/resources | API 설정 |
| `application.yml` | batch-module/src/main/resources | 배치 설정 |
| `filtering-rules.yml` | config-module/src/main/resources/rules | 매칭 규칙 |
| `Dockerfile` | 루트 | Docker 이미지 빌드 |
| `docker-compose.yml` | 루트 | Docker Compose |
| `ci-cd.yml` | .github/workflows | CI/CD 파이프라인 |

---

## 종합 요약

| 항목 | 값 |
|------|-----|
| **프로젝트명** | OpenWLF (Open Watchlist Filtering System) |
| **프로젝트 타입** | 금융 컴플라이언스 AML/KYC 필터링 시스템 |
| **Java 버전** | 17 (LTS) |
| **Spring Boot 버전** | 4.0.1 |
| **빌드 도구** | Gradle 9.2+ |
| **데이터베이스** | H2 (Dev), PostgreSQL (Prod) |
| **API 문서** | OpenAPI 3.0 / Swagger UI |
| **매칭 알고리즘** | 9종 |
| **제재 리스트** | OFAC, UN, EU |
| **배포** | Docker, Docker Compose |
| **CI/CD** | GitHub Actions |
| **테스트** | JUnit 5, Mockito |
| **모듈 수** | 5개 |
| **API 엔드포인트** | 30+ 개 |
| **포트** | API: 8080, Batch: 8081 |
