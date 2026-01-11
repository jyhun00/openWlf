package aml.openwlf.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for watchlist search
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Search criteria for watchlist entries")
public class WatchlistSearchRequest {
    
    @Schema(description = "Search term for name or alias", example = "John")
    private String searchTerm;
    
    @Schema(description = "Filter by list source", example = "OFAC")
    private String listSource;
    
    @Schema(description = "Filter by active status", example = "true")
    private Boolean isActive;
}
