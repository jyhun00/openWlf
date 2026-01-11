package aml.openwlf.api.dto.cases;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Case 상태 변경 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "케이스 상태 변경 요청")
public class CaseStatusUpdateRequest {
    
    @NotBlank(message = "상태는 필수입니다")
    @Schema(description = "새로운 상태", example = "IN_PROGRESS", required = true,
            allowableValues = {"OPEN", "IN_PROGRESS", "PENDING_INFO", 
                              "PENDING_REVIEW", "ESCALATED", "CLOSED"})
    private String status;
    
    @NotBlank(message = "수정자 정보는 필수입니다")
    @Schema(description = "수정자 ID", example = "analyst1", required = true)
    private String updatedBy;
}
