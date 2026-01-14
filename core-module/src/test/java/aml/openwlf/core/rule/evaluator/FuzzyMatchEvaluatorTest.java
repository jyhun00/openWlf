package aml.openwlf.core.rule.evaluator;

import aml.openwlf.config.rule.RuleDefinition;
import aml.openwlf.core.model.CustomerInfo;
import aml.openwlf.core.model.MatchedRule;
import aml.openwlf.core.normalization.NormalizationService;
import aml.openwlf.core.rule.WatchlistEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FuzzyMatchEvaluator 테스트")
class FuzzyMatchEvaluatorTest {

    private FuzzyMatchEvaluator evaluator;
    private NormalizationService normalizationService;
    private FieldValueExtractor fieldExtractor;

    @BeforeEach
    void setUp() {
        normalizationService = new NormalizationService();
        fieldExtractor = new FieldValueExtractor();
        evaluator = new FuzzyMatchEvaluator(fieldExtractor, normalizationService);
    }
    
    @Test
    @DisplayName("매칭 타입은 FUZZY")
    void shouldReturnFuzzyMatchType() {
        assertThat(evaluator.getMatchType()).isEqualTo("FUZZY");
    }
    
    @Nested
    @DisplayName("유사 이름 매칭 테스트")
    class SimilarNameMatchingTest {
        
        @Test
        @DisplayName("오타가 있는 이름 매칭")
        void shouldMatchNameWithTypo() {
            // given
            CustomerInfo customer = CustomerInfo.builder()
                    .name("John Smithe")  // 오타: Smith -> Smithe
                    .build();
            
            WatchlistEntry entry = WatchlistEntry.builder()
                    .name("John Smith")
                    .build();
            
            RuleDefinition rule = createFuzzyNameRule(0.8);
            
            // when
            List<MatchedRule> results = evaluator.evaluate(customer, entry, rule);
            
            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getRuleName()).isEqualTo("FUZZY_NAME_MATCH");
            assertThat(results.get(0).getDescription()).contains("similarity");
        }
        
        @Test
        @DisplayName("유사도 임계값 이상이면 매칭")
        void shouldMatchWhenAboveThreshold() {
            // given
            CustomerInfo customer = CustomerInfo.builder()
                    .name("Jon Smith")  // John -> Jon (1글자 차이)
                    .build();
            
            WatchlistEntry entry = WatchlistEntry.builder()
                    .name("John Smith")
                    .build();
            
            RuleDefinition rule = createFuzzyNameRule(0.8);
            
            // when
            List<MatchedRule> results = evaluator.evaluate(customer, entry, rule);
            
            // then
            assertThat(results).hasSize(1);
        }
        
        @Test
        @DisplayName("유사도 임계값 미만이면 매칭 안됨")
        void shouldNotMatchWhenBelowThreshold() {
            // given
            CustomerInfo customer = CustomerInfo.builder()
                    .name("Jane Doe")  // 완전히 다른 이름
                    .build();
            
            WatchlistEntry entry = WatchlistEntry.builder()
                    .name("John Smith")
                    .build();
            
            RuleDefinition rule = createFuzzyNameRule(0.8);
            
            // when
            List<MatchedRule> results = evaluator.evaluate(customer, entry, rule);
            
            // then
            assertThat(results).isEmpty();
        }
        
        @Test
        @DisplayName("정확히 일치해도 퍼지 매칭으로 처리")
        void shouldMatchExactNameAsFuzzy() {
            // given
            CustomerInfo customer = CustomerInfo.builder()
                    .name("John Smith")
                    .build();
            
            WatchlistEntry entry = WatchlistEntry.builder()
                    .name("John Smith")
                    .build();
            
            RuleDefinition rule = createFuzzyNameRule(0.8);
            
            // when
            List<MatchedRule> results = evaluator.evaluate(customer, entry, rule);
            
            // then
            assertThat(results).hasSize(1);
        }
        
        @Test
        @DisplayName("빈 이름은 매칭 안됨")
        void shouldNotMatchEmptyName() {
            // given
            CustomerInfo customer = CustomerInfo.builder()
                    .name("")
                    .build();
            
            WatchlistEntry entry = WatchlistEntry.builder()
                    .name("John Smith")
                    .build();
            
            RuleDefinition rule = createFuzzyNameRule(0.8);
            
            // when
            List<MatchedRule> results = evaluator.evaluate(customer, entry, rule);
            
            // then
            assertThat(results).isEmpty();
        }
    }
    
    @Nested
    @DisplayName("Alias 퍼지 매칭 테스트")
    class AliasFuzzyMatchingTest {
        
        @Test
        @DisplayName("여러 Alias 중 가장 유사한 것과 매칭")
        void shouldMatchBestSimilarAlias() {
            // given
            CustomerInfo customer = CustomerInfo.builder()
                    .name("Johny Smith")  // Johnny -> Johny
                    .build();
            
            WatchlistEntry entry = WatchlistEntry.builder()
                    .name("John Smith")
                    .aliases(List.of("Johnny Smith", "J. Smith", "John S."))
                    .build();
            
            RuleDefinition rule = createFuzzyAliasRule(0.8);
            
            // when
            List<MatchedRule> results = evaluator.evaluate(customer, entry, rule);
            
            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTargetValue()).isEqualTo("Johnny Smith");
        }
    }
    
    @Nested
    @DisplayName("점수 계산 테스트")
    class ScoreCalculationTest {
        
        @Test
        @DisplayName("유사도에 비례한 점수 계산")
        void shouldCalculateProportionalScore() {
            // given
            CustomerInfo customer = CustomerInfo.builder()
                    .name("Jon Smith")
                    .build();
            
            WatchlistEntry entry = WatchlistEntry.builder()
                    .name("John Smith")
                    .build();
            
            RuleDefinition rule = createFuzzyNameRuleWithProportionalScore(80.0);
            
            // when
            List<MatchedRule> results = evaluator.evaluate(customer, entry, rule);
            
            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getScore()).isGreaterThan(0);
            assertThat(results.get(0).getScore()).isLessThanOrEqualTo(80.0);
        }
    }
    
    @Nested
    @DisplayName("임계값 변경 테스트")
    class ThresholdTest {
        
        @Test
        @DisplayName("낮은 임계값으로 더 많은 매칭")
        void shouldMatchMoreWithLowerThreshold() {
            // given
            CustomerInfo customer = CustomerInfo.builder()
                    .name("J Smith")
                    .build();
            
            WatchlistEntry entry = WatchlistEntry.builder()
                    .name("John Smith")
                    .build();
            
            RuleDefinition highThreshold = createFuzzyNameRule(0.9);
            RuleDefinition lowThreshold = createFuzzyNameRule(0.5);
            
            // when
            List<MatchedRule> highResults = evaluator.evaluate(customer, entry, highThreshold);
            List<MatchedRule> lowResults = evaluator.evaluate(customer, entry, lowThreshold);
            
            // then
            assertThat(highResults).isEmpty();
            assertThat(lowResults).hasSize(1);
        }
        
        @Test
        @DisplayName("기본 임계값은 0.8")
        void shouldUseDefaultThreshold() {
            // given
            CustomerInfo customer = CustomerInfo.builder()
                    .name("John Smth")  // Smith -> Smth
                    .build();
            
            WatchlistEntry entry = WatchlistEntry.builder()
                    .name("John Smith")
                    .build();
            
            RuleDefinition rule = createFuzzyNameRuleWithoutThreshold();
            
            // when
            List<MatchedRule> results = evaluator.evaluate(customer, entry, rule);
            
            // then
            assertThat(results).hasSize(1);
        }
    }
    
    @Nested
    @DisplayName("특수 문자 처리 테스트")
    class SpecialCharacterTest {
        
        @Test
        @DisplayName("특수 문자 제거 후 유사도 계산")
        void shouldNormalizeSpecialCharacters() {
            // given
            CustomerInfo customer = CustomerInfo.builder()
                    .name("O'Neill")
                    .build();
            
            WatchlistEntry entry = WatchlistEntry.builder()
                    .name("Oneill")
                    .build();
            
            RuleDefinition rule = createFuzzyNameRule(0.9);
            
            // when
            List<MatchedRule> results = evaluator.evaluate(customer, entry, rule);
            
            // then
            assertThat(results).hasSize(1);
        }
        
        @Test
        @DisplayName("악센트 제거 후 유사도 계산")
        void shouldNormalizeAccents() {
            // given
            CustomerInfo customer = CustomerInfo.builder()
                    .name("José García")
                    .build();
            
            WatchlistEntry entry = WatchlistEntry.builder()
                    .name("Jose Garcia")
                    .build();
            
            RuleDefinition rule = createFuzzyNameRule(0.9);
            
            // when
            List<MatchedRule> results = evaluator.evaluate(customer, entry, rule);
            
            // then
            assertThat(results).hasSize(1);
        }
    }
    
    // Helper methods
    private RuleDefinition createFuzzyNameRule(double threshold) {
        RuleDefinition.MatchCondition condition = RuleDefinition.MatchCondition.builder()
                .matchType("FUZZY")
                .sourceField("name")
                .targetField("name")
                .parameters(Map.of("similarityThreshold", threshold))
                .build();
        
        RuleDefinition.ScoreConfig score = RuleDefinition.ScoreConfig.builder()
                .partialMatch(80.0)
                .proportionalToSimilarity(false)
                .build();
        
        return RuleDefinition.builder()
                .id("FUZZY_NAME_MATCH")
                .type("NAME")
                .description("Fuzzy name match")
                .enabled(true)
                .condition(condition)
                .score(score)
                .build();
    }
    
    private RuleDefinition createFuzzyAliasRule(double threshold) {
        RuleDefinition.MatchCondition condition = RuleDefinition.MatchCondition.builder()
                .matchType("FUZZY")
                .sourceField("name")
                .targetField("aliases")
                .parameters(Map.of("similarityThreshold", threshold))
                .build();
        
        RuleDefinition.ScoreConfig score = RuleDefinition.ScoreConfig.builder()
                .partialMatch(70.0)
                .proportionalToSimilarity(false)
                .build();
        
        return RuleDefinition.builder()
                .id("FUZZY_ALIAS_MATCH")
                .type("ALIAS")
                .description("Fuzzy alias match")
                .enabled(true)
                .condition(condition)
                .score(score)
                .build();
    }
    
    private RuleDefinition createFuzzyNameRuleWithProportionalScore(double maxScore) {
        RuleDefinition.MatchCondition condition = RuleDefinition.MatchCondition.builder()
                .matchType("FUZZY")
                .sourceField("name")
                .targetField("name")
                .parameters(Map.of("similarityThreshold", 0.8))
                .build();
        
        RuleDefinition.ScoreConfig score = RuleDefinition.ScoreConfig.builder()
                .maxScore(maxScore)
                .proportionalToSimilarity(true)
                .build();
        
        return RuleDefinition.builder()
                .id("FUZZY_NAME_MATCH")
                .type("NAME")
                .description("Fuzzy name match")
                .enabled(true)
                .condition(condition)
                .score(score)
                .build();
    }
    
    private RuleDefinition createFuzzyNameRuleWithoutThreshold() {
        RuleDefinition.MatchCondition condition = RuleDefinition.MatchCondition.builder()
                .matchType("FUZZY")
                .sourceField("name")
                .targetField("name")
                // No parameters - use default threshold
                .build();
        
        RuleDefinition.ScoreConfig score = RuleDefinition.ScoreConfig.builder()
                .partialMatch(80.0)
                .build();
        
        return RuleDefinition.builder()
                .id("FUZZY_NAME_MATCH")
                .type("NAME")
                .description("Fuzzy name match")
                .enabled(true)
                .condition(condition)
                .score(score)
                .build();
    }
}
