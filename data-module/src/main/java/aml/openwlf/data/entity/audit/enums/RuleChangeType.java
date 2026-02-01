package aml.openwlf.data.entity.audit.enums;

/**
 * 규칙 변경 타입
 */
public enum RuleChangeType {
    /**
     * 규칙 생성
     */
    RULE_CREATED,

    /**
     * 규칙 수정
     */
    RULE_UPDATED,

    /**
     * 규칙 삭제
     */
    RULE_DELETED,

    /**
     * 규칙 활성화
     */
    RULE_ENABLED,

    /**
     * 규칙 비활성화
     */
    RULE_DISABLED,

    /**
     * 임계값 변경
     */
    THRESHOLD_CHANGED,

    /**
     * 가중치 변경
     */
    WEIGHT_CHANGED,

    /**
     * 설정 파일 재로드
     */
    CONFIG_RELOADED,

    /**
     * 매칭 알고리즘 변경
     */
    MATCH_TYPE_CHANGED,

    /**
     * 우선순위 변경
     */
    PRIORITY_CHANGED
}
