package aml.openwlf.data.entity.audit;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Map;

/**
 * API 접근 감사 로그 엔티티
 *
 * 모든 API 호출을 추적하여 누가, 언제, 무엇을 요청했는지 기록합니다.
 */
@Entity
@Table(name = "audit_api_access_log",
        indexes = {
                @Index(name = "idx_audit_api_user_id", columnList = "user_id"),
                @Index(name = "idx_audit_api_endpoint", columnList = "endpoint"),
                @Index(name = "idx_audit_api_created_at", columnList = "created_at"),
                @Index(name = "idx_audit_api_request_id", columnList = "request_id"),
                @Index(name = "idx_audit_api_status", columnList = "response_status")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AuditApiAccessLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== 요청 정보 =====

    /**
     * 요청 추적용 UUID
     */
    @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;

    /**
     * HTTP 메서드 (GET, POST, PUT, DELETE)
     */
    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    /**
     * API 엔드포인트 (/api/alerts/123)
     */
    @Column(name = "endpoint", nullable = false, length = 500)
    private String endpoint;

    /**
     * 쿼리 파라미터 (?status=NEW&page=0)
     */
    @Column(name = "query_params", columnDefinition = "TEXT")
    private String queryParams;

    /**
     * 요청 본문 (JSON, 민감정보 마스킹)
     */
    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    // ===== 응답 정보 =====

    /**
     * HTTP 상태 코드
     */
    @Column(name = "response_status", nullable = false)
    private Integer responseStatus;

    /**
     * 응답 시간 (ms)
     */
    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    /**
     * 에러 메시지 (있는 경우)
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // ===== 사용자 정보 =====

    /**
     * 인증된 사용자 ID
     */
    @Column(name = "user_id", length = 100)
    private String userId;

    /**
     * 사용자 역할
     */
    @Column(name = "user_role", length = 50)
    private String userRole;

    /**
     * 클라이언트 IP (IPv4/IPv6)
     */
    @Column(name = "client_ip", length = 45)
    private String clientIp;

    /**
     * 클라이언트 정보 (User-Agent)
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    // ===== 컨텍스트 =====

    /**
     * 세션 ID
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;

    /**
     * 분산 추적용 Correlation ID
     */
    @Column(name = "correlation_id", length = 36)
    private String correlationId;

    // ===== 타임스탬프 =====

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ===== 메타데이터 =====

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata;
}
