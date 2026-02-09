package aml.openwlf.api.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("SecurityConfig 접근 제어 테스트")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("인증되지 않은 사용자")
    class UnauthenticatedUserTest {

        @Test
        @DisplayName("API 요청 시 401 반환")
        void shouldReturn401ForApiRequest() throws Exception {
            mockMvc.perform(get("/api/alerts"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("로그인 페이지 접근 가능")
        void shouldAllowLoginPage() throws Exception {
            mockMvc.perform(get("/login"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("인증 API 접근 가능")
        void shouldAllowAuthApi() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"invalid\",\"password\":\"invalid\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("웹 페이지 접근 시 로그인으로 리다이렉트")
        void shouldRedirectToLoginForWebPages() throws Exception {
            mockMvc.perform(get("/cases"))
                    .andExpect(status().is3xxRedirection());
        }
    }

    @Nested
    @DisplayName("ADMIN 역할")
    class AdminRoleTest {

        @Test
        @WithMockUser(username = "admin", authorities = "ROLE_ADMIN")
        @DisplayName("룰 관리 API 접근 가능")
        void shouldAccessRuleApi() throws Exception {
            mockMvc.perform(get("/api/rules"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "admin", authorities = "ROLE_ADMIN")
        @DisplayName("Alert API 접근 가능")
        void shouldAccessAlertApi() throws Exception {
            mockMvc.perform(get("/api/alerts")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("VIEWER 역할")
    class ViewerRoleTest {

        @Test
        @WithMockUser(username = "viewer1", authorities = "ROLE_VIEWER")
        @DisplayName("GET API 접근 가능")
        void shouldAccessGetApi() throws Exception {
            mockMvc.perform(get("/api/alerts")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "viewer1", authorities = "ROLE_VIEWER")
        @DisplayName("룰 관리 API 접근 불가")
        void shouldNotAccessRuleApi() throws Exception {
            mockMvc.perform(get("/api/rules"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "viewer1", authorities = "ROLE_VIEWER")
        @DisplayName("Alert PUT API 접근 불가")
        void shouldNotAccessAlertPutApi() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .put("/api/alerts/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"IN_REVIEW\",\"updatedBy\":\"viewer1\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "viewer1", authorities = "ROLE_VIEWER")
        @DisplayName("회원가입 페이지 접근 불가 (403)")
        void shouldNotAccessMemberPage() throws Exception {
            mockMvc.perform(get("/member/register"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("ANALYST 역할")
    class AnalystRoleTest {

        @Test
        @WithMockUser(username = "analyst1", authorities = "ROLE_ANALYST")
        @DisplayName("Alert PUT API 접근 가능")
        void shouldAccessAlertPutApi() throws Exception {
            // Just checking authorization, not actual data
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .put("/api/alerts/999/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"IN_REVIEW\",\"updatedBy\":\"analyst1\"}"))
                    .andExpect(status().isNotFound()); // 404 means authorization passed
        }

        @Test
        @WithMockUser(username = "analyst1", authorities = "ROLE_ANALYST")
        @DisplayName("룰 관리 API 접근 불가")
        void shouldNotAccessRuleApi() throws Exception {
            mockMvc.perform(get("/api/rules"))
                    .andExpect(status().isForbidden());
        }
    }
}
