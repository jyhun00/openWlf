package aml.openwlf.data.service;

import aml.openwlf.core.filtering.WatchlistProvider;
import aml.openwlf.core.normalization.NormalizationService;
import aml.openwlf.core.rule.WatchlistEntry;
import aml.openwlf.data.entity.WatchlistEntryEntity;
import aml.openwlf.data.repository.WatchlistEntryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Service for managing watchlist data with in-memory caching.
 * All active watchlist entries are loaded into memory at startup for fast filtering.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WatchlistDataService implements WatchlistProvider {
    
    private final WatchlistEntryRepository repository;
    private final NormalizationService normalizationService;
    private final ObjectMapper objectMapper;
    
    // In-memory cache for watchlist entries
    private final Map<Long, WatchlistEntry> watchlistCache = new ConcurrentHashMap<>();
    private final Map<String, List<WatchlistEntry>> sourceIndexCache = new ConcurrentHashMap<>();
    private volatile List<WatchlistEntry> allEntriesCache = new ArrayList<>();
    
    // Read-write lock for cache operations
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    
    // Cache metadata
    private volatile LocalDateTime lastCacheRefresh;
    private volatile long cacheSize;
    
    /**
     * Initialize cache on application startup
     */
    @PostConstruct
    public void initializeCache() {
        log.info("Initializing watchlist cache...");
        refreshCache();
        log.info("Watchlist cache initialized with {} entries", cacheSize);
    }
    
    /**
     * Refresh the entire cache from database
     */
    @Transactional(readOnly = true)
    public void refreshCache() {
        cacheLock.writeLock().lock();
        try {
            log.info("Refreshing watchlist cache from database...");
            long startTime = System.currentTimeMillis();
            
            // Clear existing cache
            watchlistCache.clear();
            sourceIndexCache.clear();
            
            // Load all active entries from database
            List<WatchlistEntryEntity> entities = repository.findByIsActiveTrue();
            
            // Build cache
            List<WatchlistEntry> allEntries = new ArrayList<>(entities.size());
            Map<String, List<WatchlistEntry>> sourceIndex = new HashMap<>();
            
            for (WatchlistEntryEntity entity : entities) {
                WatchlistEntry entry = toWatchlistEntry(entity);
                watchlistCache.put(entity.getId(), entry);
                allEntries.add(entry);
                
                // Build source index
                sourceIndex.computeIfAbsent(entity.getListSource(), k -> new ArrayList<>())
                        .add(entry);
            }
            
            // Update source index cache
            sourceIndexCache.putAll(sourceIndex);
            
            // Update all entries cache (immutable copy for thread safety)
            allEntriesCache = Collections.unmodifiableList(allEntries);
            
            // Update metadata
            cacheSize = allEntries.size();
            lastCacheRefresh = LocalDateTime.now();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Watchlist cache refreshed: {} entries loaded in {}ms", cacheSize, duration);
            
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Get all watchlist entries from cache (used for filtering)
     */
    @Override
    public List<WatchlistEntry> getAllEntries() {
        cacheLock.readLock().lock();
        try {
            return allEntriesCache;
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    /**
     * Get watchlist entries by source from cache
     */
    @Override
    public List<WatchlistEntry> getEntriesBySource(String source) {
        cacheLock.readLock().lock();
        try {
            return sourceIndexCache.getOrDefault(source, Collections.emptyList());
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    /**
     * Get a specific entry from cache by ID
     */
    public Optional<WatchlistEntry> getEntryFromCache(Long id) {
        cacheLock.readLock().lock();
        try {
            return Optional.ofNullable(watchlistCache.get(id));
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    /**
     * Get cache statistics
     */
    public CacheStats getCacheStats() {
        cacheLock.readLock().lock();
        try {
            return CacheStats.builder()
                    .totalEntries(cacheSize)
                    .sourceCount(sourceIndexCache.size())
                    .lastRefresh(lastCacheRefresh)
                    .sources(new ArrayList<>(sourceIndexCache.keySet()))
                    .build();
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    /**
     * Get all watchlist entries with pagination (from database)
     */
    @Transactional(readOnly = true)
    public Page<WatchlistEntryEntity> getAllEntries(Pageable pageable) {
        return repository.findAll(pageable);
    }
    
    /**
     * Get watchlist entry by ID (from database)
     */
    @Transactional(readOnly = true)
    public Optional<WatchlistEntryEntity> getEntryById(Long id) {
        return repository.findById(id);
    }
    
    /**
     * Search watchlist entries with filters and pagination (from database)
     */
    @Transactional(readOnly = true)
    public Page<WatchlistEntryEntity> searchEntries(
            String searchTerm, 
            String listSource, 
            Boolean isActive, 
            Pageable pageable) {
        
        if (searchTerm != null && !searchTerm.isBlank()) {
            String normalizedSearchTerm = normalizationService.normalizeName(searchTerm);
            
            if (listSource != null && !listSource.isBlank()) {
                return repository.searchByNameOrAliasWithFilters(
                        normalizedSearchTerm, listSource, isActive, pageable);
            } else {
                return repository.searchByNameOrAliasWithActiveFilter(
                        normalizedSearchTerm, isActive, pageable);
            }
        }
        
        if (listSource != null && !listSource.isBlank()) {
            if (isActive != null) {
                return repository.findByListSourceAndIsActive(listSource, isActive, pageable);
            }
            return repository.findByListSource(listSource, pageable);
        }
        
        if (isActive != null) {
            return repository.findByIsActive(isActive, pageable);
        }
        
        return repository.findAll(pageable);
    }
    
    /**
     * Get statistics about watchlist entries
     */
    @Transactional(readOnly = true)
    public WatchlistStats getStatistics() {
        long totalCount = repository.count();
        long activeCount = repository.countByIsActiveTrue();
        
        return WatchlistStats.builder()
                .totalEntries(totalCount)
                .activeEntries(activeCount)
                .inactiveEntries(totalCount - activeCount)
                .cachedEntries(cacheSize)
                .lastCacheRefresh(lastCacheRefresh)
                .build();
    }
    
    /**
     * Save entry and update cache
     */
    @Transactional
    public WatchlistEntryEntity saveEntry(WatchlistEntryEntity entity) {
        entity.setNormalizedName(normalizationService.normalizeName(entity.getName()));
        WatchlistEntryEntity saved = repository.save(entity);
        
        // Update cache if entry is active
        if (Boolean.TRUE.equals(saved.getIsActive())) {
            updateCacheEntry(saved);
        } else {
            removeCacheEntry(saved.getId());
        }
        
        return saved;
    }
    
    /**
     * Save multiple entries and refresh cache
     */
    @Transactional
    public void saveAll(List<WatchlistEntryEntity> entities) {
        entities.forEach(entity -> 
                entity.setNormalizedName(normalizationService.normalizeName(entity.getName())));
        repository.saveAll(entities);
        
        // Refresh entire cache for bulk operations
        refreshCache();
    }
    
    /**
     * Delete entry and update cache
     */
    @Transactional
    public void deleteEntry(Long id) {
        repository.deleteById(id);
        removeCacheEntry(id);
    }
    
    /**
     * Update a single entry in cache
     */
    private void updateCacheEntry(WatchlistEntryEntity entity) {
        cacheLock.writeLock().lock();
        try {
            WatchlistEntry entry = toWatchlistEntry(entity);
            WatchlistEntry oldEntry = watchlistCache.put(entity.getId(), entry);
            
            // Rebuild all entries list
            List<WatchlistEntry> newAllEntries = new ArrayList<>(watchlistCache.values());
            allEntriesCache = Collections.unmodifiableList(newAllEntries);
            
            // Update source index
            if (oldEntry != null && !oldEntry.getListSource().equals(entry.getListSource())) {
                // Source changed - remove from old source index
                List<WatchlistEntry> oldSourceList = sourceIndexCache.get(oldEntry.getListSource());
                if (oldSourceList != null) {
                    oldSourceList.removeIf(e -> e.getId().equals(entity.getId()));
                }
            }
            
            // Add to current source index
            sourceIndexCache.computeIfAbsent(entity.getListSource(), k -> new ArrayList<>())
                    .removeIf(e -> e.getId().equals(entity.getId()));
            sourceIndexCache.get(entity.getListSource()).add(entry);
            
            cacheSize = watchlistCache.size();
            
            log.debug("Cache updated for entry ID: {}", entity.getId());
            
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Remove an entry from cache
     */
    private void removeCacheEntry(Long id) {
        cacheLock.writeLock().lock();
        try {
            WatchlistEntry removed = watchlistCache.remove(id);
            
            if (removed != null) {
                // Rebuild all entries list
                List<WatchlistEntry> newAllEntries = new ArrayList<>(watchlistCache.values());
                allEntriesCache = Collections.unmodifiableList(newAllEntries);
                
                // Update source index
                List<WatchlistEntry> sourceList = sourceIndexCache.get(removed.getListSource());
                if (sourceList != null) {
                    sourceList.removeIf(e -> e.getId().equals(id));
                }
                
                cacheSize = watchlistCache.size();
                
                log.debug("Cache entry removed for ID: {}", id);
            }
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    public List<String> parseAliases(WatchlistEntryEntity entity) {
        List<String> aliases = new ArrayList<>();
        if (entity.getAliases() != null && !entity.getAliases().isEmpty()) {
            try {
                aliases = objectMapper.readValue(entity.getAliases(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                aliases = Arrays.asList(entity.getAliases().split(","));
            }
        }
        return aliases;
    }
    
    private WatchlistEntry toWatchlistEntry(WatchlistEntryEntity entity) {
        return WatchlistEntry.builder()
                .id(entity.getId())
                .name(entity.getName())
                .aliases(parseAliases(entity))
                .dateOfBirth(entity.getDateOfBirth())
                .nationality(entity.getNationality())
                .listSource(entity.getListSource())
                .entryType(entity.getEntryType())
                .build();
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class WatchlistStats {
        private long totalEntries;
        private long activeEntries;
        private long inactiveEntries;
        private long cachedEntries;
        private LocalDateTime lastCacheRefresh;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CacheStats {
        private long totalEntries;
        private int sourceCount;
        private LocalDateTime lastRefresh;
        private List<String> sources;
    }
}
