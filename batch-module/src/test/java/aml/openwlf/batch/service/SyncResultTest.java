package aml.openwlf.batch.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SyncResult 테스트")
class SyncResultTest {
    
    @Nested
    @DisplayName("성공 결과 생성 테스트")
    class SuccessResultTest {
        
        @Test
        @DisplayName("성공 결과 생성")
        void shouldCreateSuccessResult() {
            // when
            SyncResult result = SyncResult.success("OFAC", 100, 50, 200, 10);
            
            // then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getSourceFile()).isEqualTo("OFAC");
            assertThat(result.getInsertCount()).isEqualTo(100);
            assertThat(result.getUpdateCount()).isEqualTo(50);
            assertThat(result.getUnchangedCount()).isEqualTo(200);
            assertThat(result.getDeactivatedCount()).isEqualTo(10);
            assertThat(result.getEndTime()).isNotNull();
            assertThat(result.getErrorMessage()).isNull();
        }
        
        @Test
        @DisplayName("전체 처리 건수 계산")
        void shouldCalculateTotalProcessed() {
            // given
            SyncResult result = SyncResult.success("OFAC", 100, 50, 200, 10);
            
            // when
            int total = result.getTotalProcessed();
            
            // then
            assertThat(total).isEqualTo(350); // 100 + 50 + 200
        }
        
        @Test
        @DisplayName("성공 결과 toString 형식")
        void shouldFormatSuccessToString() {
            // given
            SyncResult result = SyncResult.success("OFAC", 100, 50, 200, 10);
            result.setDurationMs(1500L);
            
            // when
            String str = result.toString();
            
            // then
            assertThat(str).contains("OFAC");
            assertThat(str).contains("SUCCESS");
            assertThat(str).contains("Insert=100");
            assertThat(str).contains("Update=50");
            assertThat(str).contains("Unchanged=200");
            assertThat(str).contains("Deactivated=10");
            assertThat(str).contains("Duration=1500ms");
        }
    }
    
    @Nested
    @DisplayName("실패 결과 생성 테스트")
    class FailedResultTest {
        
        @Test
        @DisplayName("에러 메시지만으로 실패 결과 생성")
        void shouldCreateFailedResultWithMessage() {
            // when
            SyncResult result = SyncResult.failed("UN", "Connection timeout");
            
            // then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getSourceFile()).isEqualTo("UN");
            assertThat(result.getErrorMessage()).isEqualTo("Connection timeout");
            assertThat(result.getFullErrorLog()).isNull();
            assertThat(result.getEndTime()).isNotNull();
        }
        
        @Test
        @DisplayName("전체 에러 로그와 함께 실패 결과 생성")
        void shouldCreateFailedResultWithFullLog() {
            // given
            String errorMessage = "Connection timeout";
            String fullLog = "java.net.SocketTimeoutException: Connection timeout\n" +
                    "    at java.net.Socket.connect(Socket.java:600)";
            
            // when
            SyncResult result = SyncResult.failed("UN", errorMessage, fullLog);
            
            // then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).isEqualTo(errorMessage);
            assertThat(result.getFullErrorLog()).isEqualTo(fullLog);
        }
        
        @Test
        @DisplayName("실패 결과 toString 형식")
        void shouldFormatFailedToString() {
            // given
            SyncResult result = SyncResult.failed("UN", "Network error");
            
            // when
            String str = result.toString();
            
            // then
            assertThat(str).contains("UN");
            assertThat(str).contains("FAILED");
            assertThat(str).contains("Network error");
        }
        
        @Test
        @DisplayName("실패 시 처리 건수는 모두 0")
        void shouldHaveZeroCountsWhenFailed() {
            // when
            SyncResult result = SyncResult.failed("OFAC", "Error");
            
            // then
            assertThat(result.getInsertCount()).isEqualTo(0);
            assertThat(result.getUpdateCount()).isEqualTo(0);
            assertThat(result.getUnchangedCount()).isEqualTo(0);
            assertThat(result.getDeactivatedCount()).isEqualTo(0);
            assertThat(result.getTotalProcessed()).isEqualTo(0);
        }
    }
    
    @Nested
    @DisplayName("추가 메타데이터 테스트")
    class MetadataTest {
        
        @Test
        @DisplayName("시작 시간과 소요 시간 설정")
        void shouldSetTimeMetadata() {
            // given
            SyncResult result = SyncResult.success("OFAC", 100, 50, 200, 10);
            LocalDateTime startTime = LocalDateTime.now().minusMinutes(5);
            
            // when
            result.setStartTime(startTime);
            result.setDurationMs(300000L); // 5분
            
            // then
            assertThat(result.getStartTime()).isEqualTo(startTime);
            assertThat(result.getDurationMs()).isEqualTo(300000L);
        }
        
        @Test
        @DisplayName("파일 크기 설정")
        void shouldSetFileSize() {
            // given
            SyncResult result = SyncResult.success("OFAC", 100, 50, 200, 10);
            
            // when
            result.setFileSizeBytes(1024 * 1024 * 50L); // 50MB
            
            // then
            assertThat(result.getFileSizeBytes()).isEqualTo(52428800L);
        }
    }
    
    @Nested
    @DisplayName("Builder 테스트")
    class BuilderTest {
        
        @Test
        @DisplayName("Builder로 전체 필드 설정")
        void shouldBuildWithAllFields() {
            // given
            LocalDateTime startTime = LocalDateTime.now().minusMinutes(1);
            LocalDateTime endTime = LocalDateTime.now();
            
            // when
            SyncResult result = SyncResult.builder()
                    .sourceFile("EU")
                    .success(true)
                    .insertCount(500)
                    .updateCount(100)
                    .unchangedCount(1000)
                    .deactivatedCount(50)
                    .startTime(startTime)
                    .endTime(endTime)
                    .durationMs(60000L)
                    .fileSizeBytes(10485760L) // 10MB
                    .build();
            
            // then
            assertThat(result.getSourceFile()).isEqualTo("EU");
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getInsertCount()).isEqualTo(500);
            assertThat(result.getUpdateCount()).isEqualTo(100);
            assertThat(result.getUnchangedCount()).isEqualTo(1000);
            assertThat(result.getDeactivatedCount()).isEqualTo(50);
            assertThat(result.getTotalProcessed()).isEqualTo(1600);
            assertThat(result.getStartTime()).isEqualTo(startTime);
            assertThat(result.getEndTime()).isEqualTo(endTime);
            assertThat(result.getDurationMs()).isEqualTo(60000L);
            assertThat(result.getFileSizeBytes()).isEqualTo(10485760L);
        }
    }
}
