package aml.openwlf.data.service;

import aml.openwlf.data.entity.AlertEntity.AlertStatus;
import aml.openwlf.data.repository.AlertRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertStatisticsService 테스트")
class AlertStatisticsServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @InjectMocks
    private AlertStatisticsService alertStatisticsService;

    @Nested
    @DisplayName("통계 조회 테스트")
    class GetStatisticsTest {

        @Test
        @DisplayName("전체 Alert 통계 조회")
        void shouldGetAlertStatistics() {
            // given
            when(alertRepository.count()).thenReturn(100L);
            when(alertRepository.countByStatus(AlertStatus.NEW)).thenReturn(20L);
            when(alertRepository.countByStatus(AlertStatus.IN_REVIEW)).thenReturn(15L);
            when(alertRepository.countByStatus(AlertStatus.ESCALATED)).thenReturn(5L);
            when(alertRepository.countByStatus(AlertStatus.CONFIRMED)).thenReturn(30L);
            when(alertRepository.countByStatus(AlertStatus.FALSE_POSITIVE)).thenReturn(25L);
            when(alertRepository.countByStatus(AlertStatus.CLOSED)).thenReturn(5L);
            when(alertRepository.countAlertsSince(any())).thenReturn(10L);

            // when
            AlertStatisticsService.AlertStats stats = alertStatisticsService.getStatistics();

            // then
            assertThat(stats.getTotalAlerts()).isEqualTo(100);
            assertThat(stats.getNewAlerts()).isEqualTo(20);
            assertThat(stats.getInReviewAlerts()).isEqualTo(15);
            assertThat(stats.getEscalatedAlerts()).isEqualTo(5);
            assertThat(stats.getConfirmedAlerts()).isEqualTo(30);
            assertThat(stats.getFalsePositiveAlerts()).isEqualTo(25);
            assertThat(stats.getClosedAlerts()).isEqualTo(5);
            assertThat(stats.getOpenAlerts()).isEqualTo(40); // 20 + 15 + 5
            assertThat(stats.getAlertsToday()).isEqualTo(10);
        }

        @Test
        @DisplayName("Alert가 없는 경우 통계 조회")
        void shouldGetEmptyStatistics() {
            // given
            when(alertRepository.count()).thenReturn(0L);
            when(alertRepository.countByStatus(any())).thenReturn(0L);
            when(alertRepository.countAlertsSince(any())).thenReturn(0L);

            // when
            AlertStatisticsService.AlertStats stats = alertStatisticsService.getStatistics();

            // then
            assertThat(stats.getTotalAlerts()).isEqualTo(0);
            assertThat(stats.getOpenAlerts()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("개별 통계 메서드 테스트")
    class IndividualStatisticsTest {

        @Test
        @DisplayName("특정 상태의 Alert 수 조회")
        void shouldGetAlertCountByStatus() {
            // given
            when(alertRepository.countByStatus(AlertStatus.NEW)).thenReturn(25L);

            // when
            long count = alertStatisticsService.getAlertCountByStatus(AlertStatus.NEW);

            // then
            assertThat(count).isEqualTo(25);
        }

        @Test
        @DisplayName("열린 Alert 수 조회")
        void shouldGetOpenAlertCount() {
            // given
            when(alertRepository.countByStatus(AlertStatus.NEW)).thenReturn(10L);
            when(alertRepository.countByStatus(AlertStatus.IN_REVIEW)).thenReturn(5L);
            when(alertRepository.countByStatus(AlertStatus.ESCALATED)).thenReturn(3L);

            // when
            long count = alertStatisticsService.getOpenAlertCount();

            // then
            assertThat(count).isEqualTo(18); // 10 + 5 + 3
        }
    }

    @Nested
    @DisplayName("비율 계산 테스트")
    class RateCalculationTest {

        @Test
        @DisplayName("False Positive 비율 계산")
        void shouldCalculateFalsePositiveRate() {
            // given
            when(alertRepository.countByStatus(AlertStatus.CONFIRMED)).thenReturn(60L);
            when(alertRepository.countByStatus(AlertStatus.FALSE_POSITIVE)).thenReturn(40L);

            // when
            double rate = alertStatisticsService.getFalsePositiveRate();

            // then
            assertThat(rate).isEqualTo(0.4); // 40 / (60 + 40)
        }

        @Test
        @DisplayName("해결된 Alert가 없는 경우 False Positive 비율")
        void shouldReturnZeroWhenNoResolvedAlerts() {
            // given
            when(alertRepository.countByStatus(AlertStatus.CONFIRMED)).thenReturn(0L);
            when(alertRepository.countByStatus(AlertStatus.FALSE_POSITIVE)).thenReturn(0L);

            // when
            double rate = alertStatisticsService.getFalsePositiveRate();

            // then
            assertThat(rate).isEqualTo(0.0);
        }

        @Test
        @DisplayName("AlertStats 해결률 계산")
        void shouldCalculateResolutionRate() {
            // given
            AlertStatisticsService.AlertStats stats = AlertStatisticsService.AlertStats.builder()
                    .totalAlerts(100)
                    .closedAlerts(25)
                    .build();

            // when
            double rate = stats.getResolutionRate();

            // then
            assertThat(rate).isEqualTo(0.25);
        }

        @Test
        @DisplayName("AlertStats False Positive 비율 계산")
        void shouldCalculateStatsFalsePositiveRate() {
            // given
            AlertStatisticsService.AlertStats stats = AlertStatisticsService.AlertStats.builder()
                    .confirmedAlerts(70)
                    .falsePositiveAlerts(30)
                    .build();

            // when
            double rate = stats.getFalsePositiveRate();

            // then
            assertThat(rate).isEqualTo(0.3); // 30 / (70 + 30)
        }

        @Test
        @DisplayName("총 Alert가 0인 경우 해결률")
        void shouldReturnZeroResolutionRateWhenNoAlerts() {
            // given
            AlertStatisticsService.AlertStats stats = AlertStatisticsService.AlertStats.builder()
                    .totalAlerts(0)
                    .closedAlerts(0)
                    .build();

            // when
            double rate = stats.getResolutionRate();

            // then
            assertThat(rate).isEqualTo(0.0);
        }
    }
}
