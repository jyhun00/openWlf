package aml.openwlf.api.audit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 감사 대상 메서드 표시 어노테이션
 *
 * 이 어노테이션이 붙은 메서드는 AuditLoggingAspect에 의해 자동으로 감사 로깅됩니다.
 *
 * <pre>
 * {@code
 * @Auditable(action = "STATUS_CHANGED", entityType = "ALERT", entityId = "#alertId")
 * public AlertEntity updateStatus(Long alertId, AlertStatus status) {
 *     // ...
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Auditable {

    /**
     * 수행되는 액션 유형
     * 예: CREATED, STATUS_CHANGED, ASSIGNED, RESOLVED 등
     */
    String action();

    /**
     * 대상 엔티티 유형
     * 예: ALERT, CASE, RULE, WATCHLIST 등
     */
    String entityType() default "";

    /**
     * 엔티티 ID를 추출하기 위한 SpEL 표현식
     * 예: "#alertId", "#request.id", "#result.id"
     */
    String entityId() default "";

    /**
     * 민감 데이터 포함 여부
     * true인 경우 요청/응답 데이터가 마스킹됩니다.
     */
    boolean sensitiveData() default false;

    /**
     * 변경 사유를 추출하기 위한 SpEL 표현식
     * 예: "#reason", "#request.comment"
     */
    String reason() default "";

    /**
     * 이전 값을 추출하기 위한 SpEL 표현식
     * 상태 변경 등에서 사용
     */
    String oldValue() default "";

    /**
     * 새 값을 추출하기 위한 SpEL 표현식
     */
    String newValue() default "";

    /**
     * 로깅 레벨 (기본: INFO)
     */
    LogLevel logLevel() default LogLevel.INFO;

    /**
     * 비동기 로깅 여부 (기본: true)
     * false로 설정하면 동기적으로 로깅됩니다.
     */
    boolean async() default true;

    enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
}
