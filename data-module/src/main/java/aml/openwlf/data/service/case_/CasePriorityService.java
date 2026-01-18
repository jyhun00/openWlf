package aml.openwlf.data.service.case_;

import aml.openwlf.data.config.CasePriorityProperties;
import aml.openwlf.data.entity.CaseEntity.CasePriority;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 케이스 우선순위 및 기한 계산 서비스
 *
 * OOP 원칙: 단일 책임 원칙 (SRP)
 * - 우선순위 결정 로직만 담당
 * - 설정 기반으로 동작하여 OCP 준수
 */
@Service
@RequiredArgsConstructor
public class CasePriorityService {

    private final CasePriorityProperties properties;

    /**
     * 리스크 점수를 기반으로 우선순위 결정
     */
    public CasePriority determinePriority(Double score) {
        if (score == null) {
            return CasePriority.MEDIUM;
        }

        if (score >= properties.getCriticalThreshold()) {
            return CasePriority.CRITICAL;
        }
        if (score >= properties.getHighThreshold()) {
            return CasePriority.HIGH;
        }
        if (score >= properties.getMediumThreshold()) {
            return CasePriority.MEDIUM;
        }
        return CasePriority.LOW;
    }

    /**
     * 우선순위에 따른 기한 계산
     */
    public LocalDateTime calculateDueDate(CasePriority priority) {
        CasePriorityProperties.DueDays dueDays = properties.getDueDays();

        return switch (priority) {
            case CRITICAL -> LocalDateTime.now().plusDays(dueDays.getCritical());
            case HIGH -> LocalDateTime.now().plusDays(dueDays.getHigh());
            case MEDIUM -> LocalDateTime.now().plusDays(dueDays.getMedium());
            case LOW -> LocalDateTime.now().plusDays(dueDays.getLow());
        };
    }

    /**
     * 점수 기반으로 기한 직접 계산
     */
    public LocalDateTime calculateDueDateFromScore(Double score) {
        CasePriority priority = determinePriority(score);
        return calculateDueDate(priority);
    }
}
