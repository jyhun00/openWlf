package aml.openwlf.core.rule.evaluator;

import aml.openwlf.config.rule.RuleDefinition;
import aml.openwlf.core.model.CustomerInfo;
import aml.openwlf.core.model.MatchedRule;
import aml.openwlf.core.rule.WatchlistEntry;

import java.util.List;

/**
 * 룰 평가기 인터페이스
 * 각 매칭 타입별로 구현체를 제공
 */
public interface RuleEvaluator {
    
    /**
     * 이 평가기가 처리할 수 있는 매칭 타입
     */
    String getMatchType();
    
    /**
     * 룰 평가 실행
     * 
     * @param customer 고객 정보
     * @param entry 감시목록 항목
     * @param rule 룰 정의
     * @return 매칭된 룰 목록 (매칭되지 않으면 빈 리스트)
     */
    List<MatchedRule> evaluate(CustomerInfo customer, WatchlistEntry entry, RuleDefinition rule);
}
