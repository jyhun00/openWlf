package aml.openwlf.api.controller;

import aml.openwlf.api.dto.sanctions.*;
import aml.openwlf.data.entity.EntityAddressEntity;
import aml.openwlf.data.entity.EntityDocumentEntity;
import aml.openwlf.data.entity.EntityNameEntity;
import aml.openwlf.data.entity.SanctionsEntity;
import aml.openwlf.data.entity.SanctionsSyncHistoryEntity;
import aml.openwlf.data.repository.SanctionsSyncHistoryRepository;
import aml.openwlf.data.service.SanctionsQueryService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 제재 대상 조회 API Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/sanctions")
@RequiredArgsConstructor
@Tag(name = "Sanctions (v2)", description = "제재 대상 조회 API (비정규화 스키마)")
public class SanctionsController {

    private final SanctionsQueryService queryService;
    private final SanctionsSyncHistoryRepository syncHistoryRepository;
    
    // ========================================
    // 목록 조회
    // ========================================
    
    @GetMapping
    @Operation(
            summary = "제재 대상 목록 조회",
            description = "제재 대상 목록을 페이징하여 조회합니다. 이름, 국적, 출처, 유형으로 필터링 가능합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<Page<SanctionsListItemDto>> getList(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            
            @Parameter(description = "정렬 필드", example = "primaryName")
            @RequestParam(defaultValue = "entityId") String sortBy,
            
            @Parameter(description = "정렬 방향", example = "asc")
            @RequestParam(defaultValue = "asc") String sortDir,
            
            @Parameter(description = "이름 검색어")
            @RequestParam(required = false) String name,
            
            @Parameter(description = "국적 필터 (예: KP, IR, RU)")
            @RequestParam(required = false) String nationality,
            
            @Parameter(description = "출처 필터 (예: UN, OFAC, EU)")
            @RequestParam(required = false) String sourceFile,
            
            @Parameter(description = "유형 필터 (예: Individual, Entity, Vessel)")
            @RequestParam(required = false) String entityType
    ) {
        log.info("제재 대상 목록 조회 - page: {}, name: {}, nationality: {}, source: {}, type: {}",
                page, name, nationality, sourceFile, entityType);
        
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<SanctionsEntity> entityPage = queryService.search(name, nationality, sourceFile, entityType, pageable);
        Page<SanctionsListItemDto> dtoPage = entityPage.map(this::toListItemDto);
        
        return ResponseEntity.ok(dtoPage);
    }
    
    // ========================================
    // 상세 조회
    // ========================================
    
    @GetMapping("/{entityId}")
    @Operation(
            summary = "제재 대상 상세 조회",
            description = "제재 대상의 상세 정보를 조회합니다. 이름, 주소, 문서 정보가 모두 포함됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = SanctionsEntityDto.class))),
            @ApiResponse(responseCode = "404", description = "엔티티를 찾을 수 없음")
    })
    public ResponseEntity<SanctionsEntityDto> getDetail(
            @Parameter(description = "엔티티 ID", required = true, example = "1")
            @PathVariable Long entityId
    ) {
        log.info("제재 대상 상세 조회 - entityId: {}", entityId);
        
        return queryService.findByIdWithDetails(entityId)
                .map(entity -> ResponseEntity.ok(toDetailDto(entity)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    // ========================================
    // 검색
    // ========================================
    
    @GetMapping("/search")
    @Operation(
            summary = "이름 유사도 검색",
            description = "이름의 유사도를 기반으로 검색합니다. PostgreSQL pg_trgm 확장 사용 시 더 정확한 결과를 제공합니다."
    )
    @ApiResponse(responseCode = "200", description = "검색 성공")
    public ResponseEntity<List<SanctionsListItemDto>> searchByName(
            @Parameter(description = "검색할 이름", required = true, example = "kim jong")
            @RequestParam String name,
            
            @Parameter(description = "유사도 임계값 (0.0 ~ 1.0)", example = "0.3")
            @RequestParam(defaultValue = "0.3") double threshold,
            
            @Parameter(description = "최대 결과 수", example = "50")
            @RequestParam(defaultValue = "50") int limit
    ) {
        log.info("이름 유사도 검색 - name: {}, threshold: {}, limit: {}", name, threshold, limit);
        
        List<EntityNameEntity> nameEntities = queryService.searchByNameSimilarity(name, threshold, limit);
        
        // 엔티티 ID 기준으로 중복 제거 후 변환
        List<SanctionsListItemDto> results = nameEntities.stream()
                .map(EntityNameEntity::getSanctionsEntity)
                .distinct()
                .map(this::toListItemDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(results);
    }
    
    // ========================================
    // 필터 옵션
    // ========================================
    
    @GetMapping("/sources")
    @Operation(summary = "데이터 출처 목록", description = "사용 가능한 데이터 출처(UN, OFAC 등) 목록을 반환합니다.")
    public ResponseEntity<List<String>> getSources() {
        return ResponseEntity.ok(queryService.getSourceFiles());
    }
    
    @GetMapping("/entity-types")
    @Operation(summary = "엔티티 유형 목록", description = "사용 가능한 엔티티 유형(Individual, Entity 등) 목록을 반환합니다.")
    public ResponseEntity<List<String>> getEntityTypes() {
        return ResponseEntity.ok(queryService.getEntityTypes());
    }
    
    // ========================================
    // 통계
    // ========================================
    
    @GetMapping("/stats")
    @Operation(
            summary = "통계 조회",
            description = "제재 대상 전체 통계를 조회합니다. 출처별, 유형별, 국적별 집계가 포함됩니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<SanctionsStatsDto> getStatistics() {
        log.info("제재 대상 통계 조회");
        
        SanctionsQueryService.SanctionsStats stats = queryService.getStatistics();
        
        SanctionsStatsDto dto = SanctionsStatsDto.builder()
                .totalEntities(stats.getTotalEntities())
                .activeEntities(stats.getActiveEntities())
                .bySourceFile(stats.getBySourceFile())
                .byEntityType(stats.getByEntityType())
                .topNationalities(stats.getTopNationalities())
                .totalNames(stats.getTotalNames())
                .totalAddresses(stats.getTotalAddresses())
                .totalDocuments(stats.getTotalDocuments())
                .lastDataUpdate(getLastSuccessfulSyncTime())
                .build();
        
        return ResponseEntity.ok(dto);
    }
    
    // ========================================
    // DTO 변환 메서드
    // ========================================
    
    private SanctionsListItemDto toListItemDto(SanctionsEntity entity) {
        List<String> aliases = entity.getNames() != null
                ? entity.getNames().stream()
                    .filter(n -> !"Primary".equals(n.getNameType()))
                    .map(EntityNameEntity::getFullName)
                    .limit(5) // 최대 5개만
                    .collect(Collectors.toList())
                : List.of();
        
        return SanctionsListItemDto.builder()
                .entityId(entity.getEntityId())
                .sourceUid(entity.getSourceUid())
                .sourceFile(entity.getSourceFile())
                .entityType(entity.getEntityType())
                .primaryName(entity.getPrimaryName())
                .aliases(aliases)
                .nationality(entity.getNationality())
                .birthDate(entity.getBirthDate())
                .sanctionListType(entity.getSanctionListType())
                .lastUpdatedAt(entity.getLastUpdatedAt())
                .build();
    }
    
    private SanctionsEntityDto toDetailDto(SanctionsEntity entity) {
        return SanctionsEntityDto.builder()
                .entityId(entity.getEntityId())
                .sourceUid(entity.getSourceUid())
                .sourceFile(entity.getSourceFile())
                .entityType(entity.getEntityType())
                .primaryName(entity.getPrimaryName())
                .gender(entity.getGender())
                .birthDate(entity.getBirthDate())
                .nationality(entity.getNationality())
                .vesselFlag(entity.getVesselFlag())
                .sanctionListType(entity.getSanctionListType())
                .names(toNameDtos(entity.getNames()))
                .addresses(toAddressDtos(entity.getAddresses()))
                .documents(toDocumentDtos(entity.getDocuments()))
                .additionalFeatures(entity.getAdditionalFeatures())
                .isActive(entity.getIsActive())
                .lastUpdatedAt(entity.getLastUpdatedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
    
    private List<SanctionsEntityDto.NameInfo> toNameDtos(List<EntityNameEntity> names) {
        if (names == null) return List.of();
        
        return names.stream()
                .map(n -> SanctionsEntityDto.NameInfo.builder()
                        .nameId(n.getNameId())
                        .nameType(n.getNameType())
                        .fullName(n.getFullName())
                        .script(n.getScript())
                        .qualityScore(n.getQualityScore())
                        .isPrimary(n.isPrimary())
                        .build())
                .collect(Collectors.toList());
    }
    
    private List<SanctionsEntityDto.Address> toAddressDtos(List<EntityAddressEntity> addresses) {
        if (addresses == null) return List.of();
        
        return addresses.stream()
                .map(a -> SanctionsEntityDto.Address.builder()
                        .addressId(a.getAddressId())
                        .addressType(a.getAddressType())
                        .fullAddress(a.getFullAddress())
                        .city(a.getCity())
                        .stateProvince(a.getStateProvince())
                        .country(a.getCountry())
                        .countryCode(a.getCountryCode())
                        .build())
                .collect(Collectors.toList());
    }
    
    private List<SanctionsEntityDto.Document> toDocumentDtos(List<EntityDocumentEntity> documents) {
        if (documents == null) return List.of();
        
        return documents.stream()
                .map(d -> SanctionsEntityDto.Document.builder()
                        .documentId(d.getDocumentId())
                        .documentType(d.getDocumentType())
                        .documentNumber(d.getDocumentNumber())
                        .issuingCountry(d.getIssuingCountry())
                        .issuingCountryCode(d.getIssuingCountryCode())
                        .issueDate(d.getIssueDate())
                        .expiryDate(d.getExpiryDate())
                        .issuingAuthority(d.getIssuingAuthority())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 마지막 성공적인 동기화 시간 조회
     */
    private LocalDateTime getLastSuccessfulSyncTime() {
        return syncHistoryRepository.findLatestByEachSource().stream()
                .filter(h -> h.getStatus() == SanctionsSyncHistoryEntity.SyncStatus.SUCCESS)
                .map(SanctionsSyncHistoryEntity::getFinishedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }
}
