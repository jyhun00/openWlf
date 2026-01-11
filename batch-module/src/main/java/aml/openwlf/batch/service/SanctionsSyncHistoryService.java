package aml.openwlf.batch.service;

import aml.openwlf.batch.dto.SanctionsSyncHistoryDto;
import aml.openwlf.batch.dto.SanctionsSyncStatusDto;
import aml.openwlf.batch.dto.SanctionsSyncStatusDto.SourceSyncStatus;
import aml.openwlf.data.entity.SanctionsSyncHistoryEntity;
import aml.openwlf.data.entity.SanctionsSyncHistoryEntity.SyncStatus;
import aml.openwlf.data.repository.SanctionsSyncHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 제재 리스트 동기화 이력 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SanctionsSyncHistoryService {

    private final SanctionsSyncHistoryRepository historyRepository;

    /**
     * 동기화 시작 이력 생성
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SanctionsSyncHistoryEntity startSync(String sourceFile) {
        SanctionsSyncHistoryEntity history = SanctionsSyncHistoryEntity.builder()
                .sourceFile(sourceFile)
                .status(SyncStatus.FAIL)  // 기본값은 FAIL, 성공 시 업데이트
                .startedAt(LocalDateTime.now())
                .insertCount(0)
                .updateCount(0)
                .unchangedCount(0)
                .deactivatedCount(0)
                .totalProcessed(0)
                .build();
        
        return historyRepository.save(history);
    }

    /**
     * 동기화 성공 이력 업데이트
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SanctionsSyncHistoryEntity completeSuccess(Long historyId, SyncResult result) {
        SanctionsSyncHistoryEntity history = historyRepository.findById(historyId)
                .orElseThrow(() -> new IllegalArgumentException("History not found: " + historyId));
        
        LocalDateTime now = LocalDateTime.now();
        long durationMs = java.time.Duration.between(history.getStartedAt(), now).toMillis();
        
        history.setStatus(SyncStatus.SUCCESS);
        history.setInsertCount(result.getInsertCount());
        history.setUpdateCount(result.getUpdateCount());
        history.setUnchangedCount(result.getUnchangedCount());
        history.setDeactivatedCount(result.getDeactivatedCount());
        history.setTotalProcessed(result.getTotalProcessed());
        history.setFinishedAt(now);
        history.setDurationMs(durationMs);
        history.setFileSizeBytes(result.getFileSizeBytes());
        history.setDescription(String.format("Successfully synchronized %d entries (Insert: %d, Update: %d, Unchanged: %d, Deactivated: %d)",
                result.getTotalProcessed(), result.getInsertCount(), result.getUpdateCount(),
                result.getUnchangedCount(), result.getDeactivatedCount()));
        
        return historyRepository.save(history);
    }

    /**
     * 동기화 실패 이력 업데이트
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SanctionsSyncHistoryEntity completeFail(Long historyId, Exception exception) {
        SanctionsSyncHistoryEntity history = historyRepository.findById(historyId)
                .orElseThrow(() -> new IllegalArgumentException("History not found: " + historyId));
        
        LocalDateTime now = LocalDateTime.now();
        long durationMs = java.time.Duration.between(history.getStartedAt(), now).toMillis();
        
        // 전체 스택트레이스를 문자열로 변환
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String fullStackTrace = sw.toString();
        
        history.setStatus(SyncStatus.FAIL);
        history.setFinishedAt(now);
        history.setDurationMs(durationMs);
        history.setDescription(fullStackTrace);
        
        return historyRepository.save(history);
    }

    /**
     * 동기화 실패 이력 업데이트 (에러 메시지만)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SanctionsSyncHistoryEntity completeFail(Long historyId, String errorMessage, String fullErrorLog) {
        SanctionsSyncHistoryEntity history = historyRepository.findById(historyId)
                .orElseThrow(() -> new IllegalArgumentException("History not found: " + historyId));
        
        LocalDateTime now = LocalDateTime.now();
        long durationMs = java.time.Duration.between(history.getStartedAt(), now).toMillis();
        
        history.setStatus(SyncStatus.FAIL);
        history.setFinishedAt(now);
        history.setDurationMs(durationMs);
        history.setDescription(fullErrorLog != null ? fullErrorLog : errorMessage);
        
        return historyRepository.save(history);
    }

    /**
     * 현재 동기화 상태 조회 (OFAC, UN 모두)
     */
    @Transactional(readOnly = true)
    public SanctionsSyncStatusDto getCurrentStatus() {
        return SanctionsSyncStatusDto.builder()
                .ofac(getSourceStatus("OFAC"))
                .un(getSourceStatus("UN"))
                .build();
    }

    /**
     * 특정 소스의 동기화 상태 조회
     */
    @Transactional(readOnly = true)
    public SourceSyncStatus getSourceStatus(String sourceFile) {
        LocalDateTime last24h = LocalDateTime.now().minusHours(24);
        
        Optional<SanctionsSyncHistoryEntity> lastSync = 
                historyRepository.findTopBySourceFileOrderByStartedAtDesc(sourceFile);
        
        Optional<SanctionsSyncHistoryEntity> lastSuccess = 
                historyRepository.findTopBySourceFileAndStatusOrderByStartedAtDesc(sourceFile, SyncStatus.SUCCESS);
        
        long failureCount = historyRepository.countFailuresSince(sourceFile, last24h);
        int consecutiveFailures = historyRepository.countConsecutiveFailures(sourceFile, 10);
        
        SourceSyncStatus.SourceSyncStatusBuilder builder = SourceSyncStatus.builder()
                .sourceFile(sourceFile)
                .failureCountLast24h((int) failureCount)
                .consecutiveFailures(consecutiveFailures)
                .healthy(consecutiveFailures == 0);
        
        lastSync.ifPresent(sync -> {
            builder.lastStatus(sync.getStatus().name())
                   .lastSyncAt(sync.getFinishedAt() != null ? sync.getFinishedAt() : sync.getStartedAt())
                   .lastTotalProcessed(sync.getTotalProcessed())
                   .lastDurationSeconds(sync.getDurationSeconds());
            
            if (sync.getStatus() == SyncStatus.FAIL) {
                // 에러 메시지 첫 줄만 요약으로 표시
                String desc = sync.getDescription();
                if (desc != null && !desc.isEmpty()) {
                    String firstLine = desc.split("\n")[0];
                    builder.lastErrorMessage(firstLine.length() > 200 ? 
                            firstLine.substring(0, 200) + "..." : firstLine);
                }
            }
        });
        
        lastSuccess.ifPresent(sync -> 
            builder.lastSuccessAt(sync.getFinishedAt())
        );
        
        return builder.build();
    }

    /**
     * 동기화 이력 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<SanctionsSyncHistoryDto> getHistory(String sourceFile, String status, Pageable pageable) {
        Page<SanctionsSyncHistoryEntity> entities;
        
        if (sourceFile != null && status != null) {
            entities = historyRepository.findBySourceFileAndStatusOrderByStartedAtDesc(
                    sourceFile, SyncStatus.valueOf(status), pageable);
        } else if (sourceFile != null) {
            entities = historyRepository.findBySourceFileOrderByStartedAtDesc(sourceFile, pageable);
        } else if (status != null) {
            entities = historyRepository.findByStatusOrderByStartedAtDesc(
                    SyncStatus.valueOf(status), pageable);
        } else {
            entities = historyRepository.findAllByOrderByStartedAtDesc(pageable);
        }
        
        return entities.map(this::toDto);
    }

    /**
     * 특정 이력 상세 조회
     */
    @Transactional(readOnly = true)
    public SanctionsSyncHistoryDto getHistoryDetail(Long historyId) {
        return historyRepository.findById(historyId)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("History not found: " + historyId));
    }

    /**
     * Entity -> DTO 변환
     */
    private SanctionsSyncHistoryDto toDto(SanctionsSyncHistoryEntity entity) {
        return SanctionsSyncHistoryDto.builder()
                .historyId(entity.getHistoryId())
                .sourceFile(entity.getSourceFile())
                .status(entity.getStatus().name())
                .insertCount(entity.getInsertCount())
                .updateCount(entity.getUpdateCount())
                .unchangedCount(entity.getUnchangedCount())
                .deactivatedCount(entity.getDeactivatedCount())
                .totalProcessed(entity.getTotalProcessed())
                .startedAt(entity.getStartedAt())
                .finishedAt(entity.getFinishedAt())
                .durationMs(entity.getDurationMs())
                .durationSeconds(entity.getDurationSeconds())
                .description(entity.getDescription())
                .fileSizeBytes(entity.getFileSizeBytes())
                .build();
    }
}
