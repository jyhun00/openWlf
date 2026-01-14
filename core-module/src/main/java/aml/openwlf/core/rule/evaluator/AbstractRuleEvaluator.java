package aml.openwlf.core.rule.evaluator;

import aml.openwlf.config.rule.RuleDefinition;
import aml.openwlf.core.model.CustomerInfo;
import aml.openwlf.core.model.MatchedRule;
import aml.openwlf.core.rule.WatchlistEntry;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * RuleEvaluator의 추상 기본 클래스
 *
 * 모든 RuleEvaluator 구현체에서 공통으로 사용하는 로직을 제공합니다:
 * - 점수 계산 로직
 * - 필드 값 추출 (FieldValueExtractor 위임)
 * - 유효성 검사
 */
@RequiredArgsConstructor
public abstract class AbstractRuleEvaluator implements RuleEvaluator {

    protected final FieldValueExtractor fieldExtractor;

    /**
     * 유사도 기반 점수 계산
     *
     * @param similarity 유사도 (0.0 ~ 1.0)
     * @param scoreConfig 점수 설정
     * @return 계산된 점수
     */
    protected double calculateScore(double similarity, RuleDefinition.ScoreConfig scoreConfig) {
        if (scoreConfig.isProportionalToSimilarity()) {
            return similarity * scoreConfig.getMaxScore();
        }
        return scoreConfig.getPartialMatch();
    }

    /**
     * 정확 매칭 점수 반환
     *
     * @param scoreConfig 점수 설정
     * @return 정확 매칭 점수
     */
    protected double getExactMatchScore(RuleDefinition.ScoreConfig scoreConfig) {
        return scoreConfig.getExactMatch();
    }

    /**
     * CustomerInfo에서 필드 값 추출
     *
     * @param customer 고객 정보
     * @param field 필드 이름
     * @return 필드 값
     */
    protected String getFieldValue(CustomerInfo customer, String field) {
        return fieldExtractor.getCustomerFieldValue(customer, field);
    }

    /**
     * WatchlistEntry에서 필드 값 목록 추출
     *
     * @param entry 감시 목록 항목
     * @param field 필드 이름
     * @return 필드 값 목록
     */
    protected List<String> getTargetFieldValues(WatchlistEntry entry, String field) {
        return fieldExtractor.getWatchlistFieldValues(entry, field);
    }

    /**
     * 소스 값이 유효한지 확인
     *
     * @param sourceValue 소스 값
     * @return 유효 여부
     */
    protected boolean isValidSourceValue(String sourceValue) {
        return sourceValue != null && !sourceValue.isBlank();
    }

    /**
     * 타겟 값이 유효한지 확인
     *
     * @param targetValue 타겟 값
     * @return 유효 여부
     */
    protected boolean isValidTargetValue(String targetValue) {
        return targetValue != null && !targetValue.isBlank();
    }

    /**
     * MatchedRule 빌더 생성 헬퍼
     *
     * @param rule 규칙 정의
     * @param score 점수
     * @param matchedValue 매칭된 값
     * @param targetValue 타겟 값
     * @param description 설명
     * @return 생성된 MatchedRule
     */
    protected MatchedRule buildMatchedRule(RuleDefinition rule, double score,
                                           String matchedValue, String targetValue, String description) {
        return MatchedRule.builder()
                .ruleName(rule.getId())
                .ruleType(rule.getType())
                .score(score)
                .matchedValue(matchedValue)
                .targetValue(targetValue)
                .description(description)
                .build();
    }
}
