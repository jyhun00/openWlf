package aml.openwlf.data.entity.audit;

import aml.openwlf.data.entity.audit.enums.ExportType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Map;

/**
 * 데이터 내보내기 감사 로그 엔티티
 *
 * 보고서 생성, 데이터 다운로드, 인쇄 등을 기록합니다.
 */
@Entity
@Table(name = "audit_data_export_log",
        indexes = {
                @Index(name = "idx_export_type", columnList = "export_type"),
                @Index(name = "idx_export_data_type", columnList = "data_type"),
                @Index(name = "idx_export_by", columnList = "exported_by"),
                @Index(name = "idx_export_created_at", columnList = "created_at")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AuditDataExportLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== 내보내기 정보 =====

    /**
     * 내보내기 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "export_type", nullable = false, length = 50)
    private ExportType exportType;

    // ===== 대상 데이터 =====

    /**
     * 데이터 타입 (ALERTS, CASES, SANCTIONS, STATISTICS, FILTERING_HISTORY, WATCHLIST)
     */
    @Column(name = "data_type", nullable = false, length = 50)
    private String dataType;

    /**
     * 보고서 이름
     */
    @Column(name = "report_name", length = 255)
    private String reportName;

    // ===== 범위 =====

    /**
     * 적용된 필터 조건
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "filter_criteria")
    private Map<String, Object> filterCriteria;

    /**
     * 조회 시작일
     */
    @Column(name = "date_range_start")
    private Instant dateRangeStart;

    /**
     * 조회 종료일
     */
    @Column(name = "date_range_end")
    private Instant dateRangeEnd;

    /**
     * 내보낸 레코드 수
     */
    @Column(name = "record_count")
    private Integer recordCount;

    // ===== 파일 정보 =====

    /**
     * 파일 포맷 (CSV, EXCEL, PDF, JSON)
     */
    @Column(name = "file_format", length = 20)
    private String fileFormat;

    /**
     * 파일 크기 (bytes)
     */
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    /**
     * 파일 체크섬 (SHA-256)
     */
    @Column(name = "file_checksum", length = 64)
    private String fileChecksum;

    // ===== 수행자 정보 =====

    /**
     * 내보내기 수행자
     */
    @Column(name = "exported_by", nullable = false, length = 100)
    private String exportedBy;

    /**
     * 수행자 역할
     */
    @Column(name = "exported_by_role", length = 50)
    private String exportedByRole;

    /**
     * 수행자 부서
     */
    @Column(name = "exported_by_department", length = 100)
    private String exportedByDepartment;

    /**
     * 클라이언트 IP
     */
    @Column(name = "client_ip", length = 45)
    private String clientIp;

    // ===== 사유 =====

    /**
     * 내보내기 사유
     */
    @Column(name = "export_reason", columnDefinition = "TEXT")
    private String exportReason;

    /**
     * 승인자 (민감 데이터)
     */
    @Column(name = "authorized_by", length = 100)
    private String authorizedBy;

    // ===== 타임스탬프 =====

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ===== 메타데이터 =====

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata;
}
