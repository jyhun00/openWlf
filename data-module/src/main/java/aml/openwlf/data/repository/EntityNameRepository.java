package aml.openwlf.data.repository;

import aml.openwlf.data.entity.EntityNameEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 이름/별칭 Repository
 * 
 * Trigram 인덱스를 활용한 부분 일치 검색에 최적화되어 있습니다.
 * Native Query로 PostgreSQL의 pg_trgm 확장 기능을 활용할 수 있습니다.
 */
@Repository
public interface EntityNameRepository extends JpaRepository<EntityNameEntity, Long> {
    
    // ========================================
    // 기본 조회
    // ========================================
    
    List<EntityNameEntity> findBySanctionsEntityEntityId(Long entityId);
    
    List<EntityNameEntity> findByNameType(String nameType);
    
    List<EntityNameEntity> findBySanctionsEntityEntityIdAndNameType(Long entityId, String nameType);
    
    // ========================================
    // 이름 검색 (JPQL)
    // ========================================
    
    /**
     * 이름 부분 일치 검색
     */
    @Query("SELECT n FROM EntityNameEntity n WHERE LOWER(n.fullName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<EntityNameEntity> searchByFullName(@Param("name") String name);
    
    /**
     * 정규화된 이름으로 검색
     */
    @Query("SELECT n FROM EntityNameEntity n WHERE LOWER(n.normalizedName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<EntityNameEntity> searchByNormalizedName(@Param("name") String name);
    
    /**
     * 정규화된 이름으로 검색 (페이징)
     */
    @Query("SELECT n FROM EntityNameEntity n WHERE LOWER(n.normalizedName) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<EntityNameEntity> searchByNormalizedName(@Param("name") String name, Pageable pageable);
    
    /**
     * 고품질 이름만 검색 (Low Quality AKA 제외)
     */
    @Query("SELECT n FROM EntityNameEntity n WHERE " +
           "LOWER(n.normalizedName) LIKE LOWER(CONCAT('%', :name, '%')) AND " +
           "n.nameType <> 'Low Quality AKA'")
    List<EntityNameEntity> searchByNormalizedNameHighQuality(@Param("name") String name);
    
    /**
     * 활성 엔티티의 이름만 검색
     */
    @Query("SELECT n FROM EntityNameEntity n " +
           "JOIN n.sanctionsEntity s " +
           "WHERE LOWER(n.normalizedName) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "AND s.isActive = true")
    List<EntityNameEntity> searchByNormalizedNameActive(@Param("name") String name);
    
    /**
     * 활성 엔티티의 이름만 검색 (페이징)
     */
    @Query("SELECT n FROM EntityNameEntity n " +
           "JOIN n.sanctionsEntity s " +
           "WHERE LOWER(n.normalizedName) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "AND s.isActive = true")
    Page<EntityNameEntity> searchByNormalizedNameActive(@Param("name") String name, Pageable pageable);
    
    // ========================================
    // PostgreSQL Trigram 검색 (Native Query)
    // 사용 전 pg_trgm 확장과 인덱스 생성 필요:
    // CREATE EXTENSION IF NOT EXISTS pg_trgm;
    // CREATE INDEX idx_en_full_name_trgm ON entity_names USING gin (full_name gin_trgm_ops);
    // ========================================
    
    /**
     * Trigram 유사도 검색 (PostgreSQL)
     * similarity() 함수 사용 - 0.0 ~ 1.0 사이의 유사도 반환
     */
    @Query(value = "SELECT * FROM entity_names n " +
                   "WHERE similarity(n.normalized_name, :name) > :threshold " +
                   "ORDER BY similarity(n.normalized_name, :name) DESC",
           nativeQuery = true)
    List<EntityNameEntity> searchBySimilarity(
            @Param("name") String name,
            @Param("threshold") double threshold);
    
    /**
     * Trigram 유사도 검색 (결과 개수 제한)
     */
    @Query(value = "SELECT * FROM entity_names n " +
                   "WHERE similarity(n.normalized_name, :name) > :threshold " +
                   "ORDER BY similarity(n.normalized_name, :name) DESC " +
                   "LIMIT :limit",
           nativeQuery = true)
    List<EntityNameEntity> searchBySimilarityWithLimit(
            @Param("name") String name,
            @Param("threshold") double threshold,
            @Param("limit") int limit);
    
    /**
     * Trigram LIKE 검색 (% 연산자 활용)
     */
    @Query(value = "SELECT * FROM entity_names n " +
                   "WHERE n.normalized_name % :name " +
                   "ORDER BY similarity(n.normalized_name, :name) DESC",
           nativeQuery = true)
    List<EntityNameEntity> searchByTrigramLike(@Param("name") String name);
    
    // ========================================
    // 통계
    // ========================================
    
    long countByNameType(String nameType);
    
    @Query("SELECT n.nameType, COUNT(n) FROM EntityNameEntity n GROUP BY n.nameType")
    List<Object[]> countByNameTypeGrouped();
    
    @Query("SELECT n.script, COUNT(n) FROM EntityNameEntity n WHERE n.script IS NOT NULL GROUP BY n.script")
    List<Object[]> countByScriptGrouped();
}
