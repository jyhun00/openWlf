package aml.openwlf.core.rule.evaluator;

import aml.openwlf.config.rule.RuleDefinition;
import aml.openwlf.core.model.CustomerInfo;
import aml.openwlf.core.model.MatchedRule;
import aml.openwlf.core.normalization.NormalizationService;
import aml.openwlf.core.rule.WatchlistEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 유사도 기반 매칭 평가기 (Levenshtein Distance)
 */
@Slf4j
@Component
public class FuzzyMatchEvaluator extends AbstractRuleEvaluator {

    private final NormalizationService normalizationService;

    private static final double DEFAULT_THRESHOLD = 0.8;

    public FuzzyMatchEvaluator(FieldValueExtractor fieldExtractor,
                               NormalizationService normalizationService) {
        super(fieldExtractor);
        this.normalizationService = normalizationService;
    }

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

        if (!isValidSourceValue(sourceValue)) {
            return results;
        }

        double threshold = rule.getCondition().getParameter("similarityThreshold", DEFAULT_THRESHOLD);

        double bestSimilarity = 0;
        String bestMatch = null;

        for (String targetValue : targetValues) {
            if (!isValidTargetValue(targetValue)) {
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

            results.add(buildMatchedRule(
                    rule,
                    score,
                    sourceValue,
                    bestMatch,
                    rule.getDescription() + String.format(" (similarity: %.0f%%)", bestSimilarity * 100)
            ));
        }

        return results;
    }
}
