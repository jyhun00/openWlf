package aml.openwlf.batch.parser;

import aml.openwlf.batch.parser.model.ParsedSanctionsData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OfacXmlParser 테스트")
class OfacXmlParserTest {

    private OfacXmlParser parser;

    @BeforeEach
    void setUp() {
        parser = new OfacXmlParser();
    }

    @Test
    @DisplayName("소스 파일명은 'OFAC'이다")
    void shouldReturnOfacAsSourceFile() {
        assertThat(parser.getSourceFile()).isEqualTo("OFAC");
    }

    @Nested
    @DisplayName("parse() - 기본 파싱")
    class BasicParsing {

        @Test
        @DisplayName("DistinctParty를 파싱하여 기본 정보를 추출한다")
        void shouldParseDistinctPartyBasicInfo() throws Exception {
            // given
            String xml = createBasicOfacXml();
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).isNotEmpty();
            ParsedSanctionsData data = result.get(0);
            assertThat(data.getSourceUid()).startsWith("OFAC-");
            assertThat(data.getSourceFile()).isEqualTo("OFAC");
            assertThat(data.getSanctionListType()).isEqualTo("SDN");
        }

        @Test
        @DisplayName("Profile이 없는 DistinctParty는 건너뛴다")
        void shouldSkipDistinctPartyWithoutProfile() throws Exception {
            // given
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Sanctions>
                    <DistinctParty FixedRef="123">
                    </DistinctParty>
                </Sanctions>
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
                <Sanctions>
                </Sanctions>
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
        @DisplayName("PartySubType이 'Individual'이면 Individual로 분류한다")
        void shouldClassifyAsIndividualWhenPartySubTypeContainsIndividual() throws Exception {
            // given
            String xml = createOfacXmlWithPartySubType("1", "Individual");
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEntityType()).isEqualTo("Individual");
        }

        @Test
        @DisplayName("PartySubType이 'Entity'이면 Entity로 분류한다")
        void shouldClassifyAsEntityWhenPartySubTypeContainsEntity() throws Exception {
            // given
            String xml = createOfacXmlWithPartySubType("2", "Entity");
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEntityType()).isEqualTo("Entity");
        }

        @Test
        @DisplayName("PartySubType이 'Vessel'이면 Vessel로 분류한다")
        void shouldClassifyAsVesselWhenPartySubTypeContainsVessel() throws Exception {
            // given
            String xml = createOfacXmlWithPartySubType("3", "Vessel");
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEntityType()).isEqualTo("Vessel");
        }
    }

    @Nested
    @DisplayName("parse() - 이름 파싱")
    class NameParsing {

        @Test
        @DisplayName("Primary Name을 파싱한다")
        void shouldParsePrimaryName() throws Exception {
            // given
            String xml = createOfacXmlWithIdentity(true);
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPrimaryName()).isNotNull();
            assertThat(result.get(0).getNames()).isNotEmpty();
            
            // Primary name 존재 확인
            assertThat(result.get(0).getNames())
                    .anyMatch(n -> "Primary".equals(n.getNameType()));
        }

        @Test
        @DisplayName("AKA(별칭)를 파싱한다")
        void shouldParseAliases() throws Exception {
            // given
            String xml = createOfacXmlWithMultipleAliases();
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            List<ParsedSanctionsData.ParsedName> names = result.get(0).getNames();
            assertThat(names.size()).isGreaterThanOrEqualTo(2);
        }
    }

    @Nested
    @DisplayName("parse() - 문서 파싱")
    class DocumentParsing {

        @Test
        @DisplayName("IDRegDocument를 파싱한다")
        void shouldParseIDRegDocument() throws Exception {
            // given
            String xml = createOfacXmlWithDocument();
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result).hasSize(1);
            List<ParsedSanctionsData.ParsedDocument> documents = result.get(0).getDocuments();
            assertThat(documents).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("parse() - 복합 테스트")
    class ComplexParsing {

        @Test
        @DisplayName("여러 DistinctParty를 파싱한다")
        void shouldParseMultipleDistinctParties() throws Exception {
            // given
            String xml = createOfacXmlWithMultipleParties();
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            // when
            List<ParsedSanctionsData> result = parser.parse(inputStream);

            // then
            assertThat(result.size()).isGreaterThanOrEqualTo(2);
        }
    }

    // ========================================
    // Helper Methods for XML Generation
    // ========================================

    private String createBasicOfacXml() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <Sanctions>
                <ReferenceValueSets>
                    <PartySubType ID="1">Individual</PartySubType>
                    <AliasType ID="1">A.K.A.</AliasType>
                </ReferenceValueSets>
                <DistinctParty FixedRef="12345">
                    <Profile ID="1" PartySubTypeID="1">
                        <Identity>
                            <Alias Primary="true" AliasTypeID="1">
                                <DocumentedName>
                                    <DocumentedNamePart>
                                        <NamePartValue>John Doe</NamePartValue>
                                    </DocumentedNamePart>
                                </DocumentedName>
                            </Alias>
                        </Identity>
                    </Profile>
                </DistinctParty>
            </Sanctions>
            """;
    }

    private String createOfacXmlWithPartySubType(String id, String type) {
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <Sanctions>
                <ReferenceValueSets>
                    <PartySubType ID="%s">%s</PartySubType>
                    <AliasType ID="1">A.K.A.</AliasType>
                </ReferenceValueSets>
                <DistinctParty FixedRef="12345">
                    <Profile ID="1" PartySubTypeID="%s">
                        <Identity>
                            <Alias Primary="true">
                                <DocumentedName>
                                    <DocumentedNamePart>
                                        <NamePartValue>Test Name</NamePartValue>
                                    </DocumentedNamePart>
                                </DocumentedName>
                            </Alias>
                        </Identity>
                    </Profile>
                </DistinctParty>
            </Sanctions>
            """, id, type, id);
    }

    private String createOfacXmlWithIdentity(boolean primary) {
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <Sanctions>
                <ReferenceValueSets>
                    <PartySubType ID="1">Individual</PartySubType>
                    <AliasType ID="1">A.K.A.</AliasType>
                </ReferenceValueSets>
                <DistinctParty FixedRef="12345">
                    <Profile ID="1" PartySubTypeID="1">
                        <Identity>
                            <Alias Primary="%s" AliasTypeID="1">
                                <DocumentedName>
                                    <DocumentedNamePart>
                                        <NamePartValue>Test Primary Name</NamePartValue>
                                    </DocumentedNamePart>
                                </DocumentedName>
                            </Alias>
                        </Identity>
                    </Profile>
                </DistinctParty>
            </Sanctions>
            """, primary);
    }

    private String createOfacXmlWithMultipleAliases() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <Sanctions>
                <ReferenceValueSets>
                    <PartySubType ID="1">Individual</PartySubType>
                    <AliasType ID="1">A.K.A.</AliasType>
                    <AliasType ID="2">F.K.A.</AliasType>
                </ReferenceValueSets>
                <DistinctParty FixedRef="12345">
                    <Profile ID="1" PartySubTypeID="1">
                        <Identity>
                            <Alias Primary="true" AliasTypeID="1">
                                <DocumentedName>
                                    <DocumentedNamePart>
                                        <NamePartValue>Primary Name</NamePartValue>
                                    </DocumentedNamePart>
                                </DocumentedName>
                            </Alias>
                            <Alias Primary="false" AliasTypeID="1">
                                <DocumentedName>
                                    <DocumentedNamePart>
                                        <NamePartValue>Alias One</NamePartValue>
                                    </DocumentedNamePart>
                                </DocumentedName>
                            </Alias>
                            <Alias Primary="false" AliasTypeID="2">
                                <DocumentedName>
                                    <DocumentedNamePart>
                                        <NamePartValue>Former Name</NamePartValue>
                                    </DocumentedNamePart>
                                </DocumentedName>
                            </Alias>
                        </Identity>
                    </Profile>
                </DistinctParty>
            </Sanctions>
            """;
    }

    private String createOfacXmlWithDocument() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <Sanctions>
                <ReferenceValueSets>
                    <PartySubType ID="1">Individual</PartySubType>
                    <AliasType ID="1">A.K.A.</AliasType>
                    <IDRegDocType ID="1">Passport</IDRegDocType>
                </ReferenceValueSets>
                <DistinctParty FixedRef="12345">
                    <Profile ID="1" PartySubTypeID="1">
                        <Identity>
                            <Alias Primary="true">
                                <DocumentedName>
                                    <DocumentedNamePart>
                                        <NamePartValue>Test Name</NamePartValue>
                                    </DocumentedNamePart>
                                </DocumentedName>
                            </Alias>
                        </Identity>
                        <IDRegDocument IDRegDocTypeID="1">
                            <IDRegistrationNo>AB123456</IDRegistrationNo>
                            <IssuingAuthority>Test Authority</IssuingAuthority>
                        </IDRegDocument>
                    </Profile>
                </DistinctParty>
            </Sanctions>
            """;
    }

    private String createOfacXmlWithMultipleParties() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <Sanctions>
                <ReferenceValueSets>
                    <PartySubType ID="1">Individual</PartySubType>
                    <PartySubType ID="2">Entity</PartySubType>
                    <AliasType ID="1">A.K.A.</AliasType>
                </ReferenceValueSets>
                <DistinctParty FixedRef="11111">
                    <Profile ID="1" PartySubTypeID="1">
                        <Identity>
                            <Alias Primary="true">
                                <DocumentedName>
                                    <DocumentedNamePart>
                                        <NamePartValue>Person One</NamePartValue>
                                    </DocumentedNamePart>
                                </DocumentedName>
                            </Alias>
                        </Identity>
                    </Profile>
                </DistinctParty>
                <DistinctParty FixedRef="22222">
                    <Profile ID="2" PartySubTypeID="2">
                        <Identity>
                            <Alias Primary="true">
                                <DocumentedName>
                                    <DocumentedNamePart>
                                        <NamePartValue>Company One</NamePartValue>
                                    </DocumentedNamePart>
                                </DocumentedName>
                            </Alias>
                        </Identity>
                    </Profile>
                </DistinctParty>
            </Sanctions>
            """;
    }
}
