package aml.openwlf.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating alert status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Alert update request")
public class AlertUpdateRequest {
    
    @Schema(description = "New status", example = "IN_REVIEW", 
            allowableValues = {"NEW", "IN_REVIEW", "ESCALATED", "CONFIRMED", "FALSE_POSITIVE", "CLOSED"})
    @NotBlank(message = "Status is required")
    private String status;
    
    @Schema(description = "User making the update", example = "analyst@company.com")
    private String updatedBy;
    
    @Schema(description = "Comment for resolution (required for CONFIRMED, FALSE_POSITIVE, CLOSED)")
    private String comment;
}
