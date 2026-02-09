package aml.openwlf.api.init;

import aml.openwlf.data.entity.UserEntity;
import aml.openwlf.data.entity.UserRole;
import aml.openwlf.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("dev")
@Order(2)
@RequiredArgsConstructor
public class SecuritySampleDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Users already exist, skipping security sample data initialization.");
            return;
        }

        log.info("Initializing security sample data...");

        createUser("admin", "admin123", "admin@openwlf.com", "시스템 관리자", UserRole.ROLE_ADMIN);
        createUser("manager1", "manager123", "manager1@openwlf.com", "김매니저", UserRole.ROLE_MANAGER);
        createUser("analyst1", "analyst123", "analyst1@openwlf.com", "이분석가", UserRole.ROLE_ANALYST);
        createUser("analyst2", "analyst123", "analyst2@openwlf.com", "박분석가", UserRole.ROLE_ANALYST);
        createUser("viewer1", "viewer123", "viewer1@openwlf.com", "최열람자", UserRole.ROLE_VIEWER);

        log.info("Security sample data initialization completed. Created {} users.", userRepository.count());
    }

    private void createUser(String username, String password, String email, String fullName, UserRole role) {
        UserEntity user = UserEntity.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .fullName(fullName)
                .role(role)
                .isEnabled(true)
                .isAccountLocked(false)
                .failedLoginAttempts(0)
                .build();
        userRepository.save(user);
        log.info("Created user: {} ({})", username, role);
    }
}
