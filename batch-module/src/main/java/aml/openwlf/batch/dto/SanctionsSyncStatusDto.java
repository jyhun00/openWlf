package aml.openwlf.batch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 제재 리스트 동기화 현재 상태 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "제재 리스트 동기화 현재 상태")
public class SanctionsSyncStatusDto {

    @Schema(description = "OFAC 동기화 상태")
    private SourceSyncStatus ofac;

    @Schema(description = "UN 동기화 상태")
    private SourceSyncStatus un;

    /**
     * 개별 소스 동기화 상태
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "개별 소스 동기화 상태")
    public static class SourceSyncStatus {

        @Schema(description = "데이터 소스", example = "OFAC")
        private String sourceFile;

        @Schema(description = "마지막 동기화 상태", example = "SUCCESS")
        private String lastStatus;

        @Schema(description = "마지막 동기화 시간", example = "2025-12-29T02:05:30")
        private LocalDateTime lastSyncAt;

        @Schema(description = "마지막 성공 시간", example = "2025-12-29T02:05:30")
        private LocalDateTime lastSuccessAt;

        @Schema(description = "마지막 동기화 처리 건수", example = "12678")
        private Integer lastTotalProcessed;

        @Schema(description = "마지막 동기화 소요 시간 (초)", example = "330.0")
        private Double lastDurationSeconds;

        @Schema(description = "최근 24시간 실패 횟수", example = "0")
        private Integer failureCountLast24h;

        @Schema(description = "연속 실패 횟수", example = "0")
        private Integer consecutiveFailures;

        @Schema(description = "상태 양호 여부", example = "true")
        private Boolean healthy;

        @Schema(description = "마지막 에러 메시지 (실패 시)")
        private String lastErrorMessage;
    }
}
