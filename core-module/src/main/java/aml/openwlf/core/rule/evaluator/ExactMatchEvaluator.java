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
 * 정확히 일치하는지 평가하는 평가기
 */
@Slf4j
@Component
public class ExactMatchEvaluator extends AbstractRuleEvaluator {

    private final NormalizationService normalizationService;

    public ExactMatchEvaluator(FieldValueExtractor fieldExtractor,
                               NormalizationService normalizationService) {
        super(fieldExtractor);
        this.normalizationService = normalizationService;
    }

    @Override
    public String getMatchType() {
        return "EXACT";
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

        String normalizedSource = normalizeValue(sourceValue, sourceField);

        for (String targetValue : targetValues) {
            if (!isValidTargetValue(targetValue)) {
                continue;
            }

            String normalizedTarget = normalizeValue(targetValue, targetField);

            if (normalizedSource.equals(normalizedTarget)) {
                log.debug("Exact match found: {} = {} (Rule: {})",
                        sourceValue, targetValue, rule.getId());

                results.add(buildMatchedRule(
                        rule,
                        getExactMatchScore(rule.getScore()),
                        sourceValue,
                        targetValue,
                        rule.getDescription()
                ));

                break;
            }
        }

        return results;
    }

    private String normalizeValue(String value, String field) {
        if (fieldExtractor.isNameField(field)) {
            return normalizationService.normalizeName(value);
        } else if (field.equalsIgnoreCase("nationality")) {
            return normalizationService.normalizeNationality(value);
        }
        return value.toUpperCase().trim();
    }
}
