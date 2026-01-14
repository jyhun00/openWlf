package aml.openwlf.core.matching.strategy;

import lombok.RequiredArgsConstructor;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.stereotype.Component;

/**
 * 한글 이름 특화 매칭 전략
 *
 * 한글 이름의 특성을 고려한 매칭 알고리즘입니다.
 * - 초성 매칭: "김철수" ≈ "김창수" (ㄱㅊㅅ)
 * - 자모 분리 매칭: 오타에 강함
 */
@Component
@RequiredArgsConstructor
public class KoreanNameMatchingStrategy implements MatchingStrategy {

    private final JaroWinklerSimilarity jaroWinkler = new JaroWinklerSimilarity();

    private static final char[] CHOSUNG_LIST = {
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
            'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    private static final char[] JUNGSUNG_LIST = {
            'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ',
            'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ'
    };

    private static final char[] JONGSUNG_LIST = {
            '\0', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ',
            'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ',
            'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    @Override
    public String getStrategyName() {
        return "KOREAN";
    }

    @Override
    public double calculateSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return 0.0;
        }

        if (!containsKorean(str1) || !containsKorean(str2)) {
            return 0.0;
        }

        String korean1 = extractKorean(str1);
        String korean2 = extractKorean(str2);

        if (korean1.equals(korean2)) {
            return 1.0;
        }

        String chosung1 = extractChosung(korean1);
        String chosung2 = extractChosung(korean2);

        if (chosung1.equals(chosung2) && !chosung1.isEmpty()) {
            return 0.8;
        }

        String jamo1 = decomposeToJamo(korean1);
        String jamo2 = decomposeToJamo(korean2);

        return jaroWinkler.apply(jamo1, jamo2) * 0.9;
    }

    @Override
    public boolean matches(String str1, String str2) {
        return calculateSimilarity(str1, str2) >= 0.7;
    }

    @Override
    public boolean isApplicable(String str) {
        return containsKorean(str);
    }

    /**
     * 초성 추출
     * 예: "김철수" -> "ㄱㅊㅅ"
     *
     * @param korean 한글 문자열
     * @return 초성 문자열
     */
    public String extractChosung(String korean) {
        if (korean == null) return "";

        StringBuilder result = new StringBuilder();

        for (char c : korean.toCharArray()) {
            if (c >= 0xAC00 && c <= 0xD7A3) {
                int index = (c - 0xAC00) / (21 * 28);
                result.append(CHOSUNG_LIST[index]);
            }
        }

        return result.toString();
    }

    /**
     * 한글을 자모로 분리
     * 예: "김" -> "ㄱㅣㅁ"
     *
     * @param korean 한글 문자열
     * @return 자모 분리된 문자열
     */
    public String decomposeToJamo(String korean) {
        if (korean == null) return "";

        StringBuilder result = new StringBuilder();

        for (char c : korean.toCharArray()) {
            if (c >= 0xAC00 && c <= 0xD7A3) {
                int base = c - 0xAC00;
                int cho = base / (21 * 28);
                int jung = (base % (21 * 28)) / 28;
                int jong = base % 28;

                result.append(CHOSUNG_LIST[cho]);
                result.append(JUNGSUNG_LIST[jung]);
                if (jong != 0) {
                    result.append(JONGSUNG_LIST[jong]);
                }
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * 초성 매칭 여부 확인
     *
     * @param str1 첫 번째 문자열
     * @param str2 두 번째 문자열
     * @return 초성 일치 여부
     */
    public boolean matchesChosung(String str1, String str2) {
        String chosung1 = extractChosung(extractKorean(str1));
        String chosung2 = extractChosung(extractKorean(str2));

        return !chosung1.isEmpty() && chosung1.equals(chosung2);
    }

    /**
     * 한글 포함 여부 확인
     *
     * @param str 대상 문자열
     * @return 한글 포함 여부
     */
    public boolean containsKorean(String str) {
        if (str == null) return false;
        for (char c : str.toCharArray()) {
            if (c >= 0xAC00 && c <= 0xD7A3) {
                return true;
            }
        }
        return false;
    }

    private String extractKorean(String str) {
        if (str == null) return "";
        StringBuilder result = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (c >= 0xAC00 && c <= 0xD7A3) {
                result.append(c);
            }
        }
        return result.toString();
    }
}
