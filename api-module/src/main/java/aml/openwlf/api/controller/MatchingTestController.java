package aml.openwlf.api.controller;

import aml.openwlf.api.dto.MatchingTestRequest;
import aml.openwlf.api.dto.MatchingTestResponse;
import aml.openwlf.api.dto.MatchingTestResponse.*;
import aml.openwlf.core.matching.AdvancedMatchingService;
import aml.openwlf.core.matching.CompositeMatchResult;
import aml.openwlf.core.matching.AdvancedMatchingService.MetaphoneResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 고급 매칭 알고리즘 테스트 컨트롤러
 * 
 * 다양한 매칭 알고리즘의 결과를 테스트하고 비교할 수 있습니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/matching")
@RequiredArgsConstructor
@Tag(name = "Matching Test", description = "고급 매칭 알고리즘 테스트 API")
public class MatchingTestController {
    
    private final AdvancedMatchingService matchingService;
    
    @PostMapping("/test")
    @Operation(
            summary = "매칭 알고리즘 테스트",
            description = """
                    두 이름에 대해 다양한 매칭 알고리즘을 실행하고 결과를 비교합니다.
                    
                    **지원 알고리즘:**
                    - **Soundex**: 영어 발음 기반 매칭 (Robert ≈ Rupert)
                    - **Double Metaphone**: 개선된 발음 매칭 (Muhammad ≈ Mohammed)
                    - **Jaro-Winkler**: 이름 매칭에 최적화된 유사도 (오타에 강함)
                    - **N-Gram**: 부분 문자열 기반 매칭 (철자 변형에 강함)
                    - **Korean**: 한글 초성/자모 기반 매칭 (김철수 ≈ 김창수)
                    - **Composite**: 모든 알고리즘의 가중 평균
                    """
    )
    public ResponseEntity<MatchingTestResponse> testMatching(
            @Valid @RequestBody MatchingTestRequest request) {
        
        log.info("Testing matching algorithms: '{}' vs '{}'", request.getName1(), request.getName2());
        
        String name1 = request.getName1();
        String name2 = request.getName2();
        
        // Soundex
        SoundexResult soundexResult = buildSoundexResult(name1, name2);
        
        // Double Metaphone
        aml.openwlf.api.dto.MatchingTestResponse.MetaphoneResult metaphoneResult = 
                buildMetaphoneResult(name1, name2);
        
        // Jaro-Winkler
        JaroWinklerResult jaroWinklerResult = buildJaroWinklerResult(name1, name2);
        
        // N-Gram
        NGramResult ngramResult = buildNGramResult(name1, name2);
        
        // Korean
        KoreanResult koreanResult = buildKoreanResult(name1, name2);
        
        // Composite
        aml.openwlf.api.dto.MatchingTestResponse.CompositeResult compositeResult = 
                buildCompositeResult(name1, name2);
        
        // Summary
        String summary = buildSummary(soundexResult, metaphoneResult, jaroWinklerResult, 
                ngramResult, koreanResult, compositeResult);
        
        MatchingTestResponse response = MatchingTestResponse.builder()
                .name1(name1)
                .name2(name2)
                .soundex(soundexResult)
                .metaphone(metaphoneResult)
                .jaroWinkler(jaroWinklerResult)
                .ngram(ngramResult)
                .korean(koreanResult)
                .composite(compositeResult)
                .summary(summary)
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/algorithms")
    @Operation(
            summary = "지원 알고리즘 목록",
            description = "사용 가능한 매칭 알고리즘 목록과 설명을 반환합니다."
    )
    public ResponseEntity<List<AlgorithmInfo>> getAlgorithms() {
        List<AlgorithmInfo> algorithms = List.of(
                new AlgorithmInfo("SOUNDEX", "Soundex", 
                        "영어 발음 기반 매칭. Robert≈Rupert, Smith≈Smyth", "영어 이름"),
                new AlgorithmInfo("METAPHONE", "Double Metaphone", 
                        "개선된 발음 매칭. Muhammad≈Mohammed, Stephen≈Steven", "다국어 이름"),
                new AlgorithmInfo("JARO_WINKLER", "Jaro-Winkler", 
                        "이름 매칭에 최적화된 유사도. 앞부분 일치에 가중치, 오타에 강함", "일반 이름 매칭"),
                new AlgorithmInfo("NGRAM", "N-Gram (Bigram/Trigram)", 
                        "부분 문자열 기반 매칭. 철자 변형에 강함", "철자가 불확실한 경우"),
                new AlgorithmInfo("KOREAN", "Korean Name Matching", 
                        "한글 초성/자모 기반 매칭. 김철수≈김창수 (초성 ㄱㅊㅅ)", "한글 이름"),
                new AlgorithmInfo("COMPOSITE", "Composite Matching", 
                        "여러 알고리즘의 가중 평균. 가장 정확한 종합 판단", "높은 정확도 필요시")
        );
        
        return ResponseEntity.ok(algorithms);
    }
    
    private SoundexResult buildSoundexResult(String name1, String name2) {
        return SoundexResult.builder()
                .code1(matchingService.getSoundexCode(name1))
                .code2(matchingService.getSoundexCode(name2))
                .matched(matchingService.matchesSoundex(name1, name2))
                .similarity(matchingService.calculateSoundexSimilarity(name1, name2))
                .build();
    }
    
    private aml.openwlf.api.dto.MatchingTestResponse.MetaphoneResult buildMetaphoneResult(
            String name1, String name2) {
        MetaphoneResult mp1 = matchingService.getMetaphoneCode(name1);
        MetaphoneResult mp2 = matchingService.getMetaphoneCode(name2);
        
        return aml.openwlf.api.dto.MatchingTestResponse.MetaphoneResult.builder()
                .primary1(mp1.primary())
                .alternate1(mp1.alternate())
                .primary2(mp2.primary())
                .alternate2(mp2.alternate())
                .matched(matchingService.matchesMetaphone(name1, name2))
                .similarity(matchingService.calculateMetaphoneSimilarity(name1, name2))
                .build();
    }
    
    private JaroWinklerResult buildJaroWinklerResult(String name1, String name2) {
        double fullSimilarity = matchingService.calculateJaroWinklerSimilarity(name1, name2);
        double tokenSimilarity = matchingService.calculateTokenJaroWinklerSimilarity(name1, name2);
        
        return JaroWinklerResult.builder()
                .fullStringSimilarity(fullSimilarity)
                .tokenSimilarity(tokenSimilarity)
                .matched(tokenSimilarity >= 0.85)
                .build();
    }
    
    private NGramResult buildNGramResult(String name1, String name2) {
        double bigram = matchingService.calculateBigramSimilarity(name1, name2);
        double trigram = matchingService.calculateTrigramSimilarity(name1, name2);
        
        return NGramResult.builder()
                .bigramSimilarity(bigram)
                .trigramSimilarity(trigram)
                .matched(bigram >= 0.6)
                .build();
    }
    
    private KoreanResult buildKoreanResult(String name1, String name2) {
        boolean containsKorean = containsKorean(name1) || containsKorean(name2);
        
        if (!containsKorean) {
            return KoreanResult.builder()
                    .containsKorean(false)
                    .build();
        }
        
        String chosung1 = matchingService.extractChosung(name1);
        String chosung2 = matchingService.extractChosung(name2);
        
        return KoreanResult.builder()
                .containsKorean(true)
                .chosung1(chosung1)
                .chosung2(chosung2)
                .chosungMatched(matchingService.matchesChosung(name1, name2))
                .similarity(matchingService.calculateKoreanNameSimilarity(name1, name2))
                .build();
    }
    
    private aml.openwlf.api.dto.MatchingTestResponse.CompositeResult buildCompositeResult(
            String name1, String name2) {
        CompositeMatchResult result = matchingService.calculateCompositeMatch(name1, name2);
        
        return aml.openwlf.api.dto.MatchingTestResponse.CompositeResult.builder()
                .compositeScore(result.compositeScore())
                .jaroWinklerScore(result.jaroWinklerScore())
                .metaphoneScore(result.metaphoneScore())
                .ngramScore(result.ngramScore())
                .koreanScore(result.koreanScore())
                .phoneticMatch(result.metaphoneMatch())
                .highConfidenceMatch(result.isHighConfidenceMatch(0.75))
                .build();
    }
    
    private String buildSummary(SoundexResult soundex, 
                                aml.openwlf.api.dto.MatchingTestResponse.MetaphoneResult metaphone,
                                JaroWinklerResult jaroWinkler, 
                                NGramResult ngram,
                                KoreanResult korean, 
                                aml.openwlf.api.dto.MatchingTestResponse.CompositeResult composite) {
        
        List<String> matches = new ArrayList<>();
        
        if (soundex.isMatched()) matches.add("발음(Soundex)");
        if (metaphone.isMatched()) matches.add("발음(Metaphone)");
        if (jaroWinkler.isMatched()) matches.add("Jaro-Winkler");
        if (ngram.isMatched()) matches.add("N-Gram");
        if (korean.isContainsKorean() && korean.isChosungMatched()) matches.add("한글 초성");
        
        if (composite.isHighConfidenceMatch()) {
            return String.format("⚠️ 높은 유사도 감지! (종합 %.0f%%) - 매칭 알고리즘: %s", 
                    composite.getCompositeScore() * 100,
                    matches.isEmpty() ? "없음" : String.join(", ", matches));
        } else if (!matches.isEmpty()) {
            return String.format("⚡ 부분 매칭 감지 (종합 %.0f%%) - 매칭 알고리즘: %s",
                    composite.getCompositeScore() * 100,
                    String.join(", ", matches));
        } else {
            return String.format("✅ 낮은 유사도 (종합 %.0f%%) - 매칭 없음",
                    composite.getCompositeScore() * 100);
        }
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
    
    public record AlgorithmInfo(String code, String name, String description, String bestFor) {}
}
