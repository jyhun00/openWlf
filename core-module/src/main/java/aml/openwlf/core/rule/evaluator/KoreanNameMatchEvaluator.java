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
 * 한글 이름 특화 매칭 평가기
 * 
 * 한글 이름의 특성을 고려한 매칭 알고리즘입니다.
 * 
 * 지원 기능:
 * - 초성 매칭: "김철수" ≈ "김창수" (ㄱㅊㅅ)
 * - 자모 분리 매칭: 오타에 강함
 * - 동명이인 감지 보조
 * 
 * 주의: 한글이 포함된 이름에만 적용됩니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KoreanNameMatchEvaluator implements RuleEvaluator {
    
    private final AdvancedMatchingService matchingService;
    
    private static final double DEFAULT_THRESHOLD = 0.7;
    
    @Override
    public String getMatchType() {
        return "KOREAN";
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
        
        // 한글이 포함되어 있지 않으면 스킵
        if (!containsKorean(sourceValue)) {
            return results;
        }
        
        double threshold = rule.getCondition().getParameter("similarityThreshold", DEFAULT_THRESHOLD);
        boolean chosungOnly = rule.getCondition().getParameter("chosungOnly", false);
        
        double bestSimilarity = 0;
        String bestMatch = null;
        boolean isChosungMatch = false;
        
        for (String targetValue : targetValues) {
            if (targetValue == null || targetValue.isBlank() || !containsKorean(targetValue)) {
                continue;
            }
            
            // 초성만 매칭하는 경우
            if (chosungOnly) {
                if (matchingService.matchesChosung(sourceValue, targetValue)) {
                    bestSimilarity = 0.8;
                    bestMatch = targetValue;
                    isChosungMatch = true;
                    break;
                }
                continue;
            }
            
            // 전체 한글 유사도 계산
            double similarity = matchingService.calculateKoreanNameSimilarity(sourceValue, targetValue);
            
            if (similarity >= threshold && similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestMatch = targetValue;
                isChosungMatch = matchingService.matchesChosung(sourceValue, targetValue);
            }
        }
        
        if (bestMatch != null) {
            double score = calculateScore(bestSimilarity, rule.getScore());
            
            String sourceChosung = matchingService.extractChosung(sourceValue);
            String targetChosung = matchingService.extractChosung(bestMatch);
            
            log.debug("Korean name match found: {} ~ {} (similarity: {:.2f}, chosung: {} ≈ {}, Rule: {})", 
                    sourceValue, bestMatch, bestSimilarity, sourceChosung, targetChosung, rule.getId());
            
            String matchType = isChosungMatch ? "초성 일치" : "자모 유사도";
            
            results.add(MatchedRule.builder()
                    .ruleName(rule.getId())
                    .ruleType(rule.getType())
                    .score(score)
                    .matchedValue(sourceValue)
                    .targetValue(bestMatch)
                    .description(String.format("%s - %s (유사도: %.0f%%, 초성: %s ≈ %s)", 
                            rule.getDescription(), matchType, bestSimilarity * 100,
                            sourceChosung, targetChosung))
                    .build());
        }
        
        return results;
    }
    
    private boolean containsKorean(String str) {
        if (str == null) return false;
        for (char c : str.toCharArray()) {
            if (c >= 0xAC00 && c <= 0xD7A3) {
                return true;
            }
        }
        return false;
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
