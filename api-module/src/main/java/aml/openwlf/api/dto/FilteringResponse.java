package aml.openwlf.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for filtering result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Watchlist filtering result")
public class FilteringResponse {
    
    @Schema(description = "Alert flag indicating if customer should be blocked", example = "true")
    private boolean alert;
    
    @Schema(description = "Risk score (0-100)", example = "85.5")
    private double score;
    
    @Schema(description = "List of matched rules")
    private List<MatchedRuleDto> matchedRules;
    
    @Schema(description = "Detailed explanation of the filtering result")
    private String explanation;
    
    @Schema(description = "Customer information that was filtered")
    private CustomerInfoDto customerInfo;
    
    @Schema(description = "Alert reference number (generated when score >= 50)", example = "ALT-20251228-A1B2C3D4")
    private String alertReference;
}
