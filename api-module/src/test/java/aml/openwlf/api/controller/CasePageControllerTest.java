package aml.openwlf.api.controller;

import aml.openwlf.data.entity.AlertEntity;
import aml.openwlf.data.entity.AlertEntity.AlertStatus;
import aml.openwlf.data.entity.CaseEntity;
import aml.openwlf.data.entity.CaseEntity.*;
import aml.openwlf.data.repository.AlertRepository;
import aml.openwlf.data.service.CaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("CasePageController 통합 테스트")
class CasePageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private CaseService caseService;

    private CaseEntity testCase;

    @BeforeEach
    void setUp() {
        AlertEntity alert = AlertEntity.builder()
                .alertReference("ALT-PAGE-" + System.currentTimeMillis())
                .status(AlertStatus.NEW)
                .customerId("CUST-001")
                .customerName("Test Customer")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .nationality("KR")
                .score(75.0)
                .explanation("Test alert for page testing")
                .matchedRules("[]")
                .build();
        alert = alertRepository.save(alert);

        CaseService.CreateCaseRequest request = CaseService.CreateCaseRequest.builder()
                .title("Page Test Case")
                .description("Test case for page controller testing")
                .caseType(CaseType.SANCTIONS)
                .assignedTo("analyst01")
                .assignedTeam("compliance")
                .createdBy("system")
                .build();
        testCase = caseService.createCaseFromAlert(alert.getId(), request);
    }

    @Nested
    @DisplayName("GET /cases 케이스 목록 페이지")
    class ListPageTest {

        @Test
        @DisplayName("목록 페이지 정상 렌더링")
        void shouldRenderListPage() throws Exception {
            mockMvc.perform(get("/cases"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("case/list"))
                    .andExpect(model().attributeExists("cases", "statuses", "priorities"))
                    .andExpect(content().string(containsString("케이스 목록")));
        }

        @Test
        @DisplayName("목록에 케이스 데이터 표시")
        void shouldShowCaseData() throws Exception {
            mockMvc.perform(get("/cases"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(testCase.getCaseReference())))
                    .andExpect(content().string(containsString("Test Customer")));
        }

        @Test
        @DisplayName("상태 필터 적용")
        void shouldFilterByStatus() throws Exception {
            mockMvc.perform(get("/cases").param("status", "OPEN"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("case/list"))
                    .andExpect(model().attribute("selectedStatus", "OPEN"));
        }

        @Test
        @DisplayName("우선순위 필터 적용")
        void shouldFilterByPriority() throws Exception {
            mockMvc.perform(get("/cases").param("priority", "HIGH"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("case/list"))
                    .andExpect(model().attribute("selectedPriority", "HIGH"));
        }

        @Test
        @DisplayName("빈 결과 처리")
        void shouldHandleEmptyResult() throws Exception {
            mockMvc.perform(get("/cases").param("status", "CLOSED"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("해당 조건에 맞는 케이스가 없습니다")));
        }
    }

    @Nested
    @DisplayName("GET /cases/{id} 케이스 상세 페이지")
    class DetailPageTest {

        @Test
        @DisplayName("상세 페이지 정상 렌더링")
        void shouldRenderDetailPage() throws Exception {
            mockMvc.perform(get("/cases/" + testCase.getId()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("case/detail"))
                    .andExpect(model().attributeExists("caseEntity", "linkedAlerts", "comments", "activities", "decisions"))
                    .andExpect(content().string(containsString("케이스 상세")));
        }

        @Test
        @DisplayName("상세 페이지에 케이스 정보 표시")
        void shouldShowCaseInfo() throws Exception {
            mockMvc.perform(get("/cases/" + testCase.getId()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(testCase.getCaseReference())))
                    .andExpect(content().string(containsString("Test Customer")))
                    .andExpect(content().string(containsString("SANCTIONS")));
        }

        @Test
        @DisplayName("결정 폼 표시 (미결정 케이스)")
        void shouldShowDecisionForm() throws Exception {
            mockMvc.perform(get("/cases/" + testCase.getId()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("TRUE_POSITIVE")))
                    .andExpect(content().string(containsString("FALSE_POSITIVE")))
                    .andExpect(content().string(containsString("결정 제출")));
        }

        @Test
        @DisplayName("존재하지 않는 케이스 조회 시 에러")
        void shouldReturn400WhenCaseNotFound() throws Exception {
            mockMvc.perform(get("/cases/999999"))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("POST /cases/{id}/decision 결정 제출")
    class DecisionSubmitTest {

        @Test
        @DisplayName("결정 제출 성공 시 redirect")
        void shouldRedirectOnSuccess() throws Exception {
            mockMvc.perform(post("/cases/" + testCase.getId() + "/decision")
                            .param("decision", "TRUE_POSITIVE")
                            .param("rationale", "Confirmed sanctions match with high confidence")
                            .param("decidedBy", "analyst01"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/cases/" + testCase.getId() + "/decision/result"));
        }

        @Test
        @DisplayName("사유 10자 미만 시 redirect back with error")
        void shouldRedirectBackWhenRationaleTooShort() throws Exception {
            mockMvc.perform(post("/cases/" + testCase.getId() + "/decision")
                            .param("decision", "TRUE_POSITIVE")
                            .param("rationale", "Short")
                            .param("decidedBy", "analyst01"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/cases/" + testCase.getId()))
                    .andExpect(flash().attributeExists("error"));
        }

        @Test
        @DisplayName("결정자 미입력 시 redirect back with error")
        void shouldRedirectBackWhenDecidedByEmpty() throws Exception {
            mockMvc.perform(post("/cases/" + testCase.getId() + "/decision")
                            .param("decision", "TRUE_POSITIVE")
                            .param("rationale", "Confirmed match with high confidence")
                            .param("decidedBy", ""))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/cases/" + testCase.getId()))
                    .andExpect(flash().attributeExists("error"));
        }
    }

    @Nested
    @DisplayName("GET /cases/{id}/decision/result 결정 결과 페이지")
    class DecisionResultPageTest {

        @Test
        @DisplayName("결정 결과 페이지 정상 렌더링")
        void shouldRenderResultPage() throws Exception {
            // First make a decision
            caseService.makeDecision(testCase.getId(), CaseDecision.TRUE_POSITIVE,
                    "Confirmed match", "analyst01");

            mockMvc.perform(get("/cases/" + testCase.getId() + "/decision/result"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("case/decision-result"))
                    .andExpect(model().attributeExists("caseEntity"))
                    .andExpect(content().string(containsString("결정 완료")))
                    .andExpect(content().string(containsString("TRUE_POSITIVE")));
        }

        @Test
        @DisplayName("결정 결과에 케이스 정보 표시")
        void shouldShowCaseInfoInResult() throws Exception {
            caseService.makeDecision(testCase.getId(), CaseDecision.FALSE_POSITIVE,
                    "False positive - different person", "analyst01");

            mockMvc.perform(get("/cases/" + testCase.getId() + "/decision/result"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(testCase.getCaseReference())))
                    .andExpect(content().string(containsString("Test Customer")))
                    .andExpect(content().string(containsString("FALSE_POSITIVE")));
        }
    }
}
