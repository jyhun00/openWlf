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
 * UN Security Council Consolidated List XML 파서
 * 
 * UN Consolidated List XML 구조:
 * <CONSOLIDATED_LIST>
 *   <INDIVIDUALS>
 *     <INDIVIDUAL>
 *       <DATAID>...</DATAID>
 *       <FIRST_NAME>...</FIRST_NAME>
 *       <SECOND_NAME>...</SECOND_NAME>
 *       <THIRD_NAME>...</THIRD_NAME>
 *       <FOURTH_NAME>...</FOURTH_NAME>
 *       <UN_LIST_TYPE>...</UN_LIST_TYPE>
 *       <REFERENCE_NUMBER>...</REFERENCE_NUMBER>
 *       <LISTED_ON>...</LISTED_ON>
 *       <NATIONALITY>
 *         <VALUE>...</VALUE>
 *       </NATIONALITY>
 *       <INDIVIDUAL_DATE_OF_BIRTH>
 *         <DATE>...</DATE>
 *       </INDIVIDUAL_DATE_OF_BIRTH>
 *       <INDIVIDUAL_PLACE_OF_BIRTH>
 *         <CITY>...</CITY>
 *         <COUNTRY>...</COUNTRY>
 *       </INDIVIDUAL_PLACE_OF_BIRTH>
 *       <INDIVIDUAL_ADDRESS>
 *         <STREET>...</STREET>
 *         <CITY>...</CITY>
 *         <COUNTRY>...</COUNTRY>
 *       </INDIVIDUAL_ADDRESS>
 *       <INDIVIDUAL_ALIAS>
 *         <QUALITY>...</QUALITY>
 *         <ALIAS_NAME>...</ALIAS_NAME>
 *       </INDIVIDUAL_ALIAS>
 *       <INDIVIDUAL_DOCUMENT>
 *         <TYPE_OF_DOCUMENT>...</TYPE_OF_DOCUMENT>
 *         <NUMBER>...</NUMBER>
 *         <ISSUING_COUNTRY>...</ISSUING_COUNTRY>
 *       </INDIVIDUAL_DOCUMENT>
 *     </INDIVIDUAL>
 *   </INDIVIDUALS>
 *   <ENTITIES>
 *     <ENTITY>
 *       <DATAID>...</DATAID>
 *       <FIRST_NAME>...</FIRST_NAME>
 *       <UN_LIST_TYPE>...</UN_LIST_TYPE>
 *       <ENTITY_ADDRESS>...</ENTITY_ADDRESS>
 *       <ENTITY_ALIAS>...</ENTITY_ALIAS>
 *     </ENTITY>
 *   </ENTITIES>
 * </CONSOLIDATED_LIST>
 */
@Slf4j
@Component
public class UnXmlParser implements SanctionsXmlParser {

    private static final String SOURCE_FILE = "UN";
    private static final String SANCTION_LIST_TYPE = "UN Security Council Consolidated List";
    
    private static final List<DateTimeFormatter> DATE_FORMATTERS = Arrays.asList(
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("yyyy")
    );

    @Override
    public List<ParsedSanctionsData> parse(InputStream inputStream) throws Exception {
        List<ParsedSanctionsData> result = new ArrayList<>();
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(inputStream);
        
        // INDIVIDUAL 엘리먼트들 파싱
        NodeList individuals = document.getElementsByTagName("INDIVIDUAL");
        log.info("UN XML: Found {} INDIVIDUAL elements", individuals.getLength());
        
        for (int i = 0; i < individuals.getLength(); i++) {
            try {
                Element individual = (Element) individuals.item(i);
                ParsedSanctionsData data = parseIndividual(individual);
                if (data != null) {
                    result.add(data);
                }
            } catch (Exception e) {
                log.warn("Failed to parse INDIVIDUAL at index {}: {}", i, e.getMessage());
            }
        }
        
        // ENTITY 엘리먼트들 파싱
        NodeList entities = document.getElementsByTagName("ENTITY");
        log.info("UN XML: Found {} ENTITY elements", entities.getLength());
        
        for (int i = 0; i < entities.getLength(); i++) {
            try {
                Element entity = (Element) entities.item(i);
                ParsedSanctionsData data = parseEntity(entity);
                if (data != null) {
                    result.add(data);
                }
            } catch (Exception e) {
                log.warn("Failed to parse ENTITY at index {}: {}", i, e.getMessage());
            }
        }
        
        log.info("UN XML: Successfully parsed {} total entries", result.size());
        return result;
    }
    
    private ParsedSanctionsData parseIndividual(Element individual) {
        String dataId = getElementText(individual, "DATAID");
        if (dataId == null || dataId.isEmpty()) {
            log.debug("Skipping individual with no DATAID");
            return null;
        }
        
        ParsedSanctionsData.ParsedSanctionsDataBuilder dataBuilder = ParsedSanctionsData.builder()
                .sourceUid("UN-" + dataId)
                .sourceFile(SOURCE_FILE)
                .entityType("Individual")
                .sanctionListType(SANCTION_LIST_TYPE);
        
        // 이름 조합
        String primaryName = buildIndividualName(individual);
        dataBuilder.primaryName(primaryName);
        
        // 이름 목록 구성
        List<ParsedSanctionsData.ParsedName> names = new ArrayList<>();
        names.add(ParsedSanctionsData.ParsedName.builder()
                .nameType("Primary")
                .fullName(primaryName)
                .script("Latin")
                .qualityScore(100)
                .firstName(getElementText(individual, "FIRST_NAME"))
                .middleName(getElementText(individual, "SECOND_NAME"))
                .lastName(getElementText(individual, "THIRD_NAME"))
                .build());
        
        // 별칭 파싱
        NodeList aliases = individual.getElementsByTagName("INDIVIDUAL_ALIAS");
        for (int i = 0; i < aliases.getLength(); i++) {
            Element alias = (Element) aliases.item(i);
            ParsedSanctionsData.ParsedName aliasName = parseAlias(alias);
            if (aliasName != null) {
                names.add(aliasName);
            }
        }
        dataBuilder.names(names);
        
        // 생년월일
        NodeList dobs = individual.getElementsByTagName("INDIVIDUAL_DATE_OF_BIRTH");
        if (dobs.getLength() > 0) {
            Element dob = (Element) dobs.item(0);
            String dateStr = getElementText(dob, "DATE");
            if (dateStr == null || dateStr.isEmpty()) {
                dateStr = getElementText(dob, "YEAR");
            }
            dataBuilder.birthDate(parseDate(dateStr));
        }
        
        // 성별
        String gender = getElementText(individual, "GENDER");
        if (gender != null && !gender.isEmpty()) {
            dataBuilder.gender(gender);
        }
        
        // 국적
        List<String> nationalities = new ArrayList<>();
        NodeList nationalityNodes = individual.getElementsByTagName("NATIONALITY");
        for (int i = 0; i < nationalityNodes.getLength(); i++) {
            Element nat = (Element) nationalityNodes.item(i);
            String value = getElementText(nat, "VALUE");
            if (value != null && !value.isEmpty()) {
                nationalities.add(value);
            }
        }
        if (!nationalities.isEmpty()) {
            dataBuilder.nationality(String.join(",", nationalities));
        }
        
        // 주소
        List<ParsedSanctionsData.ParsedAddress> addresses = new ArrayList<>();
        NodeList addressNodes = individual.getElementsByTagName("INDIVIDUAL_ADDRESS");
        for (int i = 0; i < addressNodes.getLength(); i++) {
            Element addr = (Element) addressNodes.item(i);
            ParsedSanctionsData.ParsedAddress address = parseAddress(addr);
            if (address != null) {
                addresses.add(address);
            }
        }
        dataBuilder.addresses(addresses);
        
        // 문서
        List<ParsedSanctionsData.ParsedDocument> documents = new ArrayList<>();
        NodeList docNodes = individual.getElementsByTagName("INDIVIDUAL_DOCUMENT");
        for (int i = 0; i < docNodes.getLength(); i++) {
            Element doc = (Element) docNodes.item(i);
            ParsedSanctionsData.ParsedDocument document = parseDocument(doc);
            if (document != null) {
                documents.add(document);
            }
        }
        dataBuilder.documents(documents);
        
        // 추가 정보
        Map<String, Object> additionalFeatures = new HashMap<>();
        
        String unListType = getElementText(individual, "UN_LIST_TYPE");
        if (unListType != null) {
            additionalFeatures.put("unListType", unListType);
        }
        
        String referenceNumber = getElementText(individual, "REFERENCE_NUMBER");
        if (referenceNumber != null) {
            additionalFeatures.put("referenceNumber", referenceNumber);
        }
        
        String listedOn = getElementText(individual, "LISTED_ON");
        if (listedOn != null) {
            additionalFeatures.put("listedOn", listedOn);
        }
        
        String comments = getElementText(individual, "COMMENTS1");
        if (comments != null) {
            additionalFeatures.put("comments", comments);
        }
        
        // 출생지
        NodeList pobs = individual.getElementsByTagName("INDIVIDUAL_PLACE_OF_BIRTH");
        if (pobs.getLength() > 0) {
            Element pob = (Element) pobs.item(0);
            String city = getElementText(pob, "CITY");
            String country = getElementText(pob, "COUNTRY");
            if (city != null || country != null) {
                String placeOfBirth = (city != null ? city : "") + 
                        (city != null && country != null ? ", " : "") + 
                        (country != null ? country : "");
                additionalFeatures.put("placeOfBirth", placeOfBirth);
            }
        }
        
        // 직위/직함
        NodeList designations = individual.getElementsByTagName("DESIGNATION");
        List<String> designationList = new ArrayList<>();
        for (int i = 0; i < designations.getLength(); i++) {
            Element designation = (Element) designations.item(i);
            String value = getElementText(designation, "VALUE");
            if (value != null && !value.isEmpty()) {
                designationList.add(value);
            }
        }
        if (!designationList.isEmpty()) {
            additionalFeatures.put("designations", designationList);
        }
        
        dataBuilder.additionalFeatures(additionalFeatures);
        
        return dataBuilder.build();
    }
    
    private ParsedSanctionsData parseEntity(Element entity) {
        String dataId = getElementText(entity, "DATAID");
        if (dataId == null || dataId.isEmpty()) {
            log.debug("Skipping entity with no DATAID");
            return null;
        }
        
        ParsedSanctionsData.ParsedSanctionsDataBuilder dataBuilder = ParsedSanctionsData.builder()
                .sourceUid("UN-" + dataId)
                .sourceFile(SOURCE_FILE)
                .entityType("Entity")
                .sanctionListType(SANCTION_LIST_TYPE);
        
        // 엔티티 이름
        String primaryName = getElementText(entity, "FIRST_NAME");
        if (primaryName == null || primaryName.isEmpty()) {
            primaryName = getElementText(entity, "NAME");
        }
        dataBuilder.primaryName(primaryName);
        
        // 이름 목록
        List<ParsedSanctionsData.ParsedName> names = new ArrayList<>();
        names.add(ParsedSanctionsData.ParsedName.builder()
                .nameType("Primary")
                .fullName(primaryName)
                .script("Latin")
                .qualityScore(100)
                .build());
        
        // 별칭 파싱
        NodeList aliases = entity.getElementsByTagName("ENTITY_ALIAS");
        for (int i = 0; i < aliases.getLength(); i++) {
            Element alias = (Element) aliases.item(i);
            ParsedSanctionsData.ParsedName aliasName = parseAlias(alias);
            if (aliasName != null) {
                names.add(aliasName);
            }
        }
        dataBuilder.names(names);
        
        // 주소
        List<ParsedSanctionsData.ParsedAddress> addresses = new ArrayList<>();
        NodeList addressNodes = entity.getElementsByTagName("ENTITY_ADDRESS");
        for (int i = 0; i < addressNodes.getLength(); i++) {
            Element addr = (Element) addressNodes.item(i);
            ParsedSanctionsData.ParsedAddress address = parseAddress(addr);
            if (address != null) {
                addresses.add(address);
            }
        }
        dataBuilder.addresses(addresses);
        
        // 추가 정보
        Map<String, Object> additionalFeatures = new HashMap<>();
        
        String unListType = getElementText(entity, "UN_LIST_TYPE");
        if (unListType != null) {
            additionalFeatures.put("unListType", unListType);
        }
        
        String referenceNumber = getElementText(entity, "REFERENCE_NUMBER");
        if (referenceNumber != null) {
            additionalFeatures.put("referenceNumber", referenceNumber);
        }
        
        String listedOn = getElementText(entity, "LISTED_ON");
        if (listedOn != null) {
            additionalFeatures.put("listedOn", listedOn);
        }
        
        String comments = getElementText(entity, "COMMENTS1");
        if (comments != null) {
            additionalFeatures.put("comments", comments);
        }
        
        dataBuilder.additionalFeatures(additionalFeatures);
        
        return dataBuilder.build();
    }
    
    private String buildIndividualName(Element individual) {
        StringBuilder name = new StringBuilder();
        
        String firstName = getElementText(individual, "FIRST_NAME");
        String secondName = getElementText(individual, "SECOND_NAME");
        String thirdName = getElementText(individual, "THIRD_NAME");
        String fourthName = getElementText(individual, "FOURTH_NAME");
        
        if (firstName != null && !firstName.isEmpty()) {
            name.append(firstName);
        }
        if (secondName != null && !secondName.isEmpty()) {
            if (name.length() > 0) name.append(" ");
            name.append(secondName);
        }
        if (thirdName != null && !thirdName.isEmpty()) {
            if (name.length() > 0) name.append(" ");
            name.append(thirdName);
        }
        if (fourthName != null && !fourthName.isEmpty()) {
            if (name.length() > 0) name.append(" ");
            name.append(fourthName);
        }
        
        return name.toString().trim();
    }
    
    private ParsedSanctionsData.ParsedName parseAlias(Element alias) {
        String aliasName = getElementText(alias, "ALIAS_NAME");
        if (aliasName == null || aliasName.isEmpty()) {
            aliasName = getElementText(alias, "NAME");
        }
        if (aliasName == null || aliasName.isEmpty()) {
            return null;
        }
        
        String quality = getElementText(alias, "QUALITY");
        String nameType;
        int qualityScore;
        
        if ("Low".equalsIgnoreCase(quality) || "a.k.a.".equalsIgnoreCase(quality)) {
            nameType = "Low Quality AKA";
            qualityScore = 50;
        } else if ("Good".equalsIgnoreCase(quality) || "High".equalsIgnoreCase(quality)) {
            nameType = "AKA";
            qualityScore = 100;
        } else {
            nameType = "AKA";
            qualityScore = 75;
        }
        
        return ParsedSanctionsData.ParsedName.builder()
                .nameType(nameType)
                .fullName(aliasName)
                .script("Latin")
                .qualityScore(qualityScore)
                .build();
    }
    
    private ParsedSanctionsData.ParsedAddress parseAddress(Element addr) {
        StringBuilder fullAddress = new StringBuilder();
        
        String street = getElementText(addr, "STREET");
        String city = getElementText(addr, "CITY");
        String stateProvince = getElementText(addr, "STATE_PROVINCE");
        String zipCode = getElementText(addr, "ZIP_CODE");
        String country = getElementText(addr, "COUNTRY");
        String note = getElementText(addr, "NOTE");
        
        if (street != null && !street.isEmpty()) {
            fullAddress.append(street);
        }
        if (city != null && !city.isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(city);
        }
        if (stateProvince != null && !stateProvince.isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(stateProvince);
        }
        if (country != null && !country.isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(country);
        }
        
        // 빈 주소는 반환하지 않음
        if (fullAddress.length() == 0 && (country == null || country.isEmpty())) {
            return null;
        }
        
        return ParsedSanctionsData.ParsedAddress.builder()
                .fullAddress(fullAddress.toString())
                .street(street)
                .city(city)
                .stateProvince(stateProvince)
                .postalCode(zipCode)
                .country(country)
                .note(note)
                .build();
    }
    
    private ParsedSanctionsData.ParsedDocument parseDocument(Element doc) {
        String docType = getElementText(doc, "TYPE_OF_DOCUMENT");
        String docNumber = getElementText(doc, "NUMBER");
        
        // 문서 번호가 없으면 반환하지 않음
        if (docNumber == null || docNumber.isEmpty()) {
            return null;
        }
        
        String issuingCountry = getElementText(doc, "ISSUING_COUNTRY");
        String dateOfIssue = getElementText(doc, "DATE_OF_ISSUE");
        String cityOfIssue = getElementText(doc, "CITY_OF_ISSUE");
        String note = getElementText(doc, "NOTE");
        
        ParsedSanctionsData.ParsedDocument.ParsedDocumentBuilder builder = 
                ParsedSanctionsData.ParsedDocument.builder()
                .documentType(docType != null ? docType : "Other")
                .documentNumber(docNumber)
                .issuingCountry(issuingCountry)
                .issueDate(parseDate(dateOfIssue));
        
        // note에 발급 도시 정보 포함
        if (cityOfIssue != null && !cityOfIssue.isEmpty()) {
            note = (note != null ? note + "; " : "") + "Issued in: " + cityOfIssue;
        }
        builder.note(note);
        
        return builder.build();
    }
    
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        
        // 여러 날짜 포맷 시도
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                // 연도만 있는 경우
                if (dateStr.matches("\\d{4}")) {
                    return LocalDate.of(Integer.parseInt(dateStr), 1, 1);
                }
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // 다음 포맷터 시도
            }
        }
        log.debug("Failed to parse date: {}", dateStr);
        return null;
    }
    
    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            Node node = nodes.item(0);
            // 직접 자식인지 확인 (다른 중첩 요소의 자식이 아닌지)
            if (node.getParentNode() == parent) {
                String text = node.getTextContent();
                return text != null ? text.trim() : null;
            }
            // 첫 번째 발견된 요소 사용
            String text = node.getTextContent();
            return text != null ? text.trim() : null;
        }
        return null;
    }

    @Override
    public String getSourceFile() {
        return SOURCE_FILE;
    }
}
