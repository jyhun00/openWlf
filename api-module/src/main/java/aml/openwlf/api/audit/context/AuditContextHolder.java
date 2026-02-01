package aml.openwlf.api.audit.context;

import java.util.Optional;

/**
 * ThreadLocal 기반 감사 컨텍스트 홀더
 *
 * 현재 스레드의 감사 컨텍스트를 관리합니다.
 * 요청 시작 시 설정하고 요청 종료 시 반드시 clear해야 합니다.
 */
public final class AuditContextHolder {

    private static final ThreadLocal<AuditContext> CONTEXT_HOLDER = new ThreadLocal<>();

    private AuditContextHolder() {
        // Utility class
    }

    /**
     * 현재 스레드의 감사 컨텍스트 설정
     */
    public static void setContext(AuditContext context) {
        CONTEXT_HOLDER.set(context);
    }

    /**
     * 현재 스레드의 감사 컨텍스트 조회
     */
    public static Optional<AuditContext> getContext() {
        return Optional.ofNullable(CONTEXT_HOLDER.get());
    }

    /**
     * 현재 스레드의 감사 컨텍스트 삭제
     * 요청 종료 시 반드시 호출해야 메모리 누수를 방지할 수 있습니다.
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }

    /**
     * 현재 사용자 ID 조회
     */
    public static String getCurrentUserId() {
        return getContext()
                .map(AuditContext::getUserId)
                .orElse("SYSTEM");
    }

    /**
     * 현재 사용자 역할 조회
     */
    public static String getCurrentUserRole() {
        return getContext()
                .map(AuditContext::getUserRole)
                .orElse(null);
    }

    /**
     * 현재 클라이언트 IP 조회
     */
    public static String getCurrentClientIp() {
        return getContext()
                .map(AuditContext::getClientIp)
                .orElse(null);
    }

    /**
     * 현재 요청 ID 조회
     */
    public static String getCurrentRequestId() {
        return getContext()
                .map(AuditContext::getRequestId)
                .orElse(null);
    }
}
