package aml.openwlf.data.entity.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Map;

/**
 * 모든 Audit 로그 엔티티의 공통 기반 클래스
 *
 * 감사 로그에 필요한 공통 필드들을 정의합니다:
 * - 수행자 정보 (ID, 역할, IP)
 * - 생성 시간
 * - 추가 메타데이터
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 작업을 수행한 사용자 ID
     * SYSTEM인 경우 시스템 자동 작업
     */
    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    /**
     * 수행자의 역할
     */
    @Column(name = "performed_by_role", length = 50)
    private String performedByRole;

    /**
     * 클라이언트 IP 주소 (IPv4/IPv6)
     */
    @Column(name = "client_ip", length = 45)
    private String clientIp;

    /**
     * 로그 생성 시간
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * 추가 메타데이터 (JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}
