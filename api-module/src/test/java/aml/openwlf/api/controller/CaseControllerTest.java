package aml.openwlf.api.controller;

import aml.openwlf.api.dto.cases.*;
import aml.openwlf.data.entity.AlertEntity;
import aml.openwlf.data.entity.AlertEntity.AlertStatus;
import aml.openwlf.data.entity.CaseEntity;
import aml.openwlf.data.entity.CaseEntity.*;
import aml.openwlf.data.repository.AlertRepository;
import aml.openwlf.data.repository.CaseRepository;
import aml.openwlf.data.service.CaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("CaseController 통합 테스트")
class CaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private CaseService caseService;

    private AlertEntity testAlert;
    private CaseEntity testCase;

    @BeforeEach
    void setUp() {
        testAlert = AlertEntity.builder()
                .alertReference("ALT-TEST-" + System.currentTimeMillis())
                .status(AlertStatus.NEW)
                .customerId("CUST-001")
                .customerName("John Smith")
                .dateOfBirth(LocalDate.of(1985, 5, 15))
                .nationality("US")
                .score(85.0)
                .explanation("Test alert")
                .matchedRules("[]")
                .build();
        testAlert = alertRepository.save(testAlert);

        // Create a case from the alert
        CaseService.CreateCaseRequest request = CaseService.CreateCaseRequest.builder()
                .title("Test Case")
                .description("Test case for integration testing")
                .caseType(CaseType.SANCTIONS)
                .assignedTo("analyst01")
                .assignedTeam("compliance")
                .createdBy("system")
                .build();
        testCase = caseService.createCaseFromAlert(testAlert.getId(), request);
    }

    @Nested
    @DisplayName("GET /api/cases 테스트")
    class GetAllCasesTest {

        @Test
        @DisplayName("모든 케이스 조회 성공")
        void shouldGetAllCases() throws Exception {
            mockMvc.perform(get("/api/cases")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").exists());
        }

        @Test
        @DisplayName("상태 필터로 케이스 조회")
        void shouldFilterByStatus() throws Exception {
            mockMvc.perform(get("/api/cases")
                            .param("status", "OPEN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("우선순위 필터로 케이스 조회")
        void shouldFilterByPriority() throws Exception {
            mockMvc.perform(get("/api/cases")
                            .param("priority", "HIGH"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/cases/open 테스트")
    class GetOpenCasesTest {

        @Test
        @DisplayName("열린 케이스 조회 성공")
        void shouldGetOpenCases() throws Exception {
            mockMvc.perform(get("/api/cases/open"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
        }
    }

    @Nested
    @DisplayName("GET /api/cases/{id} 테스트")
    class GetCaseByIdTest {

        @Test
        @DisplayName("ID로 케이스 조회 성공")
        void shouldGetCaseById() throws Exception {
            mockMvc.perform(get("/api/cases/" + testCase.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testCase.getId()))
                    .andExpect(jsonPath("$.caseReference").value(testCase.getCaseReference()))
                    .andExpect(jsonPath("$.status").value("OPEN"))
                    .andExpect(jsonPath("$.customerName").value("John Smith"));
        }

        @Test
        @DisplayName("존재하지 않는 케이스 조회 시 404")
        void shouldReturn404WhenNotFound() throws Exception {
            mockMvc.perform(get("/api/cases/999999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/cases/{id}/status 테스트")
    class UpdateStatusTest {

        @Test
        @DisplayName("상태 업데이트 성공")
        void shouldUpdateStatus() throws Exception {
            CaseStatusUpdateRequest request = CaseStatusUpdateRequest.builder()
                    .status("IN_PROGRESS")
                    .updatedBy("analyst01")
                    .build();

            mockMvc.perform(put("/api/cases/" + testCase.getId() + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }

        @Test
        @DisplayName("존재하지 않는 케이스 상태 업데이트 시 404")
        void shouldReturn404WhenUpdatingNonExistent() throws Exception {
            CaseStatusUpdateRequest request = CaseStatusUpdateRequest.builder()
                    .status("IN_PROGRESS")
                    .updatedBy("analyst01")
                    .build();

            mockMvc.perform(put("/api/cases/999999/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/cases/{id}/assign 테스트")
    class AssignCaseTest {

        @Test
        @DisplayName("담당자 배정 성공")
        void shouldAssignCase() throws Exception {
            CaseAssignRequest request = CaseAssignRequest.builder()
                    .assignedTo("analyst02")
                    .assignedBy("manager01")
                    .build();

            mockMvc.perform(put("/api/cases/" + testCase.getId() + "/assign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assignedTo").value("analyst02"));
        }
    }

    @Nested
    @DisplayName("POST /api/cases/{id}/decision 테스트")
    class MakeDecisionTest {

        @Test
        @DisplayName("결정 제출 성공")
        void shouldMakeDecision() throws Exception {
            CaseDecisionRequest request = CaseDecisionRequest.builder()
                    .decision("TRUE_POSITIVE")
                    .rationale("Confirmed sanctions match with high confidence")
                    .decidedBy("senior_analyst")
                    .build();

            mockMvc.perform(post("/api/cases/" + testCase.getId() + "/decision")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.decision").value("TRUE_POSITIVE"))
                    .andExpect(jsonPath("$.status").value("CLOSED"));
        }

        @Test
        @DisplayName("사유 없이 결정 시 400")
        void shouldReturn400WhenRationaleMissing() throws Exception {
            CaseDecisionRequest request = CaseDecisionRequest.builder()
                    .decision("TRUE_POSITIVE")
                    .decidedBy("analyst01")
                    .build();

            mockMvc.perform(post("/api/cases/" + testCase.getId() + "/decision")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("사유 10자 미만 시 400")
        void shouldReturn400WhenRationaleTooShort() throws Exception {
            CaseDecisionRequest request = CaseDecisionRequest.builder()
                    .decision("TRUE_POSITIVE")
                    .rationale("Short")
                    .decidedBy("analyst01")
                    .build();

            mockMvc.perform(post("/api/cases/" + testCase.getId() + "/decision")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("존재하지 않는 케이스 결정 시 404")
        void shouldReturn404WhenCaseNotFound() throws Exception {
            CaseDecisionRequest request = CaseDecisionRequest.builder()
                    .decision("TRUE_POSITIVE")
                    .rationale("Confirmed sanctions match with high confidence")
                    .decidedBy("analyst01")
                    .build();

            mockMvc.perform(post("/api/cases/999999/decision")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/cases/{id}/comments 테스트")
    class CommentTest {

        @Test
        @DisplayName("코멘트 추가 성공")
        void shouldAddComment() throws Exception {
            AddCommentRequest request = AddCommentRequest.builder()
                    .content("Investigation notes for this case")
                    .commentType("ANALYSIS")
                    .createdBy("analyst01")
                    .build();

            mockMvc.perform(post("/api/cases/" + testCase.getId() + "/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.content").value("Investigation notes for this case"))
                    .andExpect(jsonPath("$.commentType").value("ANALYSIS"));
        }

        @Test
        @DisplayName("코멘트 목록 조회 성공")
        void shouldGetComments() throws Exception {
            mockMvc.perform(get("/api/cases/" + testCase.getId() + "/comments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/cases/{id}/activities 테스트")
    class ActivitiesTest {

        @Test
        @DisplayName("활동 로그 조회 성공")
        void shouldGetActivities() throws Exception {
            mockMvc.perform(get("/api/cases/" + testCase.getId() + "/activities"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
        }
    }

    @Nested
    @DisplayName("GET /api/cases/stats 테스트")
    class StatisticsTest {

        @Test
        @DisplayName("통계 조회 성공")
        void shouldGetStatistics() throws Exception {
            mockMvc.perform(get("/api/cases/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCases").exists())
                    .andExpect(jsonPath("$.openCases").exists());
        }
    }
}
