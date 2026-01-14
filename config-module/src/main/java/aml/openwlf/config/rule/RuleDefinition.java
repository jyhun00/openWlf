package aml.openwlf.config.rule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 외부 파일에서 로드된 룰 정의
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleDefinition {
    
    /**
     * 룰 고유 식별자 (예: EXACT_NAME_MATCH)
     */
    private String id;
    
    /**
     * 룰 이름 (표시용)
     */
    private String name;
    
    /**
     * 룰 타입 (NAME, DOB, NATIONALITY, ALIAS 등)
     */
    private String type;
    
    /**
     * 룰 설명
     */
    private String description;
    
    /**
     * 룰 활성화 여부
     */
    private boolean enabled;
    
    /**
     * 우선순위 (낮을수록 먼저 실행)
     */
    private int priority;
    
    /**
     * 매칭 조건
     */
    private MatchCondition condition;
    
    /**
     * 점수 설정
     */
    private ScoreConfig score;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchCondition {
        /**
         * 매칭 타입: EXACT, FUZZY, CONTAINS, DATE_RANGE
         */
        private String matchType;
        
        /**
         * 비교 대상 필드 (customer 측): name, dateOfBirth, nationality
         */
        private String sourceField;
        
        /**
         * 비교 대상 필드 (watchlist 측): name, aliases, dateOfBirth, nationality
         */
        private String targetField;
        
        /**
         * 추가 파라미터 (예: similarityThreshold, dateRangeDays)
         */
        private Map<String, Object> parameters;
        
        /**
         * 파라미터 값을 타입 안전하게 가져오기
         *
         * 지원 타입: Double, Integer, Boolean, String
         * 타입 변환을 자동으로 수행하여 ClassCastException을 방지합니다.
         *
         * @param key 파라미터 키
         * @param defaultValue 기본값 (타입 추론에 사용)
         * @return 파라미터 값 또는 기본값
         */
        @SuppressWarnings("unchecked")
        public <T> T getParameter(String key, T defaultValue) {
            if (parameters == null || !parameters.containsKey(key)) {
                return defaultValue;
            }

            // 타입별 안전한 변환
            if (defaultValue instanceof Double) {
                return (T) (Double) RuleParameters.getDouble(parameters, key, (Double) defaultValue);
            }
            if (defaultValue instanceof Integer) {
                return (T) (Integer) RuleParameters.getInt(parameters, key, (Integer) defaultValue);
            }
            if (defaultValue instanceof Boolean) {
                return (T) (Boolean) RuleParameters.getBoolean(parameters, key, (Boolean) defaultValue);
            }
            if (defaultValue instanceof String) {
                return (T) RuleParameters.getString(parameters, key, (String) defaultValue);
            }

            // 기타 타입은 그대로 반환 시도
            Object value = parameters.get(key);
            try {
                return (T) value;
            } catch (ClassCastException e) {
                return defaultValue;
            }
        }

        /**
         * Double 파라미터 추출 (타입 안전)
         */
        public double getDoubleParameter(String key, double defaultValue) {
            return RuleParameters.getDouble(parameters, key, defaultValue);
        }

        /**
         * Integer 파라미터 추출 (타입 안전)
         */
        public int getIntParameter(String key, int defaultValue) {
            return RuleParameters.getInt(parameters, key, defaultValue);
        }

        /**
         * Boolean 파라미터 추출 (타입 안전)
         */
        public boolean getBooleanParameter(String key, boolean defaultValue) {
            return RuleParameters.getBoolean(parameters, key, defaultValue);
        }

        /**
         * String 파라미터 추출 (타입 안전)
         */
        public String getStringParameter(String key, String defaultValue) {
            return RuleParameters.getString(parameters, key, defaultValue);
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreConfig {
        /**
         * 완전 일치 시 점수
         */
        private double exactMatch;
        
        /**
         * 부분/유사 일치 시 기본 점수
         */
        private double partialMatch;
        
        /**
         * 유사도 기반 점수 계산 여부
         */
        private boolean proportionalToSimilarity;
        
        /**
         * 최대 점수 (proportionalToSimilarity 사용 시)
         */
        private double maxScore;
    }
}
