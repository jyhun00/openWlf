package aml.openwlf.config.rule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 전체 룰 설정을 담는 루트 클래스
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleConfiguration {
    
    /**
     * 설정 버전
     */
    private String version;
    
    /**
     * 설정 설명
     */
    private String description;
    
    /**
     * 룰 목록
     */
    @Builder.Default
    private List<RuleDefinition> rules = new ArrayList<>();
    
    /**
     * 활성화된 룰만 필터링하여 우선순위 순으로 반환
     */
    public List<RuleDefinition> getEnabledRules() {
        return rules.stream()
                .filter(RuleDefinition::isEnabled)
                .sorted((r1, r2) -> Integer.compare(r1.getPriority(), r2.getPriority()))
                .toList();
    }
    
    /**
     * ID로 룰 찾기
     */
    public RuleDefinition findRuleById(String id) {
        return rules.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}
