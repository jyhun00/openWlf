package aml.openwlf.batch.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 제재 리스트 다운로드 설정
 */
@Data
@Component
@ConfigurationProperties(prefix = "sanctions.download")
public class SanctionsDownloadProperties {

    /**
     * OFAC SDN Advanced XML URL
     */
    private String ofacUrl = "https://sanctionslistservice.ofac.treas.gov/api/PublicationPreview/exports/SDN_ADVANCED.XML";

    /**
     * UN Consolidated List XML URL
     */
    private String unUrl = "https://scsanctions.un.org/resources/xml/en/consolidated.xml";

    /**
     * EU Consolidated Financial Sanctions List XML URL
     */
    private String euUrl = "https://webgate.ec.europa.eu/fsd/fsf/public/files/xmlFullSanctionsList_1_1/content?token=dG9rZW4tMjAxNw";

    /**
     * 다운로드 타임아웃 (밀리초)
     */
    private int downloadTimeoutMs = 300000; // 5분

    /**
     * 재시도 횟수
     */
    private int maxRetries = 3;

    /**
     * 재시도 간격 (밀리초)
     */
    private long retryDelayMs = 5000;

    /**
     * 배치 크기 (한 번에 처리할 엔티티 수)
     */
    private int batchSize = 500;
}
