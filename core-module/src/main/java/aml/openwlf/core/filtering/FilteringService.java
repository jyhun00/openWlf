package aml.openwlf.core.filtering;

import aml.openwlf.core.model.CustomerInfo;
import aml.openwlf.core.model.FilteringResult;
import aml.openwlf.core.model.MatchedRule;
import aml.openwlf.core.rule.RuleEngine;
import aml.openwlf.core.rule.WatchlistEntry;
import aml.openwlf.core.scoring.ScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Main filtering service orchestrating the entire filtering process
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FilteringService {
    
    private final RuleEngine ruleEngine;
    private final ScoringService scoringService;
    private final WatchlistProvider watchlistProvider;
    
    /**
     * Filter customer against all watchlists
     */
    public FilteringResult filterCustomer(CustomerInfo customerInfo) {
        log.info("Starting filtering for customer: {}", customerInfo.getName());
        
        // Get all watchlist entries
        List<WatchlistEntry> watchlistEntries = watchlistProvider.getAllEntries();
        
        // Collect all matched rules
        List<MatchedRule> allMatchedRules = new ArrayList<>();
        
        for (WatchlistEntry entry : watchlistEntries) {
            List<MatchedRule> matchedRules = ruleEngine.applyRules(customerInfo, entry);
            allMatchedRules.addAll(matchedRules);
        }
        
        // Calculate score and determine alert
        FilteringResult result = scoringService.calculateScore(customerInfo, allMatchedRules);
        
        log.info("Filtering completed for customer: {} - Alert: {}, Score: {}", 
                customerInfo.getName(), result.isAlert(), result.getScore());
        
        return result;
    }
}
