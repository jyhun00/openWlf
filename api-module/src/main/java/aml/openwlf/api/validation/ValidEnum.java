package aml.openwlf.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Enum 값 검증을 위한 커스텀 어노테이션
 *
 * OOP 원칙: 개방-폐쇄 원칙 (OCP)
 * - 새로운 Enum 타입에도 재사용 가능
 * - 검증 로직 변경 없이 확장 가능
 *
 * 사용 예:
 * <pre>
 * @ValidEnum(enumClass = CaseStatus.class)
 * private String status;
 * </pre>
 */
@Documented
@Constraint(validatedBy = EnumValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEnum {

    /**
     * 검증할 Enum 클래스
     */
    Class<? extends Enum<?>> enumClass();

    /**
     * 에러 메시지
     */
    String message() default "유효하지 않은 값입니다. 허용되는 값: {allowedValues}";

    /**
     * 대소문자 무시 여부
     */
    boolean ignoreCase() default true;

    /**
     * null 허용 여부
     */
    boolean nullable() default true;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
