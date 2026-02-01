package aml.openwlf.data.entity.audit.enums;

/**
 * Case 감사 로그 액션 타입
 */
public enum CaseActionType {
    /**
     * Case 생성
     */
    CREATED,

    /**
     * 상태 변경
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
     * 우선순위 변경
     */
    PRIORITY_CHANGED,

    /**
     * 최종 결정
     */
    DECISION_MADE,

    /**
     * Alert 연결
     */
    ALERT_LINKED,

    /**
     * Alert 연결 해제
     */
    ALERT_UNLINKED,

    /**
     * 코멘트 추가
     */
    COMMENT_ADDED,

    /**
     * SAR 제출
     */
    SAR_FILED,

    /**
     * 문서 첨부
     */
    DOCUMENT_ATTACHED,

    /**
     * 조회
     */
    VIEWED,

    /**
     * 내보내기
     */
    EXPORTED,

    /**
     * 마감일 변경
     */
    DUE_DATE_CHANGED,

    /**
     * Case 종료
     */
    CLOSED,

    /**
     * Case 재개
     */
    REOPENED
}
