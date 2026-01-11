package aml.openwlf.api.dto.cases;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Alert 연결 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Alert 연결 요청")
public class LinkAlertRequest {
    
    @NotNull(message = "Alert ID는 필수입니다")
    @Schema(description = "연결할 Alert ID", example = "1", required = true)
    private Long alertId;
    
    @Schema(description = "연결 사유", example = "Related alert for same customer")
    private String reason;
    
    @NotBlank(message = "연결자 정보는 필수입니다")
    @Schema(description = "연결자 ID", example = "analyst1", required = true)
    private String linkedBy;
}
