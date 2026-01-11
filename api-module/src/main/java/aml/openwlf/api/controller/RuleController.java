package aml.openwlf.api.controller;

import aml.openwlf.config.rule.RuleConfiguration;
import aml.openwlf.config.rule.RuleDefinition;
import aml.openwlf.core.rule.RuleEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 룰 설정 관리 API Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
@Tag(name = "Rule Management", description = "필터링 룰 설정 조회 및 관리 API")
public class RuleController {
    
    private final RuleEngine ruleEngine;
    
    @GetMapping
    @Operation(
            summary = "전체 룰 설정 조회",
            description = "현재 로드된 전체 룰 설정을 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<RuleConfiguration> getConfiguration() {
        log.info("Fetching current rule configuration");
        return ResponseEntity.ok(ruleEngine.getCurrentConfiguration());
    }
    
    @GetMapping("/enabled")
    @Operation(
            summary = "활성화된 룰 목록 조회",
            description = "현재 활성화된 룰만 우선순위 순으로 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<List<RuleDefinition>> getEnabledRules() {
        log.info("Fetching enabled rules");
        List<RuleDefinition> enabledRules = ruleEngine.getCurrentConfiguration().getEnabledRules();
        return ResponseEntity.ok(enabledRules);
    }
    
    @GetMapping("/{ruleId}")
    @Operation(
            summary = "특정 룰 조회",
            description = "ID로 특정 룰 설정을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "룰을 찾을 수 없음")
    })
    public ResponseEntity<RuleDefinition> getRuleById(@PathVariable String ruleId) {
        log.info("Fetching rule: {}", ruleId);
        RuleDefinition rule = ruleEngine.getCurrentConfiguration().findRuleById(ruleId);
        
        if (rule == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(rule);
    }
    
    @PostMapping("/reload")
    @Operation(
            summary = "룰 설정 리로드",
            description = "외부 설정 파일에서 룰을 다시 로드합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리로드 성공"),
            @ApiResponse(responseCode = "500", description = "리로드 실패")
    })
    public ResponseEntity<Map<String, Object>> reloadConfiguration() {
        log.info("Reloading rule configuration");
        
        try {
            ruleEngine.reloadConfiguration();
            RuleConfiguration config = ruleEngine.getCurrentConfiguration();
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Rule configuration reloaded successfully",
                    "version", config.getVersion(),
                    "totalRules", config.getRules().size(),
                    "enabledRules", config.getEnabledRules().size()
            ));
        } catch (Exception e) {
            log.error("Failed to reload rule configuration", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to reload: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/match-types")
    @Operation(
            summary = "지원하는 매칭 타입 목록",
            description = "현재 시스템에서 지원하는 매칭 타입 목록을 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<List<String>> getSupportedMatchTypes() {
        return ResponseEntity.ok(ruleEngine.getSupportedMatchTypes());
    }
    
    @GetMapping("/stats")
    @Operation(
            summary = "룰 통계 조회",
            description = "룰 설정에 대한 통계 정보를 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        RuleConfiguration config = ruleEngine.getCurrentConfiguration();
        List<RuleDefinition> enabledRules = config.getEnabledRules();
        
        // 타입별 룰 수 집계
        Map<String, Long> rulesByType = config.getRules().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        RuleDefinition::getType,
                        java.util.stream.Collectors.counting()
                ));
        
        // 매칭 타입별 룰 수 집계
        Map<String, Long> rulesByMatchType = config.getRules().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        r -> r.getCondition().getMatchType(),
                        java.util.stream.Collectors.counting()
                ));
        
        return ResponseEntity.ok(Map.of(
                "version", config.getVersion(),
                "description", config.getDescription() != null ? config.getDescription() : "",
                "totalRules", config.getRules().size(),
                "enabledRules", enabledRules.size(),
                "disabledRules", config.getRules().size() - enabledRules.size(),
                "rulesByType", rulesByType,
                "rulesByMatchType", rulesByMatchType,
                "supportedMatchTypes", ruleEngine.getSupportedMatchTypes()
        ));
    }
}
