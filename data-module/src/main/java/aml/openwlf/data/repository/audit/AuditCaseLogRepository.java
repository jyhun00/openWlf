package aml.openwlf.data.repository.audit;

import aml.openwlf.data.entity.audit.AuditCaseLogEntity;
import aml.openwlf.data.entity.audit.enums.CaseActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Case 감사 로그 Repository
 */
@Repository
public interface AuditCaseLogRepository extends JpaRepository<AuditCaseLogEntity, Long> {

    /**
     * Case ID로 모든 이력 조회
     */
    List<AuditCaseLogEntity> findByCaseIdOrderByCreatedAtDesc(Long caseId);

    /**
     * Case 참조번호로 이력 조회
     */
    List<AuditCaseLogEntity> findByCaseReferenceOrderByCreatedAtDesc(String caseReference);

    /**
     * 액션 타입으로 조회
     */
    Page<AuditCaseLogEntity> findByActionType(CaseActionType actionType, Pageable pageable);

    /**
     * 수행자로 조회
     */
    Page<AuditCaseLogEntity> findByPerformedBy(String performedBy, Pageable pageable);

    /**
     * 규제 관련 액션만 조회
     */
    Page<AuditCaseLogEntity> findByIsRegulatoryActionTrue(Pageable pageable);

    /**
     * 기간별 조회
     */
    Page<AuditCaseLogEntity> findByCreatedAtBetween(Instant start, Instant end, Pageable pageable);

    /**
     * SAR 제출 이력 조회
     */
    @Query("SELECT a FROM AuditCaseLogEntity a WHERE a.actionType = 'SAR_FILED' ORDER BY a.createdAt DESC")
    Page<AuditCaseLogEntity> findSarFiledHistory(Pageable pageable);

    /**
     * 결정 이력 조회
     */
    @Query("SELECT a FROM AuditCaseLogEntity a WHERE a.caseId = :caseId AND a.actionType = 'DECISION_MADE' ORDER BY a.createdAt DESC")
    List<AuditCaseLogEntity> findDecisionHistory(@Param("caseId") Long caseId);

    /**
     * 액션 타입별 통계
     */
    @Query("SELECT a.actionType, COUNT(a) FROM AuditCaseLogEntity a WHERE a.createdAt BETWEEN :start AND :end GROUP BY a.actionType")
    List<Object[]> countByActionTypeBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * 팀별 처리 통계
     */
    @Query("SELECT a.performedByTeam, COUNT(a) FROM AuditCaseLogEntity a " +
            "WHERE a.createdAt BETWEEN :start AND :end AND a.performedByTeam IS NOT NULL " +
            "GROUP BY a.performedByTeam ORDER BY COUNT(a) DESC")
    List<Object[]> countByTeamBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * 규제 마감일 임박 Case 이력
     */
    @Query("SELECT a FROM AuditCaseLogEntity a WHERE a.regulatoryDeadline BETWEEN :now AND :deadline ORDER BY a.regulatoryDeadline ASC")
    List<AuditCaseLogEntity> findUpcomingRegulatoryDeadlines(@Param("now") Instant now, @Param("deadline") Instant deadline);
}
