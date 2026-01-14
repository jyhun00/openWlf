package aml.openwlf.core.rule.config;

import aml.openwlf.config.rule.RuleConfigurationLoader;
import aml.openwlf.core.matching.AdvancedMatchingService;
import aml.openwlf.core.matching.strategy.*;
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
    public FieldValueExtractor fieldValueExtractor() {
        return new FieldValueExtractor();
    }

    // Matching Strategies
    @Bean
    public SoundexMatchingStrategy soundexMatchingStrategy() {
        return new SoundexMatchingStrategy();
    }

    @Bean
    public MetaphoneMatchingStrategy metaphoneMatchingStrategy() {
        return new MetaphoneMatchingStrategy();
    }

    @Bean
    public JaroWinklerMatchingStrategy jaroWinklerMatchingStrategy() {
        return new JaroWinklerMatchingStrategy();
    }

    @Bean
    public NGramMatchingStrategy ngramMatchingStrategy() {
        return new NGramMatchingStrategy();
    }

    @Bean
    public KoreanNameMatchingStrategy koreanNameMatchingStrategy() {
        return new KoreanNameMatchingStrategy();
    }

    @Bean
    public AdvancedMatchingService advancedMatchingService(
            SoundexMatchingStrategy soundexStrategy,
            MetaphoneMatchingStrategy metaphoneStrategy,
            JaroWinklerMatchingStrategy jaroWinklerStrategy,
            NGramMatchingStrategy ngramStrategy,
            KoreanNameMatchingStrategy koreanStrategy) {
        return new AdvancedMatchingService(
                soundexStrategy,
                metaphoneStrategy,
                jaroWinklerStrategy,
                ngramStrategy,
                koreanStrategy
        );
    }

    // Rule Evaluators
    @Bean
    public ExactMatchEvaluator exactMatchEvaluator(FieldValueExtractor fieldExtractor,
                                                   NormalizationService normalizationService) {
        return new ExactMatchEvaluator(fieldExtractor, normalizationService);
    }

    @Bean
    public FuzzyMatchEvaluator fuzzyMatchEvaluator(FieldValueExtractor fieldExtractor,
                                                   NormalizationService normalizationService) {
        return new FuzzyMatchEvaluator(fieldExtractor, normalizationService);
    }

    @Bean
    public DateRangeMatchEvaluator dateRangeMatchEvaluator(FieldValueExtractor fieldExtractor) {
        return new DateRangeMatchEvaluator(fieldExtractor);
    }

    @Bean
    public ContainsMatchEvaluator containsMatchEvaluator(FieldValueExtractor fieldExtractor,
                                                         NormalizationService normalizationService) {
        return new ContainsMatchEvaluator(fieldExtractor, normalizationService);
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
