package aml.openwlf.core.rule;

import aml.openwlf.config.rule.RuleConfiguration;
import aml.openwlf.config.rule.RuleConfigurationLoader;
import aml.openwlf.config.rule.RuleDefinition;
import aml.openwlf.core.model.CustomerInfo;
import aml.openwlf.core.model.MatchedRule;
import aml.openwlf.core.rule.evaluator.RuleEvaluator;
import aml.openwlf.core.rule.evaluator.RuleEvaluatorRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 외부 설정 파일 기반 룰 엔진
 * 
 * YAML 파일에서 룰 정의를 읽어와 동적으로 평가합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEngine {
    
    private final RuleConfigurationLoader configLoader;
    private final RuleEvaluatorRegistry evaluatorRegistry;
    
    /**
     * 고객 정보를 감시목록 항목과 대조하여 매칭되는 룰 반환
     */
    public List<MatchedRule> applyRules(CustomerInfo customer, WatchlistEntry entry) {
        List<MatchedRule> matchedRules = new ArrayList<>();
        
        RuleConfiguration config = configLoader.getConfiguration();
        List<RuleDefinition> enabledRules = config.getEnabledRules();
        
        log.debug("Applying {} enabled rules for customer: {}", 
                enabledRules.size(), customer.getName());
        
        for (RuleDefinition rule : enabledRules) {
            try {
                List<MatchedRule> results = evaluateRule(customer, entry, rule);
                matchedRules.addAll(results);
            } catch (Exception e) {
                log.error("Error evaluating rule {}: {}", rule.getId(), e.getMessage());
            }
        }
        
        return matchedRules;
    }
    
    /**
     * 단일 룰 평가
     */
    private List<MatchedRule> evaluateRule(CustomerInfo customer, WatchlistEntry entry, RuleDefinition rule) {
        String matchType = rule.getCondition().getMatchType();
        
        if (!evaluatorRegistry.isSupported(matchType)) {
            log.warn("Unsupported match type: {} for rule: {}", matchType, rule.getId());
            return List.of();
        }
        
        RuleEvaluator evaluator = evaluatorRegistry.getEvaluator(matchType);
        return evaluator.evaluate(customer, entry, rule);
    }
    
    /**
     * 현재 로드된 룰 설정 정보 반환
     */
    public RuleConfiguration getCurrentConfiguration() {
        return configLoader.getConfiguration();
    }
    
    /**
     * 룰 설정 리로드
     */
    public void reloadConfiguration() {
        configLoader.reload();
        log.info("Rule configuration reloaded");
    }
    
    /**
     * 지원하는 매칭 타입 목록
     */
    public List<String> getSupportedMatchTypes() {
        return evaluatorRegistry.getSupportedMatchTypes();
    }
}
