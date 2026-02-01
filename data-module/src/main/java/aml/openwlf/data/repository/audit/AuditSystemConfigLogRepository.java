package aml.openwlf.data.repository.audit;

import aml.openwlf.data.entity.audit.AuditSystemConfigLogEntity;
import aml.openwlf.data.entity.audit.enums.ConfigCategory;
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
 * 시스템 설정 변경 감사 로그 Repository
 */
@Repository
public interface AuditSystemConfigLogRepository extends JpaRepository<AuditSystemConfigLogEntity, Long> {

    /**
     * 설정 키로 변경 이력 조회
     */
    List<AuditSystemConfigLogEntity> findByConfigKeyOrderByCreatedAtDesc(String configKey);

    /**
     * 설정 카테고리로 조회
     */
    Page<AuditSystemConfigLogEntity> findByConfigCategory(ConfigCategory configCategory, Pageable pageable);

    /**
     * 변경자로 조회
     */
    Page<AuditSystemConfigLogEntity> findByChangedBy(String changedBy, Pageable pageable);

    /**
     * 기간별 조회
     */
    Page<AuditSystemConfigLogEntity> findByCreatedAtBetween(Instant start, Instant end, Pageable pageable);

    /**
     * 특정 설정의 최신 값 조회
     */
    Optional<AuditSystemConfigLogEntity> findFirstByConfigKeyOrderByCreatedAtDesc(String configKey);

    /**
     * 재시작이 필요한 변경 조회
     */
    Page<AuditSystemConfigLogEntity> findByRequiresRestartTrue(Pageable pageable);

    /**
     * 미적용 변경 조회
     */
    Page<AuditSystemConfigLogEntity> findByIsAppliedFalse(Pageable pageable);

    /**
     * 카테고리별 변경 통계
     */
    @Query("SELECT a.configCategory, COUNT(a) FROM AuditSystemConfigLogEntity a " +
            "WHERE a.createdAt BETWEEN :start AND :end GROUP BY a.configCategory")
    List<Object[]> countByCategoryBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * 임계값 설정 변경 이력
     */
    @Query("SELECT a FROM AuditSystemConfigLogEntity a WHERE a.configCategory = 'THRESHOLD' ORDER BY a.createdAt DESC")
    Page<AuditSystemConfigLogEntity> findThresholdChanges(Pageable pageable);

    /**
     * 보안 설정 변경 이력
     */
    @Query("SELECT a FROM AuditSystemConfigLogEntity a WHERE a.configCategory = 'SECURITY' ORDER BY a.createdAt DESC")
    Page<AuditSystemConfigLogEntity> findSecurityChanges(Pageable pageable);

    /**
     * 티켓 참조번호로 조회
     */
    List<AuditSystemConfigLogEntity> findByTicketReference(String ticketReference);

    /**
     * 최근 변경자 목록
     */
    @Query("SELECT DISTINCT a.changedBy FROM AuditSystemConfigLogEntity a WHERE a.createdAt >= :since ORDER BY a.changedBy")
    List<String> findRecentChangers(@Param("since") Instant since);
}
