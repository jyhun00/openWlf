package aml.openwlf.data.service;

import aml.openwlf.data.entity.EntityNameEntity;
import aml.openwlf.data.entity.SanctionsEntity;
import aml.openwlf.data.repository.EntityAddressRepository;
import aml.openwlf.data.repository.EntityDocumentRepository;
import aml.openwlf.data.repository.EntityNameRepository;
import aml.openwlf.data.repository.SanctionsEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 제재 대상 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SanctionsQueryService {
    
    private final SanctionsEntityRepository sanctionsRepository;
    private final EntityNameRepository nameRepository;
    private final EntityAddressRepository addressRepository;
    private final EntityDocumentRepository documentRepository;
    
    /**
     * 전체 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<SanctionsEntity> findAll(Pageable pageable) {
        return sanctionsRepository.findAll(pageable);
    }
    
    /**
     * 활성 엔티티만 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<SanctionsEntity> findAllActive(Pageable pageable) {
        return sanctionsRepository.findByIsActive(true, pageable);
    }
    
    /**
     * ID로 상세 조회 (연관 데이터 포함)
     */
    @Transactional(readOnly = true)
    public Optional<SanctionsEntity> findByIdWithDetails(Long entityId) {
        return sanctionsRepository.findByIdWithAllRelations(entityId);
    }
    
    /**
     * 복합 검색
     */
    @Transactional(readOnly = true)
    public Page<SanctionsEntity> search(
            String name,
            String nationality,
            String sourceFile,
            String entityType,
            Pageable pageable) {
        
        return sanctionsRepository.searchWithFilters(name, nationality, sourceFile, entityType, pageable);
    }
    
    /**
     * 이름으로 검색 (Trigram 유사도 검색)
     */
    @Transactional(readOnly = true)
    public List<EntityNameEntity> searchByNameSimilarity(String name, double threshold, int limit) {
        try {
            return nameRepository.searchBySimilarityWithLimit(name, threshold, limit);
        } catch (Exception e) {
            // pg_trgm 확장이 없는 경우 LIKE 검색으로 대체
            log.warn("Trigram search failed, falling back to LIKE search: {}", e.getMessage());
            return nameRepository.searchByNormalizedNameActive(name);
        }
    }
    
    /**
     * 출처별 조회
     */
    @Transactional(readOnly = true)
    public Page<SanctionsEntity> findBySourceFile(String sourceFile, Pageable pageable) {
        return sanctionsRepository.findBySourceFileAndIsActive(sourceFile, true, pageable);
    }
    
    /**
     * 엔티티 유형별 조회
     */
    @Transactional(readOnly = true)
    public Page<SanctionsEntity> findByEntityType(String entityType, Pageable pageable) {
        return sanctionsRepository.findByEntityType(entityType, pageable);
    }
    
    /**
     * 국적별 조회
     */
    @Transactional(readOnly = true)
    public List<SanctionsEntity> findByNationality(String nationality) {
        return sanctionsRepository.findByNationalityActive(nationality);
    }
    
    /**
     * 통계 조회
     */
    @Transactional(readOnly = true)
    public SanctionsStats getStatistics() {
        long totalEntities = sanctionsRepository.count();
        long activeEntities = sanctionsRepository.countByIsActiveTrue();
        long totalNames = nameRepository.count();
        long totalAddresses = addressRepository.count();
        long totalDocuments = documentRepository.count();
        
        // 출처별 통계
        Map<String, Long> bySourceFile = sanctionsRepository.countBySourceFileGrouped()
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1],
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
        
        // 유형별 통계
        Map<String, Long> byEntityType = sanctionsRepository.countByEntityTypeGrouped()
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1],
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
        
        // 국적별 상위 10개
        Map<String, Long> topNationalities = sanctionsRepository.countByNationalityGrouped()
                .stream()
                .limit(10)
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1],
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
        
        return SanctionsStats.builder()
                .totalEntities(totalEntities)
                .activeEntities(activeEntities)
                .bySourceFile(bySourceFile)
                .byEntityType(byEntityType)
                .topNationalities(topNationalities)
                .totalNames(totalNames)
                .totalAddresses(totalAddresses)
                .totalDocuments(totalDocuments)
                .build();
    }
    
    /**
     * 출처 목록 조회
     */
    @Transactional(readOnly = true)
    public List<String> getSourceFiles() {
        return sanctionsRepository.countBySourceFileGrouped()
                .stream()
                .map(row -> (String) row[0])
                .collect(Collectors.toList());
    }
    
    /**
     * 엔티티 유형 목록 조회
     */
    @Transactional(readOnly = true)
    public List<String> getEntityTypes() {
        return sanctionsRepository.countByEntityTypeGrouped()
                .stream()
                .map(row -> (String) row[0])
                .collect(Collectors.toList());
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SanctionsStats {
        private long totalEntities;
        private long activeEntities;
        private Map<String, Long> bySourceFile;
        private Map<String, Long> byEntityType;
        private Map<String, Long> topNationalities;
        private long totalNames;
        private long totalAddresses;
        private long totalDocuments;
    }
}
