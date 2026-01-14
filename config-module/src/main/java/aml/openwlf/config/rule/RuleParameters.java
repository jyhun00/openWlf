package aml.openwlf.config.rule;

import java.util.Map;
import java.util.Optional;

/**
 * 타입 안전한 규칙 파라미터 접근을 위한 유틸리티 클래스
 *
 * Map<String, Object> 형태의 파라미터에서 타입 안전하게 값을 추출합니다.
 * ClassCastException 없이 안전하게 파라미터 값에 접근할 수 있습니다.
 */
public final class RuleParameters {

    private RuleParameters() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }

    /**
     * Double 타입 파라미터 추출
     *
     * @param parameters 파라미터 맵
     * @param key 파라미터 키
     * @param defaultValue 기본값
     * @return 파라미터 값 또는 기본값
     */
    public static double getDouble(Map<String, Object> parameters, String key, double defaultValue) {
        if (parameters == null || !parameters.containsKey(key)) {
            return defaultValue;
        }
        Object value = parameters.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Integer 타입 파라미터 추출
     *
     * @param parameters 파라미터 맵
     * @param key 파라미터 키
     * @param defaultValue 기본값
     * @return 파라미터 값 또는 기본값
     */
    public static int getInt(Map<String, Object> parameters, String key, int defaultValue) {
        if (parameters == null || !parameters.containsKey(key)) {
            return defaultValue;
        }
        Object value = parameters.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Boolean 타입 파라미터 추출
     *
     * @param parameters 파라미터 맵
     * @param key 파라미터 키
     * @param defaultValue 기본값
     * @return 파라미터 값 또는 기본값
     */
    public static boolean getBoolean(Map<String, Object> parameters, String key, boolean defaultValue) {
        if (parameters == null || !parameters.containsKey(key)) {
            return defaultValue;
        }
        Object value = parameters.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    /**
     * String 타입 파라미터 추출
     *
     * @param parameters 파라미터 맵
     * @param key 파라미터 키
     * @param defaultValue 기본값
     * @return 파라미터 값 또는 기본값
     */
    public static String getString(Map<String, Object> parameters, String key, String defaultValue) {
        if (parameters == null || !parameters.containsKey(key)) {
            return defaultValue;
        }
        Object value = parameters.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }

    /**
     * Optional로 파라미터 추출
     *
     * @param parameters 파라미터 맵
     * @param key 파라미터 키
     * @return Optional 래핑된 값
     */
    public static Optional<Object> get(Map<String, Object> parameters, String key) {
        if (parameters == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(parameters.get(key));
    }
}
