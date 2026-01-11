package aml.openwlf.batch.service;

import aml.openwlf.batch.dto.SanctionsSyncHistoryDto;
import aml.openwlf.batch.dto.SanctionsSyncStatusDto;
import aml.openwlf.data.entity.SanctionsSyncHistoryEntity;
import aml.openwlf.data.entity.SanctionsSyncHistoryEntity.SyncStatus;
import aml.openwlf.data.repository.SanctionsSyncHistoryRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SanctionsSyncHistoryService 테스트")
class SanctionsSyncHistoryServiceTest {

    @Mock
    private SanctionsSyncHistoryRepository historyRepository;

    @InjectMocks
    private SanctionsSyncHistoryService historyService;

    @Captor
    private ArgumentCaptor<SanctionsSyncHistoryEntity> entityCaptor;

    @Nested
    @DisplayName("startSync() 메서드")
    class StartSync {

        @Test
        @DisplayName("동기화 시작 이력을 생성한다")
        void shouldCreateSyncHistoryOnStart() {
            // given
            SanctionsSyncHistoryEntity savedEntity = SanctionsSyncHistoryEntity.builder()
                    .historyId(1L)
                    .sourceFile("OFAC")
                    .status(SyncStatus.FAIL)
                    .startedAt(LocalDateTime.now())
                    .build();
            
            when(historyRepository.save(any(SanctionsSyncHistoryEntity.class))).thenReturn(savedEntity);

            // when
            SanctionsSyncHistoryEntity result = historyService.startSync("OFAC");

            // then
            verify(historyRepository).save(entityCaptor.capture());
            SanctionsSyncHistoryEntity captured = entityCaptor.getValue();
            
            assertThat(captured.getSourceFile()).isEqualTo("OFAC");
            assertThat(captured.getStatus()).isEqualTo(SyncStatus.FAIL); // 기본값 FAIL
            assertThat(captured.getStartedAt()).isNotNull();
            assertThat(captured.getInsertCount()).isZero();
            assertThat(captured.getUpdateCount()).isZero();
            
            assertThat(result.getHistoryId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("completeSuccess() 메서드")
    class CompleteSuccess {

        @Test
        @DisplayName("동기화 성공 시 이력을 업데이트한다")
        void shouldUpdateHistoryOnSuccess() {
            // given
            Long historyId = 1L;
            LocalDateTime startTime = LocalDateTime.now().minusMinutes(5);
            
            SanctionsSyncHistoryEntity existingEntity = SanctionsSyncHistoryEntity.builder()
                    .historyId(historyId)
                    .sourceFile("OFAC")
                    .status(SyncStatus.FAIL)
                    .startedAt(startTime)
                    .build();
            
            SyncResult syncResult = SyncResult.builder()
                    .insertCount(100)
                    .updateCount(50)
                    .unchangedCount(30)
                    .deactivatedCount(5)
                    .fileSizeBytes(1024000L)
                    .build();
            
            when(historyRepository.findById(historyId)).thenReturn(Optional.of(existingEntity));
            when(historyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            SanctionsSyncHistoryEntity result = historyService.completeSuccess(historyId, syncResult);

            // then
            assertThat(result.getStatus()).isEqualTo(SyncStatus.SUCCESS);
            assertThat(result.getInsertCount()).isEqualTo(100);
            assertThat(result.getUpdateCount()).isEqualTo(50);
            assertThat(result.getUnchangedCount()).isEqualTo(30);
            assertThat(result.getDeactivatedCount()).isEqualTo(5);
            assertThat(result.getFileSizeBytes()).isEqualTo(1024000L);
            assertThat(result.getFinishedAt()).isNotNull();
            assertThat(result.getDurationMs()).isPositive();
        }

        @Test
        @DisplayName("존재하지 않는 이력 ID면 예외를 던진다")
        void shouldThrowExceptionWhenHistoryNotFound() {
            // given
            Long historyId = 999L;
            when(historyRepository.findById(historyId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> historyService.completeSuccess(historyId, SyncResult.builder().build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("History not found");
        }
    }

    @Nested
    @DisplayName("completeFail() 메서드")
    class CompleteFail {

        @Test
        @DisplayName("예외와 함께 실패 이력을 기록한다")
        void shouldRecordFailureWithException() {
            // given
            Long historyId = 1L;
            SanctionsSyncHistoryEntity existingEntity = SanctionsSyncHistoryEntity.builder()
                    .historyId(historyId)
                    .sourceFile("UN")
                    .status(SyncStatus.FAIL)
                    .startedAt(LocalDateTime.now())
                    .build();
            
            Exception exception = new RuntimeException("Connection timeout");
            
            when(historyRepository.findById(historyId)).thenReturn(Optional.of(existingEntity));
            when(historyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            SanctionsSyncHistoryEntity result = historyService.completeFail(historyId, exception);

            // then
            assertThat(result.getStatus()).isEqualTo(SyncStatus.FAIL);
            assertThat(result.getFinishedAt()).isNotNull();
            assertThat(result.getDescription()).contains("Connection timeout");
        }

        @Test
        @DisplayName("에러 메시지와 로그로 실패 이력을 기록한다")
        void shouldRecordFailureWithErrorMessage() {
            // given
            Long historyId = 1L;
            SanctionsSyncHistoryEntity existingEntity = SanctionsSyncHistoryEntity.builder()
                    .historyId(historyId)
                    .sourceFile("UN")
                    .status(SyncStatus.FAIL)
                    .startedAt(LocalDateTime.now())
                    .build();
            
            String errorMessage = "Network error";
            String fullErrorLog = "java.net.SocketException: Network error\n\tat ...";
            
            when(historyRepository.findById(historyId)).thenReturn(Optional.of(existingEntity));
            when(historyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            SanctionsSyncHistoryEntity result = historyService.completeFail(historyId, errorMessage, fullErrorLog);

            // then
            assertThat(result.getStatus()).isEqualTo(SyncStatus.FAIL);
            assertThat(result.getDescription()).isEqualTo(fullErrorLog);
        }
    }

    @Nested
    @DisplayName("getCurrentStatus() 메서드")
    class GetCurrentStatus {

        @Test
        @DisplayName("OFAC과 UN 상태를 모두 반환한다")
        void shouldReturnStatusForBothSources() {
            // given
            SanctionsSyncHistoryEntity ofacHistory = createHistoryEntity("OFAC", SyncStatus.SUCCESS);
            SanctionsSyncHistoryEntity unHistory = createHistoryEntity("UN", SyncStatus.SUCCESS);
            
            when(historyRepository.findTopBySourceFileOrderByStartedAtDesc("OFAC"))
                    .thenReturn(Optional.of(ofacHistory));
            when(historyRepository.findTopBySourceFileOrderByStartedAtDesc("UN"))
                    .thenReturn(Optional.of(unHistory));
            when(historyRepository.findTopBySourceFileAndStatusOrderByStartedAtDesc(anyString(), any()))
                    .thenReturn(Optional.empty());
            when(historyRepository.countFailuresSince(anyString(), any())).thenReturn(0L);
            when(historyRepository.countConsecutiveFailures(anyString(), anyInt())).thenReturn(0);

            // when
            SanctionsSyncStatusDto result = historyService.getCurrentStatus();

            // then
            assertThat(result.getOfac()).isNotNull();
            assertThat(result.getOfac().getSourceFile()).isEqualTo("OFAC");
            assertThat(result.getUn()).isNotNull();
            assertThat(result.getUn().getSourceFile()).isEqualTo("UN");
        }
    }

    @Nested
    @DisplayName("getSourceStatus() 메서드")
    class GetSourceStatus {

        @Test
        @DisplayName("특정 소스의 상태를 조회한다")
        void shouldReturnSourceStatus() {
            // given
            SanctionsSyncHistoryEntity lastSync = createHistoryEntity("OFAC", SyncStatus.SUCCESS);
            lastSync.setTotalProcessed(1000);
            
            when(historyRepository.findTopBySourceFileOrderByStartedAtDesc("OFAC"))
                    .thenReturn(Optional.of(lastSync));
            when(historyRepository.findTopBySourceFileAndStatusOrderByStartedAtDesc("OFAC", SyncStatus.SUCCESS))
                    .thenReturn(Optional.of(lastSync));
            when(historyRepository.countFailuresSince(eq("OFAC"), any())).thenReturn(2L);
            when(historyRepository.countConsecutiveFailures("OFAC", 10)).thenReturn(0);

            // when
            SanctionsSyncStatusDto.SourceSyncStatus status = historyService.getSourceStatus("OFAC");

            // then
            assertThat(status.getSourceFile()).isEqualTo("OFAC");
            assertThat(status.getLastStatus()).isEqualTo("SUCCESS");
            assertThat(status.getLastTotalProcessed()).isEqualTo(1000);
            assertThat(status.getFailureCountLast24h()).isEqualTo(2);
            assertThat(status.getHealthy()).isTrue();
        }

        @Test
        @DisplayName("연속 실패가 있으면 healthy가 false이다")
        void shouldReturnUnhealthyWhenConsecutiveFailures() {
            // given
            SanctionsSyncHistoryEntity lastSync = createHistoryEntity("UN", SyncStatus.FAIL);
            lastSync.setDescription("Error occurred");
            
            when(historyRepository.findTopBySourceFileOrderByStartedAtDesc("UN"))
                    .thenReturn(Optional.of(lastSync));
            when(historyRepository.findTopBySourceFileAndStatusOrderByStartedAtDesc("UN", SyncStatus.SUCCESS))
                    .thenReturn(Optional.empty());
            when(historyRepository.countFailuresSince(eq("UN"), any())).thenReturn(5L);
            when(historyRepository.countConsecutiveFailures("UN", 10)).thenReturn(3);

            // when
            SanctionsSyncStatusDto.SourceSyncStatus status = historyService.getSourceStatus("UN");

            // then
            assertThat(status.getHealthy()).isFalse();
            assertThat(status.getConsecutiveFailures()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("getHistory() 메서드")
    class GetHistory {

        @Test
        @DisplayName("전체 이력을 페이징하여 조회한다")
        void shouldReturnPaginatedHistory() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<SanctionsSyncHistoryEntity> entities = List.of(
                    createHistoryEntity("OFAC", SyncStatus.SUCCESS),
                    createHistoryEntity("UN", SyncStatus.SUCCESS)
            );
            Page<SanctionsSyncHistoryEntity> page = new PageImpl<>(entities, pageable, 2);
            
            when(historyRepository.findAllByOrderByStartedAtDesc(pageable)).thenReturn(page);

            // when
            Page<SanctionsSyncHistoryDto> result = historyService.getHistory(null, null, pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("sourceFile로 필터링하여 조회한다")
        void shouldFilterBySourceFile() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<SanctionsSyncHistoryEntity> entities = List.of(
                    createHistoryEntity("OFAC", SyncStatus.SUCCESS)
            );
            Page<SanctionsSyncHistoryEntity> page = new PageImpl<>(entities, pageable, 1);
            
            when(historyRepository.findBySourceFileOrderByStartedAtDesc("OFAC", pageable)).thenReturn(page);

            // when
            Page<SanctionsSyncHistoryDto> result = historyService.getHistory("OFAC", null, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getSourceFile()).isEqualTo("OFAC");
        }

        @Test
        @DisplayName("status로 필터링하여 조회한다")
        void shouldFilterByStatus() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<SanctionsSyncHistoryEntity> entities = List.of(
                    createHistoryEntity("OFAC", SyncStatus.FAIL)
            );
            Page<SanctionsSyncHistoryEntity> page = new PageImpl<>(entities, pageable, 1);
            
            when(historyRepository.findByStatusOrderByStartedAtDesc(SyncStatus.FAIL, pageable)).thenReturn(page);

            // when
            Page<SanctionsSyncHistoryDto> result = historyService.getHistory(null, "FAIL", pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo("FAIL");
        }

        @Test
        @DisplayName("sourceFile과 status 모두로 필터링한다")
        void shouldFilterByBothSourceFileAndStatus() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<SanctionsSyncHistoryEntity> entities = List.of(
                    createHistoryEntity("UN", SyncStatus.SUCCESS)
            );
            Page<SanctionsSyncHistoryEntity> page = new PageImpl<>(entities, pageable, 1);
            
            when(historyRepository.findBySourceFileAndStatusOrderByStartedAtDesc("UN", SyncStatus.SUCCESS, pageable))
                    .thenReturn(page);

            // when
            Page<SanctionsSyncHistoryDto> result = historyService.getHistory("UN", "SUCCESS", pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getHistoryDetail() 메서드")
    class GetHistoryDetail {

        @Test
        @DisplayName("이력 상세 정보를 조회한다")
        void shouldReturnHistoryDetail() {
            // given
            Long historyId = 1L;
            SanctionsSyncHistoryEntity entity = createHistoryEntity("OFAC", SyncStatus.SUCCESS);
            entity.setHistoryId(historyId);
            entity.setInsertCount(100);
            entity.setDescription("Sync completed");
            
            when(historyRepository.findById(historyId)).thenReturn(Optional.of(entity));

            // when
            SanctionsSyncHistoryDto result = historyService.getHistoryDetail(historyId);

            // then
            assertThat(result.getHistoryId()).isEqualTo(historyId);
            assertThat(result.getSourceFile()).isEqualTo("OFAC");
            assertThat(result.getStatus()).isEqualTo("SUCCESS");
            assertThat(result.getInsertCount()).isEqualTo(100);
        }

        @Test
        @DisplayName("존재하지 않는 이력은 예외를 던진다")
        void shouldThrowExceptionWhenNotFound() {
            // given
            Long historyId = 999L;
            when(historyRepository.findById(historyId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> historyService.getHistoryDetail(historyId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("History not found");
        }
    }

    // ========================================
    // Helper Methods
    // ========================================

    private SanctionsSyncHistoryEntity createHistoryEntity(String sourceFile, SyncStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return SanctionsSyncHistoryEntity.builder()
                .historyId(1L)
                .sourceFile(sourceFile)
                .status(status)
                .startedAt(now.minusMinutes(5))
                .finishedAt(now)
                .durationMs(300000L)
                .insertCount(0)
                .updateCount(0)
                .unchangedCount(0)
                .deactivatedCount(0)
                .totalProcessed(0)
                .build();
    }
}
