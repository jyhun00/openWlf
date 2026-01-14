# OpenWLF 작업 내역 (2026-01-14)

## 개요

| 항목 | 내용 |
|------|------|
| 작업일 | 2026-01-14 |
| 커밋 수 | 7개 |
| 변경 파일 | 46개 |
| 추가 라인 | +2,224 |
| 삭제 라인 | -1,176 |

---

## 커밋 상세

### 1. Strategy 패턴 리팩토링 및 아키텍처 개선
**커밋**: `90798fe`

#### 변경 사항
- **AdvancedMatchingService**: Strategy 패턴 적용으로 527줄 → 192줄로 축소
- **AbstractRuleEvaluator**: 추상 기본 클래스 추가 (DRY 원칙)
- **FieldValueExtractor**: 필드 값 추출 유틸리티 분리
- **AlertStatisticsService**: AlertService에서 통계 로직 분리 (SRP)
- **BaseEntity**: JPA Auditing 자동 타임스탬프 관리

#### 신규 파일
```
core-module/src/main/java/aml/openwlf/core/matching/strategy/
├── MatchingStrategy.java (인터페이스)
├── SoundexMatchingStrategy.java
├── MetaphoneMatchingStrategy.java
├── JaroWinklerMatchingStrategy.java
├── NGramMatchingStrategy.java
└── KoreanNameMatchingStrategy.java

core-module/src/main/java/aml/openwlf/core/rule/evaluator/
├── AbstractRuleEvaluator.java
└── FieldValueExtractor.java

data-module/src/main/java/aml/openwlf/data/
├── entity/BaseEntity.java
└── service/AlertStatisticsService.java
```

---

### 2. EU 토큰 환경변수화 (보안)
**커밋**: `35567e6`

#### 변경 사항
- EU 제재 리스트 URL에서 토큰 분리
- 환경변수 `SANCTIONS_EU_TOKEN`으로 설정 가능

#### 수정 파일
- `SanctionsDownloadProperties.java`: `euToken` 필드 및 `getEuFullUrl()` 메서드 추가
- `application.yml`: `eu-token: ${SANCTIONS_EU_TOKEN:}` 설정

#### 사용법
```bash
# Linux/Mac
export SANCTIONS_EU_TOKEN=your-token-here

# Windows
set SANCTIONS_EU_TOKEN=your-token-here
```

---

### 3. TODO 해결 - 실제 동기화 시간 사용
**커밋**: `3959fba`

#### 변경 사항
- `SanctionsController`: 하드코딩된 `LocalDateTime.now()` 제거
- 실제 마지막 성공 동기화 시간 조회 로직 구현

#### 수정 내용
```java
// Before
.lastDataUpdate(LocalDateTime.now()) // TODO: 실제 마지막 업데이트 시간으로 대체

// After
.lastDataUpdate(getLastSuccessfulSyncTime())
```

---

### 4. 매칭 가중치 설정 파일화
**커밋**: `9683a97`

#### 변경 사항
- 하드코딩된 매칭 알고리즘 가중치를 설정 파일로 이동
- `MatchingWeightProperties` 클래스 추가

#### 설정 예시 (application.yml)
```yaml
matching:
  weights:
    with-korean:           # 한글 이름 포함 시
      jaro-winkler: 0.3
      metaphone: 0.2
      ngram: 0.2
      korean: 0.3
    without-korean:        # 영문만 있는 경우
      jaro-winkler: 0.4
      metaphone: 0.3
      ngram: 0.3
```

---

### 5. RuleController API 문서화
**커밋**: `ae4b293`

#### 변경 사항
- `@Parameter` 어노테이션 추가 (ruleId)
- `@Content`, `@Schema` 응답 타입 문서화
- `@ArraySchema` 리스트 응답 문서화

---

### 6. unchecked 경고 개선 (타입 안정성)
**커밋**: `f5a393c`

#### 변경 사항
| 파일 | 변경 전 | 변경 후 |
|------|---------|---------|
| SanctionsEntity.java | `(T) value` | `type.cast(value)` |
| OfacXmlParser.java | `(List<String>) obj` | `castToStringList()` 유틸 |

#### 결과
- @SuppressWarnings("unchecked") 3개 제거 (5개 → 2개)

---

### 7. WatchlistController API 문서화
**커밋**: `ec30ea6`

#### 변경 사항
- `@ArraySchema` 페이지네이션 응답 문서화
- `@Schema` 통계 응답 타입 명시
- 설명 문구 보강

---

## 아키텍처 개선 요약

### 적용된 디자인 패턴
| 패턴 | 적용 위치 | 효과 |
|------|----------|------|
| **Strategy** | AdvancedMatchingService | 알고리즘 캡슐화, 확장성 향상 |
| **Template Method** | AbstractRuleEvaluator | 공통 로직 재사용 |
| **Facade** | AdvancedMatchingService | 복잡한 매칭 로직 단순화 |

### SOLID 원칙 준수
| 원칙 | 적용 내용 |
|------|----------|
| **SRP** | AlertService → AlertStatisticsService 분리 |
| **OCP** | Strategy 패턴으로 새 알고리즘 추가 용이 |
| **DRY** | AbstractRuleEvaluator, FieldValueExtractor |

---

## 테스트 현황

### 추가된 테스트
- `AlertStatisticsServiceTest.java` (189줄)
  - 전체 통계 조회
  - 개별 통계 메서드
  - 비율 계산 (해결률, False Positive율)

### 테스트 커버리지
- 모든 모듈 빌드 성공
- 기존 테스트 모두 통과

---

## 설정 변경 사항

### application.yml 추가 설정
```yaml
# 매칭 알고리즘 가중치
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

# EU 제재 리스트 토큰 (환경변수)
sanctions:
  download:
    eu-token: ${SANCTIONS_EU_TOKEN:}
```

---

## 향후 권장 작업

### 높은 우선순위
1. **CaseService 테스트 작성** - 핵심 비즈니스 로직
2. **SanctionsQueryService 테스트 작성** - 제재 대상 검색

### 중간 우선순위
3. **MatchingTestController API 문서화**
4. **FilteringHistoryService 테스트 작성**

### 낮은 우선순위
5. **RuleDefinition unchecked 경고** - 제네릭 메서드 개선
6. **EuXmlParserTest unchecked 경고** - 테스트 코드 개선
