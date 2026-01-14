package aml.openwlf.core.rule.evaluator;

import aml.openwlf.config.rule.RuleDefinition;
import aml.openwlf.core.matching.AdvancedMatchingService;
import aml.openwlf.core.model.CustomerInfo;
import aml.openwlf.core.model.MatchedRule;
import aml.openwlf.core.rule.WatchlistEntry;
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
public class KoreanNameMatchEvaluator extends AbstractRuleEvaluator {

    private final AdvancedMatchingService matchingService;

    private static final double DEFAULT_THRESHOLD = 0.7;

    public KoreanNameMatchEvaluator(FieldValueExtractor fieldExtractor,
                                    AdvancedMatchingService matchingService) {
        super(fieldExtractor);
        this.matchingService = matchingService;
    }

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

        if (!isValidSourceValue(sourceValue)) {
            return results;
        }

        if (!containsKorean(sourceValue)) {
            return results;
        }

        double threshold = rule.getCondition().getParameter("similarityThreshold", DEFAULT_THRESHOLD);
        boolean chosungOnly = rule.getCondition().getParameter("chosungOnly", false);

        double bestSimilarity = 0;
        String bestMatch = null;
        boolean isChosungMatch = false;

        for (String targetValue : targetValues) {
            if (!isValidTargetValue(targetValue) || !containsKorean(targetValue)) {
                continue;
            }

            if (chosungOnly) {
                if (matchingService.matchesChosung(sourceValue, targetValue)) {
                    bestSimilarity = 0.8;
                    bestMatch = targetValue;
                    isChosungMatch = true;
                    break;
                }
                continue;
            }

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

            results.add(buildMatchedRule(
                    rule,
                    score,
                    sourceValue,
                    bestMatch,
                    String.format("%s - %s (유사도: %.0f%%, 초성: %s ≈ %s)",
                            rule.getDescription(), matchType, bestSimilarity * 100,
                            sourceChosung, targetChosung)
            ));
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
}
