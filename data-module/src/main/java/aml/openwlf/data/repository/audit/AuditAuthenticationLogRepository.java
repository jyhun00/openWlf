package aml.openwlf.data.repository.audit;

import aml.openwlf.data.entity.audit.AuditAuthenticationLogEntity;
import aml.openwlf.data.entity.audit.enums.AuthEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * 인증 감사 로그 Repository
 */
@Repository
public interface AuditAuthenticationLogRepository extends JpaRepository<AuditAuthenticationLogEntity, Long> {

    /**
     * 사용자 ID로 조회
     */
    Page<AuditAuthenticationLogEntity> findByUserId(String userId, Pageable pageable);

    /**
     * 이벤트 타입으로 조회
     */
    Page<AuditAuthenticationLogEntity> findByEventType(AuthEventType eventType, Pageable pageable);

    /**
     * 로그인 실패 로그 조회
     */
    Page<AuditAuthenticationLogEntity> findByIsSuccessFalse(Pageable pageable);

    /**
     * 기간별 인증 이벤트 조회
     */
    Page<AuditAuthenticationLogEntity> findByCreatedAtBetween(Instant start, Instant end, Pageable pageable);

    /**
     * IP별 로그인 실패 횟수 (브루트포스 탐지)
     */
    @Query("SELECT a.clientIp, COUNT(a) FROM AuditAuthenticationLogEntity a " +
            "WHERE a.eventType = 'LOGIN_FAILURE' AND a.createdAt >= :since " +
            "GROUP BY a.clientIp HAVING COUNT(a) >= :threshold ORDER BY COUNT(a) DESC")
    List<Object[]> findBruteForceAttempts(@Param("since") Instant since, @Param("threshold") long threshold);

    /**
     * 사용자별 로그인 실패 횟수 (계정 잠금 체크)
     */
    @Query("SELECT COUNT(a) FROM AuditAuthenticationLogEntity a " +
            "WHERE a.userId = :userId AND a.eventType = 'LOGIN_FAILURE' AND a.createdAt >= :since")
    long countFailedLoginAttempts(@Param("userId") String userId, @Param("since") Instant since);

    /**
     * 비정상 로그인 위치 탐지 (다른 지역에서 로그인)
     */
    @Query("SELECT DISTINCT a.geoLocation FROM AuditAuthenticationLogEntity a " +
            "WHERE a.userId = :userId AND a.eventType = 'LOGIN_SUCCESS' AND a.createdAt >= :since")
    List<String> findLoginLocations(@Param("userId") String userId, @Param("since") Instant since);

    /**
     * 이벤트 타입별 통계
     */
    @Query("SELECT a.eventType, COUNT(a) FROM AuditAuthenticationLogEntity a " +
            "WHERE a.createdAt BETWEEN :start AND :end GROUP BY a.eventType")
    List<Object[]> countByEventTypeBetween(@Param("start") Instant start, @Param("end") Instant end);
}
