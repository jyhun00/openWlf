package aml.openwlf.data.service;

import aml.openwlf.core.normalization.NormalizationService;
import aml.openwlf.core.rule.WatchlistEntry;
import aml.openwlf.data.entity.WatchlistEntryEntity;
import aml.openwlf.data.repository.WatchlistEntryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WatchlistDataService 테스트")
class WatchlistDataServiceTest {
    
    @Mock
    private WatchlistEntryRepository repository;
    
    @Mock
    private NormalizationService normalizationService;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @InjectMocks
    private WatchlistDataService watchlistDataService;
    
    @BeforeEach
    void setUp() {
        // Initialize with empty cache
        when(repository.findByIsActiveTrue()).thenReturn(Collections.emptyList());
        watchlistDataService.initializeCache();
    }
    
    @Nested
    @DisplayName("캐시 초기화 테스트")
    class CacheInitializationTest {
        
        @Test
        @DisplayName("시작 시 활성화된 항목만 캐시에 로드")
        void shouldLoadOnlyActiveEntriesOnInit() {
            // given
            List<WatchlistEntryEntity> activeEntries = List.of(
                    createWatchlistEntity(1L, "John Smith", "OFAC", true),
                    createWatchlistEntity(2L, "Jane Doe", "UN", true)
            );
            when(repository.findByIsActiveTrue()).thenReturn(activeEntries);
            
            // when
            watchlistDataService.refreshCache();
            
            // then
            List<WatchlistEntry> cachedEntries = watchlistDataService.getAllEntries();
            assertThat(cachedEntries).hasSize(2);
        }
        
        @Test
        @DisplayName("캐시 통계 조회")
        void shouldReturnCacheStatistics() {
            // given
            List<WatchlistEntryEntity> entries = List.of(
                    createWatchlistEntity(1L, "Person A", "OFAC", true),
                    createWatchlistEntity(2L, "Person B", "OFAC", true),
                    createWatchlistEntity(3L, "Person C", "UN", true)
            );
            when(repository.findByIsActiveTrue()).thenReturn(entries);
            watchlistDataService.refreshCache();
            
            // when
            WatchlistDataService.CacheStats stats = watchlistDataService.getCacheStats();
            
            // then
            assertThat(stats.getTotalEntries()).isEqualTo(3);
            assertThat(stats.getSourceCount()).isEqualTo(2); // OFAC, UN
            assertThat(stats.getSources()).containsExactlyInAnyOrder("OFAC", "UN");
            assertThat(stats.getLastRefresh()).isNotNull();
        }
    }
    
    @Nested
    @DisplayName("WatchlistProvider 인터페이스 테스트")
    class WatchlistProviderTest {
        
        @Test
        @DisplayName("getAllEntries - 모든 항목 반환")
        void shouldGetAllEntries() {
            // given
            List<WatchlistEntryEntity> entries = List.of(
                    createWatchlistEntity(1L, "Person A", "OFAC", true),
                    createWatchlistEntity(2L, "Person B", "UN", true)
            );
            when(repository.findByIsActiveTrue()).thenReturn(entries);
            watchlistDataService.refreshCache();
            
            // when
            List<WatchlistEntry> result = watchlistDataService.getAllEntries();
            
            // then
            assertThat(result).hasSize(2);
        }
        
        @Test
        @DisplayName("getEntriesBySource - 소스별 항목 반환")
        void shouldGetEntriesBySource() {
            // given
            List<WatchlistEntryEntity> entries = List.of(
                    createWatchlistEntity(1L, "Person A", "OFAC", true),
                    createWatchlistEntity(2L, "Person B", "OFAC", true),
                    createWatchlistEntity(3L, "Person C", "UN", true)
            );
            when(repository.findByIsActiveTrue()).thenReturn(entries);
            watchlistDataService.refreshCache();
            
            // when
            List<WatchlistEntry> ofacEntries = watchlistDataService.getEntriesBySource("OFAC");
            List<WatchlistEntry> unEntries = watchlistDataService.getEntriesBySource("UN");
            List<WatchlistEntry> euEntries = watchlistDataService.getEntriesBySource("EU");
            
            // then
            assertThat(ofacEntries).hasSize(2);
            assertThat(unEntries).hasSize(1);
            assertThat(euEntries).isEmpty();
        }
        
        @Test
        @DisplayName("존재하지 않는 소스 조회 시 빈 리스트 반환")
        void shouldReturnEmptyListForNonExistentSource() {
            // when
            List<WatchlistEntry> result = watchlistDataService.getEntriesBySource("INVALID");
            
            // then
            assertThat(result).isEmpty();
        }
    }
    
    @Nested
    @DisplayName("데이터베이스 조회 테스트")
    class DatabaseQueryTest {
        
        @Test
        @DisplayName("ID로 항목 조회")
        void shouldGetEntryById() {
            // given
            WatchlistEntryEntity entity = createWatchlistEntity(1L, "John Smith", "OFAC", true);
            when(repository.findById(1L)).thenReturn(Optional.of(entity));
            
            // when
            Optional<WatchlistEntryEntity> result = watchlistDataService.getEntryById(1L);
            
            // then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("John Smith");
        }
        
        @Test
        @DisplayName("페이지네이션으로 전체 항목 조회")
        void shouldGetAllEntriesWithPagination() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<WatchlistEntryEntity> entities = List.of(
                    createWatchlistEntity(1L, "Person A", "OFAC", true)
            );
            Page<WatchlistEntryEntity> page = new PageImpl<>(entities, pageable, 1);
            when(repository.findAll(pageable)).thenReturn(page);
            
            // when
            Page<WatchlistEntryEntity> result = watchlistDataService.getAllEntries(pageable);
            
            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
        
        @Test
        @DisplayName("검색어로 항목 검색")
        void shouldSearchEntriesByTerm() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            String searchTerm = "John";
            when(normalizationService.normalizeName(searchTerm)).thenReturn("JOHN");
            
            List<WatchlistEntryEntity> entities = List.of(
                    createWatchlistEntity(1L, "John Smith", "OFAC", true)
            );
            Page<WatchlistEntryEntity> page = new PageImpl<>(entities, pageable, 1);
            when(repository.searchByNameOrAliasWithActiveFilter(eq("JOHN"), eq(null), eq(pageable)))
                    .thenReturn(page);
            
            // when
            Page<WatchlistEntryEntity> result = watchlistDataService.searchEntries(
                    searchTerm, null, null, pageable);
            
            // then
            assertThat(result.getContent()).hasSize(1);
        }
    }
    
    @Nested
    @DisplayName("항목 저장 테스트")
    class SaveEntryTest {
        
        @Test
        @DisplayName("새 항목 저장 시 정규화된 이름 설정")
        void shouldNormalizeNameOnSave() {
            // given
            WatchlistEntryEntity entity = createWatchlistEntity(null, "John Smith", "OFAC", true);
            entity.setId(1L); // ID가 있어야 캐시 업데이트 가능
            when(normalizationService.normalizeName("John Smith")).thenReturn("JOHN SMITH");
            when(repository.save(any())).thenReturn(entity);

            // when
            WatchlistEntryEntity result = watchlistDataService.saveEntry(entity);

            // then
            assertThat(result.getNormalizedName()).isEqualTo("JOHN SMITH");
            verify(normalizationService).normalizeName("John Smith");
        }

        @Test
        @DisplayName("활성화된 항목 저장 시 캐시 업데이트")
        void shouldUpdateCacheWhenSavingActiveEntry() {
            // given
            WatchlistEntryEntity entity = createWatchlistEntity(1L, "John Smith", "OFAC", true);
            when(normalizationService.normalizeName(any())).thenReturn("JOHN SMITH");
            when(repository.save(any())).thenReturn(entity);
            
            // when
            watchlistDataService.saveEntry(entity);
            
            // then
            Optional<WatchlistEntry> cached = watchlistDataService.getEntryFromCache(1L);
            assertThat(cached).isPresent();
        }
        
        @Test
        @DisplayName("비활성화된 항목 저장 시 캐시에서 제거")
        void shouldRemoveFromCacheWhenSavingInactiveEntry() {
            // given - 먼저 활성 항목을 캐시에 로드
            WatchlistEntryEntity activeEntity = createWatchlistEntity(1L, "John Smith", "OFAC", true);
            when(repository.findByIsActiveTrue()).thenReturn(List.of(activeEntity));
            watchlistDataService.refreshCache();
            
            // 비활성화 후 저장
            WatchlistEntryEntity inactiveEntity = createWatchlistEntity(1L, "John Smith", "OFAC", false);
            when(normalizationService.normalizeName(any())).thenReturn("JOHN SMITH");
            when(repository.save(any())).thenReturn(inactiveEntity);
            
            // when
            watchlistDataService.saveEntry(inactiveEntity);
            
            // then
            Optional<WatchlistEntry> cached = watchlistDataService.getEntryFromCache(1L);
            assertThat(cached).isEmpty();
        }
    }
    
    @Nested
    @DisplayName("항목 삭제 테스트")
    class DeleteEntryTest {
        
        @Test
        @DisplayName("항목 삭제 시 캐시에서도 제거")
        void shouldRemoveFromCacheOnDelete() {
            // given - 먼저 항목을 캐시에 로드
            WatchlistEntryEntity entity = createWatchlistEntity(1L, "John Smith", "OFAC", true);
            when(repository.findByIsActiveTrue()).thenReturn(List.of(entity));
            watchlistDataService.refreshCache();
            
            // 캐시에 존재 확인
            assertThat(watchlistDataService.getEntryFromCache(1L)).isPresent();
            
            // when
            watchlistDataService.deleteEntry(1L);
            
            // then
            verify(repository).deleteById(1L);
            assertThat(watchlistDataService.getEntryFromCache(1L)).isEmpty();
        }
    }
    
    @Nested
    @DisplayName("통계 조회 테스트")
    class StatisticsTest {
        
        @Test
        @DisplayName("감시목록 통계 조회")
        void shouldGetStatistics() {
            // given
            when(repository.count()).thenReturn(100L);
            when(repository.countByIsActiveTrue()).thenReturn(80L);
            
            // when
            WatchlistDataService.WatchlistStats stats = watchlistDataService.getStatistics();
            
            // then
            assertThat(stats.getTotalEntries()).isEqualTo(100);
            assertThat(stats.getActiveEntries()).isEqualTo(80);
            assertThat(stats.getInactiveEntries()).isEqualTo(20);
        }
    }
    
    @Nested
    @DisplayName("Alias 파싱 테스트")
    class AliasParsingTest {
        
        @Test
        @DisplayName("JSON 형식 alias 파싱")
        void shouldParseJsonAliases() throws Exception {
            // given
            WatchlistEntryEntity entity = createWatchlistEntity(1L, "John Smith", "OFAC", true);
            entity.setAliases("[\"Johnny\", \"J. Smith\"]");
            
            when(objectMapper.readValue(eq("[\"Johnny\", \"J. Smith\"]"), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                    .thenReturn(Arrays.asList("Johnny", "J. Smith"));
            
            // when
            List<String> aliases = watchlistDataService.parseAliases(entity);
            
            // then
            assertThat(aliases).containsExactly("Johnny", "J. Smith");
        }
        
        @Test
        @DisplayName("콤마 구분 형식 alias 파싱 (JSON 실패 시)")
        void shouldParseCommaSeparatedAliasesWhenJsonFails() throws Exception {
            // given
            WatchlistEntryEntity entity = createWatchlistEntity(1L, "John Smith", "OFAC", true);
            entity.setAliases("Johnny,J. Smith");
            
            when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                    .thenThrow(new RuntimeException("Parse error"));
            
            // when
            List<String> aliases = watchlistDataService.parseAliases(entity);
            
            // then
            assertThat(aliases).containsExactly("Johnny", "J. Smith");
        }
        
        @Test
        @DisplayName("null alias 처리")
        void shouldHandleNullAliases() {
            // given
            WatchlistEntryEntity entity = createWatchlistEntity(1L, "John Smith", "OFAC", true);
            entity.setAliases(null);
            
            // when
            List<String> aliases = watchlistDataService.parseAliases(entity);
            
            // then
            assertThat(aliases).isEmpty();
        }
        
        @Test
        @DisplayName("빈 alias 처리")
        void shouldHandleEmptyAliases() {
            // given
            WatchlistEntryEntity entity = createWatchlistEntity(1L, "John Smith", "OFAC", true);
            entity.setAliases("");
            
            // when
            List<String> aliases = watchlistDataService.parseAliases(entity);
            
            // then
            assertThat(aliases).isEmpty();
        }
    }
    
    private WatchlistEntryEntity createWatchlistEntity(Long id, String name, String source, boolean active) {
        return WatchlistEntryEntity.builder()
                .id(id)
                .name(name)
                .normalizedName(name.toUpperCase())
                .listSource(source)
                .entryType("INDIVIDUAL")
                .isActive(active)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
