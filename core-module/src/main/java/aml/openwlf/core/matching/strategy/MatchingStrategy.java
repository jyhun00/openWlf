package aml.openwlf.core.matching.strategy;

/**
 * 매칭 알고리즘 전략 인터페이스
 *
 * 각 매칭 알고리즘을 캡슐화하여 OCP(개방-폐쇄 원칙)를 준수합니다.
 * 새로운 알고리즘 추가 시 이 인터페이스를 구현하면 됩니다.
 */
public interface MatchingStrategy {

    /**
     * 두 문자열의 유사도 계산
     *
     * @param str1 첫 번째 문자열
     * @param str2 두 번째 문자열
     * @return 0.0 ~ 1.0 사이의 유사도
     */
    double calculateSimilarity(String str1, String str2);

    /**
     * 두 문자열이 매칭되는지 여부 확인
     *
     * @param str1 첫 번째 문자열
     * @param str2 두 번째 문자열
     * @return 매칭 여부
     */
    boolean matches(String str1, String str2);

    /**
     * 전략의 이름 반환
     *
     * @return 전략 이름 (예: "SOUNDEX", "METAPHONE")
     */
    String getStrategyName();

    /**
     * 이 전략이 주어진 문자열에 적용 가능한지 확인
     *
     * @param str 대상 문자열
     * @return 적용 가능 여부
     */
    default boolean isApplicable(String str) {
        return str != null && !str.isBlank();
    }
}
