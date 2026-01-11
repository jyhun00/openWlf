package aml.openwlf.core.rule.evaluator;

import aml.openwlf.config.rule.RuleDefinition;
import aml.openwlf.core.model.CustomerInfo;
import aml.openwlf.core.model.MatchedRule;
import aml.openwlf.core.rule.WatchlistEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 날짜 범위 매칭 평가기
 */
@Slf4j
@Component
public class DateRangeMatchEvaluator implements RuleEvaluator {
    
    private static final int DEFAULT_RANGE_DAYS = 365;
    
    @Override
    public String getMatchType() {
        return "DATE_RANGE";
    }
    
    @Override
    public List<MatchedRule> evaluate(CustomerInfo customer, WatchlistEntry entry, RuleDefinition rule) {
        List<MatchedRule> results = new ArrayList<>();
        
        LocalDate customerDob = customer.getDateOfBirth();
        LocalDate entryDob = entry.getDateOfBirth();
        
        if (customerDob == null || entryDob == null) {
            return results;
        }
        
        int rangeDays = rule.getCondition().getParameter("rangeDays", DEFAULT_RANGE_DAYS);
        
        // 완전 일치
        if (customerDob.equals(entryDob)) {
            log.debug("Exact DOB match: {} (Rule: {})", customerDob, rule.getId());
            
            results.add(MatchedRule.builder()
                    .ruleName(rule.getId())
                    .ruleType(rule.getType())
                    .score(rule.getScore().getExactMatch())
                    .matchedValue(customerDob.toString())
                    .targetValue(entryDob.toString())
                    .description(rule.getDescription() + " (exact match)")
                    .build());
            
            return results;
        }
        
        // 근사 일치 (범위 내)
        long daysDiff = Math.abs(ChronoUnit.DAYS.between(customerDob, entryDob));
        
        if (daysDiff <= rangeDays) {
            double score = calculateProximityScore(daysDiff, rangeDays, rule.getScore());
            
            log.debug("Approximate DOB match: {} ~ {} (diff: {} days, score: {:.1f}, Rule: {})", 
                    customerDob, entryDob, daysDiff, score, rule.getId());
            
            results.add(MatchedRule.builder()
                    .ruleName(rule.getId())
                    .ruleType(rule.getType())
                    .score(score)
                    .matchedValue(customerDob.toString())
                    .targetValue(entryDob.toString())
                    .description(rule.getDescription() + String.format(" (within %d days)", daysDiff))
                    .build());
        }
        
        return results;
    }
    
    private double calculateProximityScore(long daysDiff, int rangeDays, RuleDefinition.ScoreConfig scoreConfig) {
        if (scoreConfig.isProportionalToSimilarity()) {
            // 날짜가 가까울수록 높은 점수
            double proximity = 1.0 - ((double) daysDiff / rangeDays);
            return proximity * scoreConfig.getMaxScore();
        }
        return scoreConfig.getPartialMatch();
    }
}
