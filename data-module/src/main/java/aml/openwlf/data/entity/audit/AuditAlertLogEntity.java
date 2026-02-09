package aml.openwlf.data.entity.audit;

import aml.openwlf.data.entity.audit.enums.AlertActionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Map;

/**
 * Alert 감사 로그 엔티티
 *
 * Alert의 모든 변경 이력을 상세히 기록합니다.
 */
@Entity
@Table(name = "audit_alert_log",
        indexes = {
                @Index(name = "idx_alert_log_alert_id", columnList = "alert_id"),
                @Index(name = "idx_alert_log_alert_ref", columnList = "alert_reference"),
                @Index(name = "idx_alert_log_action", columnList = "action_type"),
                @Index(name = "idx_alert_log_performed_by", columnList = "performed_by"),
                @Index(name = "idx_alert_log_created_at", columnList = "created_at"),
                @Index(name = "idx_alert_log_customer", columnList = "customer_id")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AuditAlertLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== Alert 식별 =====

    /**
     * Alert ID
     */
    @Column(name = "alert_id", nullable = false)
    private Long alertId;

    /**
     * Alert 참조번호 (ALT-20250117-abc123)
     */
    @Column(name = "alert_reference", nullable = false, length = 50)
    private String alertReference;

    // ===== 액션 정보 =====

    /**
     * 액션 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private AlertActionType actionType;

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

    // ===== 컨텍스트 =====

    /**
     * 관련 고객 ID
     */
    @Column(name = "customer_id", length = 100)
    private String customerId;

    /**
     * 연결된 Case ID (있는 경우)
     */
    @Column(name = "case_id")
    private Long caseId;

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
     * 수행자 IP
     */
    @Column(name = "performed_by_ip", length = 45)
    private String performedByIp;

    // ===== 타임스탬프 =====

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ===== 메타데이터 =====

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata;
}
