package aml.openwlf.data.repository;

import aml.openwlf.data.entity.CaseEntity;
import aml.openwlf.data.entity.CaseEntity.CasePriority;
import aml.openwlf.data.entity.CaseEntity.CaseStatus;
import aml.openwlf.data.entity.CaseEntity.CaseType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CaseRepository extends JpaRepository<CaseEntity, Long> {
    
    /**
     * 케이스 참조번호로 조회
     */
    Optional<CaseEntity> findByCaseReference(String caseReference);
    
    /**
     * 상태로 조회
     */
    Page<CaseEntity> findByStatus(CaseStatus status, Pageable pageable);
    
    /**
     * 여러 상태로 조회 (열린 케이스)
     */
    Page<CaseEntity> findByStatusIn(List<CaseStatus> statuses, Pageable pageable);
    
    /**
     * 담당자로 조회
     */
    Page<CaseEntity> findByAssignedTo(String assignedTo, Pageable pageable);
    
    /**
     * 담당 팀으로 조회
     */
    Page<CaseEntity> findByAssignedTeam(String assignedTeam, Pageable pageable);
    
    /**
     * 우선순위로 조회
     */
    Page<CaseEntity> findByPriority(CasePriority priority, Pageable pageable);
    
    /**
     * 케이스 유형으로 조회
     */
    Page<CaseEntity> findByCaseType(CaseType caseType, Pageable pageable);
    
    /**
     * 고객 ID로 조회
     */
    List<CaseEntity> findByCustomerId(String customerId);
    
    /**
     * 마감 기한 임박한 케이스 조회
     */
    @Query("SELECT c FROM CaseEntity c WHERE c.status NOT IN :closedStatuses " +
           "AND c.dueDate <= :deadline ORDER BY c.dueDate ASC")
    List<CaseEntity> findCasesDueBefore(
            @Param("closedStatuses") List<CaseStatus> closedStatuses,
            @Param("deadline") LocalDateTime deadline);
    
    /**
     * 복합 검색
     */
    @Query("SELECT c FROM CaseEntity c WHERE " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:priority IS NULL OR c.priority = :priority) AND " +
           "(:caseType IS NULL OR c.caseType = :caseType) AND " +
           "(:assignedTo IS NULL OR c.assignedTo = :assignedTo) AND " +
           "(:customerId IS NULL OR c.customerId = :customerId)")
    Page<CaseEntity> searchCases(
            @Param("status") CaseStatus status,
            @Param("priority") CasePriority priority,
            @Param("caseType") CaseType caseType,
            @Param("assignedTo") String assignedTo,
            @Param("customerId") String customerId,
            Pageable pageable);
    
    /**
     * 상태별 카운트
     */
    long countByStatus(CaseStatus status);
    
    /**
     * 우선순위별 카운트
     */
    long countByPriority(CasePriority priority);
    
    /**
     * 담당자별 열린 케이스 카운트
     */
    @Query("SELECT COUNT(c) FROM CaseEntity c WHERE c.assignedTo = :assignedTo " +
           "AND c.status NOT IN :closedStatuses")
    long countOpenCasesByAssignee(
            @Param("assignedTo") String assignedTo,
            @Param("closedStatuses") List<CaseStatus> closedStatuses);
    
    /**
     * 기간 내 생성된 케이스 카운트
     */
    @Query("SELECT COUNT(c) FROM CaseEntity c WHERE c.createdAt >= :since")
    long countCasesSince(@Param("since") LocalDateTime since);
    
    /**
     * 기간 내 종료된 케이스 카운트
     */
    @Query("SELECT COUNT(c) FROM CaseEntity c WHERE c.closedAt >= :since")
    long countClosedCasesSince(@Param("since") LocalDateTime since);
}
