package aml.openwlf.data.entity.audit.enums;

/**
 * 감시목록 변경 타입
 */
public enum WatchlistChangeType {
    /**
     * 항목 생성
     */
    ENTRY_CREATED,

    /**
     * 항목 수정
     */
    ENTRY_UPDATED,

    /**
     * 항목 삭제
     */
    ENTRY_DELETED,

    /**
     * 항목 활성화
     */
    ENTRY_ACTIVATED,

    /**
     * 항목 비활성화
     */
    ENTRY_DEACTIVATED,

    /**
     * 대량 가져오기
     */
    BULK_IMPORT,

    /**
     * 캐시 새로고침
     */
    CACHE_REFRESHED,

    /**
     * 동기화로 인한 삽입
     */
    SYNC_INSERTED,

    /**
     * 동기화로 인한 업데이트
     */
    SYNC_UPDATED,

    /**
     * 동기화로 인한 비활성화
     */
    SYNC_DEACTIVATED
}
