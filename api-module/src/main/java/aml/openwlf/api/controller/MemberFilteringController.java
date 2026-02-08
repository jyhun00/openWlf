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
 * REST API Controller for member registration watchlist filtering
 */
@Slf4j
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
@Tag(name = "Member Registration Filtering", description = "APIs for member registration watchlist filtering")
public class MemberFilteringController {

    private static final double REGISTRATION_THRESHOLD = 70.0;

    private final FilteringService filteringService;
    private final FilteringHistoryService historyService;
    private final AlertService alertService;

    @PostMapping("/filter")
    @Operation(
            summary = "Filter member against watchlists during registration",
            description = "Checks member information against all active watchlists and determines if registration should be allowed. " +
                    "If score >= 70, registration is blocked. If score >= 50, an alert is created."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Filtering completed successfully",
                    content = @Content(schema = @Schema(implementation = MemberFilteringResponse.class))
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
    public ResponseEntity<MemberFilteringResponse> filterMember(
            @Parameter(description = "Member information to filter", required = true)
            @Valid @RequestBody MemberFilterRequest request) {

        log.info("Received member filtering request for: {}", request.getName());

        // Convert DTO to domain model (using CustomerInfo for compatibility)
        CustomerInfo customerInfo = CustomerInfo.builder()
                .name(request.getName())
                .dateOfBirth(request.getDateOfBirth())
                .nationality(request.getNationality())
                .build();

        // Perform filtering
        FilteringResult result = filteringService.filterCustomer(customerInfo);

        // Save to history
        try {
            historyService.saveFilteringResult(result);
        } catch (Exception e) {
            log.error("Failed to save filtering history", e);
        }

        // Create alert if score >= 50
        String alertReference = null;
        try {
            Optional<AlertEntity> alert = alertService.createAlertIfNeeded(result);
            if (alert.isPresent()) {
                alertReference = alert.get().getAlertReference();
                log.info("Alert created: {} for member: {}", alertReference, request.getName());
            }
        } catch (Exception e) {
            log.error("Failed to create alert", e);
        }

        // Determine registration eligibility
        boolean registrationAllowed = result.getScore() < REGISTRATION_THRESHOLD;
        String rejectionReason = null;
        if (!registrationAllowed) {
            rejectionReason = String.format(
                    "Watchlist 필터링 점수(%.1f)가 임계값(%.1f)을 초과했습니다.",
                    result.getScore(), REGISTRATION_THRESHOLD);
        }

        // Build response
        MemberFilteringResponse response = MemberFilteringResponse.builder()
                .alert(result.isAlert())
                .score(result.getScore())
                .matchedRules(result.getMatchedRules().stream()
                        .map(this::toMatchedRuleDto)
                        .collect(Collectors.toList()))
                .registrationAllowed(registrationAllowed)
                .rejectionReason(rejectionReason)
                .alertReference(alertReference)
                .memberInfo(toMemberInfoDto(request))
                .build();

        log.info("Member filtering completed: name={}, score={}, allowed={}",
                request.getName(), response.getScore(), response.isRegistrationAllowed());

        return ResponseEntity.ok(response);
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

    private MemberInfoDto toMemberInfoDto(MemberFilterRequest request) {
        return MemberInfoDto.builder()
                .name(request.getName())
                .dateOfBirth(request.getDateOfBirth())
                .nationality(request.getNationality())
                .address(request.getAddress())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .build();
    }
}
