package aml.openwlf.core.rule.evaluator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 룰 평가기 레지스트리
 * 매칭 타입별 평가기를 관리
 */
@Slf4j
@Component
public class RuleEvaluatorRegistry {
    
    private final List<RuleEvaluator> evaluators;
    private final Map<String, RuleEvaluator> evaluatorMap = new HashMap<>();
    
    public RuleEvaluatorRegistry(List<RuleEvaluator> evaluators) {
        this.evaluators = evaluators;
    }
    
    @PostConstruct
    public void init() {
        for (RuleEvaluator evaluator : evaluators) {
            String matchType = evaluator.getMatchType().toUpperCase();
            evaluatorMap.put(matchType, evaluator);
            log.info("Registered rule evaluator: {} -> {}", 
                    matchType, evaluator.getClass().getSimpleName());
        }
    }
    
    /**
     * 매칭 타입에 해당하는 평가기 반환
     */
    public RuleEvaluator getEvaluator(String matchType) {
        RuleEvaluator evaluator = evaluatorMap.get(matchType.toUpperCase());
        if (evaluator == null) {
            throw new IllegalArgumentException("No evaluator found for match type: " + matchType);
        }
        return evaluator;
    }
    
    /**
     * 지원하는 매칭 타입 목록
     */
    public List<String> getSupportedMatchTypes() {
        return evaluatorMap.keySet().stream().sorted().toList();
    }
    
    /**
     * 매칭 타입 지원 여부
     */
    public boolean isSupported(String matchType) {
        return evaluatorMap.containsKey(matchType.toUpperCase());
    }
}
