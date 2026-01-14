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
 * 포함 여부 매칭 평가기
 */
@Slf4j
@Component
public class ContainsMatchEvaluator extends AbstractRuleEvaluator {

    private final NormalizationService normalizationService;

    public ContainsMatchEvaluator(FieldValueExtractor fieldExtractor,
                                  NormalizationService normalizationService) {
        super(fieldExtractor);
        this.normalizationService = normalizationService;
    }

    @Override
    public String getMatchType() {
        return "CONTAINS";
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

        boolean allWords = rule.getCondition().getParameter("allWords", true);

        String normalizedSource = normalizationService.normalizeName(sourceValue);

        for (String targetValue : targetValues) {
            if (!isValidTargetValue(targetValue)) {
                continue;
            }

            String normalizedTarget = normalizationService.normalizeName(targetValue);

            boolean matched;
            if (allWords) {
                matched = containsAllWords(normalizedSource, normalizedTarget)
                        || containsAllWords(normalizedTarget, normalizedSource);
            } else {
                matched = normalizedSource.contains(normalizedTarget)
                        || normalizedTarget.contains(normalizedSource);
            }

            if (matched) {
                log.debug("Contains match found: {} <-> {} (Rule: {})",
                        sourceValue, targetValue, rule.getId());

                results.add(buildMatchedRule(
                        rule,
                        rule.getScore().getPartialMatch(),
                        sourceValue,
                        targetValue,
                        rule.getDescription()
                ));

                break;
            }
        }

        return results;
    }

    private boolean containsAllWords(String source, String target) {
        String[] sourceWords = source.split("\\s+");
        for (String word : sourceWords) {
            if (!word.isEmpty() && !target.contains(word)) {
                return false;
            }
        }
        return sourceWords.length > 0;
    }
}
