package aml.openwlf.data.repository;

import aml.openwlf.data.entity.WatchlistEntryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for watchlist entries
 */
@Repository
public interface WatchlistEntryRepository extends JpaRepository<WatchlistEntryEntity, Long> {

    List<WatchlistEntryEntity> findByIsActiveTrue();

    List<WatchlistEntryEntity> findByListSourceAndIsActiveTrue(String listSource);

    @Query("SELECT w FROM WatchlistEntryEntity w WHERE w.isActive = true AND " +
           "(w.normalizedName LIKE %:searchTerm% OR w.aliases LIKE %:searchTerm%)")
    List<WatchlistEntryEntity> searchByNameOrAlias(@Param("searchTerm") String searchTerm);

    // Paginated queries
    Page<WatchlistEntryEntity> findAll(Pageable pageable);

    Page<WatchlistEntryEntity> findByIsActive(Boolean isActive, Pageable pageable);

    Page<WatchlistEntryEntity> findByListSource(String listSource, Pageable pageable);

    Page<WatchlistEntryEntity> findByListSourceAndIsActive(String listSource, Boolean isActive, Pageable pageable);

    @Query("SELECT w FROM WatchlistEntryEntity w WHERE " +
           "(w.normalizedName LIKE %:searchTerm% OR w.aliases LIKE %:searchTerm%)")
    Page<WatchlistEntryEntity> searchByNameOrAlias(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT w FROM WatchlistEntryEntity w WHERE " +
           "(w.normalizedName LIKE %:searchTerm% OR w.aliases LIKE %:searchTerm%) " +
           "AND (:isActive IS NULL OR w.isActive = :isActive)")
    Page<WatchlistEntryEntity> searchByNameOrAliasWithActiveFilter(
            @Param("searchTerm") String searchTerm,
            @Param("isActive") Boolean isActive,
            Pageable pageable);

    @Query("SELECT w FROM WatchlistEntryEntity w WHERE " +
           "(w.normalizedName LIKE %:searchTerm% OR w.aliases LIKE %:searchTerm%) " +
           "AND w.listSource = :listSource " +
           "AND (:isActive IS NULL OR w.isActive = :isActive)")
    Page<WatchlistEntryEntity> searchByNameOrAliasWithFilters(
            @Param("searchTerm") String searchTerm,
            @Param("listSource") String listSource,
            @Param("isActive") Boolean isActive,
            Pageable pageable);

    // Count queries for statistics
    long countByIsActiveTrue();

    long countByListSource(String listSource);
}
