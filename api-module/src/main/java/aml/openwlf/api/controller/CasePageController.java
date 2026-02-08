package aml.openwlf.api.controller;

import aml.openwlf.data.entity.CaseActivityEntity;
import aml.openwlf.data.entity.CaseAlertEntity;
import aml.openwlf.data.entity.CaseCommentEntity;
import aml.openwlf.data.entity.CaseEntity;
import aml.openwlf.data.entity.CaseEntity.*;
import aml.openwlf.data.service.CaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Thymeleaf page controller for Case management workflow
 */
@Slf4j
@Controller
@RequestMapping("/cases")
@RequiredArgsConstructor
public class CasePageController {

    private final CaseService caseService;

    /**
     * 케이스 목록 (결정 대기 중인 케이스 위주)
     */
    @GetMapping
    public String listCases(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size);
        Page<CaseEntity> cases;

        if (status != null && !status.isEmpty()) {
            CaseStatus caseStatus = CaseStatus.valueOf(status);
            cases = caseService.searchCases(caseStatus, null, null, null, null, pageable);
        } else if (priority != null && !priority.isEmpty()) {
            CasePriority casePriority = CasePriority.valueOf(priority);
            cases = caseService.searchCases(null, casePriority, null, null, null, pageable);
        } else {
            cases = caseService.getOpenCases(pageable);
        }

        model.addAttribute("cases", cases);
        model.addAttribute("statuses", CaseStatus.values());
        model.addAttribute("priorities", CasePriority.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPriority", priority);

        return "case/list";
    }

    /**
     * 케이스 상세 + 결정 폼
     */
    @GetMapping("/{id}")
    public String caseDetail(@PathVariable Long id, Model model) {
        CaseEntity caseEntity = caseService.getCaseById(id)
                .orElseThrow(() -> new IllegalArgumentException("Case not found: " + id));

        List<CaseAlertEntity> linkedAlerts = caseService.getLinkedAlerts(id);
        List<CaseCommentEntity> comments = caseService.getComments(id);
        List<CaseActivityEntity> activities = caseService.getActivities(id);

        model.addAttribute("caseEntity", caseEntity);
        model.addAttribute("linkedAlerts", linkedAlerts);
        model.addAttribute("comments", comments);
        model.addAttribute("activities", activities);
        model.addAttribute("decisions", CaseDecision.values());

        return "case/detail";
    }

    /**
     * 결정 제출 처리
     */
    @PostMapping("/{id}/decision")
    public String submitDecision(
            @PathVariable Long id,
            @RequestParam String decision,
            @RequestParam String rationale,
            @RequestParam String decidedBy,
            RedirectAttributes redirectAttributes) {

        if (rationale == null || rationale.trim().length() < 10) {
            redirectAttributes.addFlashAttribute("error", "결정 사유는 10자 이상 입력해야 합니다.");
            return "redirect:/cases/" + id;
        }

        if (decidedBy == null || decidedBy.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "결정자 정보는 필수입니다.");
            return "redirect:/cases/" + id;
        }

        try {
            CaseDecision caseDecision = CaseDecision.valueOf(decision);
            caseService.makeDecision(id, caseDecision, rationale.trim(), decidedBy.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Case not found: " + id));

            log.info("Decision submitted for case {}: {} by {}", id, decision, decidedBy);
            return "redirect:/cases/" + id + "/decision/result";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "유효하지 않은 결정입니다: " + e.getMessage());
            return "redirect:/cases/" + id;
        }
    }

    /**
     * 결정 완료 결과 페이지
     */
    @GetMapping("/{id}/decision/result")
    public String decisionResult(@PathVariable Long id, Model model) {
        CaseEntity caseEntity = caseService.getCaseById(id)
                .orElseThrow(() -> new IllegalArgumentException("Case not found: " + id));

        model.addAttribute("caseEntity", caseEntity);
        return "case/decision-result";
    }
}
