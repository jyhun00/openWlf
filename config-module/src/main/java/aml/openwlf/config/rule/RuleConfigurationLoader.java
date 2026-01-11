package aml.openwlf.config.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 외부 YAML 파일에서 룰 설정을 로드하는 컴포넌트
 */
@Slf4j
@Component
public class RuleConfigurationLoader {
    
    private final ResourceLoader resourceLoader;
    private final ObjectMapper yamlMapper;
    
    @Value("${rule.config.path:classpath:rules/filtering-rules.yml}")
    private String configPath;
    
    @Value("${rule.config.watch-for-changes:false}")
    private boolean watchForChanges;
    
    private RuleConfiguration cachedConfiguration;
    private long lastModified = 0;
    
    public RuleConfigurationLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.yamlMapper.findAndRegisterModules();
    }
    
    @PostConstruct
    public void init() {
        loadConfiguration();
        log.info("Rule configuration loaded: {} rules (version: {})", 
                cachedConfiguration.getRules().size(),
                cachedConfiguration.getVersion());
    }
    
    /**
     * 현재 룰 설정 반환 (변경 감지 시 자동 리로드)
     */
    public RuleConfiguration getConfiguration() {
        if (watchForChanges && isConfigurationChanged()) {
            log.info("Rule configuration file changed, reloading...");
            loadConfiguration();
        }
        return cachedConfiguration;
    }
    
    /**
     * 설정 파일 강제 리로드
     */
    public synchronized void reload() {
        log.info("Reloading rule configuration from: {}", configPath);
        loadConfiguration();
        log.info("Rule configuration reloaded: {} rules", cachedConfiguration.getRules().size());
    }
    
    /**
     * 설정 파일 로드
     */
    private synchronized void loadConfiguration() {
        try {
            InputStream inputStream;
            
            if (configPath.startsWith("classpath:")) {
                // 클래스패스 리소스
                Resource resource = resourceLoader.getResource(configPath);
                inputStream = resource.getInputStream();
            } else if (configPath.startsWith("file:")) {
                // 외부 파일
                Path filePath = Paths.get(configPath.substring(5));
                inputStream = Files.newInputStream(filePath);
                lastModified = Files.getLastModifiedTime(filePath).toMillis();
            } else {
                // 기본값: 클래스패스
                Resource resource = resourceLoader.getResource("classpath:" + configPath);
                inputStream = resource.getInputStream();
            }
            
            cachedConfiguration = yamlMapper.readValue(inputStream, RuleConfiguration.class);
            inputStream.close();
            
            validateConfiguration(cachedConfiguration);
            
        } catch (IOException e) {
            log.error("Failed to load rule configuration from: {}", configPath, e);
            if (cachedConfiguration == null) {
                // 기본 설정으로 폴백
                cachedConfiguration = createDefaultConfiguration();
                log.warn("Using default rule configuration");
            }
        }
    }
    
    /**
     * 설정 파일 변경 여부 확인
     */
    private boolean isConfigurationChanged() {
        if (!configPath.startsWith("file:")) {
            return false;
        }
        
        try {
            Path filePath = Paths.get(configPath.substring(5));
            long currentModified = Files.getLastModifiedTime(filePath).toMillis();
            return currentModified > lastModified;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 설정 유효성 검증
     */
    private void validateConfiguration(RuleConfiguration config) {
        if (config.getRules() == null || config.getRules().isEmpty()) {
            throw new IllegalStateException("No rules defined in configuration");
        }
        
        for (RuleDefinition rule : config.getRules()) {
            if (rule.getId() == null || rule.getId().isBlank()) {
                throw new IllegalStateException("Rule ID is required");
            }
            if (rule.getCondition() == null) {
                throw new IllegalStateException("Rule condition is required for: " + rule.getId());
            }
            if (rule.getScore() == null) {
                throw new IllegalStateException("Rule score is required for: " + rule.getId());
            }
        }
        
        log.debug("Rule configuration validation passed");
    }
    
    /**
     * 기본 설정 생성 (폴백용)
     */
    private RuleConfiguration createDefaultConfiguration() {
        return RuleConfiguration.builder()
                .version("1.0-default")
                .description("Default fallback configuration")
                .rules(java.util.List.of(
                        RuleDefinition.builder()
                                .id("EXACT_NAME_MATCH")
                                .name("Exact Name Match")
                                .type("NAME")
                                .description("Exact name match after normalization")
                                .enabled(true)
                                .priority(1)
                                .condition(RuleDefinition.MatchCondition.builder()
                                        .matchType("EXACT")
                                        .sourceField("name")
                                        .targetField("name")
                                        .build())
                                .score(RuleDefinition.ScoreConfig.builder()
                                        .exactMatch(100.0)
                                        .build())
                                .build()
                ))
                .build();
    }
}
