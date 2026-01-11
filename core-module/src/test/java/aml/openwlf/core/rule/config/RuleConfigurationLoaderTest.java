package aml.openwlf.core.rule.config;

import aml.openwlf.config.rule.RuleConfiguration;
import aml.openwlf.config.rule.RuleConfigurationLoader;
import aml.openwlf.config.rule.RuleDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TestConfig.class)
@TestPropertySource(properties = {
        "rule.config.path=classpath:rules/filtering-rules.yml"
})
class RuleConfigurationLoaderTest {
    
    @Autowired
    private RuleConfigurationLoader configLoader;
    
    @Test
    void shouldLoadRulesFromYamlFile() {
        RuleConfiguration config = configLoader.getConfiguration();
        
        assertThat(config).isNotNull();
        assertThat(config.getVersion()).isEqualTo("1.0.0");
        assertThat(config.getRules()).isNotEmpty();
    }
    
    @Test
    void shouldHaveEnabledRules() {
        RuleConfiguration config = configLoader.getConfiguration();
        
        assertThat(config.getEnabledRules()).isNotEmpty();
        assertThat(config.getEnabledRules())
                .allMatch(RuleDefinition::isEnabled);
    }
    
    @Test
    void shouldFindRuleById() {
        RuleConfiguration config = configLoader.getConfiguration();
        
        RuleDefinition exactNameRule = config.findRuleById("EXACT_NAME_MATCH");
        
        assertThat(exactNameRule).isNotNull();
        assertThat(exactNameRule.getType()).isEqualTo("NAME");
        assertThat(exactNameRule.getCondition().getMatchType()).isEqualTo("EXACT");
    }
    
    @Test
    void shouldSortRulesByPriority() {
        RuleConfiguration config = configLoader.getConfiguration();
        
        var enabledRules = config.getEnabledRules();
        
        for (int i = 1; i < enabledRules.size(); i++) {
            assertThat(enabledRules.get(i).getPriority())
                    .isGreaterThanOrEqualTo(enabledRules.get(i - 1).getPriority());
        }
    }
    
    @Test
    void shouldParseScoreConfig() {
        RuleConfiguration config = configLoader.getConfiguration();
        RuleDefinition rule = config.findRuleById("EXACT_NAME_MATCH");
        
        assertThat(rule.getScore()).isNotNull();
        assertThat(rule.getScore().getExactMatch()).isEqualTo(100.0);
    }
    
    @Test
    void shouldParseConditionParameters() {
        RuleConfiguration config = configLoader.getConfiguration();
        RuleDefinition rule = config.findRuleById("FUZZY_NAME_MATCH");
        
        assertThat(rule.getCondition().getParameters()).isNotNull();
        Double threshold = rule.getCondition().getParameter("similarityThreshold", 0.0);
        assertThat(threshold).isEqualTo(0.8);
    }
}
