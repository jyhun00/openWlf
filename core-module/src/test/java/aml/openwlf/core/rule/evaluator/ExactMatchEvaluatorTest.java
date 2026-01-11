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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExactMatchEvaluator 테스트")
class ExactMatchEvaluatorTest {
    
    private ExactMatchEvaluator evaluator;
    private NormalizationService normalizationService;
    
    @BeforeEach
    void setUp() {
        normalizationService = new NormalizationService();
        evaluator = new ExactMatchEvaluator(normalizationService);
    }
    
    @Test
    @DisplayName("매칭 타입은 EXACT")
    void shouldReturnExactMatchType() {
        assertThat(evaluator.getMatchType()).isEqualTo("EXACT");
    }
    
    @Nested
    @DisplayName("이름 매칭 테스트")
    class NameMatchingTest {
        
        @Test
        @DisplayName("정확히 같은 이름 매칭")
        void shouldMatchExactName() {
            // given
            CustomerInfo customer = CustomerInfo.builder()
                    .name("John Smith")
                    .build();
            
            WatchlistEntry entry = WatchlistEntry.builder()
                    .name("John Smith")
                    .build();
            
            RuleDefinition rule = createNameMatchRule();
            
            // when
            List<MatchedRule> results = evaluator.evaluate(customer, entry, rule);
            
            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getRuleName()).isEqualTo("EXACT_NAME_MATCH");
            assertThat(results.get(0).getScore()).isEqualTo(100.0);
        }
        
        @Test
        @DisplayName("대소문자 무시하고 매칭")
        void shouldMatchIgnoreCase() {
            // given
            CustomerInfo customer = CustomerInfo.builder()
                    .name("JOHN SMITH")
                    .build();
            
            WatchlistEntry entry = WatchlistEntry.builder()
                    .name("john smith")
                    .build();
            
            RuleDefinition rule = createNameMatchRule();
            
            // when
            List<MatchedRule> results = evaluator.evaluate(customer, entry, rule);
            
            // then
            assertThat(results).hasSize(1);
        }
        
        @Test
        @DisplayName("이름 순서 달라도 정규화 후 매칭")
        void shouldMatchReorderedName() {
            // given
            CustomerInfo customer = CustomerInfo.builder()
                    .name("Smith John")
                    .build();
            
            WatchlistEntry entry = WatchlistEntry.builder()
                    .name("John Smith")
                    .build();
            
            RuleDefinition rule = createNameMatchRule();
            
            // when
            List<MatchedRule> results = evaluator.evaluate(customer, entry, rule);
            
            // then
            assertThat(results).hasSize(1);
        }
        
        @Test
        @DisplayName("다른 이름은 매칭 안됨")
        void shouldNotMatchDifferentName() {
            // given
            CustomerInfo customer = CustomerInfo.builder()
                    .name("John Smith")
                    .build();
            
            WatchlistEntry entry = WatchlistEntry.builder()
                    .name("Jane Doe")
                    .build();
            
            RuleDefinition rule = createNameMatchRule();
            
            // when
            List<MatchedRule> results = evaluator.evaluate(customer, entry, rule);
            
            // then
            assertThat(results).isEmpty();
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
            
            RuleDefinition rule = createNameMatchRule();
            
            // when
            List<MatchedRule> results = evaluator.evaluate(customer, entry, rule);
            
            // then
            assertThat(results).isEmpty();
        }
        
        @Test
        @DisplayName("null 이름은 매칭 안됨")
        void shouldNotMatchNullName() {
            // given
            CustomerInfo customer = CustomerInfo.builder()
                    .name(null)
                    .build();
            
            WatchlistEntry entry = WatchlistEntry.builder()
                    .name("John Smith")
                    .build();
            
            RuleDefinition rule = createNameMatchRule();
            
            // when
            List<MatchedRule> results = evaluator.evaluate(customer, entry, rule);
            
            // then
            assertThat(results).isEmpty();
        }
    }
    
    @Nested
    @DisplayName("Alias 매칭 테스트")
    class AliasMatchingTest {
        
        @Test
        @DisplayName("Alias와 정확히 매칭")
        void shouldMatchExactAlias() {
            // given
            CustomerInfo customer = CustomerInfo.builder()
                    .name("Johnny Smith")
                    .build();
            
            WatchlistEntry entry = WatchlistEntry.builder()
                    .name("John Smith")
                    .aliases(List.of("Johnny Smith", "J. Smith"))
                    .build();
            
            RuleDefinition rule = createAliasMatchRule();
            
            // when
            List<MatchedRule> results = evaluator.evaluate(customer, entry, rule);
            
            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getRuleName()).isEqualTo("EXACT_ALIAS_MATCH");
        }
        
        @Test
        @DisplayName("여러 Alias 중 하나와 매칭")
        void shouldMatchOneOfManyAliases() {
            // given
            CustomerInfo customer = CustomerInfo.builder()
                    .name("J Smith")
                    .build();
            
            WatchlistEntry entry = WatchlistEntry.builder()
                    .name("John Smith")
                    .aliases(List.of("Johnny", "J Smith", "John S."))
                    .build();
            
            RuleDefinition rule = createAliasMatchRule();
            
            // when
            List<MatchedRule> results = evaluator.evaluate(customer, entry, rule);
            
            // then
            assertThat(results).hasSize(1);
        }
        
        @Test
        @DisplayName("Alias가 없으면 매칭 안됨")
        void shouldNotMatchWhenNoAliases() {
            // given
            CustomerInfo customer = CustomerInfo.builder()
                    .name("Johnny Smith")
                    .build();
            
            WatchlistEntry entry = WatchlistEntry.builder()
                    .name("John Smith")
                    .aliases(null)
                    .build();
            
            RuleDefinition rule = createAliasMatchRule();
            
            // when
            List<MatchedRule> results = evaluator.evaluate(customer, entry, rule);
            
            // then
            assertThat(results).isEmpty();
        }
    }
    
    @Nested
    @DisplayName("국적 매칭 테스트")
    class NationalityMatchingTest {
        
        @Test
        @DisplayName("국적 정확히 매칭")
        void shouldMatchExactNationality() {
            // given
            CustomerInfo customer = CustomerInfo.builder()
                    .name("John Smith")
                    .nationality("US")
                    .build();
            
            WatchlistEntry entry = WatchlistEntry.builder()
                    .name("John Smith")
                    .nationality("US")
                    .build();
            
            RuleDefinition rule = createNationalityMatchRule();
            
            // when
            List<MatchedRule> results = evaluator.evaluate(customer, entry, rule);
            
            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getRuleName()).isEqualTo("NATIONALITY_MATCH");
        }
        
        @Test
        @DisplayName("국적 대소문자 무시")
        void shouldMatchNationalityIgnoreCase() {
            // given
            CustomerInfo customer = CustomerInfo.builder()
                    .nationality("us")
                    .build();
            
            WatchlistEntry entry = WatchlistEntry.builder()
                    .nationality("US")
                    .build();
            
            RuleDefinition rule = createNationalityMatchRule();
            
            // when
            List<MatchedRule> results = evaluator.evaluate(customer, entry, rule);
            
            // then
            assertThat(results).hasSize(1);
        }
        
        @Test
        @DisplayName("다른 국적은 매칭 안됨")
        void shouldNotMatchDifferentNationality() {
            // given
            CustomerInfo customer = CustomerInfo.builder()
                    .nationality("US")
                    .build();
            
            WatchlistEntry entry = WatchlistEntry.builder()
                    .nationality("KP")
                    .build();
            
            RuleDefinition rule = createNationalityMatchRule();
            
            // when
            List<MatchedRule> results = evaluator.evaluate(customer, entry, rule);
            
            // then
            assertThat(results).isEmpty();
        }
    }
    
    @Nested
    @DisplayName("생년월일 매칭 테스트")
    class DateOfBirthMatchingTest {
        
        @Test
        @DisplayName("생년월일 정확히 매칭")
        void shouldMatchExactDateOfBirth() {
            // given
            CustomerInfo customer = CustomerInfo.builder()
                    .name("John Smith")
                    .dateOfBirth(LocalDate.of(1985, 5, 15))
                    .build();
            
            WatchlistEntry entry = WatchlistEntry.builder()
                    .name("John Smith")
                    .dateOfBirth(LocalDate.of(1985, 5, 15))
                    .build();
            
            RuleDefinition rule = createDobMatchRule();
            
            // when
            List<MatchedRule> results = evaluator.evaluate(customer, entry, rule);
            
            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getRuleName()).isEqualTo("DOB_MATCH");
        }
        
        @Test
        @DisplayName("다른 생년월일은 매칭 안됨")
        void shouldNotMatchDifferentDob() {
            // given
            CustomerInfo customer = CustomerInfo.builder()
                    .dateOfBirth(LocalDate.of(1985, 5, 15))
                    .build();
            
            WatchlistEntry entry = WatchlistEntry.builder()
                    .dateOfBirth(LocalDate.of(1990, 3, 20))
                    .build();
            
            RuleDefinition rule = createDobMatchRule();
            
            // when
            List<MatchedRule> results = evaluator.evaluate(customer, entry, rule);
            
            // then
            assertThat(results).isEmpty();
        }
    }
    
    // Helper methods for creating test rule definitions
    private RuleDefinition createNameMatchRule() {
        RuleDefinition.MatchCondition condition = RuleDefinition.MatchCondition.builder()
                .matchType("EXACT")
                .sourceField("name")
                .targetField("name")
                .build();
        
        RuleDefinition.ScoreConfig score = RuleDefinition.ScoreConfig.builder()
                .exactMatch(100.0)
                .build();
        
        return RuleDefinition.builder()
                .id("EXACT_NAME_MATCH")
                .type("NAME")
                .description("Exact name match")
                .enabled(true)
                .condition(condition)
                .score(score)
                .build();
    }
    
    private RuleDefinition createAliasMatchRule() {
        RuleDefinition.MatchCondition condition = RuleDefinition.MatchCondition.builder()
                .matchType("EXACT")
                .sourceField("name")
                .targetField("aliases")
                .build();
        
        RuleDefinition.ScoreConfig score = RuleDefinition.ScoreConfig.builder()
                .exactMatch(90.0)
                .build();
        
        return RuleDefinition.builder()
                .id("EXACT_ALIAS_MATCH")
                .type("ALIAS")
                .description("Exact alias match")
                .enabled(true)
                .condition(condition)
                .score(score)
                .build();
    }
    
    private RuleDefinition createNationalityMatchRule() {
        RuleDefinition.MatchCondition condition = RuleDefinition.MatchCondition.builder()
                .matchType("EXACT")
                .sourceField("nationality")
                .targetField("nationality")
                .build();
        
        RuleDefinition.ScoreConfig score = RuleDefinition.ScoreConfig.builder()
                .exactMatch(30.0)
                .build();
        
        return RuleDefinition.builder()
                .id("NATIONALITY_MATCH")
                .type("NATIONALITY")
                .description("Nationality match")
                .enabled(true)
                .condition(condition)
                .score(score)
                .build();
    }
    
    private RuleDefinition createDobMatchRule() {
        RuleDefinition.MatchCondition condition = RuleDefinition.MatchCondition.builder()
                .matchType("EXACT")
                .sourceField("dateOfBirth")
                .targetField("dateOfBirth")
                .build();
        
        RuleDefinition.ScoreConfig score = RuleDefinition.ScoreConfig.builder()
                .exactMatch(50.0)
                .build();
        
        return RuleDefinition.builder()
                .id("DOB_MATCH")
                .type("DOB")
                .description("Date of birth match")
                .enabled(true)
                .condition(condition)
                .score(score)
                .build();
    }
}
