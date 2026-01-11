package aml.openwlf.api.init;

import aml.openwlf.data.entity.WatchlistEntryEntity;
import aml.openwlf.data.service.WatchlistDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Initialize sample watchlist data on startup
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SampleDataInitializer implements CommandLineRunner {
    
    private final WatchlistDataService watchlistDataService;
    private final ObjectMapper objectMapper;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing sample watchlist data...");
        
        List<WatchlistEntryEntity> sampleEntries = new ArrayList<>();
        
        // OFAC SDN List samples
        sampleEntries.add(createEntry(
                "VLADIMIR PUTIN",
                List.of("PUTIN VLADIMIR", "V. PUTIN"),
                LocalDate.of(1952, 10, 7),
                "RU",
                "OFAC",
                "INDIVIDUAL"
        ));
        
        sampleEntries.add(createEntry(
                "KIM JONG UN",
                List.of("KIM JONG-UN", "SUPREME LEADER KIM"),
                LocalDate.of(1984, 1, 8),
                "KP",
                "OFAC",
                "INDIVIDUAL"
        ));
        
        // UN Sanctions List samples
        sampleEntries.add(createEntry(
                "OSAMA BIN LADEN",
                List.of("USAMA BIN LADEN", "BIN LADEN OSAMA"),
                LocalDate.of(1957, 3, 10),
                "SA",
                "UN",
                "INDIVIDUAL"
        ));
        
        // EU Sanctions List samples
        sampleEntries.add(createEntry(
                "BASHAR AL-ASSAD",
                List.of("ASSAD BASHAR", "BASHAR HAFEZ AL-ASSAD"),
                LocalDate.of(1965, 9, 11),
                "SY",
                "EU",
                "INDIVIDUAL"
        ));
        
        // Fictional test entries
        sampleEntries.add(createEntry(
                "JOHN SMITH",
                List.of("J. SMITH", "SMITH JOHN"),
                LocalDate.of(1975, 5, 15),
                "US",
                "TEST",
                "INDIVIDUAL"
        ));
        
        sampleEntries.add(createEntry(
                "ACME CORPORATION",
                List.of("ACME CORP", "ACME INC"),
                null,
                "US",
                "OFAC",
                "ENTITY"
        ));
        
        sampleEntries.add(createEntry(
                "MARIA GARCIA",
                List.of("M. GARCIA", "GARCIA MARIA"),
                LocalDate.of(1980, 12, 25),
                "MX",
                "TEST",
                "INDIVIDUAL"
        ));
        
        sampleEntries.add(createEntry(
                "LI MING",
                List.of("MING LI", "LI M."),
                LocalDate.of(1985, 3, 20),
                "CN",
                "TEST",
                "INDIVIDUAL"
        ));
        
        watchlistDataService.saveAll(sampleEntries);
        
        log.info("Sample data initialization completed. Added {} entries.", sampleEntries.size());
    }
    
    private WatchlistEntryEntity createEntry(
            String name,
            List<String> aliases,
            LocalDate dob,
            String nationality,
            String source,
            String type) throws Exception {
        
        return WatchlistEntryEntity.builder()
                .name(name)
                .aliases(objectMapper.writeValueAsString(aliases))
                .dateOfBirth(dob)
                .nationality(nationality)
                .listSource(source)
                .entryType(type)
                .isActive(true)
                .versionDate(LocalDate.now())
                .build();
    }
}
