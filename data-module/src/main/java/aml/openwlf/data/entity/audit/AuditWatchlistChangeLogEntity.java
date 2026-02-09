package aml.openwlf.data.entity.audit;

import aml.openwlf.data.entity.audit.enums.WatchlistChangeType;
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
 * 감시목록 변경 감사 로그 엔티티
 *
 * 감시목록 데이터의 추가, 수정, 삭제를 기록합니다.
 */
@Entity
@Table(name = "audit_watchlist_change_log",
        indexes = {
                @Index(name = "idx_watchlist_change_type", columnList = "change_type"),
                @Index(name = "idx_watchlist_change_entry", columnList = "entry_id"),
                @Index(name = "idx_watchlist_change_source", columnList = "list_source"),
                @Index(name = "idx_watchlist_change_by", columnList = "changed_by"),
                @Index(name = "idx_watchlist_change_created_at", columnList = "created_at")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AuditWatchlistChangeLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== 변경 타입 =====

    /**
     * 변경 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 50)
    private WatchlistChangeType changeType;

    // ===== 대상 항목 =====

    /**
     * 항목 ID
     */
    @Column(name = "entry_id")
    private Long entryId;

    /**
     * 항목 이름
     */
    @Column(name = "entry_name", length = 500)
    private String entryName;

    /**
     * 항목 타입 (INDIVIDUAL, ENTITY, VESSEL)
     */
    @Column(name = "entry_type", length = 50)
    private String entryType;

    /**
     * 목록 출처 (OFAC, UN, EU, INTERNAL)
     */
    @Column(name = "list_source", length = 50)
    private String listSource;

    // ===== 변경 내용 =====

    /**
     * 이전 데이터
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_data")
    private Map<String, Object> oldData;

    /**
     * 새 데이터
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_data")
    private Map<String, Object> newData;

    /**
     * 변경된 필드 목록
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changed_fields")
    private List<String> changedFields;

    // ===== 변경 사유 =====

    /**
     * 변경 사유
     */
    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;

    /**
     * 출처 문서 참조
     */
    @Column(name = "source_reference", length = 255)
    private String sourceReference;

    // ===== 변경자 정보 =====

    /**
     * 변경자 (SYSTEM 또는 사용자 ID)
     */
    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    /**
     * 변경 소스 (SYNC, MANUAL, IMPORT)
     */
    @Column(name = "change_source", nullable = false, length = 50)
    private String changeSource;

    /**
     * 클라이언트 IP
     */
    @Column(name = "client_ip", length = 45)
    private String clientIp;

    // ===== 동기화 관련 =====

    /**
     * SanctionsSyncHistory ID
     */
    @Column(name = "sync_job_id")
    private Long syncJobId;

    // ===== 타임스탬프 =====

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ===== 메타데이터 =====

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata;
}
