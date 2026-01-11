package aml.openwlf.config.rule;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 룰 설정 관련 프로퍼티
 */
@Data
@Component
@ConfigurationProperties(prefix = "rule.config")
public class RuleConfigurationProperties {
    
    /**
     * 룰 설정 파일 경로
     * - classpath:rules/filtering-rules.yml
     * - file:/path/to/filtering-rules.yml
     */
    private String path = "classpath:rules/filtering-rules.yml";
    
    /**
     * 외부 파일 변경 감지 여부 (file: 경로만 지원)
     */
    private boolean watchForChanges = false;
}
