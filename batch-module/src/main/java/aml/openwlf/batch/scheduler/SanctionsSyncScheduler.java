package aml.openwlf.batch.scheduler;

import aml.openwlf.batch.service.SanctionsSyncService;
import aml.openwlf.batch.service.SyncResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 제재 리스트 동기화 스케줄러
 * 
 * 매일 새벽 2시에 OFAC, UN 제재 리스트를 다운로드하여 DB와 동기화합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SanctionsSyncScheduler {

    private final SanctionsSyncService sanctionsSyncService;

    /**
     * 매일 새벽 2시에 전체 제재 리스트 동기화 실행
     * 
     * cron: 초 분 시 일 월 요일
     * "0 0 2 * * *" = 매일 02:00:00
     */
    @Scheduled(cron = "${sanctions.sync.cron:0 0 2 * * *}")
    public void scheduledSyncAll() {
        log.info("========== Starting scheduled sanctions list synchronization ==========");
        
        long startTime = System.currentTimeMillis();
        
        try {
            List<SyncResult> results = sanctionsSyncService.syncAll();
            
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("========== Scheduled sync completed in {} ms ==========", duration);
            for (SyncResult result : results) {
                if (result.isSuccess()) {
                    log.info("  {} - Insert: {}, Update: {}, Unchanged: {}, Deactivated: {}",
                            result.getSourceFile(),
                            result.getInsertCount(),
                            result.getUpdateCount(),
                            result.getUnchangedCount(),
                            result.getDeactivatedCount());
                } else {
                    log.error("  {} - FAILED: {}", result.getSourceFile(), result.getErrorMessage());
                }
            }
        } catch (Exception e) {
            log.error("Scheduled sync failed with unexpected error", e);
        }
    }

    /**
     * OFAC만 별도로 동기화 (필요 시 수동 호출용)
     */
    public SyncResult syncOfacManually() {
        log.info("Manual OFAC sync requested");
        return sanctionsSyncService.syncOfac();
    }

    /**
     * UN만 별도로 동기화 (필요 시 수동 호출용)
     */
    public SyncResult syncUnManually() {
        log.info("Manual UN sync requested");
        return sanctionsSyncService.syncUn();
    }
}
