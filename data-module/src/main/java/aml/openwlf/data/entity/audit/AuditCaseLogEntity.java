package aml.openwlf.data.entity.audit;

import aml.openwlf.data.entity.audit.enums.CaseActionType;
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
 * Case 감사 로그 엔티티
 *
 * 기존 CaseActivityEntity를 확장하여 더 상세한 감사 추적을 제공합니다.
 */
@Entity
@Table(name = "audit_case_log",
        indexes = {
                @Index(name = "idx_case_log_case_id", columnList = "case_id"),
                @Index(name = "idx_case_log_case_ref", columnList = "case_reference"),
                @Index(name = "idx_case_log_action", columnList = "action_type"),
                @Index(name = "idx_case_log_performed_by", columnList = "performed_by"),
                @Index(name = "idx_case_log_created_at", columnList = "created_at")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AuditCaseLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== Case 식별 =====

    /**
     * Case ID
     */
    @Column(name = "case_id", nullable = false)
    private Long caseId;

    /**
     * Case 참조번호 (CASE-20250117-abc123)
     */
    @Column(name = "case_reference", nullable = false, length = 50)
    private String caseReference;

    // ===== 액션 정보 =====

    /**
     * 액션 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private CaseActionType actionType;

    // ===== 변경 내용 =====

    /**
     * 변경된 필드명
     */
    @Column(name = "field_name", length = 100)
    private String fieldName;

    /**
     * 이전 값
     */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /**
     * 새 값
     */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /**
     * 변경 사유
     */
    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;

    // ===== 관련 엔티티 =====

    /**
     * 관련 Alert ID 목록
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "related_alert_ids", columnDefinition = "jsonb")
    private List<Long> relatedAlertIds;

    /**
     * 관련 문서 ID 목록
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "related_document_ids", columnDefinition = "jsonb")
    private List<Long> relatedDocumentIds;

    // ===== 고객 정보 =====

    /**
     * 고객 ID
     */
    @Column(name = "customer_id", length = 100)
    private String customerId;

    /**
     * 고객 이름
     */
    @Column(name = "customer_name", length = 255)
    private String customerName;

    // ===== 수행자 정보 =====

    /**
     * 작업 수행자
     */
    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    /**
     * 수행자 역할
     */
    @Column(name = "performed_by_role", length = 50)
    private String performedByRole;

    /**
     * 수행자 팀
     */
    @Column(name = "performed_by_team", length = 100)
    private String performedByTeam;

    /**
     * 수행자 IP
     */
    @Column(name = "performed_by_ip", length = 45)
    private String performedByIp;

    // ===== 규제 관련 =====

    /**
     * 규제 마감일 (SAR 제출 기한 등)
     */
    @Column(name = "regulatory_deadline")
    private Instant regulatoryDeadline;

    /**
     * 규제 관련 액션 여부
     */
    @Column(name = "is_regulatory_action")
    @Builder.Default
    private Boolean isRegulatoryAction = false;

    // ===== 타임스탬프 =====

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ===== 메타데이터 =====

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}
