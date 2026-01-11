package aml.openwlf.api.controller;

import aml.openwlf.api.dto.AlertAssignRequest;
import aml.openwlf.api.dto.AlertDto;
import aml.openwlf.api.dto.AlertUpdateRequest;
import aml.openwlf.data.entity.AlertEntity;
import aml.openwlf.data.entity.AlertEntity.AlertStatus;
import aml.openwlf.data.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API Controller for alert management
 */
@Slf4j
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Tag(name = "Alert Management", description = "APIs for managing and querying alerts")
public class AlertController {
    
    private final AlertService alertService;
    
    @GetMapping
    @Operation(
            summary = "Get all alerts",
            description = "Retrieves all alerts with pagination and optional filtering"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved alerts"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Page<AlertDto>> getAllAlerts(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            
            @Parameter(description = "Sort field", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            
            @Parameter(description = "Sort direction (asc/desc)", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDir,
            
            @Parameter(description = "Filter by status")
            @RequestParam(required = false) String status,
            
            @Parameter(description = "Filter by customer ID")
            @RequestParam(required = false) String customerId,
            
            @Parameter(description = "Filter by minimum score")
            @RequestParam(required = false) Double minScore
    ) {
        log.info("Fetching alerts - page: {}, size: {}, status: {}, customerId: {}, minScore: {}", 
                page, size, status, customerId, minScore);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        AlertStatus alertStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                alertStatus = AlertStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status filter: {}", status);
            }
        }
        
        Page<AlertEntity> entityPage = alertService.searchAlerts(alertStatus, customerId, minScore, pageable);
        Page<AlertDto> dtoPage = entityPage.map(this::toDto);
        
        return ResponseEntity.ok(dtoPage);
    }
    
    @GetMapping("/open")
    @Operation(
            summary = "Get open alerts",
            description = "Retrieves all open alerts (NEW, IN_REVIEW, ESCALATED)"
    )
    public ResponseEntity<Page<AlertDto>> getOpenAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AlertEntity> entityPage = alertService.getOpenAlerts(pageable);
        return ResponseEntity.ok(entityPage.map(this::toDto));
    }
    
    @GetMapping("/{id}")
    @Operation(
            summary = "Get alert by ID",
            description = "Retrieves a specific alert by its ID"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved alert",
                    content = @Content(schema = @Schema(implementation = AlertDto.class))),
            @ApiResponse(responseCode = "404", description = "Alert not found")
    })
    public ResponseEntity<AlertDto> getAlertById(
            @Parameter(description = "Alert ID", required = true)
            @PathVariable Long id
    ) {
        log.info("Fetching alert with id: {}", id);
        
        return alertService.getAlertById(id)
                .map(entity -> ResponseEntity.ok(toDto(entity)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/reference/{reference}")
    @Operation(
            summary = "Get alert by reference",
            description = "Retrieves a specific alert by its reference number"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved alert"),
            @ApiResponse(responseCode = "404", description = "Alert not found")
    })
    public ResponseEntity<AlertDto> getAlertByReference(
            @Parameter(description = "Alert reference", required = true, example = "ALT-20251228-A1B2C3D4")
            @PathVariable String reference
    ) {
        log.info("Fetching alert with reference: {}", reference);
        
        return alertService.getAlertByReference(reference)
                .map(entity -> ResponseEntity.ok(toDto(entity)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}/status")
    @Operation(
            summary = "Update alert status",
            description = "Updates the status of an alert"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alert status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status"),
            @ApiResponse(responseCode = "404", description = "Alert not found")
    })
    public ResponseEntity<AlertDto> updateAlertStatus(
            @Parameter(description = "Alert ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody AlertUpdateRequest request
    ) {
        log.info("Updating alert {} status to {}", id, request.getStatus());
        
        AlertStatus newStatus;
        try {
            newStatus = AlertStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        
        // Check if it's a resolution status
        if (newStatus == AlertStatus.CONFIRMED || 
            newStatus == AlertStatus.FALSE_POSITIVE || 
            newStatus == AlertStatus.CLOSED) {
            return alertService.resolveAlert(id, newStatus, request.getComment(), request.getUpdatedBy())
                    .map(entity -> ResponseEntity.ok(toDto(entity)))
                    .orElse(ResponseEntity.notFound().build());
        }
        
        return alertService.updateStatus(id, newStatus, request.getUpdatedBy())
                .map(entity -> ResponseEntity.ok(toDto(entity)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}/assign")
    @Operation(
            summary = "Assign alert to analyst",
            description = "Assigns an alert to a specific analyst for review"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alert assigned successfully"),
            @ApiResponse(responseCode = "404", description = "Alert not found")
    })
    public ResponseEntity<AlertDto> assignAlert(
            @Parameter(description = "Alert ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody AlertAssignRequest request
    ) {
        log.info("Assigning alert {} to {}", id, request.getAssignedTo());
        
        return alertService.assignAlert(id, request.getAssignedTo())
                .map(entity -> ResponseEntity.ok(toDto(entity)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/stats")
    @Operation(
            summary = "Get alert statistics",
            description = "Retrieves statistics about alerts"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved statistics")
    public ResponseEntity<AlertService.AlertStats> getStatistics() {
        log.info("Fetching alert statistics");
        return ResponseEntity.ok(alertService.getStatistics());
    }
    
    @GetMapping("/{id}/case")
    @Operation(
            summary = "Alert의 연결된 Case 조회",
            description = "Alert가 연결된 Case가 있으면 Case ID를 반환합니다."
    )
    public ResponseEntity<?> getLinkedCase(@PathVariable Long id) {
        // 이 기능은 CaseAlertRepository가 필요하므로 CaseController에서 처리하거나
        // 별도 서비스를 통해 조회해야 합니다.
        // 여기서는 간단히 정보 메시지만 반환
        return ResponseEntity.ok(java.util.Map.of(
                "message", "Use GET /api/cases?customerId=xxx to find cases for this alert's customer",
                "alertId", id
        ));
    }
    
    private AlertDto toDto(AlertEntity entity) {
        return AlertDto.builder()
                .id(entity.getId())
                .alertReference(entity.getAlertReference())
                .status(entity.getStatus().name())
                .customerId(entity.getCustomerId())
                .customerName(entity.getCustomerName())
                .dateOfBirth(entity.getDateOfBirth())
                .nationality(entity.getNationality())
                .score(entity.getScore())
                .explanation(entity.getExplanation())
                .assignedTo(entity.getAssignedTo())
                .resolutionComment(entity.getResolutionComment())
                .resolvedAt(entity.getResolvedAt())
                .resolvedBy(entity.getResolvedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
