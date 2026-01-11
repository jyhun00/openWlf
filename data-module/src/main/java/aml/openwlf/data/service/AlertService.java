package aml.openwlf.data.service;

import aml.openwlf.core.model.FilteringResult;
import aml.openwlf.core.model.MatchedRule;
import aml.openwlf.data.entity.AlertEntity;
import aml.openwlf.data.entity.AlertEntity.AlertStatus;
import aml.openwlf.data.repository.AlertRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing alerts
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {
    
    private final AlertRepository alertRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${watchlist.threshold.alert-generation:50.0}")
    private double alertGenerationThreshold;
    
    /**
     * Create alert from filtering result if score >= threshold
     * @return Optional containing the created alert, or empty if score is below threshold
     */
    @Transactional
    public Optional<AlertEntity> createAlertIfNeeded(FilteringResult result) {
        if (result.getScore() < alertGenerationThreshold) {
            log.debug("Score {} is below alert threshold {}, skipping alert creation", 
                    result.getScore(), alertGenerationThreshold);
            return Optional.empty();
        }
        
        log.info("Creating alert for customer: {} with score: {}", 
                result.getCustomerInfo().getName(), result.getScore());
        
        AlertEntity alert = AlertEntity.builder()
                .alertReference(generateAlertReference())
                .status(AlertStatus.NEW)
                .customerId(result.getCustomerInfo().getCustomerId())
                .customerName(result.getCustomerInfo().getName())
                .dateOfBirth(result.getCustomerInfo().getDateOfBirth())
                .nationality(result.getCustomerInfo().getNationality())
                .score(result.getScore())
                .matchedRules(serializeMatchedRules(result.getMatchedRules()))
                .explanation(result.getExplanation())
                .build();
        
        AlertEntity savedAlert = alertRepository.save(alert);
        log.info("Alert created with reference: {}", savedAlert.getAlertReference());
        
        return Optional.of(savedAlert);
    }
    
    /**
     * Get alert by ID
     */
    @Transactional(readOnly = true)
    public Optional<AlertEntity> getAlertById(Long id) {
        return alertRepository.findById(id);
    }
    
    /**
     * Get alert by reference
     */
    @Transactional(readOnly = true)
    public Optional<AlertEntity> getAlertByReference(String reference) {
        return alertRepository.findByAlertReference(reference);
    }
    
    /**
     * Get all alerts with pagination
     */
    @Transactional(readOnly = true)
    public Page<AlertEntity> getAllAlerts(Pageable pageable) {
        return alertRepository.findAll(pageable);
    }
    
    /**
     * Get alerts by status
     */
    @Transactional(readOnly = true)
    public Page<AlertEntity> getAlertsByStatus(AlertStatus status, Pageable pageable) {
        return alertRepository.findByStatus(status, pageable);
    }
    
    /**
     * Get open alerts (NEW, IN_REVIEW, ESCALATED)
     */
    @Transactional(readOnly = true)
    public Page<AlertEntity> getOpenAlerts(Pageable pageable) {
        List<AlertStatus> openStatuses = List.of(
                AlertStatus.NEW, 
                AlertStatus.IN_REVIEW, 
                AlertStatus.ESCALATED
        );
        return alertRepository.findByStatusIn(openStatuses, pageable);
    }
    
    /**
     * Search alerts with filters
     */
    @Transactional(readOnly = true)
    public Page<AlertEntity> searchAlerts(
            AlertStatus status, 
            String customerId, 
            Double minScore, 
            Pageable pageable) {
        return alertRepository.searchAlerts(status, customerId, minScore, pageable);
    }
    
    /**
     * Update alert status
     */
    @Transactional
    public Optional<AlertEntity> updateStatus(Long alertId, AlertStatus newStatus, String updatedBy) {
        return alertRepository.findById(alertId)
                .map(alert -> {
                    AlertStatus oldStatus = alert.getStatus();
                    alert.setStatus(newStatus);
                    
                    log.info("Alert {} status changed from {} to {} by {}", 
                            alert.getAlertReference(), oldStatus, newStatus, updatedBy);
                    
                    return alertRepository.save(alert);
                });
    }
    
    /**
     * Assign alert to user
     */
    @Transactional
    public Optional<AlertEntity> assignAlert(Long alertId, String assignedTo) {
        return alertRepository.findById(alertId)
                .map(alert -> {
                    alert.setAssignedTo(assignedTo);
                    if (alert.getStatus() == AlertStatus.NEW) {
                        alert.setStatus(AlertStatus.IN_REVIEW);
                    }
                    
                    log.info("Alert {} assigned to {}", alert.getAlertReference(), assignedTo);
                    
                    return alertRepository.save(alert);
                });
    }
    
    /**
     * Resolve alert
     */
    @Transactional
    public Optional<AlertEntity> resolveAlert(
            Long alertId, 
            AlertStatus resolution, 
            String comment, 
            String resolvedBy) {
        
        if (resolution != AlertStatus.CONFIRMED && 
            resolution != AlertStatus.FALSE_POSITIVE && 
            resolution != AlertStatus.CLOSED) {
            throw new IllegalArgumentException("Invalid resolution status: " + resolution);
        }
        
        return alertRepository.findById(alertId)
                .map(alert -> {
                    alert.setStatus(resolution);
                    alert.setResolutionComment(comment);
                    alert.setResolvedAt(LocalDateTime.now());
                    alert.setResolvedBy(resolvedBy);
                    
                    log.info("Alert {} resolved as {} by {}", 
                            alert.getAlertReference(), resolution, resolvedBy);
                    
                    return alertRepository.save(alert);
                });
    }
    
    /**
     * Get alert statistics
     */
    @Transactional(readOnly = true)
    public AlertStats getStatistics() {
        long total = alertRepository.count();
        long newCount = alertRepository.countByStatus(AlertStatus.NEW);
        long inReviewCount = alertRepository.countByStatus(AlertStatus.IN_REVIEW);
        long escalatedCount = alertRepository.countByStatus(AlertStatus.ESCALATED);
        long confirmedCount = alertRepository.countByStatus(AlertStatus.CONFIRMED);
        long falsePositiveCount = alertRepository.countByStatus(AlertStatus.FALSE_POSITIVE);
        long closedCount = alertRepository.countByStatus(AlertStatus.CLOSED);
        long todayCount = alertRepository.countAlertsSince(
                LocalDateTime.now().toLocalDate().atStartOfDay());
        
        return AlertStats.builder()
                .totalAlerts(total)
                .newAlerts(newCount)
                .inReviewAlerts(inReviewCount)
                .escalatedAlerts(escalatedCount)
                .confirmedAlerts(confirmedCount)
                .falsePositiveAlerts(falsePositiveCount)
                .closedAlerts(closedCount)
                .alertsToday(todayCount)
                .openAlerts(newCount + inReviewCount + escalatedCount)
                .build();
    }
    
    /**
     * Generate unique alert reference
     */
    private String generateAlertReference() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ALT-" + timestamp + "-" + uuid;
    }
    
    /**
     * Serialize matched rules to JSON
     */
    private String serializeMatchedRules(List<MatchedRule> matchedRules) {
        try {
            return objectMapper.writeValueAsString(matchedRules);
        } catch (Exception e) {
            log.error("Failed to serialize matched rules", e);
            return "[]";
        }
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AlertStats {
        private long totalAlerts;
        private long newAlerts;
        private long inReviewAlerts;
        private long escalatedAlerts;
        private long confirmedAlerts;
        private long falsePositiveAlerts;
        private long closedAlerts;
        private long alertsToday;
        private long openAlerts;
    }
}
