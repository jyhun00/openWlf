package aml.openwlf.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for watchlist entry responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Watchlist entry information")
public class WatchlistEntryDto {
    
    @Schema(description = "Unique identifier", example = "1")
    private Long id;
    
    @Schema(description = "Name of the listed individual or entity", example = "John Doe")
    private String name;
    
    @Schema(description = "Alternative names or aliases")
    private List<String> aliases;
    
    @Schema(description = "Date of birth", example = "1980-01-15")
    private LocalDate dateOfBirth;
    
    @Schema(description = "Nationality code", example = "US")
    private String nationality;
    
    @Schema(description = "Source of the watchlist", example = "OFAC")
    private String listSource;
    
    @Schema(description = "Type of entry", example = "INDIVIDUAL")
    private String entryType;
    
    @Schema(description = "Whether the entry is active", example = "true")
    private Boolean isActive;
    
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;
    
    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
