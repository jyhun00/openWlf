package aml.openwlf.batch.parser.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ParsedSanctionsData 테스트")
class ParsedSanctionsDataTest {

    @Nested
    @DisplayName("generateContentHash() 메서드")
    class GenerateContentHash {

        @Test
        @DisplayName("동일한 데이터에 대해 동일한 해시를 생성한다")
        void shouldGenerateSameHashForSameData() {
            // given
            ParsedSanctionsData data1 = createSampleData();
            ParsedSanctionsData data2 = createSampleData();

            // when
            String hash1 = data1.generateContentHash();
            String hash2 = data2.generateContentHash();

            // then
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("다른 데이터에 대해 다른 해시를 생성한다")
        void shouldGenerateDifferentHashForDifferentData() {
            // given
            ParsedSanctionsData data1 = createSampleData();
            ParsedSanctionsData data2 = createSampleData();
            data2.setPrimaryName("Different Name");

            // when
            String hash1 = data1.generateContentHash();
            String hash2 = data2.generateContentHash();

            // then
            assertThat(hash1).isNotEqualTo(hash2);
        }

        @Test
        @DisplayName("이름 순서가 달라도 동일한 해시를 생성한다")
        void shouldGenerateSameHashRegardlessOfNameOrder() {
            // given
            ParsedSanctionsData data1 = ParsedSanctionsData.builder()
                    .sourceUid("UN-123")
                    .sourceFile("UN")
                    .names(Arrays.asList(
                            ParsedSanctionsData.ParsedName.builder()
                                    .nameType("Primary").fullName("John Doe").build(),
                            ParsedSanctionsData.ParsedName.builder()
                                    .nameType("AKA").fullName("Johnny").build()
                    ))
                    .build();

            ParsedSanctionsData data2 = ParsedSanctionsData.builder()
                    .sourceUid("UN-123")
                    .sourceFile("UN")
                    .names(Arrays.asList(
                            ParsedSanctionsData.ParsedName.builder()
                                    .nameType("AKA").fullName("Johnny").build(),
                            ParsedSanctionsData.ParsedName.builder()
                                    .nameType("Primary").fullName("John Doe").build()
                    ))
                    .build();

            // when
            String hash1 = data1.generateContentHash();
            String hash2 = data2.generateContentHash();

            // then
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("주소 순서가 달라도 동일한 해시를 생성한다")
        void shouldGenerateSameHashRegardlessOfAddressOrder() {
            // given
            ParsedSanctionsData data1 = ParsedSanctionsData.builder()
                    .sourceUid("UN-123")
                    .sourceFile("UN")
                    .addresses(Arrays.asList(
                            ParsedSanctionsData.ParsedAddress.builder()
                                    .fullAddress("Address A").build(),
                            ParsedSanctionsData.ParsedAddress.builder()
                                    .fullAddress("Address B").build()
                    ))
                    .build();

            ParsedSanctionsData data2 = ParsedSanctionsData.builder()
                    .sourceUid("UN-123")
                    .sourceFile("UN")
                    .addresses(Arrays.asList(
                            ParsedSanctionsData.ParsedAddress.builder()
                                    .fullAddress("Address B").build(),
                            ParsedSanctionsData.ParsedAddress.builder()
                                    .fullAddress("Address A").build()
                    ))
                    .build();

            // when
            String hash1 = data1.generateContentHash();
            String hash2 = data2.generateContentHash();

            // then
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("문서 순서가 달라도 동일한 해시를 생성한다")
        void shouldGenerateSameHashRegardlessOfDocumentOrder() {
            // given
            ParsedSanctionsData data1 = ParsedSanctionsData.builder()
                    .sourceUid("UN-123")
                    .sourceFile("UN")
                    .documents(Arrays.asList(
                            ParsedSanctionsData.ParsedDocument.builder()
                                    .documentType("Passport").documentNumber("ABC123").build(),
                            ParsedSanctionsData.ParsedDocument.builder()
                                    .documentType("ID").documentNumber("XYZ789").build()
                    ))
                    .build();

            ParsedSanctionsData data2 = ParsedSanctionsData.builder()
                    .sourceUid("UN-123")
                    .sourceFile("UN")
                    .documents(Arrays.asList(
                            ParsedSanctionsData.ParsedDocument.builder()
                                    .documentType("ID").documentNumber("XYZ789").build(),
                            ParsedSanctionsData.ParsedDocument.builder()
                                    .documentType("Passport").documentNumber("ABC123").build()
                    ))
                    .build();

            // when
            String hash1 = data1.generateContentHash();
            String hash2 = data2.generateContentHash();

            // then
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("null 필드가 있어도 해시를 생성한다")
        void shouldGenerateHashWithNullFields() {
            // given
            ParsedSanctionsData data = ParsedSanctionsData.builder()
                    .sourceUid("UN-123")
                    .sourceFile("UN")
                    .primaryName(null)
                    .gender(null)
                    .birthDate(null)
                    .build();

            // when
            String hash = data.generateContentHash();

            // then
            assertThat(hash).isNotNull();
            assertThat(hash).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("ParsedName 내부 클래스")
    class ParsedNameTest {

        @Test
        @DisplayName("빌더로 이름 정보를 생성할 수 있다")
        void shouldBuildParsedName() {
            // when
            ParsedSanctionsData.ParsedName name = ParsedSanctionsData.ParsedName.builder()
                    .nameType("Primary")
                    .fullName("Kim Jong Un")
                    .script("Latin")
                    .qualityScore(100)
                    .firstName("Jong Un")
                    .middleName(null)
                    .lastName("Kim")
                    .build();

            // then
            assertThat(name.getNameType()).isEqualTo("Primary");
            assertThat(name.getFullName()).isEqualTo("Kim Jong Un");
            assertThat(name.getScript()).isEqualTo("Latin");
            assertThat(name.getQualityScore()).isEqualTo(100);
            assertThat(name.getFirstName()).isEqualTo("Jong Un");
            assertThat(name.getLastName()).isEqualTo("Kim");
        }
    }

    @Nested
    @DisplayName("ParsedAddress 내부 클래스")
    class ParsedAddressTest {

        @Test
        @DisplayName("빌더로 주소 정보를 생성할 수 있다")
        void shouldBuildParsedAddress() {
            // when
            ParsedSanctionsData.ParsedAddress address = ParsedSanctionsData.ParsedAddress.builder()
                    .addressType("Primary")
                    .fullAddress("123 Main St, New York, NY 10001, USA")
                    .street("123 Main St")
                    .city("New York")
                    .stateProvince("NY")
                    .postalCode("10001")
                    .country("United States")
                    .countryCode("US")
                    .note("Business address")
                    .build();

            // then
            assertThat(address.getAddressType()).isEqualTo("Primary");
            assertThat(address.getFullAddress()).contains("123 Main St");
            assertThat(address.getCity()).isEqualTo("New York");
            assertThat(address.getCountryCode()).isEqualTo("US");
        }
    }

    @Nested
    @DisplayName("ParsedDocument 내부 클래스")
    class ParsedDocumentTest {

        @Test
        @DisplayName("빌더로 문서 정보를 생성할 수 있다")
        void shouldBuildParsedDocument() {
            // when
            ParsedSanctionsData.ParsedDocument document = ParsedSanctionsData.ParsedDocument.builder()
                    .documentType("Passport")
                    .documentNumber("AB123456")
                    .issuingCountry("North Korea")
                    .issuingCountryCode("KP")
                    .issueDate(LocalDate.of(2020, 1, 1))
                    .expiryDate(LocalDate.of(2030, 1, 1))
                    .issuingAuthority("Ministry of Foreign Affairs")
                    .note("Diplomatic passport")
                    .build();

            // then
            assertThat(document.getDocumentType()).isEqualTo("Passport");
            assertThat(document.getDocumentNumber()).isEqualTo("AB123456");
            assertThat(document.getIssuingCountryCode()).isEqualTo("KP");
            assertThat(document.getIssueDate()).isEqualTo(LocalDate.of(2020, 1, 1));
            assertThat(document.getExpiryDate()).isEqualTo(LocalDate.of(2030, 1, 1));
        }
    }

    @Nested
    @DisplayName("기본값 테스트")
    class DefaultValuesTest {

        @Test
        @DisplayName("컬렉션 필드는 빈 리스트로 초기화된다")
        void shouldInitializeCollectionsAsEmptyLists() {
            // when
            ParsedSanctionsData data = ParsedSanctionsData.builder()
                    .sourceUid("UN-123")
                    .build();

            // then
            assertThat(data.getNames()).isNotNull().isEmpty();
            assertThat(data.getAddresses()).isNotNull().isEmpty();
            assertThat(data.getDocuments()).isNotNull().isEmpty();
            assertThat(data.getAdditionalFeatures()).isNotNull().isEmpty();
        }
    }

    private ParsedSanctionsData createSampleData() {
        Map<String, Object> additionalFeatures = new HashMap<>();
        additionalFeatures.put("programs", Arrays.asList("DPRK", "IRAN"));

        return ParsedSanctionsData.builder()
                .sourceUid("UN-12345")
                .sourceFile("UN")
                .entityType("Individual")
                .primaryName("Test Person")
                .gender("Male")
                .birthDate(LocalDate.of(1970, 1, 1))
                .nationality("North Korea")
                .sanctionListType("UN Security Council Consolidated List")
                .names(Arrays.asList(
                        ParsedSanctionsData.ParsedName.builder()
                                .nameType("Primary")
                                .fullName("Test Person")
                                .script("Latin")
                                .qualityScore(100)
                                .build()
                ))
                .addresses(Arrays.asList(
                        ParsedSanctionsData.ParsedAddress.builder()
                                .fullAddress("Pyongyang, North Korea")
                                .city("Pyongyang")
                                .country("North Korea")
                                .countryCode("KP")
                                .build()
                ))
                .documents(Arrays.asList(
                        ParsedSanctionsData.ParsedDocument.builder()
                                .documentType("Passport")
                                .documentNumber("123456789")
                                .issuingCountryCode("KP")
                                .build()
                ))
                .additionalFeatures(additionalFeatures)
                .build();
    }
}
