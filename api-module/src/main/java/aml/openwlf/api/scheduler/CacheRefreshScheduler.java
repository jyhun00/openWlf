package aml.openwlf.api.scheduler;

import aml.openwlf.data.service.WatchlistDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 인메모리 캐시 자동 갱신 스케줄러
 * 
 * 기본 설정: 매일 새벽 2시에 캐시 갱신
 * application.yml에서 시간 변경 가능:
 *   cache:
 *     refresh:
 *       cron: "0 0 2 * * ?"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheRefreshScheduler {
    
    private final WatchlistDataService watchlistDataService;
    
    @Value("${cache.refresh.enabled:true}")
    private boolean refreshEnabled;

    @Scheduled(cron = "${cache.refresh.cron:0 0 2 * * ?}")
    public void refreshCacheDaily() {
        if (!refreshEnabled) {
            log.info("캐시 자동 갱신이 비활성화되어 있습니다.");
            return;
        }
        
        String startTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log.info("========================================");
        log.info("스케줄된 캐시 갱신 시작: {}", startTime);
        log.info("========================================");
        
        try {
            // 갱신 전 상태
            WatchlistDataService.CacheStats beforeStats = watchlistDataService.getCacheStats();
            log.info("갱신 전 캐시 상태 - entries: {}, sources: {}", 
                    beforeStats.getTotalEntries(), beforeStats.getSourceCount());
            
            // 캐시 갱신 실행
            watchlistDataService.refreshCache();
            
            // 갱신 후 상태
            WatchlistDataService.CacheStats afterStats = watchlistDataService.getCacheStats();
            log.info("갱신 후 캐시 상태 - entries: {}, sources: {}", 
                    afterStats.getTotalEntries(), afterStats.getSourceCount());
            
            long diff = afterStats.getTotalEntries() - beforeStats.getTotalEntries();
            if (diff != 0) {
                log.info("엔트리 변화: {} ({}{})", 
                        afterStats.getTotalEntries(), 
                        diff > 0 ? "+" : "", 
                        diff);
            }
            
            log.info("========================================");
            log.info("스케줄된 캐시 갱신 완료");
            log.info("========================================");
            
        } catch (Exception e) {
            log.error("========================================");
            log.error("스케줄된 캐시 갱신 실패!", e);
            log.error("========================================");
        }
    }
    
    /**
     * 애플리케이션 상태 확인용 (선택적)
     * 매 시간 정각에 캐시 상태 로깅
     */
    @Scheduled(cron = "${cache.status-log.cron:0 0 * * * ?}")
    public void logCacheStatus() {
        if (!refreshEnabled) {
            return;
        }
        
        try {
            WatchlistDataService.CacheStats stats = watchlistDataService.getCacheStats();
            log.debug("캐시 상태 - entries: {}, lastRefresh: {}", 
                    stats.getTotalEntries(), 
                    stats.getLastRefresh());
        } catch (Exception e) {
            log.warn("캐시 상태 확인 실패: {}", e.getMessage());
        }
    }
}
