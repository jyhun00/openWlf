package aml.openwlf.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 매칭 알고리즘 테스트 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "매칭 알고리즘 테스트 결과")
public class MatchingTestResponse {
    
    @Schema(description = "입력된 첫 번째 이름")
    private String name1;
    
    @Schema(description = "입력된 두 번째 이름")
    private String name2;
    
    @Schema(description = "Soundex 결과")
    private SoundexResult soundex;
    
    @Schema(description = "Double Metaphone 결과")
    private MetaphoneResult metaphone;
    
    @Schema(description = "Jaro-Winkler 결과")
    private JaroWinklerResult jaroWinkler;
    
    @Schema(description = "N-Gram 결과")
    private NGramResult ngram;
    
    @Schema(description = "한글 매칭 결과 (한글이 포함된 경우)")
    private KoreanResult korean;
    
    @Schema(description = "복합 매칭 결과")
    private CompositeResult composite;
    
    @Schema(description = "종합 판정")
    private String summary;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Soundex 매칭 결과")
    public static class SoundexResult {
        @Schema(description = "첫 번째 이름 Soundex 코드", example = "M530")
        private String code1;
        
        @Schema(description = "두 번째 이름 Soundex 코드", example = "M530")
        private String code2;
        
        @Schema(description = "매칭 여부")
        private boolean matched;
        
        @Schema(description = "유사도 (0.0 ~ 1.0)", example = "1.0")
        private double similarity;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Double Metaphone 매칭 결과")
    public static class MetaphoneResult {
        @Schema(description = "첫 번째 이름 Primary 코드")
        private String primary1;
        
        @Schema(description = "첫 번째 이름 Alternate 코드")
        private String alternate1;
        
        @Schema(description = "두 번째 이름 Primary 코드")
        private String primary2;
        
        @Schema(description = "두 번째 이름 Alternate 코드")
        private String alternate2;
        
        @Schema(description = "매칭 여부")
        private boolean matched;
        
        @Schema(description = "유사도 (0.0 ~ 1.0)")
        private double similarity;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Jaro-Winkler 매칭 결과")
    public static class JaroWinklerResult {
        @Schema(description = "전체 문자열 유사도", example = "0.95")
        private double fullStringSimilarity;
        
        @Schema(description = "토큰 기반 유사도 (순서 무관)", example = "0.98")
        private double tokenSimilarity;
        
        @Schema(description = "매칭 여부 (0.85 이상)")
        private boolean matched;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "N-Gram 매칭 결과")
    public static class NGramResult {
        @Schema(description = "Bigram (n=2) 유사도", example = "0.75")
        private double bigramSimilarity;
        
        @Schema(description = "Trigram (n=3) 유사도", example = "0.68")
        private double trigramSimilarity;
        
        @Schema(description = "매칭 여부 (0.6 이상)")
        private boolean matched;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "한글 매칭 결과")
    public static class KoreanResult {
        @Schema(description = "한글 포함 여부")
        private boolean containsKorean;
        
        @Schema(description = "첫 번째 이름 초성", example = "ㄱㅊㅅ")
        private String chosung1;
        
        @Schema(description = "두 번째 이름 초성", example = "ㄱㅊㅎ")
        private String chosung2;
        
        @Schema(description = "초성 매칭 여부")
        private boolean chosungMatched;
        
        @Schema(description = "한글 유사도", example = "0.8")
        private double similarity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "복합 매칭 결과")
    public static class CompositeResult {
        @Schema(description = "종합 유사도 (가중 평균)", example = "0.85")
        private double compositeScore;

        @Schema(description = "Jaro-Winkler 기여도", example = "0.90")
        private double jaroWinklerScore;

        @Schema(description = "Metaphone 기여도", example = "1.0")
        private double metaphoneScore;

        @Schema(description = "N-Gram 기여도", example = "0.75")
        private double ngramScore;

        @Schema(description = "한글 기여도", example = "0.0")
        private double koreanScore;

        @Schema(description = "발음 매칭 여부")
        private boolean phoneticMatch;

        @Schema(description = "높은 신뢰도 매칭 여부 (0.75 이상 또는 발음 일치)")
        private boolean highConfidenceMatch;
    }
}