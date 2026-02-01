package aml.openwlf.api.audit.context;

import lombok.Builder;
import lombok.Data;

/**
 * 감사 컨텍스트 데이터 클래스
 *
 * 현재 요청의 감사 관련 정보를 담는 불변 데이터 객체입니다.
 */
@Data
@Builder
public class AuditContext {

    /**
     * 요청 추적용 UUID
     */
    private final String requestId;

    /**
     * 인증된 사용자 ID
     */
    private final String userId;

    /**
     * 사용자 역할
     */
    private final String userRole;

    /**
     * 클라이언트 IP 주소
     */
    private final String clientIp;

    /**
     * User-Agent
     */
    private final String userAgent;

    /**
     * 세션 ID
     */
    private final String sessionId;

    /**
     * 분산 추적용 Correlation ID
     */
    private final String correlationId;

    /**
     * 요청 시작 시간 (나노초)
     */
    private final long requestStartTime;

    /**
     * HTTP 메서드
     */
    private final String httpMethod;

    /**
     * 요청 URI
     */
    private final String requestUri;
}
