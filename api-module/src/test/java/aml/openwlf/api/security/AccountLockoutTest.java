package aml.openwlf.api.security;

import aml.openwlf.api.dto.auth.LoginRequest;
import aml.openwlf.data.entity.UserEntity;
import aml.openwlf.data.entity.UserRole;
import aml.openwlf.data.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("계정 잠금 통합 테스트")
class AccountLockoutTest {

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
        if (!userRepository.existsByUsername("locktest")) {
            UserEntity user = UserEntity.builder()
                    .username("locktest")
                    .password(passwordEncoder.encode("correct123"))
                    .email("locktest@test.com")
                    .fullName("Lock Test User")
                    .role(UserRole.ROLE_ANALYST)
                    .isEnabled(true)
                    .isAccountLocked(false)
                    .failedLoginAttempts(0)
                    .build();
            userRepository.save(user);
        }
    }

    @Test
    @DisplayName("잘못된 비밀번호 시 실패 횟수 증가")
    void shouldIncrementFailedAttemptsOnBadPassword() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("locktest")
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        UserEntity user = userRepository.findByUsername("locktest").orElseThrow();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("5회 실패 후 계정 잠금")
    void shouldLockAccountAfter5FailedAttempts() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("locktest")
                .password("wrongpassword")
                .build();

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        UserEntity user = userRepository.findByUsername("locktest").orElseThrow();
        assertThat(user.isAccountLocked()).isTrue();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
    }

    @Test
    @DisplayName("성공적 로그인 시 실패 횟수 초기화")
    void shouldResetFailedAttemptsOnSuccessfulLogin() throws Exception {
        // First, fail a few times
        LoginRequest badRequest = LoginRequest.builder()
                .username("locktest")
                .password("wrongpassword")
                .build();

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isUnauthorized());
        }

        UserEntity userBefore = userRepository.findByUsername("locktest").orElseThrow();
        assertThat(userBefore.getFailedLoginAttempts()).isEqualTo(3);

        // Now login successfully
        LoginRequest goodRequest = LoginRequest.builder()
                .username("locktest")
                .password("correct123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goodRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());

        UserEntity userAfter = userRepository.findByUsername("locktest").orElseThrow();
        assertThat(userAfter.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(userAfter.getLastLoginAt()).isNotNull();
    }
}
