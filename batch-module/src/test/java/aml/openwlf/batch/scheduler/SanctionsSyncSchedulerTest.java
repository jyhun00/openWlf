package aml.openwlf.batch.scheduler;

import aml.openwlf.batch.service.SanctionsSyncService;
import aml.openwlf.batch.service.SyncResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SanctionsSyncScheduler 테스트")
class SanctionsSyncSchedulerTest {

    @Mock
    private SanctionsSyncService sanctionsSyncService;

    private SanctionsSyncScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SanctionsSyncScheduler(sanctionsSyncService);
    }

    @Nested
    @DisplayName("scheduledSyncAll() 메서드")
    class ScheduledSyncAll {

        @Test
        @DisplayName("syncAll을 호출한다")
        void shouldCallSyncAll() {
            // given
            List<SyncResult> results = List.of(
                    SyncResult.success("OFAC", 100, 50, 30, 5),
                    SyncResult.success("UN", 200, 100, 50, 10)
            );
            when(sanctionsSyncService.syncAll()).thenReturn(results);

            // when
            scheduler.scheduledSyncAll();

            // then
            verify(sanctionsSyncService, times(1)).syncAll();
        }

        @Test
        @DisplayName("syncAll이 실패해도 예외를 던지지 않는다")
        void shouldNotThrowExceptionWhenSyncAllFails() {
            // given
            List<SyncResult> results = List.of(
                    SyncResult.failed("OFAC", "Connection error"),
                    SyncResult.failed("UN", "Timeout")
            );
            when(sanctionsSyncService.syncAll()).thenReturn(results);

            // when & then - 예외 없이 실행됨
            scheduler.scheduledSyncAll();
            
            verify(sanctionsSyncService).syncAll();
        }

        @Test
        @DisplayName("예외가 발생해도 메서드가 완료된다")
        void shouldCompleteEvenWhenExceptionOccurs() {
            // given
            when(sanctionsSyncService.syncAll()).thenThrow(new RuntimeException("Unexpected error"));

            // when & then - 예외가 발생해도 메서드는 완료됨 (catch 블록에서 로깅만 함)
            scheduler.scheduledSyncAll();
            
            verify(sanctionsSyncService).syncAll();
        }
    }

    @Nested
    @DisplayName("syncOfacManually() 메서드")
    class SyncOfacManually {

        @Test
        @DisplayName("OFAC 수동 동기화를 실행한다")
        void shouldSyncOfacManually() {
            // given
            SyncResult expected = SyncResult.success("OFAC", 100, 50, 30, 5);
            when(sanctionsSyncService.syncOfac()).thenReturn(expected);

            // when
            SyncResult result = scheduler.syncOfacManually();

            // then
            assertThat(result).isEqualTo(expected);
            verify(sanctionsSyncService, times(1)).syncOfac();
        }

        @Test
        @DisplayName("OFAC 동기화 실패 시 실패 결과를 반환한다")
        void shouldReturnFailureResultWhenFailed() {
            // given
            SyncResult failResult = SyncResult.failed("OFAC", "Network error");
            when(sanctionsSyncService.syncOfac()).thenReturn(failResult);

            // when
            SyncResult result = scheduler.syncOfacManually();

            // then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).isEqualTo("Network error");
        }
    }

    @Nested
    @DisplayName("syncUnManually() 메서드")
    class SyncUnManually {

        @Test
        @DisplayName("UN 수동 동기화를 실행한다")
        void shouldSyncUnManually() {
            // given
            SyncResult expected = SyncResult.success("UN", 200, 100, 50, 10);
            when(sanctionsSyncService.syncUn()).thenReturn(expected);

            // when
            SyncResult result = scheduler.syncUnManually();

            // then
            assertThat(result).isEqualTo(expected);
            verify(sanctionsSyncService, times(1)).syncUn();
        }

        @Test
        @DisplayName("UN 동기화 실패 시 실패 결과를 반환한다")
        void shouldReturnFailureResultWhenFailed() {
            // given
            SyncResult failResult = SyncResult.failed("UN", "Parse error");
            when(sanctionsSyncService.syncUn()).thenReturn(failResult);

            // when
            SyncResult result = scheduler.syncUnManually();

            // then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).isEqualTo("Parse error");
        }
    }
}
