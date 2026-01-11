package aml.openwlf.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for customer filtering
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer information for watchlist filtering")
public class CustomerFilterRequest {
    
    @NotBlank(message = "Customer name is required")
    @Schema(description = "Customer full name", example = "John Smith")
    private String name;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Date of birth", example = "1980-01-15")
    private LocalDate dateOfBirth;
    
    @Schema(description = "Nationality code (ISO 3166-1 alpha-2)", example = "US")
    private String nationality;
    
    @Schema(description = "Customer ID (optional)", example = "CUST-12345")
    private String customerId;
}
