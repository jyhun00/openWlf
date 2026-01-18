package aml.openwlf.data.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 케이스 우선순위 관련 설정
 *
 * OOP 원칙: 개방-폐쇄 원칙 (OCP)
 * - 하드코딩된 값들을 설정으로 외부화하여 수정에 닫혀있고 확장에 열림
 * - application.yml에서 값을 변경 가능
 *
 * 사용 예:
 * <pre>
 * case:
 *   priority:
 *     critical-threshold: 90
 *     high-threshold: 70
 *     medium-threshold: 50
 *     due-days:
 *       critical: 1
 *       high: 3
 *       medium: 7
 *       low: 14
 * </pre>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "case.priority")
public class CasePriorityProperties {

    /**
     * CRITICAL 우선순위 점수 임계값 (이상)
     */
    private int criticalThreshold = 90;

    /**
     * HIGH 우선순위 점수 임계값 (이상)
     */
    private int highThreshold = 70;

    /**
     * MEDIUM 우선순위 점수 임계값 (이상)
     */
    private int mediumThreshold = 50;

    /**
     * 우선순위별 처리 기한 (일)
     */
    private DueDays dueDays = new DueDays();

    @Getter
    @Setter
    public static class DueDays {
        private int critical = 1;
        private int high = 3;
        private int medium = 7;
        private int low = 14;
    }
}
