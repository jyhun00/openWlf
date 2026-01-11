package aml.openwlf.batch.parser.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * XML에서 파싱된 제재 엔티티 데이터를 담는 DTO
 * Entity로 변환하기 전 중간 단계로 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedSanctionsData {

    /**
     * 원본 고유 ID (OFAC: UID, UN: DATAID)
     */
    private String sourceUid;

    /**
     * 데이터 출처 (OFAC, UN)
     */
    private String sourceFile;

    /**
     * 엔티티 유형 (Individual, Entity, Vessel)
     */
    private String entityType;

    /**
     * 대표 이름
     */
    private String primaryName;

    /**
     * 성별
     */
    private String gender;

    /**
     * 생년월일
     */
    private LocalDate birthDate;

    /**
     * 국적
     */
    private String nationality;

    /**
     * 선박 국적 (Vessel인 경우)
     */
    private String vesselFlag;

    /**
     * 제재 리스트 유형
     */
    private String sanctionListType;

    /**
     * 이름/별칭 목록
     */
    @Builder.Default
    private List<ParsedName> names = new ArrayList<>();

    /**
     * 주소 목록
     */
    @Builder.Default
    private List<ParsedAddress> addresses = new ArrayList<>();

    /**
     * 문서/신분증 목록
     */
    @Builder.Default
    private List<ParsedDocument> documents = new ArrayList<>();

    /**
     * 추가 속성 (프로그램, 지정 사유 등)
     */
    @Builder.Default
    private Map<String, Object> additionalFeatures = new HashMap<>();

    /**
     * 파싱된 이름 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedName {
        private String nameType;  // Primary, AKA, FKA, Low Quality AKA
        private String fullName;
        private String script;    // Latin, Arabic, Cyrillic 등
        private Integer qualityScore;
        private String firstName;
        private String middleName;
        private String lastName;
    }

    /**
     * 파싱된 주소 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedAddress {
        private String addressType;
        private String fullAddress;
        private String street;
        private String city;
        private String stateProvince;
        private String postalCode;
        private String country;
        private String countryCode;
        private String note;
    }

    /**
     * 파싱된 문서 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedDocument {
        private String documentType;
        private String documentNumber;
        private String issuingCountry;
        private String issuingCountryCode;
        private LocalDate issueDate;
        private LocalDate expiryDate;
        private String issuingAuthority;
        private String note;
    }

    /**
     * raw 데이터 비교를 위한 해시 생성
     */
    public String generateContentHash() {
        StringBuilder sb = new StringBuilder();
        sb.append(sourceUid).append("|");
        sb.append(sourceFile).append("|");
        sb.append(entityType).append("|");
        sb.append(primaryName).append("|");
        sb.append(gender).append("|");
        sb.append(birthDate).append("|");
        sb.append(nationality).append("|");
        sb.append(vesselFlag).append("|");
        sb.append(sanctionListType).append("|");
        
        // 이름 정렬 후 해시에 포함
        names.stream()
            .sorted((a, b) -> {
                String aKey = a.getNameType() + ":" + a.getFullName();
                String bKey = b.getNameType() + ":" + b.getFullName();
                return aKey.compareTo(bKey);
            })
            .forEach(n -> sb.append(n.getNameType()).append(":").append(n.getFullName()).append("|"));
        
        // 주소 정렬 후 해시에 포함
        addresses.stream()
            .sorted((a, b) -> {
                String aKey = a.getFullAddress() != null ? a.getFullAddress() : "";
                String bKey = b.getFullAddress() != null ? b.getFullAddress() : "";
                return aKey.compareTo(bKey);
            })
            .forEach(a -> sb.append(a.getFullAddress()).append("|"));
        
        // 문서 정렬 후 해시에 포함
        documents.stream()
            .sorted((a, b) -> {
                String aKey = a.getDocumentType() + ":" + a.getDocumentNumber();
                String bKey = b.getDocumentType() + ":" + b.getDocumentNumber();
                return aKey.compareTo(bKey);
            })
            .forEach(d -> sb.append(d.getDocumentType()).append(":").append(d.getDocumentNumber()).append("|"));

        return Integer.toHexString(sb.toString().hashCode());
    }
}
