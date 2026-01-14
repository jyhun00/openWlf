package aml.openwlf.data.service;

import aml.openwlf.data.entity.AlertEntity.AlertStatus;
import aml.openwlf.data.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Alert 통계 서비스
 *
 * SRP(단일 책임 원칙)를 준수하여 AlertService에서 통계 관련 로직을 분리했습니다.
 * Alert 생성/조회/상태변경은 AlertService가, 통계 계산은 이 서비스가 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertStatisticsService {

    private final AlertRepository alertRepository;

    /**
     * 전체 Alert 통계 조회
     *
     * @return Alert 통계 정보
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
     * 특정 기간 동안의 Alert 통계 조회
     *
     * @param since 시작 시간
     * @return 기간 내 Alert 수
     */
    @Transactional(readOnly = true)
    public long getAlertCountSince(LocalDateTime since) {
        return alertRepository.countAlertsSince(since);
    }

    /**
     * 특정 상태의 Alert 수 조회
     *
     * @param status Alert 상태
     * @return 해당 상태의 Alert 수
     */
    @Transactional(readOnly = true)
    public long getAlertCountByStatus(AlertStatus status) {
        return alertRepository.countByStatus(status);
    }

    /**
     * 열린 Alert (NEW, IN_REVIEW, ESCALATED) 수 조회
     *
     * @return 열린 Alert 수
     */
    @Transactional(readOnly = true)
    public long getOpenAlertCount() {
        return alertRepository.countByStatus(AlertStatus.NEW)
                + alertRepository.countByStatus(AlertStatus.IN_REVIEW)
                + alertRepository.countByStatus(AlertStatus.ESCALATED);
    }

    /**
     * False Positive 비율 계산
     *
     * @return False Positive 비율 (0.0 ~ 1.0)
     */
    @Transactional(readOnly = true)
    public double getFalsePositiveRate() {
        long resolved = alertRepository.countByStatus(AlertStatus.CONFIRMED)
                + alertRepository.countByStatus(AlertStatus.FALSE_POSITIVE);
        if (resolved == 0) {
            return 0.0;
        }
        return (double) alertRepository.countByStatus(AlertStatus.FALSE_POSITIVE) / resolved;
    }

    /**
     * Alert 통계 DTO
     */
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

        /**
         * 해결률 계산 (Closed / Total)
         */
        public double getResolutionRate() {
            if (totalAlerts == 0) return 0.0;
            return (double) closedAlerts / totalAlerts;
        }

        /**
         * False Positive 비율 계산
         */
        public double getFalsePositiveRate() {
            long resolved = confirmedAlerts + falsePositiveAlerts;
            if (resolved == 0) return 0.0;
            return (double) falsePositiveAlerts / resolved;
        }
    }
}
