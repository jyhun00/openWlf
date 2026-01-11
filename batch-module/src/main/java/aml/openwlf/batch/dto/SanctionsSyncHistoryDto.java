package aml.openwlf.batch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 제재 리스트 동기화 이력 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "제재 리스트 동기화 이력")
public class SanctionsSyncHistoryDto {

    @Schema(description = "이력 ID", example = "1")
    private Long historyId;

    @Schema(description = "데이터 소스", example = "OFAC", allowableValues = {"OFAC", "UN"})
    private String sourceFile;

    @Schema(description = "실행 상태", example = "SUCCESS", allowableValues = {"SUCCESS", "FAIL"})
    private String status;

    @Schema(description = "신규 삽입 건수", example = "150")
    private Integer insertCount;

    @Schema(description = "업데이트 건수", example = "23")
    private Integer updateCount;

    @Schema(description = "변경 없음 건수", example = "12500")
    private Integer unchangedCount;

    @Schema(description = "비활성화 건수", example = "5")
    private Integer deactivatedCount;

    @Schema(description = "총 처리 건수", example = "12678")
    private Integer totalProcessed;

    @Schema(description = "실행 시작 시간", example = "2025-12-29T02:00:00")
    private LocalDateTime startedAt;

    @Schema(description = "실행 완료 시간", example = "2025-12-29T02:05:30")
    private LocalDateTime finishedAt;

    @Schema(description = "소요 시간 (밀리초)", example = "330000")
    private Long durationMs;

    @Schema(description = "소요 시간 (초)", example = "330.0")
    private Double durationSeconds;

    @Schema(description = "설명 / 에러 로그")
    private String description;

    @Schema(description = "다운로드 파일 크기 (bytes)", example = "52428800")
    private Long fileSizeBytes;
}
