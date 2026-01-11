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
 * 포함 여부 매칭 평가기
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContainsMatchEvaluator implements RuleEvaluator {
    
    private final NormalizationService normalizationService;
    
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
        
        if (sourceValue == null || sourceValue.isBlank()) {
            return results;
        }
        
        boolean allWords = rule.getCondition().getParameter("allWords", true);
        
        String normalizedSource = normalizationService.normalizeName(sourceValue);
        
        for (String targetValue : targetValues) {
            if (targetValue == null || targetValue.isBlank()) {
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
                
                results.add(MatchedRule.builder()
                        .ruleName(rule.getId())
                        .ruleType(rule.getType())
                        .score(rule.getScore().getPartialMatch())
                        .matchedValue(sourceValue)
                        .targetValue(targetValue)
                        .description(rule.getDescription())
                        .build());
                
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
            default -> List.of();
        };
    }
}
