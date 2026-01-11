package aml.openwlf.batch.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SanctionsDownloadProperties 테스트")
class SanctionsDownloadPropertiesTest {

    @Nested
    @DisplayName("기본값 테스트")
    class DefaultValues {

        @Test
        @DisplayName("OFAC URL 기본값이 설정되어 있다")
        void shouldHaveDefaultOfacUrl() {
            // given
            SanctionsDownloadProperties properties = new SanctionsDownloadProperties();

            // then
            assertThat(properties.getOfacUrl())
                    .isEqualTo("https://sanctionslistservice.ofac.treas.gov/api/PublicationPreview/exports/SDN_ADVANCED.XML");
        }

        @Test
        @DisplayName("UN URL 기본값이 설정되어 있다")
        void shouldHaveDefaultUnUrl() {
            // given
            SanctionsDownloadProperties properties = new SanctionsDownloadProperties();

            // then
            assertThat(properties.getUnUrl())
                    .isEqualTo("https://scsanctions.un.org/resources/xml/en/consolidated.xml");
        }

        @Test
        @DisplayName("다운로드 타임아웃 기본값은 5분이다")
        void shouldHaveDefaultDownloadTimeout() {
            // given
            SanctionsDownloadProperties properties = new SanctionsDownloadProperties();

            // then
            assertThat(properties.getDownloadTimeoutMs()).isEqualTo(300000);
        }

        @Test
        @DisplayName("최대 재시도 횟수 기본값은 3이다")
        void shouldHaveDefaultMaxRetries() {
            // given
            SanctionsDownloadProperties properties = new SanctionsDownloadProperties();

            // then
            assertThat(properties.getMaxRetries()).isEqualTo(3);
        }

        @Test
        @DisplayName("재시도 간격 기본값은 5초이다")
        void shouldHaveDefaultRetryDelay() {
            // given
            SanctionsDownloadProperties properties = new SanctionsDownloadProperties();

            // then
            assertThat(properties.getRetryDelayMs()).isEqualTo(5000);
        }

        @Test
        @DisplayName("배치 크기 기본값은 500이다")
        void shouldHaveDefaultBatchSize() {
            // given
            SanctionsDownloadProperties properties = new SanctionsDownloadProperties();

            // then
            assertThat(properties.getBatchSize()).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("Setter 테스트")
    class SetterTests {

        @Test
        @DisplayName("OFAC URL을 변경할 수 있다")
        void shouldBeAbleToChangeOfacUrl() {
            // given
            SanctionsDownloadProperties properties = new SanctionsDownloadProperties();
            String newUrl = "https://custom.ofac.url/SDN.XML";

            // when
            properties.setOfacUrl(newUrl);

            // then
            assertThat(properties.getOfacUrl()).isEqualTo(newUrl);
        }

        @Test
        @DisplayName("UN URL을 변경할 수 있다")
        void shouldBeAbleToChangeUnUrl() {
            // given
            SanctionsDownloadProperties properties = new SanctionsDownloadProperties();
            String newUrl = "https://custom.un.url/consolidated.xml";

            // when
            properties.setUnUrl(newUrl);

            // then
            assertThat(properties.getUnUrl()).isEqualTo(newUrl);
        }

        @Test
        @DisplayName("다운로드 타임아웃을 변경할 수 있다")
        void shouldBeAbleToChangeDownloadTimeout() {
            // given
            SanctionsDownloadProperties properties = new SanctionsDownloadProperties();

            // when
            properties.setDownloadTimeoutMs(600000);

            // then
            assertThat(properties.getDownloadTimeoutMs()).isEqualTo(600000);
        }

        @Test
        @DisplayName("최대 재시도 횟수를 변경할 수 있다")
        void shouldBeAbleToChangeMaxRetries() {
            // given
            SanctionsDownloadProperties properties = new SanctionsDownloadProperties();

            // when
            properties.setMaxRetries(5);

            // then
            assertThat(properties.getMaxRetries()).isEqualTo(5);
        }

        @Test
        @DisplayName("재시도 간격을 변경할 수 있다")
        void shouldBeAbleToChangeRetryDelay() {
            // given
            SanctionsDownloadProperties properties = new SanctionsDownloadProperties();

            // when
            properties.setRetryDelayMs(10000);

            // then
            assertThat(properties.getRetryDelayMs()).isEqualTo(10000);
        }

        @Test
        @DisplayName("배치 크기를 변경할 수 있다")
        void shouldBeAbleToChangeBatchSize() {
            // given
            SanctionsDownloadProperties properties = new SanctionsDownloadProperties();

            // when
            properties.setBatchSize(1000);

            // then
            assertThat(properties.getBatchSize()).isEqualTo(1000);
        }
    }
}
