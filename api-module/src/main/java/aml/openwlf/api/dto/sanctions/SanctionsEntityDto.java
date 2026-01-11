package aml.openwlf.api.dto.sanctions;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Sanctions entity detail")
public class SanctionsEntityDto {
    
    @Schema(description = "Entity ID", example = "1")
    private Long entityId;
    
    @Schema(description = "Source UID", example = "UN-6908330")
    private String sourceUid;
    
    @Schema(description = "Source file", example = "UN")
    private String sourceFile;
    
    @Schema(description = "Entity type", example = "Individual")
    private String entityType;
    
    @Schema(description = "Primary name", example = "KIM JONG UN")
    private String primaryName;
    
    @Schema(description = "Gender", example = "Male")
    private String gender;
    
    @Schema(description = "Birth date", example = "1984-01-08")
    private LocalDate birthDate;
    
    @Schema(description = "Nationality", example = "KP")
    private String nationality;
    
    @Schema(description = "Vessel flag", example = "KP")
    private String vesselFlag;
    
    @Schema(description = "Sanction list type", example = "UN Security Council Consolidated List")
    private String sanctionListType;
    
    @Schema(description = "Names/aliases")
    private List<NameInfo> names;
    
    @Schema(description = "Addresses")
    private List<Address> addresses;
    
    @Schema(description = "Documents")
    private List<Document> documents;
    
    @Schema(description = "Additional features")
    private Map<String, Object> additionalFeatures;
    
    @Schema(description = "Is active", example = "true")
    private Boolean isActive;
    
    @Schema(description = "Last updated", example = "2025-12-28T15:30:00")
    private LocalDateTime lastUpdatedAt;
    
    @Schema(description = "Created at", example = "2025-01-01T00:00:00")
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Name/Alias info")
    public static class NameInfo {
        
        @Schema(description = "Name ID", example = "1")
        private Long nameId;
        
        @Schema(description = "Name type", example = "Primary")
        private String nameType;
        
        @Schema(description = "Full name", example = "KIM JONG UN")
        private String fullName;
        
        @Schema(description = "Script", example = "Latin")
        private String script;
        
        @Schema(description = "Quality score (0-100)", example = "100")
        private Integer qualityScore;
        
        @Schema(description = "Is primary name", example = "true")
        private Boolean isPrimary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Address info")
    public static class Address {
        
        @Schema(description = "Address ID", example = "1")
        private Long addressId;
        
        @Schema(description = "Address type", example = "Registered")
        private String addressType;
        
        @Schema(description = "Full address", example = "Pyongyang, DPRK")
        private String fullAddress;
        
        @Schema(description = "City", example = "Pyongyang")
        private String city;
        
        @Schema(description = "State/Province")
        private String stateProvince;
        
        @Schema(description = "Country", example = "DPRK")
        private String country;
        
        @Schema(description = "Country code", example = "KP")
        private String countryCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Document/ID info")
    public static class Document {
        
        @Schema(description = "Document ID", example = "1")
        private Long documentId;
        
        @Schema(description = "Document type", example = "Passport")
        private String documentType;
        
        @Schema(description = "Document number", example = "PS123456789")
        private String documentNumber;
        
        @Schema(description = "Issuing country", example = "DPRK")
        private String issuingCountry;
        
        @Schema(description = "Issuing country code", example = "KP")
        private String issuingCountryCode;
        
        @Schema(description = "Issue date", example = "2020-01-01")
        private LocalDate issueDate;
        
        @Schema(description = "Expiry date", example = "2030-01-01")
        private LocalDate expiryDate;
        
        @Schema(description = "Issuing authority")
        private String issuingAuthority;
    }
}
