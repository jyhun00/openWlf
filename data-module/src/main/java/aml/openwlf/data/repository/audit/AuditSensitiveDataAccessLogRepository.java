package aml.openwlf.data.repository.audit;

import aml.openwlf.data.entity.audit.AuditSensitiveDataAccessLogEntity;
import aml.openwlf.data.entity.audit.enums.DataAccessType;
import aml.openwlf.data.entity.audit.enums.DataCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * 민감 데이터 접근 감사 로그 Repository
 */
@Repository
public interface AuditSensitiveDataAccessLogRepository extends JpaRepository<AuditSensitiveDataAccessLogEntity, Long> {

    /**
     * 접근자로 조회
     */
    Page<AuditSensitiveDataAccessLogEntity> findByAccessedBy(String accessedBy, Pageable pageable);

    /**
     * 데이터 카테고리로 조회
     */
    Page<AuditSensitiveDataAccessLogEntity> findByDataCategory(DataCategory dataCategory, Pageable pageable);

    /**
     * 접근 타입으로 조회
     */
    Page<AuditSensitiveDataAccessLogEntity> findByAccessType(DataAccessType accessType, Pageable pageable);

    /**
     * 특정 엔티티 접근 이력 조회
     */
    Page<AuditSensitiveDataAccessLogEntity> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);

    /**
     * 기간별 조회
     */
    Page<AuditSensitiveDataAccessLogEntity> findByCreatedAtBetween(Instant start, Instant end, Pageable pageable);

    /**
     * 접근자별 접근 횟수 통계
     */
    @Query("SELECT a.accessedBy, COUNT(a) FROM AuditSensitiveDataAccessLogEntity a " +
            "WHERE a.createdAt BETWEEN :start AND :end GROUP BY a.accessedBy ORDER BY COUNT(a) DESC")
    List<Object[]> countByAccessedByBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * 데이터 카테고리별 접근 통계
     */
    @Query("SELECT a.dataCategory, COUNT(a) FROM AuditSensitiveDataAccessLogEntity a " +
            "WHERE a.createdAt BETWEEN :start AND :end GROUP BY a.dataCategory ORDER BY COUNT(a) DESC")
    List<Object[]> countByDataCategoryBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * 비정상적인 대량 접근 탐지
     */
    @Query("SELECT a.accessedBy, a.dataCategory, COUNT(a) FROM AuditSensitiveDataAccessLogEntity a " +
            "WHERE a.createdAt >= :since GROUP BY a.accessedBy, a.dataCategory " +
            "HAVING COUNT(a) >= :threshold ORDER BY COUNT(a) DESC")
    List<Object[]> findAbnormalAccessPatterns(@Param("since") Instant since, @Param("threshold") long threshold);

    /**
     * 내보내기/다운로드 이력 조회
     */
    @Query("SELECT a FROM AuditSensitiveDataAccessLogEntity a " +
            "WHERE a.accessType IN ('EXPORT', 'DOWNLOAD') AND a.createdAt BETWEEN :start AND :end " +
            "ORDER BY a.createdAt DESC")
    Page<AuditSensitiveDataAccessLogEntity> findExportDownloadHistory(@Param("start") Instant start, @Param("end") Instant end, Pageable pageable);

    /**
     * 부서별 접근 통계
     */
    @Query("SELECT a.accessedByDepartment, a.dataCategory, COUNT(a) FROM AuditSensitiveDataAccessLogEntity a " +
            "WHERE a.createdAt BETWEEN :start AND :end AND a.accessedByDepartment IS NOT NULL " +
            "GROUP BY a.accessedByDepartment, a.dataCategory ORDER BY COUNT(a) DESC")
    List<Object[]> countByDepartmentAndCategoryBetween(@Param("start") Instant start, @Param("end") Instant end);
}
