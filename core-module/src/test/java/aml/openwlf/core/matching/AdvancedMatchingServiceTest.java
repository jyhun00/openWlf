package aml.openwlf.core.matching;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AdvancedMatchingService 테스트")
class AdvancedMatchingServiceTest {
    
    private AdvancedMatchingService matchingService;
    
    @BeforeEach
    void setUp() {
        matchingService = new AdvancedMatchingService();
    }
    
    @Nested
    @DisplayName("Soundex 테스트")
    class SoundexTest {
        
        @Test
        @DisplayName("동일 발음 이름은 같은 Soundex 코드를 가진다")
        void sameSoundingNamesHaveSameCode() {
            // Robert = Rupert
            assertThat(matchingService.matchesSoundex("Robert", "Rupert")).isTrue();
            
            // Smith = Smyth
            assertThat(matchingService.matchesSoundex("Smith", "Smyth")).isTrue();
        }
        
        @Test
        @DisplayName("완전히 다른 이름은 매칭되지 않는다")
        void differentNamesDoNotMatch() {
            assertThat(matchingService.matchesSoundex("John", "Mary")).isFalse();
        }
        
        @Test
        @DisplayName("Soundex 유사도 계산")
        void soundexSimilarityCalculation() {
            double similarity = matchingService.calculateSoundexSimilarity("Robert", "Rupert");
            assertThat(similarity).isGreaterThanOrEqualTo(0.8);
        }
    }
    
    @Nested
    @DisplayName("Double Metaphone 테스트")
    class MetaphoneTest {
        
        @Test
        @DisplayName("Muhammad 변형들은 매칭된다")
        void muhammadVariationsMatch() {
            assertThat(matchingService.matchesMetaphone("Muhammad", "Mohammed")).isTrue();
            assertThat(matchingService.matchesMetaphone("Muhammad", "Mohamed")).isTrue();
            assertThat(matchingService.matchesMetaphone("Mohammed", "Mohamad")).isTrue();
        }
        
        @Test
        @DisplayName("Stephen과 Steven은 매칭된다")
        void stephenStevenMatch() {
            assertThat(matchingService.matchesMetaphone("Stephen", "Steven")).isTrue();
        }
        
        @Test
        @DisplayName("Catherine 변형들은 매칭된다")
        void catherineVariationsMatch() {
            assertThat(matchingService.matchesMetaphone("Catherine", "Katherine")).isTrue();
            assertThat(matchingService.matchesMetaphone("Catherine", "Kathryn")).isTrue();
        }
        
        @Test
        @DisplayName("Metaphone 코드 생성")
        void metaphoneCodeGeneration() {
            var result = matchingService.getMetaphoneCode("Muhammad");
            assertThat(result.primary()).isNotEmpty();
            assertThat(result.isEmpty()).isFalse();
        }
    }
    
    @Nested
    @DisplayName("Jaro-Winkler 테스트")
    class JaroWinklerTest {
        
        @Test
        @DisplayName("동일 문자열은 유사도 1.0")
        void identicalStringsHaveSimilarityOne() {
            double similarity = matchingService.calculateJaroWinklerSimilarity("John Smith", "John Smith");
            assertThat(similarity).isEqualTo(1.0);
        }
        
        @Test
        @DisplayName("오타가 있는 이름도 높은 유사도")
        void typosStillHaveHighSimilarity() {
            double similarity = matchingService.calculateJaroWinklerSimilarity("John Smith", "Jon Smith");
            assertThat(similarity).isGreaterThan(0.9);
        }
        
        @Test
        @DisplayName("토큰 기반 매칭 - 순서가 달라도 높은 유사도")
        void tokenMatchingIgnoresOrder() {
            double similarity = matchingService.calculateTokenJaroWinklerSimilarity("John Smith", "Smith John");
            assertThat(similarity).isGreaterThan(0.9);
        }
        
        @Test
        @DisplayName("완전히 다른 이름은 낮은 유사도")
        void differentNamesHaveLowSimilarity() {
            double similarity = matchingService.calculateJaroWinklerSimilarity("John Smith", "Mary Johnson");
            assertThat(similarity).isLessThan(0.7);
        }
    }
    
    @Nested
    @DisplayName("N-Gram 테스트")
    class NGramTest {
        
        @Test
        @DisplayName("Bigram 유사도 계산")
        void bigramSimilarity() {
            double similarity = matchingService.calculateBigramSimilarity("Johnson", "Jonson");
            assertThat(similarity).isGreaterThan(0.7);
        }
        
        @Test
        @DisplayName("Trigram 유사도 계산")
        void trigramSimilarity() {
            double similarity = matchingService.calculateTrigramSimilarity("Anderson", "Andersen");
            // Jaccard 유사도는 교집합/합집합이므로 유사한 문자열도 0.4~0.5 정도
            assertThat(similarity).isGreaterThan(0.4);
        }
        
        @Test
        @DisplayName("동일 문자열은 유사도 1.0")
        void identicalStringsHaveSimilarityOne() {
            double similarity = matchingService.calculateBigramSimilarity("Test", "Test");
            assertThat(similarity).isEqualTo(1.0);
        }
    }
    
    @Nested
    @DisplayName("한글 이름 매칭 테스트")
    class KoreanNameTest {
        
        @Test
        @DisplayName("초성 추출")
        void chosungExtraction() {
            String chosung = matchingService.extractChosung("김철수");
            assertThat(chosung).isEqualTo("ㄱㅊㅅ");
        }
        
        @Test
        @DisplayName("같은 초성은 매칭된다")
        void sameChosungMatches() {
            assertThat(matchingService.matchesChosung("김철수", "김창수")).isTrue();
            assertThat(matchingService.matchesChosung("박지민", "박준민")).isTrue();
        }
        
        @Test
        @DisplayName("다른 초성은 매칭되지 않는다")
        void differentChosungDoesNotMatch() {
            assertThat(matchingService.matchesChosung("김철수", "이철수")).isFalse();
        }
        
        @Test
        @DisplayName("자모 분리")
        void jamoDecomposition() {
            String jamo = matchingService.decomposeToJamo("김");
            assertThat(jamo).isEqualTo("ㄱㅣㅁ");
        }
        
        @Test
        @DisplayName("한글 이름 유사도 계산")
        void koreanNameSimilarity() {
            double similarity = matchingService.calculateKoreanNameSimilarity("김철수", "김철호");
            assertThat(similarity).isGreaterThan(0.5);
        }
        
        @Test
        @DisplayName("완전히 동일한 한글 이름은 유사도 1.0")
        void identicalKoreanNamesHaveSimilarityOne() {
            double similarity = matchingService.calculateKoreanNameSimilarity("홍길동", "홍길동");
            assertThat(similarity).isEqualTo(1.0);
        }
        
        @Test
        @DisplayName("영어 이름은 한글 유사도 0")
        void englishNamesHaveZeroKoreanSimilarity() {
            double similarity = matchingService.calculateKoreanNameSimilarity("John Smith", "Jane Doe");
            assertThat(similarity).isEqualTo(0.0);
        }
    }
    
    @Nested
    @DisplayName("복합 매칭 테스트")
    class CompositeMatchTest {
        
        @Test
        @DisplayName("유사한 이름의 복합 점수가 높다")
        void similarNamesHaveHighCompositeScore() {
            var result = matchingService.calculateCompositeMatch("John Smith", "Jon Smyth");
            assertThat(result.compositeScore()).isGreaterThan(0.6);
        }
        
        @Test
        @DisplayName("발음 매칭 여부 확인")
        void phoneticMatchDetection() {
            var result = matchingService.calculateCompositeMatch("Muhammad Ali", "Mohammed Ali");
            assertThat(result.metaphoneMatch()).isTrue();
        }
        
        @Test
        @DisplayName("한글 이름 복합 점수")
        void koreanNameCompositeScore() {
            var result = matchingService.calculateCompositeMatch("김철수", "김철호");
            assertThat(result.koreanScore()).isGreaterThan(0.0);
            assertThat(result.compositeScore()).isGreaterThan(0.5);
        }
        
        @Test
        @DisplayName("highConfidenceMatch 판정")
        void highConfidenceMatchDetection() {
            var result = matchingService.calculateCompositeMatch("John Smith", "John Smith");
            assertThat(result.isHighConfidenceMatch(0.8)).isTrue();
        }
    }
    
    @Nested
    @DisplayName("Edge Case 테스트")
    class EdgeCaseTest {
        
        @Test
        @DisplayName("null 입력 처리")
        void nullInputHandling() {
            assertThat(matchingService.calculateJaroWinklerSimilarity(null, "test")).isEqualTo(0.0);
            assertThat(matchingService.calculateJaroWinklerSimilarity("test", null)).isEqualTo(0.0);
            assertThat(matchingService.getSoundexCode(null)).isEmpty();
            assertThat(matchingService.extractChosung(null)).isEmpty();
        }
        
        @Test
        @DisplayName("빈 문자열 처리")
        void emptyStringHandling() {
            assertThat(matchingService.calculateJaroWinklerSimilarity("", "test")).isEqualTo(0.0);
            assertThat(matchingService.matchesSoundex("", "")).isFalse();
        }
        
        @Test
        @DisplayName("특수문자가 포함된 이름 처리")
        void specialCharacterHandling() {
            double similarity = matchingService.calculateJaroWinklerSimilarity("O'Brien", "OBrien");
            assertThat(similarity).isGreaterThan(0.8);
        }
        
        @Test
        @DisplayName("악센트가 있는 이름 처리")
        void accentedCharacterHandling() {
            double similarity = matchingService.calculateJaroWinklerSimilarity("José", "Jose");
            assertThat(similarity).isGreaterThan(0.9);
        }
    }
}
