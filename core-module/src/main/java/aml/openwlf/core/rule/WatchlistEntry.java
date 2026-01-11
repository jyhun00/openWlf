package aml.openwlf.core.rule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Watchlist entry for matching
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistEntry {
    private Long id;
    private String name;
    private List<String> aliases;
    private LocalDate dateOfBirth;
    private String nationality;
    private String listSource; // OFAC, UN, EU, etc.
    private String entryType; // INDIVIDUAL, ENTITY
}
