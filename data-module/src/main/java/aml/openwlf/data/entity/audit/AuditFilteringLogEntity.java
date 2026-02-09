package aml.openwlf.data.entity.audit;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 필터링 상세 감사 로그 엔티티
 *
 * 기존 FilteringHistoryEntity를 확장하여 더 상세한 감사 정보를 기록합니다.
 */
@Entity
@Table(name = "audit_filtering_log",
        indexes = {
                @Index(name = "idx_filtering_log_customer", columnList = "customer_id"),
                @Index(name = "idx_filtering_log_alert", columnList = "is_alert_generated"),
                @Index(name = "idx_filtering_log_score", columnList = "total_score"),
                @Index(name = "idx_filtering_log_created_at", columnList = "created_at"),
                @Index(name = "idx_filtering_log_request", columnList = "request_id")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AuditFilteringLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== 필터링 식별 =====

    /**
     * FilteringHistory ID 연결
     */
    @Column(name = "filtering_id")
    private Long filteringId;

    /**
     * 요청 추적용 UUID
     */
    @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;

    // ===== 요청 정보 =====

    /**
     * 고객 ID
     */
    @Column(name = "customer_id", nullable = false, length = 100)
    private String customerId;

    /**
     * 고객 이름
     */
    @Column(name = "customer_name", length = 255)
    private String customerName;

    /**
     * 고객 국적
     */
    @Column(name = "customer_nationality", length = 100)
    private String customerNationality;

    /**
     * 고객 생년월일
     */
    @Column(name = "customer_dob")
    private LocalDate customerDob;

    // ===== 필터링 결과 =====

    /**
     * 최종 점수
     */
    @Column(name = "total_score", precision = 5, scale = 2)
    private BigDecimal totalScore;

    /**
     * Alert 생성 여부
     */
    @Column(name = "is_alert_generated", nullable = false)
    private Boolean isAlertGenerated;

    /**
     * 생성된 Alert ID
     */
    @Column(name = "alert_id")
    private Long alertId;

    /**
     * 생성된 Alert 참조번호
     */
    @Column(name = "alert_reference", length = 50)
    private String alertReference;

    // ===== 매칭 상세 =====

    /**
     * 매칭된 규칙 수
     */
    @Column(name = "matched_rules_count")
    private Integer matchedRulesCount;

    /**
     * 매칭된 감시목록 항목 수
     */
    @Column(name = "matched_watchlist_count")
    private Integer matchedWatchlistCount;

    /**
     * 상세 규칙 정보
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matched_rules")
    private List<Map<String, Object>> matchedRules;

    /**
     * 매칭된 감시목록 항목 ID
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matched_watchlist_entries")
    private List<Long> matchedWatchlistEntries;

    /**
     * 점수 상세 분석
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "score_breakdown")
    private Map<String, Object> scoreBreakdown;

    // ===== 처리 정보 =====

    /**
     * 처리 시간 (ms)
     */
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    /**
     * 평가된 규칙 수
     */
    @Column(name = "rules_evaluated_count")
    private Integer rulesEvaluatedCount;

    /**
     * 스캔한 감시목록 항목 수
     */
    @Column(name = "watchlist_entries_scanned")
    private Integer watchlistEntriesScanned;

    // ===== 요청자 정보 =====

    /**
     * 요청자 (시스템 또는 사용자)
     */
    @Column(name = "requested_by", length = 100)
    private String requestedBy;

    /**
     * 요청 소스 (API, BATCH, REALTIME)
     */
    @Column(name = "request_source", length = 50)
    private String requestSource;

    /**
     * 클라이언트 IP
     */
    @Column(name = "client_ip", length = 45)
    private String clientIp;

    // ===== 타임스탬프 =====

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ===== 메타데이터 =====

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata;
}
