package aml.openwlf.data.service;

import aml.openwlf.data.entity.*;
import aml.openwlf.data.entity.AlertEntity.AlertStatus;
import aml.openwlf.data.entity.CaseActivityEntity.ActivityType;
import aml.openwlf.data.entity.CaseCommentEntity.CommentType;
import aml.openwlf.data.entity.CaseEntity.*;
import aml.openwlf.data.repository.*;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CaseService 테스트")
class CaseServiceTest {

    @Mock
    private CaseRepository caseRepository;
    @Mock
    private CaseAlertRepository caseAlertRepository;
    @Mock
    private CaseCommentRepository caseCommentRepository;
    @Mock
    private CaseActivityRepository caseActivityRepository;
    @Mock
    private AlertRepository alertRepository;

    @InjectMocks
    private CaseService caseService;

    @Captor
    private ArgumentCaptor<CaseEntity> caseCaptor;
    @Captor
    private ArgumentCaptor<CaseActivityEntity> activityCaptor;

    private AlertEntity testAlert;
    private CaseEntity testCase;

    @BeforeEach
    void setUp() {
        testAlert = AlertEntity.builder()
                .id(1L)
                .alertReference("ALT-TEST-001")
                .status(AlertStatus.NEW)
                .customerId("CUST-001")
                .customerName("John Smith")
                .dateOfBirth(LocalDate.of(1985, 5, 15))
                .nationality("US")
                .score(85.0)
                .explanation("Test alert")
                .build();

        testCase = CaseEntity.builder()
                .id(1L)
                .caseReference("CASE-TEST-001")
                .title("Test Case")
                .description("Test case description")
                .status(CaseStatus.OPEN)
                .priority(CasePriority.HIGH)
                .caseType(CaseType.SANCTIONS)
                .customerId("CUST-001")
                .customerName("John Smith")
                .dateOfBirth(LocalDate.of(1985, 5, 15))
                .nationality("US")
                .riskScore(85.0)
                .assignedTo("analyst01")
                .assignedTeam("compliance")
                .createdBy("system")
                .build();
    }

    @Nested
    @DisplayName("Case 생성 테스트")
    class CreateCaseTest {

        @Test
        @DisplayName("Alert에서 Case 생성 성공")
        void shouldCreateCaseFromAlert() {
            // given
            when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
            when(caseAlertRepository.existsByAlertEntityId(1L)).thenReturn(false);
            when(caseAlertRepository.existsByCaseEntityIdAndAlertEntityId(anyLong(), eq(1L))).thenReturn(false);
            when(caseRepository.save(any(CaseEntity.class))).thenAnswer(i -> {
                CaseEntity c = i.getArgument(0);
                c.setId(1L);
                return c;
            });
            when(caseRepository.findById(1L)).thenAnswer(i ->
                    Optional.of(testCase));
            when(caseAlertRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(alertRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(caseActivityRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            CaseService.CreateCaseRequest request = CaseService.CreateCaseRequest.builder()
                    .title("Test Case")
                    .description("Test")
                    .caseType(CaseType.SANCTIONS)
                    .assignedTo("analyst01")
                    .createdBy("system")
                    .build();

            // when
            CaseEntity result = caseService.createCaseFromAlert(1L, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getCustomerName()).isEqualTo("John Smith");
            assertThat(result.getStatus()).isEqualTo(CaseStatus.OPEN);
            assertThat(result.getPriority()).isEqualTo(CasePriority.HIGH);
            assertThat(result.getCaseReference()).startsWith("CASE-");
            verify(alertRepository).save(argThat(a -> a.getStatus() == AlertStatus.IN_REVIEW));
        }

        @Test
        @DisplayName("이미 연결된 Alert로 Case 생성 시 예외")
        void shouldThrowWhenAlertAlreadyLinked() {
            // given
            when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
            when(caseAlertRepository.existsByAlertEntityId(1L)).thenReturn(true);
            when(caseAlertRepository.findCaseIdByAlertId(1L)).thenReturn(Optional.of(99L));

            CaseService.CreateCaseRequest request = CaseService.CreateCaseRequest.builder()
                    .createdBy("system").build();

            // when/then
            assertThatThrownBy(() -> caseService.createCaseFromAlert(1L, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already linked");
        }

        @Test
        @DisplayName("존재하지 않는 Alert로 Case 생성 시 예외")
        void shouldThrowWhenAlertNotFound() {
            // given
            when(alertRepository.findById(999L)).thenReturn(Optional.empty());

            CaseService.CreateCaseRequest request = CaseService.CreateCaseRequest.builder()
                    .createdBy("system").build();

            // when/then
            assertThatThrownBy(() -> caseService.createCaseFromAlert(999L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Alert not found");
        }
    }

    @Nested
    @DisplayName("Case 조회 테스트")
    class GetCaseTest {

        @Test
        @DisplayName("ID로 Case 조회")
        void shouldGetCaseById() {
            when(caseRepository.findById(1L)).thenReturn(Optional.of(testCase));

            Optional<CaseEntity> result = caseService.getCaseById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getCaseReference()).isEqualTo("CASE-TEST-001");
        }

        @Test
        @DisplayName("Reference로 Case 조회")
        void shouldGetCaseByReference() {
            when(caseRepository.findByCaseReference("CASE-TEST-001")).thenReturn(Optional.of(testCase));

            Optional<CaseEntity> result = caseService.getCaseByReference("CASE-TEST-001");

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("Open Case 목록 조회")
        void shouldGetOpenCases() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<CaseEntity> page = new PageImpl<>(List.of(testCase), pageable, 1);
            when(caseRepository.findByStatusIn(anyList(), eq(pageable))).thenReturn(page);

            Page<CaseEntity> result = caseService.getOpenCases(pageable);

            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Case 상태 변경 테스트")
    class UpdateStatusTest {

        @Test
        @DisplayName("상태 업데이트 성공")
        void shouldUpdateStatus() {
            when(caseRepository.findById(1L)).thenReturn(Optional.of(testCase));
            when(caseRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(caseActivityRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Optional<CaseEntity> result = caseService.updateStatus(1L, CaseStatus.IN_PROGRESS, "analyst01");

            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(CaseStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("CLOSED 상태로 변경 시 closedAt/closedBy 설정")
        void shouldSetClosedFieldsWhenClosing() {
            when(caseRepository.findById(1L)).thenReturn(Optional.of(testCase));
            when(caseRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(caseActivityRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Optional<CaseEntity> result = caseService.updateStatus(1L, CaseStatus.CLOSED, "manager01");

            assertThat(result).isPresent();
            assertThat(result.get().getClosedAt()).isNotNull();
            assertThat(result.get().getClosedBy()).isEqualTo("manager01");
        }

        @Test
        @DisplayName("존재하지 않는 Case 상태 변경 시 빈 Optional")
        void shouldReturnEmptyWhenCaseNotFound() {
            when(caseRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<CaseEntity> result = caseService.updateStatus(999L, CaseStatus.IN_PROGRESS, "analyst01");

            assertThat(result).isEmpty();
            verify(caseRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Case 담당자 배정 테스트")
    class AssignCaseTest {

        @Test
        @DisplayName("담당자 배정 성공")
        void shouldAssignCase() {
            when(caseRepository.findById(1L)).thenReturn(Optional.of(testCase));
            when(caseRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(caseActivityRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Optional<CaseEntity> result = caseService.assignCase(1L, "analyst02", "manager01");

            assertThat(result).isPresent();
            assertThat(result.get().getAssignedTo()).isEqualTo("analyst02");
        }

        @Test
        @DisplayName("OPEN 상태 Case 배정 시 IN_PROGRESS로 변경")
        void shouldChangeToInProgressWhenAssigningOpenCase() {
            testCase.setStatus(CaseStatus.OPEN);
            when(caseRepository.findById(1L)).thenReturn(Optional.of(testCase));
            when(caseRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(caseActivityRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Optional<CaseEntity> result = caseService.assignCase(1L, "analyst02", "manager01");

            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(CaseStatus.IN_PROGRESS);
        }
    }

    @Nested
    @DisplayName("Case 최종 결정 테스트")
    class MakeDecisionTest {

        @Test
        @DisplayName("TRUE_POSITIVE 결정 성공")
        void shouldMakeTruePositiveDecision() {
            when(caseRepository.findById(1L)).thenReturn(Optional.of(testCase));
            when(caseRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(caseAlertRepository.findByCaseEntityId(1L)).thenReturn(List.of());
            when(caseActivityRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(caseCommentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Optional<CaseEntity> result = caseService.makeDecision(
                    1L, CaseDecision.TRUE_POSITIVE, "Confirmed match", "analyst01");

            assertThat(result).isPresent();
            CaseEntity decided = result.get();
            assertThat(decided.getDecision()).isEqualTo(CaseDecision.TRUE_POSITIVE);
            assertThat(decided.getDecisionRationale()).isEqualTo("Confirmed match");
            assertThat(decided.getStatus()).isEqualTo(CaseStatus.CLOSED);
            assertThat(decided.getClosedAt()).isNotNull();
            assertThat(decided.getClosedBy()).isEqualTo("analyst01");
        }

        @Test
        @DisplayName("FALSE_POSITIVE 결정 시 연결 Alert 상태 변경")
        void shouldUpdateLinkedAlertsOnFalsePositive() {
            CaseAlertEntity link = CaseAlertEntity.builder()
                    .caseEntity(testCase)
                    .alertEntity(testAlert)
                    .build();
            when(caseRepository.findById(1L)).thenReturn(Optional.of(testCase));
            when(caseRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(caseAlertRepository.findByCaseEntityId(1L)).thenReturn(List.of(link));
            when(alertRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(caseActivityRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(caseCommentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            caseService.makeDecision(1L, CaseDecision.FALSE_POSITIVE, "False positive confirmed", "analyst01");

            verify(alertRepository).save(argThat(a -> a.getStatus() == AlertStatus.FALSE_POSITIVE));
        }

        @Test
        @DisplayName("ESCALATED_TO_LE 결정 시 연결 Alert CONFIRMED 처리")
        void shouldConfirmAlertsOnEscalation() {
            CaseAlertEntity link = CaseAlertEntity.builder()
                    .caseEntity(testCase)
                    .alertEntity(testAlert)
                    .build();
            when(caseRepository.findById(1L)).thenReturn(Optional.of(testCase));
            when(caseRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(caseAlertRepository.findByCaseEntityId(1L)).thenReturn(List.of(link));
            when(alertRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(caseActivityRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(caseCommentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            caseService.makeDecision(1L, CaseDecision.ESCALATED_TO_LE, "Reported to law enforcement", "analyst01");

            verify(alertRepository).save(argThat(a -> a.getStatus() == AlertStatus.CONFIRMED));
        }

        @Test
        @DisplayName("존재하지 않는 Case 결정 시 빈 Optional")
        void shouldReturnEmptyWhenCaseNotFoundForDecision() {
            when(caseRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<CaseEntity> result = caseService.makeDecision(
                    999L, CaseDecision.TRUE_POSITIVE, "test", "analyst01");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("결정 시 코멘트 자동 추가")
        void shouldAddDecisionComment() {
            when(caseRepository.findById(1L)).thenReturn(Optional.of(testCase));
            when(caseRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(caseAlertRepository.findByCaseEntityId(1L)).thenReturn(List.of());
            when(caseActivityRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(caseCommentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            caseService.makeDecision(1L, CaseDecision.TRUE_POSITIVE, "Confirmed match", "analyst01");

            verify(caseCommentRepository).save(argThat(c ->
                    c.getCommentType() == CommentType.DECISION &&
                    c.getContent().equals("Confirmed match")));
        }
    }

    @Nested
    @DisplayName("코멘트 관리 테스트")
    class CommentTest {

        @Test
        @DisplayName("코멘트 추가 성공")
        void shouldAddComment() {
            when(caseRepository.findById(1L)).thenReturn(Optional.of(testCase));
            when(caseCommentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(caseActivityRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            CaseCommentEntity result = caseService.addComment(1L, "Test comment", CommentType.NOTE, "analyst01");

            assertThat(result.getContent()).isEqualTo("Test comment");
            assertThat(result.getCommentType()).isEqualTo(CommentType.NOTE);
        }

        @Test
        @DisplayName("존재하지 않는 Case에 코멘트 추가 시 예외")
        void shouldThrowWhenAddingCommentToNonExistentCase() {
            when(caseRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> caseService.addComment(999L, "Test", CommentType.NOTE, "analyst01"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Case not found");
        }
    }

    @Nested
    @DisplayName("통계 테스트")
    class StatisticsTest {

        @Test
        @DisplayName("통계 조회 성공")
        void shouldGetStatistics() {
            when(caseRepository.count()).thenReturn(100L);
            when(caseRepository.countByStatus(CaseStatus.OPEN)).thenReturn(20L);
            when(caseRepository.countByStatus(CaseStatus.IN_PROGRESS)).thenReturn(30L);
            when(caseRepository.countByStatus(CaseStatus.PENDING_INFO)).thenReturn(5L);
            when(caseRepository.countByStatus(CaseStatus.PENDING_REVIEW)).thenReturn(10L);
            when(caseRepository.countByStatus(CaseStatus.ESCALATED)).thenReturn(3L);
            when(caseRepository.countByStatus(CaseStatus.CLOSED)).thenReturn(32L);
            when(caseRepository.countByPriority(CasePriority.CRITICAL)).thenReturn(5L);
            when(caseRepository.countByPriority(CasePriority.HIGH)).thenReturn(15L);
            when(caseRepository.countCasesSince(any())).thenReturn(8L);
            when(caseRepository.countClosedCasesSince(any())).thenReturn(4L);

            CaseService.CaseStats stats = caseService.getStatistics();

            assertThat(stats.getTotalCases()).isEqualTo(100L);
            assertThat(stats.getOpenCases()).isEqualTo(20L);
            assertThat(stats.getInProgressCases()).isEqualTo(30L);
            assertThat(stats.getClosedCases()).isEqualTo(32L);
            assertThat(stats.getCriticalPriorityCases()).isEqualTo(5L);
        }
    }
}
