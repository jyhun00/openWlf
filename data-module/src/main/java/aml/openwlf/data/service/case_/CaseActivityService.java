package aml.openwlf.data.service.case_;

import aml.openwlf.data.entity.CaseActivityEntity;
import aml.openwlf.data.entity.CaseActivityEntity.ActivityType;
import aml.openwlf.data.entity.CaseEntity;
import aml.openwlf.data.repository.CaseActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 케이스 활동 로그 서비스
 *
 * OOP 원칙: 단일 책임 원칙 (SRP)
 * - 케이스 활동 로그 기록 및 조회만 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseActivityService {

    private final CaseActivityRepository caseActivityRepository;

    /**
     * 활동 로그 기록
     */
    @Transactional
    public CaseActivityEntity logActivity(CaseEntity caseEntity, ActivityType type,
                                           String description, String oldValue,
                                           String newValue, String performedBy) {
        CaseActivityEntity activity = CaseActivityEntity.builder()
                .caseEntity(caseEntity)
                .activityType(type)
                .description(description)
                .oldValue(oldValue)
                .newValue(newValue)
                .performedBy(performedBy)
                .build();

        CaseActivityEntity saved = caseActivityRepository.save(activity);
        log.debug("Activity logged for case {}: {} by {}",
                caseEntity.getCaseReference(), type, performedBy);
        return saved;
    }

    /**
     * 케이스 생성 로그
     */
    @Transactional
    public void logCaseCreated(CaseEntity caseEntity, String alertReference, String createdBy) {
        logActivity(caseEntity, ActivityType.CASE_CREATED,
                "Case created from Alert: " + alertReference,
                null, null, createdBy);
    }

    /**
     * 상태 변경 로그
     */
    @Transactional
    public void logStatusChanged(CaseEntity caseEntity, String oldStatus,
                                  String newStatus, String changedBy) {
        logActivity(caseEntity, ActivityType.STATUS_CHANGED,
                String.format("Status changed from %s to %s", oldStatus, newStatus),
                oldStatus, newStatus, changedBy);
    }

    /**
     * 담당자 배정 로그
     */
    @Transactional
    public void logAssigned(CaseEntity caseEntity, String oldAssignee,
                            String newAssignee, String assignedBy) {
        ActivityType type = oldAssignee == null ? ActivityType.ASSIGNED : ActivityType.REASSIGNED;
        logActivity(caseEntity, type,
                String.format("Case assigned to %s", newAssignee),
                oldAssignee, newAssignee, assignedBy);
    }

    /**
     * 우선순위 변경 로그
     */
    @Transactional
    public void logPriorityChanged(CaseEntity caseEntity, String oldPriority,
                                    String newPriority, String changedBy) {
        logActivity(caseEntity, ActivityType.PRIORITY_CHANGED,
                String.format("Priority changed from %s to %s", oldPriority, newPriority),
                oldPriority, newPriority, changedBy);
    }

    /**
     * 결정 로그
     */
    @Transactional
    public void logDecisionMade(CaseEntity caseEntity, String decision,
                                 String rationale, String decidedBy) {
        logActivity(caseEntity, ActivityType.DECISION_MADE,
                String.format("Decision: %s - %s", decision, rationale),
                null, decision, decidedBy);
    }

    /**
     * Alert 연결 로그
     */
    @Transactional
    public void logAlertLinked(CaseEntity caseEntity, String alertReference,
                                String reason, String linkedBy) {
        logActivity(caseEntity, ActivityType.ALERT_LINKED,
                String.format("Alert %s linked: %s", alertReference, reason),
                null, alertReference, linkedBy);
    }

    /**
     * Alert 연결 해제 로그
     */
    @Transactional
    public void logAlertUnlinked(CaseEntity caseEntity, String alertReference, String unlinkedBy) {
        logActivity(caseEntity, ActivityType.ALERT_UNLINKED,
                String.format("Alert %s unlinked", alertReference),
                alertReference, null, unlinkedBy);
    }

    /**
     * 코멘트 추가 로그
     */
    @Transactional
    public void logCommentAdded(CaseEntity caseEntity, String commentType, String createdBy) {
        logActivity(caseEntity, ActivityType.COMMENT_ADDED,
                String.format("%s comment added", commentType),
                null, null, createdBy);
    }

    /**
     * SAR 제출 로그
     */
    @Transactional
    public void logSarFiled(CaseEntity caseEntity, String sarReference, String filedBy) {
        logActivity(caseEntity, ActivityType.SAR_FILED,
                String.format("SAR filed with reference: %s", sarReference),
                null, sarReference, filedBy);
    }

    /**
     * 활동 로그 조회
     */
    @Transactional(readOnly = true)
    public List<CaseActivityEntity> getActivities(Long caseId) {
        return caseActivityRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId);
    }
}
