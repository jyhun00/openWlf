package aml.openwlf.core.matching.strategy;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.language.DoubleMetaphone;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Double Metaphone 기반 발음 매칭 전략
 *
 * Soundex보다 정확하고 다국어 지원이 더 좋습니다.
 * Primary와 Alternate 두 개의 코드를 반환합니다.
 */
@Slf4j
@Component
public class MetaphoneMatchingStrategy implements MatchingStrategy {

    private final DoubleMetaphone doubleMetaphone = new DoubleMetaphone();

    @Override
    public String getStrategyName() {
        return "METAPHONE";
    }

    @Override
    public double calculateSimilarity(String str1, String str2) {
        MetaphoneResult result1 = getMetaphoneCode(str1);
        MetaphoneResult result2 = getMetaphoneCode(str2);

        if (result1.isEmpty() || result2.isEmpty()) {
            return 0.0;
        }

        if (result1.primary().equals(result2.primary())) {
            return 1.0;
        }

        if (result1.primary().equals(result2.alternate()) ||
                result1.alternate().equals(result2.primary())) {
            return 0.9;
        }

        Set<String> tokens1 = new HashSet<>(Arrays.asList(result1.primary().split("-")));
        Set<String> tokens2 = new HashSet<>(Arrays.asList(result2.primary().split("-")));

        int totalTokens = Math.max(tokens1.size(), tokens2.size());
        Set<String> intersection = new HashSet<>(tokens1);
        intersection.retainAll(tokens2);
        intersection.remove("");

        return (double) intersection.size() / totalTokens;
    }

    @Override
    public boolean matches(String str1, String str2) {
        MetaphoneResult result1 = getMetaphoneCode(str1);
        MetaphoneResult result2 = getMetaphoneCode(str2);

        if (result1.isEmpty() || result2.isEmpty()) {
            return false;
        }

        return result1.primary().equals(result2.primary()) ||
                result1.primary().equals(result2.alternate()) ||
                result1.alternate().equals(result2.primary()) ||
                result1.alternate().equals(result2.alternate()) ||
                hasCommonMetaphoneToken(result1, result2);
    }

    /**
     * Double Metaphone 코드 생성
     *
     * @param name 이름
     * @return MetaphoneResult (primary, alternate)
     */
    public MetaphoneResult getMetaphoneCode(String name) {
        if (name == null || name.isBlank()) {
            return new MetaphoneResult("", "");
        }

        try {
            String normalized = normalizeForPhonetic(name);
            if (normalized.isEmpty()) {
                return new MetaphoneResult("", "");
            }

            StringBuilder primary = new StringBuilder();
            StringBuilder alternate = new StringBuilder();

            for (String word : normalized.split("\\s+")) {
                if (!word.isEmpty()) {
                    String p = doubleMetaphone.doubleMetaphone(word, false);
                    String a = doubleMetaphone.doubleMetaphone(word, true);

                    if (p != null) primary.append(p).append("-");
                    if (a != null) alternate.append(a).append("-");
                }
            }

            return new MetaphoneResult(
                    primary.length() > 0 ? primary.substring(0, primary.length() - 1) : "",
                    alternate.length() > 0 ? alternate.substring(0, alternate.length() - 1) : ""
            );
        } catch (Exception e) {
            log.debug("Double Metaphone encoding failed for: {}", name);
            return new MetaphoneResult("", "");
        }
    }

    private boolean hasCommonMetaphoneToken(MetaphoneResult r1, MetaphoneResult r2) {
        Set<String> tokens1 = new HashSet<>();
        tokens1.addAll(Arrays.asList(r1.primary().split("-")));
        tokens1.addAll(Arrays.asList(r1.alternate().split("-")));

        Set<String> tokens2 = new HashSet<>();
        tokens2.addAll(Arrays.asList(r2.primary().split("-")));
        tokens2.addAll(Arrays.asList(r2.alternate().split("-")));

        tokens1.retainAll(tokens2);
        tokens1.remove("");

        return !tokens1.isEmpty();
    }

    private String normalizeForPhonetic(String str) {
        if (str == null) return "";

        String normalized = Normalizer.normalize(str, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");

        return normalized.toUpperCase()
                .replaceAll("[^A-Z\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public record MetaphoneResult(String primary, String alternate) {
        public boolean isEmpty() {
            return (primary == null || primary.isEmpty()) &&
                    (alternate == null || alternate.isEmpty());
        }
    }
}
