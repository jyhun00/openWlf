package aml.openwlf.data.entity.audit.enums;

/**
 * Alert 감사 로그 액션 타입
 */
public enum AlertActionType {
    /**
     * Alert 생성
     */
    CREATED,

    /**
     * 상태 변경 (NEW -> IN_REVIEW 등)
     */
    STATUS_CHANGED,

    /**
     * 담당자 배정
     */
    ASSIGNED,

    /**
     * 담당자 재배정
     */
    REASSIGNED,

    /**
     * 에스컬레이션
     */
    ESCALATED,

    /**
     * 해결 완료
     */
    RESOLVED,

    /**
     * 코멘트 추가
     */
    COMMENT_ADDED,

    /**
     * 조회
     */
    VIEWED,

    /**
     * 내보내기
     */
    EXPORTED,

    /**
     * Case에 연결
     */
    LINKED_TO_CASE,

    /**
     * Case에서 연결 해제
     */
    UNLINKED_FROM_CASE
}
