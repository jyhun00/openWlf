package aml.openwlf.data.service;

import aml.openwlf.data.entity.*;
import aml.openwlf.data.entity.AlertEntity.AlertStatus;
import aml.openwlf.data.entity.CaseActivityEntity.ActivityType;
import aml.openwlf.data.entity.CaseCommentEntity.CommentType;
import aml.openwlf.data.entity.CaseEntity.*;
import aml.openwlf.data.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Case Management Service
 * Alert에서 Case로 전환하고, Case의 전체 생명주기를 관리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseService {
    
    private final CaseRepository caseRepository;
    private final CaseAlertRepository caseAlertRepository;
    private final CaseCommentRepository caseCommentRepository;
    private final CaseActivityRepository caseActivityRepository;
    private final AlertRepository alertRepository;
    
    // ==================== Case 생성 ====================
    
    /**
     * Alert에서 Case 생성 (Alert → Case 전환)
     */
    @Transactional
    public CaseEntity createCaseFromAlert(Long alertId, CreateCaseRequest request) {
        AlertEntity alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        
        // 이미 Case에 연결되어 있는지 확인
        if (caseAlertRepository.existsByAlertEntityId(alertId)) {
            Long existingCaseId = caseAlertRepository.findCaseIdByAlertId(alertId)
                    .orElseThrow();
            throw new IllegalStateException(
                    "Alert is already linked to case ID: " + existingCaseId);
        }
        
        // Case 생성
        CaseEntity caseEntity = CaseEntity.builder()
                .caseReference(generateCaseReference())
                .title(request.getTitle() != null ? request.getTitle() : 
                        "Case for " + alert.getCustomerName())
                .description(request.getDescription())
                .status(CaseStatus.OPEN)
                .priority(determinePriority(alert.getScore()))
                .caseType(request.getCaseType() != null ? request.getCaseType() : CaseType.SANCTIONS)
                .customerId(alert.getCustomerId())
                .customerName(alert.getCustomerName())
                .dateOfBirth(alert.getDateOfBirth())
                .nationality(alert.getNationality())
                .riskScore(alert.getScore())
                .assignedTo(request.getAssignedTo())
                .assignedTeam(request.getAssignedTeam())
                .dueDate(calculateDueDate(determinePriority(alert.getScore())))
                .createdBy(request.getCreatedBy())
                .build();
        
        CaseEntity savedCase = caseRepository.save(caseEntity);
        
        // Alert 연결
        linkAlertToCase(savedCase.getId(), alertId, "Initial case creation", request.getCreatedBy());
        
        // Alert 상태 업데이트
        alert.setStatus(AlertStatus.IN_REVIEW);
        alertRepository.save(alert);
        
        // 활동 로그
        logActivity(savedCase, ActivityType.CASE_CREATED, 
                "Case created from Alert: " + alert.getAlertReference(), 
                null, null, request.getCreatedBy());
        
        log.info("Case {} created from Alert {} by {}", 
                savedCase.getCaseReference(), alert.getAlertReference(), request.getCreatedBy());
        
        return savedCase;
    }
    
    /**
     * 여러 Alert를 묶어서 Case 생성
     */
    @Transactional
    public CaseEntity createCaseFromMultipleAlerts(List<Long> alertIds, CreateCaseRequest request) {
        if (alertIds == null || alertIds.isEmpty()) {
            throw new IllegalArgumentException("At least one alert is required");
        }
        
        List<AlertEntity> alerts = alertRepository.findAllById(alertIds);
        if (alerts.isEmpty()) {
            throw new IllegalArgumentException("No valid alerts found");
        }
        
        // 첫 번째 Alert 기준으로 Case 생성
        AlertEntity primaryAlert = alerts.get(0);
        double maxScore = alerts.stream()
                .mapToDouble(AlertEntity::getScore)
                .max()
                .orElse(0.0);
        
        CaseEntity caseEntity = CaseEntity.builder()
                .caseReference(generateCaseReference())
                .title(request.getTitle() != null ? request.getTitle() : 
                        "Consolidated case for " + primaryAlert.getCustomerName())
                .description(request.getDescription())
                .status(CaseStatus.OPEN)
                .priority(determinePriority(maxScore))
                .caseType(request.getCaseType() != null ? request.getCaseType() : CaseType.SANCTIONS)
                .customerId(primaryAlert.getCustomerId())
                .customerName(primaryAlert.getCustomerName())
                .dateOfBirth(primaryAlert.getDateOfBirth())
                .nationality(primaryAlert.getNationality())
                .riskScore(maxScore)
                .assignedTo(request.getAssignedTo())
                .assignedTeam(request.getAssignedTeam())
                .dueDate(calculateDueDate(determinePriority(maxScore)))
                .createdBy(request.getCreatedBy())
                .build();
        
        CaseEntity savedCase = caseRepository.save(caseEntity);
        
        // 모든 Alert 연결
        for (AlertEntity alert : alerts) {
            if (!caseAlertRepository.existsByAlertEntityId(alert.getId())) {
                linkAlertToCase(savedCase.getId(), alert.getId(), 
                        "Consolidated case creation", request.getCreatedBy());
                alert.setStatus(AlertStatus.IN_REVIEW);
                alertRepository.save(alert);
            }
        }
        
        logActivity(savedCase, ActivityType.CASE_CREATED,
                String.format("Case created from %d alerts", alerts.size()),
                null, null, request.getCreatedBy());
        
        log.info("Case {} created from {} alerts by {}",
                savedCase.getCaseReference(), alerts.size(), request.getCreatedBy());
        
        return savedCase;
    }
    
    // ==================== Case 조회 ====================
    
    @Transactional(readOnly = true)
    public Optional<CaseEntity> getCaseById(Long id) {
        return caseRepository.findById(id);
    }
    
    @Transactional(readOnly = true)
    public Optional<CaseEntity> getCaseByReference(String reference) {
        return caseRepository.findByCaseReference(reference);
    }
    
    @Transactional(readOnly = true)
    public Page<CaseEntity> getAllCases(Pageable pageable) {
        return caseRepository.findAll(pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<CaseEntity> getOpenCases(Pageable pageable) {
        List<CaseStatus> openStatuses = List.of(
                CaseStatus.OPEN, CaseStatus.IN_PROGRESS, 
                CaseStatus.PENDING_INFO, CaseStatus.PENDING_REVIEW, CaseStatus.ESCALATED);
        return caseRepository.findByStatusIn(openStatuses, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<CaseEntity> getCasesByAssignee(String assignedTo, Pageable pageable) {
        return caseRepository.findByAssignedTo(assignedTo, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<CaseEntity> searchCases(
            CaseStatus status, CasePriority priority, CaseType caseType,
            String assignedTo, String customerId, Pageable pageable) {
        return caseRepository.searchCases(status, priority, caseType, assignedTo, customerId, pageable);
    }
    
    // ==================== Case 업데이트 ====================
    
    /**
     * Case 상태 변경
     */
    @Transactional
    public Optional<CaseEntity> updateStatus(Long caseId, CaseStatus newStatus, String updatedBy) {
        return caseRepository.findById(caseId)
                .map(caseEntity -> {
                    CaseStatus oldStatus = caseEntity.getStatus();
                    caseEntity.setStatus(newStatus);
                    
                    if (newStatus == CaseStatus.CLOSED) {
                        caseEntity.setClosedAt(LocalDateTime.now());
                        caseEntity.setClosedBy(updatedBy);
                    }
                    
                    logActivity(caseEntity, ActivityType.STATUS_CHANGED,
                            String.format("Status changed from %s to %s", oldStatus, newStatus),
                            oldStatus.name(), newStatus.name(), updatedBy);
                    
                    log.info("Case {} status changed: {} -> {} by {}",
                            caseEntity.getCaseReference(), oldStatus, newStatus, updatedBy);
                    
                    return caseRepository.save(caseEntity);
                });
    }
    
    /**
     * 담당자 배정
     */
    @Transactional
    public Optional<CaseEntity> assignCase(Long caseId, String assignedTo, String assignedBy) {
        return caseRepository.findById(caseId)
                .map(caseEntity -> {
                    String oldAssignee = caseEntity.getAssignedTo();
                    caseEntity.setAssignedTo(assignedTo);
                    
                    if (caseEntity.getStatus() == CaseStatus.OPEN) {
                        caseEntity.setStatus(CaseStatus.IN_PROGRESS);
                    }
                    
                    ActivityType activityType = oldAssignee == null ? 
                            ActivityType.ASSIGNED : ActivityType.REASSIGNED;
                    
                    logActivity(caseEntity, activityType,
                            String.format("Case assigned to %s", assignedTo),
                            oldAssignee, assignedTo, assignedBy);
                    
                    log.info("Case {} assigned to {} by {}",
                            caseEntity.getCaseReference(), assignedTo, assignedBy);
                    
                    return caseRepository.save(caseEntity);
                });
    }
    
    /**
     * 우선순위 변경
     */
    @Transactional
    public Optional<CaseEntity> updatePriority(Long caseId, CasePriority newPriority, String updatedBy) {
        return caseRepository.findById(caseId)
                .map(caseEntity -> {
                    CasePriority oldPriority = caseEntity.getPriority();
                    caseEntity.setPriority(newPriority);
                    caseEntity.setDueDate(calculateDueDate(newPriority));
                    
                    logActivity(caseEntity, ActivityType.PRIORITY_CHANGED,
                            String.format("Priority changed from %s to %s", oldPriority, newPriority),
                            oldPriority.name(), newPriority.name(), updatedBy);
                    
                    return caseRepository.save(caseEntity);
                });
    }
    
    /**
     * 최종 결정
     */
    @Transactional
    public Optional<CaseEntity> makeDecision(Long caseId, CaseDecision decision, 
                                              String rationale, String decidedBy) {
        return caseRepository.findById(caseId)
                .map(caseEntity -> {
                    caseEntity.setDecision(decision);
                    caseEntity.setDecisionRationale(rationale);
                    caseEntity.setStatus(CaseStatus.CLOSED);
                    caseEntity.setClosedAt(LocalDateTime.now());
                    caseEntity.setClosedBy(decidedBy);
                    
                    // 연결된 Alert들 상태 업데이트
                    updateLinkedAlertsStatus(caseEntity.getId(), decision);
                    
                    logActivity(caseEntity, ActivityType.DECISION_MADE,
                            String.format("Decision: %s - %s", decision, rationale),
                            null, decision.name(), decidedBy);
                    
                    // 결정 코멘트 추가
                    addComment(caseId, rationale, CommentType.DECISION, decidedBy);
                    
                    log.info("Case {} decision made: {} by {}",
                            caseEntity.getCaseReference(), decision, decidedBy);
                    
                    return caseRepository.save(caseEntity);
                });
    }
    
    /**
     * SAR 제출 기록
     */
    @Transactional
    public Optional<CaseEntity> fileSar(Long caseId, String sarReference, String filedBy) {
        return caseRepository.findById(caseId)
                .map(caseEntity -> {
                    caseEntity.setSarFiled(true);
                    caseEntity.setSarReference(sarReference);
                    
                    logActivity(caseEntity, ActivityType.SAR_FILED,
                            String.format("SAR filed with reference: %s", sarReference),
                            null, sarReference, filedBy);
                    
                    return caseRepository.save(caseEntity);
                });
    }
    
    // ==================== Alert 연결 관리 ====================
    
    /**
     * Alert를 Case에 연결
     */
    @Transactional
    public void linkAlertToCase(Long caseId, Long alertId, String reason, String linkedBy) {
        CaseEntity caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Case not found: " + caseId));
        
        AlertEntity alertEntity = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        
        if (caseAlertRepository.existsByCaseEntityIdAndAlertEntityId(caseId, alertId)) {
            throw new IllegalStateException("Alert is already linked to this case");
        }
        
        CaseAlertEntity link = CaseAlertEntity.builder()
                .caseEntity(caseEntity)
                .alertEntity(alertEntity)
                .linkReason(reason)
                .linkedBy(linkedBy)
                .build();
        
        caseAlertRepository.save(link);
        
        // 리스크 점수 업데이트 (최대값)
        double maxScore = Math.max(
                caseEntity.getRiskScore() != null ? caseEntity.getRiskScore() : 0,
                alertEntity.getScore());
        caseEntity.setRiskScore(maxScore);
        caseRepository.save(caseEntity);
        
        logActivity(caseEntity, ActivityType.ALERT_LINKED,
                String.format("Alert %s linked: %s", alertEntity.getAlertReference(), reason),
                null, alertEntity.getAlertReference(), linkedBy);
    }
    
    /**
     * Alert 연결 해제
     */
    @Transactional
    public void unlinkAlertFromCase(Long caseId, Long alertId, String unlinkedBy) {
        if (!caseAlertRepository.existsByCaseEntityIdAndAlertEntityId(caseId, alertId)) {
            throw new IllegalStateException("Alert is not linked to this case");
        }
        
        CaseEntity caseEntity = caseRepository.findById(caseId).orElseThrow();
        AlertEntity alertEntity = alertRepository.findById(alertId).orElseThrow();
        
        caseAlertRepository.deleteByCaseEntityIdAndAlertEntityId(caseId, alertId);
        
        logActivity(caseEntity, ActivityType.ALERT_UNLINKED,
                String.format("Alert %s unlinked", alertEntity.getAlertReference()),
                alertEntity.getAlertReference(), null, unlinkedBy);
    }
    
    /**
     * Case에 연결된 Alert 목록
     */
    @Transactional(readOnly = true)
    public List<CaseAlertEntity> getLinkedAlerts(Long caseId) {
        return caseAlertRepository.findByCaseEntityId(caseId);
    }
    
    // ==================== 코멘트 관리 ====================
    
    /**
     * 코멘트 추가
     */
    @Transactional
    public CaseCommentEntity addComment(Long caseId, String content, CommentType type, String createdBy) {
        CaseEntity caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Case not found: " + caseId));
        
        CaseCommentEntity comment = CaseCommentEntity.builder()
                .caseEntity(caseEntity)
                .content(content)
                .commentType(type)
                .createdBy(createdBy)
                .build();
        
        CaseCommentEntity saved = caseCommentRepository.save(comment);
        
        logActivity(caseEntity, ActivityType.COMMENT_ADDED,
                String.format("%s comment added", type),
                null, null, createdBy);
        
        return saved;
    }
    
    /**
     * 코멘트 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CaseCommentEntity> getComments(Long caseId) {
        return caseCommentRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId);
    }
    
    // ==================== 활동 로그 ====================
    
    /**
     * 활동 로그 조회
     */
    @Transactional(readOnly = true)
    public List<CaseActivityEntity> getActivities(Long caseId) {
        return caseActivityRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId);
    }
    
    private void logActivity(CaseEntity caseEntity, ActivityType type, String description,
                            String oldValue, String newValue, String performedBy) {
        CaseActivityEntity activity = CaseActivityEntity.builder()
                .caseEntity(caseEntity)
                .activityType(type)
                .description(description)
                .oldValue(oldValue)
                .newValue(newValue)
                .performedBy(performedBy)
                .build();
        
        caseActivityRepository.save(activity);
    }
    
    // ==================== 통계 ====================
    
    @Transactional(readOnly = true)
    public CaseStats getStatistics() {
        long total = caseRepository.count();
        long open = caseRepository.countByStatus(CaseStatus.OPEN);
        long inProgress = caseRepository.countByStatus(CaseStatus.IN_PROGRESS);
        long pendingInfo = caseRepository.countByStatus(CaseStatus.PENDING_INFO);
        long pendingReview = caseRepository.countByStatus(CaseStatus.PENDING_REVIEW);
        long escalated = caseRepository.countByStatus(CaseStatus.ESCALATED);
        long closed = caseRepository.countByStatus(CaseStatus.CLOSED);
        
        long critical = caseRepository.countByPriority(CasePriority.CRITICAL);
        long high = caseRepository.countByPriority(CasePriority.HIGH);
        
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        long createdToday = caseRepository.countCasesSince(todayStart);
        long closedToday = caseRepository.countClosedCasesSince(todayStart);
        
        return CaseStats.builder()
                .totalCases(total)
                .openCases(open)
                .inProgressCases(inProgress)
                .pendingInfoCases(pendingInfo)
                .pendingReviewCases(pendingReview)
                .escalatedCases(escalated)
                .closedCases(closed)
                .criticalPriorityCases(critical)
                .highPriorityCases(high)
                .casesCreatedToday(createdToday)
                .casesClosedToday(closedToday)
                .build();
    }
    
    // ==================== Helper Methods ====================
    
    private String generateCaseReference() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "CASE-" + timestamp + "-" + uuid;
    }
    
    private CasePriority determinePriority(Double score) {
        if (score == null) return CasePriority.MEDIUM;
        if (score >= 90) return CasePriority.CRITICAL;
        if (score >= 70) return CasePriority.HIGH;
        if (score >= 50) return CasePriority.MEDIUM;
        return CasePriority.LOW;
    }
    
    private LocalDateTime calculateDueDate(CasePriority priority) {
        return switch (priority) {
            case CRITICAL -> LocalDateTime.now().plusDays(1);
            case HIGH -> LocalDateTime.now().plusDays(3);
            case MEDIUM -> LocalDateTime.now().plusDays(7);
            case LOW -> LocalDateTime.now().plusDays(14);
        };
    }
    
    private void updateLinkedAlertsStatus(Long caseId, CaseDecision decision) {
        List<CaseAlertEntity> links = caseAlertRepository.findByCaseEntityId(caseId);
        
        AlertStatus alertStatus = switch (decision) {
            case TRUE_POSITIVE, ESCALATED_TO_LE -> AlertStatus.CONFIRMED;
            case FALSE_POSITIVE, NO_ACTION_REQUIRED -> AlertStatus.FALSE_POSITIVE;
            case INCONCLUSIVE -> AlertStatus.CLOSED;
        };
        
        for (CaseAlertEntity link : links) {
            AlertEntity alert = link.getAlertEntity();
            alert.setStatus(alertStatus);
            alertRepository.save(alert);
        }
    }
    
    // ==================== Request/Response DTOs ====================
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CreateCaseRequest {
        private String title;
        private String description;
        private CaseType caseType;
        private String assignedTo;
        private String assignedTeam;
        private String createdBy;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CaseStats {
        private long totalCases;
        private long openCases;
        private long inProgressCases;
        private long pendingInfoCases;
        private long pendingReviewCases;
        private long escalatedCases;
        private long closedCases;
        private long criticalPriorityCases;
        private long highPriorityCases;
        private long casesCreatedToday;
        private long casesClosedToday;
    }
}
