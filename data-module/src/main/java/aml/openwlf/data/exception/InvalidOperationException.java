package aml.openwlf.data.exception;

/**
 * 현재 상태에서 허용되지 않는 작업을 시도할 때 발생하는 예외
 *
 * HTTP 400 Bad Request에 매핑됨
 */
public class InvalidOperationException extends BusinessException {

    private static final String ERROR_CODE = "INVALID_OPERATION";

    public InvalidOperationException(String message) {
        super(ERROR_CODE, message);
    }

    public InvalidOperationException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }

    // 팩토리 메서드들
    public static InvalidOperationException alertAlreadyLinked(Long alertId, Long existingCaseId) {
        return new InvalidOperationException(
                String.format("Alert (ID: %d) is already linked to Case (ID: %d)",
                        alertId, existingCaseId));
    }

    public static InvalidOperationException noAlertsProvided() {
        return new InvalidOperationException("At least one alert is required");
    }

    public static InvalidOperationException linkNotExists(Long alertId, Long caseId) {
        return new InvalidOperationException(
                String.format("Alert (ID: %d) is not linked to Case (ID: %d)", alertId, caseId));
    }
}
