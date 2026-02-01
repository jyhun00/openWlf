package aml.openwlf.data.entity.audit;

import aml.openwlf.data.entity.audit.enums.RuleChangeType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 규칙 변경 감사 로그 엔티티
 *
 * 필터링 규칙의 모든 변경 사항을 기록합니다.
 */
@Entity
@Table(name = "audit_rule_change_log",
        indexes = {
                @Index(name = "idx_rule_change_type", columnList = "change_type"),
                @Index(name = "idx_rule_change_rule_id", columnList = "rule_id"),
                @Index(name = "idx_rule_change_by", columnList = "changed_by"),
                @Index(name = "idx_rule_change_created_at", columnList = "created_at")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AuditRuleChangeLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== 변경 타입 =====

    /**
     * 변경 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 50)
    private RuleChangeType changeType;

    // ===== 규칙 정보 =====

    /**
     * 규칙 ID
     */
    @Column(name = "rule_id", length = 100)
    private String ruleId;

    /**
     * 규칙 이름
     */
    @Column(name = "rule_name", length = 255)
    private String ruleName;

    /**
     * 규칙 타입 (SANCTIONS, PEP, ADVERSE_MEDIA)
     */
    @Column(name = "rule_type", length = 50)
    private String ruleType;

    /**
     * 매칭 타입 (EXACT, FUZZY, PHONETIC 등)
     */
    @Column(name = "match_type", length = 50)
    private String matchType;

    // ===== 변경 내용 =====

    /**
     * 이전 설정 (전체)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_config", columnDefinition = "jsonb")
    private Map<String, Object> oldConfig;

    /**
     * 새 설정 (전체)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_config", columnDefinition = "jsonb")
    private Map<String, Object> newConfig;

    /**
     * 변경된 필드 목록
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changed_fields", columnDefinition = "jsonb")
    private List<String> changedFields;

    // ===== 변경 사유 =====

    /**
     * 변경 사유 (필수)
     */
    @Column(name = "change_reason", nullable = false, columnDefinition = "TEXT")
    private String changeReason;

    /**
     * 변경 요청 티켓 번호
     */
    @Column(name = "ticket_reference", length = 100)
    private String ticketReference;

    /**
     * 승인 참조 번호
     */
    @Column(name = "approval_reference", length = 100)
    private String approvalReference;

    // ===== 변경자 정보 =====

    /**
     * 변경자
     */
    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    /**
     * 변경자 역할
     */
    @Column(name = "changed_by_role", length = 50)
    private String changedByRole;

    /**
     * 승인자 (4-eyes principle)
     */
    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    /**
     * 클라이언트 IP
     */
    @Column(name = "client_ip", length = 45)
    private String clientIp;

    // ===== 타임스탬프 =====

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * 변경 적용 시점
     */
    @Column(name = "effective_from")
    private Instant effectiveFrom;

    // ===== 메타데이터 =====

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}
