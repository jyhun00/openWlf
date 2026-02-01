package aml.openwlf.data.entity.audit.enums;

/**
 * 인증 이벤트 타입
 */
public enum AuthEventType {
    /**
     * 로그인 성공
     */
    LOGIN_SUCCESS,

    /**
     * 로그인 실패
     */
    LOGIN_FAILURE,

    /**
     * 로그아웃
     */
    LOGOUT,

    /**
     * 토큰 갱신
     */
    TOKEN_REFRESH,

    /**
     * 세션 만료
     */
    SESSION_EXPIRED,

    /**
     * 비밀번호 변경
     */
    PASSWORD_CHANGE,

    /**
     * 비밀번호 변경 실패
     */
    PASSWORD_CHANGE_FAILURE,

    /**
     * MFA 인증 성공
     */
    MFA_SUCCESS,

    /**
     * MFA 인증 실패
     */
    MFA_FAILURE,

    /**
     * 계정 잠금
     */
    ACCOUNT_LOCKED,

    /**
     * 계정 잠금 해제
     */
    ACCOUNT_UNLOCKED,

    /**
     * 비밀번호 초기화 요청
     */
    PASSWORD_RESET_REQUESTED,

    /**
     * 비밀번호 초기화 완료
     */
    PASSWORD_RESET_COMPLETED
}
