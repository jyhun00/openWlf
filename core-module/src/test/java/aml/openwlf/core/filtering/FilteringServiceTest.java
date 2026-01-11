package aml.openwlf.core.filtering;

import aml.openwlf.core.model.CustomerInfo;
import aml.openwlf.core.model.FilteringResult;
import aml.openwlf.core.model.MatchedRule;
import aml.openwlf.core.rule.RuleEngine;
import aml.openwlf.core.rule.WatchlistEntry;
import aml.openwlf.core.scoring.ScoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FilteringService 테스트")
class FilteringServiceTest {
    
    @Mock
    private RuleEngine ruleEngine;
    
    @Mock
    private ScoringService scoringService;
    
    @Mock
    private WatchlistProvider watchlistProvider;
    
    @InjectMocks
    private FilteringService filteringService;
    
    @Captor
    private ArgumentCaptor<List<MatchedRule>> matchedRulesCaptor;
    
    private CustomerInfo testCustomer;
    private WatchlistEntry testEntry;
    
    @BeforeEach
    void setUp() {
        testCustomer = CustomerInfo.builder()
                .name("John Smith")
                .dateOfBirth(LocalDate.of(1985, 5, 15))
                .nationality("US")
                .customerId("CUST-001")
                .build();
        
        testEntry = WatchlistEntry.builder()
                .id(1L)
                .name("John Smith")
                .listSource("OFAC")
                .build();
    }
    
    @Nested
    @DisplayName("기본 필터링 동작 테스트")
    class BasicFilteringTest {
        
        @Test
        @DisplayName("감시목록이 비어있으면 빈 결과 반환")
        void shouldReturnEmptyResultWhenWatchlistIsEmpty() {
            // given
            when(watchlistProvider.getAllEntries()).thenReturn(Collections.emptyList());
            when(scoringService.calculateScore(any(), any())).thenReturn(
                    FilteringResult.builder()
                            .alert(false)
                            .score(0.0)
                            .matchedRules(Collections.emptyList())
                            .explanation("No matches found")
                            .customerInfo(testCustomer)
                            .build()
            );
            
            // when
            FilteringResult result = filteringService.filterCustomer(testCustomer);
            
            // then
            assertThat(result.isAlert()).isFalse();
            assertThat(result.getScore()).isEqualTo(0.0);
            verify(ruleEngine, never()).applyRules(any(), any());
        }
        
        @Test
        @DisplayName("감시목록의 모든 항목에 대해 룰 엔진 호출")
        void shouldApplyRulesToAllWatchlistEntries() {
            // given
            WatchlistEntry entry1 = WatchlistEntry.builder().id(1L).name("Person A").build();
            WatchlistEntry entry2 = WatchlistEntry.builder().id(2L).name("Person B").build();
            WatchlistEntry entry3 = WatchlistEntry.builder().id(3L).name("Person C").build();
            
            when(watchlistProvider.getAllEntries()).thenReturn(List.of(entry1, entry2, entry3));
            when(ruleEngine.applyRules(any(), any())).thenReturn(Collections.emptyList());
            when(scoringService.calculateScore(any(), any())).thenReturn(
                    createFilteringResult(false, 0.0)
            );
            
            // when
            filteringService.filterCustomer(testCustomer);
            
            // then
            verify(ruleEngine, times(3)).applyRules(eq(testCustomer), any(WatchlistEntry.class));
            verify(ruleEngine).applyRules(testCustomer, entry1);
            verify(ruleEngine).applyRules(testCustomer, entry2);
            verify(ruleEngine).applyRules(testCustomer, entry3);
        }
        
        @Test
        @DisplayName("매칭된 모든 룰을 수집하여 스코어링 서비스에 전달")
        void shouldCollectAllMatchedRulesForScoring() {
            // given
            MatchedRule rule1 = createMatchedRule("EXACT_NAME_MATCH", 100.0);
            MatchedRule rule2 = createMatchedRule("DOB_MATCH", 50.0);
            
            WatchlistEntry entry1 = WatchlistEntry.builder().id(1L).name("Person A").build();
            WatchlistEntry entry2 = WatchlistEntry.builder().id(2L).name("Person B").build();
            
            when(watchlistProvider.getAllEntries()).thenReturn(List.of(entry1, entry2));
            when(ruleEngine.applyRules(testCustomer, entry1)).thenReturn(List.of(rule1));
            when(ruleEngine.applyRules(testCustomer, entry2)).thenReturn(List.of(rule2));
            when(scoringService.calculateScore(any(), any())).thenReturn(
                    createFilteringResult(true, 100.0)
            );
            
            // when
            filteringService.filterCustomer(testCustomer);
            
            // then
            verify(scoringService).calculateScore(eq(testCustomer), matchedRulesCaptor.capture());
            List<MatchedRule> capturedRules = matchedRulesCaptor.getValue();
            assertThat(capturedRules).hasSize(2);
            assertThat(capturedRules).containsExactlyInAnyOrder(rule1, rule2);
        }
    }
    
    @Nested
    @DisplayName("매칭 결과 테스트")
    class MatchingResultTest {
        
        @Test
        @DisplayName("정확히 일치하는 이름이 있으면 alert 반환")
        void shouldReturnAlertWhenExactNameMatch() {
            // given
            MatchedRule exactMatch = MatchedRule.builder()
                    .ruleName("EXACT_NAME_MATCH")
                    .ruleType("NAME")
                    .score(100.0)
                    .matchedValue("JOHN SMITH")
                    .targetValue("JOHN SMITH")
                    .description("Exact name match")
                    .build();
            
            when(watchlistProvider.getAllEntries()).thenReturn(List.of(testEntry));
            when(ruleEngine.applyRules(any(), any())).thenReturn(List.of(exactMatch));
            
            FilteringResult expectedResult = FilteringResult.builder()
                    .alert(true)
                    .score(100.0)
                    .matchedRules(List.of(exactMatch))
                    .explanation("ALERT: High-risk match detected")
                    .customerInfo(testCustomer)
                    .build();
            when(scoringService.calculateScore(any(), any())).thenReturn(expectedResult);
            
            // when
            FilteringResult result = filteringService.filterCustomer(testCustomer);
            
            // then
            assertThat(result.isAlert()).isTrue();
            assertThat(result.getScore()).isEqualTo(100.0);
        }
        
        @Test
        @DisplayName("매칭이 없으면 alert false 반환")
        void shouldReturnNoAlertWhenNoMatch() {
            // given
            when(watchlistProvider.getAllEntries()).thenReturn(List.of(testEntry));
            when(ruleEngine.applyRules(any(), any())).thenReturn(Collections.emptyList());
            when(scoringService.calculateScore(any(), any())).thenReturn(
                    createFilteringResult(false, 0.0)
            );
            
            // when
            FilteringResult result = filteringService.filterCustomer(testCustomer);
            
            // then
            assertThat(result.isAlert()).isFalse();
            assertThat(result.getScore()).isEqualTo(0.0);
        }
    }
    
    @Nested
    @DisplayName("에러 처리 테스트")
    class ErrorHandlingTest {
        
        @Test
        @DisplayName("룰 엔진 예외 발생 시 예외가 전파됨")
        void shouldPropagateExceptionWhenRuleEngineThrowsException() {
            // given
            WatchlistEntry entry1 = WatchlistEntry.builder().id(1L).name("Person A").build();

            when(watchlistProvider.getAllEntries()).thenReturn(List.of(entry1));
            when(ruleEngine.applyRules(testCustomer, entry1))
                    .thenThrow(new RuntimeException("Rule engine error"));

            // when/then - 현재 구현은 예외를 그대로 전파함
            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
                filteringService.filterCustomer(testCustomer);
            });
        }

        @Test
        @DisplayName("감시목록 조회 실패 시 예외가 전파됨")
        void shouldPropagateExceptionWhenWatchlistProviderFails() {
            // given
            when(watchlistProvider.getAllEntries())
                    .thenThrow(new RuntimeException("Database connection failed"));

            // when/then
            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
                filteringService.filterCustomer(testCustomer);
            });
        }
    }
    
    @Nested
    @DisplayName("고객 정보 테스트")
    class CustomerInfoTest {
        
        @Test
        @DisplayName("결과에 고객 정보 포함")
        void shouldIncludeCustomerInfoInResult() {
            // given
            when(watchlistProvider.getAllEntries()).thenReturn(Collections.emptyList());
            
            FilteringResult expectedResult = FilteringResult.builder()
                    .alert(false)
                    .score(0.0)
                    .matchedRules(Collections.emptyList())
                    .explanation("No matches found")
                    .customerInfo(testCustomer)
                    .build();
            when(scoringService.calculateScore(eq(testCustomer), any())).thenReturn(expectedResult);
            
            // when
            FilteringResult result = filteringService.filterCustomer(testCustomer);
            
            // then
            assertThat(result.getCustomerInfo()).isEqualTo(testCustomer);
            assertThat(result.getCustomerInfo().getName()).isEqualTo("John Smith");
            assertThat(result.getCustomerInfo().getCustomerId()).isEqualTo("CUST-001");
        }
    }
    
    private FilteringResult createFilteringResult(boolean alert, double score) {
        return FilteringResult.builder()
                .alert(alert)
                .score(score)
                .matchedRules(Collections.emptyList())
                .explanation("Test result")
                .customerInfo(testCustomer)
                .build();
    }
    
    private MatchedRule createMatchedRule(String name, double score) {
        return MatchedRule.builder()
                .ruleName(name)
                .ruleType("NAME")
                .score(score)
                .matchedValue("TEST")
                .targetValue("TEST")
                .description("Test rule")
                .build();
    }
}
