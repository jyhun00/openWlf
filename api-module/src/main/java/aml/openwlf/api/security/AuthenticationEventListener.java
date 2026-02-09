package aml.openwlf.api.security;

import aml.openwlf.data.entity.audit.AuditAuthenticationLogEntity;
import aml.openwlf.data.entity.audit.enums.AuthEventType;
import aml.openwlf.data.repository.audit.AuditAuthenticationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationEventListener {

    private final AuditAuthenticationLogRepository authLogRepository;

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        String clientIp = getClientIp();
        String userAgent = getUserAgent();

        String role = null;
        if (event.getAuthentication().getAuthorities() != null && !event.getAuthentication().getAuthorities().isEmpty()) {
            role = event.getAuthentication().getAuthorities().iterator().next().getAuthority();
        }

        AuditAuthenticationLogEntity logEntity = AuditAuthenticationLogEntity.builder()
                .eventType(AuthEventType.LOGIN_SUCCESS)
                .userId(username)
                .userRole(role)
                .clientIp(clientIp != null ? clientIp : "unknown")
                .userAgent(userAgent)
                .isSuccess(true)
                .build();

        authLogRepository.save(logEntity);
        log.info("Authentication success: user={}, ip={}", username, clientIp);
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        String clientIp = getClientIp();
        String failureReason = event.getException().getMessage();

        AuditAuthenticationLogEntity logEntity = AuditAuthenticationLogEntity.builder()
                .eventType(AuthEventType.LOGIN_FAILURE)
                .userId(username)
                .clientIp(clientIp != null ? clientIp : "unknown")
                .isSuccess(false)
                .failureReason(failureReason)
                .build();

        authLogRepository.save(logEntity);
        log.warn("Authentication failure: user={}, ip={}, reason={}", username, clientIp, failureReason);
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String xForwardedFor = attrs.getRequest().getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return attrs.getRequest().getRemoteAddr();
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private String getUserAgent() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return attrs.getRequest().getHeader("User-Agent");
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
}
