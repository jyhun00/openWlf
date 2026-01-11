package aml.openwlf.api.controller;

import aml.openwlf.api.dto.*;
import aml.openwlf.core.filtering.FilteringService;
import aml.openwlf.core.model.CustomerInfo;
import aml.openwlf.core.model.FilteringResult;
import aml.openwlf.core.model.MatchedRule;
import aml.openwlf.data.entity.AlertEntity;
import aml.openwlf.data.service.AlertService;
import aml.openwlf.data.service.FilteringHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST API Controller for watchlist filtering
 */
@Slf4j
@RestController
@RequestMapping("/api/filter")
@RequiredArgsConstructor
@Tag(name = "Watchlist Filtering", description = "APIs for watchlist filtering operations")
public class FilteringController {
    
    private final FilteringService filteringService;
    private final FilteringHistoryService historyService;
    private final AlertService alertService;
    
    @PostMapping("/customer")
    @Operation(
            summary = "Filter customer against watchlists",
            description = "Checks customer information against all active watchlists and returns risk assessment. " +
                    "If score >= 50, an alert is automatically created with NEW status."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Filtering completed successfully",
                    content = @Content(schema = @Schema(implementation = FilteringResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<FilteringResponse> filterCustomer(
            @Parameter(description = "Customer information to filter", required = true)
            @Valid @RequestBody CustomerFilterRequest request) {
        
        log.info("Received filtering request for customer: {}", request.getName());
        
        // Convert DTO to domain model
        CustomerInfo customerInfo = CustomerInfo.builder()
                .name(request.getName())
                .dateOfBirth(request.getDateOfBirth())
                .nationality(request.getNationality())
                .customerId(request.getCustomerId())
                .build();
        
        // Perform filtering
        FilteringResult result = filteringService.filterCustomer(customerInfo);
        
        // Save to history (async would be better in production)
        try {
            historyService.saveFilteringResult(result);
        } catch (Exception e) {
            log.error("Failed to save filtering history", e);
            // Don't fail the request if history save fails
        }
        
        // Create alert if score >= 50
        String alertReference = null;
        try {
            Optional<AlertEntity> alert = alertService.createAlertIfNeeded(result);
            if (alert.isPresent()) {
                alertReference = alert.get().getAlertReference();
                log.info("Alert created: {} for customer: {}", alertReference, request.getName());
            }
        } catch (Exception e) {
            log.error("Failed to create alert", e);
            // Don't fail the request if alert creation fails
        }
        
        // Convert to response DTO
        FilteringResponse response = toFilteringResponse(result, alertReference);
        
        log.info("Filtering completed: alert={}, score={}, alertReference={}", 
                response.isAlert(), response.getScore(), alertReference);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the filtering service is operational")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Watchlist Filtering Service is operational");
    }
    
    private FilteringResponse toFilteringResponse(FilteringResult result, String alertReference) {
        return FilteringResponse.builder()
                .alert(result.isAlert())
                .score(result.getScore())
                .matchedRules(result.getMatchedRules().stream()
                        .map(this::toMatchedRuleDto)
                        .collect(Collectors.toList()))
                .explanation(result.getExplanation())
                .customerInfo(toCustomerInfoDto(result.getCustomerInfo()))
                .alertReference(alertReference)
                .build();
    }
    
    private MatchedRuleDto toMatchedRuleDto(MatchedRule rule) {
        return MatchedRuleDto.builder()
                .ruleName(rule.getRuleName())
                .ruleType(rule.getRuleType())
                .score(rule.getScore())
                .matchedValue(rule.getMatchedValue())
                .targetValue(rule.getTargetValue())
                .description(rule.getDescription())
                .build();
    }
    
    private CustomerInfoDto toCustomerInfoDto(CustomerInfo info) {
        return CustomerInfoDto.builder()
                .name(info.getName())
                .dateOfBirth(info.getDateOfBirth())
                .nationality(info.getNationality())
                .customerId(info.getCustomerId())
                .build();
    }
}
