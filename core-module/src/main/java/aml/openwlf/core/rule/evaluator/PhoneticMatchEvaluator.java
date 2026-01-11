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
 * 발음 기반 매칭 평가기 (Soundex + Double Metaphone)
 * 
 * 이름의 발음이 유사한 경우를 감지합니다.
 * 예: "Muhammad" = "Mohammed" = "Mohamed"
 *     "Stephen" = "Steven"
 *     "Smith" = "Smyth"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PhoneticMatchEvaluator implements RuleEvaluator {
    
    private final AdvancedMatchingService matchingService;
    
    private static final double DEFAULT_THRESHOLD = 0.7;
    
    @Override
    public String getMatchType() {
        return "PHONETIC";
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
        String algorithm = rule.getCondition().getParameter("algorithm", "METAPHONE");
        
        for (String targetValue : targetValues) {
            if (targetValue == null || targetValue.isBlank()) {
                continue;
            }
            
            MatchResult matchResult = evaluatePhonetic(sourceValue, targetValue, algorithm, threshold);
            
            if (matchResult.isMatch()) {
                double score = calculateScore(matchResult.similarity(), rule.getScore());
                
                log.debug("Phonetic match found [{}]: {} ~ {} (similarity: {:.2f}, score: {:.1f}, Rule: {})", 
                        algorithm, sourceValue, targetValue, matchResult.similarity(), score, rule.getId());
                
                results.add(MatchedRule.builder()
                        .ruleName(rule.getId())
                        .ruleType(rule.getType())
                        .score(score)
                        .matchedValue(sourceValue)
                        .targetValue(targetValue)
                        .description(String.format("%s - %s match (similarity: %.0f%%, codes: %s ≈ %s)", 
                                rule.getDescription(), algorithm.toLowerCase(),
                                matchResult.similarity() * 100,
                                matchResult.sourceCode(), matchResult.targetCode()))
                        .build());
                
                break; // 첫 번째 매칭만 사용
            }
        }
        
        return results;
    }
    
    private MatchResult evaluatePhonetic(String source, String target, String algorithm, double threshold) {
        return switch (algorithm.toUpperCase()) {
            case "SOUNDEX" -> evaluateSoundex(source, target, threshold);
            case "METAPHONE", "DOUBLE_METAPHONE" -> evaluateMetaphone(source, target, threshold);
            case "BOTH" -> evaluateBoth(source, target, threshold);
            default -> evaluateMetaphone(source, target, threshold);
        };
    }
    
    private MatchResult evaluateSoundex(String source, String target, double threshold) {
        double similarity = matchingService.calculateSoundexSimilarity(source, target);
        boolean isMatch = similarity >= threshold || matchingService.matchesSoundex(source, target);
        
        return new MatchResult(
                isMatch,
                isMatch ? Math.max(similarity, 0.8) : similarity,
                matchingService.getSoundexCode(source),
                matchingService.getSoundexCode(target)
        );
    }
    
    private MatchResult evaluateMetaphone(String source, String target, double threshold) {
        double similarity = matchingService.calculateMetaphoneSimilarity(source, target);
        boolean isMatch = similarity >= threshold || matchingService.matchesMetaphone(source, target);
        
        var sourceCode = matchingService.getMetaphoneCode(source);
        var targetCode = matchingService.getMetaphoneCode(target);
        
        return new MatchResult(
                isMatch,
                isMatch ? Math.max(similarity, 0.85) : similarity,
                sourceCode.primary(),
                targetCode.primary()
        );
    }
    
    private MatchResult evaluateBoth(String source, String target, double threshold) {
        MatchResult soundex = evaluateSoundex(source, target, threshold);
        MatchResult metaphone = evaluateMetaphone(source, target, threshold);
        
        // 둘 중 하나라도 매칭되면 매칭으로 처리, 유사도는 높은 것 사용
        boolean isMatch = soundex.isMatch() || metaphone.isMatch();
        double similarity = Math.max(soundex.similarity(), metaphone.similarity());
        
        return new MatchResult(
                isMatch,
                similarity,
                soundex.sourceCode() + "/" + metaphone.sourceCode(),
                soundex.targetCode() + "/" + metaphone.targetCode()
        );
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
    
    private record MatchResult(boolean isMatch, double similarity, String sourceCode, String targetCode) {}
}
