package aml.openwlf.api.dto.cases;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Case 활동 로그 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "케이스 활동 로그")
public class CaseActivityDto {
    
    @Schema(description = "활동 ID", example = "1")
    private Long id;
    
    @Schema(description = "케이스 ID", example = "1")
    private Long caseId;
    
    @Schema(description = "활동 유형", example = "STATUS_CHANGED")
    private String activityType;
    
    @Schema(description = "활동 설명", example = "Status changed from OPEN to IN_PROGRESS")
    private String description;
    
    @Schema(description = "이전 값", example = "OPEN")
    private String oldValue;
    
    @Schema(description = "새 값", example = "IN_PROGRESS")
    private String newValue;
    
    @Schema(description = "수행자", example = "analyst1")
    private String performedBy;
    
    @Schema(description = "수행 일시")
    private LocalDateTime createdAt;
}
