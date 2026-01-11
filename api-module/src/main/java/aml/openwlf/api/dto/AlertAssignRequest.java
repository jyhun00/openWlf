package aml.openwlf.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for assigning alert to analyst
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Alert assignment request")
public class AlertAssignRequest {
    
    @Schema(description = "Analyst to assign", example = "analyst@company.com")
    @NotBlank(message = "Assignee is required")
    private String assignedTo;
}
