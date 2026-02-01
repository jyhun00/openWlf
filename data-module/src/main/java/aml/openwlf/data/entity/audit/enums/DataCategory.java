package aml.openwlf.data.entity.audit.enums;

/**
 * 민감 데이터 카테고리
 */
public enum DataCategory {
    /**
     * 제재 대상자 정보
     */
    SANCTIONS,

    /**
     * 고객 개인정보
     */
    CUSTOMER_PII,

    /**
     * Alert 상세 정보
     */
    ALERT_DETAIL,

    /**
     * Case 상세 정보
     */
    CASE_DETAIL,

    /**
     * 감시목록 데이터
     */
    WATCHLIST,

    /**
     * 필터링 이력
     */
    FILTERING_HISTORY,

    /**
     * 규칙 설정
     */
    RULE_CONFIG,

    /**
     * 시스템 설정
     */
    SYSTEM_CONFIG
}
