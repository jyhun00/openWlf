package aml.openwlf.batch.parser;

import aml.openwlf.batch.parser.model.ParsedSanctionsData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * OFAC SDN Advanced XML 파서
 * 
 * OFAC Advanced XML 구조:
 * <Sanctions>
 *   <DistinctParty FixedRef="..." DeltaAction="...">
 *     <Profile ID="...">
 *       <Identity>
 *         <Alias>
 *           <DocumentedName>
 *             <DocumentedNamePart>
 *               <NamePartValue>...</NamePartValue>
 *             </DocumentedNamePart>
 *           </DocumentedName>
 *         </Alias>
 *       </Identity>
 *       <Feature FeatureTypeID="...">
 *         <FeatureVersion>
 *           <DatePeriod>...</DatePeriod>
 *           <VersionLocation LocationID="..." />
 *           <VersionDetail>...</VersionDetail>
 *         </FeatureVersion>
 *       </Feature>
 *     </Profile>
 *   </DistinctParty>
 *   <ReferenceValueSets>
 *     <FeatureTypeValues>...</FeatureTypeValues>
 *     <AliasTypeValues>...</AliasTypeValues>
 *     <NamePartTypeValues>...</NamePartTypeValues>
 *     ...
 *   </ReferenceValueSets>
 * </Sanctions>
 */
@Slf4j
@Component
public class OfacXmlParser implements SanctionsXmlParser {

    private static final String SOURCE_FILE = "OFAC";
    private static final String SANCTION_LIST_TYPE = "SDN";
    
    // Reference value maps (ID -> Value mappings)
    private Map<String, String> featureTypes = new HashMap<>();
    private Map<String, String> aliasTypes = new HashMap<>();
    private Map<String, String> namePartTypes = new HashMap<>();
    private Map<String, String> partySubTypes = new HashMap<>();
    private Map<String, String> scriptValues = new HashMap<>();
    private Map<String, String> docTypes = new HashMap<>();
    private Map<String, String> areaValues = new HashMap<>(); // CountryID -> Country name
    private Map<String, String> detailTypes = new HashMap<>();
    
    private static final List<DateTimeFormatter> DATE_FORMATTERS = Arrays.asList(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("yyyy")
    );

    @Override
    public List<ParsedSanctionsData> parse(InputStream inputStream) throws Exception {
        List<ParsedSanctionsData> result = new ArrayList<>();
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(inputStream);
        
        // 먼저 ReferenceValueSets 파싱
        parseReferenceValueSets(document);
        
        // DistinctParty 엘리먼트들 파싱
        NodeList parties = document.getElementsByTagName("DistinctParty");
        log.info("OFAC XML: Found {} DistinctParty elements", parties.getLength());
        
        for (int i = 0; i < parties.getLength(); i++) {
            try {
                Element party = (Element) parties.item(i);
                ParsedSanctionsData data = parseDistinctParty(party);
                if (data != null) {
                    result.add(data);
                }
            } catch (Exception e) {
                log.warn("Failed to parse DistinctParty at index {}: {}", i, e.getMessage());
            }
        }
        
        log.info("OFAC XML: Successfully parsed {} entities", result.size());
        return result;
    }
    
    private void parseReferenceValueSets(Document document) {
        // FeatureTypeValues
        parseReferenceValues(document, "FeatureType", featureTypes);
        // AliasTypeValues
        parseReferenceValues(document, "AliasType", aliasTypes);
        // NamePartType
        parseReferenceValues(document, "NamePartType", namePartTypes);
        // PartySubType
        parseReferenceValues(document, "PartySubType", partySubTypes);
        // Script
        parseReferenceValues(document, "Script", scriptValues);
        // IDRegDocType
        parseReferenceValues(document, "IDRegDocType", docTypes);
        // AreaCode (Countries)
        parseAreaCodes(document);
        // DetailType
        parseReferenceValues(document, "DetailType", detailTypes);
        
        log.debug("Loaded reference values - FeatureTypes: {}, AliasTypes: {}, PartySubTypes: {}", 
                featureTypes.size(), aliasTypes.size(), partySubTypes.size());
    }
    
    private void parseReferenceValues(Document document, String tagName, Map<String, String> targetMap) {
        NodeList nodes = document.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element elem = (Element) nodes.item(i);
            String id = elem.getAttribute("ID");
            String value = getTextContent(elem);
            if (id != null && !id.isEmpty() && value != null && !value.isEmpty()) {
                targetMap.put(id, value);
            }
        }
    }
    
    private void parseAreaCodes(Document document) {
        NodeList areas = document.getElementsByTagName("AreaCode");
        for (int i = 0; i < areas.getLength(); i++) {
            Element area = (Element) areas.item(i);
            String id = area.getAttribute("ID");
            // AreaCode 안의 Description 또는 텍스트 값을 가져옴
            NodeList descriptions = area.getElementsByTagName("Description");
            String value;
            if (descriptions.getLength() > 0) {
                value = getTextContent((Element) descriptions.item(0));
            } else {
                value = getTextContent(area);
            }
            if (id != null && !id.isEmpty()) {
                // ISO 코드 추출 시도
                String isoCode = area.getAttribute("ISO2");
                if (isoCode == null || isoCode.isEmpty()) {
                    isoCode = value;
                }
                areaValues.put(id, isoCode != null ? isoCode : value);
            }
        }
        
        // Country 태그도 파싱
        NodeList countries = document.getElementsByTagName("Country");
        for (int i = 0; i < countries.getLength(); i++) {
            Element country = (Element) countries.item(i);
            String id = country.getAttribute("ID");
            String iso2 = country.getAttribute("ISO2");
            String name = getTextContent(country);
            if (id != null && !id.isEmpty()) {
                areaValues.put(id, iso2 != null && !iso2.isEmpty() ? iso2 : name);
            }
        }
    }
    
    private ParsedSanctionsData parseDistinctParty(Element party) {
        String fixedRef = party.getAttribute("FixedRef");
        
        // Profile 엘리먼트 가져오기
        NodeList profiles = party.getElementsByTagName("Profile");
        if (profiles.getLength() == 0) {
            log.debug("No Profile found for party {}", fixedRef);
            return null;
        }
        
        Element profile = (Element) profiles.item(0);
        String profileId = profile.getAttribute("ID");
        String partySubTypeId = profile.getAttribute("PartySubTypeID");
        String entityType = determineEntityType(partySubTypeId);
        
        ParsedSanctionsData.ParsedSanctionsDataBuilder dataBuilder = ParsedSanctionsData.builder()
                .sourceUid("OFAC-" + fixedRef)
                .sourceFile(SOURCE_FILE)
                .entityType(entityType)
                .sanctionListType(SANCTION_LIST_TYPE);
        
        // Identity (이름/별칭) 파싱
        List<ParsedSanctionsData.ParsedName> names = parseIdentity(profile);
        dataBuilder.names(names);
        
        // Primary name 설정
        if (!names.isEmpty()) {
            String primaryName = names.stream()
                    .filter(n -> "Primary".equals(n.getNameType()))
                    .map(ParsedSanctionsData.ParsedName::getFullName)
                    .findFirst()
                    .orElse(names.get(0).getFullName());
            dataBuilder.primaryName(primaryName);
        }
        
        // Feature 파싱 (DOB, Nationality, Gender, etc.)
        Map<String, Object> features = parseFeatures(profile);
        dataBuilder.additionalFeatures(new HashMap<>(features));
        
        // 주요 필드 추출
        if (features.containsKey("Birthdate")) {
            dataBuilder.birthDate(parseDate((String) features.get("Birthdate")));
        }
        if (features.containsKey("Gender")) {
            dataBuilder.gender((String) features.get("Gender"));
        }
        if (features.containsKey("Nationality")) {
            Object nationality = features.get("Nationality");
            if (nationality instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> nationalities = (List<String>) nationality;
                dataBuilder.nationality(String.join(",", nationalities));
            } else {
                dataBuilder.nationality((String) nationality);
            }
        }
        if (features.containsKey("Vessel Flag")) {
            dataBuilder.vesselFlag((String) features.get("Vessel Flag"));
        }
        
        // 주소 파싱
        List<ParsedSanctionsData.ParsedAddress> addresses = parseLocations(profile);
        dataBuilder.addresses(addresses);
        
        // 문서/ID 파싱
        List<ParsedSanctionsData.ParsedDocument> documents = parseIDRegDocuments(profile);
        dataBuilder.documents(documents);
        
        // SanctionsPrograms 파싱
        List<String> programs = parseSanctionsPrograms(profile);
        if (!programs.isEmpty()) {
            Map<String, Object> additionalFeatures = dataBuilder.build().getAdditionalFeatures();
            if (additionalFeatures == null) {
                additionalFeatures = new HashMap<>();
            }
            additionalFeatures.put("programs", programs);
            dataBuilder.additionalFeatures(additionalFeatures);
        }
        
        return dataBuilder.build();
    }
    
    private String determineEntityType(String partySubTypeId) {
        if (partySubTypeId == null || partySubTypeId.isEmpty()) {
            return "Unknown";
        }
        String subType = partySubTypes.getOrDefault(partySubTypeId, "").toLowerCase();
        if (subType.contains("individual")) {
            return "Individual";
        } else if (subType.contains("vessel")) {
            return "Vessel";
        } else if (subType.contains("aircraft")) {
            return "Aircraft";
        } else if (subType.contains("entity") || subType.contains("organization")) {
            return "Entity";
        }
        return "Entity"; // 기본값
    }
    
    private List<ParsedSanctionsData.ParsedName> parseIdentity(Element profile) {
        List<ParsedSanctionsData.ParsedName> names = new ArrayList<>();
        
        NodeList identities = profile.getElementsByTagName("Identity");
        if (identities.getLength() == 0) return names;
        
        Element identity = (Element) identities.item(0);
        NodeList aliases = identity.getElementsByTagName("Alias");
        
        for (int i = 0; i < aliases.getLength(); i++) {
            Element alias = (Element) aliases.item(i);
            String aliasTypeId = alias.getAttribute("AliasTypeID");
            String nameType = mapAliasType(aliasTypeId);
            boolean isPrimary = Boolean.parseBoolean(alias.getAttribute("Primary"));
            if (isPrimary) {
                nameType = "Primary";
            }
            
            // Low Quality AKA 판별
            String lowQuality = alias.getAttribute("LowQuality");
            if ("true".equalsIgnoreCase(lowQuality)) {
                nameType = "Low Quality AKA";
            }
            
            NodeList docNames = alias.getElementsByTagName("DocumentedName");
            for (int j = 0; j < docNames.getLength(); j++) {
                Element docName = (Element) docNames.item(j);
                ParsedSanctionsData.ParsedName name = parseDocumentedName(docName, nameType);
                if (name != null && name.getFullName() != null && !name.getFullName().isEmpty()) {
                    names.add(name);
                }
            }
        }
        
        return names;
    }
    
    private ParsedSanctionsData.ParsedName parseDocumentedName(Element docName, String nameType) {
        StringBuilder fullName = new StringBuilder();
        String firstName = null, middleName = null, lastName = null;
        String script = "Latin";
        
        NodeList nameParts = docName.getElementsByTagName("DocumentedNamePart");
        for (int i = 0; i < nameParts.getLength(); i++) {
            Element part = (Element) nameParts.item(i);
            NodeList values = part.getElementsByTagName("NamePartValue");
            if (values.getLength() > 0) {
                Element valueElem = (Element) values.item(0);
                String value = getTextContent(valueElem);
                String namePartGroupId = valueElem.getAttribute("NamePartGroupID");
                String scriptId = valueElem.getAttribute("ScriptID");
                
                if (scriptId != null && !scriptId.isEmpty()) {
                    script = scriptValues.getOrDefault(scriptId, "Latin");
                }
                
                if (value != null && !value.isEmpty()) {
                    if (fullName.length() > 0) {
                        fullName.append(" ");
                    }
                    fullName.append(value);
                    
                    // 이름 파트 분류 (간소화)
                    String partType = namePartTypes.getOrDefault(namePartGroupId, "");
                    if (partType.toLowerCase().contains("first") || partType.toLowerCase().contains("given")) {
                        firstName = value;
                    } else if (partType.toLowerCase().contains("middle")) {
                        middleName = value;
                    } else if (partType.toLowerCase().contains("last") || partType.toLowerCase().contains("surname")) {
                        lastName = value;
                    }
                }
            }
        }
        
        return ParsedSanctionsData.ParsedName.builder()
                .nameType(nameType)
                .fullName(fullName.toString().trim())
                .script(script)
                .qualityScore("Low Quality AKA".equals(nameType) ? 50 : 100)
                .firstName(firstName)
                .middleName(middleName)
                .lastName(lastName)
                .build();
    }
    
    private String mapAliasType(String aliasTypeId) {
        if (aliasTypeId == null || aliasTypeId.isEmpty()) return "AKA";
        String type = aliasTypes.getOrDefault(aliasTypeId, "AKA");
        if (type.toLowerCase().contains("primary") || type.toLowerCase().contains("name")) {
            return "Primary";
        } else if (type.toLowerCase().contains("also known") || type.toLowerCase().contains("a.k.a")) {
            return "AKA";
        } else if (type.toLowerCase().contains("formerly") || type.toLowerCase().contains("f.k.a")) {
            return "FKA";
        }
        return "AKA";
    }
    
    private Map<String, Object> parseFeatures(Element profile) {
        Map<String, Object> features = new HashMap<>();
        
        NodeList featureNodes = profile.getElementsByTagName("Feature");
        for (int i = 0; i < featureNodes.getLength(); i++) {
            Element feature = (Element) featureNodes.item(i);
            String featureTypeId = feature.getAttribute("FeatureTypeID");
            String featureType = featureTypes.getOrDefault(featureTypeId, "Unknown");
            
            NodeList versions = feature.getElementsByTagName("FeatureVersion");
            for (int j = 0; j < versions.getLength(); j++) {
                Element version = (Element) versions.item(j);
                
                // DatePeriod에서 날짜 추출
                NodeList datePeriods = version.getElementsByTagName("DatePeriod");
                if (datePeriods.getLength() > 0) {
                    String dateValue = extractDateFromPeriod((Element) datePeriods.item(0));
                    if (dateValue != null) {
                        features.put(featureType, dateValue);
                    }
                }
                
                // VersionDetail에서 값 추출
                NodeList details = version.getElementsByTagName("VersionDetail");
                if (details.getLength() > 0) {
                    String detailValue = getTextContent((Element) details.item(0));
                    if (detailValue != null && !detailValue.isEmpty()) {
                        // 같은 타입의 값이 여러 개인 경우 리스트로 저장
                        if (features.containsKey(featureType)) {
                            Object existing = features.get(featureType);
                            if (existing instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<String> list = (List<String>) existing;
                                list.add(detailValue);
                            } else {
                                List<String> list = new ArrayList<>();
                                list.add((String) existing);
                                list.add(detailValue);
                                features.put(featureType, list);
                            }
                        } else {
                            features.put(featureType, detailValue);
                        }
                    }
                }
                
                // VersionLocation에서 국가 정보 추출
                NodeList locations = version.getElementsByTagName("VersionLocation");
                if (locations.getLength() > 0) {
                    Element location = (Element) locations.item(0);
                    String locationId = location.getAttribute("LocationID");
                    String countryCode = areaValues.getOrDefault(locationId, locationId);
                    if (countryCode != null && !countryCode.isEmpty()) {
                        features.put(featureType, countryCode);
                    }
                }
            }
        }
        
        return features;
    }
    
    private String extractDateFromPeriod(Element datePeriod) {
        // Start 또는 단일 날짜 추출
        NodeList starts = datePeriod.getElementsByTagName("Start");
        if (starts.getLength() > 0) {
            return extractDateFromNode((Element) starts.item(0));
        }
        // From 날짜
        NodeList froms = datePeriod.getElementsByTagName("From");
        if (froms.getLength() > 0) {
            return extractDateFromNode((Element) froms.item(0));
        }
        return null;
    }
    
    private String extractDateFromNode(Element dateNode) {
        // Year, Month, Day 추출
        NodeList years = dateNode.getElementsByTagName("Year");
        NodeList months = dateNode.getElementsByTagName("Month");
        NodeList days = dateNode.getElementsByTagName("Day");
        
        String year = years.getLength() > 0 ? getTextContent((Element) years.item(0)) : null;
        String month = months.getLength() > 0 ? getTextContent((Element) months.item(0)) : null;
        String day = days.getLength() > 0 ? getTextContent((Element) days.item(0)) : null;
        
        if (year != null) {
            StringBuilder date = new StringBuilder(year);
            if (month != null) {
                date.append("-").append(String.format("%02d", Integer.parseInt(month)));
                if (day != null) {
                    date.append("-").append(String.format("%02d", Integer.parseInt(day)));
                }
            }
            return date.toString();
        }
        return null;
    }
    
    private List<ParsedSanctionsData.ParsedAddress> parseLocations(Element profile) {
        List<ParsedSanctionsData.ParsedAddress> addresses = new ArrayList<>();
        
        // Profile 레벨에서 Feature 중 Location 타입 찾기
        NodeList features = profile.getElementsByTagName("Feature");
        for (int i = 0; i < features.getLength(); i++) {
            Element feature = (Element) features.item(i);
            String featureTypeId = feature.getAttribute("FeatureTypeID");
            String featureType = featureTypes.getOrDefault(featureTypeId, "").toLowerCase();
            
            if (featureType.contains("location") || featureType.contains("address")) {
                NodeList versions = feature.getElementsByTagName("FeatureVersion");
                for (int j = 0; j < versions.getLength(); j++) {
                    Element version = (Element) versions.item(j);
                    ParsedSanctionsData.ParsedAddress address = parseVersionLocation(version);
                    if (address != null) {
                        addresses.add(address);
                    }
                }
            }
        }
        
        // 직접 Location 태그도 파싱
        NodeList locations = profile.getElementsByTagName("Location");
        for (int i = 0; i < locations.getLength(); i++) {
            Element location = (Element) locations.item(i);
            ParsedSanctionsData.ParsedAddress address = parseLocation(location);
            if (address != null) {
                addresses.add(address);
            }
        }
        
        return addresses;
    }
    
    private ParsedSanctionsData.ParsedAddress parseVersionLocation(Element version) {
        NodeList locations = version.getElementsByTagName("VersionLocation");
        if (locations.getLength() == 0) return null;
        
        Element location = (Element) locations.item(0);
        String locationId = location.getAttribute("LocationID");
        
        return ParsedSanctionsData.ParsedAddress.builder()
                .countryCode(areaValues.getOrDefault(locationId, locationId))
                .build();
    }
    
    private ParsedSanctionsData.ParsedAddress parseLocation(Element location) {
        ParsedSanctionsData.ParsedAddress.ParsedAddressBuilder builder = 
                ParsedSanctionsData.ParsedAddress.builder();
        
        // LocationPart들에서 주소 정보 추출
        NodeList parts = location.getElementsByTagName("LocationPart");
        StringBuilder fullAddress = new StringBuilder();
        
        for (int i = 0; i < parts.getLength(); i++) {
            Element part = (Element) parts.item(i);
            NodeList values = part.getElementsByTagName("LocationPartValue");
            if (values.getLength() > 0) {
                Element value = (Element) values.item(0);
                String valueText = getTextContent(value);
                String locPartTypeId = value.getAttribute("LocPartTypeID");
                String partType = detailTypes.getOrDefault(locPartTypeId, "");
                
                if (valueText != null && !valueText.isEmpty()) {
                    if (fullAddress.length() > 0) fullAddress.append(", ");
                    fullAddress.append(valueText);
                    
                    // 파트 타입에 따라 필드 설정
                    if (partType.toLowerCase().contains("city")) {
                        builder.city(valueText);
                    } else if (partType.toLowerCase().contains("state") || partType.toLowerCase().contains("province")) {
                        builder.stateProvince(valueText);
                    } else if (partType.toLowerCase().contains("postal") || partType.toLowerCase().contains("zip")) {
                        builder.postalCode(valueText);
                    } else if (partType.toLowerCase().contains("street") || partType.toLowerCase().contains("address")) {
                        builder.street(valueText);
                    }
                }
            }
        }
        
        // LocationCountry에서 국가 정보 추출
        NodeList countries = location.getElementsByTagName("LocationCountry");
        if (countries.getLength() > 0) {
            Element country = (Element) countries.item(0);
            String countryId = country.getAttribute("CountryID");
            String countryCode = areaValues.getOrDefault(countryId, countryId);
            builder.countryCode(countryCode);
            builder.country(countryCode);
        }
        
        builder.fullAddress(fullAddress.toString());
        
        ParsedSanctionsData.ParsedAddress address = builder.build();
        // 빈 주소는 반환하지 않음
        if (address.getFullAddress() == null || address.getFullAddress().isEmpty()) {
            if (address.getCountryCode() == null || address.getCountryCode().isEmpty()) {
                return null;
            }
        }
        
        return address;
    }
    
    private List<ParsedSanctionsData.ParsedDocument> parseIDRegDocuments(Element profile) {
        List<ParsedSanctionsData.ParsedDocument> documents = new ArrayList<>();
        
        NodeList idDocs = profile.getElementsByTagName("IDRegDocument");
        for (int i = 0; i < idDocs.getLength(); i++) {
            Element idDoc = (Element) idDocs.item(i);
            ParsedSanctionsData.ParsedDocument doc = parseIDRegDocument(idDoc);
            if (doc != null) {
                documents.add(doc);
            }
        }
        
        return documents;
    }
    
    private ParsedSanctionsData.ParsedDocument parseIDRegDocument(Element idDoc) {
        String docTypeId = idDoc.getAttribute("IDRegDocTypeID");
        String docType = docTypes.getOrDefault(docTypeId, "Other");
        
        ParsedSanctionsData.ParsedDocument.ParsedDocumentBuilder builder = 
                ParsedSanctionsData.ParsedDocument.builder()
                .documentType(docType);
        
        // IDRegistrationNo
        NodeList regNos = idDoc.getElementsByTagName("IDRegistrationNo");
        if (regNos.getLength() > 0) {
            builder.documentNumber(getTextContent((Element) regNos.item(0)));
        }
        
        // IssuingAuthority
        NodeList issuers = idDoc.getElementsByTagName("IssuingAuthority");
        if (issuers.getLength() > 0) {
            builder.issuingAuthority(getTextContent((Element) issuers.item(0)));
        }
        
        // IssuedBy-Loss (국가 정보)
        NodeList issuedCountries = idDoc.getElementsByTagName("IDRegDocIssuedBy");
        if (issuedCountries.getLength() > 0) {
            Element issuedBy = (Element) issuedCountries.item(0);
            String countryId = issuedBy.getAttribute("CountryID");
            if (countryId != null && !countryId.isEmpty()) {
                String countryCode = areaValues.getOrDefault(countryId, countryId);
                builder.issuingCountryCode(countryCode);
                builder.issuingCountry(countryCode);
            }
        }
        
        // IssuedDate
        NodeList issueDates = idDoc.getElementsByTagName("IDRegDocDateOfIssue");
        if (issueDates.getLength() > 0) {
            String dateStr = extractDateFromPeriod((Element) issueDates.item(0));
            builder.issueDate(parseDate(dateStr));
        }
        
        // ExpiryDate
        NodeList expiryDates = idDoc.getElementsByTagName("IDRegDocExpiry");
        if (expiryDates.getLength() > 0) {
            String dateStr = extractDateFromPeriod((Element) expiryDates.item(0));
            builder.expiryDate(parseDate(dateStr));
        }
        
        // Note/Comment
        NodeList comments = idDoc.getElementsByTagName("Comment");
        if (comments.getLength() > 0) {
            builder.note(getTextContent((Element) comments.item(0)));
        }
        
        return builder.build();
    }
    
    private List<String> parseSanctionsPrograms(Element profile) {
        List<String> programs = new ArrayList<>();
        
        NodeList programNodes = profile.getElementsByTagName("SanctionsProgram");
        for (int i = 0; i < programNodes.getLength(); i++) {
            Element program = (Element) programNodes.item(i);
            String programName = getTextContent(program);
            if (programName != null && !programName.isEmpty()) {
                programs.add(programName);
            }
        }
        
        // 부모 Profile에서도 프로그램 정보 찾기
        NodeList measures = profile.getElementsByTagName("SanctionsMeasure");
        for (int i = 0; i < measures.getLength(); i++) {
            Element measure = (Element) measures.item(i);
            NodeList measurePrograms = measure.getElementsByTagName("SanctionsProgram");
            for (int j = 0; j < measurePrograms.getLength(); j++) {
                String programName = getTextContent((Element) measurePrograms.item(j));
                if (programName != null && !programName.isEmpty() && !programs.contains(programName)) {
                    programs.add(programName);
                }
            }
        }
        
        return programs;
    }
    
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                // 연도만 있는 경우 1월 1일로 설정
                if (dateStr.matches("\\d{4}")) {
                    return LocalDate.of(Integer.parseInt(dateStr), 1, 1);
                }
                // 연도-월만 있는 경우 1일로 설정
                if (dateStr.matches("\\d{4}-\\d{2}")) {
                    dateStr = dateStr + "-01";
                }
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // 다음 포맷터 시도
            }
        }
        log.debug("Failed to parse date: {}", dateStr);
        return null;
    }
    
    private String getTextContent(Element element) {
        if (element == null) return null;
        String text = element.getTextContent();
        return text != null ? text.trim() : null;
    }

    @Override
    public String getSourceFile() {
        return SOURCE_FILE;
    }
}
