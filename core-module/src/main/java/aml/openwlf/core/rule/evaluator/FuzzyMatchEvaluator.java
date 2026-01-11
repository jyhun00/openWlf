package aml.openwlf.core.rule.evaluator;

import aml.openwlf.config.rule.RuleDefinition;
import aml.openwlf.core.model.CustomerInfo;
import aml.openwlf.core.model.MatchedRule;
import aml.openwlf.core.normalization.NormalizationService;
import aml.openwlf.core.rule.WatchlistEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 유사도 기반 매칭 평가기 (Levenshtein Distance)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FuzzyMatchEvaluator implements RuleEvaluator {
    
    private final NormalizationService normalizationService;
    
    private static final double DEFAULT_THRESHOLD = 0.8;
    
    @Override
    public String getMatchType() {
        return "FUZZY";
    }
    
    @Override
    public List<MatchedRule> evaluate(CustomerInfo customer, WatchlistEntry entry, RuleDefinition rule) {
        List<MatchedRule> results = new ArrayList<>();
        
        String sourceField = rule.getCondition().getSourceField();
        String targetField = rule.getCondition().getTargetField();
        
        String sourceValue = getFieldValue(customer, sourceField);
        List<String> targetValues = getTargetFieldValues(entry, targetField);
        
        if (sourceValue == null || sourceValue.isBlank()) {
            return results;
        }
        
        double threshold = rule.getCondition().getParameter("similarityThreshold", DEFAULT_THRESHOLD);
        
        double bestSimilarity = 0;
        String bestMatch = null;
        
        for (String targetValue : targetValues) {
            if (targetValue == null || targetValue.isBlank()) {
                continue;
            }
            
            double similarity = normalizationService.calculateSimilarity(sourceValue, targetValue);
            
            if (similarity >= threshold && similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestMatch = targetValue;
            }
        }
        
        if (bestMatch != null) {
            double score = calculateScore(bestSimilarity, rule.getScore());
            
            log.debug("Fuzzy match found: {} ~ {} (similarity: {:.2f}, score: {:.1f}, Rule: {})", 
                    sourceValue, bestMatch, bestSimilarity, score, rule.getId());
            
            results.add(MatchedRule.builder()
                    .ruleName(rule.getId())
                    .ruleType(rule.getType())
                    .score(score)
                    .matchedValue(sourceValue)
                    .targetValue(bestMatch)
                    .description(rule.getDescription() + String.format(" (similarity: %.0f%%)", bestSimilarity * 100))
                    .build());
        }
        
        return results;
    }
    
    private double calculateScore(double similarity, RuleDefinition.ScoreConfig scoreConfig) {
        if (scoreConfig.isProportionalToSimilarity()) {
            return similarity * scoreConfig.getMaxScore();
        }
        return scoreConfig.getPartialMatch();
    }
    
    private String getFieldValue(CustomerInfo customer, String field) {
        return switch (field.toLowerCase()) {
            case "name" -> customer.getName();
            case "nationality" -> customer.getNationality();
            default -> null;
        };
    }
    
    private List<String> getTargetFieldValues(WatchlistEntry entry, String field) {
        return switch (field.toLowerCase()) {
            case "name" -> List.of(entry.getName() != null ? entry.getName() : "");
            case "aliases" -> entry.getAliases() != null ? entry.getAliases() : List.of();
            case "nationality" -> List.of(entry.getNationality() != null ? entry.getNationality() : "");
            default -> List.of();
        };
    }
}
