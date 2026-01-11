package aml.openwlf.api.dto.cases;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Case 최종 결정 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "케이스 최종 결정 요청")
public class CaseDecisionRequest {
    
    @NotBlank(message = "결정은 필수입니다")
    @Schema(description = "최종 결정", example = "TRUE_POSITIVE", required = true,
            allowableValues = {"TRUE_POSITIVE", "FALSE_POSITIVE", "INCONCLUSIVE", 
                              "ESCALATED_TO_LE", "NO_ACTION_REQUIRED"})
    private String decision;
    
    @NotBlank(message = "결정 사유는 필수입니다")
    @Schema(description = "결정 사유", 
            example = "Customer name matches OFAC SDN list entry with 95% confidence. " +
                      "DOB and nationality also match.", required = true)
    private String rationale;
    
    @NotBlank(message = "결정자 정보는 필수입니다")
    @Schema(description = "결정자 ID", example = "senior_analyst", required = true)
    private String decidedBy;
}
