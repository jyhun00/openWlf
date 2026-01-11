package aml.openwlf.batch.controller;

import aml.openwlf.batch.dto.SanctionsSyncHistoryDto;
import aml.openwlf.batch.dto.SanctionsSyncStatusDto;
import aml.openwlf.batch.service.SanctionsSyncHistoryService;
import aml.openwlf.batch.service.SanctionsSyncService;
import aml.openwlf.batch.service.SyncResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 제재 리스트 동기화 관리 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/sanctions-sync")
@RequiredArgsConstructor
@Tag(name = "Sanctions Sync Admin", description = "제재 리스트 동기화 관리 API")
public class SanctionsSyncController {

    private final SanctionsSyncService sanctionsSyncService;
    private final SanctionsSyncHistoryService historyService;

    // ========================================
    // 동기화 실행 API
    // ========================================

    @PostMapping("/all")
    @Operation(summary = "전체 동기화", description = "OFAC, UN, EU 제재 리스트를 모두 동기화합니다")
    public ResponseEntity<List<SyncResult>> syncAll() {
        log.info("Manual sync all requested");
        List<SyncResult> results = sanctionsSyncService.syncAll();
        return ResponseEntity.ok(results);
    }

    @PostMapping("/ofac")
    @Operation(summary = "OFAC 동기화", description = "OFAC SDN 리스트를 동기화합니다")
    public ResponseEntity<SyncResult> syncOfac() {
        log.info("Manual OFAC sync requested");
        SyncResult result = sanctionsSyncService.syncOfac();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/un")
    @Operation(summary = "UN 동기화", description = "UN Security Council Consolidated List를 동기화합니다")
    public ResponseEntity<SyncResult> syncUn() {
        log.info("Manual UN sync requested");
        SyncResult result = sanctionsSyncService.syncUn();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/eu")
    @Operation(summary = "EU 동기화", description = "EU Consolidated Financial Sanctions List를 동기화합니다")
    public ResponseEntity<SyncResult> syncEu() {
        log.info("Manual EU sync requested");
        SyncResult result = sanctionsSyncService.syncEu();
        return ResponseEntity.ok(result);
    }

    // ========================================
    // 상태 조회 API
    // ========================================

    @GetMapping("/status")
    @Operation(summary = "동기화 상태 조회", description = "OFAC, UN 각각의 현재 동기화 상태를 조회합니다")
    public ResponseEntity<SanctionsSyncStatusDto> getStatus() {
        return ResponseEntity.ok(historyService.getCurrentStatus());
    }

    @GetMapping("/status/{sourceFile}")
    @Operation(summary = "특정 소스 동기화 상태 조회", description = "특정 소스(OFAC/UN/EU)의 동기화 상태를 조회합니다")
    public ResponseEntity<SanctionsSyncStatusDto.SourceSyncStatus> getSourceStatus(
            @Parameter(description = "소스 (OFAC, UN, EU)") @PathVariable String sourceFile) {
        return ResponseEntity.ok(historyService.getSourceStatus(sourceFile.toUpperCase()));
    }

    // ========================================
    // 이력 조회 API
    // ========================================

    @GetMapping("/history")
    @Operation(summary = "동기화 이력 목록 조회", description = "동기화 실행 이력을 조회합니다")
    public ResponseEntity<Page<SanctionsSyncHistoryDto>> getHistory(
            @Parameter(description = "소스 필터 (OFAC, UN, EU)")
            @RequestParam(required = false) String sourceFile,
            @Parameter(description = "상태 필터 (SUCCESS, FAIL)")
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        String normalizedSource = sourceFile != null ? sourceFile.toUpperCase() : null;
        String normalizedStatus = status != null ? status.toUpperCase() : null;

        return ResponseEntity.ok(historyService.getHistory(normalizedSource, normalizedStatus, pageable));
    }

    @GetMapping("/history/{historyId}")
    @Operation(summary = "동기화 이력 상세 조회", description = "특정 동기화 이력의 상세 정보를 조회합니다 (에러 로그 포함)")
    public ResponseEntity<SanctionsSyncHistoryDto> getHistoryDetail(
            @Parameter(description = "이력 ID") @PathVariable Long historyId) {
        return ResponseEntity.ok(historyService.getHistoryDetail(historyId));
    }

    @GetMapping("/history/ofac")
    @Operation(summary = "OFAC 동기화 이력 조회", description = "OFAC 동기화 이력만 조회합니다")
    public ResponseEntity<Page<SanctionsSyncHistoryDto>> getOfacHistory(
            @Parameter(description = "상태 필터 (SUCCESS, FAIL)") 
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC) 
            Pageable pageable) {
        
        String normalizedStatus = status != null ? status.toUpperCase() : null;
        return ResponseEntity.ok(historyService.getHistory("OFAC", normalizedStatus, pageable));
    }

    @GetMapping("/history/un")
    @Operation(summary = "UN 동기화 이력 조회", description = "UN 동기화 이력만 조회합니다")
    public ResponseEntity<Page<SanctionsSyncHistoryDto>> getUnHistory(
            @Parameter(description = "상태 필터 (SUCCESS, FAIL)")
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        String normalizedStatus = status != null ? status.toUpperCase() : null;
        return ResponseEntity.ok(historyService.getHistory("UN", normalizedStatus, pageable));
    }

    @GetMapping("/history/eu")
    @Operation(summary = "EU 동기화 이력 조회", description = "EU 동기화 이력만 조회합니다")
    public ResponseEntity<Page<SanctionsSyncHistoryDto>> getEuHistory(
            @Parameter(description = "상태 필터 (SUCCESS, FAIL)")
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        String normalizedStatus = status != null ? status.toUpperCase() : null;
        return ResponseEntity.ok(historyService.getHistory("EU", normalizedStatus, pageable));
    }
}
