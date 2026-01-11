package aml.openwlf.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer information")
public class CustomerInfoDto {
    
    @Schema(description = "Customer name", example = "John Smith")
    private String name;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Date of birth", example = "1980-01-15")
    private LocalDate dateOfBirth;
    
    @Schema(description = "Nationality", example = "US")
    private String nationality;
    
    @Schema(description = "Customer ID", example = "CUST-12345")
    private String customerId;
}
