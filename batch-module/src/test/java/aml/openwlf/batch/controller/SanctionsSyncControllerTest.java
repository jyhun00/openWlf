package aml.openwlf.batch.controller;

import aml.openwlf.batch.dto.SanctionsSyncHistoryDto;
import aml.openwlf.batch.dto.SanctionsSyncStatusDto;
import aml.openwlf.batch.dto.SanctionsSyncStatusDto.SourceSyncStatus;
import aml.openwlf.batch.service.SanctionsSyncHistoryService;
import aml.openwlf.batch.service.SanctionsSyncService;
import aml.openwlf.batch.service.SyncResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SanctionsSyncController 테스트")
class SanctionsSyncControllerTest {

    @Mock
    private SanctionsSyncService sanctionsSyncService;

    @Mock
    private SanctionsSyncHistoryService historyService;

    @InjectMocks
    private SanctionsSyncController controller;

    @Nested
    @DisplayName("POST /api/v1/admin/sanctions-sync/all")
    class SyncAll {

        @Test
        @DisplayName("전체 동기화 성공")
        void shouldSyncAllSuccessfully() {
            // given
            List<SyncResult> results = List.of(
                    SyncResult.success("OFAC", 100, 50, 30, 5),
                    SyncResult.success("UN", 200, 100, 50, 10)
            );
            when(sanctionsSyncService.syncAll()).thenReturn(results);

            // when
            ResponseEntity<List<SyncResult>> response = controller.syncAll();

            // then
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).hasSize(2);
            assertThat(response.getBody().get(0).getSourceFile()).isEqualTo("OFAC");
            assertThat(response.getBody().get(0).isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/sanctions-sync/ofac")
    class SyncOfac {

        @Test
        @DisplayName("OFAC 동기화 성공")
        void shouldSyncOfacSuccessfully() {
            // given
            SyncResult result = SyncResult.success("OFAC", 100, 50, 30, 5);
            when(sanctionsSyncService.syncOfac()).thenReturn(result);

            // when
            ResponseEntity<SyncResult> response = controller.syncOfac();

            // then
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().getSourceFile()).isEqualTo("OFAC");
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getInsertCount()).isEqualTo(100);
        }

        @Test
        @DisplayName("OFAC 동기화 실패")
        void shouldReturnFailureResult() {
            // given
            SyncResult result = SyncResult.failed("OFAC", "Connection timeout");
            when(sanctionsSyncService.syncOfac()).thenReturn(result);

            // when
            ResponseEntity<SyncResult> response = controller.syncOfac();

            // then
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getErrorMessage()).isEqualTo("Connection timeout");
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/sanctions-sync/un")
    class SyncUn {

        @Test
        @DisplayName("UN 동기화 성공")
        void shouldSyncUnSuccessfully() {
            // given
            SyncResult result = SyncResult.success("UN", 200, 100, 50, 10);
            when(sanctionsSyncService.syncUn()).thenReturn(result);

            // when
            ResponseEntity<SyncResult> response = controller.syncUn();

            // then
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().getSourceFile()).isEqualTo("UN");
            assertThat(response.getBody().getInsertCount()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/sanctions-sync/status")
    class GetStatus {

        @Test
        @DisplayName("동기화 상태 조회")
        void shouldReturnSyncStatus() {
            // given
            SanctionsSyncStatusDto status = SanctionsSyncStatusDto.builder()
                    .ofac(SourceSyncStatus.builder()
                            .sourceFile("OFAC")
                            .lastStatus("SUCCESS")
                            .healthy(true)
                            .build())
                    .un(SourceSyncStatus.builder()
                            .sourceFile("UN")
                            .lastStatus("SUCCESS")
                            .healthy(true)
                            .build())
                    .build();
            when(historyService.getCurrentStatus()).thenReturn(status);

            // when
            ResponseEntity<SanctionsSyncStatusDto> response = controller.getStatus();

            // then
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().getOfac().getSourceFile()).isEqualTo("OFAC");
            assertThat(response.getBody().getUn().getSourceFile()).isEqualTo("UN");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/sanctions-sync/status/{sourceFile}")
    class GetSourceStatus {

        @Test
        @DisplayName("OFAC 상태 조회")
        void shouldReturnOfacStatus() {
            // given
            SourceSyncStatus status = SourceSyncStatus.builder()
                    .sourceFile("OFAC")
                    .lastStatus("SUCCESS")
                    .healthy(true)
                    .build();
            when(historyService.getSourceStatus("OFAC")).thenReturn(status);

            // when
            ResponseEntity<SourceSyncStatus> response = controller.getSourceStatus("OFAC");

            // then
            assertThat(response.getBody().getSourceFile()).isEqualTo("OFAC");
            assertThat(response.getBody().getLastStatus()).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("소문자 sourceFile도 대문자로 변환하여 처리")
        void shouldHandleLowercaseSourceFile() {
            // given
            SourceSyncStatus status = SourceSyncStatus.builder()
                    .sourceFile("UN")
                    .lastStatus("FAIL")
                    .healthy(false)
                    .build();
            when(historyService.getSourceStatus("UN")).thenReturn(status);

            // when
            ResponseEntity<SourceSyncStatus> response = controller.getSourceStatus("un");

            // then
            assertThat(response.getBody().getSourceFile()).isEqualTo("UN");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/sanctions-sync/history")
    class GetHistory {

        @Test
        @DisplayName("이력 목록 조회")
        void shouldReturnHistoryList() {
            // given
            List<SanctionsSyncHistoryDto> historyList = List.of(
                    createHistoryDto(1L, "OFAC", "SUCCESS"),
                    createHistoryDto(2L, "UN", "SUCCESS")
            );
            Page<SanctionsSyncHistoryDto> page = new PageImpl<>(historyList);
            when(historyService.getHistory(isNull(), isNull(), any())).thenReturn(page);

            // when
            ResponseEntity<Page<SanctionsSyncHistoryDto>> response = 
                    controller.getHistory(null, null, PageRequest.of(0, 20));

            // then
            assertThat(response.getBody().getContent()).hasSize(2);
        }

        @Test
        @DisplayName("sourceFile로 필터링하여 조회")
        void shouldFilterBySourceFile() {
            // given
            List<SanctionsSyncHistoryDto> historyList = List.of(
                    createHistoryDto(1L, "OFAC", "SUCCESS")
            );
            Page<SanctionsSyncHistoryDto> page = new PageImpl<>(historyList);
            when(historyService.getHistory(eq("OFAC"), isNull(), any())).thenReturn(page);

            // when
            ResponseEntity<Page<SanctionsSyncHistoryDto>> response = 
                    controller.getHistory("OFAC", null, PageRequest.of(0, 20));

            // then
            assertThat(response.getBody().getContent().get(0).getSourceFile()).isEqualTo("OFAC");
        }

        @Test
        @DisplayName("status로 필터링하여 조회")
        void shouldFilterByStatus() {
            // given
            List<SanctionsSyncHistoryDto> historyList = List.of(
                    createHistoryDto(1L, "OFAC", "FAIL")
            );
            Page<SanctionsSyncHistoryDto> page = new PageImpl<>(historyList);
            when(historyService.getHistory(isNull(), eq("FAIL"), any())).thenReturn(page);

            // when
            ResponseEntity<Page<SanctionsSyncHistoryDto>> response = 
                    controller.getHistory(null, "FAIL", PageRequest.of(0, 20));

            // then
            assertThat(response.getBody().getContent().get(0).getStatus()).isEqualTo("FAIL");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/sanctions-sync/history/{historyId}")
    class GetHistoryDetail {

        @Test
        @DisplayName("이력 상세 조회")
        void shouldReturnHistoryDetail() {
            // given
            SanctionsSyncHistoryDto dto = createHistoryDto(1L, "OFAC", "SUCCESS");
            dto.setInsertCount(100);
            when(historyService.getHistoryDetail(1L)).thenReturn(dto);

            // when
            ResponseEntity<SanctionsSyncHistoryDto> response = controller.getHistoryDetail(1L);

            // then
            assertThat(response.getBody().getHistoryId()).isEqualTo(1L);
            assertThat(response.getBody().getInsertCount()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/sanctions-sync/history/ofac")
    class GetOfacHistory {

        @Test
        @DisplayName("OFAC 이력만 조회")
        void shouldReturnOfacHistoryOnly() {
            // given
            List<SanctionsSyncHistoryDto> historyList = List.of(
                    createHistoryDto(1L, "OFAC", "SUCCESS")
            );
            Page<SanctionsSyncHistoryDto> page = new PageImpl<>(historyList);
            when(historyService.getHistory(eq("OFAC"), isNull(), any())).thenReturn(page);

            // when
            ResponseEntity<Page<SanctionsSyncHistoryDto>> response = 
                    controller.getOfacHistory(null, PageRequest.of(0, 20));

            // then
            assertThat(response.getBody().getContent().get(0).getSourceFile()).isEqualTo("OFAC");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/sanctions-sync/history/un")
    class GetUnHistory {

        @Test
        @DisplayName("UN 이력만 조회")
        void shouldReturnUnHistoryOnly() {
            // given
            List<SanctionsSyncHistoryDto> historyList = List.of(
                    createHistoryDto(1L, "UN", "SUCCESS")
            );
            Page<SanctionsSyncHistoryDto> page = new PageImpl<>(historyList);
            when(historyService.getHistory(eq("UN"), isNull(), any())).thenReturn(page);

            // when
            ResponseEntity<Page<SanctionsSyncHistoryDto>> response = 
                    controller.getUnHistory(null, PageRequest.of(0, 20));

            // then
            assertThat(response.getBody().getContent().get(0).getSourceFile()).isEqualTo("UN");
        }
    }

    // ========================================
    // Helper Methods
    // ========================================

    private SanctionsSyncHistoryDto createHistoryDto(Long id, String sourceFile, String status) {
        return SanctionsSyncHistoryDto.builder()
                .historyId(id)
                .sourceFile(sourceFile)
                .status(status)
                .insertCount(0)
                .updateCount(0)
                .unchangedCount(0)
                .deactivatedCount(0)
                .totalProcessed(0)
                .startedAt(LocalDateTime.now().minusMinutes(5))
                .finishedAt(LocalDateTime.now())
                .durationMs(300000L)
                .build();
    }
}
