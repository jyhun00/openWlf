package aml.openwlf.data.entity.audit;

import aml.openwlf.data.entity.audit.enums.AuthEventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Map;

/**
 * 인증 감사 로그 엔티티
 *
 * 로그인, 로그아웃, 인증 실패 등을 기록합니다.
 */
@Entity
@Table(name = "audit_authentication_log",
        indexes = {
                @Index(name = "idx_auth_log_user_id", columnList = "user_id"),
                @Index(name = "idx_auth_log_event_type", columnList = "event_type"),
                @Index(name = "idx_auth_log_created_at", columnList = "created_at"),
                @Index(name = "idx_auth_log_client_ip", columnList = "client_ip"),
                @Index(name = "idx_auth_log_success", columnList = "is_success")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AuditAuthenticationLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== 이벤트 정보 =====

    /**
     * 인증 이벤트 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private AuthEventType eventType;

    // ===== 사용자 정보 =====

    /**
     * 사용자 ID (실패 시 시도된 ID)
     */
    @Column(name = "user_id", length = 100)
    private String userId;

    /**
     * 사용자 이메일
     */
    @Column(name = "user_email", length = 255)
    private String userEmail;

    /**
     * 사용자 역할
     */
    @Column(name = "user_role", length = 50)
    private String userRole;

    // ===== 클라이언트 정보 =====

    /**
     * 클라이언트 IP
     */
    @Column(name = "client_ip", nullable = false, length = 45)
    private String clientIp;

    /**
     * User-Agent
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * 기기 식별자
     */
    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;

    /**
     * 지역 정보 (선택)
     */
    @Column(name = "geo_location", length = 100)
    private String geoLocation;

    // ===== 결과 정보 =====

    /**
     * 성공 여부
     */
    @Column(name = "is_success", nullable = false)
    private Boolean isSuccess;

    /**
     * 실패 사유
     */
    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    // ===== 세션 정보 =====

    /**
     * 세션 ID
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;

    /**
     * JWT ID (jti)
     */
    @Column(name = "token_id", length = 100)
    private String tokenId;

    // ===== 타임스탬프 =====

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ===== 메타데이터 =====

    /**
     * 추가 정보 (MFA 방식 등)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}
