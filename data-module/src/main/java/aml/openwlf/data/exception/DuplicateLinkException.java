package aml.openwlf.data.exception;

/**
 * 이미 연결된 엔티티에 대해 중복 연결을 시도할 때 발생하는 예외
 *
 * HTTP 409 Conflict에 매핑됨
 */
public class DuplicateLinkException extends BusinessException {

    private static final String ERROR_CODE = "DUPLICATE_LINK";

    private final String sourceType;
    private final Object sourceId;
    private final String targetType;
    private final Object targetId;

    public DuplicateLinkException(String sourceType, Object sourceId,
                                   String targetType, Object targetId) {
        super(ERROR_CODE, String.format("%s (ID: %s) is already linked to %s (ID: %s)",
                sourceType, sourceId, targetType, targetId));
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.targetType = targetType;
        this.targetId = targetId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public Object getSourceId() {
        return sourceId;
    }

    public String getTargetType() {
        return targetType;
    }

    public Object getTargetId() {
        return targetId;
    }

    // 팩토리 메서드
    public static DuplicateLinkException alertToCase(Long alertId, Long caseId) {
        return new DuplicateLinkException("Alert", alertId, "Case", caseId);
    }
}
