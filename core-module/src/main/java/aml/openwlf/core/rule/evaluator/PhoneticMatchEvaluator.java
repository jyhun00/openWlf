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
 * 발음 기반 매칭 평가기 (Soundex + Double Metaphone)
 *
 * 이름의 발음이 유사한 경우를 감지합니다.
 * 예: "Muhammad" = "Mohammed" = "Mohamed"
 *     "Stephen" = "Steven"
 *     "Smith" = "Smyth"
 */
@Slf4j
@Component
public class PhoneticMatchEvaluator extends AbstractRuleEvaluator {

    private final AdvancedMatchingService matchingService;

    private static final double DEFAULT_THRESHOLD = 0.7;

    public PhoneticMatchEvaluator(FieldValueExtractor fieldExtractor,
                                  AdvancedMatchingService matchingService) {
        super(fieldExtractor);
        this.matchingService = matchingService;
    }

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

        if (!isValidSourceValue(sourceValue)) {
            return results;
        }

        double threshold = rule.getCondition().getParameter("similarityThreshold", DEFAULT_THRESHOLD);
        String algorithm = rule.getCondition().getParameter("algorithm", "METAPHONE");

        for (String targetValue : targetValues) {
            if (!isValidTargetValue(targetValue)) {
                continue;
            }

            MatchResult matchResult = evaluatePhonetic(sourceValue, targetValue, algorithm, threshold);

            if (matchResult.isMatch()) {
                double score = calculateScore(matchResult.similarity(), rule.getScore());

                log.debug("Phonetic match found [{}]: {} ~ {} (similarity: {:.2f}, score: {:.1f}, Rule: {})",
                        algorithm, sourceValue, targetValue, matchResult.similarity(), score, rule.getId());

                results.add(buildMatchedRule(
                        rule,
                        score,
                        sourceValue,
                        targetValue,
                        String.format("%s - %s match (similarity: %.0f%%, codes: %s ≈ %s)",
                                rule.getDescription(), algorithm.toLowerCase(),
                                matchResult.similarity() * 100,
                                matchResult.sourceCode(), matchResult.targetCode())
                ));

                break;
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

        boolean isMatch = soundex.isMatch() || metaphone.isMatch();
        double similarity = Math.max(soundex.similarity(), metaphone.similarity());

        return new MatchResult(
                isMatch,
                similarity,
                soundex.sourceCode() + "/" + metaphone.sourceCode(),
                soundex.targetCode() + "/" + metaphone.targetCode()
        );
    }

    private record MatchResult(boolean isMatch, double similarity, String sourceCode, String targetCode) {}
}
