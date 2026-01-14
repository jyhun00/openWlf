package aml.openwlf.data.service;

import aml.openwlf.core.model.CustomerInfo;
import aml.openwlf.core.model.FilteringResult;
import aml.openwlf.core.model.MatchedRule;
import aml.openwlf.data.entity.AlertEntity;
import aml.openwlf.data.entity.AlertEntity.AlertStatus;
import aml.openwlf.data.repository.AlertRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertService 테스트")
class AlertServiceTest {
    
    @Mock
    private AlertRepository alertRepository;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @InjectMocks
    private AlertService alertService;
    
    @Captor
    private ArgumentCaptor<AlertEntity> alertCaptor;
    
    private FilteringResult testFilteringResult;
    private CustomerInfo testCustomer;
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(alertService, "alertGenerationThreshold", 50.0);
        
        testCustomer = CustomerInfo.builder()
                .name("John Smith")
                .dateOfBirth(LocalDate.of(1985, 5, 15))
                .nationality("US")
                .customerId("CUST-001")
                .build();
        
        testFilteringResult = FilteringResult.builder()
                .alert(true)
                .score(85.0)
                .matchedRules(List.of(
                        MatchedRule.builder()
                                .ruleName("EXACT_NAME_MATCH")
                                .ruleType("NAME")
                                .score(100.0)
                                .build()
                ))
                .explanation("Alert: High-risk match")
                .customerInfo(testCustomer)
                .build();
    }
    
    @Nested
    @DisplayName("Alert 생성 테스트")
    class CreateAlertTest {
        
        @Test
        @DisplayName("점수가 임계값 이상이면 Alert 생성")
        void shouldCreateAlertWhenScoreAboveThreshold() throws Exception {
            // given
            when(objectMapper.writeValueAsString(any())).thenReturn("[]");
            when(alertRepository.save(any(AlertEntity.class))).thenAnswer(invocation -> {
                AlertEntity alert = invocation.getArgument(0);
                alert.setId(1L);
                return alert;
            });
            
            // when
            Optional<AlertEntity> result = alertService.createAlertIfNeeded(testFilteringResult);
            
            // then
            assertThat(result).isPresent();
            verify(alertRepository).save(alertCaptor.capture());
            
            AlertEntity savedAlert = alertCaptor.getValue();
            assertThat(savedAlert.getCustomerName()).isEqualTo("John Smith");
            assertThat(savedAlert.getScore()).isEqualTo(85.0);
            assertThat(savedAlert.getStatus()).isEqualTo(AlertStatus.NEW);
            assertThat(savedAlert.getAlertReference()).startsWith("ALT-");
        }
        
        @Test
        @DisplayName("점수가 임계값 미만이면 Alert 생성 안함")
        void shouldNotCreateAlertWhenScoreBelowThreshold() {
            // given
            FilteringResult lowScoreResult = FilteringResult.builder()
                    .alert(false)
                    .score(30.0)
                    .matchedRules(List.of())
                    .customerInfo(testCustomer)
                    .build();
            
            // when
            Optional<AlertEntity> result = alertService.createAlertIfNeeded(lowScoreResult);
            
            // then
            assertThat(result).isEmpty();
            verify(alertRepository, never()).save(any());
        }
        
        @Test
        @DisplayName("정확히 임계값인 경우 Alert 생성")
        void shouldCreateAlertAtExactThreshold() throws Exception {
            // given
            FilteringResult exactThresholdResult = FilteringResult.builder()
                    .alert(true)
                    .score(50.0)
                    .matchedRules(List.of())
                    .customerInfo(testCustomer)
                    .build();
            
            when(objectMapper.writeValueAsString(any())).thenReturn("[]");
            when(alertRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            
            // when
            Optional<AlertEntity> result = alertService.createAlertIfNeeded(exactThresholdResult);
            
            // then
            assertThat(result).isPresent();
        }
        
        @Test
        @DisplayName("Alert Reference 형식 검증")
        void shouldGenerateProperAlertReference() throws Exception {
            // given
            when(objectMapper.writeValueAsString(any())).thenReturn("[]");
            when(alertRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            
            // when
            alertService.createAlertIfNeeded(testFilteringResult);
            
            // then
            verify(alertRepository).save(alertCaptor.capture());
            String reference = alertCaptor.getValue().getAlertReference();
            
            assertThat(reference).matches("ALT-\\d{8}-[A-Z0-9]{8}");
        }
    }
    
    @Nested
    @DisplayName("Alert 조회 테스트")
    class GetAlertTest {
        
        @Test
        @DisplayName("ID로 Alert 조회")
        void shouldGetAlertById() {
            // given
            AlertEntity alert = createTestAlert();
            when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
            
            // when
            Optional<AlertEntity> result = alertService.getAlertById(1L);
            
            // then
            assertThat(result).isPresent();
            assertThat(result.get().getAlertReference()).isEqualTo("ALT-TEST-001");
        }
        
        @Test
        @DisplayName("Reference로 Alert 조회")
        void shouldGetAlertByReference() {
            // given
            AlertEntity alert = createTestAlert();
            when(alertRepository.findByAlertReference("ALT-TEST-001")).thenReturn(Optional.of(alert));
            
            // when
            Optional<AlertEntity> result = alertService.getAlertByReference("ALT-TEST-001");
            
            // then
            assertThat(result).isPresent();
        }
        
        @Test
        @DisplayName("Status로 Alert 목록 조회")
        void shouldGetAlertsByStatus() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<AlertEntity> alerts = List.of(createTestAlert());
            Page<AlertEntity> page = new PageImpl<>(alerts, pageable, 1);
            
            when(alertRepository.findByStatus(AlertStatus.NEW, pageable)).thenReturn(page);
            
            // when
            Page<AlertEntity> result = alertService.getAlertsByStatus(AlertStatus.NEW, pageable);
            
            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(AlertStatus.NEW);
        }
        
        @Test
        @DisplayName("Open Alert 조회 (NEW, IN_REVIEW, ESCALATED)")
        void shouldGetOpenAlerts() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<AlertEntity> openAlerts = List.of(
                    createTestAlertWithStatus(AlertStatus.NEW),
                    createTestAlertWithStatus(AlertStatus.IN_REVIEW)
            );
            Page<AlertEntity> page = new PageImpl<>(openAlerts, pageable, 2);
            
            when(alertRepository.findByStatusIn(anyList(), eq(pageable))).thenReturn(page);
            
            // when
            Page<AlertEntity> result = alertService.getOpenAlerts(pageable);
            
            // then
            assertThat(result.getContent()).hasSize(2);
        }
    }
    
    @Nested
    @DisplayName("Alert 상태 변경 테스트")
    class UpdateAlertStatusTest {
        
        @Test
        @DisplayName("Alert 상태 업데이트")
        void shouldUpdateAlertStatus() {
            // given
            AlertEntity alert = createTestAlert();
            when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
            when(alertRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            
            // when
            Optional<AlertEntity> result = alertService.updateStatus(1L, AlertStatus.IN_REVIEW, "analyst1");
            
            // then
            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(AlertStatus.IN_REVIEW);
        }
        
        @Test
        @DisplayName("존재하지 않는 Alert 상태 업데이트 시 빈 Optional 반환")
        void shouldReturnEmptyWhenAlertNotFound() {
            // given
            when(alertRepository.findById(999L)).thenReturn(Optional.empty());
            
            // when
            Optional<AlertEntity> result = alertService.updateStatus(999L, AlertStatus.IN_REVIEW, "analyst1");
            
            // then
            assertThat(result).isEmpty();
            verify(alertRepository, never()).save(any());
        }
    }
    
    @Nested
    @DisplayName("Alert 할당 테스트")
    class AssignAlertTest {
        
        @Test
        @DisplayName("Alert 담당자 할당")
        void shouldAssignAlert() {
            // given
            AlertEntity alert = createTestAlert();
            when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
            when(alertRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            
            // when
            Optional<AlertEntity> result = alertService.assignAlert(1L, "analyst@company.com");
            
            // then
            assertThat(result).isPresent();
            assertThat(result.get().getAssignedTo()).isEqualTo("analyst@company.com");
        }
        
        @Test
        @DisplayName("NEW 상태 Alert 할당 시 IN_REVIEW로 변경")
        void shouldChangeStatusToInReviewWhenAssigningNewAlert() {
            // given
            AlertEntity alert = createTestAlertWithStatus(AlertStatus.NEW);
            when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
            when(alertRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            
            // when
            Optional<AlertEntity> result = alertService.assignAlert(1L, "analyst@company.com");
            
            // then
            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(AlertStatus.IN_REVIEW);
        }
    }
    
    @Nested
    @DisplayName("Alert 해결 테스트")
    class ResolveAlertTest {
        
        @Test
        @DisplayName("Alert를 CONFIRMED로 해결")
        void shouldResolveAlertAsConfirmed() {
            // given
            AlertEntity alert = createTestAlertWithStatus(AlertStatus.IN_REVIEW);
            when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
            when(alertRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            
            // when
            Optional<AlertEntity> result = alertService.resolveAlert(
                    1L, AlertStatus.CONFIRMED, "Verified against sanctions list", "analyst1");
            
            // then
            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(AlertStatus.CONFIRMED);
            assertThat(result.get().getResolutionComment()).isEqualTo("Verified against sanctions list");
            assertThat(result.get().getResolvedBy()).isEqualTo("analyst1");
            assertThat(result.get().getResolvedAt()).isNotNull();
        }
        
        @Test
        @DisplayName("Alert를 FALSE_POSITIVE로 해결")
        void shouldResolveAlertAsFalsePositive() {
            // given
            AlertEntity alert = createTestAlertWithStatus(AlertStatus.IN_REVIEW);
            when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
            when(alertRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            
            // when
            Optional<AlertEntity> result = alertService.resolveAlert(
                    1L, AlertStatus.FALSE_POSITIVE, "Name similarity but different person", "analyst1");
            
            // then
            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(AlertStatus.FALSE_POSITIVE);
        }
        
        @Test
        @DisplayName("유효하지 않은 해결 상태로 해결 시도 시 예외 발생")
        void shouldThrowExceptionForInvalidResolutionStatus() {
            // when/then
            assertThatThrownBy(() -> alertService.resolveAlert(
                    1L, AlertStatus.IN_REVIEW, "Invalid", "analyst1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid resolution status");
        }
    }
    
    private AlertEntity createTestAlert() {
        return AlertEntity.builder()
                .id(1L)
                .alertReference("ALT-TEST-001")
                .status(AlertStatus.NEW)
                .customerId("CUST-001")
                .customerName("John Smith")
                .score(85.0)
                .explanation("Test alert")
                .build();
    }
    
    private AlertEntity createTestAlertWithStatus(AlertStatus status) {
        AlertEntity alert = createTestAlert();
        alert.setStatus(status);
        return alert;
    }
}
