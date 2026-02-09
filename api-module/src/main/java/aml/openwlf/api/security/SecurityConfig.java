package aml.openwlf.api.security;

import aml.openwlf.api.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * FilterChain 1: REST API (/api/**) - Stateless JWT
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/filter/health").permitAll()

                        // Rule management - ADMIN only
                        .requestMatchers("/api/rules/**").hasAuthority("ROLE_ADMIN")

                        // Case decision - ADMIN, MANAGER only
                        .requestMatchers(HttpMethod.POST, "/api/cases/*/decision").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")

                        // Write operations - ADMIN, MANAGER, ANALYST
                        .requestMatchers(HttpMethod.PUT, "/api/alerts/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_ANALYST")
                        .requestMatchers(HttpMethod.POST, "/api/cases/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_ANALYST")
                        .requestMatchers(HttpMethod.PUT, "/api/cases/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_ANALYST")
                        .requestMatchers(HttpMethod.DELETE, "/api/cases/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_ANALYST")
                        .requestMatchers(HttpMethod.POST, "/api/filter/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_ANALYST")
                        .requestMatchers(HttpMethod.POST, "/api/member/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_ANALYST")

                        // GET requests - all authenticated users
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()

                        // Default: authenticated
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * FilterChain 2: Web pages (/**) - Session-based Form Login
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/**")
                .authorizeHttpRequests(auth -> auth
                        // Public
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/error/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()

                        // H2 Console - ADMIN only
                        .requestMatchers("/h2-console/**").hasAuthority("ROLE_ADMIN")

                        // Member pages - not VIEWER
                        .requestMatchers("/member/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_ANALYST")

                        // Case pages - all authenticated
                        .requestMatchers("/cases/**").authenticated()

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/cases", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/error/403")
                )
                // H2 console needs frames
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
}
