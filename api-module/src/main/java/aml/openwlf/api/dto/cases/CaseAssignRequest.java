package aml.openwlf.api.dto.cases;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Case 담당자 배정 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "케이스 담당자 배정 요청")
public class CaseAssignRequest {
    
    @NotBlank(message = "담당자는 필수입니다")
    @Schema(description = "담당자 ID", example = "analyst1", required = true)
    private String assignedTo;
    
    @Schema(description = "담당 팀", example = "AML Investigation Team")
    private String assignedTeam;
    
    @NotBlank(message = "배정자 정보는 필수입니다")
    @Schema(description = "배정자 ID", example = "manager1", required = true)
    private String assignedBy;
}
