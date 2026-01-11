package aml.openwlf.batch.parser;

import aml.openwlf.batch.parser.model.ParsedSanctionsData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EuXmlParser 테스트")
class EuXmlParserTest {

    private EuXmlParser parser;

    @BeforeEach
    void setUp() {
        parser = new EuXmlParser();
    }

    @Test
    @DisplayName("소스 파일명은 'EU'이다")
    void shouldReturnEuAsSourceFile() {
        assertThat(parser.getSourceFile()).isEqualTo("EU");
    }

    @Nested
    @DisplayName("parse() - 기본 파싱")
    class BasicParsing {

        @Test
        @DisplayName("sanctionEntity를 파싱하여 기본 정보를 추출한다")
        void shouldParseSanctionEntityBasicInfo() throws Exception {
            // given
            String xml = createBasicEuXml();
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            ParsedSanctionsData data = result.get(0);
            assertThat(data.getSourceUid()).startsWith("EU-");
            assertThat(data.getSourceFile()).isEqualTo("EU");
            assertThat(data.getSanctionListType()).isEqualTo("EU Consolidated Financial Sanctions List");
        }

        @Test
        @DisplayName("euReferenceNumber가 없는 엔티티는 건너뛴다")
        void shouldSkipEntityWithoutReferenceNumber() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <export generationDate="2024-01-01">
                    <sanctionEntity>
                        <nameAlias wholeName="Test Name" strong="true"/>
                    </sanctionEntity>
                </export>
                """;
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("빈 XML도 파싱할 수 있다")
        void shouldHandleEmptyXml() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <export generationDate="2024-01-01">
                </export>
                """;
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("잘못된 XML은 예외를 던진다")
        void shouldThrowExceptionForInvalidXml() {
            // given
            String invalidXml = "This is not XML";
            InputStream inputStream = new ByteArrayInputStream(invalidXml.getBytes(StandardCharsets.UTF_8));

            // when & then
            assertThatThrownBy(() -> parser.parse(inputStream))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("parse() - Entity Type 판별")
    class EntityTypeDetermination {

        @Test
        @DisplayName("subjectType이 'P'이면 Individual로 분류한다")
        void shouldClassifyAsIndividualWhenSubjectTypeIsP() throws Exception {
            // given
            String xml = createEuXmlWithSubjectType("P");
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEntityType()).isEqualTo("Individual");
        }

        @Test
        @DisplayName("subjectType이 'E'이면 Entity로 분류한다")
        void shouldClassifyAsEntityWhenSubjectTypeIsE() throws Exception {
            // given
            String xml = createEuXmlWithSubjectType("E");
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEntityType()).isEqualTo("Entity");
        }

        @Test
        @DisplayName("subjectType이 없고 firstName/lastName이 있으면 Individual로 분류한다")
        void shouldClassifyAsIndividualWhenHasFirstLastName() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <export generationDate="2024-01-01">
                    <sanctionEntity euReferenceNumber="EU.123.45">
                        <nameAlias firstName="John" lastName="DOE" wholeName="John DOE" strong="true"/>
                    </sanctionEntity>
                </export>
                """;
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEntityType()).isEqualTo("Individual");
        }
    }

    @Nested
    @DisplayName("parse() - 이름 파싱")
    class NameParsing {

        @Test
        @DisplayName("strong=true인 nameAlias를 Primary로 파싱한다")
        void shouldParseStrongNameAliasAsPrimary() throws Exception {
            // given
            String xml = createBasicEuXml();
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPrimaryName()).isEqualTo("John DOE");
            assertThat(result.get(0).getNames())
                    .anyMatch(n -> "Primary".equals(n.getNameType()) && "John DOE".equals(n.getFullName()));
        }

        @Test
        @DisplayName("strong=false인 nameAlias를 Low Quality AKA로 파싱한다")
        void shouldParseWeakNameAliasAsLowQualityAka() throws Exception {
            // given
            String xml = createEuXmlWithMultipleAliases();
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getNames())
                    .anyMatch(n -> "Low Quality AKA".equals(n.getNameType()));
        }

        @Test
        @DisplayName("firstName, middleName, lastName을 조합하여 wholeName을 생성한다")
        void shouldCombineNameParts() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <export generationDate="2024-01-01">
                    <sanctionEntity euReferenceNumber="EU.123.45">
                        <subjectType classificationCode="P"/>
                        <nameAlias firstName="John" middleName="William" lastName="DOE" strong="true"/>
                    </sanctionEntity>
                </export>
                """;
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPrimaryName()).isEqualTo("John William DOE");
        }
    }

    @Nested
    @DisplayName("parse() - 생년월일 파싱")
    class BirthdateParsing {

        @Test
        @DisplayName("birthdate 속성에서 날짜를 파싱한다")
        void shouldParseBirthdateAttribute() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <export generationDate="2024-01-01">
                    <sanctionEntity euReferenceNumber="EU.123.45">
                        <subjectType classificationCode="P"/>
                        <nameAlias wholeName="Test Person" strong="true"/>
                        <birthdate birthdate="1990-05-15"/>
                    </sanctionEntity>
                </export>
                """;
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getBirthDate()).isEqualTo(LocalDate.of(1990, 5, 15));
        }

        @Test
        @DisplayName("year, month, day 개별 속성에서 날짜를 파싱한다")
        void shouldParseIndividualDateAttributes() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <export generationDate="2024-01-01">
                    <sanctionEntity euReferenceNumber="EU.123.45">
                        <subjectType classificationCode="P"/>
                        <nameAlias wholeName="Test Person" strong="true"/>
                        <birthdate year="1990" month="5" day="15"/>
                    </sanctionEntity>
                </export>
                """;
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getBirthDate()).isEqualTo(LocalDate.of(1990, 5, 15));
        }

        @Test
        @DisplayName("year만 있으면 1월 1일로 설정한다")
        void shouldDefaultToJanuaryFirstWhenOnlyYear() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <export generationDate="2024-01-01">
                    <sanctionEntity euReferenceNumber="EU.123.45">
                        <subjectType classificationCode="P"/>
                        <nameAlias wholeName="Test Person" strong="true"/>
                        <birthdate year="1990"/>
                    </sanctionEntity>
                </export>
                """;
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
        }
    }

    @Nested
    @DisplayName("parse() - 국적 파싱")
    class CitizenshipParsing {

        @Test
        @DisplayName("citizenship의 countryIso2Code를 파싱한다")
        void shouldParseCountryIso2Code() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <export generationDate="2024-01-01">
                    <sanctionEntity euReferenceNumber="EU.123.45">
                        <subjectType classificationCode="P"/>
                        <nameAlias wholeName="Test Person" strong="true"/>
                        <citizenship countryIso2Code="RU"/>
                    </sanctionEntity>
                </export>
                """;
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getNationality()).isEqualTo("RU");
        }

        @Test
        @DisplayName("여러 국적을 쉼표로 구분하여 저장한다")
        void shouldJoinMultipleCitizenships() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <export generationDate="2024-01-01">
                    <sanctionEntity euReferenceNumber="EU.123.45">
                        <subjectType classificationCode="P"/>
                        <nameAlias wholeName="Test Person" strong="true"/>
                        <citizenship countryIso2Code="RU"/>
                        <citizenship countryIso2Code="UA"/>
                    </sanctionEntity>
                </export>
                """;
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getNationality()).contains("RU").contains("UA");
        }
    }

    @Nested
    @DisplayName("parse() - 주소 파싱")
    class AddressParsing {

        @Test
        @DisplayName("address 요소를 파싱한다")
        void shouldParseAddress() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <export generationDate="2024-01-01">
                    <sanctionEntity euReferenceNumber="EU.123.45">
                        <subjectType classificationCode="P"/>
                        <nameAlias wholeName="Test Person" strong="true"/>
                        <address street="123 Main Street" city="Moscow" country="Russia" countryIso2Code="RU"/>
                    </sanctionEntity>
                </export>
                """;
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            List<ParsedSanctionsData.ParsedAddress> addresses = result.get(0).getAddresses();
            assertThat(addresses).hasSize(1);
            assertThat(addresses.get(0).getStreet()).isEqualTo("123 Main Street");
            assertThat(addresses.get(0).getCity()).isEqualTo("Moscow");
            assertThat(addresses.get(0).getCountry()).isEqualTo("Russia");
            assertThat(addresses.get(0).getCountryCode()).isEqualTo("RU");
        }
    }

    @Nested
    @DisplayName("parse() - 문서/ID 파싱")
    class IdentificationParsing {

        @Test
        @DisplayName("identification 요소를 파싱한다")
        void shouldParseIdentification() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <export generationDate="2024-01-01">
                    <sanctionEntity euReferenceNumber="EU.123.45">
                        <subjectType classificationCode="P"/>
                        <nameAlias wholeName="Test Person" strong="true"/>
                        <identification identificationTypeCode="passport" number="AB123456" issuedBy="Russia" countryIso2Code="RU"/>
                    </sanctionEntity>
                </export>
                """;
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            List<ParsedSanctionsData.ParsedDocument> documents = result.get(0).getDocuments();
            assertThat(documents).hasSize(1);
            assertThat(documents.get(0).getDocumentType()).isEqualTo("Passport");
            assertThat(documents.get(0).getDocumentNumber()).isEqualTo("AB123456");
            assertThat(documents.get(0).getIssuingCountry()).isEqualTo("Russia");
            assertThat(documents.get(0).getIssuingCountryCode()).isEqualTo("RU");
        }

        @Test
        @DisplayName("번호가 없는 identification은 건너뛴다")
        void shouldSkipIdentificationWithoutNumber() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <export generationDate="2024-01-01">
                    <sanctionEntity euReferenceNumber="EU.123.45">
                        <subjectType classificationCode="P"/>
                        <nameAlias wholeName="Test Person" strong="true"/>
                        <identification identificationTypeCode="passport" issuedBy="Russia"/>
                    </sanctionEntity>
                </export>
                """;
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDocuments()).isEmpty();
        }
    }

    @Nested
    @DisplayName("parse() - 제재 프로그램 파싱")
    class RegulationParsing {

        @Test
        @DisplayName("regulation의 programme을 파싱한다")
        void shouldParseRegulationProgramme() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <export generationDate="2024-01-01">
                    <sanctionEntity euReferenceNumber="EU.123.45" designationDate="2024-01-01">
                        <subjectType classificationCode="P"/>
                        <nameAlias wholeName="Test Person" strong="true"/>
                        <regulation programme="RUS" regulationType="Council Decision"/>
                    </sanctionEntity>
                </export>
                """;
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            @SuppressWarnings("unchecked")
            List<String> programs = (List<String>) result.get(0).getAdditionalFeatures().get("programs");
            assertThat(programs).contains("RUS");
        }
    }

    @Nested
    @DisplayName("parse() - 복합 테스트")
    class ComplexParsing {

        @Test
        @DisplayName("여러 sanctionEntity를 파싱한다")
        void shouldParseMultipleSanctionEntities() throws Exception {
            // given
            String xml = createEuXmlWithMultipleEntities();
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getSourceUid()).isEqualTo("EU-EU.123.45");
            assertThat(result.get(1).getSourceUid()).isEqualTo("EU-EU.678.90");
        }

        @Test
        @DisplayName("완전한 엔티티 정보를 파싱한다")
        void shouldParseCompleteEntityInformation() throws Exception {
            // given
            String xml = createCompleteEuXml();
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            ParsedSanctionsData data = result.get(0);

            assertThat(data.getSourceUid()).isEqualTo("EU-EU.999.99");
            assertThat(data.getEntityType()).isEqualTo("Individual");
            assertThat(data.getPrimaryName()).isEqualTo("Ivan PETROV");
            assertThat(data.getBirthDate()).isEqualTo(LocalDate.of(1970, 3, 25));
            assertThat(data.getNationality()).isEqualTo("RU");
            assertThat(data.getNames()).hasSizeGreaterThanOrEqualTo(2);
            assertThat(data.getAddresses()).hasSize(1);
            assertThat(data.getDocuments()).hasSize(1);
        }
    }

    // ========================================
    // Helper Methods for XML Generation
    // ========================================

    private String createBasicEuXml() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <export generationDate="2024-01-01">
                <sanctionEntity euReferenceNumber="EU.123.45" designationDate="2024-01-01">
                    <subjectType classificationCode="P"/>
                    <nameAlias firstName="John" lastName="DOE" wholeName="John DOE" strong="true"/>
                </sanctionEntity>
            </export>
            """;
    }

    private String createEuXmlWithSubjectType(String code) {
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <export generationDate="2024-01-01">
                <sanctionEntity euReferenceNumber="EU.123.45">
                    <subjectType classificationCode="%s"/>
                    <nameAlias wholeName="Test Name" strong="true"/>
                </sanctionEntity>
            </export>
            """, code);
    }

    private String createEuXmlWithMultipleAliases() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <export generationDate="2024-01-01">
                <sanctionEntity euReferenceNumber="EU.123.45">
                    <subjectType classificationCode="P"/>
                    <nameAlias wholeName="Primary Name" strong="true"/>
                    <nameAlias wholeName="Strong Alias" strong="true"/>
                    <nameAlias wholeName="Weak Alias" strong="false"/>
                </sanctionEntity>
            </export>
            """;
    }

    private String createEuXmlWithMultipleEntities() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <export generationDate="2024-01-01">
                <sanctionEntity euReferenceNumber="EU.123.45">
                    <subjectType classificationCode="P"/>
                    <nameAlias wholeName="Person One" strong="true"/>
                </sanctionEntity>
                <sanctionEntity euReferenceNumber="EU.678.90">
                    <subjectType classificationCode="E"/>
                    <nameAlias wholeName="Company One" strong="true"/>
                </sanctionEntity>
            </export>
            """;
    }

    private String createCompleteEuXml() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <export generationDate="2024-01-01">
                <sanctionEntity euReferenceNumber="EU.999.99" designationDate="2022-03-01">
                    <subjectType classificationCode="P"/>
                    <nameAlias firstName="Ivan" lastName="PETROV" wholeName="Ivan PETROV" strong="true"/>
                    <nameAlias wholeName="Ivan Petrovich PETROV" strong="true"/>
                    <nameAlias wholeName="I. Petrov" strong="false"/>
                    <citizenship countryIso2Code="RU"/>
                    <birthdate year="1970" month="3" day="25"/>
                    <address street="Kremlin" city="Moscow" country="Russia" countryIso2Code="RU"/>
                    <identification identificationTypeCode="passport" number="RU12345678" issuedBy="Russia" countryIso2Code="RU"/>
                    <regulation programme="RUS" regulationType="Council Decision" publicationDate="2022-03-01"/>
                </sanctionEntity>
            </export>
            """;
    }
}
