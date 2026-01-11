package aml.openwlf.core.rule.config;

import aml.openwlf.config.rule.RuleConfigurationLoader;
import aml.openwlf.core.normalization.NormalizationService;
import aml.openwlf.core.rule.RuleEngine;
import aml.openwlf.core.rule.evaluator.*;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;

import java.util.List;

@SpringBootConfiguration
@EnableAutoConfiguration
public class TestConfig {

    @Bean
    public NormalizationService normalizationService() {
        return new NormalizationService();
    }

    @Bean
    @Primary
    public RuleConfigurationLoader ruleConfigurationLoader(ResourceLoader resourceLoader) {
        return new RuleConfigurationLoader(resourceLoader);
    }

    @Bean
    public ExactMatchEvaluator exactMatchEvaluator(NormalizationService normalizationService) {
        return new ExactMatchEvaluator(normalizationService);
    }

    @Bean
    public FuzzyMatchEvaluator fuzzyMatchEvaluator(NormalizationService normalizationService) {
        return new FuzzyMatchEvaluator(normalizationService);
    }

    @Bean
    public DateRangeMatchEvaluator dateRangeMatchEvaluator() {
        return new DateRangeMatchEvaluator();
    }

    @Bean
    public ContainsMatchEvaluator containsMatchEvaluator(NormalizationService normalizationService) {
        return new ContainsMatchEvaluator(normalizationService);
    }

    @Bean
    public RuleEvaluatorRegistry ruleEvaluatorRegistry(List<RuleEvaluator> evaluators) {
        return new RuleEvaluatorRegistry(evaluators);
    }

    @Bean
    public RuleEngine ruleEngine(RuleConfigurationLoader configLoader, RuleEvaluatorRegistry registry) {
        return new RuleEngine(configLoader, registry);
    }
}
