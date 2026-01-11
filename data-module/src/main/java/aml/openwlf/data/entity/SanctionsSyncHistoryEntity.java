package aml.openwlf.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 제재 리스트 동기화 이력 테이블
 * 
 * OFAC, UN 각각의 동기화 실행 이력을 저장합니다.
 */
@Entity
@Table(name = "sanctions_sync_history", indexes = {
        @Index(name = "idx_ssh_source_file", columnList = "source_file"),
        @Index(name = "idx_ssh_status", columnList = "status"),
        @Index(name = "idx_ssh_started_at", columnList = "started_at"),
        @Index(name = "idx_ssh_source_status", columnList = "source_file, status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanctionsSyncHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    /**
     * 데이터 소스 (OFAC, UN)
     */
    @Column(name = "source_file", length = 50, nullable = false)
    private String sourceFile;

    /**
     * 실행 상태 (SUCCESS, FAIL)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private SyncStatus status;

    /**
     * 신규 삽입 건수
     */
    @Column(name = "insert_count")
    @Builder.Default
    private Integer insertCount = 0;

    /**
     * 업데이트 건수
     */
    @Column(name = "update_count")
    @Builder.Default
    private Integer updateCount = 0;

    /**
     * 변경 없음 건수
     */
    @Column(name = "unchanged_count")
    @Builder.Default
    private Integer unchangedCount = 0;

    /**
     * 비활성화 건수
     */
    @Column(name = "deactivated_count")
    @Builder.Default
    private Integer deactivatedCount = 0;

    /**
     * 총 처리 건수
     */
    @Column(name = "total_processed")
    @Builder.Default
    private Integer totalProcessed = 0;

    /**
     * 실행 시작 시간
     */
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    /**
     * 실행 완료 시간
     */
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    /**
     * 소요 시간 (밀리초)
     */
    @Column(name = "duration_ms")
    private Long durationMs;

    /**
     * 설명 / 에러 로그
     * 실패 시 전체 에러 스택트레이스 저장
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 다운로드된 XML 파일 크기 (bytes)
     */
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    /**
     * 동기화 상태 Enum
     */
    public enum SyncStatus {
        SUCCESS,
        FAIL
    }

    /**
     * 소요 시간 계산 (초 단위)
     */
    public double getDurationSeconds() {
        return durationMs != null ? durationMs / 1000.0 : 0;
    }

    /**
     * 성공 여부
     */
    public boolean isSuccess() {
        return status == SyncStatus.SUCCESS;
    }
}
