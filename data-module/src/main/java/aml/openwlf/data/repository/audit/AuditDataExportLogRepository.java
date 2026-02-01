package aml.openwlf.data.repository.audit;

import aml.openwlf.data.entity.audit.AuditDataExportLogEntity;
import aml.openwlf.data.entity.audit.enums.ExportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * 데이터 내보내기 감사 로그 Repository
 */
@Repository
public interface AuditDataExportLogRepository extends JpaRepository<AuditDataExportLogEntity, Long> {

    /**
     * 수행자로 조회
     */
    Page<AuditDataExportLogEntity> findByExportedBy(String exportedBy, Pageable pageable);

    /**
     * 내보내기 타입으로 조회
     */
    Page<AuditDataExportLogEntity> findByExportType(ExportType exportType, Pageable pageable);

    /**
     * 데이터 타입으로 조회
     */
    Page<AuditDataExportLogEntity> findByDataType(String dataType, Pageable pageable);

    /**
     * 기간별 조회
     */
    Page<AuditDataExportLogEntity> findByCreatedAtBetween(Instant start, Instant end, Pageable pageable);

    /**
     * 승인자가 필요한 내보내기 조회
     */
    Page<AuditDataExportLogEntity> findByAuthorizedByIsNotNull(Pageable pageable);

    /**
     * 내보내기 타입별 통계
     */
    @Query("SELECT a.exportType, COUNT(a), SUM(a.recordCount) FROM AuditDataExportLogEntity a " +
            "WHERE a.createdAt BETWEEN :start AND :end GROUP BY a.exportType")
    List<Object[]> countByExportTypeBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * 데이터 타입별 내보내기 통계
     */
    @Query("SELECT a.dataType, COUNT(a), SUM(a.recordCount), SUM(a.fileSizeBytes) FROM AuditDataExportLogEntity a " +
            "WHERE a.createdAt BETWEEN :start AND :end GROUP BY a.dataType")
    List<Object[]> getExportStatisticsByDataType(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * 수행자별 내보내기 통계
     */
    @Query("SELECT a.exportedBy, a.exportedByDepartment, COUNT(a), SUM(a.recordCount) FROM AuditDataExportLogEntity a " +
            "WHERE a.createdAt BETWEEN :start AND :end GROUP BY a.exportedBy, a.exportedByDepartment ORDER BY COUNT(a) DESC")
    List<Object[]> getExportStatisticsByUser(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * 대용량 내보내기 조회 (레코드 수 기준)
     */
    @Query("SELECT a FROM AuditDataExportLogEntity a WHERE a.recordCount >= :threshold ORDER BY a.createdAt DESC")
    Page<AuditDataExportLogEntity> findLargeExports(@Param("threshold") int threshold, Pageable pageable);

    /**
     * 일별 내보내기 추이
     */
    @Query("SELECT FUNCTION('DATE', a.createdAt), COUNT(a), SUM(a.recordCount) " +
            "FROM AuditDataExportLogEntity a WHERE a.createdAt BETWEEN :start AND :end " +
            "GROUP BY FUNCTION('DATE', a.createdAt) ORDER BY FUNCTION('DATE', a.createdAt)")
    List<Object[]> getDailyExportTrend(@Param("start") Instant start, @Param("end") Instant end);
}
