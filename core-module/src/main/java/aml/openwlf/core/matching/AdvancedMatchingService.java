package aml.openwlf.core.matching;

import aml.openwlf.core.matching.strategy.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 고급 매칭 알고리즘 서비스 (Facade)
 *
 * 다양한 매칭 전략을 조합하여 이름 매칭의 정확도를 높입니다.
 * 내부적으로 전략 패턴을 사용하여 각 알고리즘을 캡슐화합니다.
 *
 * 지원 전략:
 * - Soundex: 영어 발음 기반 매칭
 * - Double Metaphone: 개선된 발음 매칭 (다국어 지원)
 * - Jaro-Winkler: 이름 매칭에 최적화된 유사도
 * - N-Gram: 부분 문자열 기반 매칭
 * - Korean: 한글 이름 특화 매칭
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdvancedMatchingService {

    private final SoundexMatchingStrategy soundexStrategy;
    private final MetaphoneMatchingStrategy metaphoneStrategy;
    private final JaroWinklerMatchingStrategy jaroWinklerStrategy;
    private final NGramMatchingStrategy ngramStrategy;
    private final KoreanNameMatchingStrategy koreanStrategy;

    // ==================== Soundex ====================

    /**
     * Soundex 코드 생성
     */
    public String getSoundexCode(String name) {
        return soundexStrategy.getSoundexCode(name);
    }

    /**
     * Soundex 매칭 여부 확인
     */
    public boolean matchesSoundex(String name1, String name2) {
        return soundexStrategy.matches(name1, name2);
    }

    /**
     * Soundex 유사도 계산 (0.0 ~ 1.0)
     */
    public double calculateSoundexSimilarity(String name1, String name2) {
        return soundexStrategy.calculateSimilarity(name1, name2);
    }

    // ==================== Double Metaphone ====================

    /**
     * Double Metaphone 코드 생성
     */
    public MetaphoneResult getMetaphoneCode(String name) {
        var result = metaphoneStrategy.getMetaphoneCode(name);
        return new MetaphoneResult(result.primary(), result.alternate());
    }

    /**
     * Double Metaphone 매칭 여부 확인
     */
    public boolean matchesMetaphone(String name1, String name2) {
        return metaphoneStrategy.matches(name1, name2);
    }

    /**
     * Double Metaphone 유사도 계산 (0.0 ~ 1.0)
     */
    public double calculateMetaphoneSimilarity(String name1, String name2) {
        return metaphoneStrategy.calculateSimilarity(name1, name2);
    }

    // ==================== Jaro-Winkler ====================

    /**
     * Jaro-Winkler 유사도 계산 (0.0 ~ 1.0)
     */
    public double calculateJaroWinklerSimilarity(String str1, String str2) {
        return jaroWinklerStrategy.calculateSimilarity(str1, str2);
    }

    /**
     * 이름 토큰별 Jaro-Winkler 유사도 계산
     */
    public double calculateTokenJaroWinklerSimilarity(String name1, String name2) {
        return jaroWinklerStrategy.calculateTokenSimilarity(name1, name2);
    }

    // ==================== N-Gram ====================

    /**
     * N-Gram 유사도 계산 (0.0 ~ 1.0)
     */
    public double calculateNGramSimilarity(String str1, String str2, int n) {
        return ngramStrategy.calculateNGramSimilarity(str1, str2, n);
    }

    /**
     * Bigram 유사도 (n=2)
     */
    public double calculateBigramSimilarity(String str1, String str2) {
        return ngramStrategy.calculateBigramSimilarity(str1, str2);
    }

    /**
     * Trigram 유사도 (n=3)
     */
    public double calculateTrigramSimilarity(String str1, String str2) {
        return ngramStrategy.calculateTrigramSimilarity(str1, str2);
    }

    // ==================== Korean Name Matching ====================

    /**
     * 한글 이름 유사도 계산
     */
    public double calculateKoreanNameSimilarity(String name1, String name2) {
        return koreanStrategy.calculateSimilarity(name1, name2);
    }

    /**
     * 초성 추출
     */
    public String extractChosung(String korean) {
        return koreanStrategy.extractChosung(korean);
    }

    /**
     * 한글을 자모로 분리
     */
    public String decomposeToJamo(String korean) {
        return koreanStrategy.decomposeToJamo(korean);
    }

    /**
     * 초성 매칭 여부 확인
     */
    public boolean matchesChosung(String name1, String name2) {
        return koreanStrategy.matchesChosung(name1, name2);
    }

    // ==================== 복합 매칭 ====================

    /**
     * 복합 유사도 계산
     * 여러 알고리즘의 결과를 가중 평균하여 최종 유사도 산출
     */
    public CompositeMatchResult calculateCompositeMatch(String name1, String name2) {
        double jaroWinklerScore = jaroWinklerStrategy.calculateTokenSimilarity(name1, name2);
        double metaphoneScore = metaphoneStrategy.calculateSimilarity(name1, name2);
        double ngramScore = ngramStrategy.calculateBigramSimilarity(name1, name2);
        double koreanScore = koreanStrategy.calculateSimilarity(name1, name2);

        // 가중 평균 (한글이 있으면 한글 점수 반영)
        double compositeScore;
        if (koreanScore > 0) {
            compositeScore = (jaroWinklerScore * 0.3 + metaphoneScore * 0.2 +
                    ngramScore * 0.2 + koreanScore * 0.3);
        } else {
            compositeScore = (jaroWinklerScore * 0.4 + metaphoneScore * 0.3 + ngramScore * 0.3);
        }

        return new CompositeMatchResult(
                compositeScore,
                jaroWinklerScore,
                metaphoneScore,
                ngramScore,
                koreanScore,
                metaphoneStrategy.matches(name1, name2),
                soundexStrategy.matches(name1, name2)
        );
    }

    // ==================== Result Records ====================

    /**
     * Metaphone 결과 레코드 (하위 호환성 유지)
     */
    public record MetaphoneResult(String primary, String alternate) {
        public boolean isEmpty() {
            return (primary == null || primary.isEmpty()) &&
                    (alternate == null || alternate.isEmpty());
        }
    }
}
