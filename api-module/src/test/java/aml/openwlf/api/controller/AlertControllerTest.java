package aml.openwlf.api.controller;

import aml.openwlf.api.dto.AlertAssignRequest;
import aml.openwlf.api.dto.AlertUpdateRequest;
import aml.openwlf.data.entity.AlertEntity;
import aml.openwlf.data.entity.AlertEntity.AlertStatus;
import aml.openwlf.data.repository.AlertRepository;
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
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("AlertController 통합 테스트")
class AlertControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private AlertRepository alertRepository;
    
    private AlertEntity testAlert;
    
    @BeforeEach
    void setUp() {
        // 테스트용 Alert 생성
        testAlert = AlertEntity.builder()
                .alertReference("ALT-TEST-" + System.currentTimeMillis())
                .status(AlertStatus.NEW)
                .customerId("CUST-001")
                .customerName("John Smith")
                .dateOfBirth(LocalDate.of(1985, 5, 15))
                .nationality("US")
                .score(85.0)
                .explanation("Test alert for integration testing")
                .matchedRules("[]")
                .build();
        testAlert = alertRepository.save(testAlert);
    }
    
    @Nested
    @DisplayName("GET /api/alerts 테스트")
    class GetAllAlertsTest {
        
        @Test
        @DisplayName("모든 Alert 조회 성공")
        void shouldGetAllAlerts() throws Exception {
            mockMvc.perform(get("/api/alerts")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").exists());
        }
        
        @Test
        @DisplayName("상태 필터링으로 Alert 조회")
        void shouldGetAlertsByStatus() throws Exception {
            mockMvc.perform(get("/api/alerts")
                            .param("status", "NEW"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
        
        @Test
        @DisplayName("customerId로 Alert 조회")
        void shouldGetAlertsByCustomerId() throws Exception {
            mockMvc.perform(get("/api/alerts")
                            .param("customerId", "CUST-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
        
        @Test
        @DisplayName("최소 점수로 Alert 필터링")
        void shouldGetAlertsByMinScore() throws Exception {
            mockMvc.perform(get("/api/alerts")
                            .param("minScore", "70.0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }
    
    @Nested
    @DisplayName("GET /api/alerts/open 테스트")
    class GetOpenAlertsTest {
        
        @Test
        @DisplayName("Open Alert 조회 성공")
        void shouldGetOpenAlerts() throws Exception {
            mockMvc.perform(get("/api/alerts/open"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }
    
    @Nested
    @DisplayName("GET /api/alerts/{id} 테스트")
    class GetAlertByIdTest {
        
        @Test
        @DisplayName("ID로 Alert 조회 성공")
        void shouldGetAlertById() throws Exception {
            mockMvc.perform(get("/api/alerts/" + testAlert.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testAlert.getId()))
                    .andExpect(jsonPath("$.alertReference").value(testAlert.getAlertReference()))
                    .andExpect(jsonPath("$.status").value("NEW"))
                    .andExpect(jsonPath("$.customerName").value("John Smith"));
        }
        
        @Test
        @DisplayName("존재하지 않는 Alert 조회 시 404 반환")
        void shouldReturn404WhenAlertNotFound() throws Exception {
            mockMvc.perform(get("/api/alerts/999999"))
                    .andExpect(status().isNotFound());
        }
    }
    
    @Nested
    @DisplayName("GET /api/alerts/reference/{reference} 테스트")
    class GetAlertByReferenceTest {
        
        @Test
        @DisplayName("Reference로 Alert 조회 성공")
        void shouldGetAlertByReference() throws Exception {
            mockMvc.perform(get("/api/alerts/reference/" + testAlert.getAlertReference()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.alertReference").value(testAlert.getAlertReference()));
        }
        
        @Test
        @DisplayName("존재하지 않는 Reference로 조회 시 404 반환")
        void shouldReturn404WhenReferenceNotFound() throws Exception {
            mockMvc.perform(get("/api/alerts/reference/INVALID-REF"))
                    .andExpect(status().isNotFound());
        }
    }
    
    @Nested
    @DisplayName("PUT /api/alerts/{id}/status 테스트")
    class UpdateStatusTest {
        
        @Test
        @DisplayName("Alert 상태를 IN_REVIEW로 업데이트")
        void shouldUpdateAlertStatusToInReview() throws Exception {
            AlertUpdateRequest request = AlertUpdateRequest.builder()
                    .status("IN_REVIEW")
                    .updatedBy("analyst@company.com")
                    .build();
            
            mockMvc.perform(put("/api/alerts/" + testAlert.getId() + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("IN_REVIEW"));
        }
        
        @Test
        @DisplayName("Alert를 CONFIRMED로 해결")
        void shouldResolveAlertAsConfirmed() throws Exception {
            AlertUpdateRequest request = AlertUpdateRequest.builder()
                    .status("CONFIRMED")
                    .comment("Verified against sanctions list")
                    .updatedBy("analyst@company.com")
                    .build();
            
            mockMvc.perform(put("/api/alerts/" + testAlert.getId() + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"))
                    .andExpect(jsonPath("$.resolutionComment").value("Verified against sanctions list"));
        }
        
        @Test
        @DisplayName("Alert를 FALSE_POSITIVE로 해결")
        void shouldResolveAlertAsFalsePositive() throws Exception {
            AlertUpdateRequest request = AlertUpdateRequest.builder()
                    .status("FALSE_POSITIVE")
                    .comment("Name similarity but different person")
                    .updatedBy("analyst@company.com")
                    .build();
            
            mockMvc.perform(put("/api/alerts/" + testAlert.getId() + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("FALSE_POSITIVE"));
        }
        
        @Test
        @DisplayName("잘못된 상태값으로 업데이트 시 400 반환")
        void shouldReturn400ForInvalidStatus() throws Exception {
            AlertUpdateRequest request = AlertUpdateRequest.builder()
                    .status("INVALID_STATUS")
                    .updatedBy("analyst@company.com")
                    .build();
            
            mockMvc.perform(put("/api/alerts/" + testAlert.getId() + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
        
        @Test
        @DisplayName("존재하지 않는 Alert 상태 업데이트 시 404 반환")
        void shouldReturn404WhenUpdatingNonExistentAlert() throws Exception {
            AlertUpdateRequest request = AlertUpdateRequest.builder()
                    .status("IN_REVIEW")
                    .updatedBy("analyst@company.com")
                    .build();
            
            mockMvc.perform(put("/api/alerts/999999/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }
    
    @Nested
    @DisplayName("PUT /api/alerts/{id}/assign 테스트")
    class AssignAlertTest {
        
        @Test
        @DisplayName("Alert 담당자 할당 성공")
        void shouldAssignAlert() throws Exception {
            AlertAssignRequest request = AlertAssignRequest.builder()
                    .assignedTo("analyst@company.com")
                    .build();
            
            mockMvc.perform(put("/api/alerts/" + testAlert.getId() + "/assign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assignedTo").value("analyst@company.com"))
                    .andExpect(jsonPath("$.status").value("IN_REVIEW")); // NEW -> IN_REVIEW 자동 변경
        }
        
        @Test
        @DisplayName("존재하지 않는 Alert 할당 시 404 반환")
        void shouldReturn404WhenAssigningNonExistentAlert() throws Exception {
            AlertAssignRequest request = AlertAssignRequest.builder()
                    .assignedTo("analyst@company.com")
                    .build();
            
            mockMvc.perform(put("/api/alerts/999999/assign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }
    
    @Nested
    @DisplayName("GET /api/alerts/stats 테스트")
    class GetStatisticsTest {
        
        @Test
        @DisplayName("Alert 통계 조회 성공")
        void shouldGetAlertStatistics() throws Exception {
            mockMvc.perform(get("/api/alerts/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalAlerts").exists())
                    .andExpect(jsonPath("$.newAlerts").exists())
                    .andExpect(jsonPath("$.openAlerts").exists());
        }
    }
}
