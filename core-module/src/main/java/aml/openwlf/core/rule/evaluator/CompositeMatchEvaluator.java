package aml.openwlf.core.rule.evaluator;

import aml.openwlf.config.rule.RuleDefinition;
import aml.openwlf.core.matching.AdvancedMatchingService;
import aml.openwlf.core.matching.AdvancedMatchingService.CompositeMatchResult;
import aml.openwlf.core.model.CustomerInfo;
import aml.openwlf.core.model.MatchedRule;
import aml.openwlf.core.rule.WatchlistEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 복합 매칭 평가기
 * 
 * 여러 알고리즘의 결과를 종합하여 최종 매칭 여부를 판단합니다.
 * 단일 알고리즘보다 더 정확한 매칭이 가능합니다.
 * 
 * 사용 알고리즘:
 * - Jaro-Winkler (40%)
 * - Double Metaphone (30%)
 * - N-Gram (30%)
 * - Korean (한글인 경우 30%)
 * 
 * 추가 보너스:
 * - Metaphone 완전 일치: 높은 신뢰도로 매칭
 * - Soundex 일치: 발음 기반 매칭 확인
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompositeMatchEvaluator implements RuleEvaluator {
    
    private final AdvancedMatchingService matchingService;
    
    private static final double DEFAULT_THRESHOLD = 0.75;
    
    @Override
    public String getMatchType() {
        return "COMPOSITE";
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
        
        CompositeMatchResult bestResult = null;
        String bestMatch = null;
        
        for (String targetValue : targetValues) {
            if (targetValue == null || targetValue.isBlank()) {
                continue;
            }
            
            CompositeMatchResult result = matchingService.calculateCompositeMatch(sourceValue, targetValue);
            
            // 임계값 이상이거나 발음 매칭이면 매칭으로 처리
            if (result.isHighConfidenceMatch(threshold)) {
                if (bestResult == null || result.compositeScore() > bestResult.compositeScore()) {
                    bestResult = result;
                    bestMatch = targetValue;
                }
            }
        }
        
        if (bestResult != null) {
            double score = calculateScore(bestResult.compositeScore(), rule.getScore());
            
            log.debug("Composite match found: {} ~ {} (composite: {:.2f}, JW: {:.2f}, MP: {:.2f}, NG: {:.2f}, KR: {:.2f}, Rule: {})", 
                    sourceValue, bestMatch, 
                    bestResult.compositeScore(), bestResult.jaroWinklerScore(),
                    bestResult.metaphoneScore(), bestResult.ngramScore(), bestResult.koreanScore(),
                    rule.getId());
            
            String details = buildDetails(bestResult);
            
            results.add(MatchedRule.builder()
                    .ruleName(rule.getId())
                    .ruleType(rule.getType())
                    .score(score)
                    .matchedValue(sourceValue)
                    .targetValue(bestMatch)
                    .description(String.format("%s (종합 유사도: %.0f%% | %s)", 
                            rule.getDescription(), bestResult.compositeScore() * 100, details))
                    .build());
        }
        
        return results;
    }
    
    private String buildDetails(CompositeMatchResult result) {
        List<String> parts = new ArrayList<>();
        
        parts.add(String.format("JW:%.0f%%", result.jaroWinklerScore() * 100));
        parts.add(String.format("음성:%.0f%%", result.metaphoneScore() * 100));
        parts.add(String.format("N-gram:%.0f%%", result.ngramScore() * 100));
        
        if (result.koreanScore() > 0) {
            parts.add(String.format("한글:%.0f%%", result.koreanScore() * 100));
        }
        
        if (result.metaphoneMatch()) {
            parts.add("발음일치✓");
        }
        
        return String.join(", ", parts);
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
