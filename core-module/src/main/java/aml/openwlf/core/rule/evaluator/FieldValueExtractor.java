package aml.openwlf.core.rule.evaluator;

import aml.openwlf.core.model.CustomerInfo;
import aml.openwlf.core.rule.WatchlistEntry;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 필드 값 추출 유틸리티
 *
 * RuleEvaluator 구현체들에서 공통으로 사용하는 필드 값 추출 로직을
 * 중앙화하여 DRY 원칙을 준수합니다.
 */
@Component
public class FieldValueExtractor {

    /**
     * CustomerInfo에서 필드 값 추출
     *
     * @param customer 고객 정보
     * @param field 필드 이름
     * @return 필드 값 (없으면 null)
     */
    public String getCustomerFieldValue(CustomerInfo customer, String field) {
        if (customer == null || field == null) {
            return null;
        }

        return switch (field.toLowerCase()) {
            case "name" -> customer.getName();
            case "nationality" -> customer.getNationality();
            case "dateofbirth", "dob" -> customer.getDateOfBirth() != null
                    ? customer.getDateOfBirth().toString() : null;
            case "customerid" -> customer.getCustomerId();
            default -> null;
        };
    }

    /**
     * WatchlistEntry에서 필드 값 목록 추출
     *
     * @param entry 감시 목록 항목
     * @param field 필드 이름
     * @return 필드 값 목록 (없으면 빈 리스트)
     */
    public List<String> getWatchlistFieldValues(WatchlistEntry entry, String field) {
        if (entry == null || field == null) {
            return List.of();
        }

        return switch (field.toLowerCase()) {
            case "name" -> List.of(entry.getName() != null ? entry.getName() : "");
            case "aliases" -> entry.getAliases() != null ? entry.getAliases() : List.of();
            case "nationality" -> List.of(entry.getNationality() != null ? entry.getNationality() : "");
            case "dateofbirth", "dob" -> List.of(entry.getDateOfBirth() != null
                    ? entry.getDateOfBirth().toString() : "");
            default -> List.of();
        };
    }

    /**
     * 필드가 이름 관련 필드인지 확인
     *
     * @param field 필드 이름
     * @return 이름 관련 필드 여부
     */
    public boolean isNameField(String field) {
        if (field == null) return false;
        String lowerField = field.toLowerCase();
        return lowerField.equals("name") || lowerField.equals("aliases");
    }

    /**
     * 필드가 날짜 관련 필드인지 확인
     *
     * @param field 필드 이름
     * @return 날짜 관련 필드 여부
     */
    public boolean isDateField(String field) {
        if (field == null) return false;
        String lowerField = field.toLowerCase();
        return lowerField.equals("dateofbirth") || lowerField.equals("dob");
    }
}
