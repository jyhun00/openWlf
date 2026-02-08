package aml.openwlf.api.controller;

import aml.openwlf.api.dto.MemberFilterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("MemberFilteringController 통합 테스트")
class MemberFilteringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("POST /api/member/filter 필터링 API")
    class FilterMemberTest {

        @Test
        @DisplayName("정상 필터링 요청 성공")
        void shouldFilterMember() throws Exception {
            MemberFilterRequest request = MemberFilterRequest.builder()
                    .name("홍길동")
                    .dateOfBirth(LocalDate.of(1990, 1, 15))
                    .nationality("KR")
                    .build();

            mockMvc.perform(post("/api/member/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.score").isNumber())
                    .andExpect(jsonPath("$.registrationAllowed").isBoolean())
                    .andExpect(jsonPath("$.memberInfo.name").value("홍길동"));
        }

        @Test
        @DisplayName("Watchlist 매칭 시 높은 점수와 거부")
        void shouldRejectHighRiskMember() throws Exception {
            MemberFilterRequest request = MemberFilterRequest.builder()
                    .name("Kim Jong Un")
                    .dateOfBirth(LocalDate.of(1984, 1, 8))
                    .nationality("KP")
                    .build();

            mockMvc.perform(post("/api/member/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.score", greaterThanOrEqualTo(70.0)))
                    .andExpect(jsonPath("$.registrationAllowed").value(false))
                    .andExpect(jsonPath("$.alert").value(true))
                    .andExpect(jsonPath("$.rejectionReason").exists());
        }

        @Test
        @DisplayName("안전한 이름은 가입 허용")
        void shouldAllowSafeMember() throws Exception {
            MemberFilterRequest request = MemberFilterRequest.builder()
                    .name("이영희")
                    .nationality("KR")
                    .build();

            mockMvc.perform(post("/api/member/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.registrationAllowed").value(true));
        }

        @Test
        @DisplayName("이름 미입력 시 400")
        void shouldReturn400WhenNameMissing() throws Exception {
            MemberFilterRequest request = MemberFilterRequest.builder()
                    .nationality("KR")
                    .build();

            mockMvc.perform(post("/api/member/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("응답에 memberInfo 포함")
        void shouldIncludeMemberInfoInResponse() throws Exception {
            MemberFilterRequest request = MemberFilterRequest.builder()
                    .name("홍길동")
                    .dateOfBirth(LocalDate.of(1990, 1, 15))
                    .nationality("KR")
                    .address("서울시 강남구")
                    .phoneNumber("010-1234-5678")
                    .email("hong@example.com")
                    .build();

            mockMvc.perform(post("/api/member/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.memberInfo.name").value("홍길동"))
                    .andExpect(jsonPath("$.memberInfo.nationality").value("KR"))
                    .andExpect(jsonPath("$.memberInfo.address").value("서울시 강남구"))
                    .andExpect(jsonPath("$.memberInfo.email").value("hong@example.com"));
        }

        @Test
        @DisplayName("Alert 생성 시 alertReference 반환")
        void shouldReturnAlertReferenceWhenAlertCreated() throws Exception {
            MemberFilterRequest request = MemberFilterRequest.builder()
                    .name("Kim Jong Un")
                    .dateOfBirth(LocalDate.of(1984, 1, 8))
                    .nationality("KP")
                    .build();

            mockMvc.perform(post("/api/member/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.alertReference").exists())
                    .andExpect(jsonPath("$.alertReference", startsWith("ALT-")));
        }

        @Test
        @DisplayName("matchedRules 목록 반환")
        void shouldReturnMatchedRules() throws Exception {
            MemberFilterRequest request = MemberFilterRequest.builder()
                    .name("Kim Jong Un")
                    .dateOfBirth(LocalDate.of(1984, 1, 8))
                    .nationality("KP")
                    .build();

            mockMvc.perform(post("/api/member/filter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matchedRules").isArray())
                    .andExpect(jsonPath("$.matchedRules", hasSize(greaterThan(0))))
                    .andExpect(jsonPath("$.matchedRules[0].ruleName").exists())
                    .andExpect(jsonPath("$.matchedRules[0].score").isNumber());
        }
    }
}
