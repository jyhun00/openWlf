package aml.openwlf.api.dto.cases;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 여러 Alert를 묶어 Case 생성 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "여러 Alert를 묶어 Case 생성 요청")
public class CreateCaseFromMultipleAlertsRequest {
    
    @NotEmpty(message = "최소 하나의 Alert ID가 필요합니다")
    @Schema(description = "연결할 Alert ID 목록", example = "[1, 2, 3]", required = true)
    private List<Long> alertIds;
    
    @Schema(description = "케이스 제목", example = "Consolidated investigation for customer XYZ")
    private String title;
    
    @Schema(description = "케이스 설명")
    private String description;
    
    @Schema(description = "케이스 유형", example = "SANCTIONS")
    private String caseType;
    
    @Schema(description = "담당자 ID", example = "analyst1")
    private String assignedTo;
    
    @Schema(description = "담당 팀", example = "AML Investigation Team")
    private String assignedTeam;
    
    @NotBlank(message = "생성자 정보는 필수입니다")
    @Schema(description = "생성자 ID", example = "admin", required = true)
    private String createdBy;
}
