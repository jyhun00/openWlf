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

@DisplayName("UnXmlParser 테스트")
class UnXmlParserTest {

    private UnXmlParser parser;

    @BeforeEach
    void setUp() {
        parser = new UnXmlParser();
    }

    @Test
    @DisplayName("소스 파일명은 'UN'이다")
    void shouldReturnUnAsSourceFile() {
        assertThat(parser.getSourceFile()).isEqualTo("UN");
    }

    @Nested
    @DisplayName("parse() - Individual 파싱")
    class ParseIndividual {

        @Test
        @DisplayName("기본 Individual 정보를 파싱한다")
        void shouldParseBasicIndividualInfo() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <CONSOLIDATED_LIST>
                    <INDIVIDUALS>
                        <INDIVIDUAL>
                            <DATAID>12345</DATAID>
                            <FIRST_NAME>John</FIRST_NAME>
                            <SECOND_NAME>William</SECOND_NAME>
                            <THIRD_NAME>Doe</THIRD_NAME>
                            <UN_LIST_TYPE>Al-Qaida</UN_LIST_TYPE>
                            <REFERENCE_NUMBER>QDi.001</REFERENCE_NUMBER>
                            <LISTED_ON>2001-01-25</LISTED_ON>
                            <GENDER>Male</GENDER>
                        </INDIVIDUAL>
                    </INDIVIDUALS>
                </CONSOLIDATED_LIST>
                """;
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            ParsedSanctionsData data = result.get(0);
            assertThat(data.getSourceUid()).isEqualTo("UN-12345");
            assertThat(data.getSourceFile()).isEqualTo("UN");
            assertThat(data.getEntityType()).isEqualTo("Individual");
            assertThat(data.getPrimaryName()).isEqualTo("John William Doe");
            assertThat(data.getGender()).isEqualTo("Male");
            assertThat(data.getSanctionListType()).isEqualTo("UN Security Council Consolidated List");
        }

        @Test
        @DisplayName("생년월일을 파싱한다")
        void shouldParseDateOfBirth() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <CONSOLIDATED_LIST>
                    <INDIVIDUALS>
                        <INDIVIDUAL>
                            <DATAID>12345</DATAID>
                            <FIRST_NAME>Test</FIRST_NAME>
                            <INDIVIDUAL_DATE_OF_BIRTH>
                                <DATE>1965-03-15</DATE>
                            </INDIVIDUAL_DATE_OF_BIRTH>
                        </INDIVIDUAL>
                    </INDIVIDUALS>
                </CONSOLIDATED_LIST>
                """;
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getBirthDate()).isEqualTo(LocalDate.of(1965, 3, 15));
        }

        @Test
        @DisplayName("연도만 있는 생년월일을 파싱한다")
        void shouldParseBirthYear() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <CONSOLIDATED_LIST>
                    <INDIVIDUALS>
                        <INDIVIDUAL>
                            <DATAID>12345</DATAID>
                            <FIRST_NAME>Test</FIRST_NAME>
                            <INDIVIDUAL_DATE_OF_BIRTH>
                                <YEAR>1970</YEAR>
                            </INDIVIDUAL_DATE_OF_BIRTH>
                        </INDIVIDUAL>
                    </INDIVIDUALS>
                </CONSOLIDATED_LIST>
                """;
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getBirthDate()).isEqualTo(LocalDate.of(1970, 1, 1));
        }

        @Test
        @DisplayName("국적을 파싱한다")
        void shouldParseNationality() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <CONSOLIDATED_LIST>
                    <INDIVIDUALS>
                        <INDIVIDUAL>
                            <DATAID>12345</DATAID>
                            <FIRST_NAME>Test</FIRST_NAME>
                            <NATIONALITY>
                                <VALUE>North Korea</VALUE>
                            </NATIONALITY>
                            <NATIONALITY>
                                <VALUE>Russia</VALUE>
                            </NATIONALITY>
                        </INDIVIDUAL>
                    </INDIVIDUALS>
                </CONSOLIDATED_LIST>
                """;
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getNationality()).isEqualTo("North Korea,Russia");
        }

        @Test
        @DisplayName("별칭(Alias)을 파싱한다")
        void shouldParseAliases() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <CONSOLIDATED_LIST>
                    <INDIVIDUALS>
                        <INDIVIDUAL>
                            <DATAID>12345</DATAID>
                            <FIRST_NAME>John</FIRST_NAME>
                            <THIRD_NAME>Doe</THIRD_NAME>
                            <INDIVIDUAL_ALIAS>
                                <QUALITY>Good</QUALITY>
                                <ALIAS_NAME>Johnny D</ALIAS_NAME>
                            </INDIVIDUAL_ALIAS>
                            <INDIVIDUAL_ALIAS>
                                <QUALITY>Low</QUALITY>
                                <ALIAS_NAME>J.D.</ALIAS_NAME>
                            </INDIVIDUAL_ALIAS>
                        </INDIVIDUAL>
                    </INDIVIDUALS>
                </CONSOLIDATED_LIST>
                """;
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            List<ParsedSanctionsData.ParsedName> names = result.get(0).getNames();
            assertThat(names).hasSize(3); // Primary + 2 AKAs
            
            assertThat(names).anyMatch(n -> 
                "Primary".equals(n.getNameType()) && "John Doe".equals(n.getFullName()));
            assertThat(names).anyMatch(n -> 
                "AKA".equals(n.getNameType()) && "Johnny D".equals(n.getFullName()) && n.getQualityScore() == 100);
            assertThat(names).anyMatch(n -> 
                "Low Quality AKA".equals(n.getNameType()) && "J.D.".equals(n.getFullName()) && n.getQualityScore() == 50);
        }

        @Test
        @DisplayName("주소를 파싱한다")
        void shouldParseAddress() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <CONSOLIDATED_LIST>
                    <INDIVIDUALS>
                        <INDIVIDUAL>
                            <DATAID>12345</DATAID>
                            <FIRST_NAME>Test</FIRST_NAME>
                            <INDIVIDUAL_ADDRESS>
                                <STREET>123 Main Street</STREET>
                                <CITY>New York</CITY>
                                <STATE_PROVINCE>NY</STATE_PROVINCE>
                                <ZIP_CODE>10001</ZIP_CODE>
                                <COUNTRY>United States</COUNTRY>
                            </INDIVIDUAL_ADDRESS>
                        </INDIVIDUAL>
                    </INDIVIDUALS>
                </CONSOLIDATED_LIST>
                """;
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            List<ParsedSanctionsData.ParsedAddress> addresses = result.get(0).getAddresses();
            assertThat(addresses).hasSize(1);
            
            ParsedSanctionsData.ParsedAddress address = addresses.get(0);
            assertThat(address.getStreet()).isEqualTo("123 Main Street");
            assertThat(address.getCity()).isEqualTo("New York");
            assertThat(address.getStateProvince()).isEqualTo("NY");
            assertThat(address.getPostalCode()).isEqualTo("10001");
            assertThat(address.getCountry()).isEqualTo("United States");
            assertThat(address.getFullAddress()).contains("123 Main Street", "New York", "NY", "United States");
        }

        @Test
        @DisplayName("문서를 파싱한다")
        void shouldParseDocuments() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <CONSOLIDATED_LIST>
                    <INDIVIDUALS>
                        <INDIVIDUAL>
                            <DATAID>12345</DATAID>
                            <FIRST_NAME>Test</FIRST_NAME>
                            <INDIVIDUAL_DOCUMENT>
                                <TYPE_OF_DOCUMENT>Passport</TYPE_OF_DOCUMENT>
                                <NUMBER>AB123456</NUMBER>
                                <ISSUING_COUNTRY>North Korea</ISSUING_COUNTRY>
                                <DATE_OF_ISSUE>2020-01-15</DATE_OF_ISSUE>
                            </INDIVIDUAL_DOCUMENT>
                        </INDIVIDUAL>
                    </INDIVIDUALS>
                </CONSOLIDATED_LIST>
                """;
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            List<ParsedSanctionsData.ParsedDocument> documents = result.get(0).getDocuments();
            assertThat(documents).hasSize(1);
            
            ParsedSanctionsData.ParsedDocument doc = documents.get(0);
            assertThat(doc.getDocumentType()).isEqualTo("Passport");
            assertThat(doc.getDocumentNumber()).isEqualTo("AB123456");
            assertThat(doc.getIssuingCountry()).isEqualTo("North Korea");
            assertThat(doc.getIssueDate()).isEqualTo(LocalDate.of(2020, 1, 15));
        }

        @Test
        @DisplayName("DATAID가 없는 Individual은 건너뛴다")
        void shouldSkipIndividualWithoutDataId() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <CONSOLIDATED_LIST>
                    <INDIVIDUALS>
                        <INDIVIDUAL>
                            <FIRST_NAME>Test</FIRST_NAME>
                        </INDIVIDUAL>
                    </INDIVIDUALS>
                </CONSOLIDATED_LIST>
                """;
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("parse() - Entity 파싱")
    class ParseEntity {

        @Test
        @DisplayName("기본 Entity 정보를 파싱한다")
        void shouldParseBasicEntityInfo() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <CONSOLIDATED_LIST>
                    <ENTITIES>
                        <ENTITY>
                            <DATAID>67890</DATAID>
                            <FIRST_NAME>Test Organization</FIRST_NAME>
                            <UN_LIST_TYPE>Taliban</UN_LIST_TYPE>
                            <REFERENCE_NUMBER>TAe.001</REFERENCE_NUMBER>
                        </ENTITY>
                    </ENTITIES>
                </CONSOLIDATED_LIST>
                """;
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            ParsedSanctionsData data = result.get(0);
            assertThat(data.getSourceUid()).isEqualTo("UN-67890");
            assertThat(data.getEntityType()).isEqualTo("Entity");
            assertThat(data.getPrimaryName()).isEqualTo("Test Organization");
        }

        @Test
        @DisplayName("Entity 별칭을 파싱한다")
        void shouldParseEntityAliases() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <CONSOLIDATED_LIST>
                    <ENTITIES>
                        <ENTITY>
                            <DATAID>67890</DATAID>
                            <FIRST_NAME>Test Org</FIRST_NAME>
                            <ENTITY_ALIAS>
                                <QUALITY>Good</QUALITY>
                                <ALIAS_NAME>TO Inc.</ALIAS_NAME>
                            </ENTITY_ALIAS>
                        </ENTITY>
                    </ENTITIES>
                </CONSOLIDATED_LIST>
                """;
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            List<ParsedSanctionsData.ParsedName> names = result.get(0).getNames();
            assertThat(names).hasSize(2);
            assertThat(names).anyMatch(n -> "AKA".equals(n.getNameType()) && "TO Inc.".equals(n.getFullName()));
        }

        @Test
        @DisplayName("Entity 주소를 파싱한다")
        void shouldParseEntityAddress() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <CONSOLIDATED_LIST>
                    <ENTITIES>
                        <ENTITY>
                            <DATAID>67890</DATAID>
                            <FIRST_NAME>Test Org</FIRST_NAME>
                            <ENTITY_ADDRESS>
                                <CITY>Tehran</CITY>
                                <COUNTRY>Iran</COUNTRY>
                            </ENTITY_ADDRESS>
                        </ENTITY>
                    </ENTITIES>
                </CONSOLIDATED_LIST>
                """;
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            List<ParsedSanctionsData.ParsedAddress> addresses = result.get(0).getAddresses();
            assertThat(addresses).hasSize(1);
            assertThat(addresses.get(0).getCity()).isEqualTo("Tehran");
            assertThat(addresses.get(0).getCountry()).isEqualTo("Iran");
        }
    }

    @Nested
    @DisplayName("parse() - 복합 테스트")
    class ParseComplex {

        @Test
        @DisplayName("Individual과 Entity를 함께 파싱한다")
        void shouldParseBothIndividualsAndEntities() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <CONSOLIDATED_LIST>
                    <INDIVIDUALS>
                        <INDIVIDUAL>
                            <DATAID>11111</DATAID>
                            <FIRST_NAME>Individual One</FIRST_NAME>
                        </INDIVIDUAL>
                        <INDIVIDUAL>
                            <DATAID>22222</DATAID>
                            <FIRST_NAME>Individual Two</FIRST_NAME>
                        </INDIVIDUAL>
                    </INDIVIDUALS>
                    <ENTITIES>
                        <ENTITY>
                            <DATAID>33333</DATAID>
                            <FIRST_NAME>Entity One</FIRST_NAME>
                        </ENTITY>
                    </ENTITIES>
                </CONSOLIDATED_LIST>
                """;
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(3);
            
            long individualCount = result.stream()
                    .filter(d -> "Individual".equals(d.getEntityType()))
                    .count();
            long entityCount = result.stream()
                    .filter(d -> "Entity".equals(d.getEntityType()))
                    .count();
            
            assertThat(individualCount).isEqualTo(2);
            assertThat(entityCount).isEqualTo(1);
        }

        @Test
        @DisplayName("빈 XML도 파싱할 수 있다")
        void shouldHandleEmptyXml() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <CONSOLIDATED_LIST>
                    <INDIVIDUALS></INDIVIDUALS>
                    <ENTITIES></ENTITIES>
                </CONSOLIDATED_LIST>
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
    @DisplayName("additionalFeatures 파싱")
    class AdditionalFeatures {

        @Test
        @DisplayName("추가 정보(unListType, referenceNumber 등)를 파싱한다")
        void shouldParseAdditionalFeatures() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <CONSOLIDATED_LIST>
                    <INDIVIDUALS>
                        <INDIVIDUAL>
                            <DATAID>12345</DATAID>
                            <FIRST_NAME>Test</FIRST_NAME>
                            <UN_LIST_TYPE>Al-Qaida</UN_LIST_TYPE>
                            <REFERENCE_NUMBER>QDi.001</REFERENCE_NUMBER>
                            <LISTED_ON>2001-01-25</LISTED_ON>
                            <COMMENTS1>Test comments</COMMENTS1>
                            <INDIVIDUAL_PLACE_OF_BIRTH>
                                <CITY>Pyongyang</CITY>
                                <COUNTRY>North Korea</COUNTRY>
                            </INDIVIDUAL_PLACE_OF_BIRTH>
                        </INDIVIDUAL>
                    </INDIVIDUALS>
                </CONSOLIDATED_LIST>
                """;
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            var features = result.get(0).getAdditionalFeatures();
            assertThat(features.get("unListType")).isEqualTo("Al-Qaida");
            assertThat(features.get("referenceNumber")).isEqualTo("QDi.001");
            assertThat(features.get("listedOn")).isEqualTo("2001-01-25");
            assertThat(features.get("comments")).isEqualTo("Test comments");
            assertThat(features.get("placeOfBirth")).isEqualTo("Pyongyang, North Korea");
        }
    }
}
