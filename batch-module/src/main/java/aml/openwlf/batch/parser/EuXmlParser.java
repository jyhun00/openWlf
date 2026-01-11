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
 * EU Consolidated Financial Sanctions List XML 파서
 *
 * EU Consolidated List XML 구조:
 * <export generationDate="...">
 *   <sanctionEntity euReferenceNumber="..." designationDate="..." logicalId="...">
 *     <subjectType classificationCode="P"/>  <!-- P: Person, E: Entity -->
 *     <nameAlias firstName="..." lastName="..." wholeName="..." strong="true/false"/>
 *     <citizenship region="..." countryIso2Code="..."/>
 *     <birthdate birthdate="..." day="..." month="..." year="..." circa="..."/>
 *     <address street="..." city="..." country="..." countryIso2Code="..." poBox="..."/>
 *     <identification identificationTypeCode="..." number="..." issuedBy="..." latin="..."/>
 *     <regulation regulationType="..." publicationDate="..." publicationUrl="..." programme="..."/>
 *   </sanctionEntity>
 * </export>
 */
@Slf4j
@Component
public class EuXmlParser implements SanctionsXmlParser {

    private static final String SOURCE_FILE = "EU";
    private static final String SANCTION_LIST_TYPE = "EU Consolidated Financial Sanctions List";

    private static final List<DateTimeFormatter> DATE_FORMATTERS = Arrays.asList(
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("yyyy")
    );

    @Override
    public List<ParsedSanctionsData> parse(InputStream inputStream) throws Exception {
        List<ParsedSanctionsData> result = new ArrayList<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(inputStream);

        // sanctionEntity 엘리먼트들 파싱
        NodeList entities = document.getElementsByTagName("sanctionEntity");
        log.info("EU XML: Found {} sanctionEntity elements", entities.getLength());

        for (int i = 0; i < entities.getLength(); i++) {
            try {
                Element entity = (Element) entities.item(i);
                ParsedSanctionsData data = parseSanctionEntity(entity);
                if (data != null) {
                    result.add(data);
                }
            } catch (Exception e) {
                log.warn("Failed to parse sanctionEntity at index {}: {}", i, e.getMessage());
            }
        }

        log.info("EU XML: Successfully parsed {} entities", result.size());
        return result;
    }

    private ParsedSanctionsData parseSanctionEntity(Element entity) {
        String euRefNumber = entity.getAttribute("euReferenceNumber");
        String logicalId = entity.getAttribute("logicalId");
        String sourceId = logicalId != null && !logicalId.isEmpty() ? logicalId : euRefNumber;

        if (sourceId == null || sourceId.isEmpty()) {
            log.debug("Skipping entity with no euReferenceNumber or logicalId");
            return null;
        }

        // 엔티티 유형 결정
        String entityType = determineEntityType(entity);

        ParsedSanctionsData.ParsedSanctionsDataBuilder dataBuilder = ParsedSanctionsData.builder()
                .sourceUid("EU-" + sourceId)
                .sourceFile(SOURCE_FILE)
                .entityType(entityType)
                .sanctionListType(SANCTION_LIST_TYPE);

        // 이름/별칭 파싱
        List<ParsedSanctionsData.ParsedName> names = parseNameAliases(entity);
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

        // 생년월일 파싱
        NodeList birthdates = entity.getElementsByTagName("birthdate");
        if (birthdates.getLength() > 0) {
            Element birthdate = (Element) birthdates.item(0);
            LocalDate date = parseBirthdate(birthdate);
            dataBuilder.birthDate(date);
        }

        // 성별 파싱
        String gender = parseGender(entity);
        if (gender != null) {
            dataBuilder.gender(gender);
        }

        // 국적 파싱
        List<String> nationalities = parseCitizenships(entity);
        if (!nationalities.isEmpty()) {
            dataBuilder.nationality(String.join(",", nationalities));
        }

        // 주소 파싱
        List<ParsedSanctionsData.ParsedAddress> addresses = parseAddresses(entity);
        dataBuilder.addresses(addresses);

        // 문서/ID 파싱
        List<ParsedSanctionsData.ParsedDocument> documents = parseIdentifications(entity);
        dataBuilder.documents(documents);

        // 추가 정보
        Map<String, Object> additionalFeatures = new HashMap<>();

        // 제재 프로그램 파싱
        List<String> programs = parseRegulations(entity);
        if (!programs.isEmpty()) {
            additionalFeatures.put("programs", programs);
        }

        // 지정일
        String designationDate = entity.getAttribute("designationDate");
        if (designationDate != null && !designationDate.isEmpty()) {
            additionalFeatures.put("designationDate", designationDate);
        }

        // EU Reference Number
        if (euRefNumber != null && !euRefNumber.isEmpty()) {
            additionalFeatures.put("euReferenceNumber", euRefNumber);
        }

        // 비고/설명 파싱
        NodeList remarks = entity.getElementsByTagName("remark");
        List<String> remarkList = new ArrayList<>();
        for (int i = 0; i < remarks.getLength(); i++) {
            String remark = getTextContent((Element) remarks.item(i));
            if (remark != null && !remark.isEmpty()) {
                remarkList.add(remark);
            }
        }
        if (!remarkList.isEmpty()) {
            additionalFeatures.put("remarks", remarkList);
        }

        dataBuilder.additionalFeatures(additionalFeatures);

        return dataBuilder.build();
    }

    private String determineEntityType(Element entity) {
        NodeList subjectTypes = entity.getElementsByTagName("subjectType");
        if (subjectTypes.getLength() > 0) {
            Element subjectType = (Element) subjectTypes.item(0);
            String code = subjectType.getAttribute("classificationCode");
            if ("P".equalsIgnoreCase(code) || "person".equalsIgnoreCase(code)) {
                return "Individual";
            } else if ("E".equalsIgnoreCase(code) || "enterprise".equalsIgnoreCase(code)) {
                return "Entity";
            }
        }

        // subjectType이 없는 경우 nameAlias 구조로 추정
        NodeList nameAliases = entity.getElementsByTagName("nameAlias");
        if (nameAliases.getLength() > 0) {
            Element nameAlias = (Element) nameAliases.item(0);
            String firstName = nameAlias.getAttribute("firstName");
            String lastName = nameAlias.getAttribute("lastName");
            // firstName과 lastName이 있으면 Individual로 추정
            if ((firstName != null && !firstName.isEmpty()) ||
                (lastName != null && !lastName.isEmpty())) {
                return "Individual";
            }
        }

        return "Entity"; // 기본값
    }

    private List<ParsedSanctionsData.ParsedName> parseNameAliases(Element entity) {
        List<ParsedSanctionsData.ParsedName> names = new ArrayList<>();

        NodeList nameAliases = entity.getElementsByTagName("nameAlias");
        boolean hasPrimary = false;

        for (int i = 0; i < nameAliases.getLength(); i++) {
            Element nameAlias = (Element) nameAliases.item(i);

            String wholeName = nameAlias.getAttribute("wholeName");
            String firstName = nameAlias.getAttribute("firstName");
            String middleName = nameAlias.getAttribute("middleName");
            String lastName = nameAlias.getAttribute("lastName");
            String strong = nameAlias.getAttribute("strong");
            String function = nameAlias.getAttribute("function");
            String regulationLanguage = nameAlias.getAttribute("regulationLanguage");

            // wholeName이 없으면 firstName, lastName 조합
            if (wholeName == null || wholeName.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                if (firstName != null && !firstName.isEmpty()) {
                    sb.append(firstName);
                }
                if (middleName != null && !middleName.isEmpty()) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(middleName);
                }
                if (lastName != null && !lastName.isEmpty()) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(lastName);
                }
                wholeName = sb.toString().trim();
            }

            if (wholeName.isEmpty()) {
                continue;
            }

            // 이름 유형 결정
            String nameType;
            int qualityScore;

            boolean isStrong = "true".equalsIgnoreCase(strong);
            boolean isPrimary = !hasPrimary && isStrong && (function == null || function.isEmpty());

            if (isPrimary) {
                nameType = "Primary";
                qualityScore = 100;
                hasPrimary = true;
            } else if (isStrong) {
                nameType = "AKA";
                qualityScore = 100;
            } else {
                nameType = "Low Quality AKA";
                qualityScore = 50;
            }

            // 스크립트 판단
            String script = determineScript(wholeName, regulationLanguage);

            names.add(ParsedSanctionsData.ParsedName.builder()
                    .nameType(nameType)
                    .fullName(wholeName)
                    .firstName(firstName != null && !firstName.isEmpty() ? firstName : null)
                    .middleName(middleName != null && !middleName.isEmpty() ? middleName : null)
                    .lastName(lastName != null && !lastName.isEmpty() ? lastName : null)
                    .script(script)
                    .qualityScore(qualityScore)
                    .build());
        }

        // Primary가 없으면 첫 번째 이름을 Primary로 설정
        if (!names.isEmpty() && names.stream().noneMatch(n -> "Primary".equals(n.getNameType()))) {
            names.get(0).setNameType("Primary");
            names.get(0).setQualityScore(100);
        }

        return names;
    }

    private String determineScript(String name, String language) {
        if (language != null) {
            String lang = language.toLowerCase();
            if (lang.contains("arab")) return "Arabic";
            if (lang.contains("cyril") || lang.contains("rus")) return "Cyrillic";
            if (lang.contains("chin") || lang.contains("zh")) return "Chinese";
            if (lang.contains("korea") || lang.contains("ko")) return "Korean";
            if (lang.contains("japan") || lang.contains("ja")) return "Japanese";
        }

        // 문자 분석으로 스크립트 추정
        if (name != null) {
            for (char c : name.toCharArray()) {
                if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.ARABIC) {
                    return "Arabic";
                }
                if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CYRILLIC) {
                    return "Cyrillic";
                }
                if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                    return "Chinese";
                }
                if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HANGUL_SYLLABLES) {
                    return "Korean";
                }
            }
        }

        return "Latin";
    }

    private LocalDate parseBirthdate(Element birthdate) {
        // 먼저 birthdate 속성에서 전체 날짜 시도
        String dateStr = birthdate.getAttribute("birthdate");
        if (dateStr != null && !dateStr.isEmpty()) {
            LocalDate parsed = parseDate(dateStr);
            if (parsed != null) {
                return parsed;
            }
        }

        // 개별 속성에서 조합
        String year = birthdate.getAttribute("year");
        String month = birthdate.getAttribute("month");
        String day = birthdate.getAttribute("day");

        if (year != null && !year.isEmpty()) {
            try {
                int y = Integer.parseInt(year);
                int m = 1;
                int d = 1;

                if (month != null && !month.isEmpty()) {
                    m = Integer.parseInt(month);
                    if (day != null && !day.isEmpty()) {
                        d = Integer.parseInt(day);
                    }
                }

                return LocalDate.of(y, m, d);
            } catch (NumberFormatException e) {
                log.debug("Failed to parse birthdate year: {}", year);
            }
        }

        return null;
    }

    private String parseGender(Element entity) {
        NodeList genders = entity.getElementsByTagName("gender");
        if (genders.getLength() > 0) {
            Element gender = (Element) genders.item(0);
            String genderCode = gender.getAttribute("genderCode");
            if (genderCode != null && !genderCode.isEmpty()) {
                if ("M".equalsIgnoreCase(genderCode) || "male".equalsIgnoreCase(genderCode)) {
                    return "Male";
                } else if ("F".equalsIgnoreCase(genderCode) || "female".equalsIgnoreCase(genderCode)) {
                    return "Female";
                }
            }

            String genderText = getTextContent(gender);
            if (genderText != null && !genderText.isEmpty()) {
                return genderText;
            }
        }
        return null;
    }

    private List<String> parseCitizenships(Element entity) {
        List<String> citizenships = new ArrayList<>();

        NodeList nodes = entity.getElementsByTagName("citizenship");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element citizenship = (Element) nodes.item(i);

            // countryIso2Code 우선
            String iso2 = citizenship.getAttribute("countryIso2Code");
            if (iso2 != null && !iso2.isEmpty()) {
                if (!citizenships.contains(iso2)) {
                    citizenships.add(iso2);
                }
                continue;
            }

            // region 또는 country 속성
            String region = citizenship.getAttribute("region");
            if (region != null && !region.isEmpty()) {
                if (!citizenships.contains(region)) {
                    citizenships.add(region);
                }
                continue;
            }

            String country = citizenship.getAttribute("country");
            if (country != null && !country.isEmpty()) {
                if (!citizenships.contains(country)) {
                    citizenships.add(country);
                }
            }
        }

        return citizenships;
    }

    private List<ParsedSanctionsData.ParsedAddress> parseAddresses(Element entity) {
        List<ParsedSanctionsData.ParsedAddress> addresses = new ArrayList<>();

        NodeList addressNodes = entity.getElementsByTagName("address");
        for (int i = 0; i < addressNodes.getLength(); i++) {
            Element addr = (Element) addressNodes.item(i);
            ParsedSanctionsData.ParsedAddress address = parseAddress(addr);
            if (address != null) {
                addresses.add(address);
            }
        }

        return addresses;
    }

    private ParsedSanctionsData.ParsedAddress parseAddress(Element addr) {
        String street = addr.getAttribute("street");
        String city = addr.getAttribute("city");
        String region = addr.getAttribute("region");
        String zipCode = addr.getAttribute("zipCode");
        String poBox = addr.getAttribute("poBox");
        String country = addr.getAttribute("country");
        String countryIso2 = addr.getAttribute("countryIso2Code");
        String place = addr.getAttribute("place");

        StringBuilder fullAddress = new StringBuilder();

        if (street != null && !street.isEmpty()) {
            fullAddress.append(street);
        }
        if (poBox != null && !poBox.isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append("P.O. Box ").append(poBox);
        }
        if (city != null && !city.isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(city);
        }
        if (place != null && !place.isEmpty() && !place.equals(city)) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(place);
        }
        if (region != null && !region.isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(region);
        }
        if (zipCode != null && !zipCode.isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(" ");
            fullAddress.append(zipCode);
        }
        if (country != null && !country.isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(country);
        }

        // 빈 주소는 반환하지 않음
        if (fullAddress.length() == 0 &&
            (countryIso2 == null || countryIso2.isEmpty()) &&
            (country == null || country.isEmpty())) {
            return null;
        }

        return ParsedSanctionsData.ParsedAddress.builder()
                .fullAddress(fullAddress.toString())
                .street(street != null && !street.isEmpty() ? street : null)
                .city(city != null && !city.isEmpty() ? city : null)
                .stateProvince(region != null && !region.isEmpty() ? region : null)
                .postalCode(zipCode != null && !zipCode.isEmpty() ? zipCode : null)
                .country(country != null && !country.isEmpty() ? country : null)
                .countryCode(countryIso2 != null && !countryIso2.isEmpty() ? countryIso2 : null)
                .build();
    }

    private List<ParsedSanctionsData.ParsedDocument> parseIdentifications(Element entity) {
        List<ParsedSanctionsData.ParsedDocument> documents = new ArrayList<>();

        NodeList identNodes = entity.getElementsByTagName("identification");
        for (int i = 0; i < identNodes.getLength(); i++) {
            Element ident = (Element) identNodes.item(i);
            ParsedSanctionsData.ParsedDocument doc = parseIdentification(ident);
            if (doc != null) {
                documents.add(doc);
            }
        }

        return documents;
    }

    private ParsedSanctionsData.ParsedDocument parseIdentification(Element ident) {
        String docType = ident.getAttribute("identificationTypeCode");
        String docNumber = ident.getAttribute("number");
        String issuedBy = ident.getAttribute("issuedBy");
        String countryIso2 = ident.getAttribute("countryIso2Code");
        String issueDate = ident.getAttribute("issueDate");
        String expiryDate = ident.getAttribute("expiryDate");
        String nameOnDocument = ident.getAttribute("nameOnDocument");
        String latin = ident.getAttribute("latin");

        // 문서 번호가 없으면 반환하지 않음
        if (docNumber == null || docNumber.isEmpty()) {
            return null;
        }

        // 문서 유형 매핑
        String mappedDocType = mapDocumentType(docType);

        ParsedSanctionsData.ParsedDocument.ParsedDocumentBuilder builder =
                ParsedSanctionsData.ParsedDocument.builder()
                .documentType(mappedDocType)
                .documentNumber(docNumber)
                .issuingCountry(issuedBy != null && !issuedBy.isEmpty() ? issuedBy : null)
                .issuingCountryCode(countryIso2 != null && !countryIso2.isEmpty() ? countryIso2 : null)
                .issueDate(parseDate(issueDate))
                .expiryDate(parseDate(expiryDate));

        // note에 추가 정보
        StringBuilder note = new StringBuilder();
        if (nameOnDocument != null && !nameOnDocument.isEmpty()) {
            note.append("Name on document: ").append(nameOnDocument);
        }
        if (latin != null && !latin.isEmpty()) {
            if (note.length() > 0) note.append("; ");
            note.append("Latin: ").append(latin);
        }
        if (note.length() > 0) {
            builder.note(note.toString());
        }

        return builder.build();
    }

    private String mapDocumentType(String docType) {
        if (docType == null || docType.isEmpty()) {
            return "Other";
        }

        String lower = docType.toLowerCase();
        if (lower.contains("passport")) {
            return "Passport";
        } else if (lower.contains("national") && lower.contains("id")) {
            return "National ID";
        } else if (lower.contains("driver")) {
            return "Driver's License";
        } else if (lower.contains("tax")) {
            return "Tax ID";
        } else if (lower.contains("social") || lower.contains("ssn")) {
            return "Social Security";
        } else if (lower.contains("registration")) {
            return "Registration Number";
        }

        return docType;
    }

    private List<String> parseRegulations(Element entity) {
        List<String> programs = new ArrayList<>();

        NodeList regulations = entity.getElementsByTagName("regulation");
        for (int i = 0; i < regulations.getLength(); i++) {
            Element regulation = (Element) regulations.item(i);

            String programme = regulation.getAttribute("programme");
            if (programme != null && !programme.isEmpty()) {
                if (!programs.contains(programme)) {
                    programs.add(programme);
                }
            }

            // regulationType도 추가
            String regulationType = regulation.getAttribute("regulationType");
            if (regulationType != null && !regulationType.isEmpty()) {
                String combined = programme != null && !programme.isEmpty()
                        ? programme + " (" + regulationType + ")"
                        : regulationType;
                if (!programs.contains(combined) && !programs.contains(programme)) {
                    // programme이 이미 있으면 추가하지 않음
                    if (programme == null || programme.isEmpty()) {
                        programs.add(combined);
                    }
                }
            }
        }

        return programs;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;

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
