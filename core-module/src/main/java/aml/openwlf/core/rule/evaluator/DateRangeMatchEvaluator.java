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
public class DateRangeMatchEvaluator extends AbstractRuleEvaluator {

    private static final int DEFAULT_RANGE_DAYS = 365;

    public DateRangeMatchEvaluator(FieldValueExtractor fieldExtractor) {
        super(fieldExtractor);
    }

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

        if (customerDob.equals(entryDob)) {
            log.debug("Exact DOB match: {} (Rule: {})", customerDob, rule.getId());

            results.add(buildMatchedRule(
                    rule,
                    getExactMatchScore(rule.getScore()),
                    customerDob.toString(),
                    entryDob.toString(),
                    rule.getDescription() + " (exact match)"
            ));

            return results;
        }

        long daysDiff = Math.abs(ChronoUnit.DAYS.between(customerDob, entryDob));

        if (daysDiff <= rangeDays) {
            double score = calculateProximityScore(daysDiff, rangeDays, rule.getScore());

            log.debug("Approximate DOB match: {} ~ {} (diff: {} days, score: {:.1f}, Rule: {})",
                    customerDob, entryDob, daysDiff, score, rule.getId());

            results.add(buildMatchedRule(
                    rule,
                    score,
                    customerDob.toString(),
                    entryDob.toString(),
                    rule.getDescription() + String.format(" (within %d days)", daysDiff)
            ));
        }

        return results;
    }

    private double calculateProximityScore(long daysDiff, int rangeDays, RuleDefinition.ScoreConfig scoreConfig) {
        if (scoreConfig.isProportionalToSimilarity()) {
            double proximity = 1.0 - ((double) daysDiff / rangeDays);
            return proximity * scoreConfig.getMaxScore();
        }
        return scoreConfig.getPartialMatch();
    }
}
