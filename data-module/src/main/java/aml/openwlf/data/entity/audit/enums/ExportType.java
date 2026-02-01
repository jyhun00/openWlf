package aml.openwlf.data.entity.audit.enums;

/**
 * 데이터 내보내기 타입
 */
public enum ExportType {
    /**
     * 보고서 생성
     */
    REPORT,

    /**
     * CSV 다운로드
     */
    CSV_DOWNLOAD,

    /**
     * Excel 다운로드
     */
    EXCEL_DOWNLOAD,

    /**
     * PDF 다운로드
     */
    PDF_DOWNLOAD,

    /**
     * 인쇄
     */
    PRINT,

    /**
     * API 내보내기
     */
    API_EXPORT,

    /**
     * JSON 다운로드
     */
    JSON_DOWNLOAD
}
