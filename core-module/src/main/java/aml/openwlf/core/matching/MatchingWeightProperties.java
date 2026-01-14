package aml.openwlf.core.matching;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 복합 매칭 알고리즘 가중치 설정
 *
 * application.yml에서 matching.weights 프리픽스로 설정 가능
 */
@Data
@Component
@ConfigurationProperties(prefix = "matching.weights")
public class MatchingWeightProperties {

    /**
     * 한글 이름이 포함된 경우의 가중치
     */
    private KoreanWeights withKorean = new KoreanWeights();

    /**
     * 한글 이름이 없는 경우의 가중치 (영문만)
     */
    private NonKoreanWeights withoutKorean = new NonKoreanWeights();

    @Data
    public static class KoreanWeights {
        private double jaroWinkler = 0.3;
        private double metaphone = 0.2;
        private double ngram = 0.2;
        private double korean = 0.3;
    }

    @Data
    public static class NonKoreanWeights {
        private double jaroWinkler = 0.4;
        private double metaphone = 0.3;
        private double ngram = 0.3;
    }
}
