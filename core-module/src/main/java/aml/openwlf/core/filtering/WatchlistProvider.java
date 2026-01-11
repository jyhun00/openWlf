package aml.openwlf.core.filtering;

import aml.openwlf.core.rule.WatchlistEntry;

import java.util.List;

/**
 * Interface for providing watchlist entries
 */
public interface WatchlistProvider {
    
    /**
     * Get all watchlist entries
     */
    List<WatchlistEntry> getAllEntries();
    
    /**
     * Get watchlist entries by source
     */
    List<WatchlistEntry> getEntriesBySource(String source);
}
