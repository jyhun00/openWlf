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
 * N-Gram 기반 매칭 평가기
 *
 * 문자열을 n개의 연속된 문자로 분할하여 비교합니다.
 * 부분 문자열 기반 매칭으로 오타나 철자 변형에 강합니다.
 *
 * 특징:
 * - Bigram (n=2): 빠르고 일반적인 매칭
 * - Trigram (n=3): 더 정확하지만 짧은 이름에는 부적합
 * - 문자 순서가 조금 달라도 매칭 가능
 */
@Slf4j
@Component
public class NGramMatchEvaluator extends AbstractRuleEvaluator {

    private final AdvancedMatchingService matchingService;

    private static final double DEFAULT_THRESHOLD = 0.6;
    private static final int DEFAULT_N = 2;

    public NGramMatchEvaluator(FieldValueExtractor fieldExtractor,
                               AdvancedMatchingService matchingService) {
        super(fieldExtractor);
        this.matchingService = matchingService;
    }

    @Override
    public String getMatchType() {
        return "NGRAM";
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

        double threshold = rule.getCondition().getParameter("similarityThreshold", DEFAULT_THRESHOLD);
        int n = rule.getCondition().getParameter("ngramSize", DEFAULT_N);

        double bestSimilarity = 0;
        String bestMatch = null;

        for (String targetValue : targetValues) {
            if (!isValidTargetValue(targetValue)) {
                continue;
            }

            double similarity = matchingService.calculateNGramSimilarity(sourceValue, targetValue, n);

            if (similarity >= threshold && similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestMatch = targetValue;
            }
        }

        if (bestMatch != null) {
            double score = calculateScore(bestSimilarity, rule.getScore());
            String ngramType = n == 2 ? "Bigram" : (n == 3 ? "Trigram" : n + "-gram");

            log.debug("N-Gram match found [{}]: {} ~ {} (similarity: {:.2f}, score: {:.1f}, Rule: {})",
                    ngramType, sourceValue, bestMatch, bestSimilarity, score, rule.getId());

            results.add(buildMatchedRule(
                    rule,
                    score,
                    sourceValue,
                    bestMatch,
                    String.format("%s (%s similarity: %.0f%%)",
                            rule.getDescription(), ngramType, bestSimilarity * 100)
            ));
        }

        return results;
    }
}
