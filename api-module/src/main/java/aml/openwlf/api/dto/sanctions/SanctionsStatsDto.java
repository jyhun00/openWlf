package aml.openwlf.api.dto.sanctions;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 제재 대상 통계 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "제재 대상 통계 정보")
public class SanctionsStatsDto {
    
    @Schema(description = "전체 엔티티 수", example = "15000")
    private Long totalEntities;
    
    @Schema(description = "활성 엔티티 수", example = "14500")
    private Long activeEntities;
    
    @Schema(description = "출처별 엔티티 수")
    private Map<String, Long> bySourceFile;
    
    @Schema(description = "유형별 엔티티 수")
    private Map<String, Long> byEntityType;
    
    @Schema(description = "국적별 상위 10개")
    private Map<String, Long> topNationalities;
    
    @Schema(description = "전체 이름/별칭 수", example = "45000")
    private Long totalNames;
    
    @Schema(description = "전체 주소 수", example = "8000")
    private Long totalAddresses;
    
    @Schema(description = "전체 문서 수", example = "5000")
    private Long totalDocuments;
    
    @Schema(description = "마지막 데이터 업데이트", example = "2025-12-28T15:30:00")
    private LocalDateTime lastDataUpdate;
}
