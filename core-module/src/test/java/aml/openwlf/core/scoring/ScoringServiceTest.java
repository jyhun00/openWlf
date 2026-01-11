package aml.openwlf.core.scoring;

import aml.openwlf.core.model.CustomerInfo;
import aml.openwlf.core.model.FilteringResult;
import aml.openwlf.core.model.MatchedRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ScoringService 테스트")
class ScoringServiceTest {
    
    private ScoringService scoringService;
    private CustomerInfo testCustomer;
    
    @BeforeEach
    void setUp() {
        scoringService = new ScoringService();
        // Set threshold values via reflection (normally injected by Spring)
        ReflectionTestUtils.setField(scoringService, "alertThreshold", 70.0);
        ReflectionTestUtils.setField(scoringService, "reviewThreshold", 50.0);
        
        testCustomer = CustomerInfo.builder()
                .name("John Smith")
                .dateOfBirth(LocalDate.of(1985, 5, 15))
                .nationality("US")
                .customerId("CUST-001")
                .build();
    }
    
    @Nested
    @DisplayName("점수 계산 테스트")
    class ScoreCalculationTest {
        
        @Test
        @DisplayName("매칭 룰이 없으면 점수 0, alert false 반환")
        void shouldReturnZeroScoreWhenNoMatchedRules() {
            // given
            List<MatchedRule> emptyRules = List.of();
            
            // when
            FilteringResult result = scoringService.calculateScore(testCustomer, emptyRules);
            
            // then
            assertThat(result.isAlert()).isFalse();
            assertThat(result.getScore()).isEqualTo(0.0);
            assertThat(result.getMatchedRules()).isEmpty();
            assertThat(result.getExplanation()).contains("No matches found");
            assertThat(result.getCustomerInfo()).isEqualTo(testCustomer);
        }
        
        @Test
        @DisplayName("null 룰 리스트는 점수 0 반환")
        void shouldReturnZeroScoreWhenRulesAreNull() {
            // when
            FilteringResult result = scoringService.calculateScore(testCustomer, null);
            
            // then
            assertThat(result.isAlert()).isFalse();
            assertThat(result.getScore()).isEqualTo(0.0);
        }
        
        @Test
        @DisplayName("단일 룰 점수 계산")
        void shouldCalculateScoreForSingleRule() {
            // given
            List<MatchedRule> rules = List.of(
                    createMatchedRule("EXACT_NAME_MATCH", "NAME", 100.0)
            );
            
            // when
            FilteringResult result = scoringService.calculateScore(testCustomer, rules);
            
            // then
            assertThat(result.getScore()).isEqualTo(100.0);
            assertThat(result.isAlert()).isTrue();
        }
        
        @Test
        @DisplayName("동일 타입 룰은 최대값만 적용")
        void shouldTakeMaxScoreForSameRuleType() {
            // given
            List<MatchedRule> rules = List.of(
                    createMatchedRule("EXACT_NAME_MATCH", "NAME", 100.0),
                    createMatchedRule("FUZZY_NAME_MATCH", "NAME", 80.0)
            );
            
            // when
            FilteringResult result = scoringService.calculateScore(testCustomer, rules);
            
            // then
            assertThat(result.getScore()).isEqualTo(100.0); // Max of NAME type
        }
        
        @Test
        @DisplayName("다른 타입 룰은 합산")
        void shouldSumScoresForDifferentRuleTypes() {
            // given
            List<MatchedRule> rules = List.of(
                    createMatchedRule("EXACT_NAME_MATCH", "NAME", 50.0),
                    createMatchedRule("DOB_MATCH", "DOB", 30.0)
            );
            
            // when
            FilteringResult result = scoringService.calculateScore(testCustomer, rules);
            
            // then
            assertThat(result.getScore()).isEqualTo(80.0); // 50 + 30
        }
        
        @Test
        @DisplayName("최대 점수는 100점으로 제한")
        void shouldCapScoreAt100() {
            // given
            List<MatchedRule> rules = List.of(
                    createMatchedRule("EXACT_NAME_MATCH", "NAME", 100.0),
                    createMatchedRule("DOB_MATCH", "DOB", 50.0),
                    createMatchedRule("NATIONALITY_MATCH", "NATIONALITY", 30.0)
            );
            
            // when
            FilteringResult result = scoringService.calculateScore(testCustomer, rules);
            
            // then
            assertThat(result.getScore()).isEqualTo(100.0); // Capped at 100
        }
    }
    
    @Nested
    @DisplayName("Alert 판정 테스트")
    class AlertDeterminationTest {
        
        @Test
        @DisplayName("점수 70점 이상이면 alert true")
        void shouldTriggerAlertWhenScoreAboveThreshold() {
            // given
            List<MatchedRule> rules = List.of(
                    createMatchedRule("EXACT_NAME_MATCH", "NAME", 70.0)
            );
            
            // when
            FilteringResult result = scoringService.calculateScore(testCustomer, rules);
            
            // then
            assertThat(result.isAlert()).isTrue();
            assertThat(result.getExplanation()).contains("ALERT");
        }
        
        @Test
        @DisplayName("점수 70점 미만, 50점 이상이면 REVIEW")
        void shouldReturnReviewWhenScoreBetweenThresholds() {
            // given
            List<MatchedRule> rules = List.of(
                    createMatchedRule("FUZZY_NAME_MATCH", "NAME", 60.0)
            );
            
            // when
            FilteringResult result = scoringService.calculateScore(testCustomer, rules);
            
            // then
            assertThat(result.isAlert()).isFalse();
            assertThat(result.getExplanation()).contains("REVIEW");
        }
        
        @Test
        @DisplayName("점수 50점 미만이면 LOW RISK")
        void shouldReturnLowRiskWhenScoreBelowReviewThreshold() {
            // given
            List<MatchedRule> rules = List.of(
                    createMatchedRule("NATIONALITY_MATCH", "NATIONALITY", 30.0)
            );
            
            // when
            FilteringResult result = scoringService.calculateScore(testCustomer, rules);
            
            // then
            assertThat(result.isAlert()).isFalse();
            assertThat(result.getExplanation()).contains("LOW RISK");
        }
    }
    
    @Nested
    @DisplayName("Explanation 생성 테스트")
    class ExplanationGenerationTest {
        
        @Test
        @DisplayName("Alert 시 거래 거부 권고 포함")
        void shouldIncludeRejectRecommendationForAlert() {
            // given
            List<MatchedRule> rules = List.of(
                    createMatchedRule("EXACT_NAME_MATCH", "NAME", 100.0)
            );
            
            // when
            FilteringResult result = scoringService.calculateScore(testCustomer, rules);
            
            // then
            assertThat(result.getExplanation()).contains("Reject transaction");
            assertThat(result.getExplanation()).contains("compliance team");
        }
        
        @Test
        @DisplayName("Review 시 enhanced due diligence 권고 포함")
        void shouldIncludeEnhancedDueDiligenceForReview() {
            // given
            List<MatchedRule> rules = List.of(
                    createMatchedRule("FUZZY_NAME_MATCH", "NAME", 55.0)
            );
            
            // when
            FilteringResult result = scoringService.calculateScore(testCustomer, rules);
            
            // then
            assertThat(result.getExplanation()).contains("enhanced due diligence");
        }
        
        @Test
        @DisplayName("Low Risk 시 정상 처리 권고 포함")
        void shouldIncludeStandardProcessingForLowRisk() {
            // given
            List<MatchedRule> rules = List.of(
                    createMatchedRule("NATIONALITY_MATCH", "NATIONALITY", 20.0)
            );
            
            // when
            FilteringResult result = scoringService.calculateScore(testCustomer, rules);
            
            // then
            assertThat(result.getExplanation()).contains("standard processing");
        }
        
        @Test
        @DisplayName("Explanation에 매칭된 룰 상세 정보 포함")
        void shouldIncludeMatchedRuleDetailsInExplanation() {
            // given
            MatchedRule rule = MatchedRule.builder()
                    .ruleName("EXACT_NAME_MATCH")
                    .ruleType("NAME")
                    .score(100.0)
                    .matchedValue("JOHN SMITH")
                    .targetValue("JOHN SMITH")
                    .description("Exact name match found")
                    .build();
            
            // when
            FilteringResult result = scoringService.calculateScore(testCustomer, List.of(rule));
            
            // then
            assertThat(result.getExplanation()).contains("EXACT_NAME_MATCH");
            assertThat(result.getExplanation()).contains("100.0 points");
            assertThat(result.getExplanation()).contains("JOHN SMITH");
        }
    }
    
    @Nested
    @DisplayName("복합 시나리오 테스트")
    class ComplexScenarioTest {
        
        @Test
        @DisplayName("여러 타입의 룰이 매칭된 경우 종합 점수 계산")
        void shouldCalculateComprehensiveScore() {
            // given
            List<MatchedRule> rules = List.of(
                    createMatchedRule("EXACT_NAME_MATCH", "NAME", 100.0),
                    createMatchedRule("FUZZY_NAME_MATCH", "NAME", 80.0),
                    createMatchedRule("DOB_MATCH", "DOB", 50.0),
                    createMatchedRule("NATIONALITY_MATCH", "NATIONALITY", 30.0)
            );
            
            // when
            FilteringResult result = scoringService.calculateScore(testCustomer, rules);
            
            // then
            // NAME: max(100, 80) = 100, DOB: 50, NATIONALITY: 30 -> capped at 100
            assertThat(result.getScore()).isEqualTo(100.0);
            assertThat(result.isAlert()).isTrue();
            assertThat(result.getMatchedRules()).hasSize(4);
        }
        
        @Test
        @DisplayName("경계값 테스트 - 정확히 70점")
        void shouldTriggerAlertAtExactThreshold() {
            // given
            List<MatchedRule> rules = List.of(
                    createMatchedRule("FUZZY_NAME_MATCH", "NAME", 70.0)
            );
            
            // when
            FilteringResult result = scoringService.calculateScore(testCustomer, rules);
            
            // then
            assertThat(result.isAlert()).isTrue();
            assertThat(result.getScore()).isEqualTo(70.0);
        }
        
        @Test
        @DisplayName("경계값 테스트 - 69.9점")
        void shouldNotTriggerAlertJustBelowThreshold() {
            // given
            List<MatchedRule> rules = List.of(
                    createMatchedRule("FUZZY_NAME_MATCH", "NAME", 69.9)
            );
            
            // when
            FilteringResult result = scoringService.calculateScore(testCustomer, rules);
            
            // then
            assertThat(result.isAlert()).isFalse();
        }
    }
    
    private MatchedRule createMatchedRule(String name, String type, double score) {
        return MatchedRule.builder()
                .ruleName(name)
                .ruleType(type)
                .score(score)
                .matchedValue("TEST_VALUE")
                .targetValue("TARGET_VALUE")
                .description("Test rule")
                .build();
    }
}
