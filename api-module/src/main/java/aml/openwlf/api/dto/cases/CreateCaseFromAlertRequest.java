package aml.openwlf.api.dto.cases;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Alert에서 Case 생성 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Alert에서 Case 생성 요청")
public class CreateCaseFromAlertRequest {
    
    @Schema(description = "케이스 제목 (미입력시 자동 생성)", 
            example = "Investigation for suspicious sanctions match")
    private String title;
    
    @Schema(description = "케이스 설명")
    private String description;
    
    @Schema(description = "케이스 유형", example = "SANCTIONS",
            allowableValues = {"SANCTIONS", "PEP", "ADVERSE_MEDIA", "FRAUD", 
                              "MONEY_LAUNDERING", "TERRORIST_FINANCING", "OTHER"})
    private String caseType;
    
    @Schema(description = "담당자 ID", example = "analyst1")
    private String assignedTo;
    
    @Schema(description = "담당 팀", example = "AML Investigation Team")
    private String assignedTeam;
    
    @NotBlank(message = "생성자 정보는 필수입니다")
    @Schema(description = "생성자 ID", example = "admin", required = true)
    private String createdBy;
}
