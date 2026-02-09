package aml.openwlf.api.security;

import aml.openwlf.api.security.jwt.JwtProperties;
import aml.openwlf.api.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtTokenProvider 단위 테스트")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-key-for-jwt-token-provider-test-must-be-at-least-256-bits-long");
        properties.setAccessTokenExpirationMs(3600000);
        properties.setRefreshTokenExpirationMs(86400000);
        properties.setIssuer("test-issuer");

        jwtTokenProvider = new JwtTokenProvider(properties);
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("액세스 토큰 생성 및 검증")
    void shouldGenerateAndValidateAccessToken() {
        String token = jwtTokenProvider.generateAccessToken("testuser");

        assertThat(token).isNotNull().isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo("testuser");
    }

    @Test
    @DisplayName("리프레시 토큰 생성 및 검증")
    void shouldGenerateAndValidateRefreshToken() {
        String token = jwtTokenProvider.generateRefreshToken("testuser");

        assertThat(token).isNotNull().isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo("testuser");
    }

    @Test
    @DisplayName("잘못된 토큰 검증 실패")
    void shouldFailValidationForInvalidToken() {
        assertThat(jwtTokenProvider.validateToken("invalid.token.here")).isFalse();
    }

    @Test
    @DisplayName("빈 토큰 검증 실패")
    void shouldFailValidationForEmptyToken() {
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰 검증 실패")
    void shouldFailValidationForExpiredToken() {
        JwtProperties expiredProperties = new JwtProperties();
        expiredProperties.setSecret("test-secret-key-for-jwt-token-provider-test-must-be-at-least-256-bits-long");
        expiredProperties.setAccessTokenExpirationMs(0); // Immediate expiration
        expiredProperties.setRefreshTokenExpirationMs(0);
        expiredProperties.setIssuer("test-issuer");

        JwtTokenProvider expiredProvider = new JwtTokenProvider(expiredProperties);
        expiredProvider.init();

        String token = expiredProvider.generateAccessToken("testuser");
        assertThat(expiredProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰 검증 실패")
    void shouldFailValidationForTokenSignedWithDifferentKey() {
        JwtProperties otherProperties = new JwtProperties();
        otherProperties.setSecret("different-secret-key-for-jwt-token-test-must-be-at-least-256-bits-long-here");
        otherProperties.setAccessTokenExpirationMs(3600000);
        otherProperties.setRefreshTokenExpirationMs(86400000);
        otherProperties.setIssuer("test-issuer");

        JwtTokenProvider otherProvider = new JwtTokenProvider(otherProperties);
        otherProvider.init();

        String token = otherProvider.generateAccessToken("testuser");
        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }
}
