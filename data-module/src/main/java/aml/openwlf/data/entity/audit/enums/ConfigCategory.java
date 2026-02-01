package aml.openwlf.data.entity.audit.enums;

/**
 * 시스템 설정 카테고리
 */
public enum ConfigCategory {
    /**
     * 임계값 설정
     */
    THRESHOLD,

    /**
     * 매칭 가중치
     */
    MATCHING_WEIGHT,

    /**
     * 스케줄러 설정
     */
    SCHEDULER,

    /**
     * 동기화 설정
     */
    SYNC_CONFIG,

    /**
     * 기능 플래그
     */
    FEATURE_FLAG,

    /**
     * 보안 설정
     */
    SECURITY,

    /**
     * 알림 설정
     */
    NOTIFICATION,

    /**
     * 통합 설정 (외부 시스템)
     */
    INTEGRATION,

    /**
     * 로깅 설정
     */
    LOGGING
}
