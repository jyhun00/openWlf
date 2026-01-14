package aml.openwlf.core.matching.strategy;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.language.Soundex;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Soundex 기반 발음 매칭 전략
 *
 * 영어 이름의 발음을 4자리 코드로 변환하여 비교합니다.
 * 예: "Robert" -> "R163", "Rupert" -> "R163"
 */
@Slf4j
@Component
public class SoundexMatchingStrategy implements MatchingStrategy {

    private final Soundex soundex = new Soundex();

    @Override
    public String getStrategyName() {
        return "SOUNDEX";
    }

    @Override
    public double calculateSimilarity(String str1, String str2) {
        String code1 = getSoundexCode(str1);
        String code2 = getSoundexCode(str2);

        if (code1.isEmpty() || code2.isEmpty()) {
            return 0.0;
        }

        if (code1.equals(code2)) {
            return 1.0;
        }

        Set<String> codes1 = new HashSet<>(Arrays.asList(code1.split("-")));
        Set<String> codes2 = new HashSet<>(Arrays.asList(code2.split("-")));

        int totalCodes = Math.max(codes1.size(), codes2.size());
        Set<String> intersection = new HashSet<>(codes1);
        intersection.retainAll(codes2);

        return (double) intersection.size() / totalCodes;
    }

    @Override
    public boolean matches(String str1, String str2) {
        String code1 = getSoundexCode(str1);
        String code2 = getSoundexCode(str2);

        if (code1.isEmpty() || code2.isEmpty()) {
            return false;
        }

        if (code1.equals(code2)) {
            return true;
        }

        Set<String> codes1 = new HashSet<>(Arrays.asList(code1.split("-")));
        Set<String> codes2 = new HashSet<>(Arrays.asList(code2.split("-")));

        codes1.retainAll(codes2);
        return !codes1.isEmpty();
    }

    /**
     * Soundex 코드 생성
     *
     * @param name 이름
     * @return Soundex 코드 (단어별 '-'로 구분)
     */
    public String getSoundexCode(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }

        try {
            String normalized = normalizeForPhonetic(name);
            if (normalized.isEmpty()) {
                return "";
            }

            return Arrays.stream(normalized.split("\\s+"))
                    .filter(word -> !word.isEmpty())
                    .map(word -> {
                        try {
                            return soundex.encode(word);
                        } catch (Exception e) {
                            return "";
                        }
                    })
                    .filter(code -> !code.isEmpty())
                    .collect(Collectors.joining("-"));
        } catch (Exception e) {
            log.debug("Soundex encoding failed for: {}", name);
            return "";
        }
    }

    @Override
    public boolean isApplicable(String str) {
        if (str == null || str.isBlank()) {
            return false;
        }
        String normalized = normalizeForPhonetic(str);
        return !normalized.isEmpty();
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
}
