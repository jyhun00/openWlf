package aml.openwlf.data.repository;

import aml.openwlf.data.entity.AlertEntity;
import aml.openwlf.data.entity.AlertEntity.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for alerts
 */
@Repository
public interface AlertRepository extends JpaRepository<AlertEntity, Long> {
    
    Optional<AlertEntity> findByAlertReference(String alertReference);
    
    // Status based queries
    List<AlertEntity> findByStatus(AlertStatus status);
    
    Page<AlertEntity> findByStatus(AlertStatus status, Pageable pageable);
    
    List<AlertEntity> findByStatusIn(List<AlertStatus> statuses);
    
    Page<AlertEntity> findByStatusIn(List<AlertStatus> statuses, Pageable pageable);
    
    // Customer based queries
    List<AlertEntity> findByCustomerId(String customerId);
    
    Page<AlertEntity> findByCustomerId(String customerId, Pageable pageable);
    
    // Assignment based queries
    List<AlertEntity> findByAssignedTo(String assignedTo);
    
    Page<AlertEntity> findByAssignedToAndStatus(String assignedTo, AlertStatus status, Pageable pageable);
    
    // Score based queries
    List<AlertEntity> findByScoreGreaterThanEqual(Double score);
    
    Page<AlertEntity> findByScoreGreaterThanEqual(Double score, Pageable pageable);
    
    // Date range queries
    @Query("SELECT a FROM AlertEntity a WHERE a.createdAt BETWEEN :startDate AND :endDate")
    Page<AlertEntity> findByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
    
    // Combined search
    @Query("SELECT a FROM AlertEntity a WHERE " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:customerId IS NULL OR a.customerId = :customerId) AND " +
           "(:minScore IS NULL OR a.score >= :minScore)")
    Page<AlertEntity> searchAlerts(
            @Param("status") AlertStatus status,
            @Param("customerId") String customerId,
            @Param("minScore") Double minScore,
            Pageable pageable);
    
    // Count queries for statistics
    long countByStatus(AlertStatus status);
    
    @Query("SELECT COUNT(a) FROM AlertEntity a WHERE a.status IN :statuses")
    long countByStatusIn(@Param("statuses") List<AlertStatus> statuses);
    
    @Query("SELECT COUNT(a) FROM AlertEntity a WHERE a.createdAt >= :since")
    long countAlertsSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT AVG(a.score) FROM AlertEntity a WHERE a.status = :status")
    Double getAverageScoreByStatus(@Param("status") AlertStatus status);
}
