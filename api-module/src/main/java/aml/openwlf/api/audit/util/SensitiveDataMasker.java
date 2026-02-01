package aml.openwlf.api.audit.util;

import java.util.regex.Pattern;

/**
 * PII 마스킹 유틸리티
 *
 * 민감한 개인정보를 마스킹하여 로그에 안전하게 기록할 수 있도록 합니다.
 */
public final class SensitiveDataMasker {

    private SensitiveDataMasker() {
        // Utility class
    }

    // 이메일 패턴: user@domain.com -> u***@domain.com
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");

    // 전화번호 패턴 (다양한 형식 지원)
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(\\+?\\d{1,3}[-. ]?)?(\\(?\\d{2,4}\\)?[-. ]?)?\\d{3,4}[-. ]?\\d{3,4}");

    // 주민등록번호 패턴: 123456-1234567 -> 123456-*******
    private static final Pattern SSN_PATTERN =
            Pattern.compile("(\\d{6})-?(\\d{7})");

    // 신용카드 패턴: 1234-5678-9012-3456 -> 1234-****-****-3456
    private static final Pattern CREDIT_CARD_PATTERN =
            Pattern.compile("(\\d{4})[-. ]?(\\d{4})[-. ]?(\\d{4})[-. ]?(\\d{4})");

    // 여권번호 패턴 (한국): M12345678 -> M***45678
    private static final Pattern PASSPORT_PATTERN =
            Pattern.compile("([A-Z])?(\\d{8,9})");

    /**
     * 문자열 내의 모든 민감 정보를 마스킹
     */
    public static String maskAll(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input;
        result = maskEmails(result);
        result = maskPhones(result);
        result = maskSSN(result);
        result = maskCreditCards(result);
        return result;
    }

    /**
     * 이메일 주소 마스킹
     * user@domain.com -> u***@domain.com
     */
    public static String maskEmails(String input) {
        if (input == null) return null;
        return EMAIL_PATTERN.matcher(input).replaceAll(mr -> {
            String localPart = mr.group(1);
            String domain = mr.group(2);
            if (localPart.length() <= 1) {
                return "*@" + domain;
            }
            return localPart.charAt(0) + "***@" + domain;
        });
    }

    /**
     * 전화번호 마스킹
     * 010-1234-5678 -> 010-****-5678
     */
    public static String maskPhones(String input) {
        if (input == null) return null;
        return PHONE_PATTERN.matcher(input).replaceAll(mr -> {
            String match = mr.group();
            if (match.length() < 4) return match;
            int len = match.length();
            return match.substring(0, len / 3) +
                   "*".repeat(len / 3) +
                   match.substring(len - len / 3);
        });
    }

    /**
     * 주민등록번호 마스킹
     * 123456-1234567 -> 123456-*******
     */
    public static String maskSSN(String input) {
        if (input == null) return null;
        return SSN_PATTERN.matcher(input).replaceAll("$1-*******");
    }

    /**
     * 신용카드 번호 마스킹
     * 1234-5678-9012-3456 -> 1234-****-****-3456
     */
    public static String maskCreditCards(String input) {
        if (input == null) return null;
        return CREDIT_CARD_PATTERN.matcher(input).replaceAll("$1-****-****-$4");
    }

    /**
     * 이름 마스킹
     * 홍길동 -> 홍*동, John Smith -> J*** Smith
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        // 공백으로 분리 (서양식 이름)
        String[] parts = name.split("\\s+");
        if (parts.length > 1) {
            // 서양식: John Smith -> J*** Smith
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) sb.append(" ");
                if (i == 0) {
                    sb.append(parts[i].charAt(0)).append("***");
                } else {
                    sb.append(parts[i]);
                }
            }
            return sb.toString();
        }

        // 동양식: 홍길동 -> 홍*동
        if (name.length() <= 1) {
            return name;
        }
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }
        return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
    }

    /**
     * 생년월일 마스킹
     * 1990-01-15 -> 1990-**-**
     */
    public static String maskDateOfBirth(String dob) {
        if (dob == null || dob.isEmpty()) {
            return dob;
        }
        // YYYY-MM-DD or YYYYMMDD
        if (dob.length() >= 10 && dob.contains("-")) {
            return dob.substring(0, 5) + "**-**";
        }
        if (dob.length() == 8) {
            return dob.substring(0, 4) + "****";
        }
        return dob;
    }

    /**
     * 국적 코드는 마스킹하지 않음 (민감 정보 아님)
     */
    public static String maskNationality(String nationality) {
        return nationality;
    }

    /**
     * JSON 내의 특정 필드 값 마스킹
     */
    public static String maskJsonField(String json, String fieldName) {
        if (json == null || fieldName == null) {
            return json;
        }
        // "fieldName":"value" -> "fieldName":"***"
        Pattern pattern = Pattern.compile(
                "(\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*)\"[^\"]*\"",
                Pattern.CASE_INSENSITIVE
        );
        return pattern.matcher(json).replaceAll("$1\"***\"");
    }

    /**
     * 여러 JSON 필드 값 마스킹
     */
    public static String maskJsonFields(String json, String... fieldNames) {
        String result = json;
        for (String fieldName : fieldNames) {
            result = maskJsonField(result, fieldName);
        }
        return result;
    }
}
