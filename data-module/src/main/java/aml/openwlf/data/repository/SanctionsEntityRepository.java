package aml.openwlf.data.repository;

import aml.openwlf.data.entity.SanctionsEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 제재 대상 엔티티 Repository
 */
@Repository
public interface SanctionsEntityRepository extends JpaRepository<SanctionsEntity, Long> {
    
    // ========================================
    // 기본 조회
    // ========================================
    
    Optional<SanctionsEntity> findBySourceUidAndSourceFile(String sourceUid, String sourceFile);
    
    List<SanctionsEntity> findByIsActiveTrue();
    
    List<SanctionsEntity> findBySourceFileAndIsActiveTrue(String sourceFile);
    
    List<SanctionsEntity> findByEntityTypeAndIsActiveTrue(String entityType);
    
    // ========================================
    // 페이징 조회
    // ========================================
    
    Page<SanctionsEntity> findByIsActive(Boolean isActive, Pageable pageable);
    
    Page<SanctionsEntity> findBySourceFile(String sourceFile, Pageable pageable);
    
    Page<SanctionsEntity> findBySourceFileAndIsActive(String sourceFile, Boolean isActive, Pageable pageable);
    
    Page<SanctionsEntity> findByEntityType(String entityType, Pageable pageable);
    
    Page<SanctionsEntity> findByNationality(String nationality, Pageable pageable);
    
    // ========================================
    // 검색 쿼리 (정규화 완화의 장점 - 단순 WHERE 조건)
    // ========================================
    
    /**
     * 국적으로 검색 (조인 없이 바로 조회)
     */
    @Query("SELECT s FROM SanctionsEntity s WHERE s.nationality = :nationality AND s.isActive = true")
    List<SanctionsEntity> findByNationalityActive(@Param("nationality") String nationality);
    
    /**
     * 엔티티 유형과 국적으로 검색
     */
    @Query("SELECT s FROM SanctionsEntity s WHERE s.entityType = :entityType AND s.nationality = :nationality AND s.isActive = true")
    List<SanctionsEntity> findByEntityTypeAndNationalityActive(
            @Param("entityType") String entityType,
            @Param("nationality") String nationality);
    
    /**
     * 이름 부분 일치 검색 (정규화된 이름 기준)
     */
    @Query("SELECT s FROM SanctionsEntity s WHERE LOWER(s.normalizedName) LIKE LOWER(CONCAT('%', :name, '%')) AND s.isActive = true")
    List<SanctionsEntity> searchByNormalizedName(@Param("name") String name);
    
    /**
     * 이름 부분 일치 검색 (페이징)
     */
    @Query("SELECT s FROM SanctionsEntity s WHERE LOWER(s.normalizedName) LIKE LOWER(CONCAT('%', :name, '%')) AND s.isActive = true")
    Page<SanctionsEntity> searchByNormalizedName(@Param("name") String name, Pageable pageable);
    
    /**
     * 복합 검색 (이름 + 국적 + 출처)
     */
    @Query("SELECT s FROM SanctionsEntity s WHERE " +
           "(:name IS NULL OR LOWER(s.normalizedName) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:nationality IS NULL OR s.nationality = :nationality) AND " +
           "(:sourceFile IS NULL OR s.sourceFile = :sourceFile) AND " +
           "(:entityType IS NULL OR s.entityType = :entityType) AND " +
           "s.isActive = true")
    Page<SanctionsEntity> searchWithFilters(
            @Param("name") String name,
            @Param("nationality") String nationality,
            @Param("sourceFile") String sourceFile,
            @Param("entityType") String entityType,
            Pageable pageable);
    
    // ========================================
    // 통계 쿼리
    // ========================================
    
    long countByIsActiveTrue();
    
    long countBySourceFile(String sourceFile);
    
    long countByEntityType(String entityType);
    
    @Query("SELECT s.sourceFile, COUNT(s) FROM SanctionsEntity s WHERE s.isActive = true GROUP BY s.sourceFile")
    List<Object[]> countBySourceFileGrouped();
    
    @Query("SELECT s.entityType, COUNT(s) FROM SanctionsEntity s WHERE s.isActive = true GROUP BY s.entityType")
    List<Object[]> countByEntityTypeGrouped();
    
    @Query("SELECT s.nationality, COUNT(s) FROM SanctionsEntity s WHERE s.isActive = true AND s.nationality IS NOT NULL GROUP BY s.nationality ORDER BY COUNT(s) DESC")
    List<Object[]> countByNationalityGrouped();
    
    // ========================================
    // 캐시 로딩용 (Fetch Join으로 N+1 방지)
    // ========================================
    
    @Query("SELECT DISTINCT s FROM SanctionsEntity s " +
           "LEFT JOIN FETCH s.names " +
           "WHERE s.isActive = true")
    List<SanctionsEntity> findAllActiveWithNames();
    
    @Query("SELECT DISTINCT s FROM SanctionsEntity s " +
           "LEFT JOIN FETCH s.names " +
           "LEFT JOIN FETCH s.addresses " +
           "LEFT JOIN FETCH s.documents " +
           "WHERE s.entityId = :entityId")
    Optional<SanctionsEntity> findByIdWithAllRelations(@Param("entityId") Long entityId);
}
