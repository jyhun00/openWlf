package aml.openwlf.api.controller;

import aml.openwlf.api.dto.auth.LoginRequest;
import aml.openwlf.api.dto.auth.LoginResponse;
import aml.openwlf.api.dto.auth.TokenRefreshRequest;
import aml.openwlf.api.security.CustomUserDetails;
import aml.openwlf.api.security.jwt.JwtTokenProvider;
import aml.openwlf.data.entity.UserEntity;
import aml.openwlf.data.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "인증 API")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "사용자명과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails.getUsername());

            // Update last login time
            userRepository.findByUsername(userDetails.getUsername()).ifPresent(user -> {
                user.setLastLoginAt(LocalDateTime.now());
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
            });

            log.info("User logged in successfully: {}", userDetails.getUsername());

            return ResponseEntity.ok(LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000)
                    .tokenType("Bearer")
                    .role(userDetails.getRoleName())
                    .username(userDetails.getUsername())
                    .fullName(userDetails.getFullName())
                    .build());

        } catch (BadCredentialsException e) {
            // Increment failed login attempts
            userRepository.findByUsername(request.getUsername()).ifPresent(user -> {
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                if (user.getFailedLoginAttempts() >= 5) {
                    user.setAccountLocked(true);
                }
                userRepository.save(user);
            });

            log.warn("Login failed for user: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "AUTHENTICATION_FAILED", "message", "사용자명 또는 비밀번호가 올바르지 않습니다."));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "리프레시 토큰으로 새 액세스 토큰을 발급받습니다.")
    public ResponseEntity<?> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "INVALID_TOKEN", "message", "유효하지 않은 리프레시 토큰입니다."));
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        String newAccessToken = jwtTokenProvider.generateAccessToken(username);

        return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
                "expiresIn", jwtTokenProvider.getAccessTokenExpirationMs() / 1000,
                "tokenType", "Bearer"
        ));
    }

    @GetMapping("/me")
    @Operation(summary = "현재 사용자 정보", description = "현재 인증된 사용자의 정보를 반환합니다.")
    public ResponseEntity<?> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "NOT_AUTHENTICATED", "message", "인증되지 않은 사용자입니다."));
        }

        UserEntity user = userDetails.getUser();
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail() != null ? user.getEmail() : "",
                "fullName", user.getFullName() != null ? user.getFullName() : "",
                "role", user.getRole().name()
        ));
    }
}
