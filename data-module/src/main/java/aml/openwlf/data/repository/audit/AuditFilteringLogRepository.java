package aml.openwlf.data.repository.audit;

import aml.openwlf.data.entity.audit.AuditFilteringLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 필터링 감사 로그 Repository
 */
@Repository
public interface AuditFilteringLogRepository extends JpaRepository<AuditFilteringLogEntity, Long> {

    /**
     * 요청 ID로 조회
     */
    Optional<AuditFilteringLogEntity> findByRequestId(String requestId);

    /**
     * 고객 ID로 조회
     */
    Page<AuditFilteringLogEntity> findByCustomerId(String customerId, Pageable pageable);

    /**
     * Alert 생성된 필터링만 조회
     */
    Page<AuditFilteringLogEntity> findByIsAlertGeneratedTrue(Pageable pageable);

    /**
     * 점수 이상 필터링 조회
     */
    Page<AuditFilteringLogEntity> findByTotalScoreGreaterThanEqual(BigDecimal score, Pageable pageable);

    /**
     * 기간별 조회
     */
    Page<AuditFilteringLogEntity> findByCreatedAtBetween(Instant start, Instant end, Pageable pageable);

    /**
     * 요청 소스별 조회
     */
    Page<AuditFilteringLogEntity> findByRequestSource(String requestSource, Pageable pageable);

    /**
     * 일별 필터링 통계
     */
    @Query("SELECT FUNCTION('DATE', a.createdAt), COUNT(a), SUM(CASE WHEN a.isAlertGenerated = true THEN 1 ELSE 0 END) " +
            "FROM AuditFilteringLogEntity a WHERE a.createdAt BETWEEN :start AND :end " +
            "GROUP BY FUNCTION('DATE', a.createdAt) ORDER BY FUNCTION('DATE', a.createdAt)")
    List<Object[]> getDailyStatistics(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * 평균 처리 시간 통계
     */
    @Query("SELECT AVG(a.processingTimeMs), MAX(a.processingTimeMs), MIN(a.processingTimeMs) " +
            "FROM AuditFilteringLogEntity a WHERE a.createdAt BETWEEN :start AND :end")
    List<Object[]> getProcessingTimeStatistics(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * 점수 분포 통계
     */
    @Query("SELECT " +
            "SUM(CASE WHEN a.totalScore >= 90 THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN a.totalScore >= 70 AND a.totalScore < 90 THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN a.totalScore >= 50 AND a.totalScore < 70 THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN a.totalScore < 50 THEN 1 ELSE 0 END) " +
            "FROM AuditFilteringLogEntity a WHERE a.createdAt BETWEEN :start AND :end")
    List<Object[]> getScoreDistribution(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * 국적별 Alert 생성 통계
     */
    @Query("SELECT a.customerNationality, COUNT(a), SUM(CASE WHEN a.isAlertGenerated = true THEN 1 ELSE 0 END) " +
            "FROM AuditFilteringLogEntity a WHERE a.createdAt BETWEEN :start AND :end AND a.customerNationality IS NOT NULL " +
            "GROUP BY a.customerNationality ORDER BY COUNT(a) DESC")
    List<Object[]> getStatisticsByNationality(@Param("start") Instant start, @Param("end") Instant end);
}
