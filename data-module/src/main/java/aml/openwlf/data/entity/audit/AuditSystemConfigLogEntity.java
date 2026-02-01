package aml.openwlf.data.entity.audit;

import aml.openwlf.data.entity.audit.enums.ConfigCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Map;

/**
 * 시스템 설정 변경 감사 로그 엔티티
 *
 * 시스템 설정 및 임계값 변경을 기록합니다.
 */
@Entity
@Table(name = "audit_system_config_log",
        indexes = {
                @Index(name = "idx_config_category", columnList = "config_category"),
                @Index(name = "idx_config_key", columnList = "config_key"),
                @Index(name = "idx_config_changed_by", columnList = "changed_by"),
                @Index(name = "idx_config_created_at", columnList = "created_at")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AuditSystemConfigLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== 변경 타입 =====

    /**
     * 설정 카테고리
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "config_category", nullable = false, length = 50)
    private ConfigCategory configCategory;

    /**
     * 설정 키
     */
    @Column(name = "config_key", nullable = false, length = 255)
    private String configKey;

    // ===== 변경 내용 =====

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
     * 승인자
     */
    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    /**
     * 클라이언트 IP
     */
    @Column(name = "client_ip", length = 45)
    private String clientIp;

    // ===== 적용 정보 =====

    /**
     * 적용 여부
     */
    @Column(name = "is_applied")
    @Builder.Default
    private Boolean isApplied = true;

    /**
     * 적용 시점
     */
    @Column(name = "applied_at")
    private Instant appliedAt;

    /**
     * 재시작 필요 여부
     */
    @Column(name = "requires_restart")
    @Builder.Default
    private Boolean requiresRestart = false;

    // ===== 타임스탬프 =====

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ===== 메타데이터 =====

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}
