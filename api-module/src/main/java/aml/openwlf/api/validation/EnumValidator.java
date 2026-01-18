package aml.openwlf.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ValidEnum 어노테이션의 검증 로직 구현
 *
 * OOP 원칙: 단일 책임 원칙 (SRP)
 * - Enum 값 검증 로직만 담당
 */
public class EnumValidator implements ConstraintValidator<ValidEnum, String> {

    private Set<String> allowedValues;
    private boolean ignoreCase;
    private boolean nullable;
    private String allowedValuesString;

    @Override
    public void initialize(ValidEnum annotation) {
        Class<? extends Enum<?>> enumClass = annotation.enumClass();
        this.ignoreCase = annotation.ignoreCase();
        this.nullable = annotation.nullable();

        // Enum 상수들에서 허용되는 값 추출
        Enum<?>[] enumConstants = enumClass.getEnumConstants();
        this.allowedValues = Arrays.stream(enumConstants)
                .map(e -> ignoreCase ? e.name().toUpperCase() : e.name())
                .collect(Collectors.toSet());

        this.allowedValuesString = Arrays.stream(enumConstants)
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return nullable;
        }

        String checkValue = ignoreCase ? value.toUpperCase() : value;
        boolean isValid = allowedValues.contains(checkValue);

        if (!isValid) {
            // 기본 메시지 비활성화하고 커스텀 메시지 설정
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("유효하지 않은 값입니다: '%s'. 허용되는 값: [%s]",
                            value, allowedValuesString))
                    .addConstraintViolation();
        }

        return isValid;
    }
}
