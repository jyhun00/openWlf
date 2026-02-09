package aml.openwlf.api.security;

import aml.openwlf.api.dto.auth.LoginRequest;
import aml.openwlf.data.entity.UserEntity;
import aml.openwlf.data.entity.UserRole;
import aml.openwlf.data.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("AuthController 통합 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByUsername("testadmin")) {
            UserEntity user = UserEntity.builder()
                    .username("testadmin")
                    .password(passwordEncoder.encode("testpass123"))
                    .email("testadmin@test.com")
                    .fullName("Test Admin")
                    .role(UserRole.ROLE_ADMIN)
                    .isEnabled(true)
                    .isAccountLocked(false)
                    .failedLoginAttempts(0)
                    .build();
            userRepository.save(user);
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTest {

        @Test
        @DisplayName("정상 로그인 시 JWT 토큰 반환")
        void shouldReturnJwtTokenOnSuccessfulLogin() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .username("testadmin")
                    .password("testpass123")
                    .build();

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.role").value("ROLE_ADMIN"))
                    .andExpect(jsonPath("$.username").value("testadmin"))
                    .andExpect(jsonPath("$.fullName").value("Test Admin"));
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인 시 401 반환")
        void shouldReturn401OnBadCredentials() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .username("testadmin")
                    .password("wrongpassword")
                    .build();

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("AUTHENTICATION_FAILED"));
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 로그인 시 401 반환")
        void shouldReturn401ForNonExistentUser() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .username("nonexistent")
                    .password("password")
                    .build();

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class RefreshTest {

        @Test
        @DisplayName("유효한 리프레시 토큰으로 새 액세스 토큰 발급")
        void shouldRefreshToken() throws Exception {
            // First login to get refresh token
            LoginRequest loginRequest = LoginRequest.builder()
                    .username("testadmin")
                    .password("testpass123")
                    .build();

            String loginResponse = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            String refreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asText();

            // Use refresh token
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"));
        }

        @Test
        @DisplayName("잘못된 리프레시 토큰으로 401 반환")
        void shouldReturn401ForInvalidRefreshToken() throws Exception {
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"invalid.token.here\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/auth/me")
    class MeTest {

        @Test
        @DisplayName("JWT 토큰으로 사용자 정보 조회")
        void shouldReturnUserInfo() throws Exception {
            // Login first
            LoginRequest loginRequest = LoginRequest.builder()
                    .username("testadmin")
                    .password("testpass123")
                    .build();

            String loginResponse = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            String accessToken = objectMapper.readTree(loginResponse).get("accessToken").asText();

            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("testadmin"))
                    .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
        }

        @Test
        @DisplayName("토큰 없이 접근 시 401 반환")
        void shouldReturn401WithoutToken() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
