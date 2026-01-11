package aml.openwlf.core.rule.evaluator;

import aml.openwlf.config.rule.RuleDefinition;
import aml.openwlf.core.matching.AdvancedMatchingService;
import aml.openwlf.core.model.CustomerInfo;
import aml.openwlf.core.model.MatchedRule;
import aml.openwlf.core.rule.WatchlistEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Jaro-Winkler 유사도 기반 매칭 평가기
 * 
 * 이름 매칭에 특화된 알고리즘으로, 문자열 앞부분의 일치에 더 높은 가중치를 부여합니다.
 * Levenshtein보다 이름 매칭에 더 적합합니다.
 * 
 * 특징:
 * - 앞부분이 일치하면 더 높은 점수 (이름의 성이 같으면 유리)
 * - 토큰 기반 매칭으로 이름 순서가 달라도 매칭 가능
 * - 오타에 강함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JaroWinklerMatchEvaluator implements RuleEvaluator {
    
    private final AdvancedMatchingService matchingService;
    
    private static final double DEFAULT_THRESHOLD = 0.85;
    
    @Override
    public String getMatchType() {
        return "JARO_WINKLER";
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
        boolean useTokenMatching = rule.getCondition().getParameter("tokenMatching", true);
        
        double bestSimilarity = 0;
        String bestMatch = null;
        
        for (String targetValue : targetValues) {
            if (targetValue == null || targetValue.isBlank()) {
                continue;
            }
            
            double similarity;
            if (useTokenMatching) {
                // 토큰 기반 매칭 (이름 순서가 달라도 매칭)
                similarity = matchingService.calculateTokenJaroWinklerSimilarity(sourceValue, targetValue);
            } else {
                // 전체 문자열 매칭
                similarity = matchingService.calculateJaroWinklerSimilarity(sourceValue, targetValue);
            }
            
            if (similarity >= threshold && similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestMatch = targetValue;
            }
        }
        
        if (bestMatch != null) {
            double score = calculateScore(bestSimilarity, rule.getScore());
            
            log.debug("Jaro-Winkler match found: {} ~ {} (similarity: {:.2f}, score: {:.1f}, Rule: {})", 
                    sourceValue, bestMatch, bestSimilarity, score, rule.getId());
            
            results.add(MatchedRule.builder()
                    .ruleName(rule.getId())
                    .ruleType(rule.getType())
                    .score(score)
                    .matchedValue(sourceValue)
                    .targetValue(bestMatch)
                    .description(String.format("%s (Jaro-Winkler similarity: %.0f%%)", 
                            rule.getDescription(), bestSimilarity * 100))
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
