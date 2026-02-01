package aml.openwlf.data.entity.audit.enums;

/**
 * 민감 데이터 접근 타입
 */
public enum DataAccessType {
    /**
     * 단건 조회
     */
    VIEW,

    /**
     * 검색 조회
     */
    SEARCH,

    /**
     * 데이터 내보내기 (API)
     */
    EXPORT,

    /**
     * 파일 다운로드
     */
    DOWNLOAD,

    /**
     * 인쇄
     */
    PRINT,

    /**
     * 대량 조회
     */
    BULK_VIEW
}
