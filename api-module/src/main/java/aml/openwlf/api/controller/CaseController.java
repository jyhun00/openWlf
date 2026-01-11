package aml.openwlf.api.controller;

import aml.openwlf.api.dto.AlertDto;
import aml.openwlf.api.dto.CaseDto;
import aml.openwlf.api.dto.cases.*;
import aml.openwlf.data.entity.*;
import aml.openwlf.data.entity.CaseCommentEntity.CommentType;
import aml.openwlf.data.entity.CaseEntity.*;
import aml.openwlf.data.repository.CaseAlertRepository;
import aml.openwlf.data.repository.CaseCommentRepository;
import aml.openwlf.data.service.CaseService;
import aml.openwlf.data.service.CaseService.CreateCaseRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Case Management REST API Controller
 * 
 * Alert에서 Case로 전환하고, Case의 전체 생명주기를 관리합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
@Tag(name = "Case Management", description = "케이스 관리 API - Alert → Case 전환, 조사, 결정")
public class CaseController {
    
    private final CaseService caseService;
    private final CaseAlertRepository caseAlertRepository;
    private final CaseCommentRepository caseCommentRepository;
    
    // ==================== Case 생성 ====================
    
    @PostMapping("/from-alert/{alertId}")
    @Operation(
            summary = "Alert에서 Case 생성",
            description = """
                    Alert를 기반으로 새로운 조사 케이스를 생성합니다.
                    
                    **동작:**
                    1. Alert 정보를 기반으로 Case 생성
                    2. Alert를 Case에 자동 연결
                    3. Alert 상태를 IN_REVIEW로 변경
                    4. 리스크 점수 기반 우선순위 자동 설정
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "케이스 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 Alert가 이미 케이스에 연결됨"),
            @ApiResponse(responseCode = "404", description = "Alert를 찾을 수 없음")
    })
    public ResponseEntity<?> createCaseFromAlert(
            @Parameter(description = "Alert ID", required = true)
            @PathVariable Long alertId,
            @Valid @RequestBody CreateCaseFromAlertRequest request) {
        
        log.info("Creating case from alert {} by {}", alertId, request.getCreatedBy());
        
        try {
            CreateCaseRequest serviceRequest = CreateCaseRequest.builder()
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .caseType(request.getCaseType() != null ? 
                            CaseType.valueOf(request.getCaseType()) : null)
                    .assignedTo(request.getAssignedTo())
                    .assignedTeam(request.getAssignedTeam())
                    .createdBy(request.getCreatedBy())
                    .build();
            
            CaseEntity caseEntity = caseService.createCaseFromAlert(alertId, serviceRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(caseEntity));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/from-alerts")
    @Operation(
            summary = "여러 Alert를 묶어 Case 생성",
            description = "관련된 여러 Alert를 하나의 케이스로 통합합니다."
    )
    public ResponseEntity<?> createCaseFromMultipleAlerts(
            @Valid @RequestBody CreateCaseFromMultipleAlertsRequest request) {
        
        log.info("Creating case from {} alerts by {}", 
                request.getAlertIds().size(), request.getCreatedBy());
        
        try {
            CreateCaseRequest serviceRequest = CreateCaseRequest.builder()
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .caseType(request.getCaseType() != null ? 
                            CaseType.valueOf(request.getCaseType()) : null)
                    .assignedTo(request.getAssignedTo())
                    .assignedTeam(request.getAssignedTeam())
                    .createdBy(request.getCreatedBy())
                    .build();
            
            CaseEntity caseEntity = caseService.createCaseFromMultipleAlerts(
                    request.getAlertIds(), serviceRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(caseEntity));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    // ==================== Case 조회 ====================
    
    @GetMapping
    @Operation(
            summary = "케이스 목록 조회",
            description = "필터 조건에 맞는 케이스 목록을 페이징하여 조회합니다."
    )
    public ResponseEntity<Page<CaseDto>> getAllCases(
            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "20") int size,
            
            @Parameter(description = "정렬 필드")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            
            @Parameter(description = "정렬 방향 (asc/desc)")
            @RequestParam(defaultValue = "desc") String sortDir,
            
            @Parameter(description = "상태 필터")
            @RequestParam(required = false) String status,
            
            @Parameter(description = "우선순위 필터")
            @RequestParam(required = false) String priority,
            
            @Parameter(description = "케이스 유형 필터")
            @RequestParam(required = false) String caseType,
            
            @Parameter(description = "담당자 필터")
            @RequestParam(required = false) String assignedTo,
            
            @Parameter(description = "고객 ID 필터")
            @RequestParam(required = false) String customerId
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        CaseStatus statusEnum = parseEnum(status, CaseStatus.class);
        CasePriority priorityEnum = parseEnum(priority, CasePriority.class);
        CaseType typeEnum = parseEnum(caseType, CaseType.class);
        
        Page<CaseEntity> cases = caseService.searchCases(
                statusEnum, priorityEnum, typeEnum, assignedTo, customerId, pageable);
        
        return ResponseEntity.ok(cases.map(this::toDto));
    }
    
    @GetMapping("/open")
    @Operation(summary = "열린 케이스 목록", description = "종료되지 않은 케이스 목록을 조회합니다.")
    public ResponseEntity<Page<CaseDto>> getOpenCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("priority").ascending()
                .and(Sort.by("dueDate").ascending()));
        Page<CaseEntity> cases = caseService.getOpenCases(pageable);
        return ResponseEntity.ok(cases.map(this::toDto));
    }
    
    @GetMapping("/my")
    @Operation(summary = "내 케이스 목록", description = "특정 담당자에게 배정된 케이스를 조회합니다.")
    public ResponseEntity<Page<CaseDto>> getMyCases(
            @Parameter(description = "담당자 ID", required = true)
            @RequestParam String assignedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("dueDate").ascending());
        Page<CaseEntity> cases = caseService.getCasesByAssignee(assignedTo, pageable);
        return ResponseEntity.ok(cases.map(this::toDto));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "케이스 상세 조회", description = "케이스 ID로 상세 정보를 조회합니다.")
    public ResponseEntity<CaseDto> getCaseById(@PathVariable Long id) {
        return caseService.getCaseById(id)
                .map(entity -> ResponseEntity.ok(toDto(entity)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/reference/{reference}")
    @Operation(summary = "참조번호로 케이스 조회")
    public ResponseEntity<CaseDto> getCaseByReference(@PathVariable String reference) {
        return caseService.getCaseByReference(reference)
                .map(entity -> ResponseEntity.ok(toDto(entity)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    // ==================== Case 업데이트 ====================
    
    @PutMapping("/{id}/status")
    @Operation(summary = "케이스 상태 변경")
    public ResponseEntity<CaseDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody CaseStatusUpdateRequest request) {
        
        log.info("Updating case {} status to {} by {}", id, request.getStatus(), request.getUpdatedBy());
        
        CaseStatus newStatus;
        try {
            newStatus = CaseStatus.valueOf(request.getStatus());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        
        return caseService.updateStatus(id, newStatus, request.getUpdatedBy())
                .map(entity -> ResponseEntity.ok(toDto(entity)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}/assign")
    @Operation(summary = "케이스 담당자 배정")
    public ResponseEntity<CaseDto> assignCase(
            @PathVariable Long id,
            @Valid @RequestBody CaseAssignRequest request) {
        
        log.info("Assigning case {} to {} by {}", id, request.getAssignedTo(), request.getAssignedBy());
        
        return caseService.assignCase(id, request.getAssignedTo(), request.getAssignedBy())
                .map(entity -> ResponseEntity.ok(toDto(entity)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}/priority")
    @Operation(summary = "케이스 우선순위 변경")
    public ResponseEntity<CaseDto> updatePriority(
            @PathVariable Long id,
            @RequestParam String priority,
            @RequestParam String updatedBy) {
        
        CasePriority newPriority;
        try {
            newPriority = CasePriority.valueOf(priority);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        
        return caseService.updatePriority(id, newPriority, updatedBy)
                .map(entity -> ResponseEntity.ok(toDto(entity)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{id}/decision")
    @Operation(
            summary = "케이스 최종 결정",
            description = """
                    케이스에 대한 최종 결정을 내립니다.
                    
                    **결정 유형:**
                    - TRUE_POSITIVE: 실제 위험 확인
                    - FALSE_POSITIVE: 오탐
                    - INCONCLUSIVE: 판단 불가
                    - ESCALATED_TO_LE: 법 집행기관 보고
                    - NO_ACTION_REQUIRED: 조치 불필요
                    
                    결정 후 연결된 Alert들의 상태도 자동 업데이트됩니다.
                    """
    )
    public ResponseEntity<CaseDto> makeDecision(
            @PathVariable Long id,
            @Valid @RequestBody CaseDecisionRequest request) {
        
        log.info("Making decision on case {}: {} by {}", id, request.getDecision(), request.getDecidedBy());
        
        CaseDecision decision;
        try {
            decision = CaseDecision.valueOf(request.getDecision());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        
        return caseService.makeDecision(id, decision, request.getRationale(), request.getDecidedBy())
                .map(entity -> ResponseEntity.ok(toDto(entity)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    // ==================== Alert 연결 ====================
    
    @PostMapping("/{id}/alerts")
    @Operation(summary = "Alert 연결", description = "기존 케이스에 추가 Alert를 연결합니다.")
    public ResponseEntity<?> linkAlert(
            @PathVariable Long id,
            @Valid @RequestBody LinkAlertRequest request) {
        
        log.info("Linking alert {} to case {} by {}", request.getAlertId(), id, request.getLinkedBy());
        
        try {
            caseService.linkAlertToCase(id, request.getAlertId(), request.getReason(), request.getLinkedBy());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}/alerts/{alertId}")
    @Operation(summary = "Alert 연결 해제")
    public ResponseEntity<?> unlinkAlert(
            @PathVariable Long id,
            @PathVariable Long alertId,
            @RequestParam String unlinkedBy) {
        
        try {
            caseService.unlinkAlertFromCase(id, alertId, unlinkedBy);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @GetMapping("/{id}/alerts")
    @Operation(summary = "연결된 Alert 목록")
    public ResponseEntity<List<AlertDto>> getLinkedAlerts(@PathVariable Long id) {
        List<CaseAlertEntity> links = caseService.getLinkedAlerts(id);
        List<AlertDto> alerts = links.stream()
                .map(link -> toAlertDto(link.getAlertEntity()))
                .toList();
        return ResponseEntity.ok(alerts);
    }
    
    // ==================== 코멘트 ====================
    
    @PostMapping("/{id}/comments")
    @Operation(summary = "코멘트 추가")
    public ResponseEntity<CaseCommentDto> addComment(
            @PathVariable Long id,
            @Valid @RequestBody AddCommentRequest request) {
        
        log.info("Adding comment to case {} by {}", id, request.getCreatedBy());
        
        CommentType type = CommentType.NOTE;
        if (request.getCommentType() != null) {
            try {
                type = CommentType.valueOf(request.getCommentType());
            } catch (IllegalArgumentException ignored) {}
        }
        
        CaseCommentEntity comment = caseService.addComment(
                id, request.getContent(), type, request.getCreatedBy());
        return ResponseEntity.status(HttpStatus.CREATED).body(toCommentDto(comment));
    }
    
    @GetMapping("/{id}/comments")
    @Operation(summary = "코멘트 목록")
    public ResponseEntity<List<CaseCommentDto>> getComments(@PathVariable Long id) {
        List<CaseCommentEntity> comments = caseService.getComments(id);
        return ResponseEntity.ok(comments.stream().map(this::toCommentDto).toList());
    }
    
    // ==================== 활동 로그 ====================
    
    @GetMapping("/{id}/activities")
    @Operation(summary = "활동 로그", description = "케이스의 모든 활동 내역을 조회합니다 (감사 추적).")
    public ResponseEntity<List<CaseActivityDto>> getActivities(@PathVariable Long id) {
        List<CaseActivityEntity> activities = caseService.getActivities(id);
        return ResponseEntity.ok(activities.stream().map(this::toActivityDto).toList());
    }
    
    // ==================== 통계 ====================
    
    @GetMapping("/stats")
    @Operation(summary = "케이스 통계")
    public ResponseEntity<CaseService.CaseStats> getStatistics() {
        return ResponseEntity.ok(caseService.getStatistics());
    }
    
    // ==================== DTO 변환 ====================
    
    private CaseDto toDto(CaseEntity entity) {
        return CaseDto.builder()
                .id(entity.getId())
                .caseReference(entity.getCaseReference())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .status(entity.getStatus().name())
                .priority(entity.getPriority().name())
                .caseType(entity.getCaseType().name())
                .customerId(entity.getCustomerId())
                .customerName(entity.getCustomerName())
                .dateOfBirth(entity.getDateOfBirth())
                .nationality(entity.getNationality())
                .riskScore(entity.getRiskScore())
                .assignedTo(entity.getAssignedTo())
                .assignedTeam(entity.getAssignedTeam())
                .dueDate(entity.getDueDate())
                .decision(entity.getDecision() != null ? entity.getDecision().name() : null)
                .decisionRationale(entity.getDecisionRationale())
                .sarFiled(entity.getSarFiled())
                .sarReference(entity.getSarReference())
                .closedAt(entity.getClosedAt())
                .closedBy(entity.getClosedBy())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .linkedAlertCount((int) caseAlertRepository.countByCaseEntityId(entity.getId()))
                .commentCount((int) caseCommentRepository.countByCaseEntityId(entity.getId()))
                .build();
    }
    
    private AlertDto toAlertDto(AlertEntity entity) {
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
                .createdAt(entity.getCreatedAt())
                .build();
    }
    
    private CaseCommentDto toCommentDto(CaseCommentEntity entity) {
        return CaseCommentDto.builder()
                .id(entity.getId())
                .caseId(entity.getCaseEntity().getId())
                .content(entity.getContent())
                .commentType(entity.getCommentType().name())
                .isInternal(entity.getIsInternal())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
    private CaseActivityDto toActivityDto(CaseActivityEntity entity) {
        return CaseActivityDto.builder()
                .id(entity.getId())
                .caseId(entity.getCaseEntity().getId())
                .activityType(entity.getActivityType().name())
                .description(entity.getDescription())
                .oldValue(entity.getOldValue())
                .newValue(entity.getNewValue())
                .performedBy(entity.getPerformedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }
    
    private <T extends Enum<T>> T parseEnum(String value, Class<T> enumClass) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}