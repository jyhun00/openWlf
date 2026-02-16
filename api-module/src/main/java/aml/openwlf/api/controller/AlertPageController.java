package aml.openwlf.api.controller;

import aml.openwlf.data.entity.AlertEntity;
import aml.openwlf.data.entity.AlertEntity.AlertStatus;
import aml.openwlf.data.service.AlertService;
import aml.openwlf.data.service.case_.CaseAlertLinkService;
import aml.openwlf.api.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Slf4j
@Controller
@RequestMapping("/alerts")
@RequiredArgsConstructor
public class AlertPageController {

    private final AlertService alertService;
    private final CaseAlertLinkService caseAlertLinkService;

    @GetMapping
    public String listAlerts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) Double minScore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size);
        AlertStatus alertStatus = null;
        if (status != null && !status.isEmpty()) {
            alertStatus = AlertStatus.valueOf(status);
        }

        Page<AlertEntity> alerts = alertService.searchAlerts(alertStatus, customerId, minScore, pageable);

        model.addAttribute("alerts", alerts);
        model.addAttribute("statuses", AlertStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedCustomerId", customerId);
        model.addAttribute("selectedMinScore", minScore);

        return "alert/list";
    }

    @GetMapping("/{id}")
    public String alertDetail(@PathVariable Long id, Model model) {
        AlertEntity alert = alertService.getAlertById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + id));

        Long linkedCaseId = caseAlertLinkService.findCaseIdByAlertId(id);

        model.addAttribute("alert", alert);
        model.addAttribute("linkedCaseId", linkedCaseId);
        model.addAttribute("statuses", AlertStatus.values());

        return "alert/detail";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String comment,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            AlertStatus newStatus = AlertStatus.valueOf(status);
            String updatedBy = userDetails != null ? userDetails.getUsername() : "unknown";
            alertService.updateStatus(id, newStatus, updatedBy);
            log.info("Alert {} status updated to {} by {}", id, status, updatedBy);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "상태 변경 실패: " + e.getMessage());
        }

        return "redirect:/alerts/" + id;
    }

    @PostMapping("/{id}/assign")
    public String assignAlert(
            @PathVariable Long id,
            @RequestParam String assignedTo,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (assignedTo == null || assignedTo.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "담당자를 입력해주세요.");
            return "redirect:/alerts/" + id;
        }

        try {
            alertService.assignAlert(id, assignedTo.trim());
            log.info("Alert {} assigned to {} by {}", id, assignedTo.trim(),
                    userDetails != null ? userDetails.getUsername() : "unknown");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "담당자 배정 실패: " + e.getMessage());
        }

        return "redirect:/alerts/" + id;
    }
}
