package aml.openwlf.data.repository.audit;

import aml.openwlf.data.entity.audit.AuditWatchlistChangeLogEntity;
import aml.openwlf.data.entity.audit.enums.WatchlistChangeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * 감시목록 변경 감사 로그 Repository
 */
@Repository
public interface AuditWatchlistChangeLogRepository extends JpaRepository<AuditWatchlistChangeLogEntity, Long> {

    /**
     * 항목 ID로 변경 이력 조회
     */
    List<AuditWatchlistChangeLogEntity> findByEntryIdOrderByCreatedAtDesc(Long entryId);

    /**
     * 변경 타입으로 조회
     */
    Page<AuditWatchlistChangeLogEntity> findByChangeType(WatchlistChangeType changeType, Pageable pageable);

    /**
     * 목록 출처로 조회
     */
    Page<AuditWatchlistChangeLogEntity> findByListSource(String listSource, Pageable pageable);

    /**
     * 변경 소스로 조회 (SYNC, MANUAL, IMPORT)
     */
    Page<AuditWatchlistChangeLogEntity> findByChangeSource(String changeSource, Pageable pageable);

    /**
     * 동기화 작업 ID로 조회
     */
    List<AuditWatchlistChangeLogEntity> findBySyncJobIdOrderByCreatedAt(Long syncJobId);

    /**
     * 기간별 조회
     */
    Page<AuditWatchlistChangeLogEntity> findByCreatedAtBetween(Instant start, Instant end, Pageable pageable);

    /**
     * 목록 출처별 변경 통계
     */
    @Query("SELECT a.listSource, a.changeType, COUNT(a) FROM AuditWatchlistChangeLogEntity a " +
            "WHERE a.createdAt BETWEEN :start AND :end GROUP BY a.listSource, a.changeType")
    List<Object[]> countByListSourceAndChangeTypeBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * 수동 변경 이력 조회
     */
    @Query("SELECT a FROM AuditWatchlistChangeLogEntity a WHERE a.changeSource = 'MANUAL' ORDER BY a.createdAt DESC")
    Page<AuditWatchlistChangeLogEntity> findManualChanges(Pageable pageable);

    /**
     * 동기화 통계 (출처별)
     */
    @Query("SELECT a.listSource, " +
            "SUM(CASE WHEN a.changeType = 'SYNC_INSERTED' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN a.changeType = 'SYNC_UPDATED' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN a.changeType = 'SYNC_DEACTIVATED' THEN 1 ELSE 0 END) " +
            "FROM AuditWatchlistChangeLogEntity a WHERE a.createdAt BETWEEN :start AND :end " +
            "GROUP BY a.listSource")
    List<Object[]> getSyncStatisticsBySource(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * 최근 동기화 작업별 요약
     */
    @Query("SELECT a.syncJobId, a.listSource, COUNT(a), MIN(a.createdAt), MAX(a.createdAt) " +
            "FROM AuditWatchlistChangeLogEntity a WHERE a.syncJobId IS NOT NULL " +
            "GROUP BY a.syncJobId, a.listSource ORDER BY MAX(a.createdAt) DESC")
    List<Object[]> getRecentSyncJobSummaries(Pageable pageable);
}
