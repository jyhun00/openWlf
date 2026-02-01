package aml.openwlf.data.repository.audit;

import aml.openwlf.data.entity.audit.AuditAlertLogEntity;
import aml.openwlf.data.entity.audit.enums.AlertActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Alert 감사 로그 Repository
 */
@Repository
public interface AuditAlertLogRepository extends JpaRepository<AuditAlertLogEntity, Long> {

    /**
     * Alert ID로 모든 이력 조회
     */
    List<AuditAlertLogEntity> findByAlertIdOrderByCreatedAtDesc(Long alertId);

    /**
     * Alert 참조번호로 이력 조회
     */
    List<AuditAlertLogEntity> findByAlertReferenceOrderByCreatedAtDesc(String alertReference);

    /**
     * 액션 타입으로 조회
     */
    Page<AuditAlertLogEntity> findByActionType(AlertActionType actionType, Pageable pageable);

    /**
     * 수행자로 조회
     */
    Page<AuditAlertLogEntity> findByPerformedBy(String performedBy, Pageable pageable);

    /**
     * 고객 ID로 관련 Alert 이력 조회
     */
    Page<AuditAlertLogEntity> findByCustomerId(String customerId, Pageable pageable);

    /**
     * 기간별 조회
     */
    Page<AuditAlertLogEntity> findByCreatedAtBetween(Instant start, Instant end, Pageable pageable);

    /**
     * 상태 변경 이력 조회
     */
    @Query("SELECT a FROM AuditAlertLogEntity a WHERE a.alertId = :alertId AND a.actionType = 'STATUS_CHANGED' ORDER BY a.createdAt DESC")
    List<AuditAlertLogEntity> findStatusChangeHistory(@Param("alertId") Long alertId);

    /**
     * 액션 타입별 통계
     */
    @Query("SELECT a.actionType, COUNT(a) FROM AuditAlertLogEntity a WHERE a.createdAt BETWEEN :start AND :end GROUP BY a.actionType")
    List<Object[]> countByActionTypeBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * 수행자별 처리 건수
     */
    @Query("SELECT a.performedBy, COUNT(a) FROM AuditAlertLogEntity a " +
            "WHERE a.createdAt BETWEEN :start AND :end AND a.actionType IN ('RESOLVED', 'STATUS_CHANGED') " +
            "GROUP BY a.performedBy ORDER BY COUNT(a) DESC")
    List<Object[]> countByPerformerBetween(@Param("start") Instant start, @Param("end") Instant end);
}
