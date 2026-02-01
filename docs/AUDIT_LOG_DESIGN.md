# OpenWLF Audit Log 설계서

## 1. Audit 관점 기능 분석

### 1.1 Audit 중요도별 기능 분류

| 중요도 | 기능 | 설명 | 규제 요구사항 |
|--------|------|------|---------------|
| ⭐⭐⭐ Critical | 고객 필터링 | 고객 정보와 감시목록 대조 | AML/KYC |
| ⭐⭐⭐ Critical | Alert 관리 | Alert 생성, 상태 변경, 배정, 해결 | SAR 보고 |
| ⭐⭐⭐ Critical | Case 관리 | Case 생성, 결정, SAR 제출 | 규제 감사 |
| ⭐⭐⭐ Critical | 제재 데이터 조회 | 민감한 제재 대상자 정보 접근 | 데이터 보호 |
| ⭐⭐ High | 규칙 변경 | 필터링 규칙 수정/재로드 | 변경 관리 |
| ⭐⭐ High | 감시목록 관리 | 감시목록 데이터 변경 | 데이터 무결성 |
| ⭐⭐ High | Sanctions 동기화 | 외부 데이터 동기화 | 최신성 보장 |
| ⭐ Medium | 시스템 설정 | 임계값, 가중치 변경 | 설정 추적 |
| ⭐ Medium | 데이터 내보내기 | 보고서, 통계 다운로드 | 정보 유출 방지 |

### 1.2 현재 Audit 기능 현황

#### 이미 구현된 감사 추적
- ✅ **CaseActivityEntity**: Case 변경 이력 (상태, 배정, 결정 등)
- ✅ **FilteringHistoryEntity**: 필터링 실행 기록
- ✅ **SanctionsSyncHistoryEntity**: 동기화 이력
- ✅ **BaseEntity**: createdAt/updatedAt 자동 기록

#### 부족한 감사 추적
- ❌ API 접근 로그
- ❌ 사용자 인증/인가 로그
- ❌ Alert 변경 이력 (상세)
- ❌ 민감 데이터 조회 로그
- ❌ 규칙 변경 이력
- ❌ 감시목록 변경 이력
- ❌ 데이터 내보내기 로그
- ❌ 시스템 설정 변경 로그

---

## 2. Audit 로그 테이블 설계

### 2.1 API 접근 로그 (audit_api_access_log)

모든 API 호출을 추적하여 누가, 언제, 무엇을 요청했는지 기록합니다.

```sql
CREATE TABLE audit_api_access_log (
    id BIGSERIAL PRIMARY KEY,

    -- 요청 정보
    request_id VARCHAR(36) NOT NULL,           -- UUID, 요청 추적용
    http_method VARCHAR(10) NOT NULL,          -- GET, POST, PUT, DELETE
    endpoint VARCHAR(500) NOT NULL,            -- /api/alerts/123
    query_params TEXT,                         -- ?status=NEW&page=0
    request_body TEXT,                         -- JSON (민감정보 마스킹)

    -- 응답 정보
    response_status INT NOT NULL,              -- HTTP 상태 코드
    response_time_ms BIGINT,                   -- 응답 시간 (ms)
    error_message TEXT,                        -- 에러 메시지 (있는 경우)

    -- 사용자 정보
    user_id VARCHAR(100),                      -- 인증된 사용자 ID
    user_role VARCHAR(50),                     -- 사용자 역할
    client_ip VARCHAR(45),                     -- IPv4/IPv6
    user_agent VARCHAR(500),                   -- 클라이언트 정보

    -- 컨텍스트
    session_id VARCHAR(100),                   -- 세션 ID
    correlation_id VARCHAR(36),                -- 분산 추적용

    -- 타임스탬프
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- 인덱스용
    CONSTRAINT idx_api_log_created_at CHECK (created_at IS NOT NULL)
);

-- 인덱스
CREATE INDEX idx_audit_api_user_id ON audit_api_access_log(user_id);
CREATE INDEX idx_audit_api_endpoint ON audit_api_access_log(endpoint);
CREATE INDEX idx_audit_api_created_at ON audit_api_access_log(created_at);
CREATE INDEX idx_audit_api_request_id ON audit_api_access_log(request_id);
CREATE INDEX idx_audit_api_status ON audit_api_access_log(response_status);

-- 파티션 (월별)
-- 운영 환경에서는 월별 파티션 적용 권장
```

### 2.2 사용자 인증 로그 (audit_authentication_log)

로그인, 로그아웃, 인증 실패 등을 기록합니다.

```sql
CREATE TABLE audit_authentication_log (
    id BIGSERIAL PRIMARY KEY,

    -- 이벤트 정보
    event_type VARCHAR(50) NOT NULL,           -- LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT,
                                               -- TOKEN_REFRESH, SESSION_EXPIRED,
                                               -- PASSWORD_CHANGE, MFA_SUCCESS, MFA_FAILURE

    -- 사용자 정보
    user_id VARCHAR(100),                      -- 사용자 ID (실패 시 시도된 ID)
    user_email VARCHAR(255),                   -- 이메일
    user_role VARCHAR(50),                     -- 역할

    -- 클라이언트 정보
    client_ip VARCHAR(45) NOT NULL,
    user_agent VARCHAR(500),
    device_fingerprint VARCHAR(255),           -- 기기 식별자
    geo_location VARCHAR(100),                 -- 지역 정보 (선택)

    -- 결과 정보
    is_success BOOLEAN NOT NULL,
    failure_reason VARCHAR(255),               -- 실패 사유

    -- 세션 정보
    session_id VARCHAR(100),
    token_id VARCHAR(100),                     -- JWT ID (jti)

    -- 타임스탬프
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- 추가 메타데이터
    metadata JSONB                             -- 추가 정보 (MFA 방식 등)
);

-- 인덱스
CREATE INDEX idx_auth_log_user_id ON audit_authentication_log(user_id);
CREATE INDEX idx_auth_log_event_type ON audit_authentication_log(event_type);
CREATE INDEX idx_auth_log_created_at ON audit_authentication_log(created_at);
CREATE INDEX idx_auth_log_client_ip ON audit_authentication_log(client_ip);
CREATE INDEX idx_auth_log_success ON audit_authentication_log(is_success);
```

### 2.3 Alert 감사 로그 (audit_alert_log)

Alert의 모든 변경 이력을 상세히 기록합니다.

```sql
CREATE TABLE audit_alert_log (
    id BIGSERIAL PRIMARY KEY,

    -- Alert 식별
    alert_id BIGINT NOT NULL,
    alert_reference VARCHAR(50) NOT NULL,      -- ALT-20250117-abc123

    -- 액션 정보
    action_type VARCHAR(50) NOT NULL,          -- CREATED, STATUS_CHANGED, ASSIGNED,
                                               -- REASSIGNED, ESCALATED, RESOLVED,
                                               -- COMMENT_ADDED, VIEWED, EXPORTED

    -- 변경 내용
    field_name VARCHAR(100),                   -- 변경된 필드명
    old_value TEXT,                            -- 이전 값
    new_value TEXT,                            -- 새 값
    change_reason TEXT,                        -- 변경 사유

    -- 컨텍스트
    customer_id VARCHAR(100),                  -- 관련 고객 ID
    case_id BIGINT,                            -- 연결된 Case ID (있는 경우)

    -- 수행자 정보
    performed_by VARCHAR(100) NOT NULL,
    performed_by_role VARCHAR(50),
    performed_by_ip VARCHAR(45),

    -- 타임스탬프
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- 추가 메타데이터
    metadata JSONB                             -- 추가 컨텍스트 정보
);

-- 인덱스
CREATE INDEX idx_alert_log_alert_id ON audit_alert_log(alert_id);
CREATE INDEX idx_alert_log_alert_ref ON audit_alert_log(alert_reference);
CREATE INDEX idx_alert_log_action ON audit_alert_log(action_type);
CREATE INDEX idx_alert_log_performed_by ON audit_alert_log(performed_by);
CREATE INDEX idx_alert_log_created_at ON audit_alert_log(created_at);
CREATE INDEX idx_alert_log_customer ON audit_alert_log(customer_id);

-- 외래 키 (Alert 삭제 시에도 로그 유지)
-- ALTER TABLE audit_alert_log ADD CONSTRAINT fk_alert_log_alert
--     FOREIGN KEY (alert_id) REFERENCES alerts(id) ON DELETE SET NULL;
```

### 2.4 Case 감사 로그 (audit_case_log) - 기존 CaseActivityEntity 확장

현재 CaseActivityEntity를 확장하여 더 상세한 감사 추적을 제공합니다.

```sql
CREATE TABLE audit_case_log (
    id BIGSERIAL PRIMARY KEY,

    -- Case 식별
    case_id BIGINT NOT NULL,
    case_reference VARCHAR(50) NOT NULL,       -- CASE-20250117-abc123

    -- 액션 정보
    action_type VARCHAR(50) NOT NULL,          -- CREATED, STATUS_CHANGED, ASSIGNED,
                                               -- PRIORITY_CHANGED, DECISION_MADE,
                                               -- ALERT_LINKED, ALERT_UNLINKED,
                                               -- COMMENT_ADDED, SAR_FILED,
                                               -- DOCUMENT_ATTACHED, VIEWED, EXPORTED

    -- 변경 내용
    field_name VARCHAR(100),
    old_value TEXT,
    new_value TEXT,
    change_reason TEXT,

    -- 관련 엔티티
    related_alert_ids JSONB,                   -- 관련 Alert ID 목록
    related_document_ids JSONB,                -- 관련 문서 ID 목록

    -- 고객 정보
    customer_id VARCHAR(100),
    customer_name VARCHAR(255),

    -- 수행자 정보
    performed_by VARCHAR(100) NOT NULL,
    performed_by_role VARCHAR(50),
    performed_by_team VARCHAR(100),
    performed_by_ip VARCHAR(45),

    -- 규제 관련
    regulatory_deadline TIMESTAMP WITH TIME ZONE,  -- SAR 제출 기한 등
    is_regulatory_action BOOLEAN DEFAULT FALSE,    -- 규제 관련 액션 여부

    -- 타임스탬프
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- 추가 메타데이터
    metadata JSONB
);

-- 인덱스
CREATE INDEX idx_case_log_case_id ON audit_case_log(case_id);
CREATE INDEX idx_case_log_case_ref ON audit_case_log(case_reference);
CREATE INDEX idx_case_log_action ON audit_case_log(action_type);
CREATE INDEX idx_case_log_performed_by ON audit_case_log(performed_by);
CREATE INDEX idx_case_log_created_at ON audit_case_log(created_at);
CREATE INDEX idx_case_log_regulatory ON audit_case_log(is_regulatory_action) WHERE is_regulatory_action = TRUE;
```

### 2.5 민감 데이터 접근 로그 (audit_sensitive_data_access_log)

제재 대상자 정보, 고객 개인정보 등 민감 데이터 조회를 기록합니다.

```sql
CREATE TABLE audit_sensitive_data_access_log (
    id BIGSERIAL PRIMARY KEY,

    -- 접근 정보
    access_type VARCHAR(50) NOT NULL,          -- VIEW, SEARCH, EXPORT, DOWNLOAD, PRINT
    data_category VARCHAR(50) NOT NULL,        -- SANCTIONS, CUSTOMER_PII, ALERT_DETAIL,
                                               -- CASE_DETAIL, WATCHLIST, FILTERING_HISTORY

    -- 대상 데이터
    entity_type VARCHAR(50) NOT NULL,          -- SANCTIONS_ENTITY, CUSTOMER, ALERT, CASE
    entity_id VARCHAR(100),                    -- 조회된 엔티티 ID
    entity_ids JSONB,                          -- 다건 조회 시 ID 목록

    -- 검색 조건 (검색 시)
    search_criteria JSONB,                     -- 검색 조건 기록
    result_count INT,                          -- 결과 건수

    -- 접근자 정보
    accessed_by VARCHAR(100) NOT NULL,
    accessed_by_role VARCHAR(50),
    accessed_by_department VARCHAR(100),
    client_ip VARCHAR(45) NOT NULL,

    -- 접근 사유
    access_reason VARCHAR(500),                -- 접근 사유 (필수화 가능)
    justification_required BOOLEAN DEFAULT FALSE,

    -- 타임스탬프
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- 메타데이터
    metadata JSONB
);

-- 인덱스
CREATE INDEX idx_sensitive_access_by ON audit_sensitive_data_access_log(accessed_by);
CREATE INDEX idx_sensitive_data_category ON audit_sensitive_data_access_log(data_category);
CREATE INDEX idx_sensitive_entity ON audit_sensitive_data_access_log(entity_type, entity_id);
CREATE INDEX idx_sensitive_created_at ON audit_sensitive_data_access_log(created_at);
CREATE INDEX idx_sensitive_access_type ON audit_sensitive_data_access_log(access_type);
```

### 2.6 필터링 상세 로그 (audit_filtering_log) - FilteringHistoryEntity 확장

기존 FilteringHistoryEntity를 확장하여 더 상세한 감사 정보를 기록합니다.

```sql
CREATE TABLE audit_filtering_log (
    id BIGSERIAL PRIMARY KEY,

    -- 필터링 식별
    filtering_id BIGINT,                       -- FilteringHistory ID 연결
    request_id VARCHAR(36) NOT NULL,           -- 요청 추적용 UUID

    -- 요청 정보
    customer_id VARCHAR(100) NOT NULL,
    customer_name VARCHAR(255),
    customer_nationality VARCHAR(100),
    customer_dob DATE,

    -- 필터링 결과
    total_score DECIMAL(5,2),
    is_alert_generated BOOLEAN NOT NULL,
    alert_id BIGINT,                           -- 생성된 Alert ID
    alert_reference VARCHAR(50),

    -- 매칭 상세
    matched_rules_count INT,
    matched_watchlist_count INT,
    matched_rules JSONB,                       -- 상세 규칙 정보
    matched_watchlist_entries JSONB,           -- 매칭된 감시목록 항목 ID
    score_breakdown JSONB,                     -- 점수 상세 분석

    -- 처리 정보
    processing_time_ms BIGINT,
    rules_evaluated_count INT,
    watchlist_entries_scanned INT,

    -- 요청자 정보
    requested_by VARCHAR(100),                 -- 시스템 또는 사용자
    request_source VARCHAR(50),                -- API, BATCH, REALTIME
    client_ip VARCHAR(45),

    -- 타임스탬프
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- 메타데이터
    metadata JSONB
);

-- 인덱스
CREATE INDEX idx_filtering_log_customer ON audit_filtering_log(customer_id);
CREATE INDEX idx_filtering_log_alert ON audit_filtering_log(is_alert_generated);
CREATE INDEX idx_filtering_log_score ON audit_filtering_log(total_score);
CREATE INDEX idx_filtering_log_created_at ON audit_filtering_log(created_at);
CREATE INDEX idx_filtering_log_request ON audit_filtering_log(request_id);
```

### 2.7 규칙 변경 로그 (audit_rule_change_log)

필터링 규칙의 모든 변경 사항을 기록합니다.

```sql
CREATE TABLE audit_rule_change_log (
    id BIGSERIAL PRIMARY KEY,

    -- 변경 타입
    change_type VARCHAR(50) NOT NULL,          -- RULE_CREATED, RULE_UPDATED, RULE_DELETED,
                                               -- RULE_ENABLED, RULE_DISABLED,
                                               -- THRESHOLD_CHANGED, WEIGHT_CHANGED,
                                               -- CONFIG_RELOADED

    -- 규칙 정보
    rule_id VARCHAR(100),
    rule_name VARCHAR(255),
    rule_type VARCHAR(50),                     -- SANCTIONS, PEP, ADVERSE_MEDIA
    match_type VARCHAR(50),                    -- EXACT, FUZZY, PHONETIC 등

    -- 변경 내용
    old_config JSONB,                          -- 이전 설정 (전체)
    new_config JSONB,                          -- 새 설정 (전체)
    changed_fields JSONB,                      -- 변경된 필드 목록

    -- 변경 사유
    change_reason TEXT NOT NULL,               -- 필수: 왜 변경했는지
    ticket_reference VARCHAR(100),             -- 변경 요청 티켓 번호
    approval_reference VARCHAR(100),           -- 승인 참조 번호

    -- 변경자 정보
    changed_by VARCHAR(100) NOT NULL,
    changed_by_role VARCHAR(50),
    approved_by VARCHAR(100),                  -- 승인자 (4-eyes principle)
    client_ip VARCHAR(45),

    -- 타임스탬프
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    effective_from TIMESTAMP WITH TIME ZONE,   -- 변경 적용 시점

    -- 메타데이터
    metadata JSONB
);

-- 인덱스
CREATE INDEX idx_rule_change_type ON audit_rule_change_log(change_type);
CREATE INDEX idx_rule_change_rule_id ON audit_rule_change_log(rule_id);
CREATE INDEX idx_rule_change_by ON audit_rule_change_log(changed_by);
CREATE INDEX idx_rule_change_created_at ON audit_rule_change_log(created_at);
```

### 2.8 감시목록 변경 로그 (audit_watchlist_change_log)

감시목록 데이터의 추가, 수정, 삭제를 기록합니다.

```sql
CREATE TABLE audit_watchlist_change_log (
    id BIGSERIAL PRIMARY KEY,

    -- 변경 타입
    change_type VARCHAR(50) NOT NULL,          -- ENTRY_CREATED, ENTRY_UPDATED, ENTRY_DELETED,
                                               -- ENTRY_ACTIVATED, ENTRY_DEACTIVATED,
                                               -- BULK_IMPORT, CACHE_REFRESHED

    -- 대상 항목
    entry_id BIGINT,
    entry_name VARCHAR(500),
    entry_type VARCHAR(50),                    -- INDIVIDUAL, ENTITY, VESSEL
    list_source VARCHAR(50),                   -- OFAC, UN, EU, INTERNAL

    -- 변경 내용
    old_data JSONB,
    new_data JSONB,
    changed_fields JSONB,

    -- 변경 사유
    change_reason TEXT,
    source_reference VARCHAR(255),             -- 출처 문서 참조

    -- 변경자 정보
    changed_by VARCHAR(100) NOT NULL,          -- SYSTEM (동기화) 또는 사용자 ID
    change_source VARCHAR(50) NOT NULL,        -- SYNC, MANUAL, IMPORT
    client_ip VARCHAR(45),

    -- 동기화 관련
    sync_job_id BIGINT,                        -- SanctionsSyncHistory ID

    -- 타임스탬프
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- 메타데이터
    metadata JSONB
);

-- 인덱스
CREATE INDEX idx_watchlist_change_type ON audit_watchlist_change_log(change_type);
CREATE INDEX idx_watchlist_change_entry ON audit_watchlist_change_log(entry_id);
CREATE INDEX idx_watchlist_change_source ON audit_watchlist_change_log(list_source);
CREATE INDEX idx_watchlist_change_by ON audit_watchlist_change_log(changed_by);
CREATE INDEX idx_watchlist_change_created_at ON audit_watchlist_change_log(created_at);
```

### 2.9 데이터 내보내기 로그 (audit_data_export_log)

보고서 생성, 데이터 다운로드, 인쇄 등을 기록합니다.

```sql
CREATE TABLE audit_data_export_log (
    id BIGSERIAL PRIMARY KEY,

    -- 내보내기 정보
    export_type VARCHAR(50) NOT NULL,          -- REPORT, CSV_DOWNLOAD, EXCEL_DOWNLOAD,
                                               -- PDF_DOWNLOAD, PRINT, API_EXPORT

    -- 대상 데이터
    data_type VARCHAR(50) NOT NULL,            -- ALERTS, CASES, SANCTIONS, STATISTICS,
                                               -- FILTERING_HISTORY, WATCHLIST
    report_name VARCHAR(255),

    -- 범위
    filter_criteria JSONB,                     -- 적용된 필터 조건
    date_range_start TIMESTAMP WITH TIME ZONE,
    date_range_end TIMESTAMP WITH TIME ZONE,
    record_count INT,                          -- 내보낸 레코드 수

    -- 파일 정보
    file_format VARCHAR(20),                   -- CSV, EXCEL, PDF, JSON
    file_size_bytes BIGINT,
    file_checksum VARCHAR(64),                 -- SHA-256 해시

    -- 수행자 정보
    exported_by VARCHAR(100) NOT NULL,
    exported_by_role VARCHAR(50),
    exported_by_department VARCHAR(100),
    client_ip VARCHAR(45),

    -- 사유
    export_reason TEXT,                        -- 내보내기 사유
    authorized_by VARCHAR(100),                -- 승인자 (민감 데이터)

    -- 타임스탬프
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- 메타데이터
    metadata JSONB
);

-- 인덱스
CREATE INDEX idx_export_type ON audit_data_export_log(export_type);
CREATE INDEX idx_export_data_type ON audit_data_export_log(data_type);
CREATE INDEX idx_export_by ON audit_data_export_log(exported_by);
CREATE INDEX idx_export_created_at ON audit_data_export_log(created_at);
```

### 2.10 시스템 설정 변경 로그 (audit_system_config_log)

시스템 설정 및 임계값 변경을 기록합니다.

```sql
CREATE TABLE audit_system_config_log (
    id BIGSERIAL PRIMARY KEY,

    -- 변경 타입
    config_category VARCHAR(50) NOT NULL,      -- THRESHOLD, MATCHING_WEIGHT, SCHEDULER,
                                               -- SYNC_CONFIG, FEATURE_FLAG, SECURITY
    config_key VARCHAR(255) NOT NULL,          -- 설정 키

    -- 변경 내용
    old_value TEXT,
    new_value TEXT,

    -- 변경 사유
    change_reason TEXT NOT NULL,
    ticket_reference VARCHAR(100),

    -- 변경자 정보
    changed_by VARCHAR(100) NOT NULL,
    changed_by_role VARCHAR(50),
    approved_by VARCHAR(100),
    client_ip VARCHAR(45),

    -- 적용 정보
    is_applied BOOLEAN DEFAULT TRUE,
    applied_at TIMESTAMP WITH TIME ZONE,
    requires_restart BOOLEAN DEFAULT FALSE,

    -- 타임스탬프
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- 메타데이터
    metadata JSONB
);

-- 인덱스
CREATE INDEX idx_config_category ON audit_system_config_log(config_category);
CREATE INDEX idx_config_key ON audit_system_config_log(config_key);
CREATE INDEX idx_config_changed_by ON audit_system_config_log(changed_by);
CREATE INDEX idx_config_created_at ON audit_system_config_log(created_at);
```

---

## 3. Audit 로그 Entity 클래스 설계

### 3.1 공통 Base Audit Entity

```java
@MappedSuperclass
public abstract class BaseAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "performed_by", nullable = false)
    private String performedBy;

    @Column(name = "performed_by_role")
    private String performedByRole;

    @Column(name = "client_ip")
    private String clientIp;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Type(JsonType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}
```

### 3.2 Action Type Enums

```java
public enum AlertActionType {
    CREATED,
    STATUS_CHANGED,
    ASSIGNED,
    REASSIGNED,
    ESCALATED,
    RESOLVED,
    COMMENT_ADDED,
    VIEWED,
    EXPORTED
}

public enum CaseActionType {
    CREATED,
    STATUS_CHANGED,
    ASSIGNED,
    REASSIGNED,
    PRIORITY_CHANGED,
    DECISION_MADE,
    ALERT_LINKED,
    ALERT_UNLINKED,
    COMMENT_ADDED,
    SAR_FILED,
    DOCUMENT_ATTACHED,
    VIEWED,
    EXPORTED
}

public enum AuthEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    TOKEN_REFRESH,
    SESSION_EXPIRED,
    PASSWORD_CHANGE,
    MFA_SUCCESS,
    MFA_FAILURE,
    ACCOUNT_LOCKED,
    ACCOUNT_UNLOCKED
}

public enum DataAccessType {
    VIEW,
    SEARCH,
    EXPORT,
    DOWNLOAD,
    PRINT
}

public enum RuleChangeType {
    RULE_CREATED,
    RULE_UPDATED,
    RULE_DELETED,
    RULE_ENABLED,
    RULE_DISABLED,
    THRESHOLD_CHANGED,
    WEIGHT_CHANGED,
    CONFIG_RELOADED
}

public enum WatchlistChangeType {
    ENTRY_CREATED,
    ENTRY_UPDATED,
    ENTRY_DELETED,
    ENTRY_ACTIVATED,
    ENTRY_DEACTIVATED,
    BULK_IMPORT,
    CACHE_REFRESHED
}
```

---

## 4. 서비스별 Audit 로그 매핑

| 서비스 | 주요 작업 | 로그 테이블 |
|--------|----------|-------------|
| FilteringController | 고객 필터링 요청 | audit_api_access_log, audit_filtering_log |
| AlertController | Alert CRUD | audit_api_access_log, audit_alert_log |
| AlertController | Alert 조회 | audit_sensitive_data_access_log |
| CaseController | Case CRUD | audit_api_access_log, audit_case_log |
| CaseController | SAR 제출 | audit_case_log (is_regulatory_action = true) |
| RuleController | 규칙 조회/변경 | audit_api_access_log, audit_rule_change_log |
| WatchlistController | 감시목록 조회 | audit_api_access_log, audit_sensitive_data_access_log |
| SanctionsController | 제재 대상 조회 | audit_api_access_log, audit_sensitive_data_access_log |
| SanctionsSyncService | 동기화 실행 | audit_watchlist_change_log |
| (통계/보고서) | 데이터 내보내기 | audit_data_export_log |
| (시스템 설정) | 설정 변경 | audit_system_config_log |
| (인증) | 로그인/로그아웃 | audit_authentication_log |

---

## 5. 데이터 보존 정책

| 로그 테이블 | 보존 기간 | 사유 |
|------------|----------|------|
| audit_authentication_log | 7년 | 보안 감사 |
| audit_api_access_log | 3년 | 일반 접근 기록 |
| audit_alert_log | 10년 | AML 규제 (SAR 관련) |
| audit_case_log | 10년 | AML 규제 |
| audit_filtering_log | 7년 | 필터링 이력 |
| audit_sensitive_data_access_log | 7년 | 개인정보 보호 |
| audit_rule_change_log | 영구 | 규칙 변경 추적 |
| audit_watchlist_change_log | 7년 | 데이터 무결성 |
| audit_data_export_log | 5년 | 정보 유출 추적 |
| audit_system_config_log | 영구 | 설정 변경 추적 |

---

## 6. 구현 권장사항

### 6.1 AOP 기반 자동 로깅

```java
@Aspect
@Component
public class AuditLoggingAspect {

    @Around("@annotation(Auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint) {
        // 메서드 실행 전후로 자동 감사 로그 기록
    }
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Auditable {
    String action();
    String dataCategory() default "";
    boolean sensitiveData() default false;
}
```

### 6.2 필터 기반 API 로깅

```java
@Component
public class AuditLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) {
        // 모든 API 요청/응답 자동 로깅
    }
}
```

### 6.3 민감 데이터 마스킹

```java
@Component
public class SensitiveDataMasker {

    public String maskCustomerName(String name) {
        // 김*동, John *oe
    }

    public String maskDateOfBirth(LocalDate dob) {
        // 1990-**-**
    }

    public String maskNationality(String nationality) {
        // K*R, U*A
    }
}
```

### 6.4 비동기 로그 저장

```java
@Async
@Service
public class AsyncAuditLogService {

    public CompletableFuture<Void> saveAuditLog(AuditLogEntity log) {
        // 비동기로 로그 저장하여 성능 영향 최소화
    }
}
```

---

## 7. 감사 보고서 생성 쿼리 예시

### 7.1 일별 Alert 처리 현황

```sql
SELECT
    DATE(created_at) as date,
    action_type,
    COUNT(*) as count,
    COUNT(DISTINCT performed_by) as unique_users
FROM audit_alert_log
WHERE created_at >= NOW() - INTERVAL '30 days'
GROUP BY DATE(created_at), action_type
ORDER BY date DESC, action_type;
```

### 7.2 사용자별 민감 데이터 접근 현황

```sql
SELECT
    accessed_by,
    data_category,
    access_type,
    COUNT(*) as access_count,
    MAX(created_at) as last_access
FROM audit_sensitive_data_access_log
WHERE created_at >= NOW() - INTERVAL '7 days'
GROUP BY accessed_by, data_category, access_type
ORDER BY access_count DESC;
```

### 7.3 규칙 변경 이력

```sql
SELECT
    rule_id,
    rule_name,
    change_type,
    changed_by,
    approved_by,
    change_reason,
    created_at
FROM audit_rule_change_log
ORDER BY created_at DESC
LIMIT 100;
```

### 7.4 의심스러운 로그인 시도

```sql
SELECT
    client_ip,
    COUNT(*) as failed_attempts,
    COUNT(DISTINCT user_id) as attempted_users,
    MAX(created_at) as last_attempt
FROM audit_authentication_log
WHERE event_type = 'LOGIN_FAILURE'
  AND created_at >= NOW() - INTERVAL '1 hour'
GROUP BY client_ip
HAVING COUNT(*) >= 5
ORDER BY failed_attempts DESC;
```

---

## 8. 테이블 관계 다이어그램

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        AUDIT LOG TABLES                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────────┐     ┌──────────────────────┐                  │
│  │ audit_api_access_log │     │audit_authentication_log│                │
│  │  (모든 API 호출)     │     │  (인증/인가 이벤트)    │                │
│  └──────────┬───────────┘     └──────────────────────┘                  │
│             │                                                            │
│             │ request_id                                                 │
│             ▼                                                            │
│  ┌──────────────────────────────────────────────────────────┐           │
│  │                    DOMAIN AUDIT LOGS                      │           │
│  ├──────────────────┬───────────────────┬───────────────────┤           │
│  │ audit_alert_log  │ audit_case_log    │audit_filtering_log│           │
│  │ (Alert 변경)     │ (Case 변경)       │ (필터링 실행)     │           │
│  │                  │                   │                   │           │
│  │ alert_id ────────┼──► related_alerts │◄─── alert_id      │           │
│  │                  │ case_id           │                   │           │
│  └──────────────────┴───────────────────┴───────────────────┘           │
│                              │                                           │
│                              ▼                                           │
│  ┌──────────────────────────────────────────────────────────┐           │
│  │              SENSITIVE DATA ACCESS LOGS                   │           │
│  │  audit_sensitive_data_access_log                         │           │
│  │  (민감 데이터 조회 추적)                                  │           │
│  └──────────────────────────────────────────────────────────┘           │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────┐           │
│  │              CONFIGURATION CHANGE LOGS                    │           │
│  ├──────────────────┬───────────────────┬───────────────────┤           │
│  │audit_rule_change │audit_watchlist_   │audit_system_      │           │
│  │_log              │change_log         │config_log         │           │
│  │(규칙 변경)       │(감시목록 변경)    │(시스템 설정)      │           │
│  └──────────────────┴───────────────────┴───────────────────┘           │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────┐           │
│  │              DATA EXPORT LOGS                             │           │
│  │  audit_data_export_log                                   │           │
│  │  (데이터 내보내기 추적)                                   │           │
│  └──────────────────────────────────────────────────────────┘           │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 9. 규제 준수 체크리스트

| 규제 요구사항 | 대응 테이블 | 보존 기간 |
|--------------|-------------|----------|
| AML/CFT 거래 모니터링 기록 | audit_filtering_log | 7년 |
| SAR 제출 및 처리 기록 | audit_case_log | 10년 |
| 사용자 접근 통제 기록 | audit_api_access_log, audit_authentication_log | 7년 |
| 개인정보 접근 기록 (GDPR) | audit_sensitive_data_access_log | 7년 |
| 시스템 변경 관리 기록 | audit_rule_change_log, audit_system_config_log | 영구 |
| 데이터 무결성 추적 | audit_watchlist_change_log | 7년 |
| 감사 증적 (Audit Trail) | 모든 테이블 | 해당 기간 |

---

이 설계서는 OpenWLF 시스템의 AML/KYC 규제 준수를 위한 종합적인 audit 로그 체계를 제공합니다.
