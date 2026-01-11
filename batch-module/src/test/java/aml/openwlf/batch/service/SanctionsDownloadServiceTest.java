package aml.openwlf.batch.service;

import aml.openwlf.batch.config.SanctionsDownloadProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SanctionsDownloadService 테스트")
class SanctionsDownloadServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private SanctionsDownloadProperties properties;
    private SanctionsDownloadService downloadService;

    @BeforeEach
    void setUp() {
        properties = new SanctionsDownloadProperties();
        properties.setOfacUrl("https://test.ofac.url/SDN.XML");
        properties.setUnUrl("https://test.un.url/consolidated.xml");
        properties.setMaxRetries(3);
        properties.setRetryDelayMs(100); // 테스트에서는 짧게 설정
        
        downloadService = new SanctionsDownloadService(restTemplate, properties);
    }

    @Nested
    @DisplayName("downloadOfacXmlAsBytes() 메서드")
    class DownloadOfacXmlAsBytes {

        @Test
        @DisplayName("OFAC XML을 성공적으로 다운로드한다")
        void shouldDownloadOfacXmlSuccessfully() {
            // given
            byte[] expectedData = "<Sanctions>test</Sanctions>".getBytes();
            ResponseEntity<byte[]> response = new ResponseEntity<>(expectedData, HttpStatus.OK);
            
            when(restTemplate.exchange(
                    eq(properties.getOfacUrl()),
                    eq(HttpMethod.GET),
                    any(),
                    eq(byte[].class)
            )).thenReturn(response);

            // when
            byte[] result = downloadService.downloadOfacXmlAsBytes();

            // then
            assertThat(result).isEqualTo(expectedData);
            verify(restTemplate, times(1)).exchange(
                    eq(properties.getOfacUrl()),
                    eq(HttpMethod.GET),
                    any(),
                    eq(byte[].class)
            );
        }

        @Test
        @DisplayName("실패 시 재시도하고 성공하면 데이터를 반환한다")
        void shouldRetryAndSucceedOnSecondAttempt() {
            // given
            byte[] expectedData = "<Sanctions>test</Sanctions>".getBytes();
            ResponseEntity<byte[]> successResponse = new ResponseEntity<>(expectedData, HttpStatus.OK);
            
            when(restTemplate.exchange(
                    eq(properties.getOfacUrl()),
                    eq(HttpMethod.GET),
                    any(),
                    eq(byte[].class)
            ))
                    .thenThrow(new RestClientException("Connection failed"))
                    .thenReturn(successResponse);

            // when
            byte[] result = downloadService.downloadOfacXmlAsBytes();

            // then
            assertThat(result).isEqualTo(expectedData);
            verify(restTemplate, times(2)).exchange(
                    eq(properties.getOfacUrl()),
                    eq(HttpMethod.GET),
                    any(),
                    eq(byte[].class)
            );
        }

        @Test
        @DisplayName("모든 재시도 실패 시 예외를 던진다")
        void shouldThrowExceptionAfterAllRetriesFailed() {
            // given
            when(restTemplate.exchange(
                    eq(properties.getOfacUrl()),
                    eq(HttpMethod.GET),
                    any(),
                    eq(byte[].class)
            )).thenThrow(new RestClientException("Connection failed"));

            // when & then
            assertThatThrownBy(() -> downloadService.downloadOfacXmlAsBytes())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to download OFAC XML after")
                    .hasMessageContaining("3 attempts");

            verify(restTemplate, times(3)).exchange(
                    eq(properties.getOfacUrl()),
                    eq(HttpMethod.GET),
                    any(),
                    eq(byte[].class)
            );
        }

        @Test
        @DisplayName("HTTP 오류 응답 시 예외를 던진다")
        void shouldThrowExceptionOnHttpError() {
            // given
            ResponseEntity<byte[]> errorResponse = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            
            when(restTemplate.exchange(
                    eq(properties.getOfacUrl()),
                    eq(HttpMethod.GET),
                    any(),
                    eq(byte[].class)
            )).thenReturn(errorResponse);

            // when & then
            assertThatThrownBy(() -> downloadService.downloadOfacXmlAsBytes())
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("downloadUnXmlAsBytes() 메서드")
    class DownloadUnXmlAsBytes {

        @Test
        @DisplayName("UN XML을 성공적으로 다운로드한다")
        void shouldDownloadUnXmlSuccessfully() {
            // given
            byte[] expectedData = "<CONSOLIDATED_LIST>test</CONSOLIDATED_LIST>".getBytes();
            ResponseEntity<byte[]> response = new ResponseEntity<>(expectedData, HttpStatus.OK);
            
            when(restTemplate.exchange(
                    eq(properties.getUnUrl()),
                    eq(HttpMethod.GET),
                    any(),
                    eq(byte[].class)
            )).thenReturn(response);

            // when
            byte[] result = downloadService.downloadUnXmlAsBytes();

            // then
            assertThat(result).isEqualTo(expectedData);
        }

        @Test
        @DisplayName("모든 재시도 실패 시 예외를 던진다")
        void shouldThrowExceptionAfterAllRetriesFailed() {
            // given
            when(restTemplate.exchange(
                    eq(properties.getUnUrl()),
                    eq(HttpMethod.GET),
                    any(),
                    eq(byte[].class)
            )).thenThrow(new RestClientException("Timeout"));

            // when & then
            assertThatThrownBy(() -> downloadService.downloadUnXmlAsBytes())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to download UN XML");
        }
    }

    @Nested
    @DisplayName("downloadOfacXml() 메서드")
    class DownloadOfacXml {

        @Test
        @DisplayName("InputStream을 반환한다")
        void shouldReturnInputStream() throws Exception {
            // given
            byte[] expectedData = "<Sanctions>test</Sanctions>".getBytes();
            ResponseEntity<byte[]> response = new ResponseEntity<>(expectedData, HttpStatus.OK);
            
            when(restTemplate.exchange(
                    eq(properties.getOfacUrl()),
                    eq(HttpMethod.GET),
                    any(),
                    eq(byte[].class)
            )).thenReturn(response);

            // when
            InputStream result = downloadService.downloadOfacXml();

            // then
            assertThat(result).isNotNull();
            byte[] readData = result.readAllBytes();
            assertThat(readData).isEqualTo(expectedData);
        }
    }

    @Nested
    @DisplayName("downloadUnXml() 메서드")
    class DownloadUnXml {

        @Test
        @DisplayName("InputStream을 반환한다")
        void shouldReturnInputStream() throws Exception {
            // given
            byte[] expectedData = "<CONSOLIDATED_LIST>test</CONSOLIDATED_LIST>".getBytes();
            ResponseEntity<byte[]> response = new ResponseEntity<>(expectedData, HttpStatus.OK);
            
            when(restTemplate.exchange(
                    eq(properties.getUnUrl()),
                    eq(HttpMethod.GET),
                    any(),
                    eq(byte[].class)
            )).thenReturn(response);

            // when
            InputStream result = downloadService.downloadUnXml();

            // then
            assertThat(result).isNotNull();
            byte[] readData = result.readAllBytes();
            assertThat(readData).isEqualTo(expectedData);
        }
    }
}
