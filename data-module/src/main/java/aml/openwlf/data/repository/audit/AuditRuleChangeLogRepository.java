package aml.openwlf.data.repository.audit;

import aml.openwlf.data.entity.audit.AuditRuleChangeLogEntity;
import aml.openwlf.data.entity.audit.enums.RuleChangeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * 규칙 변경 감사 로그 Repository
 */
@Repository
public interface AuditRuleChangeLogRepository extends JpaRepository<AuditRuleChangeLogEntity, Long> {

    /**
     * 규칙 ID로 변경 이력 조회
     */
    List<AuditRuleChangeLogEntity> findByRuleIdOrderByCreatedAtDesc(String ruleId);

    /**
     * 변경 타입으로 조회
     */
    Page<AuditRuleChangeLogEntity> findByChangeType(RuleChangeType changeType, Pageable pageable);

    /**
     * 변경자로 조회
     */
    Page<AuditRuleChangeLogEntity> findByChangedBy(String changedBy, Pageable pageable);

    /**
     * 승인자로 조회
     */
    Page<AuditRuleChangeLogEntity> findByApprovedBy(String approvedBy, Pageable pageable);

    /**
     * 기간별 조회
     */
    Page<AuditRuleChangeLogEntity> findByCreatedAtBetween(Instant start, Instant end, Pageable pageable);

    /**
     * 티켓 참조번호로 조회
     */
    List<AuditRuleChangeLogEntity> findByTicketReference(String ticketReference);

    /**
     * 규칙 타입별 변경 통계
     */
    @Query("SELECT a.ruleType, a.changeType, COUNT(a) FROM AuditRuleChangeLogEntity a " +
            "WHERE a.createdAt BETWEEN :start AND :end GROUP BY a.ruleType, a.changeType")
    List<Object[]> countByRuleTypeAndChangeTypeBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * 최근 설정 재로드 이력
     */
    @Query("SELECT a FROM AuditRuleChangeLogEntity a WHERE a.changeType = 'CONFIG_RELOADED' ORDER BY a.createdAt DESC")
    Page<AuditRuleChangeLogEntity> findRecentConfigReloads(Pageable pageable);

    /**
     * 승인 대기 중인 변경 (approvedBy가 null)
     */
    @Query("SELECT a FROM AuditRuleChangeLogEntity a WHERE a.approvedBy IS NULL ORDER BY a.createdAt DESC")
    List<AuditRuleChangeLogEntity> findPendingApprovals();

    /**
     * 임계값 변경 이력
     */
    @Query("SELECT a FROM AuditRuleChangeLogEntity a WHERE a.changeType = 'THRESHOLD_CHANGED' ORDER BY a.createdAt DESC")
    Page<AuditRuleChangeLogEntity> findThresholdChangeHistory(Pageable pageable);
}
