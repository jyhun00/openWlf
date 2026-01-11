package aml.openwlf.data.repository;

import aml.openwlf.data.entity.SanctionsSyncHistoryEntity;
import aml.openwlf.data.entity.SanctionsSyncHistoryEntity.SyncStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 제재 리스트 동기화 이력 Repository
 */
@Repository
public interface SanctionsSyncHistoryRepository extends JpaRepository<SanctionsSyncHistoryEntity, Long> {

    /**
     * 특정 소스의 최근 동기화 이력 조회
     */
    Optional<SanctionsSyncHistoryEntity> findTopBySourceFileOrderByStartedAtDesc(String sourceFile);

    /**
     * 특정 소스의 최근 성공 이력 조회
     */
    Optional<SanctionsSyncHistoryEntity> findTopBySourceFileAndStatusOrderByStartedAtDesc(
            String sourceFile, SyncStatus status);

    /**
     * 특정 소스의 동기화 이력 목록 (페이징)
     */
    Page<SanctionsSyncHistoryEntity> findBySourceFileOrderByStartedAtDesc(
            String sourceFile, Pageable pageable);

    /**
     * 특정 상태의 동기화 이력 목록 (페이징)
     */
    Page<SanctionsSyncHistoryEntity> findByStatusOrderByStartedAtDesc(
            SyncStatus status, Pageable pageable);

    /**
     * 특정 소스, 특정 상태의 동기화 이력 목록 (페이징)
     */
    Page<SanctionsSyncHistoryEntity> findBySourceFileAndStatusOrderByStartedAtDesc(
            String sourceFile, SyncStatus status, Pageable pageable);

    /**
     * 전체 동기화 이력 목록 (페이징)
     */
    Page<SanctionsSyncHistoryEntity> findAllByOrderByStartedAtDesc(Pageable pageable);

    /**
     * 특정 기간 내 동기화 이력 조회
     */
    List<SanctionsSyncHistoryEntity> findByStartedAtBetweenOrderByStartedAtDesc(
            LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 특정 소스, 특정 기간 내 동기화 이력 조회
     */
    List<SanctionsSyncHistoryEntity> findBySourceFileAndStartedAtBetweenOrderByStartedAtDesc(
            String sourceFile, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 특정 소스의 실패 횟수 조회 (특정 기간 내)
     */
    @Query("SELECT COUNT(h) FROM SanctionsSyncHistoryEntity h " +
           "WHERE h.sourceFile = :sourceFile " +
           "AND h.status = 'FAIL' " +
           "AND h.startedAt >= :since")
    long countFailuresSince(@Param("sourceFile") String sourceFile, 
                            @Param("since") LocalDateTime since);

    /**
     * 각 소스별 최근 동기화 상태 조회
     */
    @Query("SELECT h FROM SanctionsSyncHistoryEntity h " +
           "WHERE h.startedAt = (SELECT MAX(h2.startedAt) FROM SanctionsSyncHistoryEntity h2 " +
           "WHERE h2.sourceFile = h.sourceFile)")
    List<SanctionsSyncHistoryEntity> findLatestByEachSource();

    /**
     * 특정 소스의 연속 실패 횟수 조회
     */
    @Query(value = "SELECT COUNT(*) FROM (" +
           "SELECT status FROM sanctions_sync_history " +
           "WHERE source_file = :sourceFile " +
           "ORDER BY started_at DESC " +
           "LIMIT :limit) sub " +
           "WHERE sub.status = 'FAIL'", nativeQuery = true)
    int countConsecutiveFailures(@Param("sourceFile") String sourceFile, 
                                  @Param("limit") int limit);

    /**
     * 통계: 소스별 성공/실패 건수
     */
    @Query("SELECT h.sourceFile, h.status, COUNT(h) " +
           "FROM SanctionsSyncHistoryEntity h " +
           "WHERE h.startedAt >= :since " +
           "GROUP BY h.sourceFile, h.status")
    List<Object[]> getStatsBySourceAndStatus(@Param("since") LocalDateTime since);
}
