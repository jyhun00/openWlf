package aml.openwlf.core.matching.strategy;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.stereotype.Component;

import java.text.Normalizer;

/**
 * Jaro-Winkler 유사도 기반 매칭 전략
 *
 * 이름 매칭에 특히 효과적입니다 (앞부분 일치에 가중치 부여).
 */
@Component
public class JaroWinklerMatchingStrategy implements MatchingStrategy {

    private final JaroWinklerSimilarity jaroWinkler = new JaroWinklerSimilarity();
    private static final double DEFAULT_THRESHOLD = 0.85;

    @Override
    public String getStrategyName() {
        return "JARO_WINKLER";
    }

    @Override
    public double calculateSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return 0.0;
        }

        String s1 = normalizeForMatching(str1);
        String s2 = normalizeForMatching(str2);

        if (s1.isEmpty() || s2.isEmpty()) {
            return 0.0;
        }

        return jaroWinkler.apply(s1, s2);
    }

    @Override
    public boolean matches(String str1, String str2) {
        return calculateSimilarity(str1, str2) >= DEFAULT_THRESHOLD;
    }

    /**
     * 이름 토큰별 Jaro-Winkler 유사도 계산
     * 이름 순서가 다른 경우에도 매칭 가능
     *
     * @param name1 첫 번째 이름
     * @param name2 두 번째 이름
     * @return 토큰 기반 유사도
     */
    public double calculateTokenSimilarity(String name1, String name2) {
        if (name1 == null || name2 == null) {
            return 0.0;
        }

        String[] tokens1 = normalizeForMatching(name1).split("\\s+");
        String[] tokens2 = normalizeForMatching(name2).split("\\s+");

        if (tokens1.length == 0 || tokens2.length == 0) {
            return 0.0;
        }

        double totalSimilarity = 0.0;
        boolean[] used = new boolean[tokens2.length];

        for (String token1 : tokens1) {
            double maxSim = 0.0;
            int maxIndex = -1;

            for (int j = 0; j < tokens2.length; j++) {
                if (!used[j]) {
                    double sim = jaroWinkler.apply(token1, tokens2[j]);
                    if (sim > maxSim) {
                        maxSim = sim;
                        maxIndex = j;
                    }
                }
            }

            if (maxIndex >= 0) {
                used[maxIndex] = true;
                totalSimilarity += maxSim;
            }
        }

        return totalSimilarity / Math.max(tokens1.length, tokens2.length);
    }

    private String normalizeForMatching(String str) {
        if (str == null) return "";

        String normalized = Normalizer.normalize(str, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");

        return normalized.toUpperCase()
                .replaceAll("[^A-Z0-9가-힣\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
