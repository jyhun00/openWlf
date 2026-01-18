package aml.openwlf.data.service.case_;

import aml.openwlf.data.entity.AlertEntity;
import aml.openwlf.data.entity.AlertEntity.AlertStatus;
import aml.openwlf.data.entity.CaseAlertEntity;
import aml.openwlf.data.entity.CaseEntity;
import aml.openwlf.data.entity.CaseEntity.CaseDecision;
import aml.openwlf.data.exception.DuplicateLinkException;
import aml.openwlf.data.exception.EntityNotFoundException;
import aml.openwlf.data.exception.InvalidOperationException;
import aml.openwlf.data.repository.AlertRepository;
import aml.openwlf.data.repository.CaseAlertRepository;
import aml.openwlf.data.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Alert-Case 연결 관리 서비스
 *
 * OOP 원칙: 단일 책임 원칙 (SRP)
 * - Alert와 Case 간의 연결 관리만 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseAlertLinkService {

    private final CaseRepository caseRepository;
    private final AlertRepository alertRepository;
    private final CaseAlertRepository caseAlertRepository;
    private final CaseActivityService caseActivityService;

    /**
     * Alert를 Case에 연결
     */
    @Transactional
    public CaseAlertEntity linkAlertToCase(Long caseId, Long alertId,
                                            String reason, String linkedBy) {
        CaseEntity caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> EntityNotFoundException.caseEntity(caseId));

        AlertEntity alertEntity = alertRepository.findById(alertId)
                .orElseThrow(() -> EntityNotFoundException.alert(alertId));

        // 중복 연결 체크
        if (caseAlertRepository.existsByCaseEntityIdAndAlertEntityId(caseId, alertId)) {
            throw DuplicateLinkException.alertToCase(alertId, caseId);
        }

        CaseAlertEntity link = CaseAlertEntity.builder()
                .caseEntity(caseEntity)
                .alertEntity(alertEntity)
                .linkReason(reason)
                .linkedBy(linkedBy)
                .build();

        CaseAlertEntity saved = caseAlertRepository.save(link);

        // 리스크 점수 업데이트 (최대값)
        updateCaseRiskScore(caseEntity, alertEntity.getScore());

        // 활동 로그
        caseActivityService.logAlertLinked(caseEntity, alertEntity.getAlertReference(),
                reason, linkedBy);

        log.info("Alert {} linked to case {} by {}",
                alertEntity.getAlertReference(), caseEntity.getCaseReference(), linkedBy);

        return saved;
    }

    /**
     * Alert 연결 해제
     */
    @Transactional
    public void unlinkAlertFromCase(Long caseId, Long alertId, String unlinkedBy) {
        if (!caseAlertRepository.existsByCaseEntityIdAndAlertEntityId(caseId, alertId)) {
            throw InvalidOperationException.linkNotExists(alertId, caseId);
        }

        CaseEntity caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> EntityNotFoundException.caseEntity(caseId));
        AlertEntity alertEntity = alertRepository.findById(alertId)
                .orElseThrow(() -> EntityNotFoundException.alert(alertId));

        caseAlertRepository.deleteByCaseEntityIdAndAlertEntityId(caseId, alertId);

        // 활동 로그
        caseActivityService.logAlertUnlinked(caseEntity, alertEntity.getAlertReference(), unlinkedBy);

        log.info("Alert {} unlinked from case {} by {}",
                alertEntity.getAlertReference(), caseEntity.getCaseReference(), unlinkedBy);
    }

    /**
     * Case에 연결된 Alert 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CaseAlertEntity> getLinkedAlerts(Long caseId) {
        return caseAlertRepository.findByCaseEntityId(caseId);
    }

    /**
     * Alert 연결 수 조회
     */
    @Transactional(readOnly = true)
    public long getLinkedAlertCount(Long caseId) {
        return caseAlertRepository.countByCaseEntityId(caseId);
    }

    /**
     * Alert가 이미 Case에 연결되어 있는지 확인
     */
    @Transactional(readOnly = true)
    public boolean isAlertLinkedToAnyCase(Long alertId) {
        return caseAlertRepository.existsByAlertEntityId(alertId);
    }

    /**
     * Alert가 연결된 Case ID 조회
     */
    @Transactional(readOnly = true)
    public Long findCaseIdByAlertId(Long alertId) {
        return caseAlertRepository.findCaseIdByAlertId(alertId).orElse(null);
    }

    /**
     * 결정에 따라 연결된 Alert들의 상태 업데이트
     */
    @Transactional
    public void updateLinkedAlertsStatus(Long caseId, CaseDecision decision) {
        List<CaseAlertEntity> links = caseAlertRepository.findByCaseEntityId(caseId);

        AlertStatus alertStatus = mapDecisionToAlertStatus(decision);

        for (CaseAlertEntity link : links) {
            AlertEntity alert = link.getAlertEntity();
            alert.setStatus(alertStatus);
            alertRepository.save(alert);
        }

        log.info("Updated {} linked alerts to status {} for case {}",
                links.size(), alertStatus, caseId);
    }

    private AlertStatus mapDecisionToAlertStatus(CaseDecision decision) {
        return switch (decision) {
            case TRUE_POSITIVE, ESCALATED_TO_LE -> AlertStatus.CONFIRMED;
            case FALSE_POSITIVE, NO_ACTION_REQUIRED -> AlertStatus.FALSE_POSITIVE;
            case INCONCLUSIVE -> AlertStatus.CLOSED;
        };
    }

    private void updateCaseRiskScore(CaseEntity caseEntity, Double alertScore) {
        double maxScore = Math.max(
                caseEntity.getRiskScore() != null ? caseEntity.getRiskScore() : 0,
                alertScore != null ? alertScore : 0);
        caseEntity.setRiskScore(maxScore);
        caseRepository.save(caseEntity);
    }
}
