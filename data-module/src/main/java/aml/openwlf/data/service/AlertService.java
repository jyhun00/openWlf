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
 *
 * Alert 생성, 조회, 상태 변경 등 핵심 기능을 담당합니다.
 * 통계 관련 기능은 AlertStatisticsService에 위임합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final ObjectMapper objectMapper;
    private final AlertStatisticsService alertStatisticsService;
    
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
     *
     * @deprecated Use {@link AlertStatisticsService#getStatistics()} directly for better SRP compliance
     */
    @Transactional(readOnly = true)
    public AlertStatisticsService.AlertStats getStatistics() {
        return alertStatisticsService.getStatistics();
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
}
