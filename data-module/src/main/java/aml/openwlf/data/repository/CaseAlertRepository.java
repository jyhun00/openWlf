package aml.openwlf.data.repository;

import aml.openwlf.data.entity.CaseAlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseAlertRepository extends JpaRepository<CaseAlertEntity, Long> {
    
    /**
     * 케이스에 연결된 Alert 목록
     */
    List<CaseAlertEntity> findByCaseEntityId(Long caseId);
    
    /**
     * Alert가 연결된 케이스 목록
     */
    List<CaseAlertEntity> findByAlertEntityId(Long alertId);
    
    /**
     * 특정 케이스-Alert 연결 조회
     */
    Optional<CaseAlertEntity> findByCaseEntityIdAndAlertEntityId(Long caseId, Long alertId);
    
    /**
     * 케이스-Alert 연결 존재 여부
     */
    boolean existsByCaseEntityIdAndAlertEntityId(Long caseId, Long alertId);
    
    /**
     * Alert가 이미 케이스에 연결되어 있는지 확인
     */
    boolean existsByAlertEntityId(Long alertId);
    
    /**
     * Alert가 연결된 케이스 ID 조회
     */
    @Query("SELECT ca.caseEntity.id FROM CaseAlertEntity ca WHERE ca.alertEntity.id = :alertId")
    Optional<Long> findCaseIdByAlertId(@Param("alertId") Long alertId);
    
    /**
     * 케이스에 연결된 Alert 수
     */
    long countByCaseEntityId(Long caseId);
    
    /**
     * 케이스의 연결 삭제
     */
    void deleteByCaseEntityIdAndAlertEntityId(Long caseId, Long alertId);
}
