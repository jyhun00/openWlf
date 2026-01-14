package aml.openwlf.core.matching.strategy;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Set;

/**
 * N-Gram 기반 매칭 전략
 *
 * 부분 문자열 기반 매칭으로 오타에 강합니다.
 * Jaccard 유사도를 사용합니다.
 */
@Component
public class NGramMatchingStrategy implements MatchingStrategy {

    private static final int DEFAULT_N = 2; // Bigram
    private static final double DEFAULT_THRESHOLD = 0.5;

    @Override
    public String getStrategyName() {
        return "NGRAM";
    }

    @Override
    public double calculateSimilarity(String str1, String str2) {
        return calculateNGramSimilarity(str1, str2, DEFAULT_N);
    }

    @Override
    public boolean matches(String str1, String str2) {
        return calculateSimilarity(str1, str2) >= DEFAULT_THRESHOLD;
    }

    /**
     * N-Gram 유사도 계산 (0.0 ~ 1.0)
     *
     * @param str1 첫 번째 문자열
     * @param str2 두 번째 문자열
     * @param n N-Gram의 N 값
     * @return Jaccard 유사도
     */
    public double calculateNGramSimilarity(String str1, String str2, int n) {
        if (str1 == null || str2 == null) {
            return 0.0;
        }

        String s1 = normalizeForMatching(str1);
        String s2 = normalizeForMatching(str2);

        if (s1.length() < n || s2.length() < n) {
            return s1.equals(s2) ? 1.0 : 0.0;
        }

        Set<String> ngrams1 = generateNGrams(s1, n);
        Set<String> ngrams2 = generateNGrams(s2, n);

        Set<String> intersection = new HashSet<>(ngrams1);
        intersection.retainAll(ngrams2);

        Set<String> union = new HashSet<>(ngrams1);
        union.addAll(ngrams2);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }

    /**
     * Bigram 유사도 (n=2)
     */
    public double calculateBigramSimilarity(String str1, String str2) {
        return calculateNGramSimilarity(str1, str2, 2);
    }

    /**
     * Trigram 유사도 (n=3)
     */
    public double calculateTrigramSimilarity(String str1, String str2) {
        return calculateNGramSimilarity(str1, str2, 3);
    }

    /**
     * N-Gram 집합 생성
     *
     * @param str 대상 문자열
     * @param n N 값
     * @return N-Gram 집합
     */
    public Set<String> generateNGrams(String str, int n) {
        Set<String> ngrams = new HashSet<>();

        String padded = "_" + str.replace(" ", "_") + "_";

        for (int i = 0; i <= padded.length() - n; i++) {
            ngrams.add(padded.substring(i, i + n));
        }

        return ngrams;
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
