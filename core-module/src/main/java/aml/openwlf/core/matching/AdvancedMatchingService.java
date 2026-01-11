package aml.openwlf.core.matching;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.language.DoubleMetaphone;
import org.apache.commons.codec.language.Soundex;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 고급 매칭 알고리즘 서비스
 * 
 * 다양한 매칭 알고리즘을 제공하여 이름 매칭의 정확도를 높입니다.
 * - Soundex: 영어 발음 기반 매칭
 * - Double Metaphone: 개선된 발음 매칭 (다국어 지원)
 * - Jaro-Winkler: 이름 매칭에 최적화된 유사도
 * - N-Gram: 부분 문자열 기반 매칭
 * - Korean: 한글 이름 특화 매칭
 */
@Slf4j
@Service
public class AdvancedMatchingService {
    
    private final Soundex soundex = new Soundex();
    private final DoubleMetaphone doubleMetaphone = new DoubleMetaphone();
    private final JaroWinklerSimilarity jaroWinkler = new JaroWinklerSimilarity();
    
    // ==================== Soundex ====================
    
    /**
     * Soundex 코드 생성
     * 영어 이름의 발음을 4자리 코드로 변환
     * 예: "Robert" -> "R163", "Rupert" -> "R163"
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
            
            // 각 단어별 Soundex 코드 생성 후 결합
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
    
    /**
     * Soundex 매칭 여부 확인
     */
    public boolean matchesSoundex(String name1, String name2) {
        String code1 = getSoundexCode(name1);
        String code2 = getSoundexCode(name2);
        
        if (code1.isEmpty() || code2.isEmpty()) {
            return false;
        }
        
        // 전체 코드 일치 또는 개별 단어 코드 중 하나라도 일치
        if (code1.equals(code2)) {
            return true;
        }
        
        Set<String> codes1 = new HashSet<>(Arrays.asList(code1.split("-")));
        Set<String> codes2 = new HashSet<>(Arrays.asList(code2.split("-")));
        
        // 교집합이 있으면 부분 매칭
        codes1.retainAll(codes2);
        return !codes1.isEmpty();
    }
    
    /**
     * Soundex 유사도 계산 (0.0 ~ 1.0)
     */
    public double calculateSoundexSimilarity(String name1, String name2) {
        String code1 = getSoundexCode(name1);
        String code2 = getSoundexCode(name2);
        
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
    
    // ==================== Double Metaphone ====================
    
    /**
     * Double Metaphone 코드 생성
     * Soundex보다 정확하고 다국어 지원이 더 좋음
     * Primary와 Alternate 두 개의 코드 반환
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
    
    /**
     * Double Metaphone 매칭 여부 확인
     */
    public boolean matchesMetaphone(String name1, String name2) {
        MetaphoneResult result1 = getMetaphoneCode(name1);
        MetaphoneResult result2 = getMetaphoneCode(name2);
        
        if (result1.isEmpty() || result2.isEmpty()) {
            return false;
        }
        
        // Primary 또는 Alternate 코드가 일치하면 매칭
        return result1.primary().equals(result2.primary()) ||
               result1.primary().equals(result2.alternate()) ||
               result1.alternate().equals(result2.primary()) ||
               result1.alternate().equals(result2.alternate()) ||
               hasCommonMetaphoneToken(result1, result2);
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
    
    /**
     * Double Metaphone 유사도 계산 (0.0 ~ 1.0)
     */
    public double calculateMetaphoneSimilarity(String name1, String name2) {
        MetaphoneResult result1 = getMetaphoneCode(name1);
        MetaphoneResult result2 = getMetaphoneCode(name2);
        
        if (result1.isEmpty() || result2.isEmpty()) {
            return 0.0;
        }
        
        // 완전 일치
        if (result1.primary().equals(result2.primary())) {
            return 1.0;
        }
        
        // Alternate 일치
        if (result1.primary().equals(result2.alternate()) ||
            result1.alternate().equals(result2.primary())) {
            return 0.9;
        }
        
        // 토큰 기반 유사도
        Set<String> tokens1 = new HashSet<>();
        tokens1.addAll(Arrays.asList(result1.primary().split("-")));
        
        Set<String> tokens2 = new HashSet<>();
        tokens2.addAll(Arrays.asList(result2.primary().split("-")));
        
        int totalTokens = Math.max(tokens1.size(), tokens2.size());
        Set<String> intersection = new HashSet<>(tokens1);
        intersection.retainAll(tokens2);
        intersection.remove("");
        
        return (double) intersection.size() / totalTokens;
    }
    
    // ==================== Jaro-Winkler ====================
    
    /**
     * Jaro-Winkler 유사도 계산 (0.0 ~ 1.0)
     * 이름 매칭에 특히 효과적 (앞부분 일치에 가중치)
     */
    public double calculateJaroWinklerSimilarity(String str1, String str2) {
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
    
    /**
     * 이름 토큰별 Jaro-Winkler 유사도 계산
     * 이름 순서가 다른 경우에도 매칭 가능
     */
    public double calculateTokenJaroWinklerSimilarity(String name1, String name2) {
        if (name1 == null || name2 == null) {
            return 0.0;
        }
        
        String[] tokens1 = normalizeForMatching(name1).split("\\s+");
        String[] tokens2 = normalizeForMatching(name2).split("\\s+");
        
        if (tokens1.length == 0 || tokens2.length == 0) {
            return 0.0;
        }
        
        // 각 토큰에 대해 가장 유사한 토큰과의 유사도 계산
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
    
    // ==================== N-Gram ====================
    
    /**
     * N-Gram 유사도 계산 (0.0 ~ 1.0)
     * 부분 문자열 기반 매칭으로 오타에 강함
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
        
        // Jaccard 유사도
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
    
    private Set<String> generateNGrams(String str, int n) {
        Set<String> ngrams = new HashSet<>();
        
        // 패딩 추가 (단어 경계 구분)
        String padded = "_" + str.replace(" ", "_") + "_";
        
        for (int i = 0; i <= padded.length() - n; i++) {
            ngrams.add(padded.substring(i, i + n));
        }
        
        return ngrams;
    }
    
    // ==================== Korean Name Matching ====================
    
    /**
     * 한글 이름 유사도 계산
     * 초성 매칭, 자모 분리 매칭 지원
     */
    public double calculateKoreanNameSimilarity(String name1, String name2) {
        if (name1 == null || name2 == null) {
            return 0.0;
        }
        
        // 한글이 포함된 경우에만 한글 매칭 적용
        if (!containsKorean(name1) || !containsKorean(name2)) {
            return 0.0;
        }
        
        String korean1 = extractKorean(name1);
        String korean2 = extractKorean(name2);
        
        // 정확히 일치
        if (korean1.equals(korean2)) {
            return 1.0;
        }
        
        // 초성 일치 확인
        String chosung1 = extractChosung(korean1);
        String chosung2 = extractChosung(korean2);
        
        if (chosung1.equals(chosung2) && !chosung1.isEmpty()) {
            return 0.8;
        }
        
        // 자모 분리 후 Jaro-Winkler 유사도
        String jamo1 = decomposeToJamo(korean1);
        String jamo2 = decomposeToJamo(korean2);
        
        return jaroWinkler.apply(jamo1, jamo2) * 0.9;
    }
    
    /**
     * 초성 추출
     * 예: "김철수" -> "ㄱㅊㅅ"
     */
    public String extractChosung(String korean) {
        if (korean == null) return "";
        
        char[] chosungList = {
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
            'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
        };
        
        StringBuilder result = new StringBuilder();
        
        for (char c : korean.toCharArray()) {
            if (c >= 0xAC00 && c <= 0xD7A3) {
                int index = (c - 0xAC00) / (21 * 28);
                result.append(chosungList[index]);
            }
        }
        
        return result.toString();
    }
    
    /**
     * 한글을 자모로 분리
     * 예: "김" -> "ㄱㅣㅁ"
     */
    public String decomposeToJamo(String korean) {
        if (korean == null) return "";
        
        char[] chosungList = {
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
            'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
        };
        char[] jungsungList = {
            'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ',
            'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ'
        };
        char[] jongsungList = {
            '\0', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ',
            'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ',
            'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
        };
        
        StringBuilder result = new StringBuilder();
        
        for (char c : korean.toCharArray()) {
            if (c >= 0xAC00 && c <= 0xD7A3) {
                int base = c - 0xAC00;
                int cho = base / (21 * 28);
                int jung = (base % (21 * 28)) / 28;
                int jong = base % 28;
                
                result.append(chosungList[cho]);
                result.append(jungsungList[jung]);
                if (jong != 0) {
                    result.append(jongsungList[jong]);
                }
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    /**
     * 초성 매칭 여부 확인
     */
    public boolean matchesChosung(String name1, String name2) {
        String chosung1 = extractChosung(extractKorean(name1));
        String chosung2 = extractChosung(extractKorean(name2));
        
        return !chosung1.isEmpty() && chosung1.equals(chosung2);
    }
    
    private boolean containsKorean(String str) {
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
    
    // ==================== 복합 매칭 ====================
    
    /**
     * 복합 유사도 계산
     * 여러 알고리즘의 결과를 가중 평균하여 최종 유사도 산출
     */
    public CompositeMatchResult calculateCompositeMatch(String name1, String name2) {
        double jaroWinklerScore = calculateTokenJaroWinklerSimilarity(name1, name2);
        double metaphoneScore = calculateMetaphoneSimilarity(name1, name2);
        double ngramScore = calculateBigramSimilarity(name1, name2);
        double koreanScore = calculateKoreanNameSimilarity(name1, name2);
        
        // 가중 평균 (한글이 있으면 한글 점수 반영)
        double compositScore;
        if (koreanScore > 0) {
            compositScore = (jaroWinklerScore * 0.3 + metaphoneScore * 0.2 + 
                           ngramScore * 0.2 + koreanScore * 0.3);
        } else {
            compositScore = (jaroWinklerScore * 0.4 + metaphoneScore * 0.3 + ngramScore * 0.3);
        }
        
        return new CompositeMatchResult(
                compositScore,
                jaroWinklerScore,
                metaphoneScore,
                ngramScore,
                koreanScore,
                matchesMetaphone(name1, name2),
                matchesSoundex(name1, name2)
        );
    }
    
    // ==================== 유틸리티 ====================
    
    private String normalizeForPhonetic(String str) {
        if (str == null) return "";
        
        // 발음 매칭을 위해 영어만 추출
        String normalized = Normalizer.normalize(str, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");
        
        return normalized.toUpperCase()
                .replaceAll("[^A-Z\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
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
    
    // ==================== Result Records ====================
    
    public record MetaphoneResult(String primary, String alternate) {
        public boolean isEmpty() {
            return (primary == null || primary.isEmpty()) && 
                   (alternate == null || alternate.isEmpty());
        }
    }
    
    public record CompositeMatchResult(
            double compositeScore,
            double jaroWinklerScore,
            double metaphoneScore,
            double ngramScore,
            double koreanScore,
            boolean metaphoneMatch,
            boolean soundexMatch
    ) {
        public boolean isHighConfidenceMatch(double threshold) {
            return compositeScore >= threshold || metaphoneMatch;
        }
    }
}
