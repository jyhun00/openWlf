package aml.openwlf.api.security;

import aml.openwlf.data.entity.UserEntity;
import aml.openwlf.data.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CustomUserDetails 단위 테스트")
class CustomUserDetailsTest {

    @Test
    @DisplayName("활성화된 계정의 상태 확인")
    void shouldReturnCorrectStatusForActiveAccount() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("activeuser")
                .password("password")
                .fullName("Active User")
                .email("active@test.com")
                .role(UserRole.ROLE_ADMIN)
                .isEnabled(true)
                .isAccountLocked(false)
                .build();

        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
        assertThat(details.getUsername()).isEqualTo("activeuser");
        assertThat(details.getPassword()).isEqualTo("password");
    }

    @Test
    @DisplayName("잠긴 계정의 상태 확인")
    void shouldReturnLockedStatusForLockedAccount() {
        UserEntity user = UserEntity.builder()
                .id(2L)
                .username("lockeduser")
                .password("password")
                .role(UserRole.ROLE_VIEWER)
                .isEnabled(true)
                .isAccountLocked(true)
                .build();

        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.isAccountNonLocked()).isFalse();
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("비활성화된 계정의 상태 확인")
    void shouldReturnDisabledStatusForDisabledAccount() {
        UserEntity user = UserEntity.builder()
                .id(3L)
                .username("disableduser")
                .password("password")
                .role(UserRole.ROLE_ANALYST)
                .isEnabled(false)
                .isAccountLocked(false)
                .build();

        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.isEnabled()).isFalse();
        assertThat(details.isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("사용자 ID, 이름, 역할 조회")
    void shouldReturnUserIdFullNameAndRole() {
        UserEntity user = UserEntity.builder()
                .id(10L)
                .username("testuser")
                .password("password")
                .fullName("Test Full Name")
                .role(UserRole.ROLE_MANAGER)
                .isEnabled(true)
                .isAccountLocked(false)
                .build();

        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.getUserId()).isEqualTo(10L);
        assertThat(details.getFullName()).isEqualTo("Test Full Name");
        assertThat(details.getRoleName()).isEqualTo("ROLE_MANAGER");
    }

    @Test
    @DisplayName("각 역할별 권한 확인")
    void shouldReturnCorrectAuthorityForEachRole() {
        for (UserRole role : UserRole.values()) {
            UserEntity user = UserEntity.builder()
                    .id(1L)
                    .username("user")
                    .password("password")
                    .role(role)
                    .isEnabled(true)
                    .isAccountLocked(false)
                    .build();

            CustomUserDetails details = new CustomUserDetails(user);

            assertThat(details.getAuthorities())
                    .hasSize(1)
                    .extracting("authority")
                    .containsExactly(role.name());
        }
    }
}
