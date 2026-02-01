package aml.openwlf.data.repository.audit;

import aml.openwlf.data.entity.audit.AuditApiAccessLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * API 접근 감사 로그 Repository
 */
@Repository
public interface AuditApiAccessLogRepository extends JpaRepository<AuditApiAccessLogEntity, Long> {

    /**
     * 요청 ID로 조회
     */
    Optional<AuditApiAccessLogEntity> findByRequestId(String requestId);

    /**
     * 사용자 ID로 조회
     */
    Page<AuditApiAccessLogEntity> findByUserId(String userId, Pageable pageable);

    /**
     * 엔드포인트로 조회
     */
    Page<AuditApiAccessLogEntity> findByEndpointContaining(String endpoint, Pageable pageable);

    /**
     * 기간별 조회
     */
    Page<AuditApiAccessLogEntity> findByCreatedAtBetween(Instant start, Instant end, Pageable pageable);

    /**
     * 에러 발생 로그 조회 (4xx, 5xx)
     */
    @Query("SELECT a FROM AuditApiAccessLogEntity a WHERE a.responseStatus >= 400 AND a.createdAt BETWEEN :start AND :end")
    Page<AuditApiAccessLogEntity> findErrorLogs(@Param("start") Instant start, @Param("end") Instant end, Pageable pageable);

    /**
     * 사용자별 요청 수 통계
     */
    @Query("SELECT a.userId, COUNT(a) FROM AuditApiAccessLogEntity a WHERE a.createdAt BETWEEN :start AND :end GROUP BY a.userId ORDER BY COUNT(a) DESC")
    List<Object[]> countByUserIdBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * 엔드포인트별 요청 수 통계
     */
    @Query("SELECT a.endpoint, COUNT(a) FROM AuditApiAccessLogEntity a WHERE a.createdAt BETWEEN :start AND :end GROUP BY a.endpoint ORDER BY COUNT(a) DESC")
    List<Object[]> countByEndpointBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * IP 주소별 요청 수 조회 (보안 모니터링용)
     */
    @Query("SELECT a.clientIp, COUNT(a) FROM AuditApiAccessLogEntity a WHERE a.createdAt >= :since GROUP BY a.clientIp HAVING COUNT(a) >= :threshold ORDER BY COUNT(a) DESC")
    List<Object[]> findSuspiciousIps(@Param("since") Instant since, @Param("threshold") long threshold);
}
