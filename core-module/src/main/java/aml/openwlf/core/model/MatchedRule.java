package aml.openwlf.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Matched rule information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchedRule {
    private String ruleName;
    private String ruleType;
    private double score;
    private String matchedValue;
    private String targetValue;
    private String description;
}
