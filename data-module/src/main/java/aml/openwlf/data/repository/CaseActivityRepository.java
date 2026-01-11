package aml.openwlf.data.repository;

import aml.openwlf.data.entity.CaseActivityEntity;
import aml.openwlf.data.entity.CaseActivityEntity.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CaseActivityRepository extends JpaRepository<CaseActivityEntity, Long> {
    
    /**
     * 케이스의 활동 로그 (페이징)
     */
    Page<CaseActivityEntity> findByCaseEntityIdOrderByCreatedAtDesc(Long caseId, Pageable pageable);
    
    /**
     * 케이스의 모든 활동 로그
     */
    List<CaseActivityEntity> findByCaseEntityIdOrderByCreatedAtDesc(Long caseId);
    
    /**
     * 케이스의 특정 유형 활동 로그
     */
    List<CaseActivityEntity> findByCaseEntityIdAndActivityTypeOrderByCreatedAtDesc(
            Long caseId, ActivityType activityType);
    
    /**
     * 특정 기간의 활동 로그
     */
    List<CaseActivityEntity> findByCaseEntityIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long caseId, LocalDateTime start, LocalDateTime end);
    
    /**
     * 특정 사용자의 활동 로그
     */
    List<CaseActivityEntity> findByPerformedByOrderByCreatedAtDesc(String performedBy);
}
