package aml.openwlf.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Matched rule details")
public class MatchedRuleDto {
    
    @Schema(description = "Rule name", example = "EXACT_NAME_MATCH")
    private String ruleName;
    
    @Schema(description = "Rule type", example = "NAME")
    private String ruleType;
    
    @Schema(description = "Score for this rule", example = "100.0")
    private double score;
    
    @Schema(description = "Value that was matched", example = "JOHN SMITH")
    private String matchedValue;
    
    @Schema(description = "Target value from watchlist", example = "JOHN SMITH")
    private String targetValue;
    
    @Schema(description = "Description of the match", example = "Exact name match found")
    private String description;
}
