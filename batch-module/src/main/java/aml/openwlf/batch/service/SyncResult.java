package aml.openwlf.batch.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 제재 리스트 동기화 결과
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncResult {

    private String sourceFile;
    private boolean success;
    private int insertCount;
    private int updateCount;
    private int unchangedCount;
    private int deactivatedCount;
    private String errorMessage;
    private String fullErrorLog;  // 전체 에러 스택트레이스
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private Long fileSizeBytes;

    public static SyncResult success(String sourceFile, int insertCount, int updateCount,
                                      int unchangedCount, int deactivatedCount) {
        return SyncResult.builder()
                .sourceFile(sourceFile)
                .success(true)
                .insertCount(insertCount)
                .updateCount(updateCount)
                .unchangedCount(unchangedCount)
                .deactivatedCount(deactivatedCount)
                .endTime(LocalDateTime.now())
                .build();
    }

    public static SyncResult failed(String sourceFile, String errorMessage, String fullErrorLog) {
        return SyncResult.builder()
                .sourceFile(sourceFile)
                .success(false)
                .errorMessage(errorMessage)
                .fullErrorLog(fullErrorLog)
                .endTime(LocalDateTime.now())
                .build();
    }

    public static SyncResult failed(String sourceFile, String errorMessage) {
        return failed(sourceFile, errorMessage, null);
    }

    public int getTotalProcessed() {
        return insertCount + updateCount + unchangedCount;
    }

    @Override
    public String toString() {
        if (success) {
            return String.format("SyncResult[%s: SUCCESS - Insert=%d, Update=%d, Unchanged=%d, Deactivated=%d, Duration=%dms]",
                    sourceFile, insertCount, updateCount, unchangedCount, deactivatedCount, durationMs);
        } else {
            return String.format("SyncResult[%s: FAILED - %s]", sourceFile, errorMessage);
        }
    }
}
