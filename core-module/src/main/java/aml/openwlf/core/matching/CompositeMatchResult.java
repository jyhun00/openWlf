package aml.openwlf.core.matching;

/**
 * 복합 매칭 결과를 담는 레코드
 */
public record CompositeMatchResult(
        double compositeScore,
        double jaroWinklerScore,
        double metaphoneScore,
        double ngramScore,
        double koreanScore,
        boolean metaphoneMatch,
        boolean soundexMatch
) {
    /**
     * 고신뢰도 매칭 여부 확인
     *
     * @param threshold 임계값
     * @return 고신뢰도 매칭 여부
     */
    public boolean isHighConfidenceMatch(double threshold) {
        return compositeScore >= threshold || metaphoneMatch;
    }
}
