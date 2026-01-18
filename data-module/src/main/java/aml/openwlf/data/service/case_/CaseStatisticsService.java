package aml.openwlf.data.service.case_;

import aml.openwlf.data.entity.CaseEntity.CasePriority;
import aml.openwlf.data.entity.CaseEntity.CaseStatus;
import aml.openwlf.data.repository.CaseRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 케이스 통계 서비스
 *
 * OOP 원칙: 단일 책임 원칙 (SRP)
 * - 케이스 통계 계산만 담당
 */
@Service
@RequiredArgsConstructor
public class CaseStatisticsService {

    private final CaseRepository caseRepository;

    /**
     * 전체 케이스 통계 조회
     */
    @Transactional(readOnly = true)
    public CaseStats getStatistics() {
        long total = caseRepository.count();

        // 상태별 카운트
        long open = caseRepository.countByStatus(CaseStatus.OPEN);
        long inProgress = caseRepository.countByStatus(CaseStatus.IN_PROGRESS);
        long pendingInfo = caseRepository.countByStatus(CaseStatus.PENDING_INFO);
        long pendingReview = caseRepository.countByStatus(CaseStatus.PENDING_REVIEW);
        long escalated = caseRepository.countByStatus(CaseStatus.ESCALATED);
        long closed = caseRepository.countByStatus(CaseStatus.CLOSED);

        // 우선순위별 카운트
        long critical = caseRepository.countByPriority(CasePriority.CRITICAL);
        long high = caseRepository.countByPriority(CasePriority.HIGH);

        // 오늘 통계
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

    /**
     * 케이스 통계 DTO
     */
    @Getter
    @Builder
    public static class CaseStats {
        private final long totalCases;
        private final long openCases;
        private final long inProgressCases;
        private final long pendingInfoCases;
        private final long pendingReviewCases;
        private final long escalatedCases;
        private final long closedCases;
        private final long criticalPriorityCases;
        private final long highPriorityCases;
        private final long casesCreatedToday;
        private final long casesClosedToday;
    }
}
