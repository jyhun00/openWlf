package aml.openwlf.batch.service;

import aml.openwlf.batch.config.SanctionsDownloadProperties;
import aml.openwlf.batch.parser.EuXmlParser;
import aml.openwlf.batch.parser.OfacXmlParser;
import aml.openwlf.batch.parser.UnXmlParser;
import aml.openwlf.batch.parser.model.ParsedSanctionsData;
import aml.openwlf.core.normalization.NormalizationService;
import aml.openwlf.data.entity.EntityAddressEntity;
import aml.openwlf.data.entity.EntityDocumentEntity;
import aml.openwlf.data.entity.EntityNameEntity;
import aml.openwlf.data.entity.SanctionsEntity;
import aml.openwlf.data.entity.SanctionsSyncHistoryEntity;
import aml.openwlf.data.repository.SanctionsEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 제재 리스트 동기화 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SanctionsSyncService {

    private final SanctionsDownloadService downloadService;
    private final OfacXmlParser ofacXmlParser;
    private final UnXmlParser unXmlParser;
    private final EuXmlParser euXmlParser;
    private final SanctionsEntityRepository sanctionsRepository;
    private final NormalizationService normalizationService;
    private final SanctionsDownloadProperties properties;
    private final SanctionsSyncHistoryService historyService;

    /**
     * OFAC 제재 리스트 동기화
     */
    public SyncResult syncOfac() {
        log.info("Starting OFAC sanctions list synchronization");
        SanctionsSyncHistoryEntity history = historyService.startSync("OFAC");
        
        try {
            byte[] xmlData = downloadService.downloadOfacXmlAsBytes();
            InputStream xmlStream = new ByteArrayInputStream(xmlData);
            List<ParsedSanctionsData> parsedData = ofacXmlParser.parse(xmlStream);
            
            SyncResult result = syncSanctionsData(parsedData, "OFAC");
            result.setFileSizeBytes((long) xmlData.length);
            result.setStartTime(history.getStartedAt());
            result.setDurationMs(java.time.Duration.between(history.getStartedAt(), LocalDateTime.now()).toMillis());
            
            historyService.completeSuccess(history.getHistoryId(), result);
            return result;
            
        } catch (Exception e) {
            log.error("Failed to sync OFAC sanctions list", e);
            String fullErrorLog = getFullStackTrace(e);
            historyService.completeFail(history.getHistoryId(), e.getMessage(), fullErrorLog);
            return SyncResult.failed("OFAC", e.getMessage(), fullErrorLog);
        }
    }

    /**
     * UN 제재 리스트 동기화
     */
    public SyncResult syncUn() {
        log.info("Starting UN sanctions list synchronization");
        SanctionsSyncHistoryEntity history = historyService.startSync("UN");
        
        try {
            byte[] xmlData = downloadService.downloadUnXmlAsBytes();
            InputStream xmlStream = new ByteArrayInputStream(xmlData);
            List<ParsedSanctionsData> parsedData = unXmlParser.parse(xmlStream);
            
            SyncResult result = syncSanctionsData(parsedData, "UN");
            result.setFileSizeBytes((long) xmlData.length);
            result.setStartTime(history.getStartedAt());
            result.setDurationMs(java.time.Duration.between(history.getStartedAt(), LocalDateTime.now()).toMillis());
            
            historyService.completeSuccess(history.getHistoryId(), result);
            return result;
            
        } catch (Exception e) {
            log.error("Failed to sync UN sanctions list", e);
            String fullErrorLog = getFullStackTrace(e);
            historyService.completeFail(history.getHistoryId(), e.getMessage(), fullErrorLog);
            return SyncResult.failed("UN", e.getMessage(), fullErrorLog);
        }
    }

    /**
     * EU 제재 리스트 동기화
     */
    public SyncResult syncEu() {
        log.info("Starting EU sanctions list synchronization");
        SanctionsSyncHistoryEntity history = historyService.startSync("EU");

        try {
            byte[] xmlData = downloadService.downloadEuXmlAsBytes();
            InputStream xmlStream = new ByteArrayInputStream(xmlData);
            List<ParsedSanctionsData> parsedData = euXmlParser.parse(xmlStream);

            SyncResult result = syncSanctionsData(parsedData, "EU");
            result.setFileSizeBytes((long) xmlData.length);
            result.setStartTime(history.getStartedAt());
            result.setDurationMs(java.time.Duration.between(history.getStartedAt(), LocalDateTime.now()).toMillis());

            historyService.completeSuccess(history.getHistoryId(), result);
            return result;

        } catch (Exception e) {
            log.error("Failed to sync EU sanctions list", e);
            String fullErrorLog = getFullStackTrace(e);
            historyService.completeFail(history.getHistoryId(), e.getMessage(), fullErrorLog);
            return SyncResult.failed("EU", e.getMessage(), fullErrorLog);
        }
    }

    /**
     * 모든 제재 리스트 동기화 (OFAC + UN + EU)
     */
    public List<SyncResult> syncAll() {
        List<SyncResult> results = new ArrayList<>();
        results.add(syncOfac());
        results.add(syncUn());
        results.add(syncEu());
        return results;
    }

    /**
     * 파싱된 데이터를 DB와 동기화
     */
    @Transactional
    protected SyncResult syncSanctionsData(List<ParsedSanctionsData> parsedData, String sourceFile) {
        log.info("Syncing {} entries for source: {}", parsedData.size(), sourceFile);
        
        int insertCount = 0, updateCount = 0, unchangedCount = 0, deactivatedCount = 0;
        
        List<SanctionsEntity> existingEntities = sanctionsRepository.findBySourceFileAndIsActiveTrue(sourceFile);
        Map<String, SanctionsEntity> existingMap = existingEntities.stream()
                .collect(Collectors.toMap(SanctionsEntity::getSourceUid, e -> e, (e1, e2) -> e1));
        
        Set<String> newSourceUids = parsedData.stream()
                .map(ParsedSanctionsData::getSourceUid)
                .collect(Collectors.toSet());
        
        List<SanctionsEntity> toSave = new ArrayList<>();
        int batchSize = properties.getBatchSize();
        
        for (ParsedSanctionsData parsed : parsedData) {
            SanctionsEntity existing = existingMap.get(parsed.getSourceUid());
            
            if (existing == null) {
                toSave.add(convertToEntity(parsed));
                insertCount++;
            } else {
                String newHash = parsed.generateContentHash();
                String existingHash = generateExistingHash(existing);
                
                if (!newHash.equals(existingHash)) {
                    updateEntity(existing, parsed);
                    toSave.add(existing);
                    updateCount++;
                } else {
                    unchangedCount++;
                }
            }
            
            if (toSave.size() >= batchSize) {
                sanctionsRepository.saveAll(toSave);
                toSave.clear();
            }
        }
        
        if (!toSave.isEmpty()) {
            sanctionsRepository.saveAll(toSave);
        }
        
        // 삭제된 데이터 처리
        List<SanctionsEntity> toDeactivate = new ArrayList<>();
        for (SanctionsEntity existing : existingEntities) {
            if (!newSourceUids.contains(existing.getSourceUid())) {
                existing.setIsActive(false);
                existing.setLastUpdatedAt(LocalDateTime.now());
                toDeactivate.add(existing);
                deactivatedCount++;
            }
        }
        if (!toDeactivate.isEmpty()) {
            sanctionsRepository.saveAll(toDeactivate);
        }
        
        SyncResult result = SyncResult.success(sourceFile, insertCount, updateCount, unchangedCount, deactivatedCount);
        log.info("Sync completed for {}: {}", sourceFile, result);
        return result;
    }

    private String generateExistingHash(SanctionsEntity entity) {
        ParsedSanctionsData parsed = ParsedSanctionsData.builder()
                .sourceUid(entity.getSourceUid())
                .sourceFile(entity.getSourceFile())
                .entityType(entity.getEntityType())
                .primaryName(entity.getPrimaryName())
                .gender(entity.getGender())
                .birthDate(entity.getBirthDate())
                .nationality(entity.getNationality())
                .vesselFlag(entity.getVesselFlag())
                .sanctionListType(entity.getSanctionListType())
                .names(entity.getNames().stream()
                        .map(n -> ParsedSanctionsData.ParsedName.builder()
                                .nameType(n.getNameType())
                                .fullName(n.getFullName())
                                .build())
                        .collect(Collectors.toList()))
                .addresses(entity.getAddresses().stream()
                        .map(a -> ParsedSanctionsData.ParsedAddress.builder()
                                .fullAddress(a.getFullAddress())
                                .build())
                        .collect(Collectors.toList()))
                .documents(entity.getDocuments().stream()
                        .map(d -> ParsedSanctionsData.ParsedDocument.builder()
                                .documentType(d.getDocumentType())
                                .documentNumber(d.getDocumentNumber())
                                .build())
                        .collect(Collectors.toList()))
                .build();
        return parsed.generateContentHash();
    }

    private SanctionsEntity convertToEntity(ParsedSanctionsData parsed) {
        SanctionsEntity entity = SanctionsEntity.builder()
                .sourceUid(parsed.getSourceUid())
                .sourceFile(parsed.getSourceFile())
                .entityType(parsed.getEntityType())
                .primaryName(parsed.getPrimaryName())
                .normalizedName(normalizationService.normalizeName(parsed.getPrimaryName()))
                .gender(parsed.getGender())
                .birthDate(parsed.getBirthDate())
                .nationality(parsed.getNationality())
                .vesselFlag(parsed.getVesselFlag())
                .sanctionListType(parsed.getSanctionListType())
                .additionalFeatures(parsed.getAdditionalFeatures())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .lastUpdatedAt(LocalDateTime.now())
                .names(new ArrayList<>())
                .addresses(new ArrayList<>())
                .documents(new ArrayList<>())
                .build();
        
        for (ParsedSanctionsData.ParsedName pn : parsed.getNames()) {
            entity.addName(convertToNameEntity(pn));
        }
        for (ParsedSanctionsData.ParsedAddress pa : parsed.getAddresses()) {
            entity.addAddress(convertToAddressEntity(pa));
        }
        for (ParsedSanctionsData.ParsedDocument pd : parsed.getDocuments()) {
            entity.addDocument(convertToDocumentEntity(pd));
        }
        return entity;
    }

    private void updateEntity(SanctionsEntity existing, ParsedSanctionsData parsed) {
        existing.setEntityType(parsed.getEntityType());
        existing.setPrimaryName(parsed.getPrimaryName());
        existing.setNormalizedName(normalizationService.normalizeName(parsed.getPrimaryName()));
        existing.setGender(parsed.getGender());
        existing.setBirthDate(parsed.getBirthDate());
        existing.setNationality(parsed.getNationality());
        existing.setVesselFlag(parsed.getVesselFlag());
        existing.setSanctionListType(parsed.getSanctionListType());
        existing.setAdditionalFeatures(parsed.getAdditionalFeatures());
        existing.setLastUpdatedAt(LocalDateTime.now());
        
        existing.getNames().clear();
        existing.getAddresses().clear();
        existing.getDocuments().clear();
        
        for (ParsedSanctionsData.ParsedName pn : parsed.getNames()) {
            existing.addName(convertToNameEntity(pn));
        }
        for (ParsedSanctionsData.ParsedAddress pa : parsed.getAddresses()) {
            existing.addAddress(convertToAddressEntity(pa));
        }
        for (ParsedSanctionsData.ParsedDocument pd : parsed.getDocuments()) {
            existing.addDocument(convertToDocumentEntity(pd));
        }
    }

    private EntityNameEntity convertToNameEntity(ParsedSanctionsData.ParsedName pn) {
        return EntityNameEntity.builder()
                .nameType(pn.getNameType())
                .fullName(pn.getFullName())
                .normalizedName(normalizationService.normalizeName(pn.getFullName()))
                .script(pn.getScript())
                .qualityScore(pn.getQualityScore())
                .firstName(pn.getFirstName())
                .middleName(pn.getMiddleName())
                .lastName(pn.getLastName())
                .build();
    }

    private EntityAddressEntity convertToAddressEntity(ParsedSanctionsData.ParsedAddress pa) {
        return EntityAddressEntity.builder()
                .addressType(pa.getAddressType())
                .fullAddress(pa.getFullAddress())
                .street(pa.getStreet())
                .city(pa.getCity())
                .stateProvince(pa.getStateProvince())
                .postalCode(pa.getPostalCode())
                .country(pa.getCountry())
                .countryCode(pa.getCountryCode())
                .note(pa.getNote())
                .build();
    }

    private EntityDocumentEntity convertToDocumentEntity(ParsedSanctionsData.ParsedDocument pd) {
        return EntityDocumentEntity.builder()
                .documentType(pd.getDocumentType())
                .documentNumber(pd.getDocumentNumber())
                .issuingCountry(pd.getIssuingCountry())
                .issuingCountryCode(pd.getIssuingCountryCode())
                .issueDate(pd.getIssueDate())
                .expiryDate(pd.getExpiryDate())
                .issuingAuthority(pd.getIssuingAuthority())
                .note(pd.getNote())
                .build();
    }

    private String getFullStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
