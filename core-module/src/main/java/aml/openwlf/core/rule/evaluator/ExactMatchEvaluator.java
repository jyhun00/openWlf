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
 * 정확히 일치하는지 평가하는 평가기
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExactMatchEvaluator implements RuleEvaluator {
    
    private final NormalizationService normalizationService;
    
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
        
        if (sourceValue == null || sourceValue.isBlank()) {
            return results;
        }
        
        String normalizedSource = normalizeValue(sourceValue, sourceField);
        
        for (String targetValue : targetValues) {
            if (targetValue == null || targetValue.isBlank()) {
                continue;
            }
            
            String normalizedTarget = normalizeValue(targetValue, targetField);
            
            if (normalizedSource.equals(normalizedTarget)) {
                log.debug("Exact match found: {} = {} (Rule: {})", 
                        sourceValue, targetValue, rule.getId());
                
                results.add(MatchedRule.builder()
                        .ruleName(rule.getId())
                        .ruleType(rule.getType())
                        .score(rule.getScore().getExactMatch())
                        .matchedValue(sourceValue)
                        .targetValue(targetValue)
                        .description(rule.getDescription())
                        .build());
                
                // 정확 일치는 하나만 찾으면 됨
                break;
            }
        }
        
        return results;
    }
    
    private String getFieldValue(CustomerInfo customer, String field) {
        return switch (field.toLowerCase()) {
            case "name" -> customer.getName();
            case "nationality" -> customer.getNationality();
            case "dateofbirth", "dob" -> customer.getDateOfBirth() != null 
                    ? customer.getDateOfBirth().toString() : null;
            case "customerid" -> customer.getCustomerId();
            default -> null;
        };
    }
    
    private List<String> getTargetFieldValues(WatchlistEntry entry, String field) {
        return switch (field.toLowerCase()) {
            case "name" -> List.of(entry.getName() != null ? entry.getName() : "");
            case "aliases" -> entry.getAliases() != null ? entry.getAliases() : List.of();
            case "nationality" -> List.of(entry.getNationality() != null ? entry.getNationality() : "");
            case "dateofbirth", "dob" -> List.of(entry.getDateOfBirth() != null 
                    ? entry.getDateOfBirth().toString() : "");
            default -> List.of();
        };
    }
    
    private String normalizeValue(String value, String field) {
        if (field.equalsIgnoreCase("name") || field.equalsIgnoreCase("aliases")) {
            return normalizationService.normalizeName(value);
        } else if (field.equalsIgnoreCase("nationality")) {
            return normalizationService.normalizeNationality(value);
        }
        return value.toUpperCase().trim();
    }
}
