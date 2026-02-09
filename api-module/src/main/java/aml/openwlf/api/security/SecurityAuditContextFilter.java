package aml.openwlf.api.security;

import aml.openwlf.api.audit.context.AuditContext;
import aml.openwlf.api.audit.context.AuditContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class SecurityAuditContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            String userId = "ANONYMOUS";
            String userRole = null;

            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                userId = authentication.getName();
                if (authentication.getAuthorities() != null && !authentication.getAuthorities().isEmpty()) {
                    userRole = authentication.getAuthorities().iterator().next().getAuthority();
                }
            }

            String clientIp = getClientIp(request);

            AuditContext context = AuditContext.builder()
                    .requestId(UUID.randomUUID().toString())
                    .userId(userId)
                    .userRole(userRole)
                    .clientIp(clientIp)
                    .userAgent(request.getHeader("User-Agent"))
                    .sessionId(request.getSession(false) != null ? request.getSession(false).getId() : null)
                    .requestStartTime(System.nanoTime())
                    .httpMethod(request.getMethod())
                    .requestUri(request.getRequestURI())
                    .build();

            AuditContextHolder.setContext(context);

            filterChain.doFilter(request, response);
        } finally {
            AuditContextHolder.clear();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
