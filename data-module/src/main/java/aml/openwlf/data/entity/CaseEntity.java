package aml.openwlf.data.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Case Entity - Alert에서 전환된 조사 케이스
 *
 * Alert는 시스템이 자동 생성하고, Case는 분석가가 상세 조사를 위해 생성합니다.
 * 하나의 Case에 여러 Alert가 연결될 수 있습니다.
 *
 * BaseEntity를 확장하여 JPA Auditing으로 createdAt/updatedAt 자동 관리
 */
@Entity
@Table(name = "cases", indexes = {
        @Index(name = "idx_case_reference", columnList = "case_reference"),
        @Index(name = "idx_case_status", columnList = "status"),
        @Index(name = "idx_case_priority", columnList = "priority"),
        @Index(name = "idx_case_assigned_to", columnList = "assigned_to"),
        @Index(name = "idx_case_created_at", columnList = "created_at"),
        @Index(name = "idx_case_customer_id", columnList = "customer_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseEntity extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 케이스 참조 번호 (예: CASE-20250111-A1B2C3)
     */
    @Column(name = "case_reference", nullable = false, unique = true, length = 50)
    private String caseReference;
    
    /**
     * 케이스 제목
     */
    @Column(nullable = false, length = 500)
    private String title;
    
    /**
     * 케이스 설명
     */
    @Column(columnDefinition = "TEXT")
    private String description;
    
    /**
     * 케이스 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CaseStatus status;
    
    /**
     * 우선순위
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CasePriority priority;
    
    /**
     * 케이스 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "case_type", nullable = false, length = 30)
    private CaseType caseType;
    
    /**
     * 고객 ID
     */
    @Column(name = "customer_id", length = 100)
    private String customerId;
    
    /**
     * 고객명
     */
    @Column(name = "customer_name", nullable = false, length = 500)
    private String customerName;
    
    /**
     * 생년월일
     */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    /**
     * 국적
     */
    @Column(length = 10)
    private String nationality;
    
    /**
     * 총 리스크 점수 (연결된 Alert들의 최고 점수)
     */
    @Column(name = "risk_score")
    private Double riskScore;
    
    /**
     * 담당자
     */
    @Column(name = "assigned_to", length = 100)
    private String assignedTo;
    
    /**
     * 담당 팀
     */
    @Column(name = "assigned_team", length = 100)
    private String assignedTeam;
    
    /**
     * 마감 기한
     */
    @Column(name = "due_date")
    private LocalDateTime dueDate;
    
    /**
     * 최종 결정
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CaseDecision decision;
    
    /**
     * 결정 사유
     */
    @Column(name = "decision_rationale", columnDefinition = "TEXT")
    private String decisionRationale;
    
    /**
     * SAR 제출 여부
     */
    @Column(name = "sar_filed")
    private Boolean sarFiled;
    
    /**
     * SAR 참조 번호
     */
    @Column(name = "sar_reference", length = 100)
    private String sarReference;
    
    /**
     * 종료일시
     */
    @Column(name = "closed_at")
    private LocalDateTime closedAt;
    
    /**
     * 종료자
     */
    @Column(name = "closed_by", length = 100)
    private String closedBy;
    
    /**
     * 생성자
     */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    /**
     * 연결된 Alert 목록
     */
    @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CaseAlertEntity> linkedAlerts = new ArrayList<>();
    
    /**
     * 케이스 코멘트 목록
     */
    @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    @Builder.Default
    private List<CaseCommentEntity> comments = new ArrayList<>();
    
    /**
     * 케이스 활동 로그
     */
    @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    @Builder.Default
    private List<CaseActivityEntity> activities = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = CaseStatus.OPEN;
        }
        if (priority == null) {
            priority = CasePriority.MEDIUM;
        }
        if (sarFiled == null) {
            sarFiled = false;
        }
    }
    
    /**
     * 케이스 상태
     */
    public enum CaseStatus {
        OPEN,           // 신규 오픈
        IN_PROGRESS,    // 조사 중
        PENDING_INFO,   // 추가 정보 대기
        PENDING_REVIEW, // 검토 대기
        ESCALATED,      // 상위 보고
        CLOSED          // 종료
    }
    
    /**
     * 우선순위
     */
    public enum CasePriority {
        CRITICAL,   // 긴급 (24시간 내 처리)
        HIGH,       // 높음 (3일 내 처리)
        MEDIUM,     // 보통 (7일 내 처리)
        LOW         // 낮음 (14일 내 처리)
    }
    
    /**
     * 케이스 유형
     */
    public enum CaseType {
        SANCTIONS,      // 제재 매칭
        PEP,            // 정치적 주요 인물
        ADVERSE_MEDIA,  // 부정적 언론 보도
        FRAUD,          // 사기 의심
        MONEY_LAUNDERING, // 자금세탁 의심
        TERRORIST_FINANCING, // 테러자금 의심
        OTHER           // 기타
    }
    
    /**
     * 최종 결정
     */
    public enum CaseDecision {
        TRUE_POSITIVE,      // 실제 위험 확인
        FALSE_POSITIVE,     // 오탐
        INCONCLUSIVE,       // 판단 불가
        ESCALATED_TO_LE,    // 법 집행기관 보고
        NO_ACTION_REQUIRED  // 조치 불필요
    }
}
