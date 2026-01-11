package aml.openwlf.core.rule;

import aml.openwlf.core.model.CustomerInfo;
import aml.openwlf.core.model.MatchedRule;
import aml.openwlf.core.rule.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TestConfig.class)
@TestPropertySource(properties = {
        "rule.config.path=classpath:rules/filtering-rules.yml"
})
class RuleEngineTest {
    
    @Autowired
    private RuleEngine ruleEngine;
    
    @Test
    void shouldMatchExactName() {
        CustomerInfo customer = CustomerInfo.builder()
                .name("John Smith")
                .build();
        
        WatchlistEntry entry = WatchlistEntry.builder()
                .name("John Smith")
                .build();
        
        List<MatchedRule> results = ruleEngine.applyRules(customer, entry);
        
        assertThat(results).isNotEmpty();
        assertThat(results).anyMatch(r -> r.getRuleName().equals("EXACT_NAME_MATCH"));
    }
    
    @Test
    void shouldMatchFuzzyName() {
        CustomerInfo customer = CustomerInfo.builder()
                .name("John Smithe")  // 오타
                .build();
        
        WatchlistEntry entry = WatchlistEntry.builder()
                .name("John Smith")
                .build();
        
        List<MatchedRule> results = ruleEngine.applyRules(customer, entry);
        
        assertThat(results).anyMatch(r -> r.getRuleName().equals("FUZZY_NAME_MATCH"));
    }
    
    @Test
    void shouldMatchAlias() {
        CustomerInfo customer = CustomerInfo.builder()
                .name("Johnny Smith")
                .build();
        
        WatchlistEntry entry = WatchlistEntry.builder()
                .name("John Smith")
                .aliases(List.of("Johnny Smith", "J. Smith"))
                .build();
        
        List<MatchedRule> results = ruleEngine.applyRules(customer, entry);
        
        assertThat(results).anyMatch(r -> r.getRuleName().equals("EXACT_ALIAS_MATCH"));
    }
    
    @Test
    void shouldMatchDateOfBirth() {
        CustomerInfo customer = CustomerInfo.builder()
                .name("Unknown Person")
                .dateOfBirth(LocalDate.of(1985, 5, 15))
                .build();
        
        WatchlistEntry entry = WatchlistEntry.builder()
                .name("John Smith")
                .dateOfBirth(LocalDate.of(1985, 5, 15))
                .build();
        
        List<MatchedRule> results = ruleEngine.applyRules(customer, entry);
        
        assertThat(results).anyMatch(r -> r.getRuleName().equals("DOB_MATCH"));
    }
    
    @Test
    void shouldMatchNationality() {
        CustomerInfo customer = CustomerInfo.builder()
                .name("Unknown Person")
                .nationality("KP")
                .build();
        
        WatchlistEntry entry = WatchlistEntry.builder()
                .name("John Smith")
                .nationality("KP")
                .build();
        
        List<MatchedRule> results = ruleEngine.applyRules(customer, entry);
        
        assertThat(results).anyMatch(r -> r.getRuleName().equals("NATIONALITY_MATCH"));
    }
    
    @Test
    void shouldReturnEmptyWhenNoMatch() {
        CustomerInfo customer = CustomerInfo.builder()
                .name("Completely Different Name")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .nationality("US")
                .build();
        
        WatchlistEntry entry = WatchlistEntry.builder()
                .name("John Smith")
                .dateOfBirth(LocalDate.of(1985, 5, 15))
                .nationality("KP")
                .build();
        
        List<MatchedRule> results = ruleEngine.applyRules(customer, entry);
        
        assertThat(results).isEmpty();
    }
    
    @Test
    void shouldGetCurrentConfiguration() {
        var config = ruleEngine.getCurrentConfiguration();
        
        assertThat(config).isNotNull();
        assertThat(config.getVersion()).isNotBlank();
        assertThat(config.getRules()).isNotEmpty();
    }
    
    @Test
    void shouldGetSupportedMatchTypes() {
        List<String> matchTypes = ruleEngine.getSupportedMatchTypes();
        
        assertThat(matchTypes).contains("EXACT", "FUZZY", "DATE_RANGE", "CONTAINS");
    }
}
