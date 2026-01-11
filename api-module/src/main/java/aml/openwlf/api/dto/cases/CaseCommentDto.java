package aml.openwlf.api.dto.cases;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Case 코멘트 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "케이스 코멘트")
public class CaseCommentDto {
    
    @Schema(description = "코멘트 ID", example = "1")
    private Long id;
    
    @Schema(description = "케이스 ID", example = "1")
    private Long caseId;
    
    @Schema(description = "코멘트 내용")
    private String content;
    
    @Schema(description = "코멘트 유형", example = "ANALYSIS")
    private String commentType;
    
    @Schema(description = "내부 전용 여부")
    private Boolean isInternal;
    
    @Schema(description = "작성자")
    private String createdBy;
    
    @Schema(description = "작성일시")
    private LocalDateTime createdAt;
    
    @Schema(description = "수정일시")
    private LocalDateTime updatedAt;
}
