package aml.openwlf.api.controller;

import aml.openwlf.api.dto.WatchlistEntryDto;
import aml.openwlf.data.entity.WatchlistEntryEntity;
import aml.openwlf.data.service.WatchlistDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API Controller for watchlist management
 */
@Slf4j
@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
@Tag(name = "Watchlist Management", description = "APIs for managing and querying watchlist entries")
public class WatchlistController {
    
    private final WatchlistDataService watchlistDataService;
    
    @GetMapping
    @Operation(
            summary = "Get all watchlist entries",
            description = "Retrieves all watchlist entries with pagination and optional filtering"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved watchlist entries"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<Page<WatchlistEntryDto>> getAllEntries(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            
            @Parameter(description = "Sort field", example = "name")
            @RequestParam(defaultValue = "id") String sortBy,
            
            @Parameter(description = "Sort direction (asc/desc)", example = "asc")
            @RequestParam(defaultValue = "asc") String sortDir,
            
            @Parameter(description = "Search term for name or alias")
            @RequestParam(required = false) String search,
            
            @Parameter(description = "Filter by list source (e.g., OFAC, UN)")
            @RequestParam(required = false) String listSource,
            
            @Parameter(description = "Filter by active status")
            @RequestParam(required = false) Boolean isActive
    ) {
        log.info("Fetching watchlist entries - page: {}, size: {}, search: {}, source: {}, active: {}", 
                page, size, search, listSource, isActive);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<WatchlistEntryEntity> entityPage = watchlistDataService.searchEntries(
                search, listSource, isActive, pageable);
        
        Page<WatchlistEntryDto> dtoPage = entityPage.map(this::toDto);
        
        return ResponseEntity.ok(dtoPage);
    }
    
    @GetMapping("/{id}")
    @Operation(
            summary = "Get watchlist entry by ID",
            description = "Retrieves a specific watchlist entry by its ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved watchlist entry",
                    content = @Content(schema = @Schema(implementation = WatchlistEntryDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Watchlist entry not found"
            )
    })
    public ResponseEntity<WatchlistEntryDto> getEntryById(
            @Parameter(description = "Watchlist entry ID", required = true)
            @PathVariable Long id
    ) {
        log.info("Fetching watchlist entry with id: {}", id);
        
        return watchlistDataService.getEntryById(id)
                .map(entity -> ResponseEntity.ok(toDto(entity)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/stats")
    @Operation(
            summary = "Get watchlist statistics",
            description = "Retrieves statistics about watchlist entries including cache info"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved statistics"
    )
    public ResponseEntity<WatchlistDataService.WatchlistStats> getStatistics() {
        log.info("Fetching watchlist statistics");
        return ResponseEntity.ok(watchlistDataService.getStatistics());
    }
    
    @GetMapping("/cache/stats")
    @Operation(
            summary = "Get cache statistics",
            description = "Retrieves information about the in-memory watchlist cache"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved cache statistics"
    )
    public ResponseEntity<WatchlistDataService.CacheStats> getCacheStats() {
        log.info("Fetching cache statistics");
        return ResponseEntity.ok(watchlistDataService.getCacheStats());
    }
    
    @PostMapping("/cache/refresh")
    @Operation(
            summary = "Refresh watchlist cache",
            description = "Manually refreshes the in-memory watchlist cache from database"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cache refreshed successfully"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Failed to refresh cache"
            )
    })
    public ResponseEntity<WatchlistDataService.CacheStats> refreshCache() {
        log.info("Manual cache refresh requested");
        
        try {
            watchlistDataService.refreshCache();
            return ResponseEntity.ok(watchlistDataService.getCacheStats());
        } catch (Exception e) {
            log.error("Failed to refresh cache", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private WatchlistEntryDto toDto(WatchlistEntryEntity entity) {
        return WatchlistEntryDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .aliases(watchlistDataService.parseAliases(entity))
                .dateOfBirth(entity.getDateOfBirth())
                .nationality(entity.getNationality())
                .listSource(entity.getListSource())
                .entryType(entity.getEntryType())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
