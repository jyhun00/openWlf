package aml.openwlf.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for alert responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Alert information")
public class AlertDto {
    
    @Schema(description = "Unique identifier", example = "1")
    private Long id;
    
    @Schema(description = "Alert reference number", example = "ALT-20251228-A1B2C3D4")
    private String alertReference;
    
    @Schema(description = "Alert status", example = "NEW")
    private String status;
    
    @Schema(description = "Customer ID", example = "CUST-001")
    private String customerId;
    
    @Schema(description = "Customer name", example = "John Doe")
    private String customerName;
    
    @Schema(description = "Date of birth", example = "1980-01-15")
    private LocalDate dateOfBirth;
    
    @Schema(description = "Nationality", example = "US")
    private String nationality;
    
    @Schema(description = "Risk score", example = "75.5")
    private Double score;
    
    @Schema(description = "Explanation of the alert")
    private String explanation;
    
    @Schema(description = "Assigned analyst", example = "analyst@company.com")
    private String assignedTo;
    
    @Schema(description = "Resolution comment")
    private String resolutionComment;
    
    @Schema(description = "Resolution timestamp")
    private LocalDateTime resolvedAt;
    
    @Schema(description = "Resolved by", example = "supervisor@company.com")
    private String resolvedBy;
    
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;
    
    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
