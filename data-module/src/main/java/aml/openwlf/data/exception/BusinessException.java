package aml.openwlf.data.exception;

/**
 * 비즈니스 로직 예외의 기본 클래스
 *
 * OOP 원칙: 예외 계층구조를 통한 다형성 활용
 * - 모든 비즈니스 예외는 이 클래스를 상속
 * - 예외 유형별로 적절한 HTTP 상태 코드 매핑 가능
 */
public abstract class BusinessException extends RuntimeException {

    private final String errorCode;

    protected BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
