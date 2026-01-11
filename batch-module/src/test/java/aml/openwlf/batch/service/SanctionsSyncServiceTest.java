package aml.openwlf.batch.service;

import aml.openwlf.batch.config.SanctionsDownloadProperties;
import aml.openwlf.batch.parser.EuXmlParser;
import aml.openwlf.batch.parser.OfacXmlParser;
import aml.openwlf.batch.parser.UnXmlParser;
import aml.openwlf.batch.parser.model.ParsedSanctionsData;
import aml.openwlf.core.normalization.NormalizationService;
import aml.openwlf.data.entity.SanctionsEntity;
import aml.openwlf.data.entity.SanctionsSyncHistoryEntity;
import aml.openwlf.data.repository.SanctionsEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SanctionsSyncService 테스트")
class SanctionsSyncServiceTest {

    @Mock
    private SanctionsDownloadService downloadService;

    @Mock
    private OfacXmlParser ofacXmlParser;

    @Mock
    private UnXmlParser unXmlParser;

    @Mock
    private EuXmlParser euXmlParser;

    @Mock
    private SanctionsEntityRepository sanctionsRepository;

    @Mock
    private NormalizationService normalizationService;

    @Mock
    private SanctionsSyncHistoryService historyService;

    private SanctionsDownloadProperties properties;
    private SanctionsSyncService syncService;

    @BeforeEach
    void setUp() {
        properties = new SanctionsDownloadProperties();
        properties.setBatchSize(100);
        
        syncService = new SanctionsSyncService(
                downloadService,
                ofacXmlParser,
                unXmlParser,
                euXmlParser,
                sanctionsRepository,
                normalizationService,
                properties,
                historyService
        );
    }

    @Nested
    @DisplayName("syncOfac() 메서드")
    class SyncOfac {

        @Test
        @DisplayName("OFAC 동기화 성공")
        void shouldSyncOfacSuccessfully() throws Exception {
            // given
            byte[] xmlData = "<Sanctions>test</Sanctions>".getBytes();
            List<ParsedSanctionsData> parsedData = createParsedDataList("OFAC", 3);
            SanctionsSyncHistoryEntity history = createHistoryEntity("OFAC");
            
            when(downloadService.downloadOfacXmlAsBytes()).thenReturn(xmlData);
            when(ofacXmlParser.parse(any(InputStream.class))).thenReturn(parsedData);
            when(historyService.startSync("OFAC")).thenReturn(history);
            when(sanctionsRepository.findBySourceFileAndIsActiveTrue("OFAC"))
                    .thenReturn(Collections.emptyList());
            when(normalizationService.normalizeName(anyString())).thenAnswer(inv -> inv.getArgument(0));
            when(sanctionsRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            // when
            SyncResult result = syncService.syncOfac();

            // then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getSourceFile()).isEqualTo("OFAC");
            assertThat(result.getInsertCount()).isEqualTo(3);
            
            verify(downloadService).downloadOfacXmlAsBytes();
            verify(ofacXmlParser).parse(any(InputStream.class));
            verify(historyService).completeSuccess(eq(history.getHistoryId()), any(SyncResult.class));
        }

        @Test
        @DisplayName("OFAC 다운로드 실패 시 에러 결과 반환")
        void shouldReturnFailResultWhenDownloadFails() {
            // given
            SanctionsSyncHistoryEntity history = createHistoryEntity("OFAC");
            when(historyService.startSync("OFAC")).thenReturn(history);
            when(downloadService.downloadOfacXmlAsBytes())
                    .thenThrow(new RuntimeException("Download failed"));

            // when
            SyncResult result = syncService.syncOfac();

            // then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getSourceFile()).isEqualTo("OFAC");
            assertThat(result.getErrorMessage()).contains("Download failed");
            
            verify(historyService).completeFail(eq(history.getHistoryId()), anyString(), anyString());
        }

        @Test
        @DisplayName("OFAC 파싱 실패 시 에러 결과 반환")
        void shouldReturnFailResultWhenParseFails() throws Exception {
            // given
            byte[] xmlData = "<Invalid>xml</Invalid>".getBytes();
            SanctionsSyncHistoryEntity history = createHistoryEntity("OFAC");
            
            when(historyService.startSync("OFAC")).thenReturn(history);
            when(downloadService.downloadOfacXmlAsBytes()).thenReturn(xmlData);
            when(ofacXmlParser.parse(any(InputStream.class)))
                    .thenThrow(new RuntimeException("Parse error"));

            // when
            SyncResult result = syncService.syncOfac();

            // then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("Parse error");
        }
    }

    @Nested
    @DisplayName("syncUn() 메서드")
    class SyncUn {

        @Test
        @DisplayName("UN 동기화 성공")
        void shouldSyncUnSuccessfully() throws Exception {
            // given
            byte[] xmlData = "<CONSOLIDATED_LIST>test</CONSOLIDATED_LIST>".getBytes();
            List<ParsedSanctionsData> parsedData = createParsedDataList("UN", 5);
            SanctionsSyncHistoryEntity history = createHistoryEntity("UN");
            
            when(downloadService.downloadUnXmlAsBytes()).thenReturn(xmlData);
            when(unXmlParser.parse(any(InputStream.class))).thenReturn(parsedData);
            when(historyService.startSync("UN")).thenReturn(history);
            when(sanctionsRepository.findBySourceFileAndIsActiveTrue("UN"))
                    .thenReturn(Collections.emptyList());
            when(normalizationService.normalizeName(anyString())).thenAnswer(inv -> inv.getArgument(0));
            when(sanctionsRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            // when
            SyncResult result = syncService.syncUn();

            // then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getSourceFile()).isEqualTo("UN");
            assertThat(result.getInsertCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("UN 다운로드 실패 시 에러 결과 반환")
        void shouldReturnFailResultWhenDownloadFails() {
            // given
            SanctionsSyncHistoryEntity history = createHistoryEntity("UN");
            when(historyService.startSync("UN")).thenReturn(history);
            when(downloadService.downloadUnXmlAsBytes())
                    .thenThrow(new RuntimeException("Network timeout"));

            // when
            SyncResult result = syncService.syncUn();

            // then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("Network timeout");
        }
    }

    @Nested
    @DisplayName("syncAll() 메서드")
    class SyncAll {

        @Test
        @DisplayName("OFAC, UN, EU 모두 동기화한다")
        void shouldSyncAllSources() throws Exception {
            // given
            byte[] ofacXml = "<Sanctions>ofac</Sanctions>".getBytes();
            byte[] unXml = "<CONSOLIDATED_LIST>un</CONSOLIDATED_LIST>".getBytes();
            byte[] euXml = "<export>eu</export>".getBytes();

            when(downloadService.downloadOfacXmlAsBytes()).thenReturn(ofacXml);
            when(downloadService.downloadUnXmlAsBytes()).thenReturn(unXml);
            when(downloadService.downloadEuXmlAsBytes()).thenReturn(euXml);
            when(ofacXmlParser.parse(any(InputStream.class))).thenReturn(createParsedDataList("OFAC", 2));
            when(unXmlParser.parse(any(InputStream.class))).thenReturn(createParsedDataList("UN", 3));
            when(euXmlParser.parse(any(InputStream.class))).thenReturn(createParsedDataList("EU", 4));
            when(historyService.startSync(anyString())).thenAnswer(inv ->
                    createHistoryEntity(inv.getArgument(0)));
            when(sanctionsRepository.findBySourceFileAndIsActiveTrue(anyString()))
                    .thenReturn(Collections.emptyList());
            when(normalizationService.normalizeName(anyString())).thenAnswer(inv -> inv.getArgument(0));
            when(sanctionsRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            // when
            List<SyncResult> results = syncService.syncAll();

            // then
            assertThat(results).hasSize(3);
            assertThat(results).anyMatch(r -> "OFAC".equals(r.getSourceFile()));
            assertThat(results).anyMatch(r -> "UN".equals(r.getSourceFile()));
            assertThat(results).anyMatch(r -> "EU".equals(r.getSourceFile()));
        }

        @Test
        @DisplayName("하나가 실패해도 다른 것은 계속 진행한다")
        void shouldContinueIfOneFails() throws Exception {
            // given
            SanctionsSyncHistoryEntity ofacHistory = createHistoryEntity("OFAC");
            SanctionsSyncHistoryEntity unHistory = createHistoryEntity("UN");
            SanctionsSyncHistoryEntity euHistory = createHistoryEntity("EU");

            when(historyService.startSync("OFAC")).thenReturn(ofacHistory);
            when(historyService.startSync("UN")).thenReturn(unHistory);
            when(historyService.startSync("EU")).thenReturn(euHistory);

            when(downloadService.downloadOfacXmlAsBytes())
                    .thenThrow(new RuntimeException("OFAC download failed"));

            byte[] unXml = "<CONSOLIDATED_LIST>un</CONSOLIDATED_LIST>".getBytes();
            byte[] euXml = "<export>eu</export>".getBytes();
            when(downloadService.downloadUnXmlAsBytes()).thenReturn(unXml);
            when(downloadService.downloadEuXmlAsBytes()).thenReturn(euXml);
            when(unXmlParser.parse(any(InputStream.class))).thenReturn(createParsedDataList("UN", 2));
            when(euXmlParser.parse(any(InputStream.class))).thenReturn(createParsedDataList("EU", 3));
            when(sanctionsRepository.findBySourceFileAndIsActiveTrue("UN"))
                    .thenReturn(Collections.emptyList());
            when(sanctionsRepository.findBySourceFileAndIsActiveTrue("EU"))
                    .thenReturn(Collections.emptyList());
            when(normalizationService.normalizeName(anyString())).thenAnswer(inv -> inv.getArgument(0));
            when(sanctionsRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            // when
            List<SyncResult> results = syncService.syncAll();

            // then
            assertThat(results).hasSize(3);

            SyncResult ofacResult = results.stream()
                    .filter(r -> "OFAC".equals(r.getSourceFile()))
                    .findFirst().orElseThrow();
            SyncResult unResult = results.stream()
                    .filter(r -> "UN".equals(r.getSourceFile()))
                    .findFirst().orElseThrow();
            SyncResult euResult = results.stream()
                    .filter(r -> "EU".equals(r.getSourceFile()))
                    .findFirst().orElseThrow();

            assertThat(ofacResult.isSuccess()).isFalse();
            assertThat(unResult.isSuccess()).isTrue();
            assertThat(euResult.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("syncSanctionsData() - 데이터 동기화 로직")
    class SyncSanctionsData {

        @Test
        @DisplayName("새로운 데이터를 insert한다")
        void shouldInsertNewData() throws Exception {
            // given
            List<ParsedSanctionsData> parsedData = createParsedDataList("OFAC", 3);
            SanctionsSyncHistoryEntity history = createHistoryEntity("OFAC");
            
            when(downloadService.downloadOfacXmlAsBytes()).thenReturn(new byte[0]);
            when(ofacXmlParser.parse(any())).thenReturn(parsedData);
            when(historyService.startSync("OFAC")).thenReturn(history);
            when(sanctionsRepository.findBySourceFileAndIsActiveTrue("OFAC"))
                    .thenReturn(Collections.emptyList()); // 기존 데이터 없음
            when(normalizationService.normalizeName(anyString())).thenAnswer(inv -> inv.getArgument(0));
            when(sanctionsRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            // when
            SyncResult result = syncService.syncOfac();

            // then
            assertThat(result.getInsertCount()).isEqualTo(3);
            assertThat(result.getUpdateCount()).isZero();
            assertThat(result.getUnchangedCount()).isZero();
        }

        @Test
        @DisplayName("기존 데이터가 삭제된 경우 비활성화한다")
        void shouldDeactivateRemovedData() throws Exception {
            // given
            List<ParsedSanctionsData> parsedData = createParsedDataList("OFAC", 2);
            
            // 기존에 3개가 있었는데, 새로운 데이터는 2개만 있음 (1개 삭제됨)
            List<SanctionsEntity> existingEntities = new ArrayList<>();
            existingEntities.add(createEntity("OFAC-1", "OFAC"));
            existingEntities.add(createEntity("OFAC-2", "OFAC"));
            existingEntities.add(createEntity("OFAC-99", "OFAC")); // 삭제될 데이터
            
            SanctionsSyncHistoryEntity history = createHistoryEntity("OFAC");
            
            when(downloadService.downloadOfacXmlAsBytes()).thenReturn(new byte[0]);
            when(ofacXmlParser.parse(any())).thenReturn(parsedData);
            when(historyService.startSync("OFAC")).thenReturn(history);
            when(sanctionsRepository.findBySourceFileAndIsActiveTrue("OFAC"))
                    .thenReturn(existingEntities);
            when(normalizationService.normalizeName(anyString())).thenAnswer(inv -> inv.getArgument(0));
            when(sanctionsRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            // when
            SyncResult result = syncService.syncOfac();

            // then
            assertThat(result.getDeactivatedCount()).isEqualTo(1);
        }
    }

    // ========================================
    // Helper Methods
    // ========================================

    private List<ParsedSanctionsData> createParsedDataList(String sourceFile, int count) {
        List<ParsedSanctionsData> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(ParsedSanctionsData.builder()
                    .sourceUid(sourceFile + "-" + i)
                    .sourceFile(sourceFile)
                    .entityType("Individual")
                    .primaryName("Test Person " + i)
                    .sanctionListType("SDN")
                    .names(List.of(
                            ParsedSanctionsData.ParsedName.builder()
                                    .nameType("Primary")
                                    .fullName("Test Person " + i)
                                    .build()
                    ))
                    .addresses(Collections.emptyList())
                    .documents(Collections.emptyList())
                    .build());
        }
        return list;
    }

    private SanctionsSyncHistoryEntity createHistoryEntity(String sourceFile) {
        return SanctionsSyncHistoryEntity.builder()
                .historyId(1L)
                .sourceFile(sourceFile)
                .status(SanctionsSyncHistoryEntity.SyncStatus.FAIL)
                .startedAt(LocalDateTime.now())
                .build();
    }

    private SanctionsEntity createEntity(String sourceUid, String sourceFile) {
        return SanctionsEntity.builder()
                .entityId((long) sourceUid.hashCode())
                .sourceUid(sourceUid)
                .sourceFile(sourceFile)
                .entityType("Individual")
                .primaryName("Test " + sourceUid)
                .normalizedName("test " + sourceUid)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .lastUpdatedAt(LocalDateTime.now())
                .names(new ArrayList<>())
                .addresses(new ArrayList<>())
                .documents(new ArrayList<>())
                .build();
    }
}
