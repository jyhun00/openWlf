package aml.openwlf.core.scoring;

import aml.openwlf.core.model.CustomerInfo;
import aml.openwlf.core.model.FilteringResult;
import aml.openwlf.core.model.MatchedRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for scoring and determining alert status
 */
@Slf4j
@Service
public class ScoringService {
    
    @Value("${watchlist.threshold.alert:70.0}")
    private double alertThreshold;
    
    @Value("${watchlist.threshold.review:50.0}")
    private double reviewThreshold;
    
    /**
     * Calculate total score from matched rules
     */
    public FilteringResult calculateScore(CustomerInfo customerInfo, List<MatchedRule> matchedRules) {
        if (matchedRules == null || matchedRules.isEmpty()) {
            return FilteringResult.builder()
                    .alert(false)
                    .score(0.0)
                    .matchedRules(List.of())
                    .explanation("No matches found")
                    .customerInfo(customerInfo)
                    .build();
        }
        
        // Calculate weighted score
        double totalScore = calculateWeightedScore(matchedRules);
        
        // Determine alert status
        boolean alert = totalScore >= alertThreshold;
        
        // Generate explanation
        String explanation = generateExplanation(totalScore, matchedRules, alert);
        
        log.info("Filtering result for customer {}: score={}, alert={}", 
                customerInfo.getName(), totalScore, alert);
        
        return FilteringResult.builder()
                .alert(alert)
                .score(totalScore)
                .matchedRules(matchedRules)
                .explanation(explanation)
                .customerInfo(customerInfo)
                .build();
    }
    
    /**
     * Calculate weighted score based on rule types
     */
    private double calculateWeightedScore(List<MatchedRule> rules) {
        double score = 0.0;
        
        // Group rules by type and take the highest score for each type
        var rulesByType = rules.stream()
                .collect(Collectors.groupingBy(MatchedRule::getRuleType));
        
        for (var entry : rulesByType.entrySet()) {
            double maxScore = entry.getValue().stream()
                    .mapToDouble(MatchedRule::getScore)
                    .max()
                    .orElse(0.0);
            score += maxScore;
        }
        
        // Cap at 100
        return Math.min(score, 100.0);
    }
    
    /**
     * Generate human-readable explanation
     */
    private String generateExplanation(double score, List<MatchedRule> rules, boolean alert) {
        StringBuilder explanation = new StringBuilder();
        
        if (alert) {
            explanation.append("[!] ALERT: High-risk match detected (Score: ")
                    .append(String.format("%.1f", score))
                    .append(")\n\n");
        } else if (score >= reviewThreshold) {
            explanation.append("[*] REVIEW: Potential match requires manual review (Score: ")
                    .append(String.format("%.1f", score))
                    .append(")\n\n");
        } else {
            explanation.append("[OK] LOW RISK: No significant matches (Score: ")
                    .append(String.format("%.1f", score))
                    .append(")\n\n");
        }
        
        explanation.append("Matched Rules:\n");
        for (MatchedRule rule : rules) {
            explanation.append("- ")
                    .append(rule.getRuleName())
                    .append(" (")
                    .append(String.format("%.1f", rule.getScore()))
                    .append(" points): ")
                    .append(rule.getDescription())
                    .append("\n");
            
            if (rule.getMatchedValue() != null && !rule.getMatchedValue().isEmpty()) {
                explanation.append("  Input: '")
                        .append(rule.getMatchedValue())
                        .append("' vs Target: '")
                        .append(rule.getTargetValue())
                        .append("'\n");
            }
        }
        
        explanation.append("\nRecommendation: ");
        if (alert) {
            explanation.append("Reject transaction and escalate to compliance team for investigation.");
        } else if (score >= reviewThreshold) {
            explanation.append("Perform enhanced due diligence before proceeding.");
        } else {
            explanation.append("Proceed with standard processing.");
        }
        
        return explanation.toString();
    }
}
