package aml.openwlf.data.repository;

import aml.openwlf.data.entity.FilteringHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for filtering history
 */
@Repository
public interface FilteringHistoryRepository extends JpaRepository<FilteringHistoryEntity, Long> {
    
    List<FilteringHistoryEntity> findByCustomerId(String customerId);
    
    List<FilteringHistoryEntity> findByIsAlertTrue();
    
    List<FilteringHistoryEntity> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    List<FilteringHistoryEntity> findByIsAlertTrueAndCreatedAtBetween(
            LocalDateTime start, LocalDateTime end);
}
