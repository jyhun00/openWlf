package aml.openwlf.batch.service;

import aml.openwlf.batch.config.SanctionsDownloadProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 제재 리스트 XML 다운로드 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SanctionsDownloadService {

    private final RestTemplate restTemplate;
    private final SanctionsDownloadProperties properties;

    /**
     * OFAC SDN Advanced XML 다운로드 (InputStream)
     */
    public InputStream downloadOfacXml() {
        return new ByteArrayInputStream(downloadOfacXmlAsBytes());
    }

    /**
     * OFAC SDN Advanced XML 다운로드 (byte[])
     */
    public byte[] downloadOfacXmlAsBytes() {
        return downloadWithRetry(properties.getOfacUrl(), "OFAC");
    }

    /**
     * UN Consolidated List XML 다운로드 (InputStream)
     */
    public InputStream downloadUnXml() {
        return new ByteArrayInputStream(downloadUnXmlAsBytes());
    }

    /**
     * UN Consolidated List XML 다운로드 (byte[])
     */
    public byte[] downloadUnXmlAsBytes() {
        return downloadWithRetry(properties.getUnUrl(), "UN");
    }

    /**
     * EU Consolidated Financial Sanctions List XML 다운로드 (InputStream)
     */
    public InputStream downloadEuXml() {
        return new ByteArrayInputStream(downloadEuXmlAsBytes());
    }

    /**
     * EU Consolidated Financial Sanctions List XML 다운로드 (byte[])
     */
    public byte[] downloadEuXmlAsBytes() {
        return downloadWithRetry(properties.getEuFullUrl(), "EU");
    }

    private byte[] downloadWithRetry(String url, String source) {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < properties.getMaxRetries()) {
            attempts++;
            try {
                log.info("Downloading {} XML (attempt {}/{}): {}", 
                        source, attempts, properties.getMaxRetries(), url);
                
                ResponseEntity<byte[]> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        byte[].class
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    byte[] body = response.getBody();
                    log.info("Successfully downloaded {} XML: {} bytes ({} MB)", 
                            source, body.length, String.format("%.2f", body.length / 1024.0 / 1024.0));
                    return body;
                } else {
                    throw new RuntimeException("Failed to download: HTTP " + response.getStatusCode());
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("Download attempt {} failed for {}: {}", attempts, source, e.getMessage());
                
                if (attempts < properties.getMaxRetries()) {
                    try {
                        Thread.sleep(properties.getRetryDelayMs());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Download interrupted", ie);
                    }
                }
            }
        }

        throw new RuntimeException("Failed to download " + source + " XML after " + 
                properties.getMaxRetries() + " attempts", lastException);
    }
}
