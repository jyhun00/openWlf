package aml.openwlf.data.exception;

/**
 * 엔티티를 찾을 수 없을 때 발생하는 예외
 *
 * HTTP 404 Not Found에 매핑됨
 */
public class EntityNotFoundException extends BusinessException {

    private static final String ERROR_CODE = "ENTITY_NOT_FOUND";

    private final String entityType;
    private final Object entityId;

    public EntityNotFoundException(String entityType, Object entityId) {
        super(ERROR_CODE, String.format("%s not found with id: %s", entityType, entityId));
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public Object getEntityId() {
        return entityId;
    }

    // 팩토리 메서드 - 가독성 향상
    public static EntityNotFoundException alert(Long id) {
        return new EntityNotFoundException("Alert", id);
    }

    public static EntityNotFoundException caseEntity(Long id) {
        return new EntityNotFoundException("Case", id);
    }

    public static EntityNotFoundException watchlistEntry(Long id) {
        return new EntityNotFoundException("WatchlistEntry", id);
    }
}
