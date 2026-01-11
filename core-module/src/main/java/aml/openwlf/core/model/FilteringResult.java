package aml.openwlf.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of watchlist filtering
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilteringResult {
    private boolean alert;
    private double score;
    private List<MatchedRule> matchedRules;
    private String explanation;
    private CustomerInfo customerInfo;
}
