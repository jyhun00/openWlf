package aml.openwlf.data.entity.audit;

import aml.openwlf.data.entity.audit.enums.DataAccessType;
import aml.openwlf.data.entity.audit.enums.DataCategory;
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
 * 민감 데이터 접근 감사 로그 엔티티
 *
 * 제재 대상자 정보, 고객 개인정보 등 민감 데이터 조회를 기록합니다.
 */
@Entity
@Table(name = "audit_sensitive_data_access_log",
        indexes = {
                @Index(name = "idx_sensitive_access_by", columnList = "accessed_by"),
                @Index(name = "idx_sensitive_data_category", columnList = "data_category"),
                @Index(name = "idx_sensitive_entity", columnList = "entity_type, entity_id"),
                @Index(name = "idx_sensitive_created_at", columnList = "created_at"),
                @Index(name = "idx_sensitive_access_type", columnList = "access_type")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AuditSensitiveDataAccessLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== 접근 정보 =====

    /**
     * 접근 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "access_type", nullable = false, length = 50)
    private DataAccessType accessType;

    /**
     * 데이터 카테고리
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "data_category", nullable = false, length = 50)
    private DataCategory dataCategory;

    // ===== 대상 데이터 =====

    /**
     * 엔티티 타입 (SANCTIONS_ENTITY, CUSTOMER, ALERT, CASE)
     */
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    /**
     * 조회된 엔티티 ID
     */
    @Column(name = "entity_id", length = 100)
    private String entityId;

    /**
     * 다건 조회 시 ID 목록
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "entity_ids")
    private List<String> entityIds;

    // ===== 검색 정보 =====

    /**
     * 검색 조건 기록
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "search_criteria")
    private Map<String, Object> searchCriteria;

    /**
     * 결과 건수
     */
    @Column(name = "result_count")
    private Integer resultCount;

    // ===== 접근자 정보 =====

    /**
     * 접근자 ID
     */
    @Column(name = "accessed_by", nullable = false, length = 100)
    private String accessedBy;

    /**
     * 접근자 역할
     */
    @Column(name = "accessed_by_role", length = 50)
    private String accessedByRole;

    /**
     * 접근자 부서
     */
    @Column(name = "accessed_by_department", length = 100)
    private String accessedByDepartment;

    /**
     * 클라이언트 IP
     */
    @Column(name = "client_ip", nullable = false, length = 45)
    private String clientIp;

    // ===== 접근 사유 =====

    /**
     * 접근 사유 (필수화 가능)
     */
    @Column(name = "access_reason", length = 500)
    private String accessReason;

    /**
     * 사유 입력 필요 여부
     */
    @Column(name = "justification_required")
    @Builder.Default
    private Boolean justificationRequired = false;

    // ===== 타임스탬프 =====

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ===== 메타데이터 =====

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata;
}
