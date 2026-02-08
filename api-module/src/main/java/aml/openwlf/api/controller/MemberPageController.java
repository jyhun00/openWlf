package aml.openwlf.api.controller;

import aml.openwlf.api.dto.*;
import aml.openwlf.core.filtering.FilteringService;
import aml.openwlf.core.model.CustomerInfo;
import aml.openwlf.core.model.FilteringResult;
import aml.openwlf.core.model.MatchedRule;
import aml.openwlf.data.entity.AlertEntity;
import aml.openwlf.data.service.AlertService;
import aml.openwlf.data.service.FilteringHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Thymeleaf page controller for member registration with watchlist filtering
 */
@Slf4j
@Controller
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberPageController {

    private static final double REGISTRATION_THRESHOLD = 70.0;

    private final FilteringService filteringService;
    private final FilteringHistoryService historyService;
    private final AlertService alertService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("memberRequest", new MemberFilterRequest());
        return "member/register";
    }

    @PostMapping("/register")
    public String processRegistration(
            @Valid @ModelAttribute("memberRequest") MemberFilterRequest request,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "member/register";
        }

        log.info("Processing member registration for: {}", request.getName());

        // Convert DTO to domain model
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

        model.addAttribute("result", response);
        return "member/result";
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
