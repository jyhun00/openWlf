package aml.openwlf.api.dto.cases;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Case 코멘트 추가 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "케이스 코멘트 추가 요청")
public class AddCommentRequest {
    
    @NotBlank(message = "코멘트 내용은 필수입니다")
    @Schema(description = "코멘트 내용", 
            example = "Reviewed customer transaction history. No suspicious patterns found.", 
            required = true)
    private String content;
    
    @Schema(description = "코멘트 유형", example = "ANALYSIS", 
            allowableValues = {"NOTE", "ANALYSIS", "FINDING", "RECOMMENDATION", 
                              "ESCALATION", "DECISION"})
    private String commentType;
    
    @NotBlank(message = "작성자 정보는 필수입니다")
    @Schema(description = "작성자 ID", example = "analyst1", required = true)
    private String createdBy;
}
