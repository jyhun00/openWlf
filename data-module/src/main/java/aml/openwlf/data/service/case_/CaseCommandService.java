package aml.openwlf.data.service.case_;

import aml.openwlf.data.entity.AlertEntity;
import aml.openwlf.data.entity.AlertEntity.AlertStatus;
import aml.openwlf.data.entity.CaseEntity;
import aml.openwlf.data.entity.CaseEntity.*;
import aml.openwlf.data.exception.EntityNotFoundException;
import aml.openwlf.data.exception.InvalidOperationException;
import aml.openwlf.data.repository.AlertRepository;
import aml.openwlf.data.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 케이스 커맨드 서비스 (생성/수정)
 *
 * OOP 원칙: 단일 책임 원칙 (SRP) + CQRS 패턴
 * - 케이스 생성 및 수정(Command) 작업만 담당
 * - 조회는 CaseQueryService가 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseCommandService {

    private final CaseRepository caseRepository;
    private final AlertRepository alertRepository;
    private final CasePriorityService casePriorityService;
    private final CaseActivityService caseActivityService;
    private final CaseAlertLinkService caseAlertLinkService;
    private final CaseCommentService caseCommentService;

    /**
     * Alert에서 Case 생성
     */
    @Transactional
    public CaseEntity createCaseFromAlert(Long alertId, CreateCaseCommand command) {
        AlertEntity alert = alertRepository.findById(alertId)
                .orElseThrow(() -> EntityNotFoundException.alert(alertId));

        // 이미 Case에 연결되어 있는지 확인
        if (caseAlertLinkService.isAlertLinkedToAnyCase(alertId)) {
            Long existingCaseId = caseAlertLinkService.findCaseIdByAlertId(alertId);
            throw InvalidOperationException.alertAlreadyLinked(alertId, existingCaseId);
        }

        CasePriority priority = casePriorityService.determinePriority(alert.getScore());

        CaseEntity caseEntity = CaseEntity.builder()
                .caseReference(generateCaseReference())
                .title(command.title() != null ? command.title() : "Case for " + alert.getCustomerName())
                .description(command.description())
                .status(CaseStatus.OPEN)
                .priority(priority)
                .caseType(command.caseType() != null ? command.caseType() : CaseType.SANCTIONS)
                .customerId(alert.getCustomerId())
                .customerName(alert.getCustomerName())
                .dateOfBirth(alert.getDateOfBirth())
                .nationality(alert.getNationality())
                .riskScore(alert.getScore())
                .assignedTo(command.assignedTo())
                .assignedTeam(command.assignedTeam())
                .dueDate(casePriorityService.calculateDueDate(priority))
                .createdBy(command.createdBy())
                .build();

        CaseEntity savedCase = caseRepository.save(caseEntity);

        // Alert 연결
        caseAlertLinkService.linkAlertToCase(savedCase.getId(), alertId,
                "Initial case creation", command.createdBy());

        // Alert 상태 업데이트
        alert.setStatus(AlertStatus.IN_REVIEW);
        alertRepository.save(alert);

        // 활동 로그
        caseActivityService.logCaseCreated(savedCase, alert.getAlertReference(), command.createdBy());

        log.info("Case {} created from Alert {} by {}",
                savedCase.getCaseReference(), alert.getAlertReference(), command.createdBy());

        return savedCase;
    }

    /**
     * 여러 Alert를 묶어서 Case 생성
     */
    @Transactional
    public CaseEntity createCaseFromMultipleAlerts(List<Long> alertIds, CreateCaseCommand command) {
        if (alertIds == null || alertIds.isEmpty()) {
            throw InvalidOperationException.noAlertsProvided();
        }

        List<AlertEntity> alerts = alertRepository.findAllById(alertIds);
        if (alerts.isEmpty()) {
            throw InvalidOperationException.noAlertsProvided();
        }

        AlertEntity primaryAlert = alerts.get(0);
        double maxScore = alerts.stream()
                .mapToDouble(AlertEntity::getScore)
                .max()
                .orElse(0.0);

        CasePriority priority = casePriorityService.determinePriority(maxScore);

        CaseEntity caseEntity = CaseEntity.builder()
                .caseReference(generateCaseReference())
                .title(command.title() != null ? command.title() :
                        "Consolidated case for " + primaryAlert.getCustomerName())
                .description(command.description())
                .status(CaseStatus.OPEN)
                .priority(priority)
                .caseType(command.caseType() != null ? command.caseType() : CaseType.SANCTIONS)
                .customerId(primaryAlert.getCustomerId())
                .customerName(primaryAlert.getCustomerName())
                .dateOfBirth(primaryAlert.getDateOfBirth())
                .nationality(primaryAlert.getNationality())
                .riskScore(maxScore)
                .assignedTo(command.assignedTo())
                .assignedTeam(command.assignedTeam())
                .dueDate(casePriorityService.calculateDueDate(priority))
                .createdBy(command.createdBy())
                .build();

        CaseEntity savedCase = caseRepository.save(caseEntity);

        // 모든 Alert 연결
        for (AlertEntity alert : alerts) {
            if (!caseAlertLinkService.isAlertLinkedToAnyCase(alert.getId())) {
                caseAlertLinkService.linkAlertToCase(savedCase.getId(), alert.getId(),
                        "Consolidated case creation", command.createdBy());
                alert.setStatus(AlertStatus.IN_REVIEW);
                alertRepository.save(alert);
            }
        }

        // 활동 로그
        caseActivityService.logActivity(savedCase,
                aml.openwlf.data.entity.CaseActivityEntity.ActivityType.CASE_CREATED,
                String.format("Case created from %d alerts", alerts.size()),
                null, null, command.createdBy());

        log.info("Case {} created from {} alerts by {}",
                savedCase.getCaseReference(), alerts.size(), command.createdBy());

        return savedCase;
    }

    /**
     * Case 상태 변경
     */
    @Transactional
    public CaseEntity updateStatus(Long caseId, CaseStatus newStatus, String updatedBy) {
        CaseEntity caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> EntityNotFoundException.caseEntity(caseId));

        CaseStatus oldStatus = caseEntity.getStatus();
        caseEntity.setStatus(newStatus);

        if (newStatus == CaseStatus.CLOSED) {
            caseEntity.setClosedAt(LocalDateTime.now());
            caseEntity.setClosedBy(updatedBy);
        }

        CaseEntity saved = caseRepository.save(caseEntity);

        caseActivityService.logStatusChanged(caseEntity, oldStatus.name(), newStatus.name(), updatedBy);

        log.info("Case {} status changed: {} -> {} by {}",
                caseEntity.getCaseReference(), oldStatus, newStatus, updatedBy);

        return saved;
    }

    /**
     * 담당자 배정
     */
    @Transactional
    public CaseEntity assignCase(Long caseId, String assignedTo, String assignedBy) {
        CaseEntity caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> EntityNotFoundException.caseEntity(caseId));

        String oldAssignee = caseEntity.getAssignedTo();
        caseEntity.setAssignedTo(assignedTo);

        if (caseEntity.getStatus() == CaseStatus.OPEN) {
            caseEntity.setStatus(CaseStatus.IN_PROGRESS);
        }

        CaseEntity saved = caseRepository.save(caseEntity);

        caseActivityService.logAssigned(caseEntity, oldAssignee, assignedTo, assignedBy);

        log.info("Case {} assigned to {} by {}",
                caseEntity.getCaseReference(), assignedTo, assignedBy);

        return saved;
    }

    /**
     * 우선순위 변경
     */
    @Transactional
    public CaseEntity updatePriority(Long caseId, CasePriority newPriority, String updatedBy) {
        CaseEntity caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> EntityNotFoundException.caseEntity(caseId));

        CasePriority oldPriority = caseEntity.getPriority();
        caseEntity.setPriority(newPriority);
        caseEntity.setDueDate(casePriorityService.calculateDueDate(newPriority));

        CaseEntity saved = caseRepository.save(caseEntity);

        caseActivityService.logPriorityChanged(caseEntity,
                oldPriority.name(), newPriority.name(), updatedBy);

        return saved;
    }

    /**
     * 최종 결정
     */
    @Transactional
    public CaseEntity makeDecision(Long caseId, CaseDecision decision,
                                    String rationale, String decidedBy) {
        CaseEntity caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> EntityNotFoundException.caseEntity(caseId));

        caseEntity.setDecision(decision);
        caseEntity.setDecisionRationale(rationale);
        caseEntity.setStatus(CaseStatus.CLOSED);
        caseEntity.setClosedAt(LocalDateTime.now());
        caseEntity.setClosedBy(decidedBy);

        CaseEntity saved = caseRepository.save(caseEntity);

        // 연결된 Alert들 상태 업데이트
        caseAlertLinkService.updateLinkedAlertsStatus(caseId, decision);

        // 활동 로그
        caseActivityService.logDecisionMade(caseEntity, decision.name(), rationale, decidedBy);

        // 결정 코멘트 추가
        caseCommentService.addDecisionComment(caseEntity, rationale, decidedBy);

        log.info("Case {} decision made: {} by {}",
                caseEntity.getCaseReference(), decision, decidedBy);

        return saved;
    }

    /**
     * SAR 제출 기록
     */
    @Transactional
    public CaseEntity fileSar(Long caseId, String sarReference, String filedBy) {
        CaseEntity caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> EntityNotFoundException.caseEntity(caseId));

        caseEntity.setSarFiled(true);
        caseEntity.setSarReference(sarReference);

        CaseEntity saved = caseRepository.save(caseEntity);

        caseActivityService.logSarFiled(caseEntity, sarReference, filedBy);

        return saved;
    }

    private String generateCaseReference() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "CASE-" + timestamp + "-" + uuid;
    }

    /**
     * 케이스 생성 커맨드 (불변 레코드)
     */
    public record CreateCaseCommand(
            String title,
            String description,
            CaseType caseType,
            String assignedTo,
            String assignedTeam,
            String createdBy
    ) {
        public static CreateCaseCommand from(
                String title, String description, CaseType caseType,
                String assignedTo, String assignedTeam, String createdBy) {
            return new CreateCaseCommand(title, description, caseType,
                    assignedTo, assignedTeam, createdBy);
        }
    }
}
